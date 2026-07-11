package com.example.data

import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Orchestrates food recognition for typed / spoken input.
 *
 * Fixes two long-standing gaps:
 *  1. Quantity — "3 roti" now logs 3× the nutrition, not 1.
 *  2. Multiple foods — "2 roti and dal and rice" returns one entry PER food
 *     (each with its own macros), instead of a single consolidated total.
 *
 * Resolution order per food: LocalFoodParser (offline Indian/everyday DB) →
 * built-in SupplementDb → USDA online.
 */
object FoodAnalyzer {

    data class Analysis(val items: List<FoodNutrition>, val unresolved: List<String>) {
        val isEmpty get() = items.isEmpty()
    }

    // Split a multi-food phrase. NOTE: we don't split on "with" so combined DB
    // entries like "coffee with milk" stay intact; LocalFoodParser still finds
    // multiple foods inside a single segment on its own. We also DON'T split on
    // "/" — that shattered fractions ("1/2 pizza" → "1" + "2 pizza"), and the
    // stray "1" then matched the first supplement in the DB (a phantom whey row).
    private val SEPARATORS = Regex("""\s*(?:,|;|\+|&|\band\b|\bplus\b|\n)\s*""", RegexOption.IGNORE_CASE)

    private val WORD_NUM = mapOf(
        "half" to 0.5f, "one" to 1f, "a" to 1f, "an" to 1f, "two" to 2f, "three" to 3f,
        "four" to 4f, "five" to 5f, "six" to 6f, "seven" to 7f, "eight" to 8f,
        "nine" to 9f, "ten" to 10f, "couple" to 2f, "few" to 3f, "dozen" to 12f
    )
    // Countable serving words — "3 scoops" means 3 servings
    private val COUNT_UNITS = setOf(
        "scoop", "scoops", "glass", "glasses", "cup", "cups", "katori", "katoris",
        "bowl", "bowls", "plate", "plates", "piece", "pieces", "slice", "slices",
        "serving", "servings", "pack", "packs", "packet", "packets", "bottle",
        "bottles", "can", "cans", "roti", "rotis", "egg", "eggs"
    )
    // Mass/volume units — "200g" is a weight, NOT a count; never used as a multiplier
    private val MASS_UNITS = setOf("g", "gm", "gms", "gram", "grams", "kg", "ml", "l", "litre", "liter")

    /**
     * Pull a leading quantity off a food phrase for the supplement / USDA path.
     * Returns (multiplier, foodNameWithoutQuantity). A weight like "200g" yields
     * multiplier 1 (we can't safely scale an unknown food by grams).
     */
    fun quantityPrefix(seg: String): Pair<Float, String> {
        val s = seg.trim()
        val tokens = s.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (tokens.size < 2) return 1f to s

        // "200g" / "200ml" attached → weight, strip but don't scale
        val attached = Regex("^(\\d+(?:\\.\\d+)?)([a-z]+)$", RegexOption.IGNORE_CASE).find(tokens[0])
        if (attached != null && attached.groupValues[2].lowercase() in MASS_UNITS) {
            return 1f to tokens.drop(1).joinToString(" ")
        }

        var mult = 1f
        var consumed = 0
        val asDigit = tokens[0].toFloatOrNull()
        val asWord = WORD_NUM[tokens[0].lowercase()]
        val asFraction = parseFraction(tokens[0])   // "1/2" → 0.5, "3/4" → 0.75
        when {
            asFraction != null -> { mult = asFraction; consumed = 1 }
            asDigit != null    -> { mult = asDigit; consumed = 1 }
            asWord != null     -> { mult = asWord;  consumed = 1 }
            else               -> return 1f to s   // no leading count
        }

        // token after the number
        if (consumed < tokens.size) {
            val unit = tokens[consumed].lowercase().trimEnd('s')
            val unitFull = tokens[consumed].lowercase()
            if (unitFull in MASS_UNITS || unit in MASS_UNITS) {
                mult = 1f; consumed++                       // "200 g chicken" → weight
            } else if (unitFull in COUNT_UNITS) {
                consumed++                                  // "3 scoops whey" → 3 servings
            }
            // else bare "3 eggs" → keep count, don't consume the food word
        }
        var rest = tokens.drop(consumed).joinToString(" ")
        if (rest.startsWith("of ")) rest = rest.substring(3)
        return mult to rest.trim().ifEmpty { s }
    }

    /** "1/2" → 0.5, "3/4" → 0.75; null if not a simple fraction. */
    private fun parseFraction(token: String): Float? {
        val m = Regex("^(\\d+)/(\\d+)$").find(token) ?: return null
        val num = m.groupValues[1].toFloat()
        val den = m.groupValues[2].toFloatOrNull() ?: return null
        return if (den != 0f) num / den else null
    }

    private fun segments(query: String): List<String> {
        val segs = query.split(SEPARATORS).map { it.trim() }.filter { it.isNotEmpty() }
        return if (segs.isEmpty() && query.isNotBlank()) listOf(query.trim()) else segs
    }

    /** Offline-only pass (LocalFoodParser + SupplementDb) — instant, no network. */
    fun analyzeLocal(query: String): Analysis {
        val items = mutableListOf<FoodNutrition>()
        val unresolved = mutableListOf<String>()
        for (seg in segments(query)) {
            val local = LocalFoodParser.parseEach(seg)
            if (local.isNotEmpty()) { items += local; continue }
            val (qty, name) = quantityPrefix(seg)
            val sup = SupplementDb.search(name).firstOrNull()
            if (sup != null) { items += SupplementDb.toNutrition(sup, qty); continue }
            unresolved += seg
        }
        return Analysis(items, unresolved)
    }

    /** Full pass — resolves anything unknown against the USDA online database. */
    suspend fun analyze(query: String): Analysis {
        val local = analyzeLocal(query)
        if (local.unresolved.isEmpty()) return local
        val items = local.items.toMutableList()
        val stillUnresolved = mutableListOf<String>()
        for (seg in local.unresolved) {
            val (qty, name) = quantityPrefix(seg)
            UsdaFoodService.search(name).fold(
                onSuccess = { items += if (qty == 1f) it else it.scaled(qty) },
                onFailure = { stillUnresolved += seg }
            )
        }
        return Analysis(items, stillUnresolved)
    }

    /** Combined total across all recognised foods (for the summary row). */
    fun total(items: List<FoodNutrition>): FoodNutrition = FoodNutrition(
        foodName    = "Total · ${items.size} items",
        servingSize = items.joinToString(", ") { it.foodName },
        calories    = items.sumOf { it.calories },
        proteinG    = items.fold(0f) { a, n -> a + n.proteinG },
        carbsG      = items.fold(0f) { a, n -> a + n.carbsG },
        fatG        = items.fold(0f) { a, n -> a + n.fatG },
        fiberG      = items.fold(0f) { a, n -> a + n.fiberG },
        confidence  = "medium",
        notes       = "Combined total"
    )
}

internal fun fmtQty(m: Float): String =
    if (abs(m - m.roundToInt()) < 0.01f) m.roundToInt().toString() else "%.1f".format(m)

/** Scale an already-resolved nutrition result by a whole/fractional count. */
fun FoodNutrition.scaled(mult: Float): FoodNutrition {
    if (mult == 1f) return this
    return copy(
        foodName    = "${fmtQty(mult)}× $foodName",
        servingSize = "${fmtQty(mult)} × $servingSize",
        calories    = (calories * mult).roundToInt(),
        proteinG    = proteinG * mult,
        carbsG      = carbsG * mult,
        fatG        = fatG * mult,
        fiberG      = fiberG * mult
    )
}

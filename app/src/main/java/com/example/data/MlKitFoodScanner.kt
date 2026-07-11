package com.example.data

import android.graphics.Bitmap
import com.google.mlkit.common.model.LocalModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.custom.CustomImageLabelerOptions
import kotlinx.coroutines.tasks.await

/**
 * On-device food image recognition — three-layer chain, cheapest first:
 *
 *  1. Custom TFLite food classifier (Google food_V1, 2 023 dish classes incl.
 *     khichdi, biryani, dosa, chole bhature…) bundled in assets. Free, offline.
 *  2. Local nutrition database lookup for the recognised dish name.
 *  3. Gemini vision (free-tier key already used by the meal planner) when the
 *     on-device model can't name the dish or the dish has no local entry.
 */
object MlKitFoodScanner {

    private val foodModel = LocalModel.Builder()
        .setAssetFilePath("food_v1.tflite")
        .build()

    private val labeler by lazy {
        ImageLabeling.getClient(
            CustomImageLabelerOptions.Builder(foodModel)
                .setConfidenceThreshold(0.25f)
                .setMaxResultCount(5)
                .build()
        )
    }

    // Model vocabulary → local DB name, for labels whose wording doesn't
    // contains-match a database alias directly.
    private val OVERRIDES = mapOf(
        "chapati"          to "roti",
        "flatbread"        to "roti",
        "crepe"            to "dosa",
        "porridge"         to "porridge",
        "congee"           to "porridge",
        "yogurt"           to "curd",
        "cheesecake"       to "barfi",
        "doughnut"         to "gulab jamun",
        "french toast"     to "bread",
        "scrambled eggs"   to "egg bhurji",
        "fried egg"        to "omelette",
        "fried chicken"    to "chicken 65",
        "chicken nugget"   to "chicken 65",
        "falafel"          to "cutlet",
        "hummus"           to "chana",
        "burrito"          to "frankie",
        "quesadilla"       to "frankie",
        "ramen"            to "hakka noodles",
        "chow mein"        to "hakka noodles",
        "spaghetti"        to "pasta",
        "macaroni"         to "pasta",
        "lasagne"          to "pasta",
        "risotto"          to "veg pulao",
        "paella"           to "veg pulao",
        "nasi goreng"      to "fried rice",
        "bibimbap"         to "fried rice",
        "gyoza"            to "momos",
        "wonton"           to "momos",
        "baozi"            to "momos",
        "smoothie"         to "mango shake",
        "milkshake"        to "mango shake"
    )

    suspend fun analyze(bitmap: Bitmap): Result<FoodNutrition> {
        val labels = try {
            labeler.process(InputImage.fromBitmap(bitmap, 0)).await()
                .sortedByDescending { it.confidence }
        } catch (e: Exception) {
            emptyList()
        }

        // 1+2. On-device model → local nutrition DB
        for (label in labels) {
            val entry = matchEntry(label.text) ?: continue
            return Result.success(toNutrition(entry, label.text, label.confidence))
        }

        // 3. Gemini vision fallback (needs internet; free tier)
        val hint = labels.take(3).joinToString(", ") { it.text }
        val gemini = GeminiFoodVision.analyze(bitmap, hint)
        if (gemini.isSuccess) return gemini

        val detected = labels.take(3).joinToString(", ") {
            "${it.text} (${(it.confidence * 100).toInt()}%)"
        }
        return Result.failure(Exception(
            if (labels.isEmpty())
                "No food detected. Try a clearer, well-lit photo with the dish centred — or use Voice mode."
            else
                "Detected $detected but couldn't complete the analysis (check internet connection). " +
                "Try again online, or use Voice mode and say the dish name."
        ))
    }

    /** Model label → nutrition entry: direct contains-match, override table, then suffix words. */
    private fun matchEntry(labelText: String): LocalFoodParser.FoodEntry? {
        val lower = labelText.lowercase().trim()
        OVERRIDES[lower]?.let { mapped ->
            LocalFoodParser.findFood(mapped)?.let { return it }
        }
        LocalFoodParser.findFood(lower)?.let { return it }
        // "Mughlai paratha" → "paratha": drop leading words one at a time
        val words = lower.split(" ").filter { it.isNotBlank() }
        for (start in 1 until words.size) {
            val suffix = words.drop(start).joinToString(" ")
            LocalFoodParser.findFood(suffix)?.let { return it }
        }
        return null
    }

    private fun toNutrition(entry: LocalFoodParser.FoodEntry, label: String, conf: Float): FoodNutrition {
        val grams = entry.servingGrams.toFloat()
        return FoodNutrition(
            foodName    = label.replaceFirstChar { it.uppercase() },
            servingSize = entry.servingLabel,
            calories    = (entry.caloriesPer100g * grams / 100f).toInt(),
            proteinG    = entry.proteinPer100g  * grams / 100f,
            carbsG      = entry.carbsPer100g    * grams / 100f,
            fatG        = entry.fatPer100g      * grams / 100f,
            fiberG      = entry.fiberPer100g    * grams / 100f,
            confidence  = when {
                conf >= 0.65f -> "high"
                conf >= 0.40f -> "medium"
                else          -> "low"
            },
            notes = "Recognised on-device as \"$label\" · ${(conf * 100).toInt()}% confidence"
        )
    }
}

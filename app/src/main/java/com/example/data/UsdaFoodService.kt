package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Free USDA FoodData Central API client.
 * Covers 600k+ foods including Indian items and branded products.
 *
 * API key comes from .env via BuildConfig (USDA_API_KEY). The DEMO_KEY
 * fallback works unauthenticated but is limited to 30 req/hour per IP —
 * get a free 1,000 req/hour key at https://api.data.gov/signup.
 *
 * Used as fallback when LocalFoodParser doesn't recognise a food (voice)
 * and when Open Food Facts doesn't find a barcode product.
 */
object UsdaFoodService {

    private const val BASE     = "https://api.nal.usda.gov/fdc/v1"
    private val API_KEY = com.example.BuildConfig.USDA_API_KEY.ifBlank { "DEMO_KEY" }
    private const val TIMEOUT  = 12_000

    // USDA nutrient IDs (SR Legacy + Branded Foods)
    private const val NID_ENERGY = 1008   // kcal
    private const val NID_PROTEIN = 1003
    private const val NID_CARBS   = 1005
    private const val NID_FAT     = 1004
    private const val NID_FIBER   = 1079

    // ── Voice fallback: search by food name ───────────────────────────────────
    suspend fun search(query: String): Result<FoodNutrition> = withContext(Dispatchers.IO) {
        try {
            val encoded = URLEncoder.encode(query.trim().take(150), "UTF-8")
            val url     = "$BASE/foods/search?query=$encoded&pageSize=5&api_key=$API_KEY"
            val raw     = httpGet(url)
            val root    = JSONObject(raw)

            val foods = root.optJSONArray("foods")
            if (foods == null || foods.length() == 0) {
                return@withContext Result.failure(
                    Exception("\"$query\" not found in USDA database. Try a more specific name.")
                )
            }

            // Pick the best match — prefer SR Legacy (whole food) over Branded
            var best = foods.getJSONObject(0)
            for (i in 0 until foods.length()) {
                val f = foods.getJSONObject(i)
                if (f.optString("dataType") == "SR Legacy") { best = f; break }
            }

            parseFood(best, query)
        } catch (e: Exception) {
            Result.failure(Exception("USDA lookup failed: ${e.message}"))
        }
    }

    // ── Barcode fallback: search UPC in Branded Foods ─────────────────────────
    suspend fun lookupBarcode(upc: String): Result<FoodNutrition> = withContext(Dispatchers.IO) {
        try {
            val encoded = URLEncoder.encode(upc.trim(), "UTF-8")
            val url     = "$BASE/foods/search?query=$encoded&dataType=Branded&pageSize=3&api_key=$API_KEY"
            val raw     = httpGet(url)
            val root    = JSONObject(raw)

            val foods = root.optJSONArray("foods")
            if (foods == null || foods.length() == 0) {
                return@withContext Result.failure(
                    Exception("Barcode $upc not found in USDA branded database.")
                )
            }

            parseFood(foods.getJSONObject(0), upc)
        } catch (e: Exception) {
            Result.failure(Exception("USDA barcode lookup failed: ${e.message}"))
        }
    }

    // ── Parse a USDA food JSON object ─────────────────────────────────────────
    private fun parseFood(food: JSONObject, fallbackName: String): Result<FoodNutrition> {
        val name  = food.optString("description", fallbackName)
            .split(",").first().trim()
            .replaceFirstChar { it.uppercase() }
        val brand = food.optString("brandOwner", "")

        // foodNutrients is an array of {nutrientId, value}
        val nutrients = food.optJSONArray("foodNutrients") ?: return Result.failure(
            Exception("No nutrition data available for $name.")
        )

        val nutrientMap = mutableMapOf<Int, Double>()
        for (i in 0 until nutrients.length()) {
            val n   = nutrients.getJSONObject(i)
            val id  = n.optInt("nutrientId", -1)
            val v   = n.optDouble("value", 0.0)
            if (id > 0) nutrientMap[id] = v
        }

        // All USDA values are per 100g
        val caloriesPer100 = nutrientMap[NID_ENERGY]  ?: 0.0
        val proteinPer100  = nutrientMap[NID_PROTEIN] ?: 0.0
        val carbsPer100    = nutrientMap[NID_CARBS]   ?: 0.0
        val fatPer100      = nutrientMap[NID_FAT]     ?: 0.0
        val fiberPer100    = nutrientMap[NID_FIBER]   ?: 0.0

        // Use USDA serving size if available, else default 100g
        val servingGrams = food.optDouble("servingSize", 100.0).toFloat()
            .coerceIn(10f, 1000f)
        val servingUnit  = food.optString("servingSizeUnit", "g")

        val factor = servingGrams / 100f
        val displayName = if (brand.isNotBlank()) "$name — $brand" else name

        return Result.success(
            FoodNutrition(
                foodName    = displayName,
                servingSize = "%.0f%s".format(servingGrams, servingUnit),
                calories    = (caloriesPer100 * factor).toInt(),
                proteinG    = (proteinPer100  * factor).toFloat(),
                carbsG      = (carbsPer100    * factor).toFloat(),
                fatG        = (fatPer100      * factor).toFloat(),
                fiberG      = (fiberPer100    * factor).toFloat(),
                confidence  = "high",
                notes       = "USDA FoodData Central · per ${servingGrams.toInt()}$servingUnit"
            )
        )
    }

    private fun httpGet(url: String): String {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod  = "GET"
            connectTimeout = TIMEOUT
            readTimeout    = TIMEOUT
            setRequestProperty("Accept", "application/json")
        }
        val code = conn.responseCode
        return try {
            if (code in 200..299) {
                conn.inputStream.bufferedReader().readText()
            } else {
                val err = conn.errorStream?.bufferedReader()?.readText() ?: ""
                throw Exception(
                    when (code) {
                        429  -> "Food database is busy right now — try again in a minute."
                        403  -> "Food database is unavailable right now."
                        else -> "Food lookup failed ($code). Please try again."
                    }
                )
            }
        } finally {
            conn.disconnect()
        }
    }
}

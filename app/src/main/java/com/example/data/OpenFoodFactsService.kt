package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Free barcode nutrition lookup via Open Food Facts.
 * No API key, no account, no rate limit for reasonable usage.
 * https://world.openfoodfacts.org/data
 */
object OpenFoodFactsService {

    private const val BASE = "https://world.openfoodfacts.org/api/v0/product"
    private const val TIMEOUT = 15_000

    suspend fun lookup(barcode: String): Result<FoodNutrition> = withContext(Dispatchers.IO) {
        try {
            val raw = httpGet("$BASE/$barcode.json")
            val root = JSONObject(raw)

            val status = root.optInt("status", 0)
            if (status != 1) {
                return@withContext Result.failure(
                    Exception("Product not found. Try searching by name instead.")
                )
            }

            val product = root.optJSONObject("product")
                ?: return@withContext Result.failure(Exception("No product data returned."))

            val nutriments = product.optJSONObject("nutriments")

            // Product name — try several fields in priority order
            val name = listOf(
                product.optString("product_name_en"),
                product.optString("product_name"),
                product.optString("abbreviated_product_name"),
                product.optString("generic_name")
            ).firstOrNull { it.isNotBlank() } ?: "Unknown product"

            val brand = product.optString("brands", "").split(",").firstOrNull()?.trim() ?: ""

            // Serving size string
            val servingSize = product.optString("serving_size", "100g").ifBlank { "100g" }

            // Prefer per-serving values; fall back to per-100g
            fun num(key: String): Double {
                val serving = nutriments?.optDouble("${key}_serving") ?: Double.NaN
                if (!serving.isNaN() && serving > 0) return serving
                return nutriments?.optDouble("${key}_100g") ?: 0.0
            }

            val calories = run {
                val kcal = num("energy-kcal")
                if (kcal > 0) kcal else num("energy") / 4.184  // kJ → kcal
            }.toInt()

            val protein = num("proteins").toFloat()
            val carbs   = num("carbohydrates").toFloat()
            val fat     = num("fat").toFloat()
            val fiber   = num("fiber").toFloat()

            val displayName = if (brand.isNotBlank()) "$name — $brand" else name

            Result.success(
                FoodNutrition(
                    foodName    = displayName,
                    servingSize = servingSize,
                    calories    = calories,
                    proteinG    = protein,
                    carbsG      = carbs,
                    fatG        = fat,
                    fiberG      = fiber,
                    confidence  = "high",
                    notes       = "Data from Open Food Facts (openfoodfacts.org)"
                )
            )
        } catch (e: Exception) {
            Result.failure(Exception("Barcode lookup failed: ${e.message}"))
        }
    }

    private fun httpGet(url: String): String {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod  = "GET"
            connectTimeout = TIMEOUT
            readTimeout    = TIMEOUT
            setRequestProperty("User-Agent", "CoachOps-Android/1.0 (contact@coachops.app)")
        }
        return try {
            conn.inputStream.bufferedReader().readText()
        } finally {
            conn.disconnect()
        }
    }
}

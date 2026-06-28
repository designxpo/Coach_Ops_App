package com.example.data

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class FoodSuggestion(
    val name: String,
    val brand: String = "",
    val emoji: String = "🍽️"
)

object NutritionixService {

    private const val BASE = "https://trackapi.nutritionix.com/v2"

    suspend fun searchFoods(query: String): List<FoodSuggestion> =
        withContext(Dispatchers.IO) {
            if (query.length < 2) return@withContext emptyList()
            try {
                val encoded = URLEncoder.encode(query.trim().take(200), "UTF-8")
                val json = JSONObject(httpGet("$BASE/search/instant?query=$encoded"))
                val results = mutableListOf<FoodSuggestion>()

                val common = json.optJSONArray("common")
                if (common != null) {
                    for (i in 0 until minOf(common.length(), 6)) {
                        val item = common.getJSONObject(i)
                        val name = item.optString("food_name", "").replaceFirstChar { it.uppercase() }
                        if (name.isNotBlank()) results.add(FoodSuggestion(name = name))
                    }
                }

                val branded = json.optJSONArray("branded")
                if (branded != null) {
                    for (i in 0 until minOf(branded.length(), 3)) {
                        val item = branded.getJSONObject(i)
                        val name  = item.optString("food_name", "").replaceFirstChar { it.uppercase() }
                        val brand = item.optString("brand_name", "")
                        if (name.isNotBlank()) results.add(FoodSuggestion(name = name, brand = brand))
                    }
                }

                results
            } catch (_: Exception) { emptyList() }
        }

    suspend fun getNutrition(query: String): Result<FoodNutrition> =
        withContext(Dispatchers.IO) {
            try {
                val body = JSONObject().put("query", query.take(200)).toString()
                val url  = "$BASE/natural/nutrients"
                val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("x-app-id",  BuildConfig.NUTRITIONIX_APP_ID)
                    setRequestProperty("x-app-key", BuildConfig.NUTRITIONIX_APP_KEY)
                    doOutput       = true
                    connectTimeout = 15_000
                    readTimeout    = 15_000
                }
                conn.outputStream.use { it.write(body.toByteArray()) }

                val code = conn.responseCode
                val raw  = if (code in 200..299) {
                    conn.inputStream.bufferedReader().readText()
                } else {
                    conn.disconnect()
                    return@withContext Result.failure(Exception(
                        when (code) {
                            401  -> "Invalid API key. Check Nutritionix credentials."
                            404  -> "Food not found. Try a different name."
                            429  -> "Too many requests. Please wait a moment."
                            else -> "Server error ($code). Try again."
                        }
                    ))
                }
                conn.disconnect()

                val foods = JSONObject(raw).getJSONArray("foods")
                if (foods.length() == 0)
                    return@withContext Result.failure(Exception("Food not found. Try a different search."))

                var calories = 0; var protein = 0f; var carbs = 0f; var fat = 0f; var fiber = 0f
                var servingDesc = ""; var displayName = ""

                for (i in 0 until foods.length()) {
                    val f = foods.getJSONObject(i)
                    calories += f.optDouble("nf_calories",           0.0).toInt()
                    protein  += f.optDouble("nf_protein",            0.0).toFloat()
                    carbs    += f.optDouble("nf_total_carbohydrate", 0.0).toFloat()
                    fat      += f.optDouble("nf_total_fat",          0.0).toFloat()
                    fiber    += f.optDouble("nf_dietary_fiber",      0.0).toFloat()
                    if (i == 0) {
                        val qty    = f.optDouble("serving_qty",          1.0)
                        val unit   = f.optString("serving_unit",         "serving")
                        val weight = f.optDouble("serving_weight_grams", 0.0)
                        servingDesc = if (weight > 0)
                            "%.0f %s (~%.0fg)".format(qty, unit, weight)
                        else
                            "%.0f %s".format(qty, unit)
                        displayName = f.optString("food_name", query).replaceFirstChar { it.uppercase() }
                    }
                }
                if (foods.length() > 1) displayName = query.replaceFirstChar { it.uppercase() }

                Result.success(FoodNutrition(
                    foodName    = displayName,
                    servingSize = servingDesc,
                    calories    = calories,
                    proteinG    = protein,
                    carbsG      = carbs,
                    fatG        = fat,
                    fiberG      = fiber,
                    confidence  = "high",
                    notes       = "Data sourced from Nutritionix database"
                ))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    private fun httpGet(url: String): String {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            setRequestProperty("x-app-id",  BuildConfig.NUTRITIONIX_APP_ID)
            setRequestProperty("x-app-key", BuildConfig.NUTRITIONIX_APP_KEY)
            connectTimeout = 8_000
            readTimeout    = 8_000
        }
        return try { conn.inputStream.bufferedReader().readText() }
        finally   { conn.disconnect() }
    }
}

package com.example.data

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class PlannedMeal(
    val name: String,       // e.g. "Oats with milk and banana"
    val timeSlot: String,   // e.g. "Breakfast (7–8 AM)"
    val calories: Int,
    val proteinG: Int,
    val carbsG: Int,
    val fatG: Int,
    val ingredients: List<String>  // for grocery list
)

data class PlannedDay(
    val dayLabel: String,   // "Monday", "Tuesday" etc.
    val meals: List<PlannedMeal>,
    val totalCalories: Int = meals.sumOf { it.calories }
)

data class WeeklyMealPlan(
    val goal: String,
    val days: List<PlannedDay>,
    val generatedAt: Long = System.currentTimeMillis()
)

object MealPlannerAI {
    private const val MODEL = "gemini-2.5-flash"

    suspend fun generate(
        goalCalories: Int,
        proteinG: Int,
        isVegetarian: Boolean,
        goal: String  // e.g. "weight loss", "muscle gain", "maintain"
    ): Result<WeeklyMealPlan> = withContext(Dispatchers.IO) {
        try {
            val prompt = """
                Create a 7-day Indian meal plan for someone with these goals:
                - Daily calories: $goalCalories kcal
                - Protein target: ${proteinG}g
                - Diet: ${if (isVegetarian) "Vegetarian" else "Non-vegetarian (include chicken, eggs, fish)"}
                - Goal: $goal

                Return ONLY valid JSON, no markdown. Format:
                {
                  "days": [
                    {
                      "dayLabel": "Monday",
                      "meals": [
                        {
                          "name": "Poha with peanuts",
                          "timeSlot": "Breakfast (7–8 AM)",
                          "calories": 350,
                          "proteinG": 12,
                          "carbsG": 55,
                          "fatG": 8,
                          "ingredients": ["poha 1 cup", "peanuts 2 tbsp", "onion 1", "green chilli 1", "mustard seeds", "curry leaves", "turmeric"]
                        }
                      ]
                    }
                  ]
                }

                Include 4-5 meals per day: Breakfast, Mid-morning snack, Lunch, Evening snack, Dinner.
                Use common Indian foods. Keep ingredients specific with quantities.
            """.trimIndent()

            val body = JSONObject().put("contents",
                JSONArray().put(JSONObject()
                    .put("role", "user")
                    .put("parts", JSONArray().put(JSONObject().put("text", prompt)))
                )
            ).toString()

            val url = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent?key=${BuildConfig.GEMINI_API_KEY}"
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
                connectTimeout = 30_000
                readTimeout = 120_000
            }
            conn.outputStream.use { it.write(body.toByteArray()) }
            val code = conn.responseCode
            val raw = if (code in 200..299) conn.inputStream.bufferedReader().readText()
                      else return@withContext Result.failure(Exception("Gemini error $code"))
            conn.disconnect()

            val text = JSONObject(raw)
                .getJSONArray("candidates").getJSONObject(0)
                .getJSONObject("content").getJSONArray("parts")
                .getJSONObject(0).getString("text")
                .trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()

            val json = JSONObject(text)
            val daysArr = json.getJSONArray("days")
            val days = (0 until daysArr.length()).map { d ->
                val dayObj = daysArr.getJSONObject(d)
                val mealsArr = dayObj.getJSONArray("meals")
                val meals = (0 until mealsArr.length()).map { m ->
                    val mealObj = mealsArr.getJSONObject(m)
                    val ingArr = mealObj.optJSONArray("ingredients")
                    val ingredients = if (ingArr != null) (0 until ingArr.length()).map { ingArr.getString(it) } else emptyList()
                    PlannedMeal(
                        name = mealObj.optString("name", ""),
                        timeSlot = mealObj.optString("timeSlot", ""),
                        calories = mealObj.optInt("calories", 0),
                        proteinG = mealObj.optInt("proteinG", 0),
                        carbsG = mealObj.optInt("carbsG", 0),
                        fatG = mealObj.optInt("fatG", 0),
                        ingredients = ingredients
                    )
                }
                PlannedDay(dayLabel = dayObj.optString("dayLabel", "Day ${d+1}"), meals = meals)
            }
            Result.success(WeeklyMealPlan(goal = goal, days = days))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

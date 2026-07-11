package com.example.data

import android.graphics.Bitmap
import android.util.Base64
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Photo → dish + nutrition via Gemini vision. Fallback for meals the on-device
 * classifier can't name (mixed Indian plates especially). Uses the same
 * free-tier key as the AI Meal Planner; one small JPEG per call.
 */
object GeminiFoodVision {

    private const val MODEL = "gemini-2.5-flash"
    private const val MAX_SIDE = 640

    suspend fun analyze(bitmap: Bitmap, hint: String = ""): Result<FoodNutrition> =
        withContext(Dispatchers.IO) {
            try {
                if (BuildConfig.GEMINI_API_KEY.isBlank())
                    return@withContext Result.failure(Exception("AI analysis unavailable"))

                val prompt = """
                    Identify the food in this photo (likely an Indian meal) and estimate the
                    nutrition for the PORTION VISIBLE in the photo (not per 100g).
                    ${if (hint.isNotBlank()) "An on-device classifier guessed: $hint (may be wrong)." else ""}
                    Respond with ONLY this JSON, no other text:
                    {"food_name":"dish name","serving_label":"e.g. 1 plate (~350g)","calories":420,
                     "protein_g":14.0,"carbs_g":62.0,"fat_g":12.0,"fiber_g":6.0,
                     "confidence":"high|medium|low"}
                    If the photo clearly contains no food, use food_name "NOT_FOOD".
                """.trimIndent()

                val body = JSONObject()
                    .put("contents", JSONArray().put(JSONObject()
                        .put("role", "user")
                        .put("parts", JSONArray()
                            .put(JSONObject().put("text", prompt))
                            .put(JSONObject().put("inline_data", JSONObject()
                                .put("mime_type", "image/jpeg")
                                .put("data", toJpegBase64(bitmap))))
                        )
                    ))
                    .put("generationConfig", JSONObject().put("response_mime_type", "application/json"))
                    .toString()

                val url = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent?key=${BuildConfig.GEMINI_API_KEY}"
                val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    doOutput = true
                    connectTimeout = 15_000
                    readTimeout = 60_000
                }
                conn.outputStream.use { it.write(body.toByteArray()) }
                val code = conn.responseCode
                if (code !in 200..299) {
                    conn.disconnect()
                    return@withContext Result.failure(Exception("AI analysis failed ($code)"))
                }
                val raw = conn.inputStream.bufferedReader().readText()
                conn.disconnect()

                val text = JSONObject(raw)
                    .getJSONArray("candidates").getJSONObject(0)
                    .getJSONObject("content").getJSONArray("parts")
                    .getJSONObject(0).getString("text")
                    .trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()

                val json = JSONObject(text)
                val name = json.optString("food_name", "")
                if (name.isBlank() || name == "NOT_FOOD")
                    return@withContext Result.failure(Exception("No food recognised in the photo."))

                Result.success(FoodNutrition(
                    foodName    = name,
                    servingSize = json.optString("serving_label", "1 serving"),
                    calories    = json.optInt("calories", 0),
                    proteinG    = json.optDouble("protein_g", 0.0).toFloat(),
                    carbsG      = json.optDouble("carbs_g", 0.0).toFloat(),
                    fatG        = json.optDouble("fat_g", 0.0).toFloat(),
                    fiberG      = json.optDouble("fiber_g", 0.0).toFloat(),
                    confidence  = json.optString("confidence", "medium"),
                    notes       = "AI photo analysis · portion estimated from image"
                ))
            } catch (e: Exception) {
                Result.failure(Exception("AI analysis failed: ${e.message?.take(80) ?: "network error"}"))
            }
        }

    private fun toJpegBase64(bitmap: Bitmap): String {
        val scale = MAX_SIDE.toFloat() / maxOf(bitmap.width, bitmap.height)
        val scaled = if (scale < 1f)
            Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true)
        else bitmap
        val out = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 80, out)
        return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }
}

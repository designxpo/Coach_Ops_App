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

data class FoodNutrition(
    val foodName:   String,
    val servingSize: String,
    val calories:   Int,
    val proteinG:   Float,
    val carbsG:     Float,
    val fatG:       Float,
    val fiberG:     Float,
    val confidence: String,   // "high" | "medium" | "low"
    val notes:      String
)

object GeminiNutrition {

    private const val MODEL = "gemini-2.5-flash"
    private const val MAX_DIM = 768   // resize before sending — saves tokens

    suspend fun analyze(bitmap: Bitmap): Result<FoodNutrition> =
        withContext(Dispatchers.IO) {
            try {
                val base64 = bitmapToBase64(bitmap)

                val prompt = """
                    Analyze this food image and return ONLY a valid JSON object — no markdown, no extra text.
                    Use realistic nutritional values. For Indian food use Indian portion sizes.
                    {
                      "foodName":   "name of the food",
                      "servingSize": "e.g. 1 plate ~300g",
                      "calories":   350,
                      "proteinG":   18.0,
                      "carbsG":     42.0,
                      "fatG":       10.0,
                      "fiberG":     4.0,
                      "confidence": "high",
                      "notes":      "brief useful note"
                    }
                    Rules:
                    - confidence must be "high", "medium", or "low"
                    - If no food is visible set confidence "low" and foodName "Not identified"
                    - All numeric values must be numbers, not strings
                """.trimIndent()

                val body = JSONObject().apply {
                    put("contents", JSONArray().apply {
                        put(JSONObject().apply {
                            put("parts", JSONArray().apply {
                                put(JSONObject().apply {
                                    put("inlineData", JSONObject().apply {
                                        put("mimeType", "image/jpeg")
                                        put("data", base64)
                                    })
                                })
                                put(JSONObject().apply { put("text", prompt) })
                            })
                        })
                    })
                    put("generationConfig", JSONObject().apply {
                        put("temperature", 0.1)
                        put("maxOutputTokens", 512)
                    })
                }

                val url  = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent"
                val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("X-goog-api-key", BuildConfig.GEMINI_API_KEY)
                    doOutput      = true
                    connectTimeout = 30_000
                    readTimeout    = 30_000
                }
                conn.outputStream.use { it.write(body.toString().toByteArray()) }
                val statusCode = conn.responseCode
                val raw = if (statusCode in 200..299) {
                    conn.inputStream.bufferedReader().readText()
                } else {
                    val errBody = conn.errorStream?.bufferedReader()?.readText() ?: ""
                    conn.disconnect()
                    val errMsg = try {
                        JSONObject(errBody).getJSONObject("error").optString("message", "")
                    } catch (_: Exception) { "" }
                    return@withContext Result.failure(Exception(
                        when (statusCode) {
                            400  -> "Invalid request. Try a clearer food photo."
                            403  -> "Gemini API not enabled. Enable 'Generative Language API' in Google Cloud Console."
                            429  -> "Too many requests. Please wait a moment and try again."
                            else -> if (errMsg.isNotBlank()) errMsg else "Server error ($statusCode). Try again."
                        }
                    ))
                }
                conn.disconnect()

                // Extract the text part from Gemini response
                val text = JSONObject(raw)
                    .getJSONArray("candidates").getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts").getJSONObject(0)
                    .getString("text")
                    .trim()
                    .removePrefix("```json").removePrefix("```")
                    .removeSuffix("```").trim()

                val j = JSONObject(text)
                val rawConfidence = j.optString("confidence", "medium")
                val safeConfidence = if (rawConfidence in setOf("high", "medium", "low")) rawConfidence else "medium"
                Result.success(FoodNutrition(
                    foodName    = j.optString("foodName",    "Unknown Food"),
                    servingSize = j.optString("servingSize", "1 serving"),
                    calories    = j.optInt("calories",       0),
                    proteinG    = j.optDouble("proteinG",    0.0).toFloat(),
                    carbsG      = j.optDouble("carbsG",      0.0).toFloat(),
                    fatG        = j.optDouble("fatG",        0.0).toFloat(),
                    fiberG      = j.optDouble("fiberG",      0.0).toFloat(),
                    confidence  = safeConfidence,
                    notes       = j.optString("notes", "").take(200)
                ))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    private fun bitmapToBase64(src: Bitmap): String {
        // Scale down to MAX_DIM on the longest side to keep token usage low
        val scaled = if (src.width > MAX_DIM || src.height > MAX_DIM) {
            val ratio  = MAX_DIM.toFloat() / maxOf(src.width, src.height)
            val w = (src.width  * ratio).toInt()
            val h = (src.height * ratio).toInt()
            Bitmap.createScaledBitmap(src, w, h, true)
        } else src
        val out = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 85, out)
        return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }
}

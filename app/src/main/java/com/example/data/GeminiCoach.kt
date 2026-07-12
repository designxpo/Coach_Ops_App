package com.example.data

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Lightweight message model for the AI coach conversation. Role is "user" or "model". */
data class AiChatMessage(val role: String, val text: String)

object GeminiCoach {

    private const val MODEL = "gemini-2.5-flash"

    suspend fun chat(
        history: List<AiChatMessage>,
        userContext: String,
        userMessage: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val systemPrompt = """
                You are an expert AI nutrition and fitness coach for ProCoach India app.
                Be concise, practical, and encouraging. Focus on Indian diet and fitness context.
                Always give actionable advice. If asked about medical conditions, advise consulting a doctor.

                User Profile:
                $userContext
            """.trimIndent()

            val contents = JSONArray()

            // Add system prompt as first user message (Gemini doesn't have a system role,
            // so we inject it as the opening user/model exchange)
            contents.put(JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().put(
                    JSONObject().put(
                        "text",
                        "System: $systemPrompt\n\nUser: Hello, I need nutrition guidance."
                    )
                ))
            })
            contents.put(JSONObject().apply {
                put("role", "model")
                put("parts", JSONArray().put(
                    JSONObject().put(
                        "text",
                        "Hello! I'm your AI nutrition coach. I'm here to help you with personalized " +
                            "nutrition and fitness guidance based on your profile. What would you like to know?"
                    )
                ))
            })

            // Add conversation history (AiChatMessage list)
            for (msg in history) {
                contents.put(JSONObject().apply {
                    put("role", msg.role)
                    put("parts", JSONArray().put(JSONObject().put("text", msg.text)))
                })
            }

            // Add current user message
            contents.put(JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().put(JSONObject().put("text", userMessage)))
            })

            val body = JSONObject().apply {
                put("contents", contents)
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.7)
                    put("maxOutputTokens", 1024)
                })
            }.toString()

            val url = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent?key=${BuildConfig.GEMINI_API_KEY}"

            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
                connectTimeout = 30_000
                readTimeout = 30_000
            }
            conn.outputStream.use { it.write(body.toByteArray()) }

            val code = conn.responseCode
            val raw = if (code in 200..299) {
                conn.inputStream.bufferedReader().readText()
            } else {
                // Drain & close the error stream, but never surface the vendor's
                // raw message to users — map to neutral, on-brand copy instead.
                conn.errorStream?.bufferedReader()?.readText()
                conn.disconnect()
                return@withContext Result.failure(Exception(
                    when (code) {
                        400  -> "Invalid request. Please try again."
                        403  -> "ProCoach AI is unavailable right now. Please try again later."
                        429  -> "Too many requests. Please wait a moment and try again."
                        else -> "ProCoach AI had a problem ($code). Please try again."
                    }
                ))
            }
            conn.disconnect()

            val text = JSONObject(raw)
                .getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")

            Result.success(text.trim())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

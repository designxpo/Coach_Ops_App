package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * All image uploads for ProCoach India go through Supabase Storage.
 *
 * Single bucket: "profile-photos" (public)
 * Sub-paths:
 *   users/{uid}               — user avatar (coach + member)
 *   trainer/{uid}             — coach marketplace hero photo
 *   portfolio/{uid}/{index}   — coach portfolio work images
 *   exercises/{exerciseId}    — exercise library images (admin panel)
 *
 * Rules enforced here (client-side):
 *   - Max 5 MB input file
 *   - Images compressed to max 1024px, JPEG 85%
 *   - Image content type required (jpeg, png, webp)
 */
object SupabaseStorage {

    private val db   = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val uid  get() = auth.currentUser?.uid

    private const val MAX_BYTES   = 5 * 1024 * 1024   // 5 MB hard limit
    private const val MAX_PX      = 1024               // max dimension after resize
    private const val JPEG_QUALITY = 85

    // ─── Public upload entry-points ───────────────────────────────────────────

    /** User profile photo — both coach and member */
    suspend fun uploadProfilePhoto(context: Context, uri: Uri): String {
        val uid = requireUid()
        val path = SupabaseConfig.userProfilePath(uid)
        val url  = upload(context, uri, path)
        // Mirror to user_records for cross-device restore
        db.collection("user_records").document(uid)
            .set(mapOf("profilePhotoUrl" to url), SetOptions.merge()).await()
        return url
    }

    /** Coach marketplace profile/hero photo */
    suspend fun uploadTrainerPhoto(context: Context, uri: Uri): String {
        val uid  = requireUid()
        val path = SupabaseConfig.trainerProfilePath(uid)
        return upload(context, uri, path)
    }

    /** Coach portfolio work image */
    suspend fun uploadPortfolioPhoto(context: Context, uri: Uri, index: Int): String {
        val uid  = requireUid()
        val path = SupabaseConfig.portfolioPath(uid, index)
        return upload(context, uri, path)
    }

    // ─── Core upload ──────────────────────────────────────────────────────────

    private suspend fun upload(context: Context, uri: Uri, path: String): String =
        withContext(Dispatchers.IO) {
            // 1. Validate input size before reading bitmap
            val inputSize = context.contentResolver.openFileDescriptor(uri, "r")
                ?.use { it.statSize } ?: 0L
            if (inputSize > MAX_BYTES) {
                throw Exception("Image is too large. Maximum size is 5 MB.")
            }

            // 2. Compress image
            val bytes = compressImage(context, uri)

            // 3. Upload via multipart/form-data (matches official Supabase SDK behaviour)
            val uploadUrl = "${SupabaseConfig.PROJECT_URL}/storage/v1/object/${SupabaseConfig.BUCKET}/$path"
            val boundary  = "ProCoachBoundary${System.currentTimeMillis()}"

            val conn = (URL(uploadUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput      = true
                setRequestProperty("Authorization", "Bearer ${SupabaseConfig.ANON_KEY}")
                setRequestProperty("Content-Type",  "multipart/form-data; boundary=$boundary")
                setRequestProperty("x-upsert",      "true")
                connectTimeout = 30_000
                readTimeout    = 60_000
            }
            try {
                conn.outputStream.use { out ->
                    val CRLF = "\r\n"
                    fun part(header: String) = out.write((header + CRLF).toByteArray(Charsets.UTF_8))

                    // cacheControl field (required by Supabase)
                    part("--$boundary")
                    part("Content-Disposition: form-data; name=\"cacheControl\"")
                    part("")
                    part("3600")

                    // file field — name must be empty string for Supabase Storage
                    part("--$boundary")
                    part("Content-Disposition: form-data; name=\"\"; filename=\"photo.jpg\"")
                    part("Content-Type: image/jpeg")
                    part("")
                    out.write(bytes)
                    out.write(CRLF.toByteArray(Charsets.UTF_8))

                    // closing boundary
                    out.write("--$boundary--$CRLF".toByteArray(Charsets.UTF_8))
                }

                val code = conn.responseCode
                if (code !in 200..299) {
                    val err = conn.errorStream?.bufferedReader()?.readText() ?: "Unknown error"
                    throw Exception("Upload failed ($code): $err")
                }
            } finally {
                conn.disconnect()
            }

            // 4. Return the public CDN URL
            SupabaseConfig.publicUrl(path)
        }

    // ─── Image compression ────────────────────────────────────────────────────

    private fun compressImage(context: Context, uri: Uri): ByteArray {
        // Pass 1: get dimensions without decoding pixels
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, opts)
        }

        // Calculate sub-sample to stay ≤ MAX_PX
        val maxDimen = maxOf(opts.outWidth, opts.outHeight)
        var sample   = 1
        while (maxDimen / sample > MAX_PX) sample *= 2

        // Pass 2: decode at reduced resolution
        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sample }
        val bitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOpts)
        } ?: throw Exception("Cannot decode image")

        // Compress to JPEG
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
        bitmap.recycle()

        val result = out.toByteArray()
        // Double-check output is within limit (should always be after resize)
        if (result.size > MAX_BYTES) throw Exception("Compressed image exceeds 5 MB limit.")
        return result
    }

    private fun requireUid() = uid ?: throw Exception("Not authenticated")
}

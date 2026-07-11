package com.example.data

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * User data export — the "right to access / data portability" side of India's
 * DPDP Act 2023 and GDPR/CCPA. Complements AccountDeletion (right to erasure).
 *
 * Gathers everything ProCoach holds about the signed-in user across Firestore,
 * writes a single human-readable JSON file to cache, and hands it to a share
 * sheet so the user can save it to Drive / Files / email. No server needed.
 */
object DataExport {

    private val db get() = FirebaseFirestore.getInstance()

    private val COACH_SUBCOLLECTIONS = listOf(
        "clients", "programs", "payments", "workout_logs", "measurements",
        "notes", "revenue_snapshots",
        "gym_members", "gym_plans", "gym_payments", "gym_checkins",
    )
    private val USER_SUBCOLLECTIONS = listOf(
        "health_daily", "health_measurements", "health_photos", "health_cycle", "awards",
    )
    private val FITNESS_SUBCOLLECTIONS = listOf("goals", "logs", "food_diary")

    /**
     * Builds the export JSON and returns the file, or a failure with a
     * user-safe message. Runs off the main thread (caller uses a coroutine).
     */
    suspend fun buildExport(context: Context): Result<File> {
        val user = FirebaseAuth.getInstance().currentUser
            ?: return Result.failure(Exception("Please sign in to export your data"))
        val uid = user.uid

        return try {
            val root = JSONObject()
            root.put("export_generated_at", isoNow())
            root.put("app", "ProCoach India")
            root.put("account", JSONObject()
                .put("uid", uid)
                .put("email", user.email ?: "")
                .put("displayName", user.displayName ?: ""))

            // Top-level profile docs
            root.put("user_record", docToJson(safeGet("user_records", uid)))
            root.put("coach_profile", docToJson(safeGet("coaches", uid)))
            root.put("trainer_marketplace_profile", docToJson(safeGet("trainers", uid)))
            root.put("client_fitness", docToJson(safeGet("client_fitness", uid)))
            root.put("member_health_summary", docToJson(safeGet("member_health", uid)))

            // Sub-collections
            root.put("coach_data", subcollections("coaches", uid, COACH_SUBCOLLECTIONS))
            root.put("health_data", subcollections("users", uid, USER_SUBCOLLECTIONS))
            root.put("fitness_data", subcollections("client_fitness", uid, FITNESS_SUBCOLLECTIONS))
            root.put("health_records", collectionUnder("member_health", uid, "records"))

            // Cross-user records where the user is a party (their side only)
            root.put("bookings_as_client", queryToJson("bookings", "clientId", uid))
            root.put("bookings_as_coach", queryToJson("bookings", "coachId", uid))
            root.put("reviews_written", queryGroupToJson("reviews", "clientId", uid))

            val dir = File(context.cacheDir, "exports").apply { mkdirs() }
            val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
            val file = File(dir, "procoach-data-$stamp.json")
            file.writeText(root.toString(2))
            Result.success(file)
        } catch (e: Exception) {
            Result.failure(Exception("Couldn't build your export — check your connection and try again."))
        }
    }

    /** Builds the export and launches a share sheet. */
    suspend fun exportAndShare(context: Context): Result<Unit> {
        val result = buildExport(context)
        val file = result.getOrElse { return Result.failure(it) }
        return try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            val share = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "My ProCoach data export")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(share, "Save or share your data")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Export ready but couldn't open the share sheet."))
        }
    }

    // ── helpers ────────────────────────────────────────────────────────────

    private suspend fun safeGet(root: String, uid: String): DocumentSnapshot? =
        runCatching { db.collection(root).document(uid).get().await() }.getOrNull()

    private fun docToJson(doc: DocumentSnapshot?): Any =
        if (doc?.exists() == true) mapToJson(doc.data ?: emptyMap()) else JSONObject.NULL

    private suspend fun subcollections(root: String, uid: String, names: List<String>): JSONObject {
        val out = JSONObject()
        for (name in names) out.put(name, collectionUnder(root, uid, name))
        return out
    }

    private suspend fun collectionUnder(root: String, uid: String, name: String): JSONArray {
        val arr = JSONArray()
        runCatching {
            db.collection(root).document(uid).collection(name).limit(2000).get().await()
                .documents.forEach { arr.put(mapToJson(it.data ?: emptyMap())) }
        }
        return arr
    }

    private suspend fun queryToJson(collection: String, field: String, value: String): JSONArray {
        val arr = JSONArray()
        runCatching {
            db.collection(collection).whereEqualTo(field, value).limit(2000).get().await()
                .documents.forEach { arr.put(mapToJson(it.data ?: emptyMap())) }
        }
        return arr
    }

    private suspend fun queryGroupToJson(group: String, field: String, value: String): JSONArray {
        val arr = JSONArray()
        runCatching {
            db.collectionGroup(group).whereEqualTo(field, value).limit(2000).get().await()
                .documents.forEach { arr.put(mapToJson(it.data ?: emptyMap())) }
        }
        return arr
    }

    @Suppress("UNCHECKED_CAST")
    private fun mapToJson(map: Map<String, Any?>): JSONObject {
        val obj = JSONObject()
        for ((k, v) in map) obj.put(k, valueToJson(v))
        return obj
    }

    @Suppress("UNCHECKED_CAST")
    private fun valueToJson(v: Any?): Any = when (v) {
        null -> JSONObject.NULL
        is Map<*, *> -> mapToJson(v as Map<String, Any?>)
        is List<*> -> JSONArray().also { arr -> v.forEach { arr.put(valueToJson(it)) } }
        is com.google.firebase.Timestamp -> v.toDate().toString()
        else -> v
    }

    private fun isoNow(): String =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US).format(Date())
}

package com.example.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ── Health data classes ───────────────────────────────────────────────────────

data class DailyHealthLog(
    val date: String = "",
    val stepsCount: Int = 0,
    val waterGlasses: Int = 0,
    val sleepHours: Float = 0f,
    val moodRating: Int = 0,
    val caloriesBurned: Int = 0
)

data class UserBodyStats(
    val id: String = "",
    val date: String = "",
    val weightKg: Float = 0f,
    val chestCm: Float = 0f,
    val waistCm: Float = 0f,
    val hipsCm: Float = 0f,
    val armsCm: Float = 0f,
    val thighsCm: Float = 0f,
    val notes: String = ""
)

data class ProgressPhoto(
    val id: String = "",
    val date: String = "",
    val localPath: String = "",
    val notes: String = ""
)

data class CycleEntry(
    val id: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val notes: String = ""
)

// ── Repository ────────────────────────────────────────────────────────────────

class HealthRepository(private val uid: String) {

    private val db = FirebaseFirestore.getInstance()
    private fun daily()   = db.collection("users").document(uid).collection("health_daily")
    private fun measure() = db.collection("users").document(uid).collection("health_measurements")
    private fun photos()  = db.collection("users").document(uid).collection("health_photos")
    private fun cycle()   = db.collection("users").document(uid).collection("health_cycle")

    fun todayKey(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    // ── Daily log ─────────────────────────────────────────────────────────────

    suspend fun getLog(date: String): DailyHealthLog {
        return try {
            val doc = daily().document(date).get().await()
            if (doc.exists()) DailyHealthLog(
                date           = date,
                stepsCount     = doc.getLong("stepsCount")?.toInt()     ?: 0,
                waterGlasses   = doc.getLong("waterGlasses")?.toInt()   ?: 0,
                sleepHours     = doc.getDouble("sleepHours")?.toFloat() ?: 0f,
                moodRating     = doc.getLong("moodRating")?.toInt()     ?: 0,
                caloriesBurned = doc.getLong("caloriesBurned")?.toInt() ?: 0
            ) else DailyHealthLog(date = date)
        } catch (_: Exception) { DailyHealthLog(date = date) }
    }

    suspend fun saveLog(log: DailyHealthLog) {
        // Guarded: a denied/failed cloud write must never crash the app.
        // MERGE (not full set): the daily doc has multiple concurrent writers —
        // the background step service and the in-app screens — so a full replace
        // let one clobber the other's fields (a banner water tap was wiped by the
        // next step save). Field-scoped helpers below keep writers from touching
        // fields they don't own; saveLog merges the fields a full-form save sets.
        if (uid.isEmpty() || log.date.isEmpty()) return
        try {
            daily().document(log.date).set(mapOf(
                "stepsCount"     to log.stepsCount,
                "waterGlasses"   to log.waterGlasses,
                "sleepHours"     to log.sleepHours,
                "moodRating"     to log.moodRating,
                "caloriesBurned" to log.caloriesBurned
            ), com.google.firebase.firestore.SetOptions.merge()).await()
        } catch (_: Exception) { }
    }

    /** Step service / step collector — writes ONLY steps + calories. */
    suspend fun saveSteps(date: String, steps: Int, caloriesBurned: Int) {
        if (uid.isEmpty() || date.isEmpty()) return
        try {
            daily().document(date).set(
                mapOf("stepsCount" to steps, "caloriesBurned" to caloriesBurned),
                com.google.firebase.firestore.SetOptions.merge()
            ).await()
        } catch (_: Exception) { }
    }

    /** Water tap (app or notification banner) — writes ONLY the water count. */
    suspend fun saveWater(date: String, glasses: Int) {
        if (uid.isEmpty() || date.isEmpty()) return
        try {
            daily().document(date).set(
                mapOf("waterGlasses" to glasses),
                com.google.firebase.firestore.SetOptions.merge()
            ).await()
        } catch (_: Exception) { }
    }

    suspend fun getLast7Days(): List<DailyHealthLog> = getLastNDays(7)

    /** Today back through the last [n] days (newest first). Used by Progress analytics. */
    suspend fun getLastNDays(n: Int): List<DailyHealthLog> {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return (0 until n).map { daysAgo ->
            val cal = java.util.Calendar.getInstance()
            cal.add(java.util.Calendar.DAY_OF_YEAR, -daysAgo)
            getLog(fmt.format(cal.time))
        }
    }

    // ── Body measurements ─────────────────────────────────────────────────────

    suspend fun getMeasurements(): List<UserBodyStats> {
        return try {
            measure().orderBy("date", Query.Direction.DESCENDING).limit(30)
                .get().await().documents.map { doc ->
                    UserBodyStats(
                        id       = doc.id,
                        date     = doc.getString("date")                 ?: "",
                        weightKg = doc.getDouble("weightKg")?.toFloat()  ?: 0f,
                        chestCm  = doc.getDouble("chestCm")?.toFloat()   ?: 0f,
                        waistCm  = doc.getDouble("waistCm")?.toFloat()   ?: 0f,
                        hipsCm   = doc.getDouble("hipsCm")?.toFloat()    ?: 0f,
                        armsCm   = doc.getDouble("armsCm")?.toFloat()    ?: 0f,
                        thighsCm = doc.getDouble("thighsCm")?.toFloat()  ?: 0f,
                        notes    = doc.getString("notes")                ?: ""
                    )
                }
        } catch (_: Exception) { emptyList() }
    }

    suspend fun saveMeasurement(m: UserBodyStats) {
        try {
            val ref = if (m.id.isEmpty()) measure().document() else measure().document(m.id)
            ref.set(mapOf(
                "date"     to m.date,     "weightKg" to m.weightKg,
                "chestCm"  to m.chestCm,  "waistCm"  to m.waistCm,
                "hipsCm"   to m.hipsCm,   "armsCm"   to m.armsCm,
                "thighsCm" to m.thighsCm, "notes"    to m.notes
            )).await()
        } catch (_: Exception) { }
    }

    suspend fun deleteMeasurement(id: String) {
        try { measure().document(id).delete().await() } catch (_: Exception) { }
    }

    // ── Progress photos ───────────────────────────────────────────────────────

    suspend fun getProgressPhotos(): List<ProgressPhoto> {
        return try {
            photos().orderBy("date", Query.Direction.DESCENDING).limit(50)
                .get().await().documents.map { doc ->
                    ProgressPhoto(
                        id        = doc.id,
                        date      = doc.getString("date")      ?: "",
                        localPath = doc.getString("localPath") ?: "",
                        notes     = doc.getString("notes")     ?: ""
                    )
                }
        } catch (_: Exception) { emptyList() }
    }

    suspend fun saveProgressPhoto(photo: ProgressPhoto) {
        try {
            photos().document().set(mapOf(
                "date" to photo.date, "localPath" to photo.localPath, "notes" to photo.notes
            )).await()
        } catch (_: Exception) { }
    }

    suspend fun deleteProgressPhoto(id: String) {
        try { photos().document(id).delete().await() } catch (_: Exception) { }
    }

    // ── Cycle tracker ─────────────────────────────────────────────────────────

    suspend fun getCycleEntries(): List<CycleEntry> {
        return try {
            cycle().orderBy("startDate", Query.Direction.DESCENDING).limit(12)
                .get().await().documents.map { doc ->
                    CycleEntry(
                        id        = doc.id,
                        startDate = doc.getString("startDate") ?: "",
                        endDate   = doc.getString("endDate")   ?: "",
                        notes     = doc.getString("notes")     ?: ""
                    )
                }
        } catch (_: Exception) { emptyList() }
    }

    suspend fun saveCycleEntry(entry: CycleEntry) {
        try {
            val ref = if (entry.id.isEmpty()) cycle().document() else cycle().document(entry.id)
            ref.set(mapOf("startDate" to entry.startDate, "endDate" to entry.endDate, "notes" to entry.notes)).await()
        } catch (_: Exception) { }
    }

    suspend fun deleteCycleEntry(id: String) {
        try { cycle().document(id).delete().await() } catch (_: Exception) { }
    }
}

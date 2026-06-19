package com.example.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

/**
 * Firestore layer for member health records.
 *
 * Collection: member_health/{memberId}/records/{recordId}
 *
 * Design decisions:
 * - Member-owned collection — data lives with the member, not the coach.
 * - Any coach with a booking can read a member's records (enforced at UI layer;
 *   Firestore rules allow any authenticated read).
 * - Latest record summary is mirrored to user_records/{memberId} so the admin
 *   panel and coach dashboard get the snapshot without querying sub-collections.
 */
object HealthSync {

    private val db  = FirebaseFirestore.getInstance()
    private val uid get() = FirebaseAuth.getInstance().currentUser?.uid

    // ─── Save ─────────────────────────────────────────────────────────────────

    suspend fun saveRecord(record: HealthRecord) {
        val u = uid ?: return
        val data = recordToMap(record)
        // Write to the member's health history
        db.collection("member_health").document(u)
            .collection("records").document(record.id)
            .set(data).await()
        // Mirror latest snapshot to user_records so coaches/admin can see it
        db.collection("user_records").document(u)
            .set(mapOf(
                "latestBmi"          to record.bmi,
                "latestBodyFat"      to record.bodyFatPercent,
                "latestWeightKg"     to record.weightKg,
                "latestLeanMassKg"   to record.leanMassKg,
                "latestFatMassKg"    to record.fatMassKg,
                "latestDailyProteinG" to record.dailyProteinG,
                "latestTdeeKcal"     to record.tdeeKcal,
                "healthUpdatedAt"    to record.recordedAt
            ), SetOptions.merge()).await()
    }

    // ─── Real-time listener ───────────────────────────────────────────────────

    fun listenMyRecords(uid: String, limit: Int = 20, onRecords: (List<HealthRecord>) -> Unit): ListenerRegistration =
        db.collection("member_health").document(uid)
            .collection("records")
            .orderBy("recordedAt", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .addSnapshotListener { snap, err ->
                if (err != null) return@addSnapshotListener
                onRecords(snap?.documents?.mapNotNull { mapToRecord(it.id, it.data) } ?: emptyList())
            }

    // ─── Fetch (member's own records) ─────────────────────────────────────────

    suspend fun getMyRecords(limit: Int = 20): List<HealthRecord> {
        val u = uid ?: return emptyList()
        return fetchRecords(u, limit)
    }

    /** Coach fetches a specific member's history using the member's UID */
    suspend fun getMemberRecords(memberId: String, limit: Int = 20): List<HealthRecord> =
        fetchRecords(memberId, limit)

    private suspend fun fetchRecords(memberId: String, limit: Int): List<HealthRecord> =
        try {
            db.collection("member_health").document(memberId)
                .collection("records")
                .orderBy("recordedAt", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get().await()
                .documents.mapNotNull { mapToRecord(it.id, it.data) }
        } catch (_: Exception) { emptyList() }

    // ─── Serialisation helpers ────────────────────────────────────────────────

    private fun recordToMap(r: HealthRecord): Map<String, Any> = mapOf(
        "id"              to r.id,
        "recordedAt"      to r.recordedAt,
        "weightKg"        to r.weightKg.toDouble(),
        "heightCm"        to r.heightCm.toDouble(),
        "ageYears"        to r.ageYears,
        "gender"          to r.gender,
        "activityLevel"   to r.activityLevel,
        "goal"            to r.goal,
        "waistCm"         to r.waistCm.toDouble(),
        "neckCm"          to r.neckCm.toDouble(),
        "hipCm"           to r.hipCm.toDouble(),
        "bmi"             to r.bmi.toDouble(),
        "bmiCategory"     to r.bmiCategory,
        "bodyFatPercent"  to r.bodyFatPercent.toDouble(),
        "bodyFatCategory" to r.bodyFatCategory,
        "bodyFatMethod"   to r.bodyFatMethod,
        "leanMassKg"      to r.leanMassKg.toDouble(),
        "fatMassKg"       to r.fatMassKg.toDouble(),
        "bmrKcal"         to r.bmrKcal,
        "tdeeKcal"        to r.tdeeKcal,
        "dailyProteinG"   to r.dailyProteinG,
        "dailyCarbsG"     to r.dailyCarbsG,
        "dailyFatG"       to r.dailyFatG,
        "dailyWaterL"     to r.dailyWaterL.toDouble(),
        "notes"           to r.notes
    )

    private fun mapToRecord(docId: String, d: Map<String, Any>?): HealthRecord? {
        if (d == null) return null
        return try {
            HealthRecord(
                id              = d["id"] as? String ?: docId,
                recordedAt      = d["recordedAt"] as? Long ?: 0L,
                weightKg        = (d["weightKg"] as? Double)?.toFloat() ?: 0f,
                heightCm        = (d["heightCm"] as? Double)?.toFloat() ?: 0f,
                ageYears        = (d["ageYears"] as? Long)?.toInt() ?: 0,
                gender          = d["gender"] as? String ?: "",
                activityLevel   = d["activityLevel"] as? String ?: "",
                goal            = d["goal"] as? String ?: "",
                waistCm         = (d["waistCm"] as? Double)?.toFloat() ?: 0f,
                neckCm          = (d["neckCm"] as? Double)?.toFloat() ?: 0f,
                hipCm           = (d["hipCm"] as? Double)?.toFloat() ?: 0f,
                bmi             = (d["bmi"] as? Double)?.toFloat() ?: 0f,
                bmiCategory     = d["bmiCategory"] as? String ?: "",
                bodyFatPercent  = (d["bodyFatPercent"] as? Double)?.toFloat() ?: 0f,
                bodyFatCategory = d["bodyFatCategory"] as? String ?: "",
                bodyFatMethod   = d["bodyFatMethod"] as? String ?: "",
                leanMassKg      = (d["leanMassKg"] as? Double)?.toFloat() ?: 0f,
                fatMassKg       = (d["fatMassKg"] as? Double)?.toFloat() ?: 0f,
                bmrKcal         = (d["bmrKcal"] as? Long)?.toInt() ?: 0,
                tdeeKcal        = (d["tdeeKcal"] as? Long)?.toInt() ?: 0,
                dailyProteinG   = (d["dailyProteinG"] as? Long)?.toInt() ?: 0,
                dailyCarbsG     = (d["dailyCarbsG"] as? Long)?.toInt() ?: 0,
                dailyFatG       = (d["dailyFatG"] as? Long)?.toInt() ?: 0,
                dailyWaterL     = (d["dailyWaterL"] as? Double)?.toFloat() ?: 0f,
                notes           = d["notes"] as? String ?: ""
            )
        } catch (_: Exception) { null }
    }
}

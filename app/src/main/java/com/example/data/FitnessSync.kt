package com.example.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

object FitnessSync {

    private val db  = FirebaseFirestore.getInstance()
    private val uid get() = FirebaseAuth.getInstance().currentUser?.uid

    // ─── Goals ────────────────────────────────────────────────────────────────

    suspend fun saveGoal(goal: FitnessGoalEntry) {
        val u = uid ?: return
        db.collection("client_fitness").document(u)
            .collection("goals").document(goal.exerciseId)
            .set(mapOf(
                "exerciseId"         to goal.exerciseId,
                "exerciseName"       to goal.exerciseName,
                "targetSets"         to goal.targetSets,
                "targetReps"         to goal.targetReps,
                "targetWeightKg"     to goal.targetWeightKg.toDouble(),
                "targetDurationSecs" to goal.targetDurationSecs,
                "createdAtMillis"    to goal.createdAtMillis
            )).await()
    }

    /** Real-time goals listener — fires immediately and on every change */
    fun listenGoals(uid: String, onGoals: (List<FitnessGoalEntry>) -> Unit): ListenerRegistration {
        val authUid = this.uid
        if (authUid == null || uid != authUid) {
            onGoals(emptyList())
            return db.collection("client_fitness").addSnapshotListener { _, _ -> }
        }
        return db.collection("client_fitness").document(authUid)
            .collection("goals")
            .addSnapshotListener { snap, err ->
                if (err != null) return@addSnapshotListener
                onGoals(snap?.documents?.mapNotNull { doc ->
                    val d = doc.data ?: return@mapNotNull null
                    FitnessGoalEntry(
                        exerciseId         = d["exerciseId"] as? String ?: "",
                        exerciseName       = d["exerciseName"] as? String ?: "",
                        targetSets         = (d["targetSets"] as? Long)?.toInt() ?: 0,
                        targetReps         = (d["targetReps"] as? Long)?.toInt() ?: 0,
                        targetWeightKg     = (d["targetWeightKg"] as? Double)?.toFloat() ?: 0f,
                        targetDurationSecs = (d["targetDurationSecs"] as? Long)?.toInt() ?: 0,
                        createdAtMillis    = d["createdAtMillis"] as? Long ?: 0L
                    )
                } ?: emptyList())
            }
    }

    suspend fun deleteGoal(exerciseId: String) {
        val u = uid ?: return
        db.collection("client_fitness").document(u)
            .collection("goals").document(exerciseId).delete().await()
    }

    // ─── Workout logs ─────────────────────────────────────────────────────────

    suspend fun saveLog(entry: WorkoutLogEntry) {
        val u = uid ?: return
        db.collection("client_fitness").document(u)
            .collection("logs").document(entry.id)
            .set(mapOf(
                "id"            to entry.id,
                "exerciseId"    to entry.exerciseId,
                "exerciseName"  to entry.exerciseName,
                "dateMillis"    to entry.dateMillis,
                "setsCompleted" to entry.setsCompleted,
                "repsCompleted" to entry.repsCompleted,
                "weightKg"      to entry.weightKg.toDouble(),
                "durationSecs"  to entry.durationSecs,
                "notes"         to entry.notes,
                "setDetails"    to entry.setDetails.map { s ->
                    mapOf(
                        "setIndex"     to s.setIndex,
                        "reps"         to s.reps,
                        "weightKg"     to s.weightKg.toDouble(),
                        "durationSecs" to s.durationSecs,
                        "isWarmup"     to s.isWarmup,
                        "rpe"          to s.rpe
                    )
                }
            )).await()
    }

    /** Real-time logs listener — fires immediately and on every change */
    fun listenLogs(uid: String, onLogs: (List<WorkoutLogEntry>) -> Unit): ListenerRegistration {
        val authUid = this.uid
        if (authUid == null || uid != authUid) {
            onLogs(emptyList())
            return db.collection("client_fitness").addSnapshotListener { _, _ -> }
        }
        return db.collection("client_fitness").document(authUid)
            .collection("logs")
            .orderBy("dateMillis", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) return@addSnapshotListener
                onLogs(snap?.documents?.mapNotNull { doc ->
                    val d = doc.data ?: return@mapNotNull null
                    @Suppress("UNCHECKED_CAST")
                    val rawSets = d["setDetails"] as? List<Map<String, Any>> ?: emptyList()
                    val setDetails = rawSets.map { s ->
                        SetDetail(
                            setIndex     = (s["setIndex"] as? Long)?.toInt() ?: 0,
                            reps         = (s["reps"] as? Long)?.toInt() ?: 0,
                            weightKg     = (s["weightKg"] as? Double)?.toFloat() ?: 0f,
                            durationSecs = (s["durationSecs"] as? Long)?.toInt() ?: 0,
                            isWarmup     = s["isWarmup"] as? Boolean ?: false,
                            rpe          = (s["rpe"] as? Long)?.toInt() ?: 0
                        )
                    }
                    WorkoutLogEntry(
                        id            = d["id"] as? String ?: doc.id,
                        exerciseId    = d["exerciseId"] as? String ?: "",
                        exerciseName  = d["exerciseName"] as? String ?: "",
                        dateMillis    = d["dateMillis"] as? Long ?: 0L,
                        setsCompleted = (d["setsCompleted"] as? Long)?.toInt() ?: 0,
                        repsCompleted = (d["repsCompleted"] as? Long)?.toInt() ?: 0,
                        weightKg      = (d["weightKg"] as? Double)?.toFloat() ?: 0f,
                        durationSecs  = (d["durationSecs"] as? Long)?.toInt() ?: 0,
                        notes         = d["notes"] as? String ?: "",
                        setDetails    = setDetails
                    )
                } ?: emptyList())
            }
}

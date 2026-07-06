package com.example.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Permanent account deletion — required by Google Play for any app with
 * account creation. Deletes the user's cloud data collection-by-collection,
 * then the Firebase Auth account itself.
 *
 * Shared business records that belong to OTHER users (a gym owner's fee
 * ledger for this member, chat threads, bookings) are intentionally kept —
 * they are the counterparty's records.
 */
object AccountDeletion {

    private val db get() = FirebaseFirestore.getInstance()

    private val COACH_SUBCOLLECTIONS = listOf(
        "clients", "programs", "payments", "workout_logs", "measurements",
        "notes", "revenue_snapshots",
        "gym_members", "gym_plans", "gym_payments", "gym_checkins",
    )
    private val USER_SUBCOLLECTIONS = listOf(
        "health_daily", "health_measurements", "health_photos", "health_cycle",
    )
    private val FITNESS_SUBCOLLECTIONS = listOf("goals", "logs", "food_diary")

    /**
     * Returns success, or failure whose message is safe to show the user.
     * On success the Firebase session is gone — caller must clear local
     * state and navigate to login.
     */
    suspend fun deleteAccount(): Result<Unit> {
        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser ?: return Result.failure(Exception("Not signed in"))
        val uid = user.uid

        // 1. Cloud data — each group individually guarded so one failure
        //    doesn't strand the rest
        runCatching {
            COACH_SUBCOLLECTIONS.forEach { deleteSubcollection("coaches", uid, it) }
            db.collection("coaches").document(uid).delete().await()
        }
        runCatching {
            USER_SUBCOLLECTIONS.forEach { deleteSubcollection("users", uid, it) }
            db.collection("users").document(uid).delete().await()
        }
        runCatching {
            FITNESS_SUBCOLLECTIONS.forEach { deleteSubcollection("client_fitness", uid, it) }
            db.collection("client_fitness").document(uid).delete().await()
        }
        runCatching {
            deleteSubcollection("member_health", uid, "records")
            db.collection("member_health").document(uid).delete().await()
        }
        runCatching { db.collection("trainers").document(uid).delete().await() }
        runCatching {
            // Phone directory entries this user published
            db.collection("user_phone_index").whereEqualTo("uid", uid)
                .get().await().documents.forEach { it.reference.delete().await() }
        }
        runCatching { db.collection("user_records").document(uid).delete().await() }

        // 2. The auth account itself. Firebase requires a recent sign-in for
        //    this — if the session is old, tell the user exactly what to do.
        return try {
            user.delete().await()
            Result.success(Unit)
        } catch (e: FirebaseAuthRecentLoginRequiredException) {
            Result.failure(Exception(
                "For security, deleting an account needs a fresh sign-in. " +
                "Please sign out, sign in again, and delete your account right away."
            ))
        } catch (e: Exception) {
            Result.failure(Exception("Couldn't delete the account — check your connection and try again."))
        }
    }

    /** Deletes a subcollection in pages of 200 (client-side deletion has no recursive delete). */
    private suspend fun deleteSubcollection(root: String, uid: String, name: String) {
        val col = db.collection(root).document(uid).collection(name)
        while (true) {
            val page = col.limit(200).get().await().documents
            if (page.isEmpty()) return
            val batch = db.batch()
            page.forEach { batch.delete(it.reference) }
            batch.commit().await()
            if (page.size < 200) return
        }
    }
}

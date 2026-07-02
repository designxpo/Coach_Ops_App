package com.example.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Member-side view of gym memberships.
 * Gym owners index each member's plan into gym_memberships/{ownerUid}_{phone};
 * the member's own app matches on their phone number to show a membership
 * card and fire renewal reminders — no manual linking needed.
 */
object GymMembershipLookup {

    data class Membership(
        val gymName: String,
        val memberName: String,
        val planName: String,
        val planEndMillis: Long,
        val status: String,
        val ownerUid: String
    ) {
        val daysLeft: Int
            get() = if (planEndMillis <= 0L) 0
                else ((planEndMillis - System.currentTimeMillis()) / 86400000L).toInt()

        val isExpired: Boolean get() = planEndMillis in 1 until System.currentTimeMillis()
    }

    suspend fun findByPhone(rawPhone: String): List<Membership> {
        if (FirebaseAuth.getInstance().currentUser == null) return emptyList()
        val phone = GymSync.normalizePhone(rawPhone)
        if (phone.length != 10) return emptyList()
        return try {
            FirebaseFirestore.getInstance()
                .collection("gym_memberships")
                .whereEqualTo("phone", phone)
                .get().await()
                .documents.mapNotNull { d ->
                    Membership(
                        gymName = d.getString("gymName")?.ifEmpty { "Your Gym" } ?: "Your Gym",
                        memberName = d.getString("memberName") ?: "",
                        planName = d.getString("planName") ?: "",
                        planEndMillis = d.getLong("planEndMillis") ?: 0L,
                        status = d.getString("status") ?: "ACTIVE",
                        ownerUid = d.getString("ownerUid") ?: return@mapNotNull null
                    )
                }
                .filter { it.status != "LEFT" }
        } catch (_: Exception) {
            emptyList()
        }
    }
}

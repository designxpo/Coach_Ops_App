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
        val ownerUid: String,
        val phone: String = "",
        val upiId: String = "",
        val renewalAmountInr: Int = 0,
        val claimStatus: String = "",    // "" none · PENDING · REJECTED
        val joinDateMillis: Long = 0L,
        val gymAddress: String = ""
    ) {
        val daysLeft: Int
            get() = if (planEndMillis <= 0L) 0
                else ((planEndMillis - System.currentTimeMillis()) / 86400000L).toInt()

        val isExpired: Boolean get() = planEndMillis in 1 until System.currentTimeMillis()
        val canPayInApp: Boolean get() = upiId.isNotEmpty() && planEndMillis > 0L
    }

    private fun claimId(ownerUid: String, phone: String) = "${ownerUid}_$phone"

    /**
     * Finds memberships for the signed-in user two ways and merges:
     *  1. by account UID (set when the gym owner added them as a linked app user)
     *  2. by phone number (fallback for pre-link records)
     * One person = one identity, even if their phone changes later.
     */
    suspend fun findByPhone(rawPhone: String): List<Membership> {
        val user = FirebaseAuth.getInstance().currentUser ?: return emptyList()
        val phone = GymSync.normalizePhone(rawPhone)
        val db = FirebaseFirestore.getInstance()
        return try {
            val docs = mutableMapOf<String, com.google.firebase.firestore.DocumentSnapshot>()
            try {
                db.collection("gym_memberships").whereEqualTo("memberUid", user.uid)
                    .get().await().documents.forEach { docs[it.id] = it }
            } catch (_: Exception) { }
            if (phone.length == 10) {
                try {
                    db.collection("gym_memberships").whereEqualTo("phone", phone)
                        .get().await().documents.forEach { docs.putIfAbsent(it.id, it) }
                } catch (_: Exception) { }
            }

            docs.values.mapNotNull { d ->
                val ownerUid = d.getString("ownerUid") ?: return@mapNotNull null
                val docPhone = d.getString("phone") ?: phone
                // Any pending "I've paid" claim for this membership?
                val claimStatus = try {
                    db.collection("gym_payment_claims")
                        .document(claimId(ownerUid, docPhone))
                        .get().await()
                        .getString("status") ?: ""
                } catch (_: Exception) { "" }
                Membership(
                    gymName = d.getString("gymName")?.ifEmpty { "Your Gym" } ?: "Your Gym",
                    memberName = d.getString("memberName") ?: "",
                    planName = d.getString("planName") ?: "",
                    planEndMillis = d.getLong("planEndMillis") ?: 0L,
                    status = d.getString("status") ?: "ACTIVE",
                    ownerUid = ownerUid,
                    phone = docPhone,
                    upiId = d.getString("upiId") ?: "",
                    renewalAmountInr = (d.getLong("renewalAmountInr") ?: 0L).toInt(),
                    claimStatus = claimStatus,
                    joinDateMillis = d.getLong("joinDateMillis") ?: 0L,
                    gymAddress = d.getString("gymAddress") ?: ""
                )
            }.filter { it.status != "LEFT" }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Member taps "I've paid" after completing the UPI transfer.
     * Creates a claim the gym owner confirms from their dashboard —
     * confirmation records the payment and extends validity automatically.
     */
    suspend fun submitPaymentClaim(ms: Membership): Boolean {
        val user = FirebaseAuth.getInstance().currentUser ?: return false
        return try {
            FirebaseFirestore.getInstance()
                .collection("gym_payment_claims")
                .document(claimId(ms.ownerUid, ms.phone))
                .set(mapOf(
                    "ownerUid" to ms.ownerUid,
                    "claimerUid" to user.uid,
                    "phone" to ms.phone,
                    "memberName" to ms.memberName,
                    "gymName" to ms.gymName,
                    "planName" to ms.planName,
                    "amountInr" to ms.renewalAmountInr,
                    "status" to "PENDING",
                    "claimedAt" to System.currentTimeMillis()
                )).await()
            true
        } catch (_: Exception) { false }
    }
}

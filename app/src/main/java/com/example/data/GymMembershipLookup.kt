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
        val claimStatus: String = ""     // "" none · PENDING · REJECTED
    ) {
        val daysLeft: Int
            get() = if (planEndMillis <= 0L) 0
                else ((planEndMillis - System.currentTimeMillis()) / 86400000L).toInt()

        val isExpired: Boolean get() = planEndMillis in 1 until System.currentTimeMillis()
        val canPayInApp: Boolean get() = upiId.isNotEmpty() && planEndMillis > 0L
    }

    private fun claimId(ownerUid: String, phone: String) = "${ownerUid}_$phone"

    suspend fun findByPhone(rawPhone: String): List<Membership> {
        if (FirebaseAuth.getInstance().currentUser == null) return emptyList()
        val phone = GymSync.normalizePhone(rawPhone)
        if (phone.length != 10) return emptyList()
        val db = FirebaseFirestore.getInstance()
        return try {
            db.collection("gym_memberships")
                .whereEqualTo("phone", phone)
                .get().await()
                .documents.mapNotNull { d ->
                    val ownerUid = d.getString("ownerUid") ?: return@mapNotNull null
                    // Any pending "I've paid" claim for this membership?
                    val claimStatus = try {
                        db.collection("gym_payment_claims")
                            .document(claimId(ownerUid, phone))
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
                        phone = phone,
                        upiId = d.getString("upiId") ?: "",
                        renewalAmountInr = (d.getLong("renewalAmountInr") ?: 0L).toInt(),
                        claimStatus = claimStatus
                    )
                }
                .filter { it.status != "LEFT" }
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

package com.example.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Firestore mirror for gym data — same pattern as FirestoreSync.
 * Everything lives under coaches/{uid}/gym_* so the existing per-owner
 * security model applies unchanged.
 */
object GymSync {

    private val auth = FirebaseAuth.getInstance()
    private val db   = FirebaseFirestore.getInstance()

    private val uid get() = auth.currentUser?.uid
    private fun col(name: String) = uid?.let {
        db.collection("coaches").document(it).collection(name)
    }

    /** Set by GymViewModel so the membership index can carry the gym's identity. */
    var gymName: String = ""
    var gymUpiId: String = ""
    var gymAddress: String = ""

    fun normalizePhone(raw: String): String = raw.filter { it.isDigit() }.takeLast(10)

    // ─── Gyms (locations) ─────────────────────────────────────────────────────

    fun saveGym(g: Gym) {
        col("gyms")?.document(g.id)?.set(mapOf(
            "id" to g.id, "name" to g.name, "city" to g.city,
            "address" to g.address, "upiId" to g.upiId, "gstin" to g.gstin,
            "createdAtMillis" to g.createdAtMillis
        ))
    }

    fun deleteGymDoc(gymId: String) {
        col("gyms")?.document(gymId)?.delete()
    }

    /** Firestore-side cascade for one location. IDs come from Room (which also
     *  covers legacy docs that predate the gymId field). */
    fun deleteGymData(
        memberIds: List<String>, planIds: List<String>,
        paymentIds: List<String>, checkInIds: List<String>,
        memberPhones: List<String>
    ) {
        val u = uid ?: return
        memberIds.forEach { col("gym_members")?.document(it)?.delete() }
        planIds.forEach { col("gym_plans")?.document(it)?.delete() }
        paymentIds.forEach { col("gym_payments")?.document(it)?.delete() }
        checkInIds.forEach { col("gym_checkins")?.document(it)?.delete() }
        memberPhones.mapNotNull { normalizePhone(it).takeIf { p -> p.length == 10 } }.forEach { p ->
            db.collection("gym_memberships").document("${u}_$p").delete()
        }
    }

    // ─── Members ──────────────────────────────────────────────────────────────

    fun saveMember(m: GymMember) {
        col("gym_members")?.document(m.id)?.set(mapOf(
            "id" to m.id, "name" to m.name, "phone" to m.phone,
            "gymId" to m.gymId,
            "gender" to m.gender, "joinDateMillis" to m.joinDateMillis,
            "planId" to m.planId, "planName" to m.planName,
            "planPriceInr" to m.planPriceInr,
            "planStartMillis" to m.planStartMillis, "planEndMillis" to m.planEndMillis,
            "status" to m.status, "notes" to m.notes, "linkedUid" to m.linkedUid
        ))
        upsertMembershipIndex(m)
    }

    fun deleteMember(memberId: String, phone: String = "") {
        col("gym_members")?.document(memberId)?.delete()
        val u = uid ?: return
        val p = normalizePhone(phone)
        if (p.length == 10) {
            db.collection("gym_memberships").document("${u}_$p").delete()
        }
    }

    // ─── Membership index — lets the member's own app find their gym plan ────
    // Keyed by phone so a member account (matched by phone) can see expiry and
    // get in-app renewal reminders.

    private fun upsertMembershipIndex(m: GymMember) {
        val u = uid ?: return
        val p = normalizePhone(m.phone)
        if (p.length != 10) return
        db.collection("gym_memberships").document("${u}_$p").set(mapOf(
            "ownerUid" to u,
            "phone" to p,
            "memberUid" to m.linkedUid,
            "gymName" to gymName,
            "gymAddress" to gymAddress,
            "upiId" to gymUpiId,
            "memberName" to m.name,
            "planName" to m.planName,
            "renewalAmountInr" to m.planPriceInr,
            "planEndMillis" to m.planEndMillis,
            "joinDateMillis" to m.joinDateMillis,
            "status" to m.status,
            "updatedAt" to System.currentTimeMillis()
        ))
    }

    // ─── Plans ────────────────────────────────────────────────────────────────

    fun savePlan(p: GymPlan) {
        col("gym_plans")?.document(p.id)?.set(mapOf(
            "id" to p.id, "name" to p.name, "durationDays" to p.durationDays,
            "gymId" to p.gymId,
            "priceInr" to p.priceInr, "description" to p.description,
            "isActive" to p.isActive
        ))
    }

    // ─── Payments ─────────────────────────────────────────────────────────────

    fun savePayment(p: GymPayment) {
        col("gym_payments")?.document(p.id)?.set(mapOf(
            "id" to p.id, "memberId" to p.memberId, "memberName" to p.memberName,
            "gymId" to p.gymId,
            "amountInr" to p.amountInr, "method" to p.method,
            "dateMillis" to p.dateMillis, "planId" to p.planId,
            "planName" to p.planName, "periodStartMillis" to p.periodStartMillis,
            "periodEndMillis" to p.periodEndMillis, "receiptNo" to p.receiptNo,
            "notes" to p.notes
        ))
    }

    fun deletePaymentsByMember(memberId: String) {
        col("gym_payments")?.whereEqualTo("memberId", memberId)?.get()
            ?.addOnSuccessListener { snap -> snap.documents.forEach { it.reference.delete() } }
    }

    // ─── Check-ins ────────────────────────────────────────────────────────────

    fun saveCheckIn(c: GymCheckIn) {
        col("gym_checkins")?.document(c.id)?.set(mapOf(
            "id" to c.id, "memberId" to c.memberId, "memberName" to c.memberName,
            "gymId" to c.gymId,
            "dateKey" to c.dateKey, "timeMillis" to c.timeMillis
        ))
    }

    fun deleteCheckInsByMember(memberId: String) {
        col("gym_checkins")?.whereEqualTo("memberId", memberId)?.get()
            ?.addOnSuccessListener { snap -> snap.documents.forEach { it.reference.delete() } }
    }

    // ─── Restore: pull the full gym dataset into Room (new device / reinstall) ─

    suspend fun pullToRoom(dao: GymDao) {
        val u = uid ?: return
        val base = db.collection("coaches").document(u)
        try {
            val gyms = base.collection("gyms").get().await().documents.mapNotNull { d ->
                Gym(
                    id = d.getString("id") ?: return@mapNotNull null,
                    name = d.getString("name") ?: "",
                    city = d.getString("city") ?: "",
                    address = d.getString("address") ?: "",
                    upiId = d.getString("upiId") ?: "",
                    gstin = d.getString("gstin") ?: "",
                    createdAtMillis = d.getLong("createdAtMillis") ?: 0L
                )
            }
            if (gyms.isNotEmpty()) dao.insertGyms(gyms)

            val members = base.collection("gym_members").get().await().documents.mapNotNull { d ->
                GymMember(
                    id = d.getString("id") ?: return@mapNotNull null,
                    name = d.getString("name") ?: "",
                    gymId = d.getString("gymId") ?: DEFAULT_GYM_ID,
                    phone = d.getString("phone") ?: "",
                    gender = d.getString("gender") ?: "",
                    joinDateMillis = d.getLong("joinDateMillis") ?: 0L,
                    planId = d.getString("planId") ?: "",
                    planName = d.getString("planName") ?: "",
                    planPriceInr = (d.getLong("planPriceInr") ?: 0L).toInt(),
                    planStartMillis = d.getLong("planStartMillis") ?: 0L,
                    planEndMillis = d.getLong("planEndMillis") ?: 0L,
                    status = d.getString("status") ?: "ACTIVE",
                    notes = d.getString("notes") ?: "",
                    linkedUid = d.getString("linkedUid") ?: ""
                )
            }
            if (members.isNotEmpty()) dao.insertMembers(members)

            val plans = base.collection("gym_plans").get().await().documents.mapNotNull { d ->
                GymPlan(
                    id = d.getString("id") ?: return@mapNotNull null,
                    name = d.getString("name") ?: "",
                    gymId = d.getString("gymId") ?: DEFAULT_GYM_ID,
                    durationDays = (d.getLong("durationDays") ?: 30L).toInt(),
                    priceInr = (d.getLong("priceInr") ?: 0L).toInt(),
                    description = d.getString("description") ?: "",
                    isActive = d.getBoolean("isActive") ?: true
                )
            }
            if (plans.isNotEmpty()) dao.insertPlans(plans)

            val payments = base.collection("gym_payments").get().await().documents.mapNotNull { d ->
                GymPayment(
                    id = d.getString("id") ?: return@mapNotNull null,
                    memberId = d.getString("memberId") ?: "",
                    gymId = d.getString("gymId") ?: DEFAULT_GYM_ID,
                    memberName = d.getString("memberName") ?: "",
                    amountInr = (d.getLong("amountInr") ?: 0L).toInt(),
                    method = d.getString("method") ?: "CASH",
                    dateMillis = d.getLong("dateMillis") ?: 0L,
                    planId = d.getString("planId") ?: "",
                    planName = d.getString("planName") ?: "",
                    periodStartMillis = d.getLong("periodStartMillis") ?: 0L,
                    periodEndMillis = d.getLong("periodEndMillis") ?: 0L,
                    receiptNo = d.getString("receiptNo") ?: "",
                    notes = d.getString("notes") ?: ""
                )
            }
            if (payments.isNotEmpty()) dao.insertPayments(payments)

            val checkIns = base.collection("gym_checkins").get().await().documents.mapNotNull { d ->
                GymCheckIn(
                    id = d.getString("id") ?: return@mapNotNull null,
                    memberId = d.getString("memberId") ?: "",
                    gymId = d.getString("gymId") ?: DEFAULT_GYM_ID,
                    memberName = d.getString("memberName") ?: "",
                    dateKey = d.getString("dateKey") ?: "",
                    timeMillis = d.getLong("timeMillis") ?: 0L
                )
            }
            if (checkIns.isNotEmpty()) dao.insertCheckIns(checkIns)
        } catch (_: Exception) { /* offline — Room stays authoritative locally */ }
    }
}

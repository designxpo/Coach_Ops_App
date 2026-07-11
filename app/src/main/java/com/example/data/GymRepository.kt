package com.example.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Gym operations repository — Room is the read model, every write mirrors to
 * Firestore via GymSync (same offline-first pattern as CoachRepository).
 */
class GymRepository(private val dao: GymDao) {

    val gyms: Flow<List<Gym>> = dao.getGyms()

    fun membersFor(gymId: String): Flow<List<GymMember>> = dao.getAllMembers(gymId)
    fun plansFor(gymId: String): Flow<List<GymPlan>> = dao.getActivePlans(gymId)
    fun paymentsFor(gymId: String): Flow<List<GymPayment>> = dao.getAllPayments(gymId)
    fun checkInsForDate(gymId: String, dateKey: String): Flow<List<GymCheckIn>> =
        dao.getCheckInsForDate(gymId, dateKey)

    fun getMember(id: String): Flow<GymMember?> = dao.getMember(id)
    fun getPaymentsForMember(id: String): Flow<List<GymPayment>> = dao.getPaymentsForMember(id)
    suspend fun getCheckInCountForMember(id: String): Int = dao.getCheckInCountForMember(id)

    // ─── Gym locations ────────────────────────────────────────────────────────

    suspend fun getGymsOnce(): List<Gym> = dao.getGymsOnce()

    suspend fun saveGym(gym: Gym) {
        dao.insertGym(gym)
        GymSync.saveGym(gym)
    }

    /**
     * Deletes one location and everything inside it — members, plans, payments,
     * attendance — locally and in Firestore. IDs are collected from Room first
     * so legacy cloud docs (written before gymId existed) are covered too.
     */
    suspend fun deleteGymCascade(gymId: String) {
        val members = dao.getMembersForGymOnce(gymId)
        GymSync.deleteGymData(
            memberIds  = members.map { it.id },
            planIds    = dao.getPlanIdsForGym(gymId),
            paymentIds = dao.getPaymentIdsForGym(gymId),
            checkInIds = dao.getCheckInIdsForGym(gymId),
            memberPhones = members.map { it.phone }
        )
        dao.deleteCheckInsForGym(gymId)
        dao.deletePaymentsForGym(gymId)
        dao.deletePlansForGym(gymId)
        dao.deleteMembersForGym(gymId)
        dao.deleteGym(gymId)
        GymSync.deleteGymDoc(gymId)
    }

    companion object {
        @Volatile private var INSTANCE: GymRepository? = null

        fun getInstance(context: Context): GymRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: GymRepository(
                    AppDatabase.getInstance(context).gymDao()
                ).also { INSTANCE = it }
            }

        fun todayKey(): String =
            SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

        /** Common Indian gym pricing presets seeded on first use. */
        fun defaultPlans(gymId: String = DEFAULT_GYM_ID): List<GymPlan> = listOf(
            GymPlan(UUID.randomUUID().toString(), "Monthly",     gymId, 30,  1200, "Standard monthly membership"),
            GymPlan(UUID.randomUUID().toString(), "Quarterly",   gymId, 90,  3000, "3 months — save ₹600"),
            GymPlan(UUID.randomUUID().toString(), "Half-Yearly", gymId, 180, 5500, "6 months — save ₹1,700"),
            GymPlan(UUID.randomUUID().toString(), "Annual",      gymId, 365, 10000, "Best value — save ₹4,400")
        )
    }

    // ─── Members ──────────────────────────────────────────────────────────────

    suspend fun addMember(member: GymMember) {
        dao.insertMember(member)
        GymSync.saveMember(member)
    }

    /**
     * Registers a member and (optionally) records the joining fee in one step.
     * Plan validity is auto-calculated from the joining date, so the owner
     * never types dates or amounts by hand.
     */
    suspend fun registerMember(
        member: GymMember,
        plan: GymPlan?,
        collectPayment: Boolean,
        amountInr: Int,
        method: String
    ): GymPayment? {
        dao.insertMember(member)
        GymSync.saveMember(member)
        if (!collectPayment || plan == null || amountInr <= 0) return null

        val receiptNo = "RCP-%04d".format(dao.getMaxReceiptNumber() + 1)
        val payment = GymPayment(
            id = UUID.randomUUID().toString(),
            memberId = member.id,
            gymId = member.gymId,
            memberName = member.name,
            amountInr = amountInr,
            method = method,
            dateMillis = System.currentTimeMillis(),
            planId = plan.id,
            planName = plan.name,
            periodStartMillis = member.planStartMillis,
            periodEndMillis = member.planEndMillis,
            receiptNo = receiptNo,
            notes = "Joining payment"
        )
        dao.insertPayment(payment)
        GymSync.savePayment(payment)
        return payment
    }

    suspend fun updateMember(member: GymMember) {
        dao.updateMember(member)
        GymSync.saveMember(member)
    }

    suspend fun deleteMember(member: GymMember) {
        dao.deletePaymentsForMember(member.id)
        dao.deleteCheckInsForMember(member.id)
        dao.deleteMember(member.id)
        GymSync.deletePaymentsByMember(member.id)
        GymSync.deleteCheckInsByMember(member.id)
        GymSync.deleteMember(member.id, member.phone)
    }

    suspend fun getMembersExpiringBetween(from: Long, to: Long): List<GymMember> =
        dao.getMembersExpiringBetween(from, to)

    // ─── Plans ────────────────────────────────────────────────────────────────

    suspend fun savePlan(plan: GymPlan) {
        dao.insertPlan(plan)
        GymSync.savePlan(plan)
    }

    suspend fun deactivatePlan(plan: GymPlan) {
        dao.deactivatePlan(plan.id)
        GymSync.savePlan(plan.copy(isActive = false))
    }

    suspend fun seedDefaultPlansIfEmpty(gymId: String = DEFAULT_GYM_ID): Boolean {
        if (dao.getPlanCountForGym(gymId) > 0) return false
        defaultPlans(gymId).forEach { savePlan(it) }
        return true
    }

    // ─── Payments: record fee + extend membership in one transaction ─────────

    /**
     * Records a payment and extends the member's plan validity.
     * Extension starts from the current expiry when the plan is still active
     * (early renewal doesn't lose days) or from today when expired.
     * Returns the saved payment (with generated receipt number).
     */
    suspend fun recordPayment(
        member: GymMember,
        plan: GymPlan,
        amountInr: Int,
        method: String,
        notes: String = ""
    ): GymPayment {
        val now = System.currentTimeMillis()
        val periodStart = if (member.planEndMillis > now) member.planEndMillis else now
        val periodEnd   = periodStart + plan.durationDays * 86400000L

        val receiptNo = "RCP-%04d".format(dao.getMaxReceiptNumber() + 1)
        val payment = GymPayment(
            id = UUID.randomUUID().toString(),
            memberId = member.id,
            gymId = member.gymId,
            memberName = member.name,
            amountInr = amountInr,
            method = method,
            dateMillis = now,
            planId = plan.id,
            planName = plan.name,
            periodStartMillis = periodStart,
            periodEndMillis = periodEnd,
            receiptNo = receiptNo,
            notes = notes
        )
        dao.insertPayment(payment)
        GymSync.savePayment(payment)

        val updated = member.copy(
            planId = plan.id,
            planName = plan.name,
            planPriceInr = plan.priceInr,
            planStartMillis = if (member.planStartMillis == 0L) now else member.planStartMillis,
            planEndMillis = periodEnd,
            status = "ACTIVE"
        )
        dao.updateMember(updated)
        GymSync.saveMember(updated)
        return payment
    }

    // ─── Check-ins ────────────────────────────────────────────────────────────

    /** Marks attendance. Returns false when the member already checked in today. */
    suspend fun checkIn(member: GymMember): Boolean {
        val today = todayKey()
        if (dao.hasCheckedInToday(member.id, today) > 0) return false
        val checkIn = GymCheckIn(
            id = UUID.randomUUID().toString(),
            memberId = member.id,
            gymId = member.gymId,
            memberName = member.name,
            dateKey = today
        )
        dao.insertCheckIn(checkIn)
        GymSync.saveCheckIn(checkIn)
        return true
    }

    // ─── Cloud restore & logout ───────────────────────────────────────────────

    suspend fun syncFromFirestoreIfEmpty() {
        if (dao.getMemberCount() == 0 && dao.getPlanCount() == 0) {
            GymSync.pullToRoom(dao)
        }
    }

    suspend fun clearAllLocalData() {
        dao.clearGyms()
        dao.clearMembers()
        dao.clearPlans()
        dao.clearPayments()
        dao.clearCheckIns()
    }
}

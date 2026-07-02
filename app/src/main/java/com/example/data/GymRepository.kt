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

    val allMembers: Flow<List<GymMember>> = dao.getAllMembers()
    val activePlans: Flow<List<GymPlan>>  = dao.getActivePlans()
    val allPayments: Flow<List<GymPayment>> = dao.getAllPayments()

    fun getMember(id: String): Flow<GymMember?> = dao.getMember(id)
    fun getPaymentsForMember(id: String): Flow<List<GymPayment>> = dao.getPaymentsForMember(id)
    fun getCheckInsForDate(dateKey: String): Flow<List<GymCheckIn>> = dao.getCheckInsForDate(dateKey)
    suspend fun getCheckInCountForMember(id: String): Int = dao.getCheckInCountForMember(id)

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
        fun defaultPlans(): List<GymPlan> = listOf(
            GymPlan(UUID.randomUUID().toString(), "Monthly",     30,  1200, "Standard monthly membership"),
            GymPlan(UUID.randomUUID().toString(), "Quarterly",   90,  3000, "3 months — save ₹600"),
            GymPlan(UUID.randomUUID().toString(), "Half-Yearly", 180, 5500, "6 months — save ₹1,700"),
            GymPlan(UUID.randomUUID().toString(), "Annual",      365, 10000, "Best value — save ₹4,400")
        )
    }

    // ─── Members ──────────────────────────────────────────────────────────────

    suspend fun addMember(member: GymMember) {
        dao.insertMember(member)
        GymSync.saveMember(member)
    }

    suspend fun updateMember(member: GymMember) {
        dao.updateMember(member)
        GymSync.saveMember(member)
    }

    suspend fun deleteMember(memberId: String) {
        dao.deletePaymentsForMember(memberId)
        dao.deleteCheckInsForMember(memberId)
        dao.deleteMember(memberId)
        GymSync.deletePaymentsByMember(memberId)
        GymSync.deleteCheckInsByMember(memberId)
        GymSync.deleteMember(memberId)
    }

    // ─── Plans ────────────────────────────────────────────────────────────────

    suspend fun savePlan(plan: GymPlan) {
        dao.insertPlan(plan)
        GymSync.savePlan(plan)
    }

    suspend fun deactivatePlan(plan: GymPlan) {
        dao.deactivatePlan(plan.id)
        GymSync.savePlan(plan.copy(isActive = false))
    }

    suspend fun seedDefaultPlansIfEmpty(): Boolean {
        if (dao.getPlanCount() > 0) return false
        defaultPlans().forEach { savePlan(it) }
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

        val receiptNo = "RCP-%04d".format(dao.getPaymentCount() + 1)
        val payment = GymPayment(
            id = UUID.randomUUID().toString(),
            memberId = member.id,
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
        dao.clearMembers()
        dao.clearPlans()
        dao.clearPayments()
        dao.clearCheckIns()
    }
}

package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface GymDao {
    // ─── Gyms (locations) ─────────────────────────────────────────────────────
    @Query("SELECT * FROM gyms ORDER BY createdAtMillis ASC")
    fun getGyms(): Flow<List<Gym>>

    @Query("SELECT * FROM gyms ORDER BY createdAtMillis ASC")
    suspend fun getGymsOnce(): List<Gym>

    @Query("SELECT COUNT(*) FROM gyms")
    suspend fun getGymCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGym(gym: Gym)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGyms(gyms: List<Gym>)

    @Query("DELETE FROM gyms WHERE id = :gymId")
    suspend fun deleteGym(gymId: String)

    // ─── Members ──────────────────────────────────────────────────────────────
    @Query("SELECT * FROM gym_members WHERE gymId = :gymId AND status != 'LEFT' ORDER BY planEndMillis ASC")
    fun getAllMembers(gymId: String): Flow<List<GymMember>>

    @Query("SELECT * FROM gym_members WHERE id = :memberId")
    fun getMember(memberId: String): Flow<GymMember?>

    @Query("SELECT COUNT(*) FROM gym_members WHERE status != 'LEFT'")
    suspend fun getMemberCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: GymMember)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMembers(members: List<GymMember>)

    @Update
    suspend fun updateMember(member: GymMember)

    @Query("DELETE FROM gym_members WHERE id = :memberId")
    suspend fun deleteMember(memberId: String)

    // Renewals-due window for the owner's daily reminder notification (all gyms)
    @Query("SELECT * FROM gym_members WHERE status = 'ACTIVE' AND planEndMillis > 0 AND planEndMillis BETWEEN :from AND :to ORDER BY planEndMillis ASC")
    suspend fun getMembersExpiringBetween(from: Long, to: Long): List<GymMember>

    // ─── Plans ────────────────────────────────────────────────────────────────
    @Query("SELECT * FROM gym_plans WHERE gymId = :gymId AND isActive = 1 ORDER BY durationDays ASC")
    fun getActivePlans(gymId: String): Flow<List<GymPlan>>

    @Query("SELECT COUNT(*) FROM gym_plans")
    suspend fun getPlanCount(): Int

    @Query("SELECT COUNT(*) FROM gym_plans WHERE gymId = :gymId")
    suspend fun getPlanCountForGym(gymId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlan(plan: GymPlan)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlans(plans: List<GymPlan>)

    @Query("UPDATE gym_plans SET isActive = 0 WHERE id = :planId")
    suspend fun deactivatePlan(planId: String)

    // ─── Payments ─────────────────────────────────────────────────────────────
    @Query("SELECT * FROM gym_payments WHERE gymId = :gymId ORDER BY dateMillis DESC")
    fun getAllPayments(gymId: String): Flow<List<GymPayment>>

    @Query("SELECT * FROM gym_payments WHERE memberId = :memberId ORDER BY dateMillis DESC")
    fun getPaymentsForMember(memberId: String): Flow<List<GymPayment>>

    @Query("SELECT COUNT(*) FROM gym_payments")
    suspend fun getPaymentCount(): Int

    // Highest receipt number issued so far ("RCP-0042" → 42) across ALL gyms —
    // a single sequence per owner means numbers never collide between locations.
    @Query("SELECT COALESCE(MAX(CAST(SUBSTR(receiptNo, 5) AS INTEGER)), 0) FROM gym_payments")
    suspend fun getMaxReceiptNumber(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: GymPayment)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayments(payments: List<GymPayment>)

    // ─── Check-ins ────────────────────────────────────────────────────────────
    @Query("SELECT * FROM gym_checkins WHERE gymId = :gymId AND dateKey = :dateKey ORDER BY timeMillis DESC")
    fun getCheckInsForDate(gymId: String, dateKey: String): Flow<List<GymCheckIn>>

    @Query("SELECT COUNT(*) FROM gym_checkins WHERE memberId = :memberId")
    suspend fun getCheckInCountForMember(memberId: String): Int

    @Query("SELECT COUNT(*) FROM gym_checkins WHERE memberId = :memberId AND dateKey = :dateKey")
    suspend fun hasCheckedInToday(memberId: String, dateKey: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCheckIn(checkIn: GymCheckIn)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCheckIns(checkIns: List<GymCheckIn>)

    // ─── Per-gym cascade (delete a location) ──────────────────────────────────
    @Query("SELECT * FROM gym_members WHERE gymId = :gymId")
    suspend fun getMembersForGymOnce(gymId: String): List<GymMember>

    @Query("SELECT id FROM gym_payments WHERE gymId = :gymId")
    suspend fun getPaymentIdsForGym(gymId: String): List<String>

    @Query("SELECT id FROM gym_checkins WHERE gymId = :gymId")
    suspend fun getCheckInIdsForGym(gymId: String): List<String>

    @Query("SELECT id FROM gym_plans WHERE gymId = :gymId")
    suspend fun getPlanIdsForGym(gymId: String): List<String>

    @Query("DELETE FROM gym_members WHERE gymId = :gymId")
    suspend fun deleteMembersForGym(gymId: String)

    @Query("DELETE FROM gym_plans WHERE gymId = :gymId")
    suspend fun deletePlansForGym(gymId: String)

    @Query("DELETE FROM gym_payments WHERE gymId = :gymId")
    suspend fun deletePaymentsForGym(gymId: String)

    @Query("DELETE FROM gym_checkins WHERE gymId = :gymId")
    suspend fun deleteCheckInsForGym(gymId: String)

    // ─── Cleanup for member deletion & logout ─────────────────────────────────
    @Query("DELETE FROM gym_payments WHERE memberId = :memberId")
    suspend fun deletePaymentsForMember(memberId: String)

    @Query("DELETE FROM gym_checkins WHERE memberId = :memberId")
    suspend fun deleteCheckInsForMember(memberId: String)

    @Query("DELETE FROM gyms")         suspend fun clearGyms()
    @Query("DELETE FROM gym_members")  suspend fun clearMembers()
    @Query("DELETE FROM gym_plans")    suspend fun clearPlans()
    @Query("DELETE FROM gym_payments") suspend fun clearPayments()
    @Query("DELETE FROM gym_checkins") suspend fun clearCheckIns()
}

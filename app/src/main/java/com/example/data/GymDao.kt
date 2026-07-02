package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface GymDao {
    // ─── Members ──────────────────────────────────────────────────────────────
    @Query("SELECT * FROM gym_members WHERE status != 'LEFT' ORDER BY planEndMillis ASC")
    fun getAllMembers(): Flow<List<GymMember>>

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

    // ─── Plans ────────────────────────────────────────────────────────────────
    @Query("SELECT * FROM gym_plans WHERE isActive = 1 ORDER BY durationDays ASC")
    fun getActivePlans(): Flow<List<GymPlan>>

    @Query("SELECT COUNT(*) FROM gym_plans")
    suspend fun getPlanCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlan(plan: GymPlan)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlans(plans: List<GymPlan>)

    @Query("UPDATE gym_plans SET isActive = 0 WHERE id = :planId")
    suspend fun deactivatePlan(planId: String)

    // ─── Payments ─────────────────────────────────────────────────────────────
    @Query("SELECT * FROM gym_payments ORDER BY dateMillis DESC")
    fun getAllPayments(): Flow<List<GymPayment>>

    @Query("SELECT * FROM gym_payments WHERE memberId = :memberId ORDER BY dateMillis DESC")
    fun getPaymentsForMember(memberId: String): Flow<List<GymPayment>>

    @Query("SELECT COUNT(*) FROM gym_payments")
    suspend fun getPaymentCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: GymPayment)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayments(payments: List<GymPayment>)

    // ─── Check-ins ────────────────────────────────────────────────────────────
    @Query("SELECT * FROM gym_checkins WHERE dateKey = :dateKey ORDER BY timeMillis DESC")
    fun getCheckInsForDate(dateKey: String): Flow<List<GymCheckIn>>

    @Query("SELECT COUNT(*) FROM gym_checkins WHERE memberId = :memberId")
    suspend fun getCheckInCountForMember(memberId: String): Int

    @Query("SELECT COUNT(*) FROM gym_checkins WHERE memberId = :memberId AND dateKey = :dateKey")
    suspend fun hasCheckedInToday(memberId: String, dateKey: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCheckIn(checkIn: GymCheckIn)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCheckIns(checkIns: List<GymCheckIn>)

    // ─── Cleanup for member deletion & logout ─────────────────────────────────
    @Query("DELETE FROM gym_payments WHERE memberId = :memberId")
    suspend fun deletePaymentsForMember(memberId: String)

    @Query("DELETE FROM gym_checkins WHERE memberId = :memberId")
    suspend fun deleteCheckInsForMember(memberId: String)

    @Query("DELETE FROM gym_members")  suspend fun clearMembers()
    @Query("DELETE FROM gym_plans")    suspend fun clearPlans()
    @Query("DELETE FROM gym_payments") suspend fun clearPayments()
    @Query("DELETE FROM gym_checkins") suspend fun clearCheckIns()
}

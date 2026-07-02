package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Gym operations entities — owned by a gym-owner account.
 * Local Room copy mirrors to Firestore under coaches/{uid}/gym_* (same
 * ownership tree the coach data already uses, so security rules stay simple).
 */

@Entity(tableName = "gym_members")
data class GymMember(
    @PrimaryKey val id: String,
    val name: String,
    val phone: String = "",
    val gender: String = "",                 // MALE / FEMALE / OTHER / ""
    val joinDateMillis: Long = System.currentTimeMillis(),
    val planId: String = "",                 // active membership plan
    val planName: String = "",
    val planPriceInr: Int = 0,               // renewal amount — prefills member's UPI payment
    val planStartMillis: Long = 0L,
    val planEndMillis: Long = 0L,            // 0 = no active plan
    val status: String = "ACTIVE",           // ACTIVE, EXPIRED, FROZEN, LEFT
    val notes: String = "",
    val linkedUid: String = ""               // ProCoach account UID when the member is an app user
) {
    val daysLeft: Int
        get() = if (planEndMillis <= 0L) 0
            else ((planEndMillis - System.currentTimeMillis()) / 86400000L).toInt()

    val isExpired: Boolean get() = planEndMillis in 1 until System.currentTimeMillis()
    val expiringSoon: Boolean get() = !isExpired && planEndMillis > 0 && daysLeft <= 7
}

@Entity(tableName = "gym_plans")
data class GymPlan(
    @PrimaryKey val id: String,
    val name: String,                        // "Monthly", "Quarterly (Save 15%)"
    val durationDays: Int,
    val priceInr: Int,
    val description: String = "",
    val isActive: Boolean = true
)

@Entity(tableName = "gym_payments")
data class GymPayment(
    @PrimaryKey val id: String,
    val memberId: String,
    val memberName: String,
    val amountInr: Int,
    val method: String,                      // CASH, UPI, CARD
    val dateMillis: Long = System.currentTimeMillis(),
    val planId: String = "",
    val planName: String = "",
    val periodStartMillis: Long = 0L,
    val periodEndMillis: Long = 0L,
    val receiptNo: String = "",              // "RCP-0001"
    val notes: String = ""
)

@Entity(tableName = "gym_checkins")
data class GymCheckIn(
    @PrimaryKey val id: String,
    val memberId: String,
    val memberName: String,
    val dateKey: String,                     // "yyyy-MM-dd" — one check-in per member per day
    val timeMillis: Long = System.currentTimeMillis()
)

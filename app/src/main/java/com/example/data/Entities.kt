package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "clients")
data class Client(
    @PrimaryKey val id: String,
    val name: String,
    val phoneNumber: String = "",
    val initialGoal: String,
    val programWeek: Int = 1,
    val consistencyScore: Int = 100,
    val programId: String? = null,
    val lastCheckInMillis: Long = System.currentTimeMillis(),
    val mrr: Int = 0,
    val status: String = "Active",
    val enrollmentDateMillis: Long = System.currentTimeMillis(),
    val city: String = "",
    val paymentCycle: String = "MONTHLY", // WEEKLY, MONTHLY, QUARTERLY, YEARLY
    val pausedAtMillis: Long = 0L,          // non-zero when status == "Paused"
    val trainingStartDateMillis: Long = 0L, // 0 = use enrollmentDateMillis
    val trainingEndDateMillis: Long = 0L    // 0 = no end date set
)

@Entity(tableName = "signals")
data class Signal(
    @PrimaryKey val id: String,
    val clientId: String,
    val clientName: String,
    val type: String, // MISSED_WORKOUT, UPI_EXPIRING, PAYMENT_OVERDUE, FORM_CHECK, MESSAGE, MISSED_HABIT, MILESTONE
    val description: String,
    val severity: String, // RED, YELLOW, BLUE
    val timestamp: Long = System.currentTimeMillis(),
    val isResolved: Boolean = false
)

@Entity(tableName = "programs")
data class Program(
    @PrimaryKey val id: String,
    val name: String,
    val durationWeeks: Int,
    val daysPerWeek: Int,
    val tags: String,
    val lastUsedMillis: Long = System.currentTimeMillis(),
    val clientCount: Int = 0,
    val isMasterTemplate: Boolean = true,
    // Comma-separated training days: "MON,WED,FRI" — empty = no fixed schedule
    val workingDays: String = ""
)

@Entity(tableName = "payments")
data class Payment(
    @PrimaryKey val id: String,
    val clientId: String,
    val amount: Int,
    val dueDateMillis: Long,
    val daysOverdue: Int = 0,
    val mandateStatus: String, // ACTIVE, EXPIRING, FAILED
    val paymentCycle: String = "MONTHLY" // WEEKLY, MONTHLY, QUARTERLY, YEARLY
)

@Entity(tableName = "workout_logs")
data class WorkoutLog(
    @PrimaryKey val id: String,
    val clientId: String,
    val programId: String? = null,
    val sessionDateMillis: Long = System.currentTimeMillis(),
    val sessionName: String,
    val exercises: String = "",
    val durationMins: Int = 0,
    val notes: String = "",
    val isMissed: Boolean = false,
    val missedReason: String = ""
)

@Entity(tableName = "body_measurements")
data class BodyMeasurement(
    @PrimaryKey val id: String,
    val clientId: String,
    val dateMillis: Long = System.currentTimeMillis(),
    val weightKg: Float = 0f,
    val bodyFatPct: Float = 0f,
    val notes: String = ""
)

@Entity(tableName = "client_notes")
data class ClientNote(
    @PrimaryKey val id: String,
    val clientId: String,
    val dateMillis: Long = System.currentTimeMillis(),
    val content: String
)

@Entity(tableName = "revenue_snapshots")
data class RevenueSnapshot(
    @PrimaryKey val monthYear: String, // "YYYY-MM"
    val totalMrr: Int
)

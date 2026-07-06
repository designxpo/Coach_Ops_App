package com.example.data

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.LocalDateTime
import java.time.ZoneId

data class HealthSummary(
    val stepsToday: Long = 0,
    val caloriesBurnedToday: Double = 0.0,
    val workoutsThisWeek: Int = 0,
    val isAvailable: Boolean = false
)

class HealthConnectManager(private val context: Context) {

    private val client by lazy { HealthConnectClient.getOrCreate(context) }

    val permissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
    )

    fun isAvailable(): Boolean =
        HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE

    /** Health Connect is present but too old — must be updated from the Play Store. */
    fun needsUpdate(): Boolean =
        HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED

    suspend fun hasPermissions(): Boolean = try {
        client.permissionController.getGrantedPermissions().containsAll(permissions)
    } catch (_: Exception) { false }

    suspend fun readTodaySummary(): HealthSummary {
        if (!isAvailable()) return HealthSummary(isAvailable = false)
        return try {
            val now = LocalDateTime.now()
            val startOfDay = now.toLocalDate().atStartOfDay()
            val zoneId = ZoneId.systemDefault()
            val todayRange = TimeRangeFilter.between(
                startOfDay.atZone(zoneId).toInstant(),
                now.atZone(zoneId).toInstant()
            )
            val weekRange = TimeRangeFilter.between(
                now.minusDays(7).atZone(zoneId).toInstant(),
                now.atZone(zoneId).toInstant()
            )

            val steps = client.readRecords(ReadRecordsRequest(StepsRecord::class, todayRange))
                .records.sumOf { it.count }

            val calories = client.readRecords(ReadRecordsRequest(TotalCaloriesBurnedRecord::class, todayRange))
                .records.sumOf { it.energy.inKilocalories }

            val workouts = client.readRecords(ReadRecordsRequest(ExerciseSessionRecord::class, weekRange))
                .records.size

            HealthSummary(stepsToday = steps, caloriesBurnedToday = calories, workoutsThisWeek = workouts, isAvailable = true)
        } catch (_: Exception) {
            HealthSummary(isAvailable = true)
        }
    }
}

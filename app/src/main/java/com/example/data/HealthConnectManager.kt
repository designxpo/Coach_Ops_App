package com.example.data

import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.CancellationException
import java.time.LocalDateTime
import java.time.ZoneId

data class HealthSummary(
    val stepsToday: Long = 0,
    val caloriesBurnedToday: Double = 0.0,
    val workoutsThisWeek: Int = 0,
    val isAvailable: Boolean = false,
    // Per-metric grant flags — the user may grant only some of the three
    val hasStepsPermission: Boolean = false,
    val hasCaloriesPermission: Boolean = false,
    val hasWorkoutsPermission: Boolean = false,
    // True when a read threw — lets the UI show "couldn't read" instead of a
    // misleading all-zeros day
    val errored: Boolean = false,
)

class HealthConnectManager(private val context: Context) {

    private val client by lazy { HealthConnectClient.getOrCreate(context) }

    private val stepsPermission    = HealthPermission.getReadPermission(StepsRecord::class)
    private val caloriesPermission = HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class)
    private val workoutsPermission = HealthPermission.getReadPermission(ExerciseSessionRecord::class)

    val permissions = setOf(stepsPermission, caloriesPermission, workoutsPermission)

    fun isAvailable(): Boolean =
        HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE

    /** Health Connect is present but too old — must be updated from the Play Store. */
    fun needsUpdate(): Boolean =
        HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED

    /** The subset of our requested permissions the user has actually granted. */
    suspend fun grantedPermissions(): Set<String> = try {
        client.permissionController.getGrantedPermissions().intersect(permissions)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.e(TAG, "getGrantedPermissions failed", e)
        FirebaseCrashlytics.getInstance().recordException(e)
        emptySet()
    }

    /**
     * "Connected" means AT LEAST ONE permission is granted — not all three.
     * The app only functionally needs steps; calories/workouts are display-only,
     * so an all-or-nothing gate would hide the whole feature on a partial grant.
     */
    suspend fun hasAnyPermission(): Boolean = grantedPermissions().isNotEmpty()

    /** Steps is the only metric the app feeds into its own step counter. */
    suspend fun hasStepsPermission(): Boolean = grantedPermissions().contains(stepsPermission)

    suspend fun readTodaySummary(): HealthSummary {
        if (!isAvailable()) return HealthSummary(isAvailable = false)

        val granted = grantedPermissions()
        val hasSteps    = granted.contains(stepsPermission)
        val hasCalories = granted.contains(caloriesPermission)
        val hasWorkouts = granted.contains(workoutsPermission)
        if (granted.isEmpty()) {
            return HealthSummary(isAvailable = true)
        }

        val now        = LocalDateTime.now()
        val zoneId     = ZoneId.systemDefault()
        val todayRange = TimeRangeFilter.between(
            now.toLocalDate().atStartOfDay().atZone(zoneId).toInstant(),
            now.atZone(zoneId).toInstant()
        )
        val weekRange = TimeRangeFilter.between(
            now.minusDays(7).atZone(zoneId).toInstant(),
            now.atZone(zoneId).toInstant()
        )

        var errored  = false
        var steps    = 0L
        var calories = 0.0
        var workouts = 0

        // Steps + calories via the AGGREGATE API — it dedupes overlapping records
        // across data sources (phone + watch + Google Fit) by priority. Summing
        // raw records would double/triple-count the same steps.
        if (hasSteps || hasCalories) {
            try {
                val metrics = buildSet {
                    if (hasSteps)    add(StepsRecord.COUNT_TOTAL)
                    if (hasCalories) add(TotalCaloriesBurnedRecord.ENERGY_TOTAL)
                }
                val agg = client.aggregate(AggregateRequest(metrics = metrics, timeRangeFilter = todayRange))
                if (hasSteps)    steps    = agg[StepsRecord.COUNT_TOTAL] ?: 0L
                if (hasCalories) calories = agg[TotalCaloriesBurnedRecord.ENERGY_TOTAL]?.inKilocalories ?: 0.0
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "aggregate steps/calories failed", e)
                FirebaseCrashlytics.getInstance().recordException(e)
                errored = true
            }
        }

        // Each read guarded independently so a denied Exercise permission zeroes
        // ONLY workouts, never steps/calories.
        if (hasWorkouts) {
            try {
                workouts = client.readRecords(ReadRecordsRequest(ExerciseSessionRecord::class, weekRange)).records.size
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "read workouts failed", e)
                FirebaseCrashlytics.getInstance().recordException(e)
                errored = true
            }
        }

        return HealthSummary(
            stepsToday          = steps,
            caloriesBurnedToday = calories,
            workoutsThisWeek    = workouts,
            isAvailable         = true,
            hasStepsPermission  = hasSteps,
            hasCaloriesPermission = hasCalories,
            hasWorkoutsPermission = hasWorkouts,
            errored             = errored,
        )
    }

    private companion object { const val TAG = "CoachOps.HealthConnect" }
}

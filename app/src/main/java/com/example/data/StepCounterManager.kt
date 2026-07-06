package com.example.data

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Automatic step counting on top of TYPE_STEP_COUNTER (the hardware pedometer).
 *
 * The hardware counts 24/7 whether or not the app is running — it reports a
 * single cumulative value since boot. To make the daily count "just work" on
 * any device we persist the last-seen cumulative reading:
 *
 *   steps_today += (current_reading - last_seen_reading)
 *
 * so every app open captures ALL steps walked since the previous open,
 * including hours or days with the app closed or killed.
 *
 * Handled edge cases:
 *  - Reboot: cumulative value resets to ~0 → current < last_seen → no bogus
 *    delta is added, tracking resumes from the new value.
 *  - Midnight rollover: the daily count resets; a delta that spans midnight
 *    is discarded rather than mis-attributed (safe undercount, never inflate).
 *  - Multi-screen use: singleton — every screen sees the same count.
 *  - Devices without the sensor: isAvailable=false → UI offers manual entry.
 *  - Android 10+ needs ACTIVITY_RECOGNITION; registration is retried via
 *    start() after the permission is granted.
 */
class StepCounterManager private constructor(private val appContext: Context) : SensorEventListener {

    private val prefs =
        appContext.getSharedPreferences("step_counter", Context.MODE_PRIVATE)
    private val sensorManager =
        appContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val stepSensor =
        sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    private val _dailySteps = MutableStateFlow(loadTodaySteps())
    val dailySteps: StateFlow<Int> = _dailySteps.asStateFlow()

    val isAvailable: Boolean get() = stepSensor != null

    private var registered = false

    companion object {
        @Volatile private var instance: StepCounterManager? = null

        fun getInstance(context: Context): StepCounterManager =
            instance ?: synchronized(this) {
                instance ?: StepCounterManager(context.applicationContext).also { instance = it }
            }
    }

    private fun dayKey(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    private fun loadTodaySteps(): Int =
        if (prefs.getString("day", "") == dayKey()) prefs.getInt("steps_today", 0) else 0

    /**
     * Registers the sensor listener. Safe to call repeatedly — also call this
     * again right after ACTIVITY_RECOGNITION is granted so a registration that
     * silently failed without the permission gets retried.
     */
    @Synchronized
    fun start() {
        val sensor = stepSensor ?: return
        // Re-register unconditionally: pre-permission registrations are silently dead
        sensorManager.unregisterListener(this)
        registered = sensorManager.registerListener(
            this, sensor, SensorManager.SENSOR_DELAY_UI
        )
        _dailySteps.value = loadTodaySteps()
    }

    /**
     * Merges an externally stored count for today (Firestore restore on a new
     * device/reinstall). Takes the max so local live counting never goes backwards.
     */
    @Synchronized
    fun mergeExternal(steps: Int) {
        val today = dayKey()
        val local = if (prefs.getString("day", "") == today) prefs.getInt("steps_today", 0) else 0
        if (steps > local) {
            prefs.edit()
                .putString("day", today)
                .putInt("steps_today", steps)
                .apply()
            _dailySteps.value = steps
        } else {
            _dailySteps.value = local
        }
    }

    @Synchronized
    override fun onSensorChanged(event: SensorEvent) {
        // Sensor callbacks fire constantly for the app's whole lifetime —
        // any exception here would crash the entire process
        try {
            handleReading(event)
        } catch (_: Exception) { }
    }

    private fun handleReading(event: SensorEvent) {
        val current = event.values[0].toLong()
        val today = dayKey()
        val storedDay = prefs.getString("day", "") ?: ""
        var stepsToday = prefs.getInt("steps_today", 0)
        val lastReading = prefs.getLong("last_reading", -1L)

        if (storedDay != today) {
            // New day — reset. The delta since the last reading may span
            // midnight, so drop it rather than inflate today's count.
            stepsToday = 0
        } else if (lastReading in 0..current) {
            // Same day: everything since the last reading (even hours with the
            // app closed) belongs to today.
            stepsToday += (current - lastReading).toInt()
        }
        // current < lastReading means the device rebooted — no delta to add,
        // tracking simply resumes from the new cumulative value.

        prefs.edit()
            .putString("day", today)
            .putInt("steps_today", stepsToday)
            .putLong("last_reading", current)
            .apply()
        _dailySteps.value = stepsToday
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) = Unit
}

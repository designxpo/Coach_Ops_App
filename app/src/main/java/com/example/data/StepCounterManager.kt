package com.example.data

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Wraps TYPE_STEP_COUNTER (hardware pedometer) to produce today's step count.
 *
 * Strategy: track the sensor delta from the moment this session starts
 * (sessionStart) and add it to the Firestore baseline set via setBaseline().
 * This survives reboots cleanly — after a reboot the cumulative sensor value
 * resets to 0, the delta just starts fresh from the new value, and whatever
 * was already saved to Firestore is preserved as the baseline.
 */
class StepCounterManager(context: Context) : SensorEventListener {

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val stepSensor =
        sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    private val _dailySteps = MutableStateFlow(0)
    val dailySteps: StateFlow<Int> = _dailySteps.asStateFlow()

    val isAvailable: Boolean get() = stepSensor != null

    private var sessionStartSensor = -1L
    private var sessionBaseline    = 0

    /** Call after loading today's step count from Firestore so we add on top of it. */
    fun setBaseline(stepsFromStorage: Int) {
        sessionBaseline = stepsFromStorage
        _dailySteps.value = stepsFromStorage
    }

    fun start() {
        stepSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        val totalSinceBoot = event.values[0].toLong()
        if (sessionStartSensor < 0) {
            sessionStartSensor = totalSinceBoot
        }
        val delta = (totalSinceBoot - sessionStartSensor).toInt().coerceAtLeast(0)
        _dailySteps.value = sessionBaseline + delta
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) = Unit
}

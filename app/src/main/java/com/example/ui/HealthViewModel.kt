package com.example.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.UserBodyStats
import com.example.data.CycleEntry
import com.example.data.DailyHealthLog
import com.example.data.HealthConnectManager
import com.example.data.HealthRepository
import com.example.data.ProgressPhoto
import com.example.data.StepCounterManager
import com.example.data.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HealthViewModel(
    private val repo: HealthRepository,
    private val stepCounter: StepCounterManager,
    private val healthConnect: HealthConnectManager? = null,
) : ViewModel() {

    private val _todayLog  = MutableStateFlow(DailyHealthLog())
    val todayLog: StateFlow<DailyHealthLog> = _todayLog.asStateFlow()

    private val _weekLogs  = MutableStateFlow<List<DailyHealthLog>>(emptyList())
    val weekLogs: StateFlow<List<DailyHealthLog>> = _weekLogs.asStateFlow()

    private val _measurements = MutableStateFlow<List<UserBodyStats>>(emptyList())
    val measurements: StateFlow<List<UserBodyStats>> = _measurements.asStateFlow()

    private val _photos = MutableStateFlow<List<ProgressPhoto>>(emptyList())
    val photos: StateFlow<List<ProgressPhoto>> = _photos.asStateFlow()

    private val _cycleEntries = MutableStateFlow<List<CycleEntry>>(emptyList())
    val cycleEntries: StateFlow<List<CycleEntry>> = _cycleEntries.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    val isStepCounterAvailable: Boolean get() = stepCounter.isAvailable

    init {
        loadAll()
        if (stepCounter.isAvailable) {
            stepCounter.start()
            viewModelScope.launch {
                stepCounter.dailySteps.collect { steps ->
                    val today = repo.todayKey()
                    // Midnight rollover with the app alive: never graft the new
                    // day's live count onto yesterday's log (it would overwrite
                    // yesterday's history AND inflate whichever doc it lands in)
                    val current = _todayLog.value.let {
                        if (it.date == today) it else DailyHealthLog(date = today)
                    }
                    if (steps > current.stepsCount) {
                        val log = current.copy(
                            stepsCount     = steps,
                            caloriesBurned = (steps * 0.04f).toInt()
                        )
                        _todayLog.value = log
                        // Persist every 50 steps to avoid excessive Firestore writes
                        if (steps > 0 && steps % 50 == 0) saveLog(log)
                    }
                }
            }
        }
    }

    /** Call right after ACTIVITY_RECOGNITION is granted — retries the (dead) registration. */
    fun restartStepCounter() = stepCounter.start()

    override fun onCleared() {
        super.onCleared()
        // Listener stays registered for the app's lifetime (singleton, negligible
        // battery — it's a hardware counter). Flush the final count — but only
        // under the log's OWN date. Re-stamping with todayKey() wrote last
        // night's step total into the NEW day's doc when the system killed the
        // app after midnight, which showed as a big fake count next morning.
        viewModelScope.launch {
            val log = _todayLog.value
            if (log.date == repo.todayKey()) {
                try { repo.saveLog(log) } catch (_: Exception) {}
            }
        }
    }

    /**
     * Pulls today's Health Connect steps (watch/band/Google Fit) into the local
     * counter so they show on the home dashboard — not only on the HC screen.
     * Must run BEFORE liveSteps is read: loadAll/refreshToday take
     * maxOf(cloud, liveSteps), so merging first is what makes HC steps flow
     * into todayLog.
     */
    private suspend fun mergeHealthConnectSteps() {
        val hc = healthConnect ?: return
        if (!hc.isAvailable() || !hc.hasStepsPermission()) return
        val steps = hc.readTodaySummary().stepsToday
        if (steps > 0) stepCounter.mergeExternal(steps.toInt())
    }

    private fun loadAll() {
        viewModelScope.launch {
            try {
                mergeHealthConnectSteps()
                val today = repo.todayKey()
                val log   = repo.getLog(today)
                // Merge cloud + live pedometer — never let a stale cloud zero
                // overwrite steps the sensor already counted (the banner and
                // the app must always agree)
                stepCounter.mergeExternal(log.stepsCount)
                val liveSteps = stepCounter.dailySteps.value
                _todayLog.value = log.copy(
                    stepsCount     = maxOf(log.stepsCount, liveSteps),
                    caloriesBurned = maxOf(log.caloriesBurned, (liveSteps * 0.04f).toInt())
                )
                _weekLogs.value     = repo.getLast7Days()
                _measurements.value = repo.getMeasurements()
                _photos.value       = repo.getProgressPhotos()
                _cycleEntries.value = repo.getCycleEntries()
            } catch (_: Exception) { }
        }
    }

    fun refreshToday() {
        viewModelScope.launch {
            try { mergeHealthConnectSteps() } catch (_: Exception) { }
            val log = repo.getLog(repo.todayKey())
            val liveSteps = stepCounter.dailySteps.value
            _todayLog.value = log.copy(
                stepsCount     = maxOf(log.stepsCount, liveSteps),
                caloriesBurned = maxOf(log.caloriesBurned, (liveSteps * 0.04f).toInt())
            )
            _weekLogs.value = repo.getLast7Days()
        }
    }

    // ── Daily log mutations ───────────────────────────────────────────────────

    fun setSteps(steps: Int) {
        val log = _todayLog.value.copy(
            stepsCount    = steps,
            caloriesBurned = (steps * 0.04f).toInt()
        )
        _todayLog.value = log
        saveLog(log)
    }

    fun addWater() {
        val log = _todayLog.value.copy(waterGlasses = (_todayLog.value.waterGlasses + 1).coerceAtMost(20))
        _todayLog.value = log
        saveLog(log)
    }

    fun removeWater() {
        val log = _todayLog.value.copy(waterGlasses = (_todayLog.value.waterGlasses - 1).coerceAtLeast(0))
        _todayLog.value = log
        saveLog(log)
    }

    fun setSleep(hours: Float) {
        val log = _todayLog.value.copy(sleepHours = hours)
        _todayLog.value = log
        saveLog(log)
    }

    fun setMood(rating: Int) {
        val log = _todayLog.value.copy(moodRating = rating)
        _todayLog.value = log
        saveLog(log)
    }

    private fun saveLog(log: DailyHealthLog) {
        viewModelScope.launch {
            repo.saveLog(log.copy(date = repo.todayKey()))
            _weekLogs.value = repo.getLast7Days()
        }
    }

    // ── Measurements ─────────────────────────────────────────────────────────

    fun saveMeasurement(m: UserBodyStats) {
        viewModelScope.launch {
            _isSaving.value = true
            repo.saveMeasurement(m.copy(date = if (m.date.isEmpty()) repo.todayKey() else m.date))
            _measurements.value = repo.getMeasurements()
            _isSaving.value = false
        }
    }

    fun deleteMeasurement(id: String) {
        viewModelScope.launch {
            repo.deleteMeasurement(id)
            _measurements.value = repo.getMeasurements()
        }
    }

    // ── Progress photos ───────────────────────────────────────────────────────

    fun saveProgressPhoto(localPath: String, notes: String = "") {
        viewModelScope.launch {
            repo.saveProgressPhoto(ProgressPhoto(
                date      = repo.todayKey(),
                localPath = localPath,
                notes     = notes
            ))
            _photos.value = repo.getProgressPhotos()
        }
    }

    fun deleteProgressPhoto(id: String) {
        viewModelScope.launch {
            repo.deleteProgressPhoto(id)
            _photos.value = repo.getProgressPhotos()
        }
    }

    // ── Cycle ─────────────────────────────────────────────────────────────────

    fun saveCycleEntry(entry: CycleEntry) {
        viewModelScope.launch {
            repo.saveCycleEntry(entry)
            _cycleEntries.value = repo.getCycleEntries()
        }
    }

    fun deleteCycleEntry(id: String) {
        viewModelScope.launch {
            repo.deleteCycleEntry(id)
            _cycleEntries.value = repo.getCycleEntries()
        }
    }

    // ── Weekly health score (0–100) ───────────────────────────────────────────
    fun weeklyHealthScore(): Int {
        val logs = _weekLogs.value.filter { it.moodRating > 0 || it.stepsCount > 0 || it.waterGlasses > 0 || it.sleepHours > 0 }
        if (logs.isEmpty()) return 0
        val stepsScore  = logs.map { (it.stepsCount.toDouble() / 10_000.0).coerceIn(0.0, 1.0) }.average()
        val waterScore  = logs.map { (it.waterGlasses.toDouble() / 8.0).coerceIn(0.0, 1.0) }.average()
        val sleepScore  = logs.map { if (it.sleepHours in 7f..9f) 1.0 else (it.sleepHours / 8.0).coerceIn(0.0, 1.0) }.average()
        val moodScore   = logs.map { (it.moodRating.toDouble() / 5.0).coerceIn(0.0, 1.0) }.average()
        val consistency = logs.size.toFloat() / 7f
        return ((stepsScore * 0.25 + waterScore * 0.25 + sleepScore * 0.25 + moodScore * 0.15 + consistency * 0.10) * 100).toInt()
    }
}

class HealthViewModelFactory(
    private val prefs: UserPreferences,
    private val context: Context,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val uid = prefs.userId.ifEmpty { "anonymous" }
        @Suppress("UNCHECKED_CAST")
        return HealthViewModel(
            HealthRepository(uid),
            StepCounterManager.getInstance(context.applicationContext),
            HealthConnectManager(context.applicationContext),
        ) as T
    }
}

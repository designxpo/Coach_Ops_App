package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.ClientGoal
import com.example.data.Exercise
import com.example.data.ExerciseCategory
import com.example.data.ExerciseRepository
import com.example.data.FitnessGoalEntry
import com.example.data.FitnessSync
import com.example.data.SetDetail
import com.example.data.HealthCalculator
import com.example.data.HealthMetrics
import com.example.data.HealthProfile
import com.example.data.HealthRecord
import com.example.data.HealthSync
import com.example.data.ProfileSync
import com.example.data.IndianMealPlan
import com.example.data.MuscleGroup
import com.example.data.FirestoreSync
import com.example.data.NutritionRepository
import com.example.data.UserPreferences
import com.example.data.WorkoutLogEntry
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class FitnessViewModel(private val userPreferences: UserPreferences) : ViewModel() {

    // ─── Live exercise library (Firestore → merges onto hardcoded) ───────────
    // Starts with hardcoded data immediately so the UI is never empty.
    private val _allExercises = MutableStateFlow<List<Exercise>>(ExerciseRepository.all)
    val allExercises: StateFlow<List<Exercise>> = _allExercises.asStateFlow()

    private val _categoryFilter = MutableStateFlow<ExerciseCategory?>(null)
    val categoryFilter: StateFlow<ExerciseCategory?> = _categoryFilter.asStateFlow()

    private val _muscleFilter = MutableStateFlow<MuscleGroup?>(null)
    val muscleFilter: StateFlow<MuscleGroup?> = _muscleFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val filteredExercises: StateFlow<List<Exercise>> = MutableStateFlow(ExerciseRepository.all).also {
        viewModelScope.launch {
            kotlinx.coroutines.flow.combine(
                _allExercises, _categoryFilter, _muscleFilter, _searchQuery
            ) { allEx, cat, muscle, query ->
                allEx.filter { ex ->
                    (cat == null || ex.category == cat) &&
                    (muscle == null || muscle in ex.primaryMuscles || muscle in ex.secondaryMuscles) &&
                    (query.isBlank() || ex.name.contains(query, ignoreCase = true))
                }
            }.collect { list -> (it as MutableStateFlow).value = list }
        }
    }

    fun setCategoryFilter(cat: ExerciseCategory?) { _categoryFilter.value = cat }
    fun setMuscleFilter(m: MuscleGroup?) { _muscleFilter.value = m }
    fun setSearchQuery(q: String) { _searchQuery.value = q }

    // ─── Detail ───────────────────────────────────────────────────────────────
    private val _selectedExercise = MutableStateFlow<Exercise?>(null)
    val selectedExercise: StateFlow<Exercise?> = _selectedExercise.asStateFlow()

    fun selectExercise(id: String) {
        // Use the live merged list so admin image updates are reflected immediately
        _selectedExercise.value = _allExercises.value.find { it.id == id }
            ?: ExerciseRepository.byId(id)
    }

    // ─── Nutrition ────────────────────────────────────────────────────────────
    val clientName: String
        get() = userPreferences.clientName.ifEmpty { userPreferences.coachName }

    val profilePhotoUrl: StateFlow<String> = userPreferences.profilePhotoFlow

    val clientGoal: ClientGoal
        get() {
            val label = userPreferences.clientGoal
            return ClientGoal.entries.find { it.label == label } ?: ClientGoal.GENERAL_FITNESS
        }

    // Starts with hardcoded data; replaced by Firestore data when admin edits a plan
    private val _mealPlan = MutableStateFlow<IndianMealPlan?>(NutritionRepository.forGoal(
        ClientGoal.entries.find { it.label == userPreferences.clientGoal } ?: ClientGoal.GENERAL_FITNESS
    ))
    val mealPlan: StateFlow<IndianMealPlan?> = _mealPlan.asStateFlow()

    // ─── Health metrics ───────────────────────────────────────────────────────
    var healthProfile: HealthProfile = userPreferences.loadHealthProfile(clientGoal)
        private set

    var lastMetrics: HealthMetrics? = HealthCalculator.calculate(healthProfile)
        private set

    private val _healthRecords = MutableStateFlow<List<HealthRecord>>(emptyList())
    val healthRecords: StateFlow<List<HealthRecord>> = _healthRecords.asStateFlow()

    private val _isSavingHealth = MutableStateFlow(false)
    val isSavingHealth: StateFlow<Boolean> = _isSavingHealth.asStateFlow()

    // ─── Goals & tracking ────────────────────────────────────────────────────
    private val _goals = MutableStateFlow<List<FitnessGoalEntry>>(emptyList())
    val goals: StateFlow<List<FitnessGoalEntry>> = _goals.asStateFlow()

    private val _logs = MutableStateFlow<List<WorkoutLogEntry>>(emptyList())
    val logs: StateFlow<List<WorkoutLogEntry>> = _logs.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow("")
    val error: StateFlow<String> = _error.asStateFlow()

    // Real-time Firestore listeners — auto-update UI whenever data changes
    private var goalsListener: ListenerRegistration? = null
    private var logsListener: ListenerRegistration? = null
    private var healthRecordsListener: ListenerRegistration? = null
    private var exerciseLibraryListener: ListenerRegistration? = null
    private var nutritionPlanListener: ListenerRegistration? = null
    private var myDietPlansListener: ListenerRegistration? = null

    private val _myDietPlans = MutableStateFlow<List<com.example.data.DietPlan>>(emptyList())
    val myDietPlans: StateFlow<List<com.example.data.DietPlan>> = _myDietPlans.asStateFlow()

    private val _myDietLogs = MutableStateFlow<List<com.example.data.DietLog>>(emptyList())
    val myDietLogs: StateFlow<List<com.example.data.DietLog>> = _myDietLogs.asStateFlow()
    private var myDietLogsListener: ListenerRegistration? = null

    init {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (!uid.isNullOrEmpty()) {
            goalsListener = FitnessSync.listenGoals(uid) { _goals.value = it }
            logsListener  = FitnessSync.listenLogs(uid)  { _logs.value = it }
            healthRecordsListener = HealthSync.listenMyRecords(uid) { _healthRecords.value = it }
            myDietPlansListener = FirestoreSync.listenMyDietPlans { plans ->
                _myDietPlans.value = plans
                plans.firstOrNull()?.let { loadMyDietLogs(it.id) }
            }
        }
        // Listen to admin-panel changes — runs for both coach and member
        exerciseLibraryListener = FirestoreSync.listenExercises { merged ->
            _allExercises.value = merged
            // If a detail screen is open, refresh it too
            _selectedExercise.value?.let { current ->
                _selectedExercise.value = merged.find { it.id == current.id } ?: current
            }
        }
        nutritionPlanListener = FirestoreSync.listenNutritionPlan(clientGoal) { fsPlan ->
            _mealPlan.value = fsPlan ?: NutritionRepository.forGoal(clientGoal)
        }
    }

    override fun onCleared() {
        super.onCleared()
        goalsListener?.remove()
        logsListener?.remove()
        healthRecordsListener?.remove()
        exerciseLibraryListener?.remove()
        nutritionPlanListener?.remove()
        myDietPlansListener?.remove()
        myDietLogsListener?.remove()
    }

    fun loadMyDietLogs(planId: String) {
        myDietLogsListener?.remove()
        myDietLogsListener = FirestoreSync.listenDietLogs(planId) { _myDietLogs.value = it }
    }

    fun saveDietLog(log: com.example.data.DietLog) {
        FirestoreSync.saveDietLog(log)
    }

    fun calculateMetrics(profile: HealthProfile) {
        userPreferences.saveHealthProfile(profile)
        healthProfile = profile
        lastMetrics = HealthCalculator.calculate(profile)
    }

    fun saveHealthRecord(profile: HealthProfile, metrics: HealthMetrics) {
        viewModelScope.launch {
            _isSavingHealth.value = true
            try {
                val record = HealthCalculator.toRecord(profile, metrics)
                HealthSync.saveRecord(record)
                ProfileSync.saveHealthProfile(userPreferences)
                // listener auto-refreshes _healthRecords
            } catch (e: Exception) {
                _error.value = e.message ?: "Error saving health record"
            } finally {
                _isSavingHealth.value = false
            }
        }
    }

    suspend fun loadMemberHealthRecords(memberId: String): List<HealthRecord> =
        HealthSync.getMemberRecords(memberId)

    fun saveGoal(exerciseId: String, exerciseName: String, sets: Int, reps: Int, weightKg: Float = 0f) {
        viewModelScope.launch {
            try {
                val goal = FitnessGoalEntry(exerciseId, exerciseName, sets, reps, weightKg)
                FitnessSync.saveGoal(goal)
                // listener auto-refreshes _goals
            } catch (e: Exception) { _error.value = e.message ?: "Error saving goal" }
        }
    }

    fun deleteGoal(exerciseId: String) {
        viewModelScope.launch {
            try {
                FitnessSync.deleteGoal(exerciseId)
                _goals.value = _goals.value.filter { it.exerciseId != exerciseId }
            } catch (e: Exception) { _error.value = e.message ?: "Error deleting goal" }
        }
    }

    fun logWorkout(
        exerciseId: String,
        exerciseName: String,
        setDetails: List<SetDetail>,
        notes: String
    ) {
        val working: List<SetDetail> = setDetails.filter { s -> !s.isWarmup }
        val heaviest: SetDetail? = working.maxByOrNull { s -> s.weightKg }
        viewModelScope.launch {
            try {
                val entry = WorkoutLogEntry(
                    id            = UUID.randomUUID().toString(),
                    exerciseId    = exerciseId,
                    exerciseName  = exerciseName,
                    dateMillis    = System.currentTimeMillis(),
                    setsCompleted = working.size,
                    repsCompleted = heaviest?.reps ?: 0,
                    weightKg      = heaviest?.weightKg ?: 0f,
                    notes         = notes,
                    setDetails    = setDetails
                )
                FitnessSync.saveLog(entry)
                // real-time listener will update _logs automatically
            } catch (e: Exception) { _error.value = e.message ?: "Error saving log" }
        }
    }

    // Calendar heatmap: returns Map<dateString "yyyy-MM-dd" → count>
    fun workoutDayMap(): Map<String, Int> {
        val fmt = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        return _logs.value
            .groupBy { fmt.format(java.util.Date(it.dateMillis)) }
            .mapValues { it.value.size }
    }

    // Progress per exercise: returns list of (dateMillis, reps) sorted ascending
    fun progressForExercise(exerciseId: String): List<Pair<Long, Int>> =
        _logs.value
            .filter { it.exerciseId == exerciseId }
            .sortedBy { it.dateMillis }
            .map { it.dateMillis to it.repsCompleted }

    fun clearError() { _error.value = "" }
}

class FitnessViewModelFactory(private val prefs: UserPreferences) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = FitnessViewModel(prefs) as T
}

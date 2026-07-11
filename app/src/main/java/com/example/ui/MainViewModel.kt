package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppControlManager
import com.example.data.AppControlState
import com.example.data.Booking
import com.example.data.FirestoreSync
import com.example.data.ProfileSync
import com.example.data.AppFeature
import com.example.data.BodyMeasurement
import com.example.data.Client
import com.example.data.ClientNote
import com.example.data.CoachRepository
import com.example.data.FeatureFlagManager
import com.example.data.Payment
import com.example.data.Program
import com.example.data.RevenueSnapshot
import com.example.data.Signal
import com.example.data.SubscriptionPlan
import com.example.data.TrainerProfile
import com.example.data.UserPreferences
import com.example.data.WorkoutLog
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class BusinessInsight(
    val emoji: String,
    val title: String,
    val body: String,
    val isAlert: Boolean = false
)

data class BusinessMetrics(
    val retentionRate: Int = 0,
    val retainedCount: Int = 0,
    val churnRiskCount: Int = 0,
    val churnRiskRevenue: Int = 0,
    val atRiskRevenue: Int = 0,
    val atRiskCount: Int = 0,
    val avgConsistency: Int = 0,
    val failedPaymentRevenue: Int = 0,
    val insights: List<BusinessInsight> = emptyList()
)

data class FlagsState(val flags: Map<AppFeature, Boolean> = AppFeature.entries.associateWith { it.defaultEnabled })

class MainViewModel(
    private val repository: CoachRepository,
    private val userPreferences: UserPreferences,
    private val featureFlagManager: FeatureFlagManager
) : ViewModel() {

    private val _appControl = MutableStateFlow(AppControlState())
    val appControl: StateFlow<AppControlState> = _appControl.asStateFlow()

    private val _remoteConfig = MutableStateFlow(com.example.data.RemoteConfig())
    val remoteConfig: StateFlow<com.example.data.RemoteConfig> = _remoteConfig.asStateFlow()

    private val _isSuspended = MutableStateFlow(false)
    val isSuspended: StateFlow<Boolean> = _isSuspended.asStateFlow()

    // Real-time admin listeners — keep refs to cancel in onCleared()
    private var appControlListener:  ListenerRegistration? = null
    private var remoteConfigListener: ListenerRegistration? = null
    private var featureFlagsListener: ListenerRegistration? = null
    private var userRecordListener:   ListenerRegistration? = null

    // ─── Diet Plans ───────────────────────────────────────────────────────────
    private val _editingDietPlan = MutableStateFlow<com.example.data.DietPlan?>(null)
    val editingDietPlan: StateFlow<com.example.data.DietPlan?> = _editingDietPlan.asStateFlow()
    fun setEditingDietPlan(plan: com.example.data.DietPlan?) { _editingDietPlan.value = plan }

    private val _clientDietPlans = MutableStateFlow<List<com.example.data.DietPlan>>(emptyList())
    val clientDietPlans: StateFlow<List<com.example.data.DietPlan>> = _clientDietPlans.asStateFlow()
    private val _planDietLogs = MutableStateFlow<List<com.example.data.DietLog>>(emptyList())
    val planDietLogs: StateFlow<List<com.example.data.DietLog>> = _planDietLogs.asStateFlow()
    private var dietPlansListener: ListenerRegistration? = null
    private var dietLogsListener: ListenerRegistration? = null

    init {
        // Real-time listeners for admin panel changes (maintenance, flags, remote config)
        appControlListener = AppControlManager.listenAppControl { state ->
            _appControl.value = state
        }
        remoteConfigListener = AppControlManager.listenRemoteConfig { config ->
            _remoteConfig.value = config
        }
        featureFlagsListener = AppControlManager.listenFeatureFlags { fsFlags ->
            // Apply every flag from Firestore into local FeatureFlagManager
            AppFeature.entries.forEach { feature ->
                val value = fsFlags[feature.key]
                if (value != null) featureFlagManager.setEnabled(feature, value)
            }
            _flagsState.value = FlagsState(featureFlagManager.allFlags())
        }

        // Watch for admin suspension and plan changes on this user's record
        val currentUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        if (currentUid != null) {
            userRecordListener = AppControlManager.listenCurrentUser(currentUid) { suspended, planStr ->
                _isSuspended.value = suspended
                if (!suspended) {
                    val plan = com.example.data.SubscriptionPlan.entries.find { it.name == planStr }
                    if (plan != null && plan != _currentPlan.value) {
                        userPreferences.subscriptionPlan = plan.name
                        _currentPlan.value = plan
                    }
                }
            }
        }

        viewModelScope.launch {
            // Remove legacy consistency-based signals (replaced by payment-based churn)
            repository.purgeLegacySignals()

            // Pull cloud data into Room on new install or new device
            repository.syncFromFirestoreIfEmpty()

            val clients = repository.allClients.first()
            val payments = repository.pendingPayments.first()
            val programs = repository.masterPrograms.first()
            // Auto-advance program weeks based on enrollment date
            repository.advanceProgramWeeks(clients, programs)
            // Recompute signals after possible week advancement
            val updatedClients = repository.allClients.first()
            repository.recomputeSignals(updatedClients, payments)
            // Save this month's revenue snapshot
            val totalMrr = updatedClients.sumOf { it.mrr }
            repository.saveRevenueSnapshot(totalMrr)

            // Push real aggregate stats to user_records so admin dashboard shows live numbers
            val sessionCount = repository.totalCompletedSessions.first()
            FirestoreSync.updateAggregates(updatedClients.size, totalMrr, sessionCount)
        }
    }

    override fun onCleared() {
        super.onCleared()
        appControlListener?.remove()
        remoteConfigListener?.remove()
        featureFlagsListener?.remove()
        userRecordListener?.remove()
        dietPlansListener?.remove()
        dietLogsListener?.remove()
    }

    val clients: StateFlow<List<Client>> = repository.allClients
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val signals: StateFlow<List<Signal>> = repository.unresolvedSignals
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val programs: StateFlow<List<Program>> = repository.masterPrograms
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val payments: StateFlow<List<Payment>> = repository.pendingPayments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val revenueSnapshots: StateFlow<List<RevenueSnapshot>> = repository.recentSnapshots
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalCompletedSessions: StateFlow<Int> = repository.totalCompletedSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Set of clientIds that already have a session logged today
    private val todayStartMs: Long = run {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        cal.timeInMillis
    }
    val clientsLoggedToday: StateFlow<Set<String>> = repository.getClientIdsWithLogSince(todayStartMs)
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    fun getSessionsSince(since: Long) = repository.getSessionsSince(since)

    val sparklineBars: StateFlow<List<Float>> = run {
        val tenDaysAgo = System.currentTimeMillis() - 10 * 86400000L
        repository.getSessionDatesSince(tenDaysAgo)
            .map { dates ->
                val counts = (0 until 10).map { i ->
                    val dayStart = tenDaysAgo + i * 86400000L
                    val dayEnd = dayStart + 86400000L
                    dates.count { it in dayStart until dayEnd }.toFloat()
                }
                val max = counts.maxOrNull()?.takeIf { it > 0f } ?: 1f
                counts.map { (it / max).coerceAtLeast(0.05f) }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), List(10) { 0.05f })
    }

    private val _currentPlan = MutableStateFlow(userPreferences.currentPlan)
    val currentPlan: StateFlow<SubscriptionPlan> = _currentPlan.asStateFlow()

    private val _flagsState = MutableStateFlow(FlagsState(featureFlagManager.allFlags()))
    val flagsState: StateFlow<FlagsState> = _flagsState.asStateFlow()

    fun isFeatureEnabled(feature: AppFeature): Boolean = featureFlagManager.isEnabled(feature)

    fun updateFlag(feature: AppFeature, enabled: Boolean) {
        featureFlagManager.setEnabled(feature, enabled)
        _flagsState.value = FlagsState(featureFlagManager.allFlags())
    }

    fun updatePlan(plan: SubscriptionPlan) {
        userPreferences.subscriptionPlan = plan.name
        _currentPlan.value = plan
    }

    fun updateAdminPin(newPin: String) {
        userPreferences.adminPin = newPin
    }

    fun checkAdminPin(pin: String) = pin == userPreferences.adminPin

    fun getPlanPrice(plan: SubscriptionPlan): Int = when (plan) {
        SubscriptionPlan.STARTER  -> userPreferences.planPriceStarter
        SubscriptionPlan.PRO      -> userPreferences.planPricePro
        SubscriptionPlan.BUSINESS -> userPreferences.planPriceBusiness
    }

    fun updatePlanPrice(plan: SubscriptionPlan, price: Int) {
        when (plan) {
            SubscriptionPlan.STARTER  -> userPreferences.planPriceStarter  = price
            SubscriptionPlan.PRO      -> userPreferences.planPricePro       = price
            SubscriptionPlan.BUSINESS -> userPreferences.planPriceBusiness  = price
        }
    }

    val businessMetrics: StateFlow<BusinessMetrics> = combine(
        repository.allClients,
        repository.pendingPayments
    ) { clientList, paymentList ->
        computeBusinessMetrics(clientList, paymentList)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BusinessMetrics())

    private val _coachName      = MutableStateFlow(userPreferences.coachName)
    val coachName: StateFlow<String>      = _coachName.asStateFlow()

    private val _coachPhone     = MutableStateFlow(userPreferences.coachPhone)
    val coachPhone: StateFlow<String>     = _coachPhone.asStateFlow()

    private val _coachSpecialty = MutableStateFlow(userPreferences.coachSpecialty)
    val coachSpecialty: StateFlow<String> = _coachSpecialty.asStateFlow()

    // Parsed list — UI uses this for multi-select chips
    val coachSpecialties: StateFlow<List<String>> = MutableStateFlow(userPreferences.coachSpecialties).also { flow ->
        viewModelScope.launch {
            _coachSpecialty.collect { csv ->
                (flow as MutableStateFlow).value = csv.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            }
        }
    }

    // Per-client data flows — callers collect these directly
    fun getWorkoutLogsFlow(clientId: String) = repository.getWorkoutLogsForClient(clientId)
    fun getWorkoutLogsForProgramFlow(programId: String) = repository.getWorkoutLogsForProgram(programId)
    fun getMeasurementsFlow(clientId: String) = repository.getMeasurementsForClient(clientId)
    fun getNotesFlow(clientId: String) = repository.getNotesForClient(clientId)

    private fun computeBusinessMetrics(clients: List<Client>, payments: List<Payment>): BusinessMetrics {
        if (clients.isEmpty()) return BusinessMetrics()

        val now = System.currentTimeMillis()
        val eligibleClients  = clients.filter { (now - it.enrollmentDateMillis) >= 28L * 86400000L }
        val retainedClients  = eligibleClients.filter { it.status == "Active" }
        val retentionRate    = if (eligibleClients.isEmpty()) 0 else (retainedClients.size * 100) / eligibleClients.size
        val avgConsistency   = clients.map { it.consistencyScore }.average().toInt()
        val milestoneClients = clients.filter { it.programWeek > 0 && it.programWeek % 4 == 0 }
        val sweetSpotClients = clients.filter { it.programWeek in 6..10 }

        // Churn risk = payment overdue 14+ days or payment failed
        val churnRiskClients = clients.filter { client ->
            payments.any { it.clientId == client.id && (it.daysOverdue > 14 || it.mandateStatus == "FAILED") }
        }
        // At risk = payment overdue 7–14 days (not already churn risk)
        val atRiskClients = clients.filter { client ->
            client !in churnRiskClients &&
            payments.any { it.clientId == client.id && it.daysOverdue in 7..14 }
        }
        // Risk MRR = clients with overdue payments only
        val churnRiskRevenue = churnRiskClients.sumOf { it.mrr }
        val atRiskRevenue    = (churnRiskClients + atRiskClients).sumOf { it.mrr }
        val failedRev         = payments.filter { it.mandateStatus in listOf("FAILED", "EXPIRING") }.sumOf { it.amount }

        val insights = mutableListOf<BusinessInsight>()

        if (churnRiskClients.isNotEmpty()) {
            val amt = if (churnRiskRevenue >= 1000) "₹${churnRiskRevenue / 1000}K" else "₹$churnRiskRevenue"
            insights += BusinessInsight(
                "⚠️",
                "${churnRiskClients.size} client${if (churnRiskClients.size > 1) "s" else ""} at churn risk",
                "$amt MRR at stake — reach out this week before they drop off",
                isAlert = true
            )
        }

        if (milestoneClients.isNotEmpty()) {
            val maxWeek = milestoneClients.maxOf { it.programWeek }
            insights += BusinessInsight(
                "🎯",
                "${milestoneClients.size} client${if (milestoneClients.size > 1) "s" else ""} hit a milestone",
                "Week $maxWeek complete — a personal shoutout now lifts 30-day retention by ~40%",
                isAlert = false
            )
        }

        if (sweetSpotClients.isNotEmpty()) {
            insights += BusinessInsight(
                "💎",
                "${sweetSpotClients.size} client${if (sweetSpotClients.size > 1) "s" else ""} in the retention sweet spot",
                "Weeks 6–10 have the highest upsell conversion — ideal time for a progress review call",
                isAlert = false
            )
        }

        if (eligibleClients.isNotEmpty()) {
            val retentionMsg = if (retentionRate >= 65)
                "Above the 65% industry benchmark — your roster is sticky 🚀"
            else
                "${eligibleClients.size - retainedClients.size} of ${eligibleClients.size} long-term client${if (eligibleClients.size > 1) "s" else ""} need attention"
            insights += BusinessInsight(
                if (retentionRate >= 65) "📈" else "📊",
                "Retention: $retentionRate% (${retainedClients.size} of ${eligibleClients.size})",
                retentionMsg,
                isAlert = retentionRate < 50
            )
        } else if (clients.isNotEmpty()) {
            insights += BusinessInsight(
                "📊",
                "Building baseline",
                "${clients.size} client${if (clients.size > 1) "s" else ""} in the onboarding window — retention tracked after 4 weeks",
                isAlert = false
            )
        }

        return BusinessMetrics(
            retentionRate        = retentionRate,
            retainedCount        = retainedClients.size,
            churnRiskCount       = churnRiskClients.size,
            churnRiskRevenue     = churnRiskRevenue,
            atRiskRevenue        = atRiskRevenue,
            atRiskCount          = churnRiskClients.size + atRiskClients.size,
            avgConsistency       = avgConsistency,
            failedPaymentRevenue = failedRev,
            insights             = insights
        )
    }

    fun resolveSignal(signalId: String) {
        viewModelScope.launch { repository.resolveSignal(signalId) }
    }

    fun resolveAllSignals() {
        viewModelScope.launch { repository.resolveAllSignals() }
    }

    fun getProgramFlow(id: String) = repository.getProgram(id)
    fun getClientsForProgramFlow(programId: String) = repository.getClientsForProgram(programId)

    fun updateProgram(program: Program) {
        viewModelScope.launch { repository.updateProgram(program) }
    }

    fun cloneProgram(program: Program) {
        viewModelScope.launch { repository.cloneProgram(program) }
    }

    fun addProgramFromTemplate(name: String, weeks: Int, daysPerWeek: Int, tags: String, workingDays: String = "") {
        viewModelScope.launch {
            repository.insertProgram(
                Program(java.util.UUID.randomUUID().toString(), name, weeks, daysPerWeek, tags, clientCount = 0, workingDays = workingDays)
            )
        }
    }

    fun addClient(name: String, phone: String, goal: String, mrr: Int, programId: String?, paymentCycle: String = "MONTHLY", city: String = "", trainingStartDateMillis: Long = 0L, trainingEndDateMillis: Long = 0L) {
        viewModelScope.launch {
            val clientId = java.util.UUID.randomUUID().toString()
            repository.insertClient(
                Client(clientId, name, phone, goal.ifBlank { "General Fitness" },
                    programId = programId, mrr = mrr, status = "Active",
                    enrollmentDateMillis = System.currentTimeMillis(), city = city,
                    paymentCycle = paymentCycle,
                    trainingStartDateMillis = trainingStartDateMillis,
                    trainingEndDateMillis = trainingEndDateMillis)
            )
            if (mrr > 0) {
                val cycleDays = when (paymentCycle) {
                    "WEEKLY"    ->  7L
                    "QUARTERLY" -> 90L
                    "YEARLY"    -> 365L
                    else        -> 30L
                }
                repository.insertPayment(
                    Payment(java.util.UUID.randomUUID().toString(), clientId, mrr,
                        System.currentTimeMillis() + cycleDays * 86400000L, 0, "ACTIVE",
                        paymentCycle = paymentCycle)
                )
            }
            pushAggregates()
        }
    }

    fun markAttendanceToday(client: Client, program: Program?) {
        viewModelScope.launch {
            val dateLabel = java.text.SimpleDateFormat("MMM d", java.util.Locale.getDefault())
                .format(java.util.Date())
            val log = com.example.data.WorkoutLog(
                id = java.util.UUID.randomUUID().toString(),
                clientId = client.id,
                programId = client.programId,
                sessionName = "Session · $dateLabel",
                sessionDateMillis = System.currentTimeMillis()
            )
            repository.insertWorkoutLog(log)
            val newScore = repository.recalculateConsistency(client, program)
            repository.updateClient(client.copy(consistencyScore = newScore,
                lastCheckInMillis = System.currentTimeMillis()))
            val payments = repository.pendingPayments.first()
            val allClients = repository.allClients.first()
            repository.recomputeSignals(allClients, payments)
            pushAggregates()
        }
    }

    fun updateClient(client: Client) {
        viewModelScope.launch { repository.updateClient(client) }
    }

    fun deleteClient(clientId: String) {
        viewModelScope.launch { repository.deleteClient(clientId); pushAggregates() }
    }

    fun toggleClientPause(client: Client) {
        viewModelScope.launch {
            val isPausing = client.status != "Paused"
            repository.updateClient(client.copy(
                status = if (isPausing) "Paused" else "Active",
                pausedAtMillis = if (isPausing) System.currentTimeMillis() else 0L
            ))
        }
    }

    fun assignProgram(clientId: String, programId: String) {
        viewModelScope.launch { repository.assignProgram(clientId, programId) }
    }

    fun logCheckIn(client: Client) {
        viewModelScope.launch {
            val updated = client.copy(lastCheckInMillis = System.currentTimeMillis())
            repository.updateClient(updated)
            val payments = repository.pendingPayments.first()
            val allClients = repository.allClients.first()
            repository.recomputeSignals(allClients, payments)
        }
    }

    fun logSession(log: WorkoutLog, client: Client, program: Program?) {
        viewModelScope.launch {
            repository.insertWorkoutLog(log)
            val newScore = repository.recalculateConsistency(client, program)
            repository.updateClient(client.copy(consistencyScore = newScore))
            val payments = repository.pendingPayments.first()
            val allClients = repository.allClients.first()
            repository.recomputeSignals(allClients, payments)
            pushAggregates()
        }
    }

    private suspend fun pushAggregates() {
        val clients = repository.allClients.first()
        val mrr = clients.sumOf { it.mrr }
        val sessions = repository.totalCompletedSessions.first()
        FirestoreSync.updateAggregates(clients.size, mrr, sessions)
    }

    fun logDay(client: Client, program: Program?, dateMillis: Long, type: String, notes: String) {
        viewModelScope.launch {
            val dateLabel = java.text.SimpleDateFormat("MMM d", java.util.Locale.getDefault())
                .format(java.util.Date(dateMillis))
            val log = WorkoutLog(
                id = java.util.UUID.randomUUID().toString(),
                clientId = client.id,
                programId = client.programId,
                sessionDateMillis = dateMillis,
                sessionName = when (type) {
                    "COMPLETED" -> "Session · $dateLabel"
                    "LEAVE"     -> "Leave · $dateLabel"
                    else        -> "Absent · $dateLabel"
                },
                isMissed = type != "COMPLETED",
                missedReason = when (type) {
                    "LEAVE"  -> if (notes.isNotBlank()) "LEAVE: $notes" else "LEAVE"
                    "ABSENT" -> if (notes.isNotBlank()) "ABSENT: $notes" else "ABSENT"
                    else     -> ""
                },
                notes = if (type == "COMPLETED") notes else ""
            )
            repository.insertWorkoutLog(log)
            if (type == "COMPLETED") {
                val newScore = repository.recalculateConsistency(client, program)
                repository.updateClient(client.copy(consistencyScore = newScore,
                    lastCheckInMillis = System.currentTimeMillis()))
            }
            val payments = repository.pendingPayments.first()
            val allClients = repository.allClients.first()
            repository.recomputeSignals(allClients, payments)
            pushAggregates()
        }
    }

    fun logMissedSession(client: Client, program: Program?, reason: String) {
        viewModelScope.launch {
            val log = WorkoutLog(
                id = java.util.UUID.randomUUID().toString(),
                clientId = client.id,
                programId = client.programId,
                sessionName = "Missed Session",
                isMissed = true,
                missedReason = reason
            )
            repository.insertWorkoutLog(log)
            // Recalculate (missed sessions don't count as completed)
            val newScore = repository.recalculateConsistency(client, program)
            repository.updateClient(client.copy(consistencyScore = newScore))
            val payments = repository.pendingPayments.first()
            val allClients = repository.allClients.first()
            repository.recomputeSignals(allClients, payments)
        }
    }

    fun logMeasurement(measurement: BodyMeasurement) {
        viewModelScope.launch { repository.insertMeasurement(measurement) }
    }

    fun addNote(note: ClientNote) {
        viewModelScope.launch { repository.insertNote(note) }
    }

    fun deleteNote(noteId: String) {
        viewModelScope.launch { repository.deleteNote(noteId) }
    }

    fun updateProfile(name: String, email: String, phone: String, specialty: String,
                      clientRange: String = "", challenge: String = "") {
        userPreferences.coachName        = name
        userPreferences.coachEmail       = email
        userPreferences.coachPhone       = phone
        userPreferences.coachSpecialty   = specialty
        userPreferences.coachClientRange = clientRange
        userPreferences.coachChallenge   = challenge
        _coachName.value      = name
        _coachPhone.value     = phone
        _coachSpecialty.value = specialty
        viewModelScope.launch { ProfileSync.saveCoachProfile(userPreferences) }
    }

    fun saveOnboardingData(name: String, phone: String, specialties: List<String>, clientRange: String, challenge: String) {
        val specialty = specialties.joinToString(",")
        userPreferences.coachName          = name
        userPreferences.coachPhone         = phone
        userPreferences.coachSpecialty     = specialty
        userPreferences.coachClientRange   = clientRange
        userPreferences.coachChallenge     = challenge
        userPreferences.onboardingComplete = true
        _coachName.value      = name
        _coachPhone.value     = phone
        _coachSpecialty.value = specialty
        viewModelScope.launch { ProfileSync.saveCoachProfile(userPreferences) }
    }

    // Reactive — updates instantly when photo is uploaded
    val profilePhotoUrl = userPreferences.profilePhotoFlow

    fun refreshProfileFromPrefs() {
        _coachName.value      = userPreferences.coachName
        _coachPhone.value     = userPreferences.coachPhone
        _coachSpecialty.value = userPreferences.coachSpecialty
    }

    // Called after login/Google sign-in to restore Room from Firestore if empty
    fun syncFromCloud() {
        viewModelScope.launch { repository.syncFromFirestoreIfEmpty() }
    }

    fun loadDietPlansForClient(clientId: String) {
        dietPlansListener?.remove()
        val coachUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return
        dietPlansListener = FirestoreSync.listenDietPlansForClient(coachUid, clientId) { plans ->
            _clientDietPlans.value = plans
        }
    }

    fun loadDietLogs(planId: String) {
        dietLogsListener?.remove()
        dietLogsListener = FirestoreSync.listenDietLogs(planId) { logs ->
            _planDietLogs.value = logs
        }
    }

    fun saveDietPlan(plan: com.example.data.DietPlan) {
        FirestoreSync.saveDietPlan(plan)
    }

    fun deleteDietPlan(planId: String) {
        FirestoreSync.deleteDietPlan(planId)
        _clientDietPlans.value = _clientDietPlans.value.filter { it.id != planId }
    }

    fun logout(gymRepository: com.example.data.GymRepository? = null) {
        com.example.data.EntitlementManager.stop()
        viewModelScope.launch {
            FirestoreSync.clearFcmToken()    // before signOut — needs auth to write
            com.example.data.AuthRepository.signOut()
            repository.clearAllLocalData()   // await Room wipe before clearing prefs
            gymRepository?.clearAllLocalData()
            userPreferences.clearLocalSession()
        }
    }

    // ─── Coach Marketplace ────────────────────────────────────────────────────

    // publish state: null = idle, true = success, false = error
    private val _publishState = MutableStateFlow<Boolean?>(null)
    val publishState: StateFlow<Boolean?> = _publishState.asStateFlow()
    fun clearPublishState() { _publishState.value = null }

    private val _coachBookings = MutableStateFlow<List<Booking>>(emptyList())
    val coachBookings: StateFlow<List<Booking>> = _coachBookings.asStateFlow()

    fun loadCoachBookings() {
        viewModelScope.launch {
            val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
            try {
                _coachBookings.value = FirestoreSync.getCoachBookings(uid)
                    .sortedByDescending { it.createdAtMillis }
            } catch (_: Exception) {
                _coachBookings.value = emptyList()
            }
        }
    }

    /** Returns true if cancelled, false if within 24hr window */
    suspend fun cancelBooking(bookingId: String): Boolean {
        val name = userPreferences.coachName.ifEmpty { "Coach" }
        val result = FirestoreSync.cancelBooking(bookingId, name)
        if (result) loadCoachBookings()
        return result
    }

    fun completeBooking(bookingId: String) {
        viewModelScope.launch {
            try { FirestoreSync.completeBooking(bookingId) } catch (_: Exception) { }
            loadCoachBookings()
        }
    }

    fun rateMember(bookingId: String, rating: Float) {
        viewModelScope.launch {
            try { FirestoreSync.rateMember(bookingId, rating) } catch (_: Exception) { }
            loadCoachBookings()
        }
    }

    fun respondToBooking(bookingId: String, accept: Boolean, response: String = "") {
        viewModelScope.launch {
            FirestoreSync.updateBookingStatus(bookingId, if (accept) "CONFIRMED" else "DECLINED", response)
            if (accept) {
                val booking = _coachBookings.value.find { it.id == bookingId }
                if (booking != null) autoRegisterClientFromBooking(booking)
            }
            loadCoachBookings()
        }
    }

    private suspend fun autoRegisterClientFromBooking(booking: Booking) {
        // Use booking.clientId (Firebase UID) as Room Client.id to prevent duplicates
        val existing = repository.allClients.first().any { it.id == booking.clientId }
        if (!existing) {
            repository.insertClient(
                Client(
                    id                   = booking.clientId,
                    name                 = booking.clientName.ifBlank { "Marketplace Client" },
                    phoneNumber          = "",
                    initialGoal          = "Marketplace Client",
                    mrr                  = booking.feeAmount,
                    status               = "Active",
                    enrollmentDateMillis = System.currentTimeMillis()
                )
            )
        }
    }

    /**
     * Loads the coach's own marketplace portfolio for editing.
     * Prefers the live Firestore doc; falls back to locally cached legacy fields
     * so a never-published (or offline) coach still gets a sensible prefill.
     */
    suspend fun loadMyPortfolio(): TrainerProfile {
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        val remote = if (uid != null) {
            try { FirestoreSync.getTrainerById(uid) } catch (_: Exception) { null }
        } else null
        if (remote != null && remote.name.isNotBlank()) return remote
        return TrainerProfile(
            uid              = uid ?: "",
            name             = userPreferences.coachName,
            specialty        = userPreferences.coachSpecialty,
            bio              = userPreferences.trainerBio,
            workDescription  = userPreferences.trainerWorkDescription,
            city             = userPreferences.trainerCity,
            feePerSession    = userPreferences.trainerFeePerSession,
            feeMonthly       = userPreferences.trainerFeeMonthly,
            availabilityDays = userPreferences.trainerAvailabilityDays,
            yearsExperience  = userPreferences.trainerYearsExperience,
            profileImageUrl  = userPreferences.trainerProfileImageUrl,
            portfolioImages  = userPreferences.trainerPortfolioImages,
            lat              = userPreferences.trainerLat,
            lng              = userPreferences.trainerLng
        )
    }

    /**
     * Publishes (or hides) the coach's structured portfolio. Computes the
     * profile-strength score and stamps the current plan tier — both drive
     * Discover ranking and the Featured badge on the member side.
     */
    fun publishPortfolio(profile: TrainerProfile, isPublic: Boolean) {
        viewModelScope.launch {
            val score = com.example.data.PortfolioScoring.score(profile)
            userPreferences.trainerIsPublic          = isPublic
            userPreferences.trainerCity              = profile.city
            userPreferences.trainerBio               = profile.bio
            userPreferences.trainerWorkDescription   = profile.workDescription
            userPreferences.trainerFeePerSession     = profile.feePerSession
            userPreferences.trainerFeeMonthly        = profile.feeMonthly
            userPreferences.trainerAvailabilityDays  = profile.availabilityDays
            userPreferences.trainerYearsExperience   = profile.yearsExperience
            userPreferences.trainerProfileImageUrl   = profile.profileImageUrl
            userPreferences.trainerPortfolioImages   = profile.portfolioImages
            userPreferences.trainerLat               = profile.lat
            userPreferences.trainerLng               = profile.lng
            userPreferences.trainerProfileScore      = score
            if (profile.specialty.isNotBlank()) userPreferences.coachSpecialty = profile.specialty
            try {
                val plan = runCatching {
                    com.example.data.SubscriptionPlan.valueOf(userPreferences.subscriptionPlan)
                }.getOrDefault(com.example.data.SubscriptionPlan.STARTER)
                // Always persist the full portfolio — a hidden profile keeps its data
                // and only the isPublic flag controls Discover visibility.
                FirestoreSync.publishTrainerProfile(profile.copy(
                    uid          = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: "",
                    name         = userPreferences.coachName,
                    specialty    = profile.specialty.ifBlank { userPreferences.coachSpecialty },
                    profileScore = score,
                    planTier     = plan.name.lowercase()
                ), isPublic = isPublic)
                _publishState.value = true
            } catch (_: Exception) {
                _publishState.value = false
            }
        }
    }
}

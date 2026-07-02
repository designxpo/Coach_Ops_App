package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.EntitlementManager
import com.example.data.Entitlements
import com.example.data.GymCheckIn
import com.example.data.GymMember
import com.example.data.GymPayment
import com.example.data.GymPlan
import com.example.data.GymRepository
import com.example.data.UserPreferences
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID

data class GymDashboardStats(
    val activeMembers: Int = 0,
    val expiringSoon: Int = 0,
    val expired: Int = 0,
    val monthCollectedInr: Int = 0,
    val todayCheckIns: Int = 0
)

class GymViewModel(
    private val repository: GymRepository,
    val userPreferences: UserPreferences
) : ViewModel() {

    val entitlements: StateFlow<Entitlements> = EntitlementManager.entitlements

    val members: StateFlow<List<GymMember>> = repository.allMembers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val plans: StateFlow<List<GymPlan>> = repository.activePlans
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val payments: StateFlow<List<GymPayment>> = repository.allPayments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayCheckIns: StateFlow<List<GymCheckIn>> =
        repository.getCheckInsForDate(GymRepository.todayKey())
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _lastReceipt = MutableStateFlow<GymPayment?>(null)
    val lastReceipt: StateFlow<GymPayment?> = _lastReceipt.asStateFlow()

    private val _snackbar = MutableStateFlow("")
    val snackbar: StateFlow<String> = _snackbar.asStateFlow()

    init {
        EntitlementManager.start(userPreferences)
        viewModelScope.launch { repository.syncFromFirestoreIfEmpty() }
    }

    private val monthStartMillis: Long = run {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.timeInMillis
    }

    val stats: StateFlow<GymDashboardStats> = combine(
        members, payments, todayCheckIns
    ) { members, payments, checkIns ->
        GymDashboardStats(
            activeMembers = members.count { it.status == "ACTIVE" && !it.isExpired },
            expiringSoon = members.count { it.expiringSoon && it.status == "ACTIVE" },
            expired = members.count { it.isExpired || it.status == "EXPIRED" },
            monthCollectedInr = payments.filter { it.dateMillis >= monthStartMillis }.sumOf { it.amountInr },
            todayCheckIns = checkIns.size
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GymDashboardStats())

    fun getMember(id: String) = repository.getMember(id)
    fun getPaymentsForMember(id: String) = repository.getPaymentsForMember(id)

    // ─── Gym setup / onboarding ───────────────────────────────────────────────

    val gymSetupComplete: Boolean get() = userPreferences.gymName.isNotEmpty()

    fun saveGymProfile(name: String, address: String, gstin: String) {
        userPreferences.gymName = name.trim()
        userPreferences.gymAddress = address.trim()
        userPreferences.gymGstin = gstin.trim().uppercase()

        // Mirror to user_records so the admin panel sees gym businesses
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance().collection("user_records").document(uid)
            .set(
                mapOf(
                    "gymName" to userPreferences.gymName,
                    "gymAddress" to userPreferences.gymAddress,
                    "gymGstin" to userPreferences.gymGstin
                ),
                SetOptions.merge()
            )

        // First-time setup starts the 30-day trial + seeds Indian plan presets
        EntitlementManager.startGymTrial(userPreferences)
        viewModelScope.launch { repository.seedDefaultPlansIfEmpty() }
    }

    // ─── Members ──────────────────────────────────────────────────────────────

    fun addMember(name: String, phone: String, gender: String, plan: GymPlan?, notes: String) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val member = GymMember(
                id = UUID.randomUUID().toString(),
                name = name.trim(),
                phone = phone.trim(),
                gender = gender,
                joinDateMillis = now,
                planId = plan?.id ?: "",
                planName = plan?.name ?: "",
                planStartMillis = if (plan != null) now else 0L,
                planEndMillis = if (plan != null) now + plan.durationDays * 86400000L else 0L,
                notes = notes.trim()
            )
            repository.addMember(member)
            _snackbar.value = "${member.name} added"
        }
    }

    fun updateMember(member: GymMember) {
        viewModelScope.launch { repository.updateMember(member) }
    }

    fun deleteMember(memberId: String) {
        viewModelScope.launch {
            repository.deleteMember(memberId)
            _snackbar.value = "Member removed"
        }
    }

    // ─── Plans ────────────────────────────────────────────────────────────────

    fun savePlan(existing: GymPlan?, name: String, durationDays: Int, priceInr: Int, description: String) {
        viewModelScope.launch {
            repository.savePlan(
                GymPlan(
                    id = existing?.id ?: UUID.randomUUID().toString(),
                    name = name.trim(),
                    durationDays = durationDays,
                    priceInr = priceInr,
                    description = description.trim()
                )
            )
        }
    }

    fun deactivatePlan(plan: GymPlan) {
        viewModelScope.launch { repository.deactivatePlan(plan) }
    }

    // ─── Payments ─────────────────────────────────────────────────────────────

    fun recordPayment(member: GymMember, plan: GymPlan, amountInr: Int, method: String, notes: String) {
        viewModelScope.launch {
            val payment = repository.recordPayment(member, plan, amountInr, method, notes)
            _lastReceipt.value = payment
            _snackbar.value = "Payment recorded — ${payment.receiptNo}"
        }
    }

    fun clearLastReceipt() { _lastReceipt.value = null }

    /** WhatsApp-ready receipt text — includes GSTIN when the gym has one. */
    fun receiptText(p: GymPayment): String {
        val gym = userPreferences.gymName.ifEmpty { "Gym" }
        val gstLine = if (userPreferences.gymGstin.isNotEmpty()) "\nGSTIN: ${userPreferences.gymGstin}" else ""
        val fmt = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
        return """🧾 *$gym* — Payment Receipt
Receipt No: ${p.receiptNo}$gstLine

Member: ${p.memberName}
Plan: ${p.planName}
Amount: ₹${"%,d".format(p.amountInr)} (${p.method})
Valid: ${fmt.format(java.util.Date(p.periodStartMillis))} → ${fmt.format(java.util.Date(p.periodEndMillis))}

Thank you for training with us! 💪"""
    }

    // ─── Attendance ───────────────────────────────────────────────────────────

    fun checkIn(member: GymMember) {
        viewModelScope.launch {
            val ok = repository.checkIn(member)
            _snackbar.value = if (ok) "${member.name} checked in ✓" else "${member.name} already checked in today"
        }
    }

    fun clearSnackbar() { _snackbar.value = "" }
}

class GymViewModelFactory(
    private val repository: GymRepository,
    private val userPreferences: UserPreferences
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        GymViewModel(repository, userPreferences) as T
}

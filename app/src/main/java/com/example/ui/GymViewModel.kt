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

/** A member's "I've paid via UPI" claim awaiting the owner's confirmation. */
data class PaymentClaim(
    val id: String,
    val memberPhone: String,
    val memberName: String,
    val planName: String,
    val amountInr: Int,
    val claimedAt: Long
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

    // ─── UPI payment claims from members (real-time) ──────────────────────────
    private val _paymentClaims = MutableStateFlow<List<PaymentClaim>>(emptyList())
    val paymentClaims: StateFlow<List<PaymentClaim>> = _paymentClaims.asStateFlow()
    private var claimsListener: com.google.firebase.firestore.ListenerRegistration? = null

    init {
        EntitlementManager.start(userPreferences)
        com.example.data.GymSync.gymName = userPreferences.gymName
        com.example.data.GymSync.gymUpiId = userPreferences.gymUpiId
        viewModelScope.launch { repository.syncFromFirestoreIfEmpty() }
        startClaimsListener()
    }

    private fun startClaimsListener() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        claimsListener = FirebaseFirestore.getInstance()
            .collection("gym_payment_claims")
            .whereEqualTo("ownerUid", uid)
            .whereEqualTo("status", "PENDING")
            .addSnapshotListener { snap, err ->
                if (err != null || snap == null) return@addSnapshotListener
                _paymentClaims.value = snap.documents.map { d ->
                    PaymentClaim(
                        id = d.id,
                        memberPhone = d.getString("phone") ?: "",
                        memberName = d.getString("memberName") ?: "",
                        planName = d.getString("planName") ?: "",
                        amountInr = (d.getLong("amountInr") ?: 0L).toInt(),
                        claimedAt = d.getLong("claimedAt") ?: 0L
                    )
                }.sortedByDescending { it.claimedAt }
            }
    }

    override fun onCleared() {
        super.onCleared()
        claimsListener?.remove()
    }

    /** Owner confirms money arrived in their UPI app → payment recorded, validity extended. */
    fun confirmClaim(claim: PaymentClaim) {
        viewModelScope.launch {
            val member = members.value.firstOrNull {
                com.example.data.GymSync.normalizePhone(it.phone) == claim.memberPhone
            }
            if (member == null) {
                _snackbar.value = "Member not found for ${claim.memberName} — record manually"
                return@launch
            }
            val plan = plans.value.firstOrNull { it.id == member.planId }
                ?: plans.value.firstOrNull { it.name == claim.planName }
            if (plan == null) {
                _snackbar.value = "Plan not found — record from member's page"
                return@launch
            }
            val payment = repository.recordPayment(
                member, plan,
                if (claim.amountInr > 0) claim.amountInr else plan.priceInr,
                "UPI", "Paid by member from app"
            )
            FirebaseFirestore.getInstance()
                .collection("gym_payment_claims").document(claim.id).delete()
            _lastReceipt.value = payment
            _snackbar.value = "Confirmed — ${payment.receiptNo} · validity extended"
        }
    }

    fun rejectClaim(claim: PaymentClaim) {
        FirebaseFirestore.getInstance()
            .collection("gym_payment_claims").document(claim.id)
            .set(mapOf("status" to "REJECTED"), SetOptions.merge())
        _snackbar.value = "Claim rejected"
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

    fun saveGymProfile(
        name: String,
        address: String,
        gstin: String,
        upiId: String = userPreferences.gymUpiId,
        city: String = userPreferences.gymCity,
        lat: Double = userPreferences.gymLat,
        lng: Double = userPreferences.gymLng
    ) {
        userPreferences.gymName = name.trim()
        userPreferences.gymAddress = address.trim()
        userPreferences.gymGstin = gstin.trim().uppercase()
        userPreferences.gymUpiId = upiId.trim().lowercase()
        userPreferences.gymCity = city.trim()
        userPreferences.gymLat = lat
        userPreferences.gymLng = lng
        com.example.data.GymSync.gymName = userPreferences.gymName
        com.example.data.GymSync.gymUpiId = userPreferences.gymUpiId

        // Mirror to user_records so the admin panel sees gym businesses
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance().collection("user_records").document(uid)
            .set(
                mapOf(
                    "gymName" to userPreferences.gymName,
                    "gymAddress" to userPreferences.gymAddress,
                    "gymGstin" to userPreferences.gymGstin,
                    "gymUpiId" to userPreferences.gymUpiId,
                    "gymCity" to userPreferences.gymCity,
                    "gymLat" to userPreferences.gymLat,
                    "gymLng" to userPreferences.gymLng
                ),
                SetOptions.merge()
            )

        // First-time setup starts the 30-day trial + seeds Indian plan presets
        EntitlementManager.startGymTrial(userPreferences)
        viewModelScope.launch {
            repository.seedDefaultPlansIfEmpty()
            // Push refreshed gym identity (name/UPI) into every member's index
            members.value.forEach { com.example.data.GymSync.saveMember(it) }
        }
    }

    // ─── Members ──────────────────────────────────────────────────────────────

    fun addMember(
        name: String,
        phone: String,
        gender: String,
        plan: GymPlan?,
        notes: String,
        joinDateMillis: Long = System.currentTimeMillis(),
        collectPayment: Boolean = false,
        amountInr: Int = 0,
        method: String = "UPI"
    ) {
        viewModelScope.launch {
            val member = GymMember(
                id = UUID.randomUUID().toString(),
                name = name.trim(),
                phone = phone.trim(),
                gender = gender,
                joinDateMillis = joinDateMillis,
                planId = plan?.id ?: "",
                planName = plan?.name ?: "",
                planPriceInr = plan?.priceInr ?: 0,
                planStartMillis = if (plan != null) joinDateMillis else 0L,
                planEndMillis = if (plan != null) joinDateMillis + plan.durationDays * 86400000L else 0L,
                notes = notes.trim()
            )
            val payment = repository.registerMember(member, plan, collectPayment, amountInr, method)
            _snackbar.value = if (payment != null)
                "${member.name} added · ₹${"%,d".format(payment.amountInr)} recorded (${payment.receiptNo})"
            else
                "${member.name} added"
        }
    }

    fun updateMember(member: GymMember) {
        viewModelScope.launch { repository.updateMember(member) }
    }

    fun deleteMember(member: GymMember) {
        viewModelScope.launch {
            repository.deleteMember(member)
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

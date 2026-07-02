package com.example.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Single source of truth for subscription entitlements across the whole app.
 *
 * Trust model (matches the existing admin-driven infra):
 *   - Plans live in user_records/{uid} and are activated from the admin panel
 *     after payment is collected (UPI/bank — gym memberships and B2B SaaS
 *     billing happen off Play Store).
 *   - This manager keeps a real-time listener on the user's record so an
 *     admin plan change reflects in the UI within seconds.
 *   - Users request upgrades in-app → a doc lands in `upgrade_requests`
 *     which the admin panel lists and actions.
 *
 * Fields read from user_records/{uid}:
 *   plan               "STARTER" | "PRO" | "BUSINESS"   (coach tiers)
 *   memberPremium      Boolean                          (client-side premium)
 *   gymTrialStartedAt  Long millis                      (0 / absent = not started)
 */
data class Entitlements(
    val plan: SubscriptionPlan = SubscriptionPlan.STARTER,
    val memberPremium: Boolean = false,
    val gymTrialStartedAt: Long = 0L,
    val gymTrialDays: Int = 30
) {
    val gymTrialActive: Boolean
        get() = gymTrialStartedAt > 0L &&
            System.currentTimeMillis() < gymTrialStartedAt + gymTrialDays * 86400000L

    val gymTrialDaysLeft: Int
        get() = if (!gymTrialActive) 0 else
            ((gymTrialStartedAt + gymTrialDays * 86400000L - System.currentTimeMillis()) / 86400000L).toInt() + 1

    val gymTrialUsed: Boolean get() = gymTrialStartedAt > 0L

    /** Gym Suite is open on the Business plan or during the free trial. */
    val gymUnlocked: Boolean get() = plan.has(PlanFeature.GYM_SUITE) || gymTrialActive
}

object EntitlementManager {

    private val db   = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _entitlements = MutableStateFlow(Entitlements())
    val entitlements: StateFlow<Entitlements> = _entitlements.asStateFlow()

    private var listener: ListenerRegistration? = null
    private var boundUid: String = ""

    /**
     * Start (or restart) the real-time listener for the signed-in user.
     * Safe to call repeatedly — no-ops when already bound to the same UID.
     * Seeds instantly from the local cache so gates render correctly offline.
     */
    fun start(prefs: UserPreferences? = null) {
        val uid = auth.currentUser?.uid ?: run { stop(); return }
        if (uid == boundUid && listener != null) return
        stop()
        boundUid = uid

        // Seed from local cache before the first network round-trip
        if (prefs != null) {
            _entitlements.value = Entitlements(
                plan              = prefs.currentPlan,
                memberPremium     = prefs.memberPremium,
                gymTrialStartedAt = prefs.gymTrialStartedAt
            )
        }

        listener = db.collection("user_records").document(uid)
            .addSnapshotListener { snap, err ->
                if (err != null || snap == null || !snap.exists()) return@addSnapshotListener
                val d = snap.data ?: return@addSnapshotListener
                val planStr = d["plan"] as? String ?: SubscriptionPlan.STARTER.name
                val plan    = SubscriptionPlan.entries.find { it.name == planStr } ?: SubscriptionPlan.STARTER
                val next = Entitlements(
                    plan              = plan,
                    memberPremium     = d["memberPremium"] as? Boolean ?: false,
                    gymTrialStartedAt = d["gymTrialStartedAt"] as? Long ?: 0L
                )
                _entitlements.value = next
                // Refresh cache so gates work offline next launch
                prefs?.let {
                    it.subscriptionPlan   = next.plan.name
                    it.memberPremium      = next.memberPremium
                    it.gymTrialStartedAt  = next.gymTrialStartedAt
                }
            }
    }

    fun stop() {
        listener?.remove()
        listener = null
        boundUid = ""
        _entitlements.value = Entitlements()
    }

    /** Starts the 30-day gym trial. One-shot — never resets an existing trial. */
    fun startGymTrial(prefs: UserPreferences) {
        val uid = auth.currentUser?.uid ?: return
        if (_entitlements.value.gymTrialUsed) return
        val now = System.currentTimeMillis()
        prefs.gymTrialStartedAt = now
        _entitlements.value = _entitlements.value.copy(gymTrialStartedAt = now)
        db.collection("user_records").document(uid)
            .set(mapOf("gymTrialStartedAt" to now), SetOptions.merge())
    }

    /**
     * Files an upgrade request the admin panel picks up.
     * requestedTier: a SubscriptionPlan name or "MEMBER_PREMIUM".
     */
    fun requestUpgrade(
        requestedTier: String,
        userName: String,
        userPhone: String,
        note: String = "",
        onResult: (Boolean) -> Unit = {}
    ) {
        val user = auth.currentUser ?: return onResult(false)
        val doc = mapOf(
            "uid"           to user.uid,
            "email"         to (user.email ?: ""),
            "name"          to userName,
            "phone"         to userPhone,
            "currentPlan"   to _entitlements.value.plan.name,
            "requestedTier" to requestedTier,
            "note"          to note,
            "status"        to "PENDING",   // PENDING → CONTACTED → ACTIVATED / DECLINED
            "requestedAt"   to System.currentTimeMillis()
        )
        db.collection("upgrade_requests").document("${user.uid}_$requestedTier")
            .set(doc, SetOptions.merge())
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }
}

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
    val gymTrialDays: Int = 30,
    // Admin-controlled per-tier feature matrix (empty = use hardcoded defaults)
    val featureMatrix: FeatureMatrix = emptyMap()
) {
    val gymTrialActive: Boolean
        get() = gymTrialStartedAt > 0L &&
            System.currentTimeMillis() < gymTrialStartedAt + gymTrialDays * 86400000L

    val gymTrialDaysLeft: Int
        get() = if (!gymTrialActive) 0 else
            ((gymTrialStartedAt + gymTrialDays * 86400000L - System.currentTimeMillis()) / 86400000L).toInt() + 1

    val gymTrialUsed: Boolean get() = gymTrialStartedAt > 0L

    // ─── Per-tier feature gates (matrix override → hardcoded default) ─────────
    /** Gym Suite entitlement from the plan/matrix, ignoring the trial. */
    val gymSuiteEntitled: Boolean
        get() = FeatureGate.coachUnlocked(featureMatrix, GatedFeature.GYM_SUITE, plan)

    /** Gym Suite is open when the tier grants it OR the free trial is active. */
    val gymUnlocked: Boolean get() = gymSuiteEntitled || gymTrialActive

    val revenueAnalyticsUnlocked: Boolean
        get() = FeatureGate.coachUnlocked(featureMatrix, GatedFeature.REVENUE_ANALYTICS, plan)

    val broadcastUnlocked: Boolean
        get() = FeatureGate.coachUnlocked(featureMatrix, GatedFeature.BROADCAST, plan)

    val aiNutritionCoachUnlocked: Boolean
        get() = FeatureGate.memberUnlocked(featureMatrix, GatedFeature.AI_NUTRITION_COACH, memberPremium)

    val aiMealPlannerUnlocked: Boolean
        get() = FeatureGate.memberUnlocked(featureMatrix, GatedFeature.AI_MEAL_PLANNER, memberPremium)
}

object EntitlementManager {

    private val db   = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _entitlements = MutableStateFlow(Entitlements())
    val entitlements: StateFlow<Entitlements> = _entitlements.asStateFlow()

    private var listener: ListenerRegistration? = null
    private var matrixListener: ListenerRegistration? = null
    private var boundUid: String = ""

    // Latest raw inputs — the user record and the admin feature matrix arrive on
    // independent listeners; rebuild() folds whichever is current into one value.
    private var curPlan = SubscriptionPlan.STARTER
    private var curMemberPremium = false
    private var curGymTrialStartedAt = 0L
    private var curMatrix: FeatureMatrix = emptyMap()

    /** Latest feature matrix, for non-reactive consumers (e.g. marketplace badges). */
    val currentMatrix: FeatureMatrix get() = curMatrix

    private fun rebuild() {
        _entitlements.value = Entitlements(
            plan              = curPlan,
            memberPremium     = curMemberPremium,
            gymTrialStartedAt = curGymTrialStartedAt,
            featureMatrix     = curMatrix
        )
    }

    /**
     * Start (or restart) the real-time listeners for the signed-in user and the
     * admin feature matrix. Safe to call repeatedly — no-ops when already bound
     * to the same UID. Seeds instantly from the local cache so gates render
     * correctly offline.
     */
    fun start(prefs: UserPreferences? = null) {
        val uid = auth.currentUser?.uid ?: run { stop(); return }
        if (uid == boundUid && listener != null) return
        stop()
        boundUid = uid

        // Seed from local cache before the first network round-trip
        if (prefs != null) {
            curPlan              = prefs.currentPlan
            curMemberPremium     = prefs.memberPremium
            curGymTrialStartedAt = prefs.gymTrialStartedAt
            rebuild()
        }

        listener = db.collection("user_records").document(uid)
            .addSnapshotListener { snap, err ->
                if (err != null || snap == null || !snap.exists()) return@addSnapshotListener
                val d = snap.data ?: return@addSnapshotListener
                val planStr = d["plan"] as? String ?: SubscriptionPlan.STARTER.name
                curPlan              = SubscriptionPlan.entries.find { it.name == planStr } ?: SubscriptionPlan.STARTER
                curMemberPremium     = d["memberPremium"] as? Boolean ?: false
                curGymTrialStartedAt = d["gymTrialStartedAt"] as? Long ?: 0L
                rebuild()
                // Refresh cache so gates work offline next launch
                prefs?.let {
                    it.subscriptionPlan   = curPlan.name
                    it.memberPremium      = curMemberPremium
                    it.gymTrialStartedAt  = curGymTrialStartedAt
                }
            }

        // Admin feature matrix — one global doc, gates every tiered feature.
        matrixListener = db.collection("admin_config").document("feature_matrix")
            .addSnapshotListener { snap, err ->
                curMatrix = if (err != null || snap == null || !snap.exists()) emptyMap()
                            else parseMatrix(snap.data)
                rebuild()
            }
    }

    private fun parseMatrix(data: Map<String, Any?>?): FeatureMatrix {
        if (data == null) return emptyMap()
        val out = HashMap<String, Map<String, Boolean>>()
        for ((featureKey, raw) in data) {
            val inner = raw as? Map<*, *> ?: continue
            val tiers = HashMap<String, Boolean>()
            for ((tk, tv) in inner) {
                val tierKey = tk as? String ?: continue
                val b = tv as? Boolean ?: continue
                tiers[tierKey] = b
            }
            if (tiers.isNotEmpty()) out[featureKey] = tiers
        }
        return out
    }

    fun stop() {
        listener?.remove(); listener = null
        matrixListener?.remove(); matrixListener = null
        boundUid = ""
        curPlan = SubscriptionPlan.STARTER
        curMemberPremium = false
        curGymTrialStartedAt = 0L
        curMatrix = emptyMap()
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

package com.example.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class UserPreferences private constructor(private val context: Context) {

    // Global prefs: only stores which UID was last active (survives between users)
    private val globalPrefs: SharedPreferences =
        context.getSharedPreferences("global_session", Context.MODE_PRIVATE)

    private var currentUid: String = globalPrefs.getString("last_uid", "") ?: ""
    val userId: String get() = currentUid

    // Each user gets their own dedicated file; anonymous = not yet signed in
    private var prefs: SharedPreferences =
        context.getSharedPreferences(prefsName(currentUid), Context.MODE_PRIVATE)

    private fun prefsName(uid: String) =
        if (uid.isNotEmpty()) "user_prefs_$uid" else "user_prefs_anonymous"

    /**
     * Call immediately after a successful login/register.
     * Switches to the UID's dedicated prefs file — no data is ever wiped.
     * If the same user logs back in, this is a no-op.
     */
    fun bindToUser(uid: String) {
        if (uid.isNotEmpty() && uid != currentUid) {
            currentUid = uid
            globalPrefs.edit().putString("last_uid", uid).apply()
            prefs = context.getSharedPreferences(prefsName(uid), Context.MODE_PRIVATE)
            // Sync all reactive flows with the new user's prefs file
            _profilePhotoFlow.value = profilePhotoUrl
        }
    }

    /**
     * Call on logout. Resets to anonymous prefs so the app shows the login screen.
     * The user's data stays safe in their UID-keyed file for when they log back in.
     */
    fun clearLocalSession() {
        globalPrefs.edit().remove("last_uid").apply()
        currentUid = ""
        prefs = context.getSharedPreferences(prefsName(""), Context.MODE_PRIVATE)
        _profilePhotoFlow.value = ""
    }

    // ─── Onboarding / session ─────────────────────────────────────────────────
    var onboardingComplete: Boolean
        get() = prefs.getBoolean("onboarding_complete", false)
        set(value) { prefs.edit().putBoolean("onboarding_complete", value).apply() }

    var userRole: String
        get() = prefs.getString("user_role", "") ?: ""
        set(value) { prefs.edit().putString("user_role", value).apply() }

    // ─── Coach profile ────────────────────────────────────────────────────────
    var coachName: String
        get() = prefs.getString("coach_name", "") ?: ""
        set(value) { prefs.edit().putString("coach_name", value).apply() }

    var coachEmail: String
        get() = prefs.getString("coach_email", "") ?: ""
        set(value) { prefs.edit().putString("coach_email", value).apply() }

    var coachPhone: String
        get() = prefs.getString("coach_phone", "") ?: ""
        set(value) { prefs.edit().putString("coach_phone", value).apply() }

    // Stored as comma-separated: "Personal Training,Yoga,HIIT"
    var coachSpecialty: String
        get() = prefs.getString("coach_specialty", "") ?: ""
        set(value) { prefs.edit().putString("coach_specialty", value).apply() }

    // Convenience: work with a list of specialties
    var coachSpecialties: List<String>
        get() = coachSpecialty.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        set(value) { coachSpecialty = value.joinToString(",") }

    var coachClientRange: String
        get() = prefs.getString("coach_client_range", "") ?: ""
        set(value) { prefs.edit().putString("coach_client_range", value).apply() }

    var coachChallenge: String
        get() = prefs.getString("coach_challenge", "") ?: ""
        set(value) { prefs.edit().putString("coach_challenge", value).apply() }

    // ─── Subscription ─────────────────────────────────────────────────────────
    var subscriptionPlan: String
        get() = prefs.getString("subscription_plan", SubscriptionPlan.STARTER.name) ?: SubscriptionPlan.STARTER.name
        set(value) { prefs.edit().putString("subscription_plan", value).apply() }

    val currentPlan: SubscriptionPlan
        get() = SubscriptionPlan.entries.find { it.name == subscriptionPlan } ?: SubscriptionPlan.STARTER

    var adminPin: String
        get() = prefs.getString("admin_pin", "") ?: ""
        set(value) { prefs.edit().putString("admin_pin", value).apply() }

    // ─── Member premium (client-side tier) — cache of user_records.memberPremium ─
    var memberPremium: Boolean
        get() = prefs.getBoolean("member_premium", false)
        set(value) { prefs.edit().putBoolean("member_premium", value).apply() }

    // ─── Gym owner profile ────────────────────────────────────────────────────
    var gymName: String
        get() = prefs.getString("gym_name", "") ?: ""
        set(value) { prefs.edit().putString("gym_name", value).apply() }

    var gymAddress: String
        get() = prefs.getString("gym_address", "") ?: ""
        set(value) { prefs.edit().putString("gym_address", value).apply() }

    var gymGstin: String
        get() = prefs.getString("gym_gstin", "") ?: ""
        set(value) { prefs.edit().putString("gym_gstin", value).apply() }

    // UPI ID for direct fee collection (money goes straight to owner's bank)
    var gymUpiId: String
        get() = prefs.getString("gym_upi_id", "") ?: ""
        set(value) { prefs.edit().putString("gym_upi_id", value).apply() }

    var gymCity: String
        get() = prefs.getString("gym_city", "") ?: ""
        set(value) { prefs.edit().putString("gym_city", value).apply() }

    var gymLat: Double
        get() = prefs.getFloat("gym_lat", 0f).toDouble()
        set(v) { prefs.edit().putFloat("gym_lat", v.toFloat()).apply() }

    var gymLng: Double
        get() = prefs.getFloat("gym_lng", 0f).toDouble()
        set(v) { prefs.edit().putFloat("gym_lng", v.toFloat()).apply() }

    // Cache of user_records.gymTrialStartedAt (0 = trial never started)
    var gymTrialStartedAt: Long
        get() = prefs.getLong("gym_trial_started_at", 0L)
        set(value) { prefs.edit().putLong("gym_trial_started_at", value).apply() }

    // ─── Per-day macro goals ──────────────────────────────────────────────────
    var trainingDayCalories: Int
        get() = prefs.getInt("macro_training_calories", 2200)
        set(v) { prefs.edit().putInt("macro_training_calories", v).apply() }

    var trainingDayProteinG: Int
        get() = prefs.getInt("macro_training_protein", 150)
        set(v) { prefs.edit().putInt("macro_training_protein", v).apply() }

    var trainingDayCarbsG: Int
        get() = prefs.getInt("macro_training_carbs", 250)
        set(v) { prefs.edit().putInt("macro_training_carbs", v).apply() }

    var trainingDayFatG: Int
        get() = prefs.getInt("macro_training_fat", 70)
        set(v) { prefs.edit().putInt("macro_training_fat", v).apply() }

    var restDayCalories: Int
        get() = prefs.getInt("macro_rest_calories", 1800)
        set(v) { prefs.edit().putInt("macro_rest_calories", v).apply() }

    var restDayProteinG: Int
        get() = prefs.getInt("macro_rest_protein", 130)
        set(v) { prefs.edit().putInt("macro_rest_protein", v).apply() }

    var restDayCarbsG: Int
        get() = prefs.getInt("macro_rest_carbs", 180)
        set(v) { prefs.edit().putInt("macro_rest_carbs", v).apply() }

    var restDayFatG: Int
        get() = prefs.getInt("macro_rest_fat", 60)
        set(v) { prefs.edit().putInt("macro_rest_fat", v).apply() }

    // ─── Live tracking (pinned notification with steps/water) ────────────────
    var liveTrackingEnabled: Boolean
        get() = prefs.getBoolean("live_tracking_enabled", true)
        set(v) { prefs.edit().putBoolean("live_tracking_enabled", v).apply() }

    var planPriceStarter: Int
        get() = prefs.getInt("plan_price_starter", 0)
        set(v) { prefs.edit().putInt("plan_price_starter", v).apply() }

    var planPricePro: Int
        get() = prefs.getInt("plan_price_pro", 999)
        set(v) { prefs.edit().putInt("plan_price_pro", v).apply() }

    var planPriceBusiness: Int
        get() = prefs.getInt("plan_price_business", 2499)
        set(v) { prefs.edit().putInt("plan_price_business", v).apply() }

    // ─── Client fields ────────────────────────────────────────────────────────
    var clientName: String
        get() = prefs.getString("client_name", "") ?: ""
        set(value) { prefs.edit().putString("client_name", value).apply() }

    var clientCity: String
        get() = prefs.getString("client_city", "") ?: ""
        set(value) { prefs.edit().putString("client_city", value).apply() }

    var clientGoal: String
        get() = prefs.getString("client_goal", "") ?: ""
        set(value) { prefs.edit().putString("client_goal", value).apply() }

    var clientLat: Double
        get() = prefs.getFloat("client_lat", 0f).toDouble()
        set(v) { prefs.edit().putFloat("client_lat", v.toFloat()).apply() }

    var clientLng: Double
        get() = prefs.getFloat("client_lng", 0f).toDouble()
        set(v) { prefs.edit().putFloat("client_lng", v.toFloat()).apply() }

    var clientRadiusKm: Int
        get() = prefs.getInt("client_radius_km", 25)
        set(v) { prefs.edit().putInt("client_radius_km", v).apply() }

    // ─── Profile photo — backed by a StateFlow so UI reacts instantly ────────
    private val _profilePhotoFlow = MutableStateFlow(prefs.getString("profile_photo_url", "") ?: "")
    val profilePhotoFlow: StateFlow<String> = _profilePhotoFlow.asStateFlow()

    var profilePhotoUrl: String
        get() = prefs.getString("profile_photo_url", "") ?: ""
        set(value) {
            prefs.edit().putString("profile_photo_url", value).apply()
            _profilePhotoFlow.value = value
        }

    // ─── Health profile (BMI / metrics calculator) ────────────────────────────
    var healthAgeYears: Int
        get() = prefs.getInt("health_age", 0)
        set(v) { prefs.edit().putInt("health_age", v).apply() }

    var healthHeightCm: Float
        get() = prefs.getFloat("health_height_cm", 0f)
        set(v) { prefs.edit().putFloat("health_height_cm", v).apply() }

    var healthWeightKg: Float
        get() = prefs.getFloat("health_weight_kg", 0f)
        set(v) { prefs.edit().putFloat("health_weight_kg", v).apply() }

    var healthGender: String
        get() = prefs.getString("health_gender", "MALE") ?: "MALE"
        set(v) { prefs.edit().putString("health_gender", v).apply() }

    var healthActivity: String
        get() = prefs.getString("health_activity", "MODERATE") ?: "MODERATE"
        set(v) { prefs.edit().putString("health_activity", v).apply() }

    fun loadHealthProfile(goal: ClientGoal = ClientGoal.GENERAL_FITNESS): HealthProfile = HealthProfile(
        ageYears      = healthAgeYears,
        heightCm      = healthHeightCm,
        weightKg      = healthWeightKg,
        gender        = Gender.entries.find { it.name == healthGender } ?: Gender.MALE,
        activityLevel = ActivityLevel.entries.find { it.name == healthActivity } ?: ActivityLevel.MODERATE,
        goal          = goal,
        waistCm       = healthWaistCm,
        neckCm        = healthNeckCm,
        hipCm         = healthHipCm
    )

    var healthWaistCm: Float
        get() = prefs.getFloat("health_waist_cm", 0f)
        set(v) { prefs.edit().putFloat("health_waist_cm", v).apply() }

    var healthNeckCm: Float
        get() = prefs.getFloat("health_neck_cm", 0f)
        set(v) { prefs.edit().putFloat("health_neck_cm", v).apply() }

    var healthHipCm: Float
        get() = prefs.getFloat("health_hip_cm", 0f)
        set(v) { prefs.edit().putFloat("health_hip_cm", v).apply() }

    fun saveHealthProfile(p: HealthProfile) {
        healthAgeYears = p.ageYears
        healthHeightCm = p.heightCm
        healthWeightKg = p.weightKg
        healthGender   = p.gender.name
        healthActivity = p.activityLevel.name
        healthWaistCm  = p.waistCm
        healthNeckCm   = p.neckCm
        healthHipCm    = p.hipCm
    }

    // ─── Trainer public profile ───────────────────────────────────────────────
    var trainerIsPublic: Boolean
        get() = prefs.getBoolean("trainer_is_public", false)
        set(value) { prefs.edit().putBoolean("trainer_is_public", value).apply() }

    var trainerCity: String
        get() = prefs.getString("trainer_city", "") ?: ""
        set(value) { prefs.edit().putString("trainer_city", value).apply() }

    var trainerBio: String
        get() = prefs.getString("trainer_bio", "") ?: ""
        set(value) { prefs.edit().putString("trainer_bio", value).apply() }

    var trainerWorkDescription: String
        get() = prefs.getString("trainer_work_desc", "") ?: ""
        set(value) { prefs.edit().putString("trainer_work_desc", value).apply() }

    var trainerFeePerSession: Int
        get() = prefs.getInt("trainer_fee_session", 0)
        set(value) { prefs.edit().putInt("trainer_fee_session", value).apply() }

    var trainerFeeMonthly: Int
        get() = prefs.getInt("trainer_fee_monthly", 0)
        set(value) { prefs.edit().putInt("trainer_fee_monthly", value).apply() }

    var trainerAvailabilityDays: String
        get() = prefs.getString("trainer_avail_days", "") ?: ""
        set(value) { prefs.edit().putString("trainer_avail_days", value).apply() }

    var trainerYearsExperience: Int
        get() = prefs.getInt("trainer_years_exp", 0)
        set(value) { prefs.edit().putInt("trainer_years_exp", value).apply() }

    var trainerLat: Double
        get() = prefs.getFloat("trainer_lat", 0f).toDouble()
        set(v) { prefs.edit().putFloat("trainer_lat", v.toFloat()).apply() }

    var trainerLng: Double
        get() = prefs.getFloat("trainer_lng", 0f).toDouble()
        set(v) { prefs.edit().putFloat("trainer_lng", v.toFloat()).apply() }

    var trainerProfileImageUrl: String
        get() = prefs.getString("trainer_profile_image_url", "") ?: ""
        set(value) { prefs.edit().putString("trainer_profile_image_url", value).apply() }

    var trainerPortfolioImages: String
        get() = prefs.getString("trainer_portfolio_images", "") ?: ""
        set(value) { prefs.edit().putString("trainer_portfolio_images", value).apply() }

    /** Last published PortfolioScoring result — shown on the Profile screen's Marketplace card. */
    var trainerProfileScore: Int
        get() = prefs.getInt("trainer_profile_score", 0)
        set(value) { prefs.edit().putInt("trainer_profile_score", value).apply() }

    /** Multi-gym: which location the Gym Suite is currently managing. */
    var activeGymId: String
        get() = prefs.getString("active_gym_id", DEFAULT_GYM_ID) ?: DEFAULT_GYM_ID
        set(value) { prefs.edit().putString("active_gym_id", value).apply() }

    companion object {
        @Volatile private var instance: UserPreferences? = null

        fun getInstance(context: Context): UserPreferences =
            instance ?: synchronized(this) {
                instance ?: UserPreferences(context.applicationContext).also { instance = it }
            }
    }
}

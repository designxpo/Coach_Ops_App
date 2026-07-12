package com.example.data

/**
 * Per-tier feature locking, admin-controlled.
 *
 * The admin portal writes a matrix to `admin_config/feature_matrix`:
 *
 *   { "<featureKey>": { "<TIER>": true|false, ... }, ... }
 *
 * Coach tiers are the SubscriptionPlan names (STARTER · PRO · BUSINESS).
 * Member tiers are FREE · PREMIUM (driven by user_records.memberPremium).
 *
 * The matrix is an OVERRIDE layer: whenever a (feature, tier) cell is present
 * it wins; when the doc is missing or a cell is absent we fall back to
 * [FeatureGate.DEFAULTS], which mirror the app's historical hardcoded gating
 * exactly — so a missing/partial matrix changes nothing.
 */
typealias FeatureMatrix = Map<String, Map<String, Boolean>>

enum class GatedFeature(val key: String, val audience: Audience) {
    // Coach tiers: STARTER / PRO / BUSINESS
    REVENUE_ANALYTICS   ("revenue_analytics",    Audience.COACH),
    GYM_SUITE           ("gym_suite",            Audience.COACH),
    FEATURED_MARKETPLACE("featured_marketplace", Audience.COACH),
    BROADCAST           ("broadcast",            Audience.COACH),
    // Member tiers: FREE / PREMIUM
    AI_NUTRITION_COACH  ("ai_nutrition_coach",   Audience.MEMBER),
    AI_MEAL_PLANNER     ("ai_meal_planner",      Audience.MEMBER);

    enum class Audience { COACH, MEMBER }
}

object FeatureGate {

    const val TIER_FREE = "FREE"
    const val TIER_PREMIUM = "PREMIUM"

    /**
     * Fallback gating — MUST stay in lock-step with the admin portal's
     * DEFAULT_FEATURE_MATRIX and reflect the app's pre-matrix behaviour so
     * shipping this is non-breaking:
     *   - revenue analytics: Pro+
     *   - gym suite: Business only (trial handled separately)
     *   - featured placement: Pro+
     *   - broadcast: everyone (was ungated) — admin may restrict
     *   - member AI coach & meal planner: Premium only
     */
    val DEFAULTS: FeatureMatrix = mapOf(
        GatedFeature.REVENUE_ANALYTICS.key    to mapOf("STARTER" to false, "PRO" to true,  "BUSINESS" to true),
        GatedFeature.GYM_SUITE.key            to mapOf("STARTER" to false, "PRO" to false, "BUSINESS" to true),
        GatedFeature.FEATURED_MARKETPLACE.key to mapOf("STARTER" to false, "PRO" to true,  "BUSINESS" to true),
        GatedFeature.BROADCAST.key            to mapOf("STARTER" to true,  "PRO" to true,  "BUSINESS" to true),
        GatedFeature.AI_NUTRITION_COACH.key   to mapOf(TIER_FREE to false, TIER_PREMIUM to true),
        GatedFeature.AI_MEAL_PLANNER.key      to mapOf(TIER_FREE to false, TIER_PREMIUM to true),
    )

    private fun lookup(matrix: FeatureMatrix, feature: GatedFeature, tier: String): Boolean =
        matrix[feature.key]?.get(tier)
            ?: DEFAULTS[feature.key]?.get(tier)
            ?: false

    /** Is a coach feature unlocked for this plan under the given matrix? */
    fun coachUnlocked(matrix: FeatureMatrix, feature: GatedFeature, plan: SubscriptionPlan): Boolean =
        lookup(matrix, feature, plan.name)

    /** Is a member feature unlocked for a free / premium member? */
    fun memberUnlocked(matrix: FeatureMatrix, feature: GatedFeature, premium: Boolean): Boolean =
        lookup(matrix, feature, if (premium) TIER_PREMIUM else TIER_FREE)

    /** Map a marketplace planTier string ("pro"/"business"/…) to a coach plan. */
    fun planFromTierString(tier: String): SubscriptionPlan = when (tier.lowercase()) {
        "business" -> SubscriptionPlan.BUSINESS
        "pro"      -> SubscriptionPlan.PRO
        else       -> SubscriptionPlan.STARTER
    }
}

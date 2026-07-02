package com.example.data

enum class SubscriptionPlan(
    val displayName: String,
    val emoji: String,
    val priceMonthly: Int,
    val maxClients: Int,
    val color: Long
) {
    STARTER("Starter", "🌱", 0,          5,          0xFF6B7280),
    PRO    ("Pro",     "⚡", 999,         20,         0xFFD4F700),
    BUSINESS("Business","🚀", 2499,      Int.MAX_VALUE, 0xFF818CF8)
}

/**
 * Per-plan feature entitlements. This is the single source of truth for
 * "what does each tier see" on the coach/gym-owner side of the app.
 */
enum class PlanFeature(val label: String) {
    REVENUE_ANALYTICS("MRR trend chart & 30-day forecast"),
    BROADCAST("WhatsApp bulk messaging"),
    ADVANCED_ANALYTICS("Retention & churn insights"),
    GYM_SUITE("Gym operations: members, plans, billing, attendance"),
    PRIORITY_SUPPORT("Priority WhatsApp support")
}

/** Which plan unlocks which feature. */
fun SubscriptionPlan.has(feature: PlanFeature): Boolean = when (feature) {
    PlanFeature.REVENUE_ANALYTICS  -> this != SubscriptionPlan.STARTER
    PlanFeature.BROADCAST          -> this != SubscriptionPlan.STARTER
    PlanFeature.ADVANCED_ANALYTICS -> this != SubscriptionPlan.STARTER
    PlanFeature.GYM_SUITE          -> this == SubscriptionPlan.BUSINESS
    PlanFeature.PRIORITY_SUPPORT   -> this == SubscriptionPlan.BUSINESS
}

/** The lowest plan that includes a feature — used on upsell cards. */
fun PlanFeature.requiredPlan(): SubscriptionPlan = when (this) {
    PlanFeature.REVENUE_ANALYTICS,
    PlanFeature.BROADCAST,
    PlanFeature.ADVANCED_ANALYTICS -> SubscriptionPlan.PRO
    PlanFeature.GYM_SUITE,
    PlanFeature.PRIORITY_SUPPORT   -> SubscriptionPlan.BUSINESS
}

/** Marketing bullet points per plan, shown on the upgrade screen. */
fun SubscriptionPlan.sellingPoints(): List<String> = when (this) {
    SubscriptionPlan.STARTER -> listOf(
        "Up to 5 clients",
        "Programs & workout tracking",
        "Client chat & bookings",
        "Payment reminders via WhatsApp"
    )
    SubscriptionPlan.PRO -> listOf(
        "Up to 20 clients",
        "Everything in Starter",
        "Revenue analytics & MRR forecast",
        "WhatsApp broadcast to all clients",
        "Retention & churn insights"
    )
    SubscriptionPlan.BUSINESS -> listOf(
        "Unlimited clients",
        "Everything in Pro",
        "Full Gym Suite — members, plans & billing",
        "Attendance tracking & GST-ready receipts",
        "Priority WhatsApp support"
    )
}

enum class AppFeature(
    val key: String,
    val label: String,
    val description: String,
    val defaultEnabled: Boolean = true
) {
    PROGRAM_TIMELINE("program_timeline", "Program Timeline",  "Daily session tracking per program"),
    REVENUE_CHART   ("revenue_chart",    "Revenue Chart",     "MRR trend visualization"),
    BODY_MEASUREMENTS("measurements",   "Body Measurements", "Track weight & body fat %"),
    CLIENT_NOTES    ("notes",           "Client Notes",      "Quick notes per client"),
    BROADCAST       ("broadcast",       "Broadcast",         "WhatsApp bulk messaging"),
    ANALYTICS       ("analytics",       "Analytics",         "Retention & churn insights"),
    MAINTENANCE_MODE("maintenance",     "Maintenance Mode",  "Lock app for all users", defaultEnabled = false),
    FORCE_UPDATE    ("force_update",    "Force Update",      "Prompt users to update", defaultEnabled = false)
}

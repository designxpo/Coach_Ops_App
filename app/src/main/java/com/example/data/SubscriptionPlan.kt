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

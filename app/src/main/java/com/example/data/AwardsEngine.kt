package com.example.data

import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// ── Award model ───────────────────────────────────────────────────────────────

enum class AwardCategory(val label: String) {
    STREAK("Streaks"),
    MILESTONE("Milestones"),
    RECORD("Records"),
    MONTHLY("Monthly Challenges"),
    HYDRATION("Hydration"),
    WORKOUT("Workouts")
}

data class AwardDef(
    val id: String,
    val title: String,
    val emoji: String,
    val description: String,
    val category: AwardCategory,
    val target: Int,              // threshold that earns it (days, count, …)
    val repeatable: Boolean = false
)

data class EarnedAward(
    val id: String,
    val earnedAt: Long,
    val lastEarnedAt: Long,
    val timesEarned: Int,
    val value: Int = 0            // e.g. step count for records
)

/** Running counters folded from completed days (never includes today). */
data class AwardStats(
    val lastCountedDate: String = "",   // yyyy-MM-dd of last folded day
    val currentStreak: Int = 0,         // step-goal streak up to lastCountedDate
    val bestStreak: Int = 0,
    val goalDaysTotal: Int = 0,         // lifetime days with step goal met
    val bestSteps: Int = 0,
    val waterStreak: Int = 0,           // consecutive days with >= WATER_GOAL glasses
    val monthKey: String = "",          // yyyy-MM currently being counted
    val monthGoalDays: Int = 0,         // goal days inside monthKey
    val weekGoalDays: Int = 0,          // goal days inside the current Mon–Sun week
    val workoutWeekKey: String = ""     // last Mon–Sun week already awarded for workouts
)

/** Everything the UI + worker need, computed in one pass. */
data class AwardsState(
    val stats: AwardStats,
    val earned: Map<String, EarnedAward>,
    val newlyEarned: List<AwardDef>,
    val todaySteps: Int,
    val todayGoalMet: Boolean,
    val liveStreak: Int,                // currentStreak + today (if goal met)
    val workoutsTotal: Int,
    val workoutsThisWeek: Int
)

// ── Engine ────────────────────────────────────────────────────────────────────

/**
 * Apple-Fitness-style awards for members, computed from data the app already
 * collects (users/{uid}/health_daily + client_fitness/{uid}/logs).
 *
 * Design notes:
 *  - No Room: earned awards + running stats live under users/{uid}/awards
 *    (covered by the existing users/{userId}/{healthCollection} rule) so a
 *    reinstall or destructive Room migration loses nothing.
 *  - Counters fold forward day by day from `lastCountedDate` to yesterday, so
 *    each completed day is counted exactly once no matter how often we run.
 *  - Today is never folded into counters — it is reported live for display and
 *    for the evening streak-at-risk notification.
 */
object AwardsEngine {

    const val STEP_GOAL  = 10_000       // mirrors StepTrackingService/FitnessHub hardcodes
    const val WATER_GOAL = 8
    private const val STATS_DOC   = "_stats"
    private const val CATCHUP_CAP = 60  // max past days folded in one run

    // ── Catalog ──────────────────────────────────────────────────────────────

    val STREAK_TIERS    = listOf(3, 7, 14, 30, 50, 100)
    val MILESTONE_TIERS = listOf(10, 25, 50, 100, 365)
    val WORKOUT_TIERS   = listOf(1, 10, 50, 100)
    const val MONTHLY_TARGET      = 15
    const val WORKOUT_WEEK_TARGET = 5

    val catalog: List<AwardDef> = buildList {
        STREAK_TIERS.forEach { t ->
            val (emoji, title) = when (t) {
                3    -> "🔥" to "On a Roll"
                7    -> "⚡" to "One Week Strong"
                14   -> "🚀" to "Fortnight Force"
                30   -> "💎" to "30-Day Legend"
                50   -> "🌟" to "Unstoppable 50"
                else -> "👑" to "Streak Royalty"
            }
            add(AwardDef("streak_$t", title, emoji,
                "Meet your ${"%,d".format(STEP_GOAL)}-step goal $t days in a row.",
                AwardCategory.STREAK, t))
        }
        MILESTONE_TIERS.forEach { t ->
            val (emoji, title) = when (t) {
                10   -> "🥉" to "Move Maker"
                25   -> "🥈" to "Quarter Century"
                50   -> "🥇" to "Fifty Club"
                100  -> "🏆" to "Century Club"
                else -> "🎖️" to "365 Step Goals"
            }
            add(AwardDef("goal_days_$t", title, emoji,
                "Reach your daily step goal $t times in total.",
                AwardCategory.MILESTONE, t))
        }
        add(AwardDef("step_record", "New Step Record", "📈",
            "Beat your best-ever daily step count.",
            AwardCategory.RECORD, 1, repeatable = true))
        add(AwardDef("monthly_challenge", "Monthly Challenge", "🗓️",
            "Meet your step goal $MONTHLY_TARGET days in a calendar month.",
            AwardCategory.MONTHLY, MONTHLY_TARGET, repeatable = true))
        add(AwardDef("hydration_7", "Hydration Hero", "💧",
            "Drink $WATER_GOAL glasses of water 7 days in a row.",
            AwardCategory.HYDRATION, 7, repeatable = true))
        add(AwardDef("perfect_week", "Perfect Week", "⭐",
            "Meet your step goal every day, Monday to Sunday.",
            AwardCategory.STREAK, 7, repeatable = true))
        WORKOUT_TIERS.forEach { t ->
            val (emoji, title) = when (t) {
                1    -> "🏋️" to "First Workout"
                10   -> "💪" to "Ten Down"
                50   -> "🦾" to "Fifty Strong"
                else -> "🏅" to "Workout Century"
            }
            add(AwardDef("workout_$t", title, emoji,
                if (t == 1) "Log your first workout." else "Log $t workouts in total.",
                AwardCategory.WORKOUT, t))
        }
        add(AwardDef("workout_week_5", "5-Workout Week", "🗡️",
            "Log $WORKOUT_WEEK_TARGET workouts in a single Mon–Sun week.",
            AwardCategory.WORKOUT, WORKOUT_WEEK_TARGET, repeatable = true))
    }

    fun def(id: String): AwardDef? = catalog.find { it.id == id }

    // ── Firestore access ─────────────────────────────────────────────────────

    private val db get() = FirebaseFirestore.getInstance()
    private fun awardsCol(uid: String) =
        db.collection("users").document(uid).collection("awards")
    private fun dailyCol(uid: String) =
        db.collection("users").document(uid).collection("health_daily")
    private fun workoutLogs(uid: String) =
        db.collection("client_fitness").document(uid).collection("logs")

    private val fmt get() = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    // ── Evaluation ───────────────────────────────────────────────────────────

    /**
     * Fold completed days into counters, detect newly earned awards, persist,
     * and return the fresh state. Safe to call repeatedly (idempotent per day).
     * Never throws — returns null on failure (offline, signed out, rules).
     */
    suspend fun evaluate(uid: String): AwardsState? = withContext(Dispatchers.IO) {
        if (uid.isEmpty()) return@withContext null
        try {
            val snap    = awardsCol(uid).get().await()
            val earned  = HashMap<String, EarnedAward>()
            var stats   = AwardStats()
            snap.documents.forEach { d ->
                if (d.id == STATS_DOC) {
                    stats = AwardStats(
                        lastCountedDate = d.getString("lastCountedDate") ?: "",
                        currentStreak   = (d.getLong("currentStreak") ?: 0L).toInt(),
                        bestStreak      = (d.getLong("bestStreak") ?: 0L).toInt(),
                        goalDaysTotal   = (d.getLong("goalDaysTotal") ?: 0L).toInt(),
                        bestSteps       = (d.getLong("bestSteps") ?: 0L).toInt(),
                        waterStreak     = (d.getLong("waterStreak") ?: 0L).toInt(),
                        monthKey        = d.getString("monthKey") ?: "",
                        monthGoalDays   = (d.getLong("monthGoalDays") ?: 0L).toInt(),
                        weekGoalDays    = (d.getLong("weekGoalDays") ?: 0L).toInt(),
                        workoutWeekKey  = d.getString("workoutWeekKey") ?: ""
                    )
                } else {
                    earned[d.id] = EarnedAward(
                        id           = d.id,
                        earnedAt     = d.getLong("earnedAt") ?: 0L,
                        lastEarnedAt = d.getLong("lastEarnedAt") ?: (d.getLong("earnedAt") ?: 0L),
                        timesEarned  = (d.getLong("timesEarned") ?: 1L).toInt(),
                        value        = (d.getLong("value") ?: 0L).toInt()
                    )
                }
            }

            val todayKey = fmt.format(Date())
            val newly    = ArrayList<AwardDef>()

            fun earn(defId: String, value: Int = 0) {
                val d   = def(defId) ?: return
                val now = System.currentTimeMillis()
                val prev = earned[defId]
                if (prev == null) {
                    earned[defId] = EarnedAward(defId, now, now, 1, value)
                    newly += d
                } else if (d.repeatable) {
                    earned[defId] = prev.copy(
                        lastEarnedAt = now,
                        timesEarned  = prev.timesEarned + 1,
                        value        = maxOf(prev.value, value)
                    )
                    newly += d
                }
            }

            // ── Fold completed days (lastCountedDate+1 .. yesterday) ─────────
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -1)
            val yesterdayKey = fmt.format(cal.time)

            val startCal = Calendar.getInstance()
            if (stats.lastCountedDate.isEmpty()) {
                // New user (or first run): begin folding 30 days back so recent
                // history counts, without scanning the whole account.
                startCal.add(Calendar.DAY_OF_YEAR, -30)
            } else {
                val parsed = runCatching { fmt.parse(stats.lastCountedDate) }.getOrNull()
                if (parsed == null) startCal.add(Calendar.DAY_OF_YEAR, -30)
                else { startCal.time = parsed; startCal.add(Calendar.DAY_OF_YEAR, 1) }
            }
            // Cap the catch-up window so a long-dormant account can't trigger
            // a huge scan; days before the cap count as misses (streak resets).
            val capCal = Calendar.getInstance()
            capCal.add(Calendar.DAY_OF_YEAR, -CATCHUP_CAP)
            if (startCal.before(capCal)) startCal.time = capCal.time

            var curStreak   = stats.currentStreak
            var bestStreak  = stats.bestStreak
            var goalDays    = stats.goalDaysTotal
            var bestSteps   = stats.bestSteps
            var waterStreak = stats.waterStreak
            var monthKey    = stats.monthKey
            var monthDays   = stats.monthGoalDays
            var weekDays    = stats.weekGoalDays

            val startKey = fmt.format(startCal.time)
            if (startKey <= yesterdayKey) {
                // One range query for the whole window (doc IDs sort by date).
                val logs = dailyCol(uid)
                    .whereGreaterThanOrEqualTo(FieldPath.documentId(), startKey)
                    .whereLessThanOrEqualTo(FieldPath.documentId(), yesterdayKey)
                    .get().await()
                    .documents.associateBy({ it.id }, { doc ->
                        Pair(
                            (doc.getLong("stepsCount") ?: 0L).toInt(),
                            (doc.getLong("waterGlasses") ?: 0L).toInt()
                        )
                    })

                val c = Calendar.getInstance().apply { time = startCal.time }
                while (true) {
                    val key = fmt.format(c.time)
                    if (key > yesterdayKey) break
                    val (steps, water) = logs[key] ?: Pair(0, 0)

                    // Week & month boundaries reset BEFORE counting the day
                    if (c.get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY) weekDays = 0
                    val mk = key.substring(0, 7)
                    if (mk != monthKey) { monthKey = mk; monthDays = 0 }

                    if (steps >= STEP_GOAL) {
                        curStreak++; goalDays++; monthDays++; weekDays++
                        if (curStreak > bestStreak) bestStreak = curStreak
                        STREAK_TIERS.forEach { t -> if (curStreak == t) earn("streak_$t") }
                        MILESTONE_TIERS.forEach { t -> if (goalDays == t) earn("goal_days_$t") }
                        if (monthDays == MONTHLY_TARGET) earn("monthly_challenge")
                    } else {
                        curStreak = 0
                    }

                    if (steps > bestSteps) {
                        if (bestSteps > 0) earn("step_record", steps)
                        bestSteps = steps
                    }

                    if (water >= WATER_GOAL) {
                        waterStreak++
                        if (waterStreak == 7) earn("hydration_7")
                    } else waterStreak = 0

                    if (c.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                        if (weekDays == 7) earn("perfect_week")
                        weekDays = 0
                    }
                    c.add(Calendar.DAY_OF_YEAR, 1)
                }
            }

            // ── Workouts (aggregate counts — cheap, no per-doc reads) ────────
            var workoutsTotal = 0
            var workoutsWeek  = 0
            var workoutWeekKey = stats.workoutWeekKey
            try {
                workoutsTotal = workoutLogs(uid).count()
                    .get(AggregateSource.SERVER).await().count.toInt()
                val monday = Calendar.getInstance().apply {
                    firstDayOfWeek = Calendar.MONDAY
                    set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }
                workoutsWeek = workoutLogs(uid)
                    .whereGreaterThanOrEqualTo("dateMillis", monday.timeInMillis)
                    .count().get(AggregateSource.SERVER).await().count.toInt()
                // Lifetime tiers: <= comparison (aggregate can jump past a tier)
                WORKOUT_TIERS.forEach { t ->
                    if (workoutsTotal >= t && earned["workout_$t"] == null) earn("workout_$t")
                }
                val weekKey = fmt.format(monday.time)
                if (workoutsWeek >= WORKOUT_WEEK_TARGET && workoutWeekKey != weekKey) {
                    earn("workout_week_5")
                    workoutWeekKey = weekKey
                }
            } catch (_: Exception) { /* aggregate unsupported/offline — skip */ }

            // ── Today (live, never folded) ───────────────────────────────────
            val todayDoc   = runCatching { dailyCol(uid).document(todayKey).get().await() }.getOrNull()
            val todaySteps = (todayDoc?.getLong("stepsCount") ?: 0L).toInt()
            val todayMet   = todaySteps >= STEP_GOAL
            val liveStreak = curStreak + if (todayMet) 1 else 0

            // ── Persist ──────────────────────────────────────────────────────
            val newStats = AwardStats(
                lastCountedDate = yesterdayKey,
                currentStreak   = curStreak,
                bestStreak      = bestStreak,
                goalDaysTotal   = goalDays,
                bestSteps       = bestSteps,
                waterStreak     = waterStreak,
                monthKey        = monthKey,
                monthGoalDays   = monthDays,
                weekGoalDays    = weekDays,
                workoutWeekKey  = workoutWeekKey
            )
            awardsCol(uid).document(STATS_DOC).set(mapOf(
                "lastCountedDate" to newStats.lastCountedDate,
                "currentStreak"   to newStats.currentStreak,
                "bestStreak"      to newStats.bestStreak,
                "goalDaysTotal"   to newStats.goalDaysTotal,
                "bestSteps"       to newStats.bestSteps,
                "waterStreak"     to newStats.waterStreak,
                "monthKey"        to newStats.monthKey,
                "monthGoalDays"   to newStats.monthGoalDays,
                "weekGoalDays"    to newStats.weekGoalDays,
                "workoutWeekKey"  to newStats.workoutWeekKey,
                "updatedAt"       to System.currentTimeMillis()
            ), SetOptions.merge()).await()

            newly.forEach { d ->
                val e = earned[d.id] ?: return@forEach
                awardsCol(uid).document(d.id).set(mapOf(
                    "earnedAt"     to e.earnedAt,
                    "lastEarnedAt" to e.lastEarnedAt,
                    "timesEarned"  to e.timesEarned,
                    "value"        to e.value
                ), SetOptions.merge()).await()
            }

            AwardsState(
                stats            = newStats,
                earned           = earned,
                newlyEarned      = newly,
                todaySteps       = todaySteps,
                todayGoalMet     = todayMet,
                liveStreak       = liveStreak,
                workoutsTotal    = workoutsTotal,
                workoutsThisWeek = workoutsWeek
            )
        } catch (_: Exception) {
            null   // offline / signed out / rules — caller degrades gracefully
        }
    }
}

package com.example.data

import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Estimates body-composition progress (fat / muscle) from logged data —
 * food calories + macros, steps, water, sleep — measured against the user's
 * goal and their computed TDEE.
 *
 * IMPORTANT: outputs are *estimates* based on energy-balance math
 * (~7700 kcal ≈ 1 kg), not measurements. They are most accurate when the user
 * logs consistently and periodically records real weight. All numbers are
 * clearly labelled "estimated" in the UI.
 */
object ProgressAnalytics {

    const val KCAL_PER_KG = 7700f
    const val STEP_KCAL   = 0.045f   // ~kcal burned per step (typical adult)

    data class DayData(
        val date: String,
        val caloriesIn: Int,
        val proteinG: Float,
        val carbsG: Float,
        val fatG: Float,
        val steps: Int,
        val activeKcal: Int,     // from the health log; 0 → derive from steps
        val waterGlasses: Int,
        val sleepHours: Float,
    )

    data class Metric(
        val label: String,
        val value: Float,
        val target: Float,
        val unit: String,
        val onTrack: Boolean,
    )

    data class Report(
        val loggedDays: Int,
        val totalDays: Int,
        val avgCaloriesIn: Int,
        val avgTdee: Int,
        val avgBalance: Int,       // + surplus / − deficit, per day
        val estFatKg: Float,       // negative = lost
        val estMuscleKg: Float,    // positive = gained
        val onTrackScore: Int,     // 0..100
        val verdict: String,
        val verdictDetail: String,
        val metrics: List<Metric>,
        val balanceSeries: List<Int>,   // per logged day, oldest→newest
    ) {
        val hasData get() = loggedDays > 0
    }

    fun emptyReport(totalDays: Int) = Report(
        0, totalDays, 0, 0, 0, 0f, 0f, 0,
        "Start logging", "Log your food and daily habits to see whether you're on track for your goal.",
        emptyList(), emptyList()
    )

    /**
     * @param days   newest-first or oldest-first — order-independent; sorted internally by date.
     * @param profile computed HealthProfile (BMR/TDEE/weight/protein target).
     * @param goal   the user's fitness goal.
     */
    fun analyze(
        days: List<DayData>,
        bmrKcal: Int,
        tdeeKcal: Int,
        weightKg: Float,
        proteinTargetG: Int,
        goal: ClientGoal,
        stepGoal: Int = 8000,
    ): Report {
        val totalDays = days.size
        val logged = days.filter { it.caloriesIn > 0 }.sortedBy { it.date }
        if (logged.isEmpty()) return emptyReport(totalDays)

        val bmr    = bmrKcal
        val tdee   = tdeeKcal
        val weight = weightKg
        val proteinTarget = if (proteinTargetG > 0) proteinTargetG.toFloat()
                            else if (weight > 0) weight * 1.6f else 100f

        // Per-day TDEE: prefer BMR + real active calories (uses steps); else static TDEE.
        fun dayTdee(d: DayData): Int {
            if (bmr > 0) {
                val active = if (d.activeKcal > 0) d.activeKcal else (d.steps * STEP_KCAL).roundToInt()
                return (bmr * 1.2f).roundToInt() + active
            }
            return if (tdee > 0) tdee else 2000
        }

        val balances = logged.map { it.caloriesIn - dayTdee(it) }
        val cumulative = balances.sum()
        val avgIn   = logged.map { it.caloriesIn }.average().roundToInt()
        val avgTdee = logged.map { dayTdee(it) }.average().roundToInt()
        val avgBal  = balances.average().roundToInt()

        val avgProtein = logged.map { it.proteinG }.average().toFloat()
        val avgSteps   = logged.map { it.steps }.average().toFloat()
        val avgWater   = logged.map { it.waterGlasses }.average().toFloat()
        val sleepDays  = logged.filter { it.sleepHours > 0f }
        val avgSleep   = if (sleepDays.isEmpty()) 0f else sleepDays.map { it.sleepHours }.average().toFloat()
        val proteinAdequate = avgProtein >= proteinTarget * 0.9f
        val weeks = (logged.size / 7f).coerceAtLeast(0.14f)

        // Estimated mass change from cumulative energy balance
        val estMass = cumulative / KCAL_PER_KG
        var estFat: Float
        var estMuscle: Float
        if (estMass < 0f) {
            // Losing: mostly fat; protein preserves muscle
            val fatFrac = if (proteinAdequate) 0.9f else 0.7f
            estFat    = estMass * fatFrac
            estMuscle = estMass * (1f - fatFrac)          // small (lean/water) loss
        } else if (estMass > 0f) {
            // Gaining: cap the muscle share by a realistic natural rate
            val capPerWeek = if (goal == ClientGoal.BUILD_MUSCLE) 0.5f else 0.25f
            val maxMuscle  = capPerWeek * weeks * (if (proteinAdequate) 1f else 0.5f)
            estMuscle = min(estMass, maxMuscle)
            estFat    = estMass - estMuscle
        } else { estFat = 0f; estMuscle = 0f }

        // ── On-track score ─────────────────────────────────────────────────────
        // Calorie direction vs goal
        val calorieScore = when (goal) {
            ClientGoal.LOSE_FAT      -> scoreDeficit(avgBal)          // want a moderate deficit
            ClientGoal.BUILD_MUSCLE  -> scoreSurplus(avgBal)          // want a slight surplus
            else                     -> scoreMaintain(avgBal)         // near zero
        }
        val proteinScore = (avgProtein / proteinTarget).coerceIn(0f, 1f)
        val stepScore    = (avgSteps / stepGoal).coerceIn(0f, 1f)
        val waterScore   = (avgWater / 8f).coerceIn(0f, 1f)
        val sleepScore   = if (avgSleep in 7f..9f) 1f
                           else if (avgSleep <= 0f) 0.5f
                           else (avgSleep / 8f).coerceIn(0f, 1f)

        val score = (calorieScore * 0.35f + proteinScore * 0.25f + stepScore * 0.15f +
                     waterScore * 0.10f + sleepScore * 0.15f) * 100f
        val scoreInt = score.roundToInt().coerceIn(0, 100)

        val verdict = when {
            scoreInt >= 75 -> "On track ✅"
            scoreInt >= 50 -> "Almost there"
            else           -> "Off track"
        }
        val detail = buildDetail(goal, avgBal, proteinAdequate, avgProtein, proteinTarget, avgSteps, stepGoal, avgSleep)

        val metrics = listOf(
            Metric("Calories", avgIn.toFloat(), avgTdee.toFloat(), "kcal", onTrack = calorieScore >= 0.6f),
            Metric("Protein", avgProtein, proteinTarget, "g", onTrack = proteinScore >= 0.85f),
            Metric("Steps", avgSteps, stepGoal.toFloat(), "", onTrack = stepScore >= 0.75f),
            Metric("Water", avgWater, 8f, "glasses", onTrack = waterScore >= 0.75f),
            Metric("Sleep", avgSleep, 8f, "h", onTrack = avgSleep in 7f..9f),
        )

        return Report(
            loggedDays = logged.size,
            totalDays = totalDays,
            avgCaloriesIn = avgIn,
            avgTdee = avgTdee,
            avgBalance = avgBal,
            estFatKg = estFat,
            estMuscleKg = estMuscle,
            onTrackScore = scoreInt,
            verdict = verdict,
            verdictDetail = detail,
            metrics = metrics,
            balanceSeries = balances,
        )
    }

    // deficit target ≈ −300 to −700 kcal/day
    private fun scoreDeficit(bal: Int): Float = when {
        bal in -700..-200 -> 1f
        bal in -1100..-100 -> 0.7f
        bal < 0 -> 0.5f
        else -> 0.2f
    }
    // surplus target ≈ +100 to +400 kcal/day
    private fun scoreSurplus(bal: Int): Float = when {
        bal in 100..400 -> 1f
        bal in 0..700 -> 0.7f
        bal > 700 -> 0.5f
        else -> 0.3f
    }
    private fun scoreMaintain(bal: Int): Float = when {
        abs(bal) <= 200 -> 1f
        abs(bal) <= 400 -> 0.7f
        else -> 0.4f
    }

    private fun buildDetail(
        goal: ClientGoal, avgBal: Int, proteinOk: Boolean, avgProtein: Float,
        proteinTarget: Float, avgSteps: Float, stepGoal: Int, avgSleep: Float,
    ): String {
        val tips = mutableListOf<String>()
        when (goal) {
            ClientGoal.LOSE_FAT -> if (avgBal >= 0)
                tips += "You're eating around maintenance — a ${-(avgBal + 400)} kcal/day cut would drive fat loss."
            ClientGoal.BUILD_MUSCLE -> if (avgBal < 100)
                tips += "Add ~${(200 - avgBal).coerceAtLeast(100)} kcal/day — a slight surplus fuels muscle growth."
            else -> {}
        }
        if (!proteinOk) tips += "Protein is low (${avgProtein.roundToInt()}g vs ${proteinTarget.roundToInt()}g target) — key for keeping/adding muscle."
        if (avgSteps < stepGoal * 0.7f) tips += "Steps are under target — more daily movement widens your deficit."
        if (avgSleep in 0.1f..6.4f) tips += "Sleep under 7h blunts fat loss and recovery."
        return if (tips.isEmpty()) "Great consistency — keep it up and your goal trend will hold." else tips.joinToString(" ")
    }
}

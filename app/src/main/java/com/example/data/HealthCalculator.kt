package com.example.data

import kotlin.math.log10
import kotlin.math.roundToInt

/**
 * All formulas sourced from peer-reviewed literature:
 *
 * BMI           : Quetelet (1832) — weight(kg) / height(m)²
 * BMR           : Mifflin & St Jeor, JADA 1990 — ADA recommended since 2005
 * TDEE          : Harris & Benedict activity multipliers (revised 1984)
 * Body Fat %    : Deurenberg et al., IJCO 1991 (BMI-based, ±3-4%)
 *                 U.S. Navy method, Hodgdon & Beckett 1984 (±3%)
 * Body Fat Cat. : ACSM Guidelines for Exercise Testing 2021
 * Protein       : ISSN Position Stand 2017 + ESPEN Senior Guidelines 2019
 * Fat %         : Dietary Reference Intakes, IoM 2005 (20-35% AMDR)
 * Water         : EFSA 2010 — 35ml/kg/day
 */
object HealthCalculator {

    // ─── BMI ─────────────────────────────────────────────────────────────────

    fun bmi(weightKg: Float, heightCm: Float): Float {
        val hM = heightCm / 100f
        return weightKg / (hM * hM)
    }

    fun bmiCategory(bmi: Float): Pair<String, Long> = when {
        bmi < 16f   -> "Severely Underweight" to 0xFF1D4ED8L
        bmi < 18.5f -> "Underweight"           to 0xFF3B82F6L
        bmi < 25f   -> "Normal"                to 0xFF10B981L
        bmi < 30f   -> "Overweight"            to 0xFFF59E0BL
        bmi < 35f   -> "Obese I"               to 0xFFF97316L
        else        -> "Obese II+"             to 0xFFEF4444L
    }

    // ─── BMR (Mifflin-St Jeor, 1990) ─────────────────────────────────────────
    // Most accurate single-equation BMR formula validated against indirect calorimetry

    fun bmr(weightKg: Float, heightCm: Float, ageYears: Int, gender: Gender): Int =
        when (gender) {
            Gender.MALE   -> (10f * weightKg + 6.25f * heightCm - 5f * ageYears + 5f).roundToInt()
            Gender.FEMALE -> (10f * weightKg + 6.25f * heightCm - 5f * ageYears - 161f).roundToInt()
        }

    // ─── Body Fat % ───────────────────────────────────────────────────────────

    /**
     * U.S. Navy Method (Hodgdon & Beckett 1984).
     * Requires waist, neck (+ hip for women) in cm.
     * Accuracy: ±3% of DEXA scan.
     */
    fun bodyFatNavy(profile: HealthProfile): Float? {
        if (!profile.hasNavyMeasurements) return null
        val h = profile.heightCm.toDouble()
        return try {
            if (profile.gender == Gender.MALE) {
                val waistNeck = profile.waistCm - profile.neckCm
                if (waistNeck <= 0) return null
                (495.0 / (1.0324 - 0.19077 * log10(waistNeck.toDouble())
                        + 0.15456 * log10(h)) - 450.0).toFloat()
            } else {
                val waistHipNeck = profile.waistCm + profile.hipCm - profile.neckCm
                if (waistHipNeck <= 0) return null
                (495.0 / (1.29579 - 0.35004 * log10(waistHipNeck.toDouble())
                        + 0.22100 * log10(h)) - 450.0).toFloat()
            }
        } catch (_: Exception) { null }
    }

    /**
     * Deurenberg BMI-based formula (Deurenberg et al., IJCO 1991).
     * Validated against hydrostatic weighing. Accuracy: ±3-4%.
     * sex coefficient: 1 = male, 0 = female.
     */
    fun bodyFatDeurenberg(bmi: Float, ageYears: Int, gender: Gender): Float {
        val sex = if (gender == Gender.MALE) 1 else 0
        return (1.20f * bmi + 0.23f * ageYears - 10.8f * sex - 5.4f).coerceIn(2f, 60f)
    }

    /**
     * ACSM 2021 body fat categories.
     * Essential fat thresholds: men < 6%, women < 14%
     */
    fun bodyFatCategory(bf: Float, gender: Gender): BodyFatCategory = when (gender) {
        Gender.MALE -> when {
            bf < 6f  -> BodyFatCategory.ESSENTIAL_FAT
            bf < 14f -> BodyFatCategory.ATHLETIC
            bf < 18f -> BodyFatCategory.FITNESS
            bf < 25f -> BodyFatCategory.AVERAGE
            else     -> BodyFatCategory.OBESE
        }
        Gender.FEMALE -> when {
            bf < 14f -> BodyFatCategory.ESSENTIAL_FAT
            bf < 21f -> BodyFatCategory.ATHLETIC
            bf < 25f -> BodyFatCategory.FITNESS
            bf < 32f -> BodyFatCategory.AVERAGE
            else     -> BodyFatCategory.OBESE
        }
    }

    // ─── Main calculation ─────────────────────────────────────────────────────

    fun calculate(profile: HealthProfile): HealthMetrics? {
        if (!profile.isValid) return null

        val bmiVal = bmi(profile.weightKg, profile.heightCm)
        val (bmiCat, bmiColor) = bmiCategory(bmiVal)
        val bmrVal = bmr(profile.weightKg, profile.heightCm, profile.ageYears, profile.gender)

        // TDEE = BMR × activity multiplier (Harris-Benedict revised activity factors)
        val tdeeBase = (bmrVal * profile.activityLevel.multiplier).roundToInt()

        // Goal-based calorie adjustment:
        //   Fat loss:    500 kcal deficit (safe 0.45 kg/week loss rate)
        //   Muscle gain: 300 kcal surplus (lean bulk to minimise fat gain)
        val tdee = when (profile.goal) {
            ClientGoal.LOSE_FAT     -> tdeeBase - 500
            ClientGoal.BUILD_MUSCLE -> tdeeBase + 300
            else                    -> tdeeBase
        }.coerceAtLeast(1200)       // 1200 kcal is accepted minimum safe intake

        // ── Protein (ISSN 2017 + ESPEN 2019 for seniors) ──────────────────────
        // Ranges based on evidence synthesis:
        //   BUILD_MUSCLE:     2.0-2.2 g/kg (upper range for maximal MPS stimulus)
        //   LOSE_FAT:         1.8-2.0 g/kg (high protein preserves LBM in deficit)
        //   IMPROVE_CARDIO:   1.4-1.6 g/kg (supports aerobic adaptation + recovery)
        //   IMPROVE_FLEXIBILITY: 1.2-1.4 g/kg (maintenance)
        //   GENERAL_FITNESS:  1.4-1.6 g/kg; seniors → 1.6-2.0 g/kg (anabolic resistance)
        val proteinPerKg: Float = when (profile.goal) {
            ClientGoal.BUILD_MUSCLE      -> if (profile.ageYears >= 50) 2.0f else 2.2f
            ClientGoal.LOSE_FAT          -> if (profile.ageYears >= 50) 2.0f else 1.8f
            ClientGoal.IMPROVE_CARDIO    -> 1.6f
            ClientGoal.IMPROVE_FLEXIBILITY -> 1.2f
            ClientGoal.GENERAL_FITNESS   -> if (profile.ageYears >= 60) 1.6f else 1.4f
        }
        val proteinG    = (profile.weightKg * proteinPerKg).roundToInt()
        val proteinKcal = proteinG * 4

        // ── Fat (IoM AMDR 2005: 20-35% of total calories) ─────────────────────
        // Seniors: higher end (30%) supports hormone production & fat-soluble vitamins
        val fatPct  = if (profile.ageYears >= 50) 0.30f else 0.27f
        val fatG    = ((tdee * fatPct) / 9f).roundToInt()
        val fatKcal = fatG * 9

        // ── Carbohydrates: remaining calories after protein & fat ──────────────
        val carbsKcal = (tdee - proteinKcal - fatKcal).coerceAtLeast(0)
        val carbsG    = carbsKcal / 4

        // ── Water (EFSA 2010: 35 ml/kg/day, minimum 2.0 L) ────────────────────
        val waterL = (profile.weightKg * 0.035f).coerceAtLeast(2.0f)

        // ── Ideal weight (BMI 18.5-24.9 for given height) ─────────────────────
        val hM         = profile.heightCm / 100f
        val idealMin   = 18.5f * hM * hM
        val idealMax   = 24.9f * hM * hM

        // ── Body fat % — Navy method preferred; Deurenberg as fallback ─────────
        val navyBf = bodyFatNavy(profile)
        val bfPercent: Float
        val bfMethod: String
        if (navyBf != null && navyBf > 2f && navyBf < 60f) {
            bfPercent = navyBf
            bfMethod  = "U.S. Navy Method"
        } else {
            bfPercent = bodyFatDeurenberg(bmiVal, profile.ageYears, profile.gender)
            bfMethod  = "Deurenberg (BMI-based)"
        }
        val bfCategory = bodyFatCategory(bfPercent, profile.gender)
        val leanMassKg = profile.weightKg * (1f - bfPercent / 100f)
        val fatMassKg  = profile.weightKg * (bfPercent / 100f)

        return HealthMetrics(
            bmi              = bmiVal,
            bmiCategory      = bmiCat,
            bmiColor         = bmiColor,
            bodyFatPercent   = bfPercent,
            bodyFatCategory  = bfCategory,
            leanMassKg       = leanMassKg,
            fatMassKg        = fatMassKg,
            bodyFatMethod    = bfMethod,
            bmrKcal          = bmrVal,
            tdeeKcal         = tdee,
            dailyProteinG    = proteinG,
            dailyCarbsG      = carbsG,
            dailyFatG        = fatG,
            dailyWaterL      = waterL,
            idealWeightMinKg = idealMin,
            idealWeightMaxKg = idealMax,
            proteinPerKg     = proteinPerKg
        )
    }

    // ─── Personalized daily targets (drives the whole app) ────────────────────
    // Turns the body profile into the day's goals shown on Home / Fitness /
    // Nutrition. Falls back to sensible defaults when the profile isn't set yet.

    fun dailyTargets(profile: HealthProfile): DailyTargets {
        val m = calculate(profile)   // null until the profile is filled in
        // Step goal scales with self-reported activity level.
        val stepGoal = when (profile.activityLevel) {
            ActivityLevel.SEDENTARY -> 6_000
            ActivityLevel.LIGHT     -> 8_000
            ActivityLevel.MODERATE  -> 10_000
            ActivityLevel.ACTIVE    -> 12_000
            ActivityLevel.ATHLETE   -> 14_000
        }
        // 250 ml per glass; keep it in a sane range.
        val glasses = m?.let { (it.dailyWaterL / 0.25f).roundToInt().coerceIn(6, 16) } ?: 8
        return DailyTargets(
            hasProfile    = m != null,
            stepGoal      = if (m != null) stepGoal else 10_000,
            waterGlasses  = glasses,
            calorieTarget = m?.tdeeKcal ?: 0,
            proteinG      = m?.dailyProteinG ?: 0,
            carbsG        = m?.dailyCarbsG ?: 0,
            fatG          = m?.dailyFatG ?: 0
        )
    }

    fun ageGroupFor(age: Int): AgeGroup = when {
        age < 18 -> AgeGroup.TEEN
        age < 36 -> AgeGroup.YOUNG_ADULT
        age < 56 -> AgeGroup.MIDDLE_AGED
        else     -> AgeGroup.SENIOR
    }

    fun exercisesForAge(age: Int): List<Exercise> =
        ExerciseRepository.byAgeGroup(ageGroupFor(age))

    /** Convert a HealthMetrics result + profile into a Firestore-ready HealthRecord */
    fun toRecord(profile: HealthProfile, metrics: HealthMetrics): HealthRecord {
        val now = System.currentTimeMillis()
        return HealthRecord(
            id              = now.toString(),
            recordedAt      = now,
            weightKg        = profile.weightKg,
            heightCm        = profile.heightCm,
            ageYears        = profile.ageYears,
            gender          = profile.gender.name,
            activityLevel   = profile.activityLevel.name,
            goal            = profile.goal.name,
            waistCm         = profile.waistCm,
            neckCm          = profile.neckCm,
            hipCm           = profile.hipCm,
            bmi             = metrics.bmi,
            bmiCategory     = metrics.bmiCategory,
            bodyFatPercent  = metrics.bodyFatPercent,
            bodyFatCategory = metrics.bodyFatCategory.label,
            bodyFatMethod   = metrics.bodyFatMethod,
            leanMassKg      = metrics.leanMassKg,
            fatMassKg       = metrics.fatMassKg,
            bmrKcal         = metrics.bmrKcal,
            tdeeKcal        = metrics.tdeeKcal,
            dailyProteinG   = metrics.dailyProteinG,
            dailyCarbsG     = metrics.dailyCarbsG,
            dailyFatG       = metrics.dailyFatG,
            dailyWaterL     = metrics.dailyWaterL
        )
    }
}

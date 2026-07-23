package com.example.data

// ─── Enums ────────────────────────────────────────────────────────────────────

enum class ExerciseCategory(val label: String, val emoji: String) {
    STRENGTH("Strength", "🏋️"),
    YOGA("Yoga", "🧘"),
    CARDIO("Cardio", "🏃"),
    HIIT("HIIT", "⚡"),
    FLEXIBILITY("Flexibility", "🤸")
}

enum class MuscleGroup(val label: String, val emoji: String) {
    CHEST("Chest", "💪"),
    BACK("Back", "🔙"),
    SHOULDERS("Shoulders", "🏋️"),
    BICEPS("Biceps", "💪"),
    TRICEPS("Triceps", "💪"),
    FOREARMS("Forearms", "🦾"),
    CORE("Core", "🎯"),
    QUADS("Quads", "🦵"),
    HAMSTRINGS("Hamstrings", "🦵"),
    GLUTES("Glutes", "🍑"),
    CALVES("Calves", "🦵"),
    FULL_BODY("Full Body", "🧍"),
    HIP_FLEXORS("Hip Flexors", "🔄"),
    SPINE("Spine", "🦴")
}

enum class EquipmentType(val label: String) {
    BODYWEIGHT("Bodyweight"),
    BARBELL("Barbell"),
    DUMBBELL("Dumbbell"),
    KETTLEBELL("Kettlebell"),
    CABLE("Cable"),
    MACHINE("Machine"),
    RESISTANCE_BAND("Resistance Band"),
    MAT("Yoga Mat"),
    PULL_UP_BAR("Pull-up Bar")
}

enum class DifficultyLevel(val label: String, val color: Long) {
    BEGINNER("Beginner", 0xFF10B981),
    INTERMEDIATE("Intermediate", 0xFFF59E0B),
    ADVANCED("Advanced", 0xFFEF4444)
}

enum class ClientGoal(val label: String, val emoji: String) {
    BUILD_MUSCLE("Build Muscle", "💪"),
    LOSE_FAT("Lose Fat", "🔥"),
    IMPROVE_CARDIO("Improve Cardio", "❤️"),
    IMPROVE_FLEXIBILITY("Improve Flexibility", "🤸"),
    GENERAL_FITNESS("General Fitness", "⭐")
}

// Body fat category (ACSM 2021 standards)
enum class BodyFatCategory(val label: String, val color: Long) {
    ESSENTIAL_FAT("Essential Fat", 0xFF3B82F6L),
    ATHLETIC(     "Athletic",      0xFF10B981L),
    FITNESS(      "Fitness",       0xFF84CC16L),
    AVERAGE(      "Average",       0xFFF59E0BL),
    OBESE(        "Obese",         0xFFEF4444L)
}

enum class AgeGroup(val label: String, val range: String, val emoji: String) {
    TEEN("Teen", "15–17", "🧒"),
    YOUNG_ADULT("Young Adult", "18–35", "🧑"),
    MIDDLE_AGED("Middle Aged", "36–55", "🧔"),
    SENIOR("Senior", "56+", "👴")
}

enum class Gender(val label: String) {
    MALE("Male"),
    FEMALE("Female")
}

enum class ActivityLevel(val label: String, val multiplier: Float) {
    SEDENTARY("Sedentary (desk job, no exercise)", 1.2f),
    LIGHT("Light active (1–3 days/week)", 1.375f),
    MODERATE("Moderately active (3–5 days/week)", 1.55f),
    ACTIVE("Very active (6–7 days/week)", 1.725f),
    ATHLETE("Athlete (2× training/day)", 1.9f)
}

// ─── Health profile (stored in UserPreferences) ───────────────────────────────

data class HealthProfile(
    val ageYears: Int = 0,
    val heightCm: Float = 0f,
    val weightKg: Float = 0f,
    val gender: Gender = Gender.MALE,
    val activityLevel: ActivityLevel = ActivityLevel.MODERATE,
    val goal: ClientGoal = ClientGoal.GENERAL_FITNESS,
    // Optional — enables more accurate U.S. Navy body fat method
    val waistCm: Float = 0f,
    val neckCm: Float = 0f,
    val hipCm: Float = 0f       // required for women (Navy method)
) {
    val isValid get() = ageYears in 10..100 && heightCm > 50f && weightKg > 10f
    val hasNavyMeasurements get() = waistCm > 0f && neckCm > 0f &&
        (gender == Gender.MALE || hipCm > 0f)
}

// ─── Calculated health metrics ────────────────────────────────────────────────

data class HealthMetrics(
    val bmi: Float,
    val bmiCategory: String,
    val bmiColor: Long,
    val bodyFatPercent: Float,
    val bodyFatCategory: BodyFatCategory,
    val leanMassKg: Float,
    val fatMassKg: Float,
    val bodyFatMethod: String,      // "Navy" or "Deurenberg (BMI-based)"
    val bmrKcal: Int,
    val tdeeKcal: Int,
    val dailyProteinG: Int,
    val dailyCarbsG: Int,
    val dailyFatG: Int,
    val dailyWaterL: Float,
    val idealWeightMinKg: Float,
    val idealWeightMaxKg: Float,
    val proteinPerKg: Float
)

// ─── Personalized daily targets (derived from the body profile) ───────────────
// The single source of truth for the goals shown on Home / Fitness / Nutrition.

data class DailyTargets(
    val hasProfile: Boolean,     // false = profile not set, values are defaults
    val stepGoal: Int,
    val waterGlasses: Int,
    val calorieTarget: Int,      // 0 when no profile
    val proteinG: Int,           // 0 when no profile
    val carbsG: Int = 0,
    val fatG: Int = 0
)

// ─── Persisted health record (stored in Firestore member_health/) ─────────────

data class HealthRecord(
    val id: String = "",
    val recordedAt: Long = 0L,
    val weightKg: Float = 0f,
    val heightCm: Float = 0f,
    val ageYears: Int = 0,
    val gender: String = "",
    val activityLevel: String = "",
    val goal: String = "",
    val waistCm: Float = 0f,
    val neckCm: Float = 0f,
    val hipCm: Float = 0f,
    // Calculated results
    val bmi: Float = 0f,
    val bmiCategory: String = "",
    val bodyFatPercent: Float = 0f,
    val bodyFatCategory: String = "",
    val bodyFatMethod: String = "",
    val leanMassKg: Float = 0f,
    val fatMassKg: Float = 0f,
    val bmrKcal: Int = 0,
    val tdeeKcal: Int = 0,
    val dailyProteinG: Int = 0,
    val dailyCarbsG: Int = 0,
    val dailyFatG: Int = 0,
    val dailyWaterL: Float = 0f,
    val notes: String = ""
)

// ─── Exercise ─────────────────────────────────────────────────────────────────

data class Exercise(
    val id: String,
    val name: String,
    val sanskritName: String? = null,
    val category: ExerciseCategory,
    val primaryMuscles: List<MuscleGroup>,
    val secondaryMuscles: List<MuscleGroup> = emptyList(),
    val equipment: EquipmentType,
    val difficulty: DifficultyLevel,
    val suitableFor: List<ClientGoal>,
    val ageGroups: List<AgeGroup> = AgeGroup.entries.toList(),
    /** "3-4 sets" */
    val sets: String,
    /** "8-12 reps" or "30 sec" */
    val reps: String,
    /** eccentric-pause-concentric-top e.g. "3-1-2-0" */
    val tempo: String,
    /** "60-90 sec" */
    val rest: String,
    val howTo: List<String>,
    val commonErrors: List<String>,
    val benefits: List<String>,
    /** Plain-language physiology */
    val bodyEffect: String,
    val caloriesBurned: String,
    val muscleEmoji: String,
    val estimatedMinutes: Int = 15,
    /** Direct Unsplash photo URL – stable, free, no API key */
    val imageUrl: String = "",
    /** Animation GIF URL (e.g. Gym visual 180×180). Empty = show static image only. */
    val gifUrl: String = "",
    /** Media credit shown on the detail screen, e.g. "© Gym visual — https://gymvisual.com/". */
    val attribution: String = ""
)

// ─── Nutrition ────────────────────────────────────────────────────────────────

data class IndianFoodItem(
    val name: String,
    val quantity: String,
    val calories: Int,
    val proteinG: Int,
    val carbsG: Int,
    val fatG: Int,
    val benefits: String,
    val isVegetarian: Boolean = true,
    val imageUrl: String = ""
)

data class IndianMeal(
    val name: String,
    val timeSlot: String,
    val items: List<IndianFoodItem>,
    val notes: String = ""
)

data class IndianMealPlan(
    val goal: ClientGoal,
    val dailyCalories: Int,
    val proteinG: Int,
    val carbsG: Int,
    val fatG: Int,
    val hydration: String,
    val generalTips: List<String>,
    val meals: List<IndianMeal>,
    val headerImageUrl: String = "",
    val iconImageUrl: String = ""
)

// ─── Per-set data (each set logged individually) ─────────────────────────────

data class SetDetail(
    val setIndex: Int = 0,
    val reps: Int = 0,
    val weightKg: Float = 0f,
    val durationSecs: Int = 0,   // for time-based exercises (planks, holds)
    val isWarmup: Boolean = false,
    val rpe: Int = 0             // Rate of Perceived Exertion 1–10, 0 = not tracked
)

// ─── Fitness tracking ─────────────────────────────────────────────────────────

data class FitnessGoalEntry(
    val exerciseId: String,
    val exerciseName: String,
    val targetSets: Int,
    val targetReps: Int,
    val targetWeightKg: Float = 0f,
    val targetDurationSecs: Int = 0,
    val createdAtMillis: Long = System.currentTimeMillis()
)

data class WorkoutLogEntry(
    val id: String,
    val exerciseId: String,
    val exerciseName: String,
    val dateMillis: Long,
    val setsCompleted: Int,       // total working sets (warmups excluded)
    val repsCompleted: Int,       // reps of last/heaviest set (for progress tracking)
    val weightKg: Float = 0f,    // weight of last/heaviest set
    val durationSecs: Int = 0,
    val notes: String = "",
    val setDetails: List<SetDetail> = emptyList()  // each set logged individually
)

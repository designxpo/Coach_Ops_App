package com.example.data

data class DietFood(
    val name: String = "",
    val quantity: String = "",
    val calories: Int = 0,
    val proteinG: Int = 0,
    val notes: String = ""
)

data class DietMeal(
    val mealName: String = "",
    val timeSlot: String = "",
    val foods: List<DietFood> = emptyList()
) {
    val totalCalories: Int get() = foods.sumOf { it.calories }
    val totalProteinG: Int get() = foods.sumOf { it.proteinG }
}

data class DietDay(
    val dayLabel: String = "",
    val meals: List<DietMeal> = emptyList()
) {
    val totalCalories: Int get() = meals.sumOf { it.totalCalories }
    val totalProteinG: Int get() = meals.sumOf { it.totalProteinG }
}

data class DietPlan(
    val id: String = "",
    val coachUid: String = "",
    val clientId: String = "",
    val clientName: String = "",
    val title: String = "",
    val description: String = "",
    val status: String = "draft",   // "draft" | "active"
    val createdAt: Long = 0L,
    val sentAt: Long = 0L,
    val days: List<DietDay> = emptyList(),
    val notes: String = ""
) {
    val isActive: Boolean get() = status == "active"
    val totalDailyCalories: Int get() = days.firstOrNull()?.totalCalories ?: 0
    val totalDailyProteinG: Int get() = days.firstOrNull()?.totalProteinG ?: 0
}

data class DietMealFollowed(
    val mealName: String = "",
    val followed: Boolean = false
)

data class DietLog(
    val id: String = "",
    val planId: String = "",
    val coachUid: String = "",
    val clientId: String = "",
    val date: String = "",          // YYYY-MM-DD
    val adherencePercent: Int = 0,
    val memberNote: String = "",
    val mealsFollowed: List<DietMealFollowed> = emptyList(),
    val createdAt: Long = 0L,
    val isReadByCoach: Boolean = false
)

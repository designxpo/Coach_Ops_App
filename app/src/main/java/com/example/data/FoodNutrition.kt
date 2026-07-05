package com.example.data

/**
 * Nutrition result shared by all food-recognition sources:
 * ML Kit image scan, barcode lookup (Open Food Facts / USDA),
 * voice + local food database, and the food diary.
 */
data class FoodNutrition(
    val foodName:   String,
    val servingSize: String,
    val calories:   Int,
    val proteinG:   Float,
    val carbsG:     Float,
    val fatG:       Float,
    val fiberG:     Float,
    val confidence: String,   // "high" | "medium" | "low"
    val notes:      String
)

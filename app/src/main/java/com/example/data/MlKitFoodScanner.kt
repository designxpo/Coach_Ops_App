package com.example.data

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * On-device food image recognition via ML Kit Image Labeling.
 * No API key. No internet. Runs entirely on device using the bundled TFLite model.
 *
 * Flow: Bitmap → ML Kit labels → map to known food names → LocalFoodParser nutrition
 */
object MlKitFoodScanner {

    // ML Kit label → food name understood by LocalFoodParser
    // Sorted longest-first so more specific matches win
    private val LABEL_MAP: List<Pair<String, String>> = listOf(
        // Specific dishes first
        "scrambled eggs"    to "scrambled egg",
        "fried egg"         to "egg omelette",
        "boiled egg"        to "boiled egg",
        "hard boiled"       to "boiled egg",
        "chicken tikka"     to "chicken tikka",
        "chicken curry"     to "chicken curry",
        "fish curry"        to "fish curry",
        "fried rice"        to "fried rice",
        "french fries"      to "french fries",
        "ice cream"         to "ice cream",
        "green salad"       to "salad",
        "tomato soup"       to "tomato soup",
        "orange juice"      to "orange juice",
        "corn flakes"       to "corn flakes",
        "cottage cheese"    to "paneer raw",
        "greek yogurt"      to "greek yogurt",
        "peanut butter"     to "peanuts",
        "white rice"        to "rice",
        "brown rice"        to "brown rice",
        "watermelon"        to "watermelon",
        "pineapple"         to "pineapple",
        "strawberry"        to "strawberry",
        "pomegranate"       to "pomegranate",
        // General food categories
        "samosa"            to "samosa",
        "biryani"           to "biryani",
        "paratha"           to "paratha",
        "chapati"           to "roti",
        "naan"              to "naan",
        "dosa"              to "dosa",
        "idli"              to "idli",
        "upma"              to "upma",
        "poha"              to "poha",
        "omelette"          to "omelette",
        "oatmeal"           to "oats",
        "pancake"           to "dosa",
        "waffle"            to "bread",
        "burger"            to "burger",
        "hamburger"         to "burger",
        "sandwich"          to "sandwich",
        "hotdog"            to "sandwich",
        "pizza"             to "pizza",
        "pasta"             to "pasta",
        "noodle"            to "noodles",
        "sushi"             to "fish",
        "dumpling"          to "samosa",
        "spring roll"       to "frankie",
        "chicken"           to "chicken curry",
        "fish"              to "fish",
        "shrimp"            to "prawn curry",
        "salmon"            to "fish",
        "tuna"              to "fish",
        "egg"               to "egg",
        "bacon"             to "egg",
        "sausage"           to "egg",
        "steak"             to "chicken breast",
        "meat"              to "chicken curry",
        "curry"             to "dal",
        "soup"              to "soup",
        "salad"             to "salad",
        "bread"             to "bread",
        "toast"             to "bread",
        "rice"              to "rice",
        "dal"               to "dal",
        "lentil"            to "dal",
        "bean"              to "chana",
        "chickpea"          to "chana",
        "tofu"              to "tofu",
        "yogurt"            to "curd",
        "curd"              to "curd",
        "cheese"            to "paneer raw",
        "milk"              to "milk",
        "butter"            to "butter",
        "cream"             to "milk",
        "coffee"            to "coffee",
        "tea"               to "tea",
        "juice"             to "orange juice",
        "smoothie"          to "mango shake",
        "banana"            to "banana",
        "apple"             to "apple",
        "orange"            to "orange",
        "mango"             to "mango",
        "grape"             to "grapes",
        "guava"             to "guava",
        "papaya"            to "papaya",
        "lemon"             to "lemon",
        "almond"            to "almonds",
        "walnut"            to "walnuts",
        "cashew"            to "cashews",
        "peanut"            to "peanuts",
        "cake"              to "barfi",
        "cookie"            to "barfi",
        "chocolate"         to "ice cream",
        "dessert"           to "ice cream",
        "sweet"             to "gulab jamun",
    ).sortedByDescending { it.first.length }

    suspend fun analyze(bitmap: Bitmap): Result<FoodNutrition> =
        suspendCoroutine { cont ->
            val options = ImageLabelerOptions.Builder()
                .setConfidenceThreshold(0.45f)
                .build()

            val labeler = ImageLabeling.getClient(options)
            val image   = InputImage.fromBitmap(bitmap, 0)

            labeler.process(image)
                .addOnSuccessListener { labels ->
                    if (labels.isEmpty()) {
                        cont.resume(Result.failure(Exception(
                            "No objects detected. Try a clearer photo with the food well-lit and centred."
                        )))
                        return@addOnSuccessListener
                    }

                    // Try to match each detected label to a food entry, highest confidence first
                    val topLabels = labels.sortedByDescending { it.confidence }

                    var bestEntry: LocalFoodParser.FoodEntry? = null
                    var bestLabel = ""
                    var bestConf  = 0f

                    outer@ for (label in topLabels) {
                        val labelLower = label.text.lowercase()
                        for ((key, foodName) in LABEL_MAP) {
                            if (labelLower.contains(key)) {
                                val entry = LocalFoodParser.findFood(foodName)
                                if (entry != null) {
                                    bestEntry = entry
                                    bestLabel = label.text
                                    bestConf  = label.confidence
                                    break@outer
                                }
                            }
                        }
                    }

                    if (bestEntry == null) {
                        val topNames = topLabels.take(4).joinToString(", ") {
                            "${it.text} (${(it.confidence * 100).toInt()}%)"
                        }
                        cont.resume(Result.failure(Exception(
                            "Could not identify a known food. Detected: $topNames\n" +
                            "Try switching to Voice mode and say the food name."
                        )))
                        return@addOnSuccessListener
                    }

                    val entry   = bestEntry
                    val grams   = entry.servingGrams.toFloat()
                    val confStr = when {
                        bestConf >= 0.80f -> "high"
                        bestConf >= 0.60f -> "medium"
                        else              -> "low"
                    }

                    cont.resume(Result.success(
                        FoodNutrition(
                            foodName    = entry.names.first().replaceFirstChar { it.uppercase() },
                            servingSize = entry.servingLabel,
                            calories    = (entry.caloriesPer100g * grams / 100f).toInt(),
                            proteinG    = entry.proteinPer100g  * grams / 100f,
                            carbsG      = entry.carbsPer100g    * grams / 100f,
                            fatG        = entry.fatPer100g      * grams / 100f,
                            fiberG      = entry.fiberPer100g    * grams / 100f,
                            confidence  = confStr,
                            notes       = "Identified as \"$bestLabel\" via ML Kit · ${(bestConf * 100).toInt()}% confidence"
                        )
                    ))
                }
                .addOnFailureListener { e ->
                    cont.resume(Result.failure(
                        Exception("ML Kit error: ${e.message ?: "unknown"}")
                    ))
                }
        }
}

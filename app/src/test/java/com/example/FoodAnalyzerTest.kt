package com.example

import com.example.data.FoodAnalyzer
import com.example.data.LocalFoodParser
import org.junit.Assert.*
import org.junit.Test

/**
 * Locks in the two food-logging refinements:
 *  1. Quantity is applied ("3 roti" = 3× the nutrition, not 1).
 *  2. Multiple foods return one entry PER food, not a consolidated total.
 */
class FoodAnalyzerTest {

    @Test
    fun quantity_multiplies_single_food() {
        val items = LocalFoodParser.parseEach("3 roti")
        assertEquals(1, items.size)
        // roti = 265 kcal/100g, 40g serving → 120g → 318 kcal for 3
        assertEquals(318, items[0].calories)
        assertTrue("name should show the count", items[0].foodName.contains("3"))
    }

    @Test
    fun single_roti_is_one_serving() {
        val one = LocalFoodParser.parseEach("roti")
        assertEquals(1, one.size)
        assertEquals(106, one[0].calories)   // 265 * 40 / 100
    }

    @Test
    fun multiple_foods_return_separate_items() {
        val analysis = FoodAnalyzer.analyzeLocal("2 roti and dal and rice")
        assertEquals(3, analysis.items.size)
        val names = analysis.items.joinToString(" ") { it.foodName.lowercase() }
        assertTrue(names.contains("roti"))
        assertTrue(names.contains("dal"))
        assertTrue(names.contains("rice"))
        // each keeps its own macros — 2 roti is double a single roti
        val roti = analysis.items.first { it.foodName.lowercase().contains("roti") }
        assertEquals(212, roti.calories)     // 265 * 80 / 100
    }

    @Test
    fun grams_are_a_weight_not_a_count() {
        val (mult, name) = FoodAnalyzer.quantityPrefix("200g paneer")
        assertEquals(1f, mult, 0.001f)       // weight is never used as a multiplier
        assertEquals("paneer", name)
        // paneer = 210 kcal/100g → 200g → 420
        assertEquals(420, LocalFoodParser.parseEach("200g paneer")[0].calories)
    }

    @Test
    fun scoops_count_as_servings_for_supplements() {
        val (mult, name) = FoodAnalyzer.quantityPrefix("3 scoops on whey")
        assertEquals(3f, mult, 0.001f)
        assertEquals("on whey", name)
    }
}

package com.example

import com.example.data.ClientGoal
import com.example.data.ProgressAnalytics
import com.example.data.ProgressAnalytics.DayData
import org.junit.Assert.*
import org.junit.Test

class ProgressAnalyticsTest {

    private fun day(d: String, cal: Int, p: Float, steps: Int = 10000, water: Int = 8, sleep: Float = 8f) =
        DayData(d, cal, p, 0f, 0f, steps, 0, water, sleep)

    @Test
    fun deficit_with_protein_estimates_fat_loss_and_is_on_track() {
        val days = (1..7).map { day("2026-07-%02d".format(it), cal = 1800, p = 140f) }
        // bmr 1500 → dayTDEE = 1800 + steps(10000*0.045=450) = 2250 ; balance = −450/day
        val r = ProgressAnalytics.analyze(days, bmrKcal = 1500, tdeeKcal = 2000, weightKg = 70f, proteinTargetG = 112, goal = ClientGoal.LOSE_FAT)
        assertTrue(r.hasData)
        assertEquals(7, r.loggedDays)
        assertEquals(-450, r.avgBalance)
        assertTrue("fat should be lost", r.estFatKg < 0f)
        assertTrue("muscle largely preserved", r.estMuscleKg > r.estFatKg) // less negative
        assertTrue("on track", r.onTrackScore >= 75)
    }

    @Test
    fun surplus_with_protein_and_muscle_goal_estimates_muscle_gain() {
        val days = (1..7).map { day("2026-07-%02d".format(it), cal = 2600, p = 150f, steps = 8000) }
        val r = ProgressAnalytics.analyze(days, bmrKcal = 1500, tdeeKcal = 2000, weightKg = 70f, proteinTargetG = 112, goal = ClientGoal.BUILD_MUSCLE)
        assertTrue(r.avgBalance > 0)
        assertTrue("muscle should be gained", r.estMuscleKg > 0f)
    }

    @Test
    fun no_logged_days_returns_empty() {
        val days = (1..5).map { day("2026-07-%02d".format(it), cal = 0, p = 0f) }
        val r = ProgressAnalytics.analyze(days, bmrKcal = 1500, tdeeKcal = 2000, weightKg = 70f, proteinTargetG = 112, goal = ClientGoal.LOSE_FAT)
        assertFalse(r.hasData)
        assertEquals(0, r.loggedDays)
    }
}

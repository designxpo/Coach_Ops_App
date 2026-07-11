package com.example

import com.example.data.PortfolioScoring
import com.example.data.TrainerProfile
import com.example.data.isFeatured
import com.example.data.meritScore
import com.example.data.tierRank
import org.junit.Assert.*
import org.junit.Test

class PortfolioScoringTest {

    private fun fullProfile() = TrainerProfile(
        name = "Rahul Sharma",
        specialty = "Weight Loss, Strength",
        bio = "I help busy professionals lose fat and build sustainable strength habits without crash diets.",
        workDescription = "Every client starts with a posture and mobility assessment. Weekly check-ins, progressive overload plans, and habit tracking through the app.",
        city = "mumbai",
        feePerSession = 800,
        feeMonthly = 8000,
        availabilityDays = "MON,WED,FRI,SAT",
        yearsExperience = 6,
        profileImageUrl = "https://example.com/p.jpg",
        portfolioImages = "https://example.com/1.jpg,https://example.com/2.jpg,https://example.com/3.jpg",
        headline = "Fat-loss coach for busy professionals",
        languages = "Hindi, English",
        education = "Graduate",
        certifications = "K11 Certified PT (2021)",
        gymsWorked = "Gold's Gym Andheri",
        clientsCoached = 120,
        clientTypes = "Beginners, Women",
        trainingModes = "Gym, Home, Online",
        assessmentIncluded = true,
        cprCertified = true,
        nutritionSupport = "Diet guidance",
        instagramUrl = "https://instagram.com/rahul.fit",
        ratingSum = 14.4f,   // three real member reviews averaging 4.8★
        ratingCount = 3
    )

    @Test
    fun empty_profile_scores_zero() {
        assertEquals(0, PortfolioScoring.score(TrainerProfile()))
    }

    @Test
    fun complete_profile_scores_exactly_100() {
        assertEquals(100, PortfolioScoring.score(fullProfile()))
    }

    @Test
    fun sections_sum_to_score_and_max_100() {
        val sections = PortfolioScoring.sections(fullProfile())
        assertEquals(100, sections.sumOf { it.max })
        assertEquals(PortfolioScoring.score(fullProfile()), sections.sumOf { it.earned })
    }

    @Test
    fun mentorship_earns_half_of_certification_and_not_both() {
        val certified = fullProfile()
        val selfDeclared = certified.copy(certifications = "", mentorship = "Trained under Coach Vikram at Gold's Gym")
        assertEquals(PortfolioScoring.score(certified) - 4, PortfolioScoring.score(selfDeclared))
        // Having both counts only the certification
        val both = certified.copy(mentorship = "Coach Vikram")
        assertEquals(PortfolioScoring.score(certified), PortfolioScoring.score(both))
    }

    @Test
    fun short_bio_and_short_work_description_earn_nothing() {
        val p = fullProfile().copy(bio = "Hi!", workDescription = "I train people.")
        assertEquals(100 - 5 - 6, PortfolioScoring.score(p))
    }

    @Test
    fun tier_labels_map_to_thresholds() {
        assertEquals("Elite Profile", PortfolioScoring.tierLabel(85))
        assertEquals("Strong Profile", PortfolioScoring.tierLabel(70))
        assertEquals("Rising Coach", PortfolioScoring.tierLabel(50))
        assertEquals("Incomplete", PortfolioScoring.tierLabel(49))
    }

    @Test
    fun next_actions_ranked_by_points_and_disappear_when_done() {
        val empty = TrainerProfile()
        val actions = PortfolioScoring.nextActions(empty, limit = 3)
        assertEquals(3, actions.size)
        // Highest-value item first, list is non-increasing
        assertTrue(actions.zipWithNext().all { (a, b) -> a.points >= b.points })
        assertEquals(8, actions.first().points)
        // A complete profile has nothing left to suggest
        assertTrue(PortfolioScoring.nextActions(fullProfile()).isEmpty())
    }

    @Test
    fun featured_flag_follows_plan_tier() {
        assertTrue(TrainerProfile(planTier = "pro").isFeatured)
        assertTrue(TrainerProfile(planTier = "business").isFeatured)
        assertFalse(TrainerProfile(planTier = "starter").isFeatured)
        assertFalse(TrainerProfile().isFeatured)
    }

    @Test
    fun partial_photos_and_reviews_earn_two_points_each() {
        val base = fullProfile()
        val onePhoto = base.copy(portfolioImages = "https://example.com/1.jpg")
        assertEquals(PortfolioScoring.score(base) - 4, PortfolioScoring.score(onePhoto))
        val oneReview = base.copy(ratingSum = 5f, ratingCount = 1)
        assertEquals(PortfolioScoring.score(base) - 4, PortfolioScoring.score(oneReview))
    }

    @Test
    fun coach_typed_testimonials_earn_nothing() {
        val noReviews = fullProfile().copy(ratingSum = 0f, ratingCount = 0)
        val withFakeQuotes = noReviews.copy(testimonials = "I am great!\nBest coach!\nAmazing!")
        assertEquals(PortfolioScoring.score(noReviews), PortfolioScoring.score(withFakeQuotes))
    }

    // ── League + merit ranking ────────────────────────────────────────────────

    @Test
    fun leagues_order_business_pro_free() {
        assertEquals(2, TrainerProfile(planTier = "business").tierRank)
        assertEquals(1, TrainerProfile(planTier = "pro").tierRank)
        assertEquals(0, TrainerProfile(planTier = "starter").tierRank)
        assertEquals(0, TrainerProfile().tierRank)
    }

    @Test
    fun within_league_consistent_quality_beats_one_lucky_review() {
        // ten 4.8★ reviews must outrank a single 5★ (bayesian smoothing)
        val consistent = TrainerProfile(ratingSum = 48f, ratingCount = 10)
        val lucky      = TrainerProfile(ratingSum = 5f,  ratingCount = 1)
        assertTrue(consistent.meritScore > lucky.meritScore)
    }

    @Test
    fun rated_free_coach_outranks_unrated_free_coach_with_full_profile() {
        val ratedCoach    = TrainerProfile(ratingSum = 24f, ratingCount = 5, profileScore = 40)
        val polishedCoach = TrainerProfile(profileScore = 100)
        assertTrue(ratedCoach.meritScore > polishedCoach.meritScore)
    }
}

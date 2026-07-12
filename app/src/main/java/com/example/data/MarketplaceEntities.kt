package com.example.data

data class TrainerProfile(
    val uid: String = "",
    val name: String = "",
    val specialty: String = "",
    val bio: String = "",
    val workDescription: String = "",  // detailed description of services / training style
    val city: String = "",
    val feePerSession: Int = 0,
    val feeMonthly: Int = 0,
    val availabilityDays: String = "", // "MON,WED,FRI"
    val yearsExperience: Int = 0,
    val rating: Float = 0f,            // aggregate from client reviews
    val profileImageUrl: String = "",  // Firebase Storage URL
    val portfolioImages: String = "",  // comma-separated Storage URLs (up to 3)
    val lat: Double = 0.0,             // geocoded from city — used for radius filtering
    val lng: Double = 0.0,
    val updatedAtMillis: Long = 0L,

    // ── Structured portfolio (trust profile shown to members) ──────────────
    val headline: String = "",         // one-line pitch, max 90 chars
    val languages: String = "",        // "Hindi, English"
    val education: String = "",        // "Graduate", "B.P.Ed / M.P.Ed", …
    val certifications: String = "",   // "K11 Certified PT (2021), ACE CPT"
    val mentorship: String = "",       // no-cert path: trained under / gym internship
    val gymsWorked: String = "",       // "Gold's Gym Andheri, Cult Fit"
    val clientsCoached: Int = 0,       // self-declared count
    val clientTypes: String = "",      // "Beginners, Women, Seniors"
    val trainingModes: String = "",    // "Gym, Home, Online, Outdoor"
    val assessmentIncluded: Boolean = false, // fitness assessment before first plan
    val cprCertified: Boolean = false,
    val nutritionSupport: String = "", // "Workout only" | "Diet guidance" | "Meal planning"
    val testimonials: String = "",     // LEGACY self-quotes — replaced by real member reviews, no longer editable or shown
    val instagramUrl: String = "",
    val profileScore: Int = 0,         // PortfolioScoring result at publish time — ranks Discover
    val planTier: String = "",         // coach's subscription at publish time: starter/pro/business
    // Certification review — set when the coach uploads a certificate document.
    // "" none · "pending" awaiting manual review · "verified_auto" OCR matched
    // name+issuer · "verified" admin approved · "rejected" admin rejected
    val certDocUrl: String = "",
    val certStatus: String = "",
    // Incremental rating aggregate — written only by member rating submissions
    val ratingSum: Float = 0f,
    val ratingCount: Int = 0
)

/** True when the certificate passed auto-OCR or manual admin review. */
val TrainerProfile.isCertVerified: Boolean
    get() = certStatus == "verified" || certStatus == "verified_auto"

/**
 * Paying coaches are surfaced first in Discover with a Featured badge.
 * Which tiers earn it is admin-controlled via the feature matrix (Pro+ by
 * default); the plan→feature mapping falls back to the historical behaviour.
 */
val TrainerProfile.isFeatured: Boolean
    get() = FeatureGate.coachUnlocked(
        EntitlementManager.currentMatrix,
        GatedFeature.FEATURED_MARKETPLACE,
        FeatureGate.planFromTierString(planTier)
    )

// ─── Discover ranking: leagues + merit ───────────────────────────────────────
// Coaches compete WITHIN their subscription level: Business league above Pro,
// Pro above Free. Inside each league the order is EARNED — member ratings and
// review volume first, profile completeness as tiebreak. A free coach rises by
// being good; subscribing promotes them into the next league where the same
// merit rules apply against similarly subscribed coaches.

/** Subscription league: business=2, pro=1, free/unknown=0. */
val TrainerProfile.tierRank: Int
    get() = when (planTier) { "business" -> 2; "pro" -> 1; else -> 0 }

/**
 * Merit inside a league. Bayesian-smoothed rating (prior: two 3.5★ reviews)
 * so a single lucky 5★ can't outrank ten consistent 4.8★s; review volume and
 * profile strength add on top.
 */
val TrainerProfile.meritScore: Float
    get() {
        val smoothedRating = (ratingSum + 3.5f * 2) / (ratingCount + 2)   // 0–5
        return smoothedRating * 20f +          // up to ~100 — earned quality
            minOf(ratingCount, 20) * 2f +      // up to 40   — earned volume
            profileScore * 0.4f                // up to 40   — completeness never outweighs real ratings
    }

data class Booking(
    val id: String = "",
    val coachId: String = "",
    val coachName: String = "",
    val clientId: String = "",
    val clientName: String = "",
    val requestedDateMillis: Long = 0L,
    val status: String = "PENDING",    // PENDING, CONFIRMED, DECLINED, COMPLETED
    val feeAmount: Int = 0,
    val notes: String = "",
    val coachResponse: String = "",
    val createdAtMillis: Long = 0L,
    val sessionDateMillis: Long = 0L,   // preferred session date chosen by client
    val clientRating: Float = 0f,       // 1–5 stars, set by client after completion
    val coachRating: Float = 0f         // 1–5 stars, coach rates the member back
)

// ─── Portfolio completeness scoring ──────────────────────────────────────────
// Drives the Discover ranking and the coach-side "profile strength" meter.
// Weights follow what members actually use to pick a coach: proof of identity
// and results first, credentials and experience next, logistics last.

data class ScoreSection(val name: String, val earned: Int, val max: Int)

data class ScoreAction(val label: String, val points: Int)

object PortfolioScoring {

    const val TIER_ELITE = 85
    const val TIER_STRONG = 70
    const val TIER_RISING = 50

    fun tierLabel(score: Int): String = when {
        score >= TIER_ELITE  -> "Elite Profile"
        score >= TIER_STRONG -> "Strong Profile"
        score >= TIER_RISING -> "Rising Coach"
        else                 -> "Incomplete"
    }

    fun sections(t: TrainerProfile): List<ScoreSection> = listOf(
        ScoreSection("Basics", basicsEarned(t), 20),
        ScoreSection("Credentials", credentialsEarned(t), 20),
        ScoreSection("Experience", experienceEarned(t), 20),
        ScoreSection("Coaching Approach", approachEarned(t), 15),
        ScoreSection("Proof & Results", proofEarned(t), 15),
        ScoreSection("Pricing & Availability", servicesEarned(t), 10)
    )

    fun score(t: TrainerProfile): Int = sections(t).sumOf { it.earned }

    private fun basicsEarned(t: TrainerProfile): Int {
        var s = 0
        if (t.profileImageUrl.isNotBlank()) s += 8
        if (t.headline.isNotBlank()) s += 5
        if (t.bio.trim().length >= 60) s += 5
        if (t.city.isNotBlank()) s += 2
        return s
    }

    private fun credentialsEarned(t: TrainerProfile): Int {
        var s = 0
        if (t.education.isNotBlank()) s += 5
        if (t.certifications.isNotBlank()) s += 8
        else if (t.mentorship.isNotBlank()) s += 4  // honest no-cert path earns half
        if (t.cprCertified) s += 4
        if (t.languages.isNotBlank()) s += 3
        return s
    }

    private fun experienceEarned(t: TrainerProfile): Int {
        var s = 0
        if (t.yearsExperience > 0) s += 6
        if (t.gymsWorked.isNotBlank()) s += 4
        if (t.clientsCoached > 0) s += 4
        if (t.trainingModes.isNotBlank()) s += 3
        if (t.clientTypes.isNotBlank()) s += 3
        return s
    }

    private fun approachEarned(t: TrainerProfile): Int {
        var s = 0
        if (t.workDescription.trim().length >= 80) s += 6
        if (t.assessmentIncluded) s += 5
        if (t.nutritionSupport.isNotBlank()) s += 4
        return s
    }

    private fun proofEarned(t: TrainerProfile): Int {
        var s = 0
        val photos = t.portfolioImages.split(",").count { it.isNotBlank() }
        s += minOf(photos, 3) * 2
        // Reviews are EARNED from real member ratings — coaches can't type these
        s += minOf(t.ratingCount, 3) * 2
        if (t.instagramUrl.isNotBlank()) s += 3
        return s
    }

    private fun servicesEarned(t: TrainerProfile): Int {
        var s = 0
        if (t.feePerSession > 0) s += 4
        if (t.feeMonthly > 0) s += 3
        if (t.availabilityDays.split(",").count { it.isNotBlank() } >= 3) s += 3
        return s
    }

    /** Highest-value missing items — the coach's "do this next" checklist. */
    fun nextActions(t: TrainerProfile, limit: Int = 4): List<ScoreAction> {
        val missing = mutableListOf<ScoreAction>()
        if (t.profileImageUrl.isBlank()) missing += ScoreAction("Add a profile photo", 8)
        if (t.certifications.isBlank()) {
            if (t.mentorship.isBlank())
                missing += ScoreAction("Add certifications or training background", 8)
            else
                missing += ScoreAction("Add a certification", 4)
        }
        if (t.workDescription.trim().length < 80) missing += ScoreAction("Describe your coaching approach (80+ chars)", 6)
        if (t.yearsExperience <= 0) missing += ScoreAction("Add years of experience", 6)
        val photos = t.portfolioImages.split(",").count { it.isNotBlank() }
        if (photos < 3) missing += ScoreAction("Add work photos (${3 - photos} more)", (3 - photos) * 2)
        if (t.ratingCount < 3) missing += ScoreAction(
            "Earn member reviews — complete sessions & ask clients to rate you (${3 - t.ratingCount} more)",
            (3 - t.ratingCount) * 2
        )
        if (t.headline.isBlank()) missing += ScoreAction("Write a one-line headline", 5)
        if (t.bio.trim().length < 60) missing += ScoreAction("Write a longer bio (60+ chars)", 5)
        if (t.education.isBlank()) missing += ScoreAction("Add your education", 5)
        if (!t.assessmentIncluded) missing += ScoreAction("Offer a fitness assessment before training", 5)
        if (t.gymsWorked.isBlank()) missing += ScoreAction("List gyms you've worked at", 4)
        if (t.clientsCoached <= 0) missing += ScoreAction("Add how many clients you've coached", 4)
        if (!t.cprCertified) missing += ScoreAction("Get CPR / First-aid certified", 4)
        if (t.nutritionSupport.isBlank()) missing += ScoreAction("Specify nutrition support level", 4)
        if (t.feePerSession <= 0) missing += ScoreAction("Set your per-session fee", 4)
        if (t.instagramUrl.isBlank()) missing += ScoreAction("Link your Instagram", 3)
        if (t.languages.isBlank()) missing += ScoreAction("Add languages you speak", 3)
        if (t.trainingModes.isBlank()) missing += ScoreAction("Select training modes", 3)
        if (t.clientTypes.isBlank()) missing += ScoreAction("Select client types you train", 3)
        if (t.feeMonthly <= 0) missing += ScoreAction("Set a monthly package price", 3)
        if (t.availabilityDays.split(",").count { it.isNotBlank() } < 3) missing += ScoreAction("Mark 3+ available days", 3)
        return missing.sortedByDescending { it.points }.take(limit)
    }
}

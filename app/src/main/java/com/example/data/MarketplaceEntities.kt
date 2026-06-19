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
    val updatedAtMillis: Long = 0L
)

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
    val clientRating: Float = 0f        // 1–5 stars, set by client after completion
)

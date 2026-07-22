package com.example.data

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

object FirestoreSync {

    private val auth    = FirebaseAuth.getInstance()
    private val db      = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private val uid get() = auth.currentUser?.uid
    private fun col(name: String) = uid?.let {
        db.collection("coaches").document(it).collection(name)
    }

    // ─── Clients ──────────────────────────────────────────────────────────────

    fun saveClient(c: Client) {
        col("clients")?.document(c.id)?.set(mapOf(
            "id" to c.id, "name" to c.name, "phoneNumber" to c.phoneNumber,
            "initialGoal" to c.initialGoal, "programWeek" to c.programWeek,
            "consistencyScore" to c.consistencyScore, "programId" to (c.programId ?: ""),
            "lastCheckInMillis" to c.lastCheckInMillis, "mrr" to c.mrr,
            "status" to c.status, "enrollmentDateMillis" to c.enrollmentDateMillis,
            "city" to c.city, "paymentCycle" to c.paymentCycle,
            "pausedAtMillis" to c.pausedAtMillis
        ))
    }

    fun deleteClient(clientId: String) {
        col("clients")?.document(clientId)?.delete()
    }

    // ─── Programs ─────────────────────────────────────────────────────────────

    fun saveProgram(p: Program) {
        col("programs")?.document(p.id)?.set(mapOf(
            "id" to p.id, "name" to p.name, "durationWeeks" to p.durationWeeks,
            "daysPerWeek" to p.daysPerWeek, "tags" to p.tags,
            "lastUsedMillis" to p.lastUsedMillis, "clientCount" to p.clientCount,
            "isMasterTemplate" to p.isMasterTemplate, "workingDays" to p.workingDays
        ))
    }

    // ─── Payments ─────────────────────────────────────────────────────────────

    fun savePayment(p: Payment) {
        col("payments")?.document(p.id)?.set(mapOf(
            "id" to p.id, "clientId" to p.clientId, "amount" to p.amount,
            "dueDateMillis" to p.dueDateMillis, "daysOverdue" to p.daysOverdue,
            "mandateStatus" to p.mandateStatus
        ))
    }

    fun deletePaymentsByClient(clientId: String) {
        col("payments")?.whereEqualTo("clientId", clientId)?.get()
            ?.addOnSuccessListener { snap -> snap.documents.forEach { it.reference.delete() } }
    }

    // ─── Workout Logs ─────────────────────────────────────────────────────────

    fun saveWorkoutLog(l: WorkoutLog) {
        col("workout_logs")?.document(l.id)?.set(mapOf(
            "id" to l.id, "clientId" to l.clientId, "programId" to (l.programId ?: ""),
            "sessionDateMillis" to l.sessionDateMillis, "sessionName" to l.sessionName,
            "exercises" to l.exercises, "durationMins" to l.durationMins,
            "notes" to l.notes, "isMissed" to l.isMissed, "missedReason" to l.missedReason
        ))
    }

    fun deleteWorkoutLogsByClient(clientId: String) {
        col("workout_logs")?.whereEqualTo("clientId", clientId)?.get()
            ?.addOnSuccessListener { snap -> snap.documents.forEach { it.reference.delete() } }
    }

    // ─── Measurements ─────────────────────────────────────────────────────────

    fun saveMeasurement(m: BodyMeasurement) {
        col("measurements")?.document(m.id)?.set(mapOf(
            "id" to m.id, "clientId" to m.clientId, "dateMillis" to m.dateMillis,
            "weightKg" to m.weightKg.toDouble(), "bodyFatPct" to m.bodyFatPct.toDouble(),
            "notes" to m.notes
        ))
    }

    fun deleteMeasurementsByClient(clientId: String) {
        col("measurements")?.whereEqualTo("clientId", clientId)?.get()
            ?.addOnSuccessListener { snap -> snap.documents.forEach { it.reference.delete() } }
    }

    // ─── Notes ────────────────────────────────────────────────────────────────

    fun saveNote(n: ClientNote) {
        col("notes")?.document(n.id)?.set(mapOf(
            "id" to n.id, "clientId" to n.clientId,
            "dateMillis" to n.dateMillis, "content" to n.content
        ))
    }

    fun deleteNote(noteId: String) {
        col("notes")?.document(noteId)?.delete()
    }

    fun deleteNotesByClient(clientId: String) {
        col("notes")?.whereEqualTo("clientId", clientId)?.get()
            ?.addOnSuccessListener { snap -> snap.documents.forEach { it.reference.delete() } }
    }

    // ─── Revenue Snapshots ────────────────────────────────────────────────────

    fun saveSnapshot(s: RevenueSnapshot) {
        col("revenue_snapshots")?.document(s.monthYear)?.set(mapOf(
            "monthYear" to s.monthYear, "totalMrr" to s.totalMrr
        ))
    }

    // ─── Profile → user_records (read by admin dashboard) ────────────────────

    fun updateProfileRecord(displayName: String) {
        val uid = uid ?: return
        db.collection("user_records").document(uid)
            .set(mapOf(
                "displayName"  to displayName,
                // NOTE: never write 'role' here — this can be called for any role;
                // role is owned by setUserRole / registerClientRecord / ProfileSync.
                "lastActiveAt" to System.currentTimeMillis()
            ), com.google.firebase.firestore.SetOptions.merge())
    }

    /**
     * Reports the running app version on every launch so the admin panel can
     * track update adoption. versionHistory accumulates every version this
     * user has ever run (arrayUnion — no duplicates), so old-version records
     * are never lost when an update ships.
     */
    fun saveAppVersion() {
        val u = uid ?: return
        db.collection("user_records").document(u)
            .set(mapOf(
                "appVersion"          to com.example.BuildConfig.VERSION_NAME,
                "appVersionCode"      to com.example.BuildConfig.VERSION_CODE,
                "appVersionUpdatedAt" to System.currentTimeMillis(),
                "versionHistory"      to com.google.firebase.firestore.FieldValue.arrayUnion(
                    com.example.BuildConfig.VERSION_NAME),
                "lastActiveAt"        to System.currentTimeMillis()
            ), com.google.firebase.firestore.SetOptions.merge())
    }

    /** Persists FCM token so the admin panel can send push notifications to this device.
     *  Never sets 'role' — role is owned by registerClientRecord / updateProfileRecord. */
    fun saveFcmToken(token: String) {
        val u = uid ?: return
        db.collection("user_records").document(u)
            .set(mapOf(
                "fcmToken"       to token,
                "tokenUpdatedAt" to System.currentTimeMillis()
            ), com.google.firebase.firestore.SetOptions.merge())
    }

    /**
     * MUST run before signOut(): removes this device's push token from the
     * account and invalidates it. Otherwise pushes targeted at the old account
     * keep landing on this device after another user logs in.
     */
    suspend fun clearFcmToken() {
        val u = uid ?: return
        try {
            db.collection("user_records").document(u)
                .set(mapOf("fcmToken" to com.google.firebase.firestore.FieldValue.delete()),
                    com.google.firebase.firestore.SetOptions.merge())
                .await()
            com.google.firebase.messaging.FirebaseMessaging.getInstance().deleteToken().await()
        } catch (_: Exception) { /* offline logout still proceeds */ }
    }

    /** Persists subscription plan so admin panel can target Pro/Business users. */
    fun saveSubscriptionPlan(plan: String) {
        val u = uid ?: return
        db.collection("user_records").document(u)
            .set(mapOf("subscriptionPlan" to plan),
                 com.google.firebase.firestore.SetOptions.merge())
    }

    /**
     * Fetches the role ("coach" / "client") stored in user_records for this UID.
     * Returns null if the document doesn't exist yet (brand-new user).
     */
    suspend fun getUserRole(uid: String): String? =
        try {
            db.collection("user_records").document(uid).get().await()
                .data?.get("role") as? String
        } catch (_: Exception) { null }

    /** Writes the role into user_records immediately after account creation.
     *  Always uses the authenticated UID — the uid parameter is validated against auth. */
    suspend fun setUserRole(role: String) {
        val u = uid ?: return
        try {
            db.collection("user_records").document(u)
                .set(mapOf("role" to role), com.google.firebase.firestore.SetOptions.merge()).await()
        } catch (_: Exception) {}
    }

    /** Returns true if a coaches/{uid} document exists — used to detect legacy coach accounts
     *  that were created before the role field was written at registration time. */
    suspend fun hasCoachData(uid: String): Boolean = try {
        db.collection("coaches").document(uid).get().await().exists()
    } catch (_: Exception) { false }

    /**
     * Called on every login on any device.
     * If onboardingComplete is already true locally, this is a no-op.
     * Otherwise checks Firestore to see if this user has previously completed
     * onboarding on another device, and restores their state + key profile fields.
     */
    suspend fun restoreOnboardingIfNeeded(uid: String, userPreferences: UserPreferences) {
        if (userPreferences.onboardingComplete) return
        try {
            val userDoc = db.collection("user_records").document(uid).get().await()
            if (userDoc.exists()) {
                val role = userDoc.getString("role") ?: ""
                // Doc existence is NOT proof of completed onboarding: AuthRepository.writeUserRecord
                // auto-creates a role-less doc at the very first sign-in. Trust the explicit
                // onboardingComplete flag when present; otherwise fall back to role presence
                // (legacy accounts predate the flag but always wrote role at onboarding).
                val complete = userDoc.getBoolean("onboardingComplete") ?: role.isNotEmpty()
                if (complete) userPreferences.onboardingComplete = true
                val name = userDoc.getString("displayName") ?: ""
                val email = userDoc.getString("email") ?: ""
                if (role == "coach" || role == "gym_owner") {
                    if (name.isNotEmpty() && userPreferences.coachName.isEmpty()) userPreferences.coachName = name
                    if (email.isNotEmpty() && userPreferences.coachEmail.isEmpty()) userPreferences.coachEmail = email
                } else if (role == "client") {
                    if (name.isNotEmpty() && userPreferences.clientName.isEmpty()) userPreferences.clientName = name
                }
                return
            }
            // Fallback for legacy coach accounts that predate user_records writes
            val coachDoc = db.collection("coaches").document(uid).get().await()
            if (coachDoc.exists()) {
                userPreferences.onboardingComplete = true
                val name = coachDoc.getString("name") ?: ""
                if (name.isNotEmpty() && userPreferences.coachName.isEmpty()) userPreferences.coachName = name
            }
        } catch (_: Exception) {
            // Offline or network error — leave onboardingComplete as-is
        }
    }

    /** Called at member registration — creates their admin record with the correct
     *  role immediately. onboardingComplete=false until ClientOnboarding finishes
     *  (ProfileSync.saveMemberProfile flips it), so a mid-onboarding quit resumes. */
    fun registerClientRecord(name: String, email: String) {
        val uid = uid ?: return
        db.collection("user_records").document(uid)
            .set(mapOf(
                "uid"                to uid,
                "displayName"        to name,
                "email"              to email,
                "role"               to "client",
                "onboardingComplete" to false,
                "joinedAt"           to System.currentTimeMillis(),
                "lastActiveAt"       to System.currentTimeMillis()
            ), com.google.firebase.firestore.SetOptions.merge())
    }

    /**
     * Phone → account directory (one doc per member, keyed by 10-digit phone).
     * Lets a gym owner/coach adding someone by phone discover they're already
     * on ProCoach — so the person is linked to ONE identity instead of
     * duplicate unlinked records.
     */
    fun savePhoneIndex(rawPhone: String, displayName: String) {
        val u = uid ?: return
        val phone = rawPhone.filter { it.isDigit() }.takeLast(10)
        if (phone.length != 10) return
        db.collection("user_phone_index").document(phone)
            .set(mapOf(
                "uid"       to u,
                "name"      to displayName,
                "updatedAt" to System.currentTimeMillis()
            ))
    }

    // ─── Aggregate stats → user_records (read by admin dashboard) ────────────

    fun updateAggregates(clientCount: Int, totalMrr: Int, sessionCount: Int) {
        val uid = uid ?: return
        db.collection("user_records").document(uid)
            .set(mapOf(
                "clientCount"  to clientCount,
                "totalMrr"     to totalMrr,
                "sessionCount" to sessionCount
            ), com.google.firebase.firestore.SetOptions.merge())
    }

    // ─── Marketplace: Trainer Profiles ───────────────────────────────────────

    // ─── Image upload helpers ─────────────────────────────────────────────────

    /** Trainer marketplace hero photo — stored in Supabase Storage */
    suspend fun uploadProfileImage(context: android.content.Context, uri: Uri): String =
        SupabaseStorage.uploadTrainerPhoto(context, uri)

    /** Coach portfolio work image — stored in Supabase Storage */
    suspend fun uploadPortfolioImage(context: android.content.Context, uri: Uri, index: Int): String =
        SupabaseStorage.uploadPortfolioPhoto(context, uri, index)

    // ─── Publish / unpublish ─────────────────────────────────────────────────

    suspend fun publishTrainerProfile(profile: TrainerProfile, isPublic: Boolean = true) {
        val uid = uid ?: throw Exception("Not authenticated")
        if (com.example.BuildConfig.DEBUG) android.util.Log.d("CoachOps", "publishTrainerProfile uid=$uid name=${profile.name} city=${profile.city} lat=${profile.lat} lng=${profile.lng}")
        // rating is NOT written here — maintained exclusively by rateBooking()
        db.collection("trainers").document(uid).set(mapOf(
            "uid"              to uid,
            "name"             to profile.name,
            "specialty"        to profile.specialty,
            "bio"              to profile.bio,
            "workDescription"  to profile.workDescription,
            "city"             to profile.city.lowercase().trim(),
            "feePerSession"    to profile.feePerSession,
            "feeMonthly"       to profile.feeMonthly,
            "availabilityDays" to profile.availabilityDays,
            "yearsExperience"  to profile.yearsExperience,
            "profileImageUrl"  to profile.profileImageUrl,
            "portfolioImages"  to profile.portfolioImages,
            "lat"              to profile.lat,
            "lng"              to profile.lng,
            "isPublic"         to isPublic,
            "updatedAtMillis"  to System.currentTimeMillis(),
            // Structured portfolio
            "headline"           to profile.headline,
            "languages"          to profile.languages,
            "education"          to profile.education,
            "certifications"     to profile.certifications,
            "mentorship"         to profile.mentorship,
            "gymsWorked"         to profile.gymsWorked,
            "clientsCoached"     to profile.clientsCoached,
            "clientTypes"        to profile.clientTypes,
            "trainingModes"      to profile.trainingModes,
            "assessmentIncluded" to profile.assessmentIncluded,
            "cprCertified"       to profile.cprCertified,
            "nutritionSupport"   to profile.nutritionSupport,
            "testimonials"       to profile.testimonials,
            "instagramUrl"       to profile.instagramUrl,
            "profileScore"       to profile.profileScore,
            "planTier"           to profile.planTier,
            "certDocUrl"         to profile.certDocUrl,
            "certStatus"         to profile.certStatus
        ), com.google.firebase.firestore.SetOptions.merge()).await()

        // Queue for the admin review dashboard when a document awaits a decision.
        // Best-effort: a rules hiccup here must never fail the profile publish.
        if (profile.certDocUrl.isNotBlank() &&
            (profile.certStatus == "pending" || profile.certStatus == "verified_auto")) {
            try {
                db.collection("cert_reviews").document(uid).set(mapOf(
                    "uid"            to uid,
                    "name"           to profile.name,
                    "certifications" to profile.certifications,
                    "certDocUrl"     to profile.certDocUrl,
                    "status"         to profile.certStatus,
                    "submittedAt"    to System.currentTimeMillis()
                ), com.google.firebase.firestore.SetOptions.merge()).await()
            } catch (_: Exception) { /* queued next publish */ }
        }
        if (com.example.BuildConfig.DEBUG) android.util.Log.d("CoachOps", "publishTrainerProfile DONE — wrote to trainers/$uid")
    }

    /** Re-stamp the marketplace league when the coach's plan changes, so an
     *  upgrade takes effect in Discover ranking without re-publishing. */
    suspend fun updateTrainerPlanTier(tier: String) {
        val uid = uid ?: return
        try {
            if (db.collection("trainers").document(uid).get().await().exists()) {
                db.collection("trainers").document(uid)
                    .set(mapOf("planTier" to tier), com.google.firebase.firestore.SetOptions.merge()).await()
            }
        } catch (_: Exception) { }
    }

    suspend fun getPublicTrainers(
        clientLat: Double = 0.0,
        clientLng: Double = 0.0,
        radiusKm: Int = 0        // 0 = no radius filter (show all)
    ): List<TrainerProfile> {
        val currentUid = auth.currentUser?.uid
        if (com.example.BuildConfig.DEBUG) android.util.Log.d("CoachOps", "getPublicTrainers START — uid=$currentUid lat=$clientLat lng=$clientLng radius=$radiusKm")

        val applyRadius = radiusKm > 0 && clientLat != 0.0 && clientLng != 0.0

        val docs = db.collection("trainers")
            .whereEqualTo("isPublic", true)
            .get().await().documents
        if (com.example.BuildConfig.DEBUG) android.util.Log.d("CoachOps", "isPublic=true docs: ${docs.size}")

        return docs.mapNotNull { doc ->
            val d = doc.data ?: return@mapNotNull null
            val trainerLat = (d["lat"] as? Double) ?: 0.0
            val trainerLng = (d["lng"] as? Double) ?: 0.0
            // Only apply distance filter when the trainer has geocoded coordinates.
            // Trainers without coordinates (published before location feature, or geocoding failed)
            // always pass through so they remain visible.
            if (applyRadius && (trainerLat != 0.0 || trainerLng != 0.0)) {
                val dist = GeoUtils.distanceKm(clientLat, clientLng, trainerLat, trainerLng)
                if (com.example.BuildConfig.DEBUG) android.util.Log.d("CoachOps", "  trainer ${d["name"]} dist=${dist}km — ${if (dist > radiusKm) "FILTERED OUT" else "included"}")
                if (dist > radiusKm) return@mapNotNull null
            }
            parseTrainer(doc.id, d)
        }
    }

    suspend fun getTrainerById(trainerUid: String): TrainerProfile? {
        val d = db.collection("trainers").document(trainerUid).get().await().data ?: return null
        return parseTrainer(trainerUid, d)
    }

    private fun parseTrainer(docId: String, d: Map<String, Any>): TrainerProfile = TrainerProfile(
        uid              = d["uid"] as? String ?: docId,
        name             = d["name"] as? String ?: "",
        specialty        = d["specialty"] as? String ?: "",
        bio              = d["bio"] as? String ?: "",
        workDescription  = d["workDescription"] as? String ?: "",
        city             = d["city"] as? String ?: "",
        feePerSession    = (d["feePerSession"] as? Long)?.toInt() ?: 0,
        feeMonthly       = (d["feeMonthly"] as? Long)?.toInt() ?: 0,
        availabilityDays = d["availabilityDays"] as? String ?: "",
        yearsExperience  = (d["yearsExperience"] as? Long)?.toInt() ?: 0,
        rating           = (d["rating"] as? Double)?.toFloat() ?: (d["rating"] as? Long)?.toFloat() ?: 0f,
        profileImageUrl  = d["profileImageUrl"] as? String ?: "",
        portfolioImages  = d["portfolioImages"] as? String ?: "",
        lat              = (d["lat"] as? Double) ?: 0.0,
        lng              = (d["lng"] as? Double) ?: 0.0,
        updatedAtMillis  = d["updatedAtMillis"] as? Long ?: 0L,
        headline           = d["headline"] as? String ?: "",
        languages           = d["languages"] as? String ?: "",
        education           = d["education"] as? String ?: "",
        certifications      = d["certifications"] as? String ?: "",
        mentorship          = d["mentorship"] as? String ?: "",
        gymsWorked          = d["gymsWorked"] as? String ?: "",
        clientsCoached      = (d["clientsCoached"] as? Long)?.toInt() ?: 0,
        clientTypes         = d["clientTypes"] as? String ?: "",
        trainingModes       = d["trainingModes"] as? String ?: "",
        assessmentIncluded  = d["assessmentIncluded"] as? Boolean ?: false,
        cprCertified        = d["cprCertified"] as? Boolean ?: false,
        nutritionSupport    = d["nutritionSupport"] as? String ?: "",
        testimonials        = d["testimonials"] as? String ?: "",
        instagramUrl        = d["instagramUrl"] as? String ?: "",
        profileScore        = (d["profileScore"] as? Long)?.toInt() ?: 0,
        planTier            = d["planTier"] as? String ?: "",
        certDocUrl          = d["certDocUrl"] as? String ?: "",
        certStatus          = d["certStatus"] as? String ?: "",
        ratingSum           = (d["ratingSum"] as? Number)?.toFloat() ?: 0f,
        ratingCount         = (d["ratingCount"] as? Number)?.toInt() ?: 0
    ).let {
        // Trainers published before the portfolio feature have no stored score —
        // compute from whatever fields they do have so they rank fairly.
        if (it.profileScore == 0) it.copy(profileScore = PortfolioScoring.score(it)) else it
    }

    // ─── Marketplace: Bookings ────────────────────────────────────────────────

    /** True if this coach already has a CONFIRMED session at this exact slot. */
    suspend fun isSlotTaken(coachId: String, sessionDateMillis: Long): Boolean {
        if (sessionDateMillis <= 0L) return false
        return try {
            db.collection("bookings")
                .whereEqualTo("coachId", coachId)
                .whereEqualTo("sessionDateMillis", sessionDateMillis)
                .whereEqualTo("status", "CONFIRMED")
                .limit(1).get().await().documents.isNotEmpty()
        } catch (_: Exception) { false }   // fail-open on read error; confirm-time guard still applies
    }

    suspend fun createBooking(
        coachId: String, coachName: String,
        clientId: String, clientName: String,
        feeAmount: Int, notes: String, sessionDateMillis: Long = 0L
    ): String {
        // Always use the authenticated UID as clientId — clientId param is ignored to prevent IDOR
        val authenticatedClientId = uid ?: throw Exception("Not authenticated")
        // Don't let a member request a slot already confirmed for someone else
        if (sessionDateMillis > 0L && isSlotTaken(coachId, sessionDateMillis))
            throw Exception("That time slot is already booked — please pick another.")
        // Idempotency: a double-tap or post-timeout retry must not mint a second
        // request. If this member already has a live (pending/confirmed) booking
        // with this coach for this slot, return it instead of creating a dupe.
        try {
            val existing = db.collection("bookings")
                .whereEqualTo("coachId", coachId)
                .whereEqualTo("clientId", authenticatedClientId)
                .whereEqualTo("sessionDateMillis", sessionDateMillis)
                .get().await().documents
                .firstOrNull { (it.getString("status") ?: "") in listOf("PENDING", "CONFIRMED") }
            if (existing != null) return existing.id
        } catch (_: Exception) { /* fall through and create */ }
        val bookingId = java.util.UUID.randomUUID().toString()
        db.collection("bookings").document(bookingId).set(mapOf(
            "id"                  to bookingId,
            "coachId"             to coachId,
            "coachName"           to coachName,
            "clientId"            to authenticatedClientId,
            "clientName"          to clientName,
            "sessionDateMillis"   to sessionDateMillis,
            "status"              to "PENDING",
            "feeAmount"           to feeAmount,
            "notes"               to notes,
            "coachResponse"       to "",
            "clientRating"        to 0f,
            "createdAtMillis"     to System.currentTimeMillis()
        )).await()
        return bookingId
    }

    suspend fun rateBooking(bookingId: String, rating: Float, review: String = "") {
        val currentUid = uid ?: throw Exception("Not authenticated")
        val clamped = rating.coerceIn(1f, 5f)
        val now = System.currentTimeMillis()
        val bRef = db.collection("bookings").document(bookingId)

        // Part 1 — the member's OWN rating + the public review. Atomic and gated
        // on a real booking. This is what "rating succeeded" means to the member,
        // and the reviews subcollection is the source of truth for a coach's
        // average, so this must succeed for the rating to count.
        var coachId = ""
        db.runTransaction { txn ->
            val data = txn.get(bRef).data ?: throw Exception("Booking not found")
            require(data["clientId"] == currentUid) { "Not your booking" }
            val status = data["status"] as? String ?: "PENDING"
            val sessionDate = data["sessionDateMillis"] as? Long ?: 0L
            val createdAt = data["createdAtMillis"] as? Long ?: 0L
            val ratable = status == "COMPLETED" ||
                (status == "CONFIRMED" && (
                    (sessionDate in 1 until now) ||
                    (sessionDate == 0L && createdAt in 1..(now - 86_400_000L))
                ))
            require(ratable) { "You can rate this coach after the session is completed" }
            val existingRating = (data["clientRating"] as? Double)?.toFloat()
                ?: (data["clientRating"] as? Long)?.toFloat() ?: 0f
            require(existingRating == 0f) { "Already rated" }
            coachId = data["coachId"] as? String ?: throw Exception("No coachId on booking")

            txn.update(bRef, mapOf("clientRating" to clamped, "clientReview" to review))
            txn.set(db.collection("trainers").document(coachId)
                .collection("reviews").document(bookingId), mapOf(
                "clientId"        to currentUid,
                "clientName"      to (data["clientName"] as? String ?: "Member"),
                "rating"          to clamped,
                "review"          to review,
                "createdAtMillis" to now
            ))
        }.await()

        // Part 2 — best-effort denormalised aggregate on the trainer doc (drives
        // Discover ranking). Kept OUT of Part 1 so a missing trainers doc or a
        // rules edge case on the aggregate can't silently fail the whole rating;
        // the review already recorded it and the average recomputes from reviews.
        try {
            val tRef = db.collection("trainers").document(coachId)
            db.runTransaction { txn ->
                val tSnap = txn.get(tRef)
                if (!tSnap.exists()) return@runTransaction   // no profile to denormalise onto
                val sum   = (tSnap.get("ratingSum") as? Number)?.toFloat() ?: 0f
                val count = (tSnap.get("ratingCount") as? Number)?.toInt() ?: 0
                val newSum = sum + clamped
                val newCount = count + 1
                txn.set(tRef, mapOf(
                    "ratingSum"   to newSum,
                    "ratingCount" to newCount,
                    "rating"      to newSum / newCount
                ), com.google.firebase.firestore.SetOptions.merge())
            }.await()
        } catch (_: Exception) { /* review is the source of truth; average self-heals */ }
    }

    data class CoachReview(
        val clientName: String,
        val rating: Float,
        val review: String,
        val createdAtMillis: Long
    )

    /** Latest member reviews for a coach — shown on the public trainer profile. */
    suspend fun getCoachReviews(coachId: String, limit: Long = 10): List<CoachReview> = try {
        db.collection("trainers").document(coachId).collection("reviews")
            .orderBy("createdAtMillis", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(limit)
            .get().await().documents.mapNotNull { d ->
                val rating = (d.get("rating") as? Number)?.toFloat() ?: return@mapNotNull null
                CoachReview(
                    clientName      = d.getString("clientName") ?: "Member",
                    rating          = rating,
                    review          = d.getString("review") ?: "",
                    createdAtMillis = d.getLong("createdAtMillis") ?: 0L
                )
            }
    } catch (_: Exception) { emptyList() }

    /** Coach rates a member after a completed session (reliability signal
     *  other coaches see on future booking requests). */
    suspend fun rateMember(bookingId: String, rating: Float) {
        val currentUid = uid ?: throw Exception("Not authenticated")
        val clamped = rating.coerceIn(1f, 5f)
        val bRef = db.collection("bookings").document(bookingId)
        // Single transaction — same all-or-nothing guarantee as rateBooking
        db.runTransaction { txn ->
            val d = txn.get(bRef).data ?: throw Exception("Booking not found")
            require(d["coachId"] == currentUid) { "Not your booking" }
            require((d["status"] as? String) == "COMPLETED") { "Complete the session before rating the member" }
            val existing = (d["coachRating"] as? Number)?.toFloat() ?: 0f
            require(existing == 0f) { "Already rated" }
            val memberId = d["clientId"] as? String ?: throw Exception("No client on booking")
            val memberName = d["clientName"] as? String ?: "Member"
            val mRef = db.collection("member_ratings").document(memberId)
            val snap = txn.get(mRef)
            val sum   = (snap.get("ratingSum") as? Number)?.toFloat() ?: 0f
            val count = (snap.get("ratingCount") as? Number)?.toInt() ?: 0
            txn.update(bRef, "coachRating", clamped)
            txn.set(mRef, mapOf(
                "ratingSum"   to sum + clamped,
                "ratingCount" to count + 1,
                "rating"      to (sum + clamped) / (count + 1),
                "name"        to memberName
            ), com.google.firebase.firestore.SetOptions.merge())
        }.await()
    }

    /** (avg, count) of coach ratings for a member, or null if never rated. */
    suspend fun getMemberRating(memberId: String): Pair<Float, Int>? = try {
        val d = db.collection("member_ratings").document(memberId).get().await()
        val count = (d.get("ratingCount") as? Number)?.toInt() ?: 0
        if (count == 0) null
        else Pair((d.get("rating") as? Number)?.toFloat() ?: 0f, count)
    } catch (_: Exception) { null }

    suspend fun getClientBookings(clientId: String): List<Booking> =
        db.collection("bookings").whereEqualTo("clientId", clientId)
            .get().await().documents.mapNotNull { mapDocToBooking(it) }

    suspend fun getCoachBookings(coachId: String): List<Booking> =
        db.collection("bookings").whereEqualTo("coachId", coachId)
            .get().await().documents.mapNotNull { mapDocToBooking(it) }

    /** Marks a confirmed session completed — unlocks the member's star rating.
     *  Status-only update so the coach's acceptance note is preserved. */
    suspend fun completeBooking(bookingId: String) {
        val currentUid = uid ?: return
        val data = db.collection("bookings").document(bookingId).get().await().data ?: return
        if (data["coachId"] != currentUid) return
        db.collection("bookings").document(bookingId).update("status", "COMPLETED").await()
    }

    suspend fun updateBookingStatus(bookingId: String, status: String, coachResponse: String = "") {
        val currentUid = uid ?: return
        val data = db.collection("bookings").document(bookingId).get().await().data ?: return
        if (data["coachId"] != currentUid) return  // only the assigned coach may change booking status
        db.collection("bookings").document(bookingId)
            .update(mapOf("status" to status, "coachResponse" to coachResponse))
    }

    /**
     * Cancel a booking. Only allowed if more than 24 hours before the session.
     * Fetches sessionDateMillis from Firestore (not caller-supplied) to prevent 24hr bypass.
     * Returns true if cancelled, false if caller is not a party or within 24hr window.
     */
    suspend fun cancelBooking(bookingId: String, cancelledBy: String): Boolean {
        val currentUid = uid ?: return false
        val bookingDoc = db.collection("bookings").document(bookingId).get().await()
        val data = bookingDoc.data ?: return false
        if (data["clientId"] != currentUid && data["coachId"] != currentUid) return false
        // Use sessionDateMillis from Firestore — never trust caller-supplied value
        val storedDateMillis = data["sessionDateMillis"] as? Long ?: 0L
        val twentyFourHours = 24 * 60 * 60 * 1000L
        val now = System.currentTimeMillis()
        if (storedDateMillis > 0 && (storedDateMillis - now) < twentyFourHours) {
            return false
        }
        db.collection("bookings").document(bookingId)
            .update(mapOf(
                "status" to "CANCELLED",
                "coachResponse" to "Cancelled by $cancelledBy",
                "cancelledAt" to now
            )).await()
        return true
    }

    private fun mapDocToBooking(doc: com.google.firebase.firestore.DocumentSnapshot): Booking? {
        val d = doc.data ?: return null
        return Booking(
            id                 = d["id"] as? String ?: doc.id,
            coachId            = d["coachId"] as? String ?: "",
            coachName          = d["coachName"] as? String ?: "",
            clientId           = d["clientId"] as? String ?: "",
            clientName         = d["clientName"] as? String ?: "",
            status             = d["status"] as? String ?: "PENDING",
            feeAmount          = (d["feeAmount"] as? Long)?.toInt() ?: 0,
            notes              = d["notes"] as? String ?: "",
            coachResponse      = d["coachResponse"] as? String ?: "",
            createdAtMillis    = d["createdAtMillis"] as? Long ?: 0L,
            sessionDateMillis  = d["sessionDateMillis"] as? Long ?: 0L,
            clientRating       = (d["clientRating"] as? Double)?.toFloat()
                                 ?: (d["clientRating"] as? Long)?.toFloat() ?: 0f,
            coachRating        = (d["coachRating"] as? Number)?.toFloat() ?: 0f
        )
    }

    // ─── Initial Sync: Firestore → Room ───────────────────────────────────────
    // Called on login when Room is empty (new install / new device)

    suspend fun pullToRoom(coachDao: CoachDao) {
        if (uid == null) return

        // Fire all 7 collection fetches up front (Firestore Tasks start executing
        // on creation) so first-login sync costs one round-trip time, not seven.
        val clientsTask  = col("clients")?.get()
        val programsTask = col("programs")?.get()
        val paymentsTask = col("payments")?.get()
        val logsTask     = col("workout_logs")?.get()
        val measTask     = col("measurements")?.get()
        val notesTask    = col("notes")?.get()
        val snapsTask    = col("revenue_snapshots")?.get()

        // Await + map ALL collections BEFORE inserting anything. Previously
        // clients were inserted first, then the next await could throw on a
        // network drop — leaving Room with clients but no programs/payments/logs,
        // and since the re-sync gate is "client count == 0", the rest was never
        // pulled again. Now a mid-pull failure inserts nothing and the empty
        // gate re-fires on the next launch.
        // Clients
        val clients = clientsTask?.await()?.documents?.mapNotNull { doc ->
            val d = doc.data ?: return@mapNotNull null
            Client(
                id                   = d["id"] as? String ?: doc.id,
                name                 = d["name"] as? String ?: "",
                phoneNumber          = d["phoneNumber"] as? String ?: "",
                initialGoal          = d["initialGoal"] as? String ?: "",
                programWeek          = (d["programWeek"] as? Long)?.toInt() ?: 1,
                consistencyScore     = (d["consistencyScore"] as? Long)?.toInt() ?: 100,
                programId            = (d["programId"] as? String)?.takeIf { it.isNotEmpty() },
                lastCheckInMillis    = d["lastCheckInMillis"] as? Long ?: System.currentTimeMillis(),
                mrr                  = (d["mrr"] as? Long)?.toInt() ?: 0,
                status               = d["status"] as? String ?: "Active",
                enrollmentDateMillis = d["enrollmentDateMillis"] as? Long ?: System.currentTimeMillis(),
                city                 = d["city"] as? String ?: "",
                paymentCycle         = d["paymentCycle"] as? String ?: "MONTHLY",
                pausedAtMillis       = d["pausedAtMillis"] as? Long ?: 0L
            )
        } ?: emptyList()

        // Programs
        val programs = programsTask?.await()?.documents?.mapNotNull { doc ->
            val d = doc.data ?: return@mapNotNull null
            Program(
                id               = d["id"] as? String ?: doc.id,
                name             = d["name"] as? String ?: "",
                durationWeeks    = (d["durationWeeks"] as? Long)?.toInt() ?: 8,
                daysPerWeek      = (d["daysPerWeek"] as? Long)?.toInt() ?: 3,
                tags             = d["tags"] as? String ?: "",
                lastUsedMillis   = d["lastUsedMillis"] as? Long ?: System.currentTimeMillis(),
                clientCount      = (d["clientCount"] as? Long)?.toInt() ?: 0,
                isMasterTemplate = d["isMasterTemplate"] as? Boolean ?: true,
                workingDays      = d["workingDays"] as? String ?: ""
            )
        } ?: emptyList()

        // Payments
        val payments = paymentsTask?.await()?.documents?.mapNotNull { doc ->
            val d = doc.data ?: return@mapNotNull null
            Payment(
                id             = d["id"] as? String ?: doc.id,
                clientId       = d["clientId"] as? String ?: "",
                amount         = (d["amount"] as? Long)?.toInt() ?: 0,
                dueDateMillis  = d["dueDateMillis"] as? Long ?: System.currentTimeMillis(),
                daysOverdue    = (d["daysOverdue"] as? Long)?.toInt() ?: 0,
                mandateStatus  = d["mandateStatus"] as? String ?: "ACTIVE"
            )
        } ?: emptyList()

        // Workout logs
        val workoutLogs = logsTask?.await()?.documents?.mapNotNull { doc ->
            val d = doc.data ?: return@mapNotNull null
            WorkoutLog(
                id                = d["id"] as? String ?: doc.id,
                clientId          = d["clientId"] as? String ?: "",
                programId         = (d["programId"] as? String)?.takeIf { it.isNotEmpty() },
                sessionDateMillis = d["sessionDateMillis"] as? Long ?: System.currentTimeMillis(),
                sessionName       = d["sessionName"] as? String ?: "",
                exercises         = d["exercises"] as? String ?: "",
                durationMins      = (d["durationMins"] as? Long)?.toInt() ?: 0,
                notes             = d["notes"] as? String ?: "",
                isMissed          = d["isMissed"] as? Boolean ?: false,
                missedReason      = d["missedReason"] as? String ?: ""
            )
        } ?: emptyList()

        // Measurements
        val measurements = measTask?.await()?.documents?.mapNotNull { doc ->
            val d = doc.data ?: return@mapNotNull null
            BodyMeasurement(
                id         = d["id"] as? String ?: doc.id,
                clientId   = d["clientId"] as? String ?: "",
                dateMillis = d["dateMillis"] as? Long ?: System.currentTimeMillis(),
                weightKg   = (d["weightKg"] as? Double)?.toFloat() ?: 0f,
                bodyFatPct = (d["bodyFatPct"] as? Double)?.toFloat() ?: 0f,
                notes      = d["notes"] as? String ?: ""
            )
        } ?: emptyList()

        // Notes
        val notes = notesTask?.await()?.documents?.mapNotNull { doc ->
            val d = doc.data ?: return@mapNotNull null
            ClientNote(
                id         = d["id"] as? String ?: doc.id,
                clientId   = d["clientId"] as? String ?: "",
                dateMillis = d["dateMillis"] as? Long ?: System.currentTimeMillis(),
                content    = d["content"] as? String ?: ""
            )
        } ?: emptyList()

        // Revenue snapshots
        val snapshots = snapsTask?.await()?.documents?.mapNotNull { doc ->
            val d = doc.data ?: return@mapNotNull null
            RevenueSnapshot(
                monthYear = d["monthYear"] as? String ?: doc.id,
                totalMrr  = (d["totalMrr"] as? Long)?.toInt() ?: 0
            )
        } ?: emptyList()

        // All fetches succeeded — NOW insert everything
        if (clients.isNotEmpty())      coachDao.insertClients(clients)
        if (programs.isNotEmpty())     coachDao.insertPrograms(programs)
        if (payments.isNotEmpty())     coachDao.insertPayments(payments)
        workoutLogs.forEach { coachDao.insertWorkoutLog(it) }
        measurements.forEach { coachDao.insertMeasurement(it) }
        notes.forEach { coachDao.insertNote(it) }
        snapshots.forEach { coachDao.insertSnapshot(it) }
    }

    // ─── Exercise library (real-time from admin panel) ────────────────────────

    /**
     * Listens to the Firestore `exercises` collection written by the admin panel.
     * Merges admin data on top of hardcoded [ExerciseRepository.all]:
     *  - Existing hardcoded exercises get admin-edited fields applied (imageUrl, etc.)
     *  - New exercises added by admin (not in hardcoded) are appended if isPublished=true.
     * Falls back silently to hardcoded data on any Firestore error.
     */
    fun listenExercises(callback: (List<Exercise>) -> Unit): ListenerRegistration {
        return db.collection("exercises")
            .addSnapshotListener { snap, err ->
                if (err != null || snap == null) {
                    android.util.Log.e("CoachOps", "listenExercises error: ${err?.message}")
                    callback(ExerciseRepository.all)
                    return@addSnapshotListener
                }

                val fsMap = snap.documents.associateBy({ it.id }) { it.toExerciseOverride() }

                // 1. Apply admin overrides onto hardcoded exercises
                val hardcodedIds = ExerciseRepository.all.map { it.id }.toSet()
                val merged = ExerciseRepository.all.map { hardcoded ->
                    val override = fsMap[hardcoded.id] ?: return@map hardcoded
                    hardcoded.copy(
                        imageUrl       = override.imageUrl.ifBlank { hardcoded.imageUrl },
                        howTo          = override.howTo.ifEmpty { hardcoded.howTo },
                        commonErrors   = override.commonErrors.ifEmpty { hardcoded.commonErrors },
                        benefits       = override.benefits.ifEmpty { hardcoded.benefits },
                        bodyEffect     = override.bodyEffect.ifBlank { hardcoded.bodyEffect },
                        sets           = override.sets.ifBlank { hardcoded.sets },
                        reps           = override.reps.ifBlank { hardcoded.reps },
                        tempo          = override.tempo.ifBlank { hardcoded.tempo },
                        rest           = override.rest.ifBlank { hardcoded.rest },
                        estimatedMinutes = if (override.estimatedMinutes > 0) override.estimatedMinutes else hardcoded.estimatedMinutes,
                        caloriesBurned = override.caloriesBurned.ifBlank { hardcoded.caloriesBurned },
                        muscleEmoji    = override.muscleEmoji.ifBlank { hardcoded.muscleEmoji }
                    )
                }

                // 2. Append admin-created exercises not in hardcoded list
                val newExercises = snap.documents
                    .filter { it.id !in hardcodedIds && it.getBoolean("isPublished") != false }
                    .mapNotNull { it.toFullExercise() }

                callback(merged + newExercises)
            }
    }

    /** Extracts only the fields the admin panel can override (for merging onto hardcoded). */
    private fun DocumentSnapshot.toExerciseOverride(): Exercise {
        val d = data ?: return Exercise(
            id = id, name = id, category = ExerciseCategory.STRENGTH,
            primaryMuscles = emptyList(), equipment = EquipmentType.BODYWEIGHT,
            difficulty = DifficultyLevel.BEGINNER, suitableFor = emptyList(),
            sets = "", reps = "", tempo = "", rest = "",
            howTo = emptyList(), commonErrors = emptyList(), benefits = emptyList(),
            bodyEffect = "", caloriesBurned = "", muscleEmoji = ""
        )
        return Exercise(
            id             = id,
            name           = d["name"] as? String ?: "",
            category       = ExerciseCategory.STRENGTH,  // not used in merge
            primaryMuscles = emptyList(),
            equipment      = EquipmentType.BODYWEIGHT,
            difficulty     = DifficultyLevel.BEGINNER,
            suitableFor    = emptyList(),
            sets           = d["sets"] as? String ?: "",
            reps           = d["reps"] as? String ?: "",
            tempo          = d["tempo"] as? String ?: "",
            rest           = d["rest"] as? String ?: "",
            howTo          = (d["howTo"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            commonErrors   = (d["commonErrors"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            benefits       = (d["benefits"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            bodyEffect     = d["bodyEffect"] as? String ?: "",
            caloriesBurned = d["caloriesBurned"] as? String ?: "",
            muscleEmoji    = d["muscleEmoji"] as? String ?: "",
            estimatedMinutes = (d["estimatedMinutes"] as? Long)?.toInt() ?: 0,
            imageUrl       = d["imageUrl"] as? String ?: ""
        )
    }

    /** Parses a full Exercise from a Firestore document (for admin-created exercises). */
    private fun DocumentSnapshot.toFullExercise(): Exercise? {
        val d = data ?: return null
        return try {
            Exercise(
                id             = id,
                name           = d["name"] as? String ?: return null,
                sanskritName   = d["sanskritName"] as? String,
                category       = enumSafe<ExerciseCategory>(d["category"] as? String) ?: ExerciseCategory.STRENGTH,
                primaryMuscles = (d["primaryMuscles"] as? List<*>)?.mapNotNull { enumSafe<MuscleGroup>(it as? String) } ?: emptyList(),
                secondaryMuscles = (d["secondaryMuscles"] as? List<*>)?.mapNotNull { enumSafe<MuscleGroup>(it as? String) } ?: emptyList(),
                equipment      = enumSafe<EquipmentType>(d["equipment"] as? String) ?: EquipmentType.BODYWEIGHT,
                difficulty     = enumSafe<DifficultyLevel>(d["difficulty"] as? String) ?: DifficultyLevel.BEGINNER,
                suitableFor    = (d["suitableFor"] as? List<*>)?.mapNotNull { enumSafe<ClientGoal>(it as? String) } ?: emptyList(),
                sets           = d["sets"] as? String ?: "3–4 sets",
                reps           = d["reps"] as? String ?: "10–15 reps",
                tempo          = d["tempo"] as? String ?: "2-1-2-0",
                rest           = d["rest"] as? String ?: "60 sec",
                howTo          = (d["howTo"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                commonErrors   = (d["commonErrors"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                benefits       = (d["benefits"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                bodyEffect     = d["bodyEffect"] as? String ?: "",
                caloriesBurned = d["caloriesBurned"] as? String ?: "",
                muscleEmoji    = d["muscleEmoji"] as? String ?: "💪",
                estimatedMinutes = (d["estimatedMinutes"] as? Long)?.toInt() ?: 20,
                imageUrl       = d["imageUrl"] as? String ?: "",
                gifUrl         = d["gifUrl"] as? String ?: "",
                attribution    = d["attribution"] as? String ?: ""
            )
        } catch (e: Exception) {
            android.util.Log.e("CoachOps", "toFullExercise $id failed: ${e.message}")
            null
        }
    }

    private inline fun <reified T : Enum<T>> enumSafe(name: String?): T? =
        if (name.isNullOrBlank()) null
        else try { enumValueOf<T>(name) } catch (_: IllegalArgumentException) { null }

    // ─── Nutrition plans (real-time from admin panel) ─────────────────────────

    /**
     * Listens to a single `nutrition_plans/{goal}` document.
     * Calls [callback] with the Firestore plan if it exists, or null to fall
     * back to the hardcoded [NutritionRepository].
     */
    fun listenNutritionPlan(goal: ClientGoal, callback: (IndianMealPlan?) -> Unit): ListenerRegistration {
        return db.collection("nutrition_plans").document(goal.name)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    android.util.Log.e("CoachOps", "listenNutritionPlan error: ${err.message}")
                    callback(null)
                    return@addSnapshotListener
                }
                if (snap == null || !snap.exists()) {
                    callback(null)
                    return@addSnapshotListener
                }
                val d = snap.data ?: run { callback(null); return@addSnapshotListener }
                try {
                    val meals = (d["meals"] as? List<*>)?.mapNotNull { mealRaw ->
                        val m = mealRaw as? Map<*, *> ?: return@mapNotNull null
                        IndianMeal(
                            name     = m["name"] as? String ?: "",
                            timeSlot = m["timeSlot"] as? String ?: "",
                            notes    = m["notes"] as? String ?: "",
                            items    = (m["items"] as? List<*>)?.mapNotNull { itemRaw ->
                                val i = itemRaw as? Map<*, *> ?: return@mapNotNull null
                                IndianFoodItem(
                                    name          = i["name"] as? String ?: "",
                                    quantity      = i["quantity"] as? String ?: "",
                                    calories      = (i["calories"] as? Long)?.toInt() ?: 0,
                                    proteinG      = (i["proteinG"] as? Long)?.toInt() ?: 0,
                                    carbsG        = (i["carbsG"] as? Long)?.toInt() ?: 0,
                                    fatG          = (i["fatG"] as? Long)?.toInt() ?: 0,
                                    benefits      = i["benefits"] as? String ?: "",
                                    isVegetarian  = i["isVegetarian"] as? Boolean ?: true,
                                    imageUrl      = i["imageUrl"] as? String ?: ""
                                )
                            } ?: emptyList()
                        )
                    } ?: emptyList()

                    callback(IndianMealPlan(
                        goal            = goal,
                        dailyCalories   = (d["dailyCalories"] as? Long)?.toInt() ?: 0,
                        proteinG        = (d["proteinG"] as? Long)?.toInt() ?: 0,
                        carbsG          = (d["carbsG"] as? Long)?.toInt() ?: 0,
                        fatG            = (d["fatG"] as? Long)?.toInt() ?: 0,
                        hydration       = d["hydration"] as? String ?: "",
                        generalTips     = (d["generalTips"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                        meals           = meals,
                        headerImageUrl  = d["headerImageUrl"] as? String ?: "",
                        iconImageUrl    = d["iconImageUrl"] as? String ?: ""
                    ))
                } catch (e: Exception) {
                    android.util.Log.e("CoachOps", "listenNutritionPlan parse failed: ${e.message}")
                    callback(null)
                }
            }
    }

    // ─── Diet Plans (coach-to-member) ─────────────────────────────────────────

    fun saveDietPlan(plan: DietPlan) {
        val coachUid = uid ?: return
        db.collection("diet_plans").document(plan.id)
            .set(mapOf(
                "coachUid"    to coachUid,
                "clientId"    to plan.clientId,
                "clientName"  to plan.clientName,
                "title"       to plan.title,
                "description" to plan.description,
                "status"      to plan.status,
                "createdAt"   to plan.createdAt,
                "sentAt"      to plan.sentAt,
                "notes"       to plan.notes,
                "days"        to plan.days.map { day ->
                    mapOf("dayLabel" to day.dayLabel,
                          "meals"    to day.meals.map { meal ->
                              mapOf("mealName" to meal.mealName,
                                    "timeSlot" to meal.timeSlot,
                                    "foods"    to meal.foods.map { f ->
                                        mapOf("name" to f.name, "quantity" to f.quantity,
                                              "calories" to f.calories, "proteinG" to f.proteinG,
                                              "notes" to f.notes)
                                    })
                          })
                }
            ), com.google.firebase.firestore.SetOptions.merge())
    }

    fun deleteDietPlan(planId: String) {
        db.collection("diet_plans").document(planId).delete()
    }

    fun listenDietPlansForClient(coachUid: String, clientId: String, callback: (List<DietPlan>) -> Unit): ListenerRegistration =
        db.collection("diet_plans")
            .whereEqualTo("coachUid", coachUid)
            .whereEqualTo("clientId", clientId)
            .addSnapshotListener { snap, err ->
                if (err != null || snap == null) { callback(emptyList()); return@addSnapshotListener }
                callback(snap.documents.mapNotNull { doc -> parseDietPlan(doc.id, doc.data ?: return@mapNotNull null) }
                    .sortedByDescending { it.createdAt })
            }

    fun listenMyDietPlans(callback: (List<DietPlan>) -> Unit): ListenerRegistration {
        val clientUid = uid
        if (clientUid == null) { callback(emptyList()); return db.collection("diet_plans").addSnapshotListener { _, _ -> } }
        return db.collection("diet_plans")
            .whereEqualTo("clientId", clientUid)
            .whereEqualTo("status", "active")
            .addSnapshotListener { snap, err ->
                if (err != null || snap == null) { callback(emptyList()); return@addSnapshotListener }
                callback(snap.documents.mapNotNull { doc -> parseDietPlan(doc.id, doc.data ?: return@mapNotNull null) }
                    .sortedByDescending { it.sentAt })
            }
    }

    fun saveDietLog(log: DietLog) {
        db.collection("diet_logs").document(log.id)
            .set(mapOf(
                "planId"           to log.planId,
                "coachUid"         to log.coachUid,
                "clientId"         to log.clientId,
                "date"             to log.date,
                "adherencePercent" to log.adherencePercent,
                "memberNote"       to log.memberNote,
                "mealsFollowed"    to log.mealsFollowed.map { mapOf("mealName" to it.mealName, "followed" to it.followed) },
                "createdAt"        to log.createdAt,
                "isReadByCoach"    to false
            ))
    }

    fun listenDietLogs(planId: String, callback: (List<DietLog>) -> Unit): ListenerRegistration {
        val currentUid = uid  // capture auth uid at registration time
        return db.collection("diet_logs")
            .whereEqualTo("planId", planId)
            .addSnapshotListener { snap, err ->
                if (err != null || snap == null) { callback(emptyList()); return@addSnapshotListener }
                callback(snap.documents.mapNotNull { doc ->
                    val d = doc.data ?: return@mapNotNull null
                    // Only return logs the authenticated user is a party to (client or coach)
                    if (currentUid != null && d["clientId"] != currentUid && d["coachUid"] != currentUid) {
                        return@mapNotNull null
                    }
                    DietLog(
                        id                = doc.id,
                        planId            = d["planId"] as? String ?: "",
                        coachUid          = d["coachUid"] as? String ?: "",
                        clientId          = d["clientId"] as? String ?: "",
                        date              = d["date"] as? String ?: "",
                        adherencePercent  = (d["adherencePercent"] as? Long)?.toInt() ?: 0,
                        memberNote        = d["memberNote"] as? String ?: "",
                        mealsFollowed     = (d["mealsFollowed"] as? List<*>)?.mapNotNull { item ->
                            val m = item as? Map<*, *> ?: return@mapNotNull null
                            DietMealFollowed(m["mealName"] as? String ?: "", m["followed"] as? Boolean ?: false)
                        } ?: emptyList(),
                        createdAt         = d["createdAt"] as? Long ?: 0L,
                        isReadByCoach     = d["isReadByCoach"] as? Boolean ?: false
                    )
                }.sortedByDescending { it.createdAt })
            }
    }

    private fun parseDietPlan(id: String, d: Map<String, Any>): DietPlan? = try {
        DietPlan(
            id          = id,
            coachUid    = d["coachUid"] as? String ?: "",
            clientId    = d["clientId"] as? String ?: "",
            clientName  = d["clientName"] as? String ?: "",
            title       = d["title"] as? String ?: "",
            description = d["description"] as? String ?: "",
            status      = d["status"] as? String ?: "draft",
            createdAt   = d["createdAt"] as? Long ?: 0L,
            sentAt      = d["sentAt"] as? Long ?: 0L,
            notes       = d["notes"] as? String ?: "",
            days        = (d["days"] as? List<*>)?.mapNotNull { dayRaw ->
                val day = dayRaw as? Map<*, *> ?: return@mapNotNull null
                DietDay(
                    dayLabel = day["dayLabel"] as? String ?: "",
                    meals    = (day["meals"] as? List<*>)?.mapNotNull { mealRaw ->
                        val meal = mealRaw as? Map<*, *> ?: return@mapNotNull null
                        DietMeal(
                            mealName = meal["mealName"] as? String ?: "",
                            timeSlot = meal["timeSlot"] as? String ?: "",
                            foods    = (meal["foods"] as? List<*>)?.mapNotNull { foodRaw ->
                                val food = foodRaw as? Map<*, *> ?: return@mapNotNull null
                                DietFood(food["name"] as? String ?: "", food["quantity"] as? String ?: "",
                                    (food["calories"] as? Long)?.toInt() ?: 0,
                                    (food["proteinG"] as? Long)?.toInt() ?: 0,
                                    food["notes"] as? String ?: "")
                            } ?: emptyList()
                        )
                    } ?: emptyList()
                )
            } ?: emptyList()
        )
    } catch (_: Exception) { null }
}

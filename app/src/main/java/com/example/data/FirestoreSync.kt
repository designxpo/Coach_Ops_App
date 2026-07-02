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
                "role"         to "coach",
                "lastActiveAt" to System.currentTimeMillis()
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
                userPreferences.onboardingComplete = true
                val role = userDoc.getString("role") ?: ""
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

    /** Called once after client completes onboarding — creates their admin record */
    fun registerClientRecord(name: String, email: String) {
        val uid = uid ?: return
        db.collection("user_records").document(uid)
            .set(mapOf(
                "uid"          to uid,
                "displayName"  to name,
                "email"        to email,
                "role"         to "client",
                "joinedAt"     to System.currentTimeMillis(),
                "lastActiveAt" to System.currentTimeMillis()
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

    suspend fun publishTrainerProfile(profile: TrainerProfile) {
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
            "isPublic"         to true,
            "updatedAtMillis"  to System.currentTimeMillis()
        ), com.google.firebase.firestore.SetOptions.merge()).await()
        if (com.example.BuildConfig.DEBUG) android.util.Log.d("CoachOps", "publishTrainerProfile DONE — wrote to trainers/$uid")
    }

    suspend fun unpublishTrainerProfile() {
        val uid = uid ?: return
        db.collection("trainers").document(uid).update("isPublic", false).await()
    }

    suspend fun getPublicTrainers(
        clientLat: Double = 0.0,
        clientLng: Double = 0.0,
        radiusKm: Int = 0        // 0 = no radius filter (show all)
    ): List<TrainerProfile> {
        val currentUid = auth.currentUser?.uid
        if (com.example.BuildConfig.DEBUG) android.util.Log.d("CoachOps", "getPublicTrainers START — uid=$currentUid lat=$clientLat lng=$clientLng radius=$radiusKm")

        val applyRadius = radiusKm > 0 && clientLat != 0.0 && clientLng != 0.0

        // Fetch ALL docs in trainers collection (no isPublic filter) to see what's there
        val allDocs = db.collection("trainers").get().await().documents
        if (com.example.BuildConfig.DEBUG) android.util.Log.d("CoachOps", "trainers collection total docs: ${allDocs.size}")
        allDocs.forEach { doc ->
            if (com.example.BuildConfig.DEBUG) android.util.Log.d("CoachOps", "  doc ${doc.id} isPublic=${doc.data?.get("isPublic")} name=${doc.data?.get("name")}")
        }

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
            TrainerProfile(
                uid              = d["uid"] as? String ?: doc.id,
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
                lat              = trainerLat,
                lng              = trainerLng,
                updatedAtMillis  = d["updatedAtMillis"] as? Long ?: 0L
            )
        }
    }

    suspend fun getTrainerById(trainerUid: String): TrainerProfile? {
        val d = db.collection("trainers").document(trainerUid).get().await().data ?: return null
        return TrainerProfile(
            uid              = d["uid"] as? String ?: trainerUid,
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
            updatedAtMillis  = d["updatedAtMillis"] as? Long ?: 0L
        )
    }

    // ─── Marketplace: Bookings ────────────────────────────────────────────────

    suspend fun createBooking(
        coachId: String, coachName: String,
        clientId: String, clientName: String,
        feeAmount: Int, notes: String, sessionDateMillis: Long = 0L
    ): String {
        // Always use the authenticated UID as clientId — clientId param is ignored to prevent IDOR
        val authenticatedClientId = uid ?: throw Exception("Not authenticated")
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

    suspend fun rateBooking(bookingId: String, rating: Float) {
        val currentUid = uid ?: throw Exception("Not authenticated")
        // Fetch booking to verify ownership and prevent duplicate ratings
        val bookingDoc = db.collection("bookings").document(bookingId).get().await()
        val data = bookingDoc.data ?: throw Exception("Booking not found")
        require(data["clientId"] == currentUid) { "Not your booking" }
        val existingRating = (data["clientRating"] as? Double)?.toFloat()
            ?: (data["clientRating"] as? Long)?.toFloat() ?: 0f
        require(existingRating == 0f) { "Already rated" }
        val coachId = data["coachId"] as? String ?: throw Exception("No coachId on booking")
        db.collection("bookings").document(bookingId)
            .update("clientRating", rating).await()
        // Recompute aggregate rating for this coach
        val allDocs = db.collection("bookings").whereEqualTo("coachId", coachId).get().await().documents
        val ratings = allDocs.mapNotNull { doc ->
            val r = (doc.data?.get("clientRating") as? Double)?.toFloat()
                ?: (doc.data?.get("clientRating") as? Long)?.toFloat() ?: 0f
            if (r > 0f) r else null
        }
        if (ratings.isNotEmpty()) {
            val avg = ratings.sum() / ratings.size
            db.collection("trainers").document(coachId).update("rating", avg)
        }
    }

    suspend fun getClientBookings(clientId: String): List<Booking> =
        db.collection("bookings").whereEqualTo("clientId", clientId)
            .get().await().documents.mapNotNull { mapDocToBooking(it) }

    suspend fun getCoachBookings(coachId: String): List<Booking> =
        db.collection("bookings").whereEqualTo("coachId", coachId)
            .get().await().documents.mapNotNull { mapDocToBooking(it) }

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
                                 ?: (d["clientRating"] as? Long)?.toFloat() ?: 0f
        )
    }

    // ─── Initial Sync: Firestore → Room ───────────────────────────────────────
    // Called on login when Room is empty (new install / new device)

    suspend fun pullToRoom(coachDao: CoachDao) {
        if (uid == null) return

        // Clients
        val clients = col("clients")?.get()?.await()?.documents?.mapNotNull { doc ->
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
        if (clients.isNotEmpty()) coachDao.insertClients(clients)

        // Programs
        val programs = col("programs")?.get()?.await()?.documents?.mapNotNull { doc ->
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
        if (programs.isNotEmpty()) coachDao.insertPrograms(programs)

        // Payments
        val payments = col("payments")?.get()?.await()?.documents?.mapNotNull { doc ->
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
        if (payments.isNotEmpty()) coachDao.insertPayments(payments)

        // Workout logs
        col("workout_logs")?.get()?.await()?.documents?.forEach { doc ->
            val d = doc.data ?: return@forEach
            coachDao.insertWorkoutLog(WorkoutLog(
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
            ))
        }

        // Measurements
        col("measurements")?.get()?.await()?.documents?.forEach { doc ->
            val d = doc.data ?: return@forEach
            coachDao.insertMeasurement(BodyMeasurement(
                id         = d["id"] as? String ?: doc.id,
                clientId   = d["clientId"] as? String ?: "",
                dateMillis = d["dateMillis"] as? Long ?: System.currentTimeMillis(),
                weightKg   = (d["weightKg"] as? Double)?.toFloat() ?: 0f,
                bodyFatPct = (d["bodyFatPct"] as? Double)?.toFloat() ?: 0f,
                notes      = d["notes"] as? String ?: ""
            ))
        }

        // Notes
        col("notes")?.get()?.await()?.documents?.forEach { doc ->
            val d = doc.data ?: return@forEach
            coachDao.insertNote(ClientNote(
                id         = d["id"] as? String ?: doc.id,
                clientId   = d["clientId"] as? String ?: "",
                dateMillis = d["dateMillis"] as? Long ?: System.currentTimeMillis(),
                content    = d["content"] as? String ?: ""
            ))
        }

        // Revenue snapshots
        col("revenue_snapshots")?.get()?.await()?.documents?.forEach { doc ->
            val d = doc.data ?: return@forEach
            coachDao.insertSnapshot(RevenueSnapshot(
                monthYear = d["monthYear"] as? String ?: doc.id,
                totalMrr  = (d["totalMrr"] as? Long)?.toInt() ?: 0
            ))
        }
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
                imageUrl       = d["imageUrl"] as? String ?: ""
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

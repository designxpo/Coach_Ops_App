package com.example.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

/**
 * Cloud-first profile sync.
 *
 * Source of truth = Firestore.  UserPreferences = read-through device cache.
 *
 * Firestore layout:
 *   user_records/{uid}  — profile snapshot for both coaches and members
 *   trainers/{uid}      — coach public marketplace profile (already written by FirestoreSync)
 *
 * Call saveCoachProfile() / saveMemberProfile() whenever profile data changes.
 * Call restoreProfile() on every login so any device shows the current profile.
 */
object ProfileSync {

    private val db   = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // ─── Coach: save all profile fields to Firestore ──────────────────────────

    suspend fun saveCoachProfile(prefs: UserPreferences) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("user_records").document(uid).set(
            mapOf(
                "uid"                to uid,
                "displayName"        to prefs.coachName,
                "email"              to (auth.currentUser?.email ?: prefs.coachEmail),
                "phone"              to prefs.coachPhone,
                "specialty"          to prefs.coachSpecialty,
                "clientRange"        to prefs.coachClientRange,
                "challenge"          to prefs.coachChallenge,
                "role"               to prefs.userRole.ifEmpty { "coach" },
                "gymName"            to prefs.gymName,
                "gymAddress"         to prefs.gymAddress,
                "gymGstin"           to prefs.gymGstin,
                "onboardingComplete" to prefs.onboardingComplete,
                "subscriptionPlan"   to prefs.subscriptionPlan,
                "profilePhotoUrl"    to prefs.profilePhotoUrl,
                "lastActiveAt"       to System.currentTimeMillis()
            ) + healthMap(prefs),
            SetOptions.merge()
        ).await()
    }

    // ─── Member: save all profile fields to Firestore ─────────────────────────

    suspend fun saveMemberProfile(prefs: UserPreferences) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("user_records").document(uid).set(
            mapOf(
                "uid"                to uid,
                "displayName"        to prefs.clientName,
                "email"              to (auth.currentUser?.email ?: ""),
                "phone"              to prefs.coachPhone,
                "clientGoal"         to prefs.clientGoal,
                "clientCity"         to prefs.clientCity,
                "role"               to "client",
                "onboardingComplete" to prefs.onboardingComplete,
                "profilePhotoUrl"    to prefs.profilePhotoUrl,
                "lastActiveAt"       to System.currentTimeMillis()
            ) + healthMap(prefs),
            SetOptions.merge()
        ).await()
        // Keep the phone → account directory current for gym/coach linking
        FirestoreSync.savePhoneIndex(prefs.coachPhone, prefs.clientName)
    }

    // ─── Health profile snapshot (saved separately so either role can call it) ─

    suspend fun saveHealthProfile(prefs: UserPreferences) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("user_records").document(uid)
            .set(healthMap(prefs), SetOptions.merge()).await()
    }

    // ─── Restore: cloud → local prefs (called on every login) ─────────────────

    suspend fun restoreProfile(uid: String, role: String, prefs: UserPreferences) {
        try {
            val doc = db.collection("user_records").document(uid).get().await()
            if (!doc.exists()) return
            val d = doc.data ?: return

            // ── Common ──────────────────────────────────────────────────────────
            prefs.onboardingComplete = (d["onboardingComplete"] as? Boolean) ?: prefs.onboardingComplete
            (d["profilePhotoUrl"] as? String)?.let { if (it.isNotEmpty()) prefs.profilePhotoUrl = it }
            restoreHealth(d, prefs)

            // ── Role-specific ("coach" and "gym_owner" share the coach shell) ───
            if (role != "client") {
                val name = d["displayName"] as? String ?: ""
                if (name.isNotEmpty()) prefs.coachName = name
                val email = d["email"] as? String ?: ""
                if (email.isNotEmpty()) prefs.coachEmail = email
                val phone = d["phone"] as? String ?: ""
                prefs.coachPhone = phone
                prefs.coachSpecialty   = d["specialty"]   as? String ?: prefs.coachSpecialty
                prefs.coachClientRange = d["clientRange"] as? String ?: prefs.coachClientRange
                prefs.coachChallenge   = d["challenge"]   as? String ?: prefs.coachChallenge
                val plan = d["subscriptionPlan"] as? String ?: ""
                if (plan.isNotEmpty()) prefs.subscriptionPlan = plan

                // Gym-owner profile
                (d["gymName"]    as? String)?.let { if (it.isNotEmpty()) prefs.gymName = it }
                (d["gymAddress"] as? String)?.let { if (it.isNotEmpty()) prefs.gymAddress = it }
                (d["gymGstin"]   as? String)?.let { if (it.isNotEmpty()) prefs.gymGstin = it }
                (d["gymTrialStartedAt"] as? Long)?.let { if (it > 0L) prefs.gymTrialStartedAt = it }

                // Also restore trainer marketplace profile
                restoreTrainerProfile(uid, prefs)
            } else {
                val name = d["displayName"] as? String ?: ""
                if (name.isNotEmpty()) prefs.clientName = name
                val goal = d["clientGoal"] as? String ?: ""
                if (goal.isNotEmpty()) prefs.clientGoal = goal
                prefs.clientCity = d["clientCity"] as? String ?: prefs.clientCity
            }
        } catch (_: Exception) {
            // Offline — silent fail, user gets local cache
        }
    }

    // ─── Trainer marketplace profile (coaches only) ────────────────────────────

    private suspend fun restoreTrainerProfile(uid: String, prefs: UserPreferences) {
        try {
            val doc = db.collection("trainers").document(uid).get().await()
            if (!doc.exists()) return
            val d = doc.data ?: return
            (d["bio"]               as? String)?.let  { prefs.trainerBio               = it }
            (d["city"]              as? String)?.let  { prefs.trainerCity              = it }
            (d["isPublic"]          as? Boolean)?.let { prefs.trainerIsPublic           = it }
            (d["feePerSession"]     as? Long)?.toInt()?.let { prefs.trainerFeePerSession = it }
            (d["feeMonthly"]        as? Long)?.toInt()?.let { prefs.trainerFeeMonthly   = it }
            (d["availabilityDays"]  as? String)?.let  { prefs.trainerAvailabilityDays  = it }
            (d["yearsExperience"]   as? Long)?.toInt()?.let { prefs.trainerYearsExperience = it }
            (d["profileImageUrl"]   as? String)?.let  { prefs.trainerProfileImageUrl   = it }
            (d["portfolioImages"]   as? String)?.let  { prefs.trainerPortfolioImages   = it }
            val lat = (d["lat"] as? Double) ?: 0.0
            val lng = (d["lng"] as? Double) ?: 0.0
            if (lat != 0.0) prefs.trainerLat = lat
            if (lng != 0.0) prefs.trainerLng = lng
        } catch (_: Exception) {}
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private fun healthMap(prefs: UserPreferences): Map<String, Any> = mapOf(
        "healthAge"        to prefs.healthAgeYears,
        "healthHeightCm"   to prefs.healthHeightCm.toDouble(),
        "healthWeightKg"   to prefs.healthWeightKg.toDouble(),
        "healthGender"     to prefs.healthGender,
        "healthActivity"   to prefs.healthActivity,
        "healthWaistCm"    to prefs.healthWaistCm.toDouble(),
        "healthNeckCm"     to prefs.healthNeckCm.toDouble(),
        "healthHipCm"      to prefs.healthHipCm.toDouble()
    )

    private fun restoreHealth(d: Map<String, Any>, prefs: UserPreferences) {
        (d["healthAge"]      as? Long)?.toInt()?.let    { if (it > 0) prefs.healthAgeYears = it }
        (d["healthHeightCm"] as? Double)?.toFloat()?.let { if (it > 0) prefs.healthHeightCm = it }
        (d["healthWeightKg"] as? Double)?.toFloat()?.let { if (it > 0) prefs.healthWeightKg = it }
        (d["healthGender"]   as? String)?.let           { if (it.isNotEmpty()) prefs.healthGender = it }
        (d["healthActivity"] as? String)?.let           { if (it.isNotEmpty()) prefs.healthActivity = it }
        (d["healthWaistCm"]  as? Double)?.toFloat()?.let { if (it > 0) prefs.healthWaistCm = it }
        (d["healthNeckCm"]   as? Double)?.toFloat()?.let { if (it > 0) prefs.healthNeckCm = it }
        (d["healthHipCm"]    as? Double)?.toFloat()?.let { if (it > 0) prefs.healthHipCm = it }
    }
}

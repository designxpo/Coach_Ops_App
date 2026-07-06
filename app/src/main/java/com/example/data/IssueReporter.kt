package com.example.data

import android.content.Context
import android.os.Build
import com.example.BuildConfig
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * In-app issue reporting — any signed-in user can report a problem straight
 * to the team; reports land in the `issue_reports` collection and surface on
 * the admin portal's Issues page.
 *
 * Every report auto-attaches diagnostics (app version, device, Android
 * version, role) plus the most recent crash from CrashLogger, so "it
 * crashed" reports arrive with the stack trace already included.
 */
object IssueReporter {

    val CATEGORIES = listOf(
        "App crash", "Payment problem", "Steps / health tracking",
        "Food scanner / diary", "Gym management", "Account / login", "Other"
    )

    suspend fun submit(context: Context, category: String, message: String) {
        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser ?: throw Exception("Please sign in to report an issue")
        val prefs = UserPreferences.getInstance(context)

        val name = prefs.coachName.ifEmpty { prefs.clientName }.ifEmpty { user.displayName ?: "" }
        val report = mapOf(
            "uid"            to user.uid,
            "name"           to name,
            "email"          to (user.email ?: prefs.coachEmail),
            "role"           to prefs.userRole,
            "category"       to category,
            "message"        to message.trim().take(4000),
            "status"         to "OPEN",
            "appVersion"     to BuildConfig.VERSION_NAME,
            "appVersionCode" to BuildConfig.VERSION_CODE,
            "device"         to "${Build.MANUFACTURER} ${Build.MODEL}",
            "androidVersion" to "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            "lastCrash"      to (CrashLogger.lastCrash(context)?.take(6000) ?: ""),
            "createdAt"      to System.currentTimeMillis(),
        )
        FirebaseFirestore.getInstance().collection("issue_reports").add(report).await()
    }
}

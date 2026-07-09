package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.data.CoachRepository
import com.example.data.FeatureFlagManager
import com.example.data.FirestoreSync
import com.example.data.UserPreferences
import com.example.ui.CoachOpsApp
import com.example.ui.theme.MyApplicationTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    private val notifPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* schedule regardless of grant result */ scheduleDailyCheck() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // First thing: every crash gets written to files/crash_log.txt before
        // reaching Crashlytics — no crash is ever untraceable again
        com.example.data.CrashLogger.install(this)
        enableEdgeToEdge()
        val repository = CoachRepository.getInstance(applicationContext)
        val userPreferences = UserPreferences.getInstance(applicationContext)
        val featureFlagManager = FeatureFlagManager.getInstance(applicationContext)
        setContent {
            MyApplicationTheme {
                CoachOpsApp(repository, userPreferences, featureFlagManager)
            }
        }
        // Create the push channel NOW: background FCM messages are displayed by
        // the SDK itself and land on a "Miscellaneous" fallback channel if this
        // doesn't exist before the first push arrives
        ProCoachMessagingService.ensureChannel(
            getSystemService(android.app.NotificationManager::class.java)
        )
        requestNotificationPermissionIfNeeded()
        registerFcmToken()
        // Capture the pedometer delta on every app launch — steps walked while
        // the app was closed are credited the moment it opens (see StepCounterManager)
        com.example.data.StepCounterManager.getInstance(applicationContext).start()
        // Live tracking banner: keeps counting with the app closed (user-toggleable)
        StepTrackingService.startIfEnabled(this)
    }

    // Register FCM token as soon as auth is ready — avoids the race where
    // FirebaseMessaging.token resolves before Firebase Auth restores the session.
    // Self-removing: a permanent listener stacks one copy per activity
    // recreation and fires duplicate writes on every later sign-in.
    private fun registerFcmToken() {
        val auth = FirebaseAuth.getInstance()
        auth.addAuthStateListener(object : FirebaseAuth.AuthStateListener {
            override fun onAuthStateChanged(a: FirebaseAuth) {
                if (a.currentUser == null) return
                a.removeAuthStateListener(this)
                FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                    FirestoreSync.saveFcmToken(token)
                }
                // Report the running version on every launch — powers the
                // admin panel's update-adoption tracking
                FirestoreSync.saveAppVersion()
            }
        })
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return
            }
        }
        scheduleDailyCheck()
    }

    private fun scheduleDailyCheck() {
        val request = PeriodicWorkRequestBuilder<NotificationWorker>(24, TimeUnit.HOURS)
            .addTag(NotificationWorker.WORK_TAG)
            .build()
        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            NotificationWorker.WORK_TAG,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
        // Evening streak-saver + award notifications for members
        StreakGuardWorker.schedule(applicationContext)
    }
}

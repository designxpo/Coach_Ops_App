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
        enableEdgeToEdge()
        val repository = CoachRepository.getInstance(applicationContext)
        val userPreferences = UserPreferences.getInstance(applicationContext)
        val featureFlagManager = FeatureFlagManager.getInstance(applicationContext)
        setContent {
            MyApplicationTheme {
                CoachOpsApp(repository, userPreferences, featureFlagManager)
            }
        }
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
    private fun registerFcmToken() {
        FirebaseAuth.getInstance().addAuthStateListener { auth ->
            if (auth.currentUser != null) {
                FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                    FirestoreSync.saveFcmToken(token)
                }
            }
        }
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
    }
}

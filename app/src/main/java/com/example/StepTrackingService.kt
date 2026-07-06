package com.example

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.example.data.DailyHealthLog
import com.example.data.HealthRepository
import com.example.data.StepCounterManager
import com.example.data.UserPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Live health tracking — a foreground service with a pinned notification.
 *
 * Keeps the step sensor listener alive while the app is CLOSED so steps count
 * continuously, and shows a fixed banner in the notification centre with
 * today's steps, calories, water and goal progress — plus a "+1 Water"
 * quick action, like a mini widget.
 *
 * Silent, low-priority channel: it never buzzes, it just sits pinned.
 */
class StepTrackingService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var waterToday = 0
    private var lastShownSteps = -1

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_ADD_WATER) {
            addWater()
            return START_STICKY
        }

        ensureChannel()
        val steps = StepCounterManager.getInstance(this).dailySteps.value
        try {
            ServiceCompat.startForeground(
                this, NOTIF_ID, buildNotification(steps),
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH else 0
            )
        } catch (_: Exception) {
            stopSelf()
            return START_NOT_STICKY
        }

        val mgr = StepCounterManager.getInstance(this)
        mgr.start()

        // Live-update the banner as steps arrive. Everything is guarded: an
        // exception in this always-running service would kill the whole app
        // process regardless of which screen the user is on.
        scope.launch {
            try {
                mgr.dailySteps.collect { s ->
                    if (s != lastShownSteps) {
                        lastShownSteps = s
                        notifyUpdate(s)
                    }
                }
            } catch (_: Exception) { /* banner stops updating; app keeps running */ }
        }
        // Load today's water once so the banner shows it
        scope.launch(Dispatchers.IO) {
            try {
                val prefs = UserPreferences.getInstance(this@StepTrackingService)
                if (prefs.userId.isNotEmpty()) {
                    val repo = HealthRepository(prefs.userId)
                    waterToday = repo.getLog(repo.todayKey()).waterGlasses
                    notifyUpdate(StepCounterManager.getInstance(this@StepTrackingService).dailySteps.value)
                }
            } catch (_: Exception) { }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    // ── Quick action: +1 glass of water ───────────────────────────────────────

    private fun addWater() {
        scope.launch(Dispatchers.IO) {
            try {
                val prefs = UserPreferences.getInstance(this@StepTrackingService)
                if (prefs.userId.isEmpty()) return@launch
                val repo = HealthRepository(prefs.userId)
                val today = repo.todayKey()
                val log = repo.getLog(today)
                val updated = log.copy(
                    date = today,
                    waterGlasses = (log.waterGlasses + 1).coerceAtMost(20)
                )
                repo.saveLog(updated)
                waterToday = updated.waterGlasses
                notifyUpdate(StepCounterManager.getInstance(this@StepTrackingService).dailySteps.value)
            } catch (_: Exception) { }
        }
    }

    // ── Notification ──────────────────────────────────────────────────────────

    private fun notifyUpdate(steps: Int) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        try { nm.notify(NOTIF_ID, buildNotification(steps)) } catch (_: Exception) { }
    }

    private fun buildNotification(steps: Int): android.app.Notification {
        val pct = ((steps * 100) / STEP_GOAL).coerceIn(0, 100)
        val kcal = (steps * 0.04f).toInt()

        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            ?: Intent(this, MainActivity::class.java)
        val openIntent = PendingIntent.getActivity(
            this, 0, launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val waterIntent = PendingIntent.getService(
            this, 1,
            Intent(this, StepTrackingService::class.java).setAction(ACTION_ADD_WATER),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_myplaces)
            .setContentTitle("🏃 ${"%,d".format(steps)} steps today")
            .setContentText("🔥 $kcal kcal · 💧 $waterToday glasses · $pct% of ${"%,d".format(STEP_GOAL)}")
            .setProgress(STEP_GOAL, steps.coerceAtMost(STEP_GOAL), false)
            .setContentIntent(openIntent)
            .addAction(0, "＋1 Water", waterIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    private fun ensureChannel() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(CHANNEL_ID) != null) return
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Live Health Tracking", NotificationManager.IMPORTANCE_LOW)
                .apply {
                    description = "Pinned banner with today's steps, water & goal progress"
                    setShowBadge(false)
                }
        )
    }

    companion object {
        const val CHANNEL_ID = "step_tracking"
        const val NOTIF_ID = 2001
        const val ACTION_ADD_WATER = "com.example.action.ADD_WATER"
        private const val STEP_GOAL = 10_000

        /**
         * Starts live tracking when all preconditions hold:
         * user enabled it, device has a step sensor, and (Android 10+)
         * ACTIVITY_RECOGNITION is granted — required for a health-type
         * foreground service on Android 14+.
         */
        fun startIfEnabled(context: Context) {
            val prefs = UserPreferences.getInstance(context)
            if (!prefs.liveTrackingEnabled) return
            if (!StepCounterManager.getInstance(context).isAvailable) return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACTIVITY_RECOGNITION)
                != PackageManager.PERMISSION_GRANTED
            ) return
            try {
                ContextCompat.startForegroundService(
                    context, Intent(context, StepTrackingService::class.java)
                )
            } catch (_: Exception) { /* FGS restrictions — will retry next app open */ }
        }

        fun stop(context: Context) {
            try { context.stopService(Intent(context, StepTrackingService::class.java)) } catch (_: Exception) { }
        }
    }
}

/** Restores the live tracking banner after the phone reboots. */
class BootReceiver : android.content.BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            StepTrackingService.startIfEnabled(context)
        }
    }
}

package com.example

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.AwardsEngine
import com.example.data.UserPreferences
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Evening check that keeps members' streaks alive (the retention loop):
 *  - "🔥 Your N-day streak ends at midnight — X steps to go!"
 *  - "🏅 Award unlocked: …" for anything earned since the last check
 *  - month-end nudge when the Monthly Challenge is within reach
 *
 * Scheduled daily around [FIRE_HOUR]:[FIRE_MINUTE] local time — late enough
 * that the day's steps are meaningful, early enough to act on the nudge.
 */
class StreakGuardWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prefs = UserPreferences.getInstance(context)
        if (prefs.userRole != "client") return Result.success()   // members only
        val uid = prefs.userId
        if (uid.isEmpty()) return Result.success()

        val state = AwardsEngine.evaluate(uid) ?: return Result.success()

        // Award celebrations (cap at 2 so a catch-up day doesn't spam)
        state.newlyEarned.take(2).forEach { d ->
            AwardNotifier.show(
                context, NOTIF_AWARD_BASE + d.id.hashCode().mod(500),
                "🏅 Award unlocked: ${d.title}", d.description
            )
        }

        // Streak at risk — only when there IS a streak and today isn't done
        if (state.stats.currentStreak >= 3 && !state.todayGoalMet) {
            val remaining = AwardsEngine.STEP_GOAL - state.todaySteps
            AwardNotifier.show(
                context, NOTIF_STREAK,
                "🔥 ${state.stats.currentStreak}-day streak at risk!",
                "${"%,d".format(remaining)} steps to go before midnight — a quick walk saves it."
            )
        }

        // Monthly Challenge nudge in the last 5 days of the month
        val cal = Calendar.getInstance()
        val daysLeft = cal.getActualMaximum(Calendar.DAY_OF_MONTH) - cal.get(Calendar.DAY_OF_MONTH)
        val need = AwardsEngine.MONTHLY_TARGET - state.stats.monthGoalDays -
            if (state.todayGoalMet) 1 else 0
        if (daysLeft in 1..5 && need in 1..daysLeft) {
            AwardNotifier.show(
                context, NOTIF_MONTHLY,
                "🗓️ Monthly Challenge within reach",
                "$need more goal ${if (need == 1) "day" else "days"} this month earns the badge — $daysLeft days left."
            )
        }
        return Result.success()
    }

    companion object {
        private const val WORK_NAME   = "coachops_streak_guard"
        private const val FIRE_HOUR   = 19
        private const val FIRE_MINUTE = 30
        private const val NOTIF_STREAK     = 4001
        private const val NOTIF_MONTHLY    = 4002
        private const val NOTIF_AWARD_BASE = 4100

        /** Enqueue the daily check, anchored to the next 19:30 local time. */
        fun schedule(context: Context) {
            val now  = Calendar.getInstance()
            val next = (now.clone() as Calendar).apply {
                set(Calendar.HOUR_OF_DAY, FIRE_HOUR)
                set(Calendar.MINUTE, FIRE_MINUTE)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                if (before(now) || timeInMillis == now.timeInMillis) add(Calendar.DAY_OF_YEAR, 1)
            }
            val request = PeriodicWorkRequestBuilder<StreakGuardWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(next.timeInMillis - now.timeInMillis, TimeUnit.MILLISECONDS)
                .addTag(WORK_NAME)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request
            )
        }
    }
}

/** Local notifications for streaks & awards, on their own mutable channel. */
object AwardNotifier {
    private const val CHANNEL_ID = "procoach_streaks"

    fun show(context: Context, id: Int, title: String, body: String) {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        ensureChannel(nm)
        val tapIntent = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try {
            nm.notify(id, NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_notify)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(tapIntent)
                .setAutoCancel(true)
                .build())
        } catch (_: SecurityException) { /* permission revoked mid-flight */ }
    }

    private fun ensureChannel(nm: NotificationManager) {
        if (nm.getNotificationChannel(CHANNEL_ID) != null) return
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Streaks & Awards",
                NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Streak reminders and award celebrations"
            }
        )
    }
}

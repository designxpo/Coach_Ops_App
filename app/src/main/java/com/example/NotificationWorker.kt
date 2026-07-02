package com.example

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.CoachRepository

class NotificationWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        ensureChannel(nm)

        val prefs = com.example.data.UserPreferences.getInstance(context)
        var notifId = 1000

        if (prefs.userRole == "client") {
            notifId = notifyMemberGymExpiry(nm, prefs, notifId)
            return Result.success()
        }

        // ── Coach / gym owner alerts ──────────────────────────────────────────
        val repo = CoachRepository.getInstance(context)
        val lowConsistency = repo.getLowConsistencyClients()
        val overduePayments = repo.getOverduePayments()

        lowConsistency.forEach { client ->
            nm.notify(notifId++, NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("${client.name} needs attention")
                .setContentText("Consistency at ${client.consistencyScore}% — reach out now")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build())
        }

        if (overduePayments.isNotEmpty()) {
            val total = overduePayments.sumOf { it.amount }
            nm.notify(notifId++, NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("${overduePayments.size} payment(s) need follow-up")
                .setContentText("₹${"%,d".format(total)} outstanding — send reminders today")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .build())
        }

        notifyGymRenewalsDue(nm, notifId)
        return Result.success()
    }

    /** Gym owner: daily digest of memberships expiring in ≤3 days or lapsed in the last 7. */
    private suspend fun notifyGymRenewalsDue(nm: NotificationManager, startId: Int) {
        try {
            val now = System.currentTimeMillis()
            val expiring = com.example.data.GymRepository.getInstance(context)
                .getMembersExpiringBetween(now - 7 * 86400000L, now + 3 * 86400000L)
            if (expiring.isEmpty()) return

            val names = expiring.take(3).joinToString(", ") { it.name }
            val extra = if (expiring.size > 3) " +${expiring.size - 3} more" else ""
            nm.notify(startId, NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("🏋️ ${expiring.size} gym renewal(s) due")
                .setContentText("$names$extra — collect fees & send reminders")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build())
        } catch (_: Exception) { /* gym tables absent for plain coaches — nothing to do */ }
    }

    /** Member: reminds them in their own app when their gym plan is about to lapse. */
    private suspend fun notifyMemberGymExpiry(
        nm: NotificationManager,
        prefs: com.example.data.UserPreferences,
        startId: Int
    ): Int {
        var id = startId
        try {
            val memberships = com.example.data.GymMembershipLookup.findByPhone(prefs.coachPhone)
            memberships.forEach { ms ->
                if (ms.planEndMillis <= 0L) return@forEach
                val daysLeft = ms.daysLeft
                val message = when {
                    ms.isExpired && daysLeft >= -3 ->
                        "Your ${ms.gymName} membership has expired — renew today to keep training!"
                    !ms.isExpired && daysLeft <= 3 ->
                        "Your ${ms.gymName} ${ms.planName} plan expires in $daysLeft day(s) — renew in advance!"
                    else -> return@forEach
                }
                nm.notify(id++, NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle("🏋️ Gym fee reminder")
                    .setContentText(message)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .build())
            }
        } catch (_: Exception) { /* offline — retry on next daily run */ }
        return id
    }

    private fun ensureChannel(nm: NotificationManager) {
        if (nm.getNotificationChannel(CHANNEL_ID) != null) return
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "ProCoach India Alerts", NotificationManager.IMPORTANCE_HIGH)
                .apply { description = "Client and payment alerts" }
        )
    }

    companion object {
        const val CHANNEL_ID = "coachops_alerts"
        const val WORK_TAG = "coachops_daily_check"
    }
}

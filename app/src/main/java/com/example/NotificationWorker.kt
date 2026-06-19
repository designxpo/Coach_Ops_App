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
        val repo = CoachRepository.getInstance(context)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        ensureChannel(nm)

        val lowConsistency = repo.getLowConsistencyClients()
        val overduePayments = repo.getOverduePayments()

        var notifId = 1000

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

        return Result.success()
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

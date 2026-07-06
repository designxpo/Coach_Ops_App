package com.example

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.data.FirestoreSync
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class ProCoachMessagingService : FirebaseMessagingService() {

    // Called when FCM assigns a new token (first install, token refresh, app restore).
    // Auth may not be ready yet, so wait for it with a SELF-REMOVING listener —
    // a permanent one would stack per refresh and re-save stale tokens forever.
    override fun onNewToken(token: String) {
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser != null) {
            FirestoreSync.saveFcmToken(token)
            return
        }
        auth.addAuthStateListener(object : FirebaseAuth.AuthStateListener {
            override fun onAuthStateChanged(a: FirebaseAuth) {
                if (a.currentUser == null) return
                a.removeAuthStateListener(this)
                // Fetch fresh — the captured token may have rotated by now
                com.google.firebase.messaging.FirebaseMessaging.getInstance().token
                    .addOnSuccessListener { FirestoreSync.saveFcmToken(it) }
            }
        })
    }

    // Called when a notification arrives while the app is in the foreground
    // (Background/killed: FCM displays it automatically from the notification payload)
    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title ?: message.data["title"] ?: return
        val body  = message.notification?.body  ?: message.data["body"]  ?: return
        showNotification(title, body)
    }

    private fun showNotification(title: String, body: String) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        ensureChannel(nm)

        val tapIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_SINGLE_TOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        nm.notify(
            System.currentTimeMillis().toInt(),
            NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_notify)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(tapIntent)
                .build()
        )
    }

    private fun ensureChannel(nm: NotificationManager) = Companion.ensureChannel(nm)

    companion object {
        const val CHANNEL_ID = "procoach_push"

        /**
         * Called at app startup too (MainActivity): background pushes are
         * displayed by the FCM SDK itself, and if this channel doesn't exist
         * yet they silently fall back to a "Miscellaneous" channel.
         */
        fun ensureChannel(nm: NotificationManager) {
            if (nm.getNotificationChannel(CHANNEL_ID) != null) return
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "ProCoach India", NotificationManager.IMPORTANCE_HIGH)
                    .apply { description = "Updates and announcements from ProCoach India" }
            )
        }
    }
}

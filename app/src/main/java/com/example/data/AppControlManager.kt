package com.example.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.tasks.await

data class AppControlState(
    val maintenanceMode:     Boolean = false,
    val maintenanceMessage:  String  = "ProCoach India is under maintenance. We'll be back shortly.",
    val forceUpdate:         Boolean = false,
    val minVersion:          String  = "1.0.0",
    val updateMessage:       String  = "A new version of ProCoach India is required. Please update from the Play Store.",
    val announcementEnabled: Boolean = false,
    val announcementText:    String  = "",
    val announcementType:    String  = "info",  // info | warning | success
    // ── Version-count update gate ─────────────────────────────────────────────
    // latestVersionCode: the newest versionCode on Play (0 = disabled).
    // updateNudgeEnabled: show a dismissible "update available" banner to anyone
    //   who is behind but within the compulsory threshold.
    // compulsoryUpdateAfter: when a user is behind by MORE than this many
    //   versions, a non-dismissable "update to continue" sheet is shown.
    val latestVersionCode:     Int     = 0,
    val updateNudgeEnabled:    Boolean = false,
    val compulsoryUpdateAfter: Int     = 4
)

data class RemoteConfig(
    // Client caps per tier — 0 means unlimited. Defaults MUST match the
    // SubscriptionPlan enum (Starter 5 · Pro 20 · Business unlimited) so a
    // missing remote_config doc changes nothing.
    val maxClientsStarter:          Int = 5,
    val maxClientsPro:              Int = 20,
    val maxClientsBusiness:         Int = 0,
    val consistencyRedThreshold:    Int = 40,
    val consistencyYellowThreshold: Int = 70,
    val checkInAlertDays:           Int = 5,
    val trialDays:                  Int = 14
) {
    /** Effective client cap for a plan — the admin portal's Remote Config knob. */
    fun maxClientsFor(plan: SubscriptionPlan): Int {
        val raw = when (plan) {
            SubscriptionPlan.STARTER  -> maxClientsStarter
            SubscriptionPlan.PRO      -> maxClientsPro
            SubscriptionPlan.BUSINESS -> maxClientsBusiness
        }
        return if (raw <= 0) Int.MAX_VALUE else raw   // 0 = unlimited
    }
}

object AppControlManager {

    private val db   = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    var appControl   = AppControlState()
        private set
    var remoteConfig = RemoteConfig()
        private set

    // ─── Real-time listeners (replace one-shot fetch) ─────────────────────────

    fun listenAppControl(callback: (AppControlState) -> Unit): ListenerRegistration =
        db.collection("admin_config").document("app_control")
            .addSnapshotListener { snap, err ->
                if (err != null || snap == null || !snap.exists()) { callback(AppControlState()); return@addSnapshotListener }
                val d = snap.data ?: return@addSnapshotListener
                appControl = AppControlState(
                    maintenanceMode     = d["maintenanceMode"]     as? Boolean ?: false,
                    maintenanceMessage  = d["maintenanceMessage"]  as? String  ?: AppControlState().maintenanceMessage,
                    forceUpdate         = d["forceUpdate"]         as? Boolean ?: false,
                    minVersion          = d["minVersion"]          as? String  ?: "1.0.0",
                    updateMessage       = d["updateMessage"]       as? String  ?: AppControlState().updateMessage,
                    announcementEnabled = d["announcementEnabled"] as? Boolean ?: false,
                    announcementText    = d["announcementText"]    as? String  ?: "",
                    announcementType    = d["announcementType"]    as? String  ?: "info",
                    latestVersionCode     = (d["latestVersionCode"]     as? Long)?.toInt() ?: 0,
                    updateNudgeEnabled    = d["updateNudgeEnabled"]     as? Boolean ?: false,
                    compulsoryUpdateAfter = (d["compulsoryUpdateAfter"] as? Long)?.toInt() ?: 4
                )
                callback(appControl)
            }

    fun listenRemoteConfig(callback: (RemoteConfig) -> Unit): ListenerRegistration =
        db.collection("admin_config").document("remote_config")
            .addSnapshotListener { snap, err ->
                if (err != null || snap == null || !snap.exists()) { callback(RemoteConfig()); return@addSnapshotListener }
                val d = snap.data ?: return@addSnapshotListener
                remoteConfig = RemoteConfig(
                    maxClientsStarter          = (d["maxClientsStarter"]          as? Long)?.toInt() ?: 5,
                    maxClientsPro              = (d["maxClientsPro"]              as? Long)?.toInt() ?: 20,
                    maxClientsBusiness         = (d["maxClientsBusiness"]         as? Long)?.toInt() ?: 0,
                    consistencyRedThreshold    = (d["consistencyRedThreshold"]    as? Long)?.toInt() ?: 40,
                    consistencyYellowThreshold = (d["consistencyYellowThreshold"] as? Long)?.toInt() ?: 70,
                    checkInAlertDays           = (d["checkInAlertDays"]           as? Long)?.toInt() ?: 5,
                    trialDays                  = (d["trialDays"]                  as? Long)?.toInt() ?: 14
                )
                callback(remoteConfig)
            }

    fun listenFeatureFlags(callback: (Map<String, Boolean>) -> Unit): ListenerRegistration =
        db.collection("admin_config").document("flags")
            .addSnapshotListener { snap, err ->
                if (err != null || snap == null || !snap.exists()) { callback(emptyMap()); return@addSnapshotListener }
                val flags = snap.data?.mapValues { (_, v) -> v as? Boolean ?: true } ?: emptyMap()
                callback(flags)
            }

    // Live watch on this user's record — detects admin suspension and plan changes in real time
    fun listenCurrentUser(uid: String, callback: (suspended: Boolean, plan: String) -> Unit): ListenerRegistration =
        db.collection("user_records").document(uid)
            .addSnapshotListener { snap, err ->
                if (err != null || snap == null) return@addSnapshotListener
                val suspended = snap.data?.get("suspended") as? Boolean ?: false
                val plan      = snap.data?.get("plan")      as? String  ?: "STARTER"
                callback(suspended, plan)
            }

    // Legacy one-shot fetch — kept for call sites not yet migrated to listeners
    suspend fun fetch() {
        try {
            val ctrlDoc = db.collection("admin_config").document("app_control").get().await()
            ctrlDoc.data?.let { d ->
                appControl = AppControlState(
                    maintenanceMode     = d["maintenanceMode"]     as? Boolean ?: false,
                    maintenanceMessage  = d["maintenanceMessage"]  as? String  ?: appControl.maintenanceMessage,
                    forceUpdate         = d["forceUpdate"]         as? Boolean ?: false,
                    minVersion          = d["minVersion"]          as? String  ?: "1.0.0",
                    updateMessage       = d["updateMessage"]       as? String  ?: appControl.updateMessage,
                    announcementEnabled = d["announcementEnabled"] as? Boolean ?: false,
                    announcementText    = d["announcementText"]    as? String  ?: "",
                    announcementType    = d["announcementType"]    as? String  ?: "info",
                    latestVersionCode     = (d["latestVersionCode"]     as? Long)?.toInt() ?: 0,
                    updateNudgeEnabled    = d["updateNudgeEnabled"]     as? Boolean ?: false,
                    compulsoryUpdateAfter = (d["compulsoryUpdateAfter"] as? Long)?.toInt() ?: 4
                )
            }
            val rcDoc = db.collection("admin_config").document("remote_config").get().await()
            rcDoc.data?.let { d ->
                remoteConfig = RemoteConfig(
                    maxClientsStarter          = (d["maxClientsStarter"]          as? Long)?.toInt() ?: 5,
                    maxClientsPro              = (d["maxClientsPro"]              as? Long)?.toInt() ?: 20,
                    maxClientsBusiness         = (d["maxClientsBusiness"]         as? Long)?.toInt() ?: 0,
                    consistencyRedThreshold    = (d["consistencyRedThreshold"]    as? Long)?.toInt() ?: 40,
                    consistencyYellowThreshold = (d["consistencyYellowThreshold"] as? Long)?.toInt() ?: 70,
                    checkInAlertDays           = (d["checkInAlertDays"]           as? Long)?.toInt() ?: 5,
                    trialDays                  = (d["trialDays"]                  as? Long)?.toInt() ?: 14
                )
            }
        } catch (_: Exception) { }
    }

    // Returns true if the current user is suspended
    suspend fun isCurrentUserSuspended(): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        return try {
            val doc = db.collection("user_records").document(uid).get().await()
            doc.data?.get("suspended") as? Boolean ?: false
        } catch (_: Exception) { false }
    }
}

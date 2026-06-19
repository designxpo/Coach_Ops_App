package com.example.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

object ChatSync {

    private val db  = FirebaseFirestore.getInstance()
    private val uid get() = FirebaseAuth.getInstance().currentUser?.uid

    // Chat ID is deterministic — both parties can compute it independently
    fun chatId(coachId: String, memberId: String) = "${coachId}_${memberId}"

    // ─── Thread management ────────────────────────────────────────────────────

    suspend fun getOrCreateThread(
        coachId: String, coachName: String,
        memberId: String, memberName: String,
        memberPhone: String = ""
    ): String {
        val id  = chatId(coachId, memberId)
        val ref = db.collection("chats").document(id)
        val snap = ref.get().await()
        if (!snap.exists()) {
            ref.set(mapOf(
                "coachId"       to coachId,
                "memberId"      to memberId,
                "coachName"     to coachName,
                "memberName"    to memberName,
                "memberPhone"   to memberPhone,
                "lastMessage"   to "",
                "lastMessageAt" to 0L,
                "unreadCount"   to 0
            )).await()
        }
        return id
    }

    // ─── Send message ─────────────────────────────────────────────────────────

    suspend fun sendMessage(
        chatId: String,
        senderId: String,
        senderName: String,
        text: String,
        type: String = "text"
    ) {
        val now   = System.currentTimeMillis()
        val msgId = now.toString()
        db.collection("chats").document(chatId)
            .collection("messages").document(msgId)
            .set(mapOf(
                "id"         to msgId,
                "senderId"   to senderId,
                "senderName" to senderName,
                "text"       to text,
                "timestamp"  to now,
                "type"       to type,
                "read"       to false
            )).await()

        // chatId = "{coachId}_{memberId}" — determine who is the RECIPIENT
        // and increment their unread counter using atomic increment
        val parts    = chatId.split("_")
        val coachId  = parts.getOrElse(0) { "" }
        val isCoachSending = senderId == coachId
        val unreadField = if (isCoachSending) "unreadMember" else "unreadCoach"

        db.collection("chats").document(chatId).set(
            mapOf(
                "lastMessage"   to text,
                "lastMessageAt" to now,
                unreadField     to com.google.firebase.firestore.FieldValue.increment(1)
            ),
            SetOptions.merge()
        ).await()
    }

    suspend fun markRead(chatId: String, isCoach: Boolean) {
        // Reset only the current user's unread counter
        val field = if (isCoach) "unreadCoach" else "unreadMember"
        try {
            db.collection("chats").document(chatId)
                .set(mapOf(field to 0), SetOptions.merge()).await()
        } catch (_: Exception) { }
    }

    // ─── Real-time listeners ──────────────────────────────────────────────────

    fun listenMessages(
        chatId: String,
        onMessages: (List<ChatMessage>) -> Unit
    ): ListenerRegistration =
        db.collection("chats").document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) { Log.e("ProCoach", "Chat messages error: ${err.message}"); return@addSnapshotListener }
                val list = snap?.documents?.mapNotNull { doc ->
                    val d = doc.data ?: return@mapNotNull null
                    ChatMessage(
                        id         = d["id"] as? String ?: doc.id,
                        senderId   = d["senderId"] as? String ?: "",
                        senderName = d["senderName"] as? String ?: "",
                        text       = d["text"] as? String ?: "",
                        timestamp  = d["timestamp"] as? Long ?: 0L,
                        type       = d["type"] as? String ?: "text",
                        read       = d["read"] as? Boolean ?: false
                    )
                } ?: emptyList()
                onMessages(list)
            }

    fun listenCoachThreads(
        coachId: String,
        onThreads: (List<ChatThread>) -> Unit
    ): ListenerRegistration =
        // No orderBy — avoids composite index requirement; sorted client-side
        db.collection("chats")
            .whereEqualTo("coachId", coachId)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    Log.e("ProCoach", "Coach threads error: ${err.message}")
                    return@addSnapshotListener
                }
                val sorted = (snap?.toThreads() ?: emptyList())
                    .sortedByDescending { it.lastMessageAt }
                onThreads(sorted)
            }

    fun listenMemberThread(
        memberId: String,
        onThreads: (List<ChatThread>) -> Unit
    ): ListenerRegistration =
        // No orderBy — avoids composite index requirement; sorted client-side
        db.collection("chats")
            .whereEqualTo("memberId", memberId)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    Log.e("ProCoach", "Member threads error: ${err.message}")
                    return@addSnapshotListener
                }
                val sorted = (snap?.toThreads() ?: emptyList())
                    .sortedByDescending { it.lastMessageAt }
                onThreads(sorted)
            }

    private fun com.google.firebase.firestore.QuerySnapshot.toThreads(): List<ChatThread> =
        documents.mapNotNull { doc ->
            val d = doc.data ?: return@mapNotNull null
            ChatThread(
                id            = doc.id,
                coachId       = d["coachId"] as? String ?: "",
                memberId      = d["memberId"] as? String ?: "",
                coachName     = d["coachName"] as? String ?: "",
                memberName    = d["memberName"] as? String ?: "",
                memberPhone   = d["memberPhone"] as? String ?: "",
                lastMessage   = d["lastMessage"] as? String ?: "",
                lastMessageAt = d["lastMessageAt"] as? Long ?: 0L,
                unreadCoach  = (d["unreadCoach"]  as? Long)?.toInt() ?: 0,
                unreadMember = (d["unreadMember"] as? Long)?.toInt() ?: 0
            )
        }
}

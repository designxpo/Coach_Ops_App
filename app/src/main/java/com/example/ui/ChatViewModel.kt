package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.ChatMessage
import com.example.data.ChatSync
import com.example.data.ChatThread
import com.example.data.UserPreferences
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChatViewModel(private val userPreferences: UserPreferences) : ViewModel() {

    private val myUid  get() = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""
    private val myName get() = if (isCoach)
        userPreferences.coachName.ifEmpty { "Coach" }
    else
        userPreferences.clientName.ifEmpty { userPreferences.coachName.ifEmpty { "Member" } }
    val isCoach        get() = userPreferences.userRole == "coach"

    // ─── Thread list ──────────────────────────────────────────────────────────
    private val _threads = MutableStateFlow<List<ChatThread>>(emptyList())
    val threads: StateFlow<List<ChatThread>> = _threads.asStateFlow()

    private var threadsListener: ListenerRegistration? = null
    private var listeningStarted = false

    fun startListeningThreads() {
        if (listeningStarted) return
        listeningStarted = true
        threadsListener?.remove()
        val uid = myUid
        if (uid.isEmpty()) { listeningStarted = false; return }
        try {
            threadsListener = if (isCoach) {
                ChatSync.listenCoachThreads(uid) { _threads.value = it }
            } else {
                ChatSync.listenMemberThread(uid) { _threads.value = it }
            }
        } catch (e: Exception) {
            Log.e("ProCoach", "startListeningThreads failed: ${e.message}", e)
            listeningStarted = false
        }
    }

    fun restartListeningThreads() {
        listeningStarted = false
        startListeningThreads()
    }

    // ─── Individual chat ──────────────────────────────────────────────────────
    private val _messages  = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    private var messagesListener: ListenerRegistration? = null
    private var activeChatId: String = ""

    fun openChat(chatId: String) {
        if (chatId.isEmpty()) return
        if (activeChatId == chatId) return
        activeChatId = chatId
        messagesListener?.remove()
        try {
            messagesListener = ChatSync.listenMessages(chatId) { _messages.value = it }
        } catch (e: Exception) {
            Log.e("ProCoach", "listenMessages failed: ${e.message}", e)
        }
        viewModelScope.launch {
            try { ChatSync.markRead(chatId, isCoach) } catch (e: Exception) {
                Log.e("ProCoach", "markRead failed: ${e.message}", e)
            }
        }
    }

    /** Total unread for the current user's role */
    val totalUnread: Int get() = if (isCoach)
        _threads.value.sumOf { it.unreadCoach }
    else
        _threads.value.sumOf { it.unreadMember }

    /** Coach opens a chat with a member — creates thread if needed */
    fun openOrCreateChat(
        memberId: String, memberName: String, memberPhone: String,
        onReady: (chatId: String) -> Unit
    ) {
        val coachId   = myUid
        val coachName = myName
        if (coachId.isEmpty()) { Log.e("ProCoach", "openOrCreateChat: not authenticated"); return }
        viewModelScope.launch {
            try {
                val chatId = ChatSync.getOrCreateThread(coachId, coachName, memberId, memberName, memberPhone)
                openChat(chatId)
                onReady(chatId)
            } catch (e: Exception) {
                Log.e("ProCoach", "openOrCreateChat failed: ${e.message}", e)
            }
        }
    }

    /** Member opens a chat with their coach — creates thread if needed */
    fun openOrCreateChatAsMember(
        coachId: String, coachName: String,
        onReady: (chatId: String) -> Unit
    ) {
        val memberId   = myUid
        val memberName = myName
        if (memberId.isEmpty()) { Log.e("ProCoach", "openOrCreateChatAsMember: not authenticated"); return }
        viewModelScope.launch {
            try {
                val chatId = ChatSync.getOrCreateThread(coachId, coachName, memberId, memberName)
                openChat(chatId)
                onReady(chatId)
            } catch (e: Exception) {
                Log.e("ProCoach", "openOrCreateChatAsMember failed: ${e.message}", e)
            }
        }
    }

    fun sendMessage(text: String, type: String = "text") {
        val chatId = activeChatId
        if (chatId.isEmpty() || text.isBlank()) return
        _isSending.value = true
        viewModelScope.launch {
            try {
                ChatSync.sendMessage(chatId, myUid, myName, text, type)
            } catch (e: Exception) {
                Log.e("ProCoach", "sendMessage failed: ${e.message}", e)
            } finally {
                _isSending.value = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        messagesListener?.remove()
        threadsListener?.remove()
    }
}

class ChatViewModelFactory(private val prefs: UserPreferences) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>) =
        ChatViewModel(prefs) as T
}

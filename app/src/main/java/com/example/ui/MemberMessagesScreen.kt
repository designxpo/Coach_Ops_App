package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Booking
import com.example.data.ChatThread
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date

/**
 * Member's messages hub.
 * - Shows existing chat threads (real-time)
 * - Shows coaches from bookings that don't have a thread yet (so member can initiate)
 * - Only coaches the member has booked appear here — no random coaches
 */
@Composable
fun MemberMessagesScreen(
    chatViewModel: ChatViewModel,
    bookings: List<Booking>,
    onOpenChat: (chatId: String, coachName: String) -> Unit,
    onStartChat: (coachId: String, coachName: String) -> Unit
) {
    val threads by chatViewModel.threads.collectAsState()

    LaunchedEffect(Unit) { chatViewModel.restartListeningThreads() }

    // Coaches from bookings who don't yet have a thread
    val threadCoachIds = remember(threads) { threads.map { it.coachId }.toSet() }
    val bookedCoachesWithoutThread = remember(bookings, threadCoachIds) {
        bookings
            .distinctBy { it.coachId }
            .filter { it.coachId.isNotEmpty() && it.coachId !in threadCoachIds }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(CyberBgPrimary).statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        item {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(40.dp).clip(CircleShape).background(CyberAccent.copy(0.15f)),
                    contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.ChatBubbleOutline, null, tint = CyberAccent, modifier = Modifier.size(20.dp))
                }
                Column {
                    Text("Messages", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = CyberTextPrimary)
                    Text("Chat with your coach", fontSize = 12.sp, color = CyberTextMuted)
                }
            }
        }

        // ── Existing conversations ────────────────────────────────────────────
        if (threads.isNotEmpty()) {
            item {
                Text("Conversations", fontSize = 13.sp, fontWeight = FontWeight.Bold,
                    color = CyberTextMuted, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
            }
            items(threads, key = { it.id }) { thread ->
                MemberThreadRow(
                    thread = thread,
                    onClick = { onOpenChat(thread.id, thread.coachName) }
                )
            }
        }

        // ── Start chat with booked coaches ────────────────────────────────────
        if (bookedCoachesWithoutThread.isNotEmpty()) {
            item {
                Spacer(Modifier.height(8.dp))
                Text("Your Coaches", fontSize = 13.sp, fontWeight = FontWeight.Bold,
                    color = CyberTextMuted, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
            }
            items(bookedCoachesWithoutThread, key = { it.coachId }) { booking ->
                StartChatRow(
                    coachName = booking.coachName,
                    onClick   = { onStartChat(booking.coachId, booking.coachName) }
                )
            }
        }

        // ── Empty state ───────────────────────────────────────────────────────
        if (threads.isEmpty() && bookedCoachesWithoutThread.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("💬", fontSize = 56.sp)
                        Text("No messages yet", fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold, color = CyberTextPrimary)
                        Text("Book a coach from Discover to start chatting",
                            fontSize = 13.sp, color = CyberTextMuted)
                    }
                }
            }
        }
    }
}

// ── Existing thread row ───────────────────────────────────────────────────────

@Composable
private fun MemberThreadRow(thread: ChatThread, onClick: () -> Unit) {
    val timeFmt = SimpleDateFormat(
        if (System.currentTimeMillis() - thread.lastMessageAt < 86400000) "h:mm a" else "d MMM",
        java.util.Locale.getDefault()
    )
    Row(modifier = Modifier.fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 4.dp)
        .clip(RoundedCornerShape(16.dp))
        .background(CyberBgCard)
        .border(1.dp, Color.White.copy(0.05f), RoundedCornerShape(16.dp))
        .clickable { onClick() }
        .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(Modifier.size(48.dp).clip(CircleShape).background(CyberAccent.copy(0.15f)),
            contentAlignment = Alignment.Center) {
            Text(thread.coachName.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = CyberAccent)
        }
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(thread.coachName, fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold, color = CyberTextPrimary)
                Box(Modifier.clip(RoundedCornerShape(4.dp))
                    .background(CyberAccent.copy(0.15f))
                    .padding(horizontal = 5.dp, vertical = 1.dp)) {
                    Text("Coach", fontSize = 9.sp, color = CyberAccent, fontWeight = FontWeight.Bold)
                }
            }
            Text(thread.lastMessage.ifEmpty { "Start the conversation" },
                fontSize = 12.sp, color = CyberTextMuted,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (thread.lastMessageAt > 0L) {
                Text(timeFmt.format(Date(thread.lastMessageAt)), fontSize = 10.sp, color = CyberTextMuted)
            }
            if (thread.unreadMember > 0) {
                Box(Modifier.size(18.dp).clip(CircleShape).background(CyberAccent),
                    contentAlignment = Alignment.Center) {
                    Text(thread.unreadMember.coerceAtMost(99).toString(),
                        fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = CyberAccentDark)
                }
            }
        }
    }
}

// ── Start chat row (coach with no existing thread) ────────────────────────────

@Composable
private fun StartChatRow(coachName: String, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 4.dp)
        .clip(RoundedCornerShape(16.dp))
        .background(CyberBgCard)
        .border(1.dp, CyberAccent.copy(0.2f), RoundedCornerShape(16.dp))
        .clickable { onClick() }
        .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(Modifier.size(48.dp).clip(CircleShape).background(CyberAccent.copy(0.1f)),
            contentAlignment = Alignment.Center) {
            Text(coachName.firstOrNull()?.toString() ?: "?",
                fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = CyberAccent)
        }
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(coachName, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = CyberTextPrimary)
                Box(Modifier.clip(RoundedCornerShape(4.dp))
                    .background(CyberAccent.copy(0.15f))
                    .padding(horizontal = 5.dp, vertical = 1.dp)) {
                    Text("Coach", fontSize = 9.sp, color = CyberAccent, fontWeight = FontWeight.Bold)
                }
            }
            Text("Tap to start a conversation", fontSize = 12.sp, color = CyberTextMuted)
        }
        Box(Modifier.size(36.dp).clip(CircleShape).background(CyberAccent),
            contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Add, null, tint = CyberAccentDark, modifier = Modifier.size(16.dp))
        }
    }
}

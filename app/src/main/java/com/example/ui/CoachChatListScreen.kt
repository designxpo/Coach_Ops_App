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
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ChatThread
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date

@Composable
fun CoachChatListScreen(
    viewModel: ChatViewModel,
    onOpenChat: (chatId: String, memberName: String, memberPhone: String) -> Unit
) {
    val threads by viewModel.threads.collectAsState()

    LaunchedEffect(Unit) { viewModel.restartListeningThreads() }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(CyberBgPrimary).statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
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
                    Text("${threads.size} conversation${if (threads.size != 1) "s" else ""}", fontSize = 12.sp, color = CyberTextMuted)
                }
            }
        }

        if (threads.isEmpty()) {
            item {
                Box(Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("💬", fontSize = 48.sp)
                        Text("No conversations yet", fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold, color = CyberTextPrimary)
                        Text("Tap the chat icon on any member to start", fontSize = 12.sp, color = CyberTextMuted)
                    }
                }
            }
        }

        items(threads, key = { it.id }) { thread ->
            ThreadRow(thread = thread, onClick = {
                onOpenChat(
                    thread.id,
                    thread.memberName.ifEmpty { "Member" },
                    thread.memberPhone
                )
            })
        }
    }
}

// ── Thread list row ───────────────────────────────────────────────────────────

@Composable
private fun ThreadRow(thread: ChatThread, onClick: () -> Unit) {
    val timeFmt = remember(thread.lastMessageAt) {
        SimpleDateFormat(
            if (System.currentTimeMillis() - thread.lastMessageAt < 86400000) "h:mm a" else "d MMM",
            java.util.Locale.getDefault()
        )
    }
    Row(
        modifier = Modifier.fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(CyberBgCard)
            .border(1.dp, Color.White.copy(0.05f), RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Avatar
        Box(Modifier.size(48.dp).clip(CircleShape).background(CyberAccent.copy(0.15f)),
            contentAlignment = Alignment.Center) {
            Text(thread.memberName.firstOrNull()?.uppercaseChar()?.toString() ?: "M",
                fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = CyberAccent)
        }

        Column(Modifier.weight(1f)) {
            Text(thread.memberName.ifEmpty { "Member" }, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = CyberTextPrimary)
            Text(
                thread.lastMessage.ifEmpty { "No messages yet" },
                fontSize = 12.sp, color = CyberTextMuted,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }

        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (thread.lastMessageAt > 0L) {
                Text(timeFmt.format(Date(thread.lastMessageAt)), fontSize = 10.sp, color = CyberTextMuted)
            }
            if (thread.unreadCoach > 0) {
                val count = thread.unreadCoach
                Box(Modifier.size(18.dp).clip(CircleShape).background(CyberAccent),
                    contentAlignment = Alignment.Center) {
                    Text(
                        text = if (count > 99) "99+" else "$count",
                        fontSize = if (count >= 10) 7.sp else 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = CyberAccentDark
                    )
                }
            }
        }
    }
}

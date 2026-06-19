package com.example.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.ChatMessage
import com.example.data.REMINDER_TEMPLATES
import com.example.data.ReminderTemplate
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date

@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    chatId: String,
    otherName: String,
    otherPhone: String = "",
    onBack: () -> Unit
) {
    val messages  by viewModel.messages.collectAsState()
    val isSending by viewModel.isSending.collectAsState()
    val context   = LocalContext.current
    val myUid     = remember { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: "" }
    val isCoach   = viewModel.isCoach

    var inputText       by remember { mutableStateOf("") }
    var showReminders   by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    LaunchedEffect(chatId) { viewModel.openChat(chatId) }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    if (showReminders) {
        ReminderSheet(
            memberName  = otherName,
            memberPhone = otherPhone,
            context     = context,
            onSendInApp = { text ->
                viewModel.sendMessage(text, "reminder")
                showReminders = false
            },
            onDismiss = { showReminders = false }
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(CyberBgPrimary).statusBarsPadding()) {
        // ── Top bar ───────────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth()
                .background(CyberBgCard)
                .border(width = 1.dp, color = Color.White.copy(0.06f),
                    shape = RoundedCornerShape(bottomStart = 0.dp, bottomEnd = 0.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(Modifier.size(38.dp).clip(CircleShape).background(CyberBgCardElevated)
                .clickable { onBack() }, contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = CyberTextPrimary, modifier = Modifier.size(18.dp))
            }
            // Avatar — show initial letter; fall back to "M" for member, "C" for coach
            val displayName    = otherName.ifEmpty { if (isCoach) "Member" else "Coach" }
            val displayInitial = displayName.first().uppercaseChar().toString()
            Box(Modifier.size(38.dp).clip(CircleShape).background(CyberAccent.copy(0.15f)),
                contentAlignment = Alignment.Center) {
                Text(displayInitial, fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold, color = CyberAccent)
            }
            Column(Modifier.weight(1f)) {
                Text(displayName, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = CyberTextPrimary)
                Text(if (isCoach) "Member" else "Your Coach", fontSize = 11.sp, color = CyberTextMuted)
            }
            if (isCoach) {
                Box(Modifier.size(38.dp).clip(CircleShape).background(Color(0xFFF59E0B).copy(0.15f))
                    .clickable { showReminders = true }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Notifications, null, tint = Color(0xFFF59E0B), modifier = Modifier.size(18.dp))
                }
            }
        }

        // ── Messages ──────────────────────────────────────────────────────────
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            var lastDate = ""
            items(messages, key = { it.id }) { msg ->
                val fmt = SimpleDateFormat("d MMM yyyy", java.util.Locale.getDefault())
                val date = fmt.format(Date(msg.timestamp))
                if (date != lastDate) {
                    lastDate = date
                    Box(Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center) {
                        Box(Modifier.clip(RoundedCornerShape(20.dp)).background(CyberBgCard)
                            .padding(horizontal = 12.dp, vertical = 4.dp)) {
                            Text(date, fontSize = 11.sp, color = CyberTextMuted)
                        }
                    }
                }
                MessageBubble(msg = msg, isMe = msg.senderId == myUid)
            }
        }

        // ── Input bar ─────────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth()
                .background(CyberBgCard)
                .border(width = 1.dp, color = Color.White.copy(0.06f),
                    shape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp))
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            BasicTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(20.dp))
                    .background(CyberBgCardElevated)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                textStyle = TextStyle(color = CyberTextPrimary, fontSize = 14.sp),
                cursorBrush = SolidColor(CyberAccent),
                maxLines = 4,
                decorationBox = { inner ->
                    if (inputText.isEmpty())
                        Text("Type a message…", fontSize = 14.sp, color = CyberTextMuted)
                    inner()
                }
            )
            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape)
                    .background(if (inputText.isBlank() || isSending) CyberBgCardElevated else CyberAccent)
                    .clickable(enabled = inputText.isNotBlank() && !isSending) {
                        viewModel.sendMessage(inputText.trim())
                        inputText = ""
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isSending) CircularProgressIndicator(color = CyberAccent, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                else Icon(Icons.AutoMirrored.Filled.Send, null,
                    tint = if (inputText.isBlank()) CyberTextMuted else CyberAccentDark,
                    modifier = Modifier.size(18.dp))
            }
        }
    }
}

// ── Message bubble ────────────────────────────────────────────────────────────

@Composable
private fun MessageBubble(msg: ChatMessage, isMe: Boolean) {
    val timeFmt = remember { SimpleDateFormat("h:mm a", java.util.Locale.getDefault()) }
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
    ) {
        if (msg.type == "reminder") {
            // Reminder pill
            Row(
                modifier = Modifier.clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFF59E0B).copy(0.15f))
                    .border(1.dp, Color(0xFFF59E0B).copy(0.3f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text("🔔", fontSize = 14.sp)
                Column {
                    Text("Reminder from Coach", fontSize = 10.sp, color = Color(0xFFF59E0B),
                        fontWeight = FontWeight.Bold)
                    Text(msg.text, fontSize = 13.sp, color = CyberTextPrimary, lineHeight = 18.sp)
                    Text(timeFmt.format(Date(msg.timestamp)), fontSize = 10.sp, color = CyberTextMuted,
                        modifier = Modifier.padding(top = 4.dp))
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .clip(RoundedCornerShape(
                        topStart = if (isMe) 18.dp else 4.dp,
                        topEnd = if (isMe) 4.dp else 18.dp,
                        bottomStart = 18.dp, bottomEnd = 18.dp
                    ))
                    .background(if (isMe) CyberAccent else CyberBgCard)
                    .border(if (isMe) 0.dp else 1.dp, Color.White.copy(0.07f), RoundedCornerShape(18.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Column {
                    Text(msg.text, fontSize = 14.sp, color = if (isMe) CyberAccentDark else CyberTextPrimary,
                        lineHeight = 20.sp)
                    Text(timeFmt.format(Date(msg.timestamp)), fontSize = 10.sp,
                        color = if (isMe) CyberAccentDark.copy(0.6f) else CyberTextMuted,
                        modifier = Modifier.align(Alignment.End).padding(top = 3.dp))
                }
            }
        }
    }
}

// ── Reminder sheet ────────────────────────────────────────────────────────────

@Composable
fun ReminderSheet(
    memberName: String,
    memberPhone: String,
    context: android.content.Context,
    onSendInApp: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var selected by remember { mutableStateOf<ReminderTemplate?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(CyberBgCard)
                .border(1.dp, Color.White.copy(0.08f), RoundedCornerShape(24.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Send Reminder to $memberName", fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold, color = CyberTextPrimary)
            Text("Select a template or send via WhatsApp", fontSize = 12.sp, color = CyberTextMuted)

            REMINDER_TEMPLATES.forEach { template ->
                val isSelected = selected == template
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) CyberAccent.copy(0.12f) else CyberBgCardElevated)
                        .border(1.dp,
                            if (isSelected) CyberAccent.copy(0.5f) else Color.White.copy(0.06f),
                            RoundedCornerShape(12.dp))
                        .clickable { selected = template }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(template.emoji, fontSize = 20.sp)
                    Column(Modifier.weight(1f)) {
                        Text(template.title, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                            color = if (isSelected) CyberAccent else CyberTextPrimary)
                        Text(template.body.replace("{name}", memberName).take(60) + "…",
                            fontSize = 11.sp, color = CyberTextMuted)
                    }
                    if (isSelected) Box(Modifier.size(18.dp).clip(CircleShape).background(CyberAccent),
                        contentAlignment = Alignment.Center) {
                        Text("✓", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = CyberAccentDark)
                    }
                }
            }

            if (selected != null) {
                val msg = selected!!.body.replace("{name}", memberName).replace("{water}", "3")
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    // In-app send
                    Box(modifier = Modifier.weight(1f).height(48.dp)
                        .clip(RoundedCornerShape(14.dp)).background(CyberAccent)
                        .clickable { onSendInApp(msg) },
                        contentAlignment = Alignment.Center) {
                        Text("Send in App", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold,
                            color = CyberAccentDark)
                    }
                    // WhatsApp
                    Box(modifier = Modifier.weight(1f).height(48.dp)
                        .clip(RoundedCornerShape(14.dp)).background(Color(0xFF25D366))
                        .clickable {
                            val phone = memberPhone.filter { it.isDigit() }
                            val fullPhone = if (phone.startsWith("91") && phone.length >= 12) phone
                                           else if (phone.length == 10) "91$phone"
                                           else phone
                            val encoded = java.net.URLEncoder.encode(msg, "UTF-8")
                            val uri = Uri.parse("https://wa.me/$fullPhone?text=$encoded")
                            context.startActivity(Intent(Intent.ACTION_VIEW, uri).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            })
                            onDismiss()
                        },
                        contentAlignment = Alignment.Center) {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("WhatsApp", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold,
                                color = Color.White)
                        }
                    }
                }
            }

            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("Cancel", color = CyberTextMuted, fontSize = 13.sp)
            }
        }
    }
}

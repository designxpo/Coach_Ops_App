package com.example.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import com.example.data.Client
import com.example.data.Signal
import com.example.ui.theme.CyberAccent
import com.example.ui.theme.CyberAccentDark
import com.example.ui.theme.CyberBgCard
import com.example.ui.theme.CyberBgCardElevated
import com.example.ui.theme.CyberBgPrimary
import com.example.ui.theme.CyberDanger
import com.example.ui.theme.CyberSuccess
import com.example.ui.theme.CyberTextMuted
import com.example.ui.theme.CyberTextPrimary
import com.example.ui.theme.CyberTextSecondary
import com.example.ui.theme.CyberWarning

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onProfileClick: () -> Unit = {},
    onClientClick: (String) -> Unit = {},
    onChatClick: () -> Unit = {},
    onLibraryClick: () -> Unit = {},
    chatUnreadCount: Int = 0,
    showGym: Boolean = false,
    onGymClick: () -> Unit = {}
) {
    val signals by viewModel.signals.collectAsStateWithLifecycle()
    val clients by viewModel.clients.collectAsStateWithLifecycle()
    val programs by viewModel.programs.collectAsStateWithLifecycle()
    val payments by viewModel.payments.collectAsStateWithLifecycle()
    val coachName by viewModel.coachName.collectAsStateWithLifecycle()
    val businessMetrics by viewModel.businessMetrics.collectAsStateWithLifecycle()

    var showNotificationsSheet by remember { mutableStateOf(false) }
    var showBroadcastSheet by remember { mutableStateOf(false) }

    if (showNotificationsSheet) {
        NotificationsSheet(
            signals = signals,
            clients = clients,
            programs = programs,
            onDismiss = { showNotificationsSheet = false },
            onResolve = { viewModel.resolveSignal(it) }
        )
    }

    if (showBroadcastSheet) {
        BroadcastSheet(
            clients = clients.filter { it.status == "Active" },
            onDismiss = { showBroadcastSheet = false }
        )
    }

    val rosterHealth = if (clients.isNotEmpty()) clients.map { it.consistencyScore }.average().toInt() else 0
    val healthLabel = when {
        rosterHealth >= 80 -> "Your roster is healthy"
        rosterHealth >= 60 -> "Needs some attention"
        else -> "Roster at risk"
    }
    val totalMrr = clients.sumOf { it.mrr }
    val mrrDisplay = when {
        totalMrr >= 100000 -> "₹%.2fL".format(totalMrr / 100000f)
        totalMrr >= 1000 -> "₹${totalMrr / 1000}K"
        else -> "₹$totalMrr"
    }
    val activePaymentCount = payments.count { it.mandateStatus == "ACTIVE" }

    val hour = remember { java.util.Calendar.getInstance() }.get(java.util.Calendar.HOUR_OF_DAY)
    val greeting = when {
        hour < 12 -> "Good morning"
        hour < 17 -> "Good afternoon"
        else -> "Good evening"
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(CyberBgPrimary),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    val photoUrl by viewModel.profilePhotoUrl.collectAsState()
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .border(2.dp, CyberAccent, CircleShape)
                            .clip(CircleShape)
                            .background(CyberBgCardElevated)
                            .clickable { onProfileClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        if (photoUrl.isNotBlank()) {
                            AsyncImage(
                                model = photoUrl,
                                contentDescription = "Profile",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(44.dp).clip(CircleShape)
                            )
                        } else {
                            Text(
                                (coachName.firstOrNull()?.toString() ?: "C").uppercase(),
                                fontSize = 16.sp, fontWeight = FontWeight.Bold, color = CyberAccent
                            )
                        }
                    }
                    Column {
                        Text(greeting, fontSize = 12.sp, color = CyberTextSecondary, fontWeight = FontWeight.Normal)
                        Text(coachName.ifEmpty { "Coach" }, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = CyberTextPrimary)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(modifier = Modifier.size(8.dp).background(CyberSuccess, CircleShape))

                    // Gym Suite entry — lives in the top bar (bottom nav stays 5 tabs)
                    if (showGym) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clickable { onGymClick() }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .align(Alignment.Center)
                                    .clip(CircleShape)
                                    .background(CyberAccent.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.Storefront,
                                    contentDescription = "My Gym",
                                    tint = CyberAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    // Chat icon with unread badge
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clickable { onChatClick() }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .align(Alignment.Center)
                                .clip(CircleShape)
                                .background(CyberBgCard),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.ChatBubbleOutline, null, tint = CyberTextPrimary, modifier = Modifier.size(20.dp))
                        }
                        if (chatUnreadCount > 0) {
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .align(Alignment.TopEnd)
                                    .background(CyberAccent, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    if (chatUnreadCount > 9) "9+" else "$chatUnreadCount",
                                    style = TextStyle(
                                        fontSize = 8.sp, lineHeight = 8.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = CyberAccentDark, textAlign = TextAlign.Center,
                                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                                    )
                                )
                            }
                        }
                    }

                    // Notification bell — outer box is 44dp so the 18dp badge can overflow the 36dp bell
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clickable { showNotificationsSheet = true }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .align(Alignment.Center)
                                .clip(CircleShape)
                                .background(CyberBgCard),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Notifications, contentDescription = null, tint = CyberTextPrimary, modifier = Modifier.size(20.dp))
                        }
                        if (signals.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .align(Alignment.TopEnd)
                                    .background(CyberDanger, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    if (signals.size > 9) "9+" else "${signals.size}",
                                    style = TextStyle(
                                        fontSize = 8.sp,
                                        lineHeight = 8.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White,
                                        textAlign = TextAlign.Center,
                                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // Roster Health
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(CyberAccent)
                    .padding(20.dp)
            ) {
                Column {
                    Text("ROSTER HEALTH", fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF0A0A0A).copy(alpha = 0.5f), letterSpacing = 0.8.sp)
                    Text("$rosterHealth%", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold,
                        color = CyberAccentDark, lineHeight = 36.sp)
                    Text(healthLabel, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF0A0A0A).copy(alpha = 0.6f))
                }
                Box(
                    modifier = Modifier.size(44.dp).clip(CircleShape).background(Color(0x2A000000)).align(Alignment.BottomEnd),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.SupervisorAccount, contentDescription = null, tint = Color(0xFF0A0A0A), modifier = Modifier.size(22.dp))
                }
            }
        }

        // Stat tiles — no fixed height so text never clips
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(28.dp))
                        .background(CyberBgCard)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier.size(32.dp).clip(CircleShape).background(CyberAccent.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.CheckCircle, null, tint = CyberAccent, modifier = Modifier.size(16.dp))
                    }
                    Column {
                        Text("TODAY'S TRIAGE", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = CyberTextMuted, letterSpacing = 0.5.sp)
                        Text("${signals.size}", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = CyberTextPrimary, lineHeight = 28.sp)
                        Text("cards pending", fontSize = 11.sp, color = CyberTextMuted)
                    }
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(28.dp))
                        .background(CyberBgCard)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier.size(32.dp).clip(CircleShape).background(CyberAccent.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.AttachMoney, null, tint = CyberAccent, modifier = Modifier.size(16.dp))
                    }
                    Column {
                        Text("MRR", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = CyberTextMuted, letterSpacing = 0.5.sp)
                        Text(mrrDisplay, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = CyberTextPrimary, lineHeight = 26.sp)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            Icon(Icons.Filled.ArrowUpward, null, tint = CyberSuccess, modifier = Modifier.size(10.dp))
                            Text(
                                "$activePaymentCount active",
                                fontSize = 11.sp,
                                color = CyberSuccess,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        // Quick Access
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickAccessTile(
                    icon  = androidx.compose.material.icons.Icons.Filled.FitnessCenter,
                    label = "Exercise Library",
                    tint  = CyberAccent,
                    modifier = Modifier.weight(1f),
                    onClick = onLibraryClick
                )
                if (showGym) {
                    QuickAccessTile(
                        icon  = Icons.Filled.Storefront,
                        label = "My Gym",
                        tint  = CyberAccent,
                        modifier = Modifier.weight(1f),
                        onClick = onGymClick
                    )
                }
            }
        }

        // Business Pulse
        item {
            Text("Business Pulse", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = CyberTextPrimary,
                modifier = Modifier.padding(top = 4.dp))
        }
        item {
            val retentionColor = when {
                businessMetrics.retentionRate >= 65 -> CyberSuccess
                businessMetrics.retentionRate >= 40 -> CyberWarning
                else -> CyberDanger
            }
            val atRiskColor = when {
                businessMetrics.atRiskRevenue == 0 -> CyberSuccess
                businessMetrics.atRiskRevenue < 50000 -> CyberWarning
                else -> CyberDanger
            }
            val atRiskDisplay = when {
                businessMetrics.atRiskRevenue >= 100000 -> "₹%.1fL".format(businessMetrics.atRiskRevenue / 100000f)
                businessMetrics.atRiskRevenue >= 1000 -> "₹${businessMetrics.atRiskRevenue / 1000}K"
                else -> "₹${businessMetrics.atRiskRevenue}"
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BusinessPulseTile(
                    modifier = Modifier.weight(1f),
                    label = "RETENTION",
                    value = "${businessMetrics.retentionRate}%",
                    subtitle = "${businessMetrics.retainedCount} of ${clients.size} past Wk 4",
                    valueColor = retentionColor
                )
                BusinessPulseTile(
                    modifier = Modifier.weight(1f),
                    label = "AT RISK MRR",
                    value = if (businessMetrics.atRiskRevenue == 0) "Clear" else atRiskDisplay,
                    subtitle = if (businessMetrics.atRiskCount == 0) "All payments on time" else "${businessMetrics.atRiskCount} member${if (businessMetrics.atRiskCount > 1) "s" else ""} overdue",
                    valueColor = atRiskColor
                )
            }
        }

        // Smart Insights
        if (businessMetrics.insights.isNotEmpty()) {
            item {
                Text("Smart Insights", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = CyberTextPrimary,
                    modifier = Modifier.padding(top = 4.dp))
            }
            items(businessMetrics.insights, key = { it.title }) { insight ->
                InsightCard(insight)
            }
        }

        item {
            Row(
                Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Priority Cards", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = CyberTextPrimary)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${signals.size} signals", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = CyberTextMuted)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(CyberBgCard)
                            .clickable { showBroadcastSheet = true }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Broadcast", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = CyberAccent)
                    }
                }
            }
        }

        if (signals.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                    Text("Queue is clear. Great job!", color = CyberTextSecondary, fontSize = 14.sp)
                }
            }
        }

        items(signals, key = { it.id }) { signal ->
            val client = clients.find { it.id == signal.clientId }
            val program = programs.find { it.id == client?.programId }
            SignalCard(
                signal = signal,
                clientPhone = client?.phoneNumber ?: "",
                programChip = program?.let { "${it.name.take(14)} · W${client?.programWeek ?: 1}" },
                onResolve = { viewModel.resolveSignal(signal.id) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignalCard(
    signal: Signal,
    clientPhone: String,
    programChip: String?,
    onResolve: () -> Unit
) {
    val context = LocalContext.current

    val severityColor = when (signal.severity) {
        "RED" -> CyberDanger
        "YELLOW" -> CyberWarning
        else -> CyberAccent
    }
    val typeChipColor = when (signal.type) {
        "MISSED_WORKOUT", "MISSED_HABIT" -> CyberWarning
        "UPI_EXPIRING", "PAYMENT_OVERDUE" -> CyberDanger
        "FORM_CHECK", "MILESTONE" -> CyberSuccess
        else -> CyberAccent
    }
    val typeLabel = when (signal.type) {
        "MISSED_WORKOUT" -> "Missed Workout"
        "MISSED_HABIT" -> "Habit Streak"
        "UPI_EXPIRING" -> "Payment"
        "PAYMENT_OVERDUE" -> "Overdue"
        "FORM_CHECK" -> "Form Check"
        "MILESTONE" -> "Milestone"
        "MESSAGE" -> "Message"
        else -> signal.type
    }
    val waMessage = when (signal.type) {
        "MISSED_WORKOUT" -> "Hey ${signal.clientName}, noticed you haven't logged a session in a few days. Everything okay? Want me to swap your plan to something lighter this week?"
        "UPI_EXPIRING" -> "Hi ${signal.clientName}, your auto-pay is due to renew soon. I'll send you the renewal link — takes 30 seconds."
        "FORM_CHECK" -> "Got your video, ${signal.clientName} — let me review it tonight and send you a voice note with feedback."
        "PAYMENT_OVERDUE" -> "Hi ${signal.clientName}, just a quick reminder about the pending payment. Let me know if you have any issues."
        "MILESTONE" -> "Amazing work ${signal.clientName}! You just hit a major milestone. Keep it up — this is where the real results show!"
        else -> "Hi ${signal.clientName}, just checking in! Let me know if you need anything."
    }

    fun openWhatsApp() {
        val url = if (clientPhone.isNotEmpty()) {
            val cleanPhone = clientPhone.trimStart('+').removePrefix("91")
            "https://wa.me/91$cleanPhone?text=${Uri.encode(waMessage)}"
        } else "https://wa.me/?text=${Uri.encode(waMessage)}"
        try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } catch (_: Exception) {}
    }

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> { onResolve(); true }
                SwipeToDismissBoxValue.EndToStart -> { openWhatsApp(); false }
                else -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val bgColor = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> CyberSuccess
                SwipeToDismissBoxValue.EndToStart -> Color(0xFF25D366)
                else -> Color.Transparent
            }
            val alignment = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                else -> Alignment.Center
            }
            val icon = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> Icons.Filled.Delete
                SwipeToDismissBoxValue.EndToStart -> Icons.AutoMirrored.Filled.Send
                else -> null
            }
            Box(
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(28.dp))
                    .background(bgColor).padding(horizontal = 20.dp),
                contentAlignment = alignment
            ) {
                if (icon != null) Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
        }
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = CyberBgCard)
        ) {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier.size(40.dp).border(2.dp, CyberAccent, CircleShape)
                        .clip(CircleShape).background(CyberBgCardElevated),
                    contentAlignment = Alignment.Center
                ) {
                    Text(signal.clientName.take(1), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = CyberAccent)
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(modifier = Modifier.size(7.dp).background(severityColor, CircleShape))
                        Text(signal.clientName, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = CyberTextPrimary)
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(signal.description, fontSize = 13.sp, color = CyberTextSecondary, maxLines = 1)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                            modifier = Modifier.clip(RoundedCornerShape(999.dp))
                                .background(typeChipColor.copy(alpha = 0.12f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(typeLabel, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = typeChipColor)
                        }
                        if (programChip != null) {
                            Box(
                                modifier = Modifier.clip(RoundedCornerShape(999.dp))
                                    .background(CyberAccent.copy(alpha = 0.10f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(programChip, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = CyberAccent, maxLines = 1)
                            }
                        }
                    }
                }

                // WhatsApp button — tappable directly
                Box(
                    modifier = Modifier.size(44.dp).clip(CircleShape).background(CyberAccent)
                        .clickable { openWhatsApp() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "WhatsApp", tint = CyberAccentDark, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
private fun QuickAccessTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    subtitle: String = "",
    onClick: () -> Unit
) {
    androidx.compose.foundation.layout.Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(CyberBgCard)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            Modifier.size(36.dp).clip(CircleShape).background(tint.copy(0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(label, color = CyberTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            if (subtitle.isNotBlank())
                Text(subtitle, color = CyberTextMuted, fontSize = 10.sp, lineHeight = 13.sp,
                    maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun BusinessPulseTile(
    modifier: Modifier,
    label: String,
    value: String,
    subtitle: String,
    valueColor: Color
) {
    Column(
        modifier = modifier
            .heightIn(min = 96.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(CyberBgCard)
            .padding(16.dp)
    ) {
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
            color = CyberTextMuted, letterSpacing = 0.5.sp)
        Spacer(Modifier.weight(1f))
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = valueColor, lineHeight = 26.sp)
        Spacer(Modifier.height(2.dp))
        Text(subtitle, fontSize = 10.sp, color = CyberTextMuted)
    }
}

@Composable
fun PausedClientsBanner(pausedClients: List<Client>, onClientClick: (String) -> Unit) {
    val now = System.currentTimeMillis()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CyberWarning.copy(alpha = 0.08f))
            .border(1.dp, CyberWarning.copy(alpha = 0.25f), RoundedCornerShape(20.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier.size(28.dp).clip(CircleShape)
                    .background(CyberWarning.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text("⏸", fontSize = 13.sp)
            }
            Text(
                "${pausedClients.size} member${if (pausedClients.size > 1) "s" else ""} on pause",
                fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CyberWarning
            )
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(pausedClients, key = { it.id }) { client ->
                val pausedDays = if (client.pausedAtMillis > 0L)
                    ((now - client.pausedAtMillis) / 86400000L).toInt()
                else null

                val durationLabel = when {
                    pausedDays == null       -> "Paused"
                    pausedDays == 0          -> "Paused today"
                    pausedDays == 1          -> "1 day"
                    pausedDays < 7           -> "$pausedDays days"
                    pausedDays < 30          -> "${pausedDays / 7}w ${pausedDays % 7}d"
                    else                     -> "${pausedDays / 7} weeks"
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(CyberBgCard)
                        .border(1.dp, CyberWarning.copy(alpha = 0.20f), RoundedCornerShape(999.dp))
                        .clickable { onClientClick(client.id) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier.size(26.dp).clip(CircleShape)
                            .background(CyberWarning.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(client.name.take(1), fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold, color = CyberWarning)
                    }
                    Column {
                        Text(client.name.split(" ").first(),
                            fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CyberTextPrimary)
                        Text(durationLabel, fontSize = 10.sp, color = CyberWarning)
                    }
                }
            }
        }
    }
}

@Composable
fun InsightCard(insight: BusinessInsight) {
    val accentColor = when {
        insight.isAlert -> CyberWarning
        insight.emoji.contains("📈") || insight.emoji.contains("💎") || insight.emoji.contains("🎯") -> CyberAccent
        else -> CyberSuccess
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CyberBgCard)
            .padding(0.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(64.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(accentColor)
        )
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(insight.emoji, fontSize = 22.sp)
            Column(modifier = Modifier.weight(1f)) {
                Text(insight.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CyberTextPrimary)
                Spacer(Modifier.height(2.dp))
                Text(insight.body, fontSize = 12.sp, color = CyberTextSecondary, lineHeight = 16.sp)
            }
        }
    }
}

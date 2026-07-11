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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Client
import com.example.data.Program
import com.example.data.WorkoutLog
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun programEmoji(tags: String) = when {
    tags.contains("Strength", ignoreCase = true) -> "🏋️"
    tags.contains("Fat Loss", ignoreCase = true) -> "🔥"
    tags.contains("Hypertrophy", ignoreCase = true) -> "💪"
    tags.contains("Mobility", ignoreCase = true) -> "🧘"
    tags.contains("Athletic", ignoreCase = true) || tags.contains("Performance", ignoreCase = true) -> "⚡"
    tags.contains("Wellness", ignoreCase = true) -> "🌟"
    else -> "📋"
}

@Composable
fun ProgramDetailScreen(
    viewModel: MainViewModel,
    programId: String,
    onBack: () -> Unit,
    onClientClick: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val fmt = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    val allPrograms by viewModel.programs.collectAsStateWithLifecycle()
    val allClients by viewModel.clients.collectAsStateWithLifecycle()
    val enrolledClients by viewModel.getClientsForProgramFlow(programId).collectAsStateWithLifecycle(emptyList())
    val programLogs by viewModel.getWorkoutLogsForProgramFlow(programId).collectAsStateWithLifecycle(emptyList())

    val program = allPrograms.find { it.id == programId }
    val logsByClient: Map<String, List<WorkoutLog>> = remember(programLogs) { programLogs.groupBy { it.clientId } }

    var showEnrollSheet by remember { mutableStateOf(false) }
    var showShareSheet by remember { mutableStateOf(false) }
    // Per-client sheet state
    var logSessionFor by remember { mutableStateOf<Client?>(null) }
    var missedSessionFor by remember { mutableStateOf<Client?>(null) }

    val unenrolledClients = allClients.filter { it.programId != programId }

    if (showEnrollSheet) {
        AssignProgramSheet(
            programs = emptyList(),
            clients = unenrolledClients,
            preselectedProgramId = programId,
            onDismiss = { showEnrollSheet = false },
            onAssign = { clientIds, pid ->
                clientIds.forEach { viewModel.assignProgram(it, pid) }
                showEnrollSheet = false
            }
        )
    }

    if (program == null) {
        Box(Modifier.fillMaxSize().background(CyberBgPrimary).statusBarsPadding(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Program not found", color = CyberTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                androidx.compose.material3.TextButton(onClick = onBack) { Text("Go Back", color = CyberAccent) }
            }
        }
        return
    }

    if (showShareSheet) {
        ShareProgramSheet(program = program, enrolledClients = enrolledClients, onDismiss = { showShareSheet = false })
    }

    logSessionFor?.let { client ->
        LogSessionSheet(
            clientId = client.id,
            programId = programId,
            programs = allPrograms,
            onDismiss = { logSessionFor = null },
            onSave = { log ->
                viewModel.logSession(log, client, program)
                logSessionFor = null
            }
        )
    }

    missedSessionFor?.let { client ->
        MissedSessionSheet(
            clientName = client.name,
            onDismiss = { missedSessionFor = null },
            onConfirm = { reason ->
                viewModel.logMissedSession(client, program, reason)
                missedSessionFor = null
            }
        )
    }

    val enrolledCount = enrolledClients.size
    val avgConsistency = if (enrolledClients.isEmpty()) 0
        else enrolledClients.map { it.consistencyScore }.average().toInt()
    val totalMrr = enrolledClients.sumOf { it.mrr }
    val avgWeek = if (enrolledClients.isEmpty()) 0
        else enrolledClients.map { it.programWeek }.average().toInt()
    val churnRiskClients = enrolledClients.filter { it.consistencyScore < 40 }
    val milestoneClients = enrolledClients.filter { it.programWeek > 0 && it.programWeek % 4 == 0 }

    val totalMrrDisplay = when {
        totalMrr >= 1000 -> "₹${totalMrr / 1000}K"
        else -> "₹$totalMrr"
    }
    val tags = program.tags.split(",").filter { it.isNotBlank() }
    val today = fmt.format(Date())

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(CyberBgPrimary).statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top bar
        item {
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(CyberBgCard)
                        .clickable { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = CyberTextPrimary, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Program", fontSize = 11.sp, color = CyberTextMuted, letterSpacing = 0.5.sp, fontWeight = FontWeight.SemiBold)
                    Text("Details", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = CyberTextPrimary)
                }
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(CyberBgCard)
                        .clickable { viewModel.cloneProgram(program) }.padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Filled.ContentCopy, null, tint = CyberTextMuted, modifier = Modifier.size(14.dp))
                        Text("Clone", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = CyberTextMuted)
                    }
                }
            }
        }

        // Hero card
        item {
            Box(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(CyberAccent).padding(24.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(programEmoji(program.tags), fontSize = 36.sp)
                        Spacer(Modifier.width(14.dp))
                        Text(program.name, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold,
                            color = CyberAccentDark, lineHeight = 25.sp, modifier = Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        tags.take(3).forEach { tag ->
                            Box(
                                modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(Color(0x26000000))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(tag.trim(), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = CyberAccentDark)
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        HeroPill(Icons.Filled.AccessTime, "${program.durationWeeks} weeks")
                        HeroPill(Icons.Filled.Bolt, "${program.daysPerWeek} sessions/wk")
                        if (enrolledCount > 0) HeroPill(Icons.Filled.SupervisorAccount, "$enrolledCount clients")
                    }
                }
            }
        }

        // Stats grid
        item {
            val consistencyTint = when {
                avgConsistency >= 70 -> CyberSuccess
                avgConsistency >= 50 -> CyberWarning
                else -> CyberDanger
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ProgramStatCard("$enrolledCount", "Enrolled", "active clients", Icons.Filled.SupervisorAccount, CyberAccent)
                    ProgramStatCard("$avgConsistency%", "Avg Score", "consistency", Icons.Filled.Timeline, consistencyTint)
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ProgramStatCard(totalMrrDisplay, "Program MRR", "monthly revenue", Icons.Filled.AttachMoney, CyberAccent)
                    ProgramStatCard("Wk $avgWeek", "Avg Progress", "of ${program.durationWeeks} weeks", Icons.AutoMirrored.Filled.TrendingUp, CyberAccent)
                }
            }
        }

        // Program timeline overview (week progress bar)
        if (enrolledCount > 0) {
            item {
                val totalWeeks = program.durationWeeks.coerceAtLeast(1)
                val filledWeeks = avgWeek.coerceIn(0, totalWeeks)
                val pct = filledWeeks * 100 / totalWeeks

                Column(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(CyberBgCard).padding(20.dp)
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Program Timeline", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = CyberTextPrimary)
                        Text("$totalWeeks weeks", fontSize = 12.sp, color = CyberTextMuted)
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        (1..totalWeeks).forEach { week ->
                            val color = when {
                                week < avgWeek -> CyberAccent
                                week == avgWeek -> CyberAccent.copy(0.6f)
                                else -> CyberBgCardElevated
                            }
                            Box(modifier = Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)).background(color))
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Avg Week $avgWeek / $totalWeeks", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = CyberAccent)
                        Text("$pct% complete", fontSize = 11.sp, color = CyberTextMuted)
                    }
                }
            }
        }

        // ── TODAY'S SESSIONS ──────────────────────────────────────────────────
        if (enrolledClients.isNotEmpty()) {
            item {
                Row(
                    Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Today's Sessions", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = CyberTextPrimary)
                        Text("Log progress or mark a miss for each member", fontSize = 12.sp, color = CyberTextMuted)
                    }
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(CyberSuccess.copy(0.1f))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        val todayDoneCount = enrolledClients.count { client ->
                            (logsByClient[client.id] ?: emptyList()).any { log ->
                                !log.isMissed &&
                                fmt.format(Date(log.sessionDateMillis)) == today
                            }
                        }
                        Text("$todayDoneCount / $enrolledCount done", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CyberSuccess)
                    }
                }
            }

            items(enrolledClients, key = { "today_${it.id}" }) { client ->
                val clientLogs = logsByClient[client.id] ?: emptyList()
                val loggedToday = clientLogs.any { log ->
                    !log.isMissed &&
                    fmt.format(Date(log.sessionDateMillis)) == today
                }
                val missedToday = clientLogs.any { log ->
                    log.isMissed &&
                    fmt.format(Date(log.sessionDateMillis)) == today
                }

                DailySessionCard(
                    client = client,
                    loggedToday = loggedToday,
                    missedToday = missedToday,
                    onLogDone = { logSessionFor = client },
                    onMarkMissed = { missedSessionFor = client },
                    onClientClick = { onClientClick(client.id) },
                    onWhatsApp = {
                        val msg = "Hey ${client.name}, reminder to log your session today! You're on Week ${client.programWeek} of ${program.name} — keep the streak going 💪"
                        val url = if (client.phoneNumber.isNotEmpty()) {
                            val phone = client.phoneNumber.trimStart('+').let { if (it.startsWith("91") && it.length > 10) it else "91$it" }
                            "https://wa.me/$phone?text=${Uri.encode(msg)}"
                        } else "https://wa.me/?text=${Uri.encode(msg)}"
                        try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } catch (_: Exception) {}
                    }
                )
            }
        }

        // ── CLIENT TIMELINE ───────────────────────────────────────────────────
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Member Timeline", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = CyberTextPrimary)
                if (enrolledCount > 0) {
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(CyberAccent.copy(0.15f))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text("$enrolledCount enrolled", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CyberAccent)
                    }
                }
            }
        }

        if (enrolledClients.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("👥", fontSize = 32.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("No members enrolled yet", color = CyberTextMuted, fontSize = 14.sp)
                    }
                }
            }
        } else {
            items(enrolledClients, key = { "timeline_${it.id}" }) { client ->
                val clientLogs = logsByClient[client.id] ?: emptyList()
                EnrolledClientTimelineCard(
                    client = client,
                    program = program,
                    logs = clientLogs,
                    onClientClick = { onClientClick(client.id) }
                )
            }
        }

        // Signals
        if (churnRiskClients.isNotEmpty() || milestoneClients.isNotEmpty()) {
            item { Text("Signals", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = CyberTextPrimary, modifier = Modifier.padding(top = 4.dp)) }
            if (churnRiskClients.isNotEmpty()) {
                item {
                    ProgramSignalCard(CyberDanger, "⚠️",
                        "${churnRiskClients.size} client${if (churnRiskClients.size > 1) "s" else ""} at churn risk",
                        churnRiskClients.joinToString(", ") { it.name } + " — needs urgent outreach this week")
                }
            }
            if (milestoneClients.isNotEmpty()) {
                item {
                    ProgramSignalCard(CyberAccent, "🎯",
                        "${milestoneClients.size} client${if (milestoneClients.size > 1) "s" else ""} hit a milestone",
                        milestoneClients.joinToString(", ") { "${it.name} (Wk ${it.programWeek})" } + " — send a shoutout!")
                }
            }
        }

        // Action buttons
        item {
            Spacer(Modifier.height(4.dp))
            val whatsappGreen = Color(0xFF25D366)
            val canShare = enrolledClients.isNotEmpty()
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(999.dp))
                        .background(if (canShare) Color(0xFF162016) else CyberBgCard)
                        .border(1.dp, if (canShare) whatsappGreen else CyberTextMuted.copy(0.2f), RoundedCornerShape(999.dp))
                        .clickable(enabled = canShare) { showShareSheet = true }
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, null,
                        tint = if (canShare) whatsappGreen else CyberTextMuted, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Send to Members", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold,
                        color = if (canShare) whatsappGreen else CyberTextMuted)
                }
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(999.dp)).background(CyberAccent)
                        .clickable { showEnrollSheet = true }.padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Add, null, tint = CyberAccentDark, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Enroll Members", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = CyberAccentDark)
                }
            }
        }
    }
}

// ─── Daily Session Card ───────────────────────────────────────────────────────

@Composable
private fun DailySessionCard(
    client: Client,
    loggedToday: Boolean,
    missedToday: Boolean,
    onLogDone: () -> Unit,
    onMarkMissed: () -> Unit,
    onClientClick: () -> Unit,
    onWhatsApp: () -> Unit
) {
    val consistencyColor = when {
        client.consistencyScore >= 80 -> CyberSuccess
        client.consistencyScore >= 60 -> CyberWarning
        else -> CyberDanger
    }
    val stateColor = when {
        loggedToday -> CyberSuccess
        missedToday -> CyberDanger
        else -> CyberBgCardElevated
    }
    val stateLabel = when {
        loggedToday -> "Done ✅"
        missedToday -> "Missed ❌"
        else -> "Pending"
    }

    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
            .background(CyberBgCard)
            .border(1.dp, stateColor.copy(if (loggedToday || missedToday) 0.4f else 0f), RoundedCornerShape(20.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.weight(1f).clickable { onClientClick() },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier.size(42.dp).clip(CircleShape).background(consistencyColor.copy(0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text(client.name.take(1), fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = consistencyColor)
            }
            Column {
                Text(client.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = CyberTextPrimary)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Wk ${client.programWeek}", fontSize = 11.sp, color = CyberTextMuted)
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(consistencyColor.copy(0.12f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("${client.consistencyScore}%", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = consistencyColor)
                    }
                    if (loggedToday || missedToday) {
                        Box(
                            modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(stateColor.copy(0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(stateLabel, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = stateColor)
                        }
                    }
                }
            }
        }

        if (!loggedToday && !missedToday) {
            // Action buttons
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier.size(38.dp).clip(CircleShape).background(CyberSuccess.copy(0.15f))
                        .border(1.dp, CyberSuccess.copy(0.3f), CircleShape)
                        .clickable { onLogDone() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Check, null, tint = CyberSuccess, modifier = Modifier.size(18.dp))
                }
                Box(
                    modifier = Modifier.size(38.dp).clip(CircleShape).background(CyberDanger.copy(0.15f))
                        .border(1.dp, CyberDanger.copy(0.3f), CircleShape)
                        .clickable { onMarkMissed() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Close, null, tint = CyberDanger, modifier = Modifier.size(18.dp))
                }
                Box(
                    modifier = Modifier.size(38.dp).clip(CircleShape).background(Color(0xFF25D366).copy(0.15f))
                        .clickable { onWhatsApp() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, null, tint = Color(0xFF25D366), modifier = Modifier.size(16.dp))
                }
            }
        } else {
            // Re-log / undo option
            Box(
                modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(CyberBgCardElevated)
                    .clickable { if (loggedToday) onLogDone() else onMarkMissed() }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text("Update", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = CyberTextMuted)
            }
        }
    }
}

// ─── Enrolled Client Timeline Card ────────────────────────────────────────────

@Composable
private fun EnrolledClientTimelineCard(
    client: Client,
    program: Program,
    logs: List<WorkoutLog>,
    onClientClick: () -> Unit = {}
) {
    val consistencyColor = when {
        client.consistencyScore >= 80 -> CyberAccent
        client.consistencyScore >= 60 -> CyberWarning
        else -> CyberDanger
    }
    val statusColor = if (client.status == "Active") CyberSuccess else CyberWarning

    // Build per-week session data
    // For each week 1..programWeek, figure out how many sessions were done vs. missed
    val completedCount = logs.count { !it.isMissed }
    val missedCount = logs.count { it.isMissed }
    val totalExpected = (client.programWeek * program.daysPerWeek).coerceAtLeast(1)

    // Show capped segment bars for visual week timeline
    val displayWeeks = program.durationWeeks.coerceAtMost(16)

    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(CyberBgCard)
            .clickable { onClientClick() }.padding(16.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier.size(44.dp).clip(CircleShape).background(consistencyColor.copy(0.12f))
                .border(1.dp, consistencyColor.copy(0.3f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(client.name.take(1), fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = consistencyColor)
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(client.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = CyberTextPrimary,
                    modifier = Modifier.weight(1f))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(statusColor.copy(0.12f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(client.status, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = statusColor)
                    }
                    Text("›", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = CyberTextMuted)
                }
            }

            Spacer(Modifier.height(10.dp))

            // Week segment progress bar
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                (1..displayWeeks).forEach { week ->
                    val segColor = when {
                        week < client.programWeek -> consistencyColor.copy(0.75f)
                        week == client.programWeek -> consistencyColor.copy(0.4f) // current week
                        else -> CyberBgCardElevated
                    }
                    Box(
                        modifier = Modifier.weight(1f).height(5.dp).clip(RoundedCornerShape(3.dp)).background(segColor)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Session dot row for recent logs (last 7 sessions)
            val recentLogs = logs.take(7)
            if (recentLogs.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Recent:", fontSize = 10.sp, color = CyberTextMuted)
                    recentLogs.forEach { log ->
                        val dotColor = if (log.isMissed) CyberDanger else CyberSuccess
                        Box(
                            modifier = Modifier.size(8.dp).clip(CircleShape).background(dotColor)
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
            }

            // Stats row
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Wk ${client.programWeek}/${program.durationWeeks}", fontSize = 11.sp, color = CyberTextMuted)
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(consistencyColor.copy(0.12f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text("${client.consistencyScore}%", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = consistencyColor)
                }
                if (completedCount > 0) {
                    Text("$completedCount done", fontSize = 11.sp, color = CyberSuccess)
                }
                if (missedCount > 0) {
                    Text("$missedCount missed", fontSize = 11.sp, color = CyberDanger)
                }
            }

            // Missed reason if latest log was missed
            val latestMissed = logs.firstOrNull { it.isMissed }
            if (latestMissed != null && latestMissed.missedReason.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Last miss: ${latestMissed.missedReason}",
                    fontSize = 11.sp, color = CyberDanger.copy(0.8f),
                    maxLines = 1
                )
            }
        }
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

@Composable
private fun HeroPill(icon: ImageVector, label: String) {
    Row(
        modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(Color(0x26000000))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(icon, null, tint = CyberAccentDark.copy(0.8f), modifier = Modifier.size(12.dp))
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = CyberAccentDark)
    }
}

@Composable
private fun ProgramStatCard(value: String, label: String, sublabel: String, icon: ImageVector, iconTint: Color) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(CyberBgCard).padding(16.dp)
    ) {
        Box(
            modifier = Modifier.size(32.dp).clip(CircleShape).background(iconTint.copy(0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = iconTint, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.height(12.dp))
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = CyberTextPrimary)
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = CyberTextSecondary)
        Text(sublabel, fontSize = 11.sp, color = CyberTextMuted)
    }
}

@Composable
private fun ProgramSignalCard(borderColor: Color, emoji: String, title: String, body: String) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(CyberBgCard).padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(modifier = Modifier.width(4.dp).height(56.dp).clip(RoundedCornerShape(999.dp)).background(borderColor))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(emoji, fontSize = 14.sp)
                Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CyberTextPrimary)
            }
            Spacer(Modifier.height(4.dp))
            Text(body, fontSize = 12.sp, color = CyberTextMuted, lineHeight = 17.sp)
        }
    }
}

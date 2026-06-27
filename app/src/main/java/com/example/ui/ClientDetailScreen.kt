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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.BodyMeasurement
import com.example.data.Client
import com.example.data.ClientNote
import com.example.data.Payment
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
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

@Composable
fun ClientDetailScreen(
    viewModel: MainViewModel,
    clientId: String,
    onBack: () -> Unit,
    onDietPlanEdit: (clientId: String) -> Unit = {}
) {
    val context = LocalContext.current
    val clients by viewModel.clients.collectAsStateWithLifecycle()
    val payments by viewModel.payments.collectAsStateWithLifecycle()
    val programs by viewModel.programs.collectAsStateWithLifecycle()
    val workoutLogs by viewModel.getWorkoutLogsFlow(clientId).collectAsStateWithLifecycle(emptyList())
    val measurements by viewModel.getMeasurementsFlow(clientId).collectAsStateWithLifecycle(emptyList())
    val notes by viewModel.getNotesFlow(clientId).collectAsStateWithLifecycle(emptyList())
    val loggedTodayIds by viewModel.clientsLoggedToday.collectAsStateWithLifecycle()

    val client = clients.find { it.id == clientId }
    val clientPayments = payments.filter { it.clientId == clientId }
    val program = programs.find { it.id == client?.programId }
    val isLoggedToday = (client?.id ?: "") in loggedTodayIds

    var selectedTab by remember { mutableStateOf(0) }
    var showEditSheet by remember { mutableStateOf(false) }
    var showLogSessionSheet by remember { mutableStateOf(false) }
    var showLogMeasurementSheet by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }
    var logDayTarget by remember { mutableStateOf<Long?>(null) }
    val tabs = listOf("Overview", "Sessions", "Progress", "Notes", "Billing", "Diet Plan")

    if (client == null) {
        Box(Modifier.fillMaxSize().background(CyberBgPrimary), contentAlignment = Alignment.Center) {
            Text("Client not found", color = CyberTextMuted, fontSize = 14.sp)
        }
        return
    }

    if (showEditSheet) {
        EditClientSheet(
            client = client,
            programs = programs,
            onDismiss = { showEditSheet = false },
            onSave = { updated -> viewModel.updateClient(updated); showEditSheet = false }
        )
    }

    if (showLogSessionSheet) {
        LogSessionSheet(
            clientId = clientId,
            programId = client.programId,
            programs = programs,
            onDismiss = { showLogSessionSheet = false },
            onSave = { log -> viewModel.logSession(log, client, program); showLogSessionSheet = false }
        )
    }

    if (showLogMeasurementSheet) {
        LogMeasurementSheet(
            clientId = clientId,
            onDismiss = { showLogMeasurementSheet = false },
            onSave = { m -> viewModel.logMeasurement(m); showLogMeasurementSheet = false }
        )
    }

    logDayTarget?.let { dayMs ->
        val existingLogForDay = workoutLogs.find { log ->
            val lc = Calendar.getInstance().apply { timeInMillis = log.sessionDateMillis }
            val dc = Calendar.getInstance().apply { timeInMillis = dayMs }
            lc.get(Calendar.YEAR) == dc.get(Calendar.YEAR) &&
            lc.get(Calendar.DAY_OF_YEAR) == dc.get(Calendar.DAY_OF_YEAR)
        }
        val prog = programs.find { it.id == client?.programId }
        LogDaySheet(
            dateMillis = dayMs,
            clientId = clientId,
            programId = client?.programId,
            existingLog = existingLogForDay,
            onDismiss = { logDayTarget = null },
            onLog = { type, notes ->
                client?.let { c -> viewModel.logDay(c, prog, dayMs, type, notes) }
                logDayTarget = null
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete ${client.name}?", color = CyberTextPrimary) },
            text = { Text("This will permanently remove the client and all their data.", color = CyberTextSecondary) },
            confirmButton = {
                TextButton(
                    onClick = { isDeleting = true; viewModel.deleteClient(clientId); showDeleteConfirm = false; onBack() },
                    enabled = !isDeleting
                ) {
                    if (isDeleting) {
                        CircularProgressIndicator(color = CyberDanger, modifier = androidx.compose.ui.Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Delete", color = CyberDanger, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel", color = CyberTextMuted)
                }
            },
            containerColor = CyberBgCard
        )
    }

    val statusColor = when {
        client.consistencyScore >= 80 -> CyberSuccess
        client.consistencyScore >= 60 -> CyberWarning
        else -> CyberDanger
    }
    val daysSinceCheckIn = ((System.currentTimeMillis() - client.lastCheckInMillis) / 86400000L).coerceAtLeast(0)
    val lastCheckInDisplay = when {
        daysSinceCheckIn == 0L -> "Today"
        daysSinceCheckIn == 1L -> "Yesterday"
        else -> "${daysSinceCheckIn}d ago"
    }
    val waMessage = "Hi ${client.name}, just checking in! How's the training going this week? Let me know if you need any adjustments."

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(CyberBgPrimary),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
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
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = CyberTextPrimary, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(12.dp))
                Text("Client Profile", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = CyberTextPrimary, modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(CyberDanger.copy(0.12f))
                        .clickable { showDeleteConfirm = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Delete, "Delete", tint = CyberDanger, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(999.dp))
                        .background(statusColor.copy(alpha = 0.15f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(client.status, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = statusColor)
                }
            }
        }

        // Hero avatar
        item {
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .border(3.dp, statusColor.copy(alpha = 0.55f), RoundedCornerShape(24.dp))
                        .clip(RoundedCornerShape(24.dp))
                        .background(CyberBgCardElevated),
                    contentAlignment = Alignment.Center
                ) {
                    Text(client.name.take(1), fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = CyberAccent)
                }
                Spacer(Modifier.height(12.dp))
                Text(client.name, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = CyberTextPrimary)
                Text("@${client.name.lowercase().replace(" ", "_")}", fontSize = 14.sp, color = CyberTextMuted)
                if (client.city.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(client.city, fontSize = 13.sp, color = CyberTextSecondary)
                }
                if (client.phoneNumber.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Filled.Phone, null, tint = CyberTextSecondary, modifier = Modifier.size(12.dp))
                        Text("+91 ${client.phoneNumber}", fontSize = 13.sp, color = CyberTextSecondary)
                    }
                }
            }
        }

        // Tab selector (scrollable)
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 0.dp)
            ) {
                items(tabs.size) { index ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (selectedTab == index) CyberAccent else CyberBgCard)
                            .clickable { selectedTab = index }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            tabs[index],
                            fontSize = 13.sp,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.SemiBold,
                            color = if (selectedTab == index) CyberAccentDark else CyberTextMuted
                        )
                    }
                }
            }
        }

        // Tab content
        when (selectedTab) {
            0 -> item {
                OverviewContent(
                    client = client,
                    program = program,
                    statusColor = statusColor,
                    lastCheckInDisplay = lastCheckInDisplay,
                    sessionCount = workoutLogs.size,
                    isLoggedToday = isLoggedToday,
                    onWhatsApp = {
                        val url = if (client.phoneNumber.isNotEmpty())
                            "https://wa.me/91${client.phoneNumber}?text=${Uri.encode(waMessage)}"
                        else "https://wa.me/?text=${Uri.encode(waMessage)}"
                        try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } catch (_: Exception) {}
                    },
                    onEdit = { showEditSheet = true },
                    onTogglePause = { viewModel.toggleClientPause(client) },
                    onCheckIn = { viewModel.markAttendanceToday(client, program) }
                )
            }

            1 -> {
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("Schedule", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = CyberTextPrimary)
                            if (program != null) {
                                Text("Week ${client.programWeek} of ${program.durationWeeks}",
                                    fontSize = 11.sp, color = CyberTextMuted)
                            }
                        }
                        Box(
                            modifier = Modifier.size(34.dp).clip(CircleShape).background(CyberAccent)
                                .clickable { showLogSessionSheet = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Add, null, tint = CyberAccentDark, modifier = Modifier.size(18.dp))
                        }
                    }
                }
                if (program == null || program.workingDays.isBlank()) {
                    // No schedule defined — show flat log list
                    if (workoutLogs.isEmpty()) {
                        item {
                            EmptyStateCard(
                                emoji = "🏋️",
                                title = "No sessions logged",
                                subtitle = "Tap + to log · Or set training days in the program to enable daily tracking"
                            )
                        }
                    } else {
                        items(workoutLogs, key = { it.id }) { log -> WorkoutLogCard(log) }
                    }
                } else {
                    // Weekly calendar — show all weeks with schedule
                    val trainingDays = program.workingDays.split(",").map { it.trim() }
                    val dayOrder = listOf("MON","TUE","WED","THU","FRI","SAT","SUN")
                    val now = System.currentTimeMillis()

                    items((1..program.durationWeeks).toList(), key = { "week_$it" }) { weekNum ->
                        // Compute this week's Monday
                        val enrollCal = Calendar.getInstance().apply {
                            timeInMillis = client.enrollmentDateMillis
                            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                        }
                        val weekStartMs = enrollCal.timeInMillis + (weekNum - 1).toLong() * 7 * 86400000L

                        val isCurrentWeek = weekNum == client.programWeek
                        val isPastWeek = weekStartMs + 7 * 86400000L < now

                        Box(
                            modifier = Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    if (isCurrentWeek) CyberAccent.copy(alpha = 0.06f) else CyberBgCard
                                )
                                .border(1.dp,
                                    if (isCurrentWeek) CyberAccent.copy(alpha = 0.25f) else Color.White.copy(0.04f),
                                    RoundedCornerShape(20.dp))
                                .padding(14.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text("Week $weekNum",
                                            fontSize = 13.sp, fontWeight = FontWeight.Bold,
                                            color = if (isCurrentWeek) CyberAccent else CyberTextPrimary)
                                        if (isCurrentWeek) {
                                            Box(
                                                modifier = Modifier.clip(RoundedCornerShape(999.dp))
                                                    .background(CyberAccent.copy(alpha = 0.15f))
                                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                                            ) { Text("Current", fontSize = 10.sp, color = CyberAccent, fontWeight = FontWeight.Bold) }
                                        }
                                    }
                                    val weekDateStr = SimpleDateFormat("MMM d", Locale.getDefault())
                                        .format(Date(weekStartMs))
                                    Text(weekDateStr, fontSize = 11.sp, color = CyberTextMuted)
                                }

                                // 7 day cells
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    dayOrder.forEachIndexed { idx, day ->
                                        val dayMs = weekStartMs + idx.toLong() * 86400000L
                                        val isTraining = day in trainingDays
                                        val isFuture = dayMs > now
                                        val isToday = run {
                                            val todayCal = Calendar.getInstance()
                                            val dayCal = Calendar.getInstance().apply { timeInMillis = dayMs }
                                            todayCal.get(Calendar.DAY_OF_YEAR) == dayCal.get(Calendar.DAY_OF_YEAR) &&
                                            todayCal.get(Calendar.YEAR) == dayCal.get(Calendar.YEAR)
                                        }
                                        val logForDay = workoutLogs.find { log ->
                                            val lc = Calendar.getInstance().apply { timeInMillis = log.sessionDateMillis }
                                            val dc = Calendar.getInstance().apply { timeInMillis = dayMs }
                                            lc.get(Calendar.YEAR) == dc.get(Calendar.YEAR) &&
                                            lc.get(Calendar.DAY_OF_YEAR) == dc.get(Calendar.DAY_OF_YEAR)
                                        }
                                        val (cellBg, cellLabel, labelColor) = when {
                                            !isTraining -> Triple(
                                                CyberBgCardElevated.copy(alpha = 0.4f),
                                                "–", CyberTextMuted.copy(alpha = 0.3f)
                                            )
                                            logForDay != null && !logForDay.isMissed -> Triple(
                                                CyberSuccess.copy(alpha = 0.18f), "✓", CyberSuccess
                                            )
                                            logForDay?.missedReason?.startsWith("LEAVE") == true -> Triple(
                                                CyberWarning.copy(alpha = 0.18f), "L", CyberWarning
                                            )
                                            logForDay != null && logForDay.isMissed -> Triple(
                                                CyberDanger.copy(alpha = 0.18f), "✗", CyberDanger
                                            )
                                            isFuture -> Triple(
                                                CyberBgCardElevated, "·", CyberTextMuted
                                            )
                                            else -> Triple(
                                                CyberAccent.copy(alpha = 0.08f), "?", CyberAccent.copy(alpha = 0.5f)
                                            )
                                        }
                                        val canTap = isTraining && !isFuture
                                        Box(
                                            modifier = Modifier.weight(1f)
                                                .aspectRatio(1f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(cellBg)
                                                .then(
                                                    if (isToday && isTraining) Modifier.border(
                                                        1.5.dp, CyberAccent.copy(alpha = 0.6f), RoundedCornerShape(8.dp)
                                                    ) else Modifier
                                                )
                                                .clickable(enabled = canTap) { logDayTarget = dayMs },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                Text(cellLabel, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = labelColor)
                                                Text(day.take(1), fontSize = 8.sp, color = if (isTraining) labelColor.copy(alpha = 0.7f) else CyberTextMuted.copy(alpha = 0.2f))
                                            }
                                        }
                                    }
                                }

                                // Legend (only on first visible week)
                                if (weekNum == 1) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        LegendDot(CyberSuccess, "Done")
                                        LegendDot(CyberWarning, "Leave")
                                        LegendDot(CyberDanger, "Absent")
                                        LegendDot(CyberTextMuted.copy(alpha = 0.3f), "Rest")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            2 -> {
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Progress", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = CyberTextPrimary)
                        Box(
                            modifier = Modifier.size(34.dp).clip(CircleShape).background(CyberAccent)
                                .clickable { showLogMeasurementSheet = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Add, null, tint = CyberAccentDark, modifier = Modifier.size(18.dp))
                        }
                    }
                }
                if (measurements.isEmpty()) {
                    item {
                        EmptyStateCard(
                            emoji = "📊",
                            title = "No measurements yet",
                            subtitle = "Tap + to log weight & body fat"
                        )
                    }
                } else {
                    items(measurements, key = { it.id }) { m -> MeasurementCard(m) }
                }
            }

            3 -> {
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Notes", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = CyberTextPrimary)
                    }
                }
                item {
                    QuickNoteInput(clientId = clientId, onSave = { viewModel.addNote(it) })
                }
                if (notes.isEmpty()) {
                    item {
                        EmptyStateCard(
                            emoji = "📝",
                            title = "No notes yet",
                            subtitle = "Add form cues, injury notes, or reminders above"
                        )
                    }
                } else {
                    items(notes, key = { it.id }) { note ->
                        NoteCard(note = note, onDelete = { viewModel.deleteNote(note.id) })
                    }
                }
            }

            4 -> {
                if (clientPayments.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                            Text("No payment records", color = CyberTextSecondary, fontSize = 14.sp)
                        }
                    }
                } else {
                    item { Text("Payment History", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = CyberTextPrimary) }
                    items(clientPayments, key = { it.id }) { payment ->
                        PaymentCard(payment = payment, clientName = client.name, clientPhone = client.phoneNumber)
                    }
                }
            }
            5 -> item {
                DietPlanTabContent(
                    viewModel          = viewModel,
                    clientId           = clientId,
                    clientName         = client.name,
                    onNavigateToEditor = { onDietPlanEdit(clientId) }
                )
            }
        }
    }
}

@Composable
private fun OverviewContent(
    client: Client,
    program: Program?,
    statusColor: Color,
    lastCheckInDisplay: String,
    sessionCount: Int,
    isLoggedToday: Boolean,
    onWhatsApp: () -> Unit,
    onEdit: () -> Unit,
    onTogglePause: () -> Unit,
    onCheckIn: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Goal card
        Box(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(CyberAccent).padding(18.dp)
        ) {
            Column {
                Text("GOAL", fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0A0A0A).copy(alpha = 0.5f), letterSpacing = 0.8.sp)
                Spacer(Modifier.height(4.dp))
                Text(client.initialGoal, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = CyberAccentDark)
            }
        }

        // Stats 2×2
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DetailStatBox(Modifier.weight(1f), "CONSISTENCY", "${client.consistencyScore}%", statusColor)
            DetailStatBox(Modifier.weight(1f), "PROGRAM WEEK", "Week ${client.programWeek}", CyberTextPrimary)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DetailStatBox(Modifier.weight(1f), "MONTHLY RETAINER", "₹${"%,d".format(client.mrr)}", CyberAccent)
            DetailStatBox(Modifier.weight(1f), "LAST CHECK-IN", lastCheckInDisplay, CyberTextPrimary)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DetailStatBox(Modifier.weight(1f), "SESSIONS LOGGED", "$sessionCount", CyberAccent)
            DetailStatBox(Modifier.weight(1f), "LOCATION", client.city.ifEmpty { "—" }, CyberTextPrimary)
        }

        // Program card
        if (program != null) {
            Box(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(CyberBgCard).padding(16.dp)
            ) {
                Column {
                    Text("CURRENT PROGRAM", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = CyberTextMuted, letterSpacing = 0.8.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(program.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = CyberTextPrimary)
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatPill(icon = { Icon(Icons.Filled.AccessTime, null, tint = CyberAccent, modifier = Modifier.size(12.dp)) }, label = "${program.durationWeeks} weeks")
                        StatPill(icon = { Icon(Icons.Filled.Bolt, null, tint = CyberAccent, modifier = Modifier.size(12.dp)) }, label = "${program.daysPerWeek} sessions/wk")
                        StatPill(icon = { Icon(Icons.Filled.FitnessCenter, null, tint = CyberAccent, modifier = Modifier.size(12.dp)) }, label = "Week ${client.programWeek}/${program.durationWeeks}")
                    }
                }
            }
        }

        // Quick actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f).clip(RoundedCornerShape(999.dp)).background(Color(0xFF25D366))
                    .clickable { onWhatsApp() }.padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Message", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            // Attendance button — green + disabled once logged today
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier.size(48.dp).clip(CircleShape)
                        .background(CyberSuccess.copy(if (isLoggedToday) 0.25f else 0.15f))
                        .clickable(enabled = !isLoggedToday) { onCheckIn() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isLoggedToday) Icons.Filled.Check else Icons.Filled.FitnessCenter,
                        "Attendance",
                        tint = CyberSuccess,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    if (isLoggedToday) "Done" else "Log",
                    fontSize = 9.sp, color = if (isLoggedToday) CyberSuccess else CyberTextMuted,
                    fontWeight = FontWeight.SemiBold
                )
            }
            // Edit button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier.size(48.dp).clip(CircleShape).background(CyberBgCard)
                        .clickable { onEdit() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Edit, "Edit", tint = CyberTextPrimary, modifier = Modifier.size(18.dp))
                }
                Text("Edit", fontSize = 9.sp, color = CyberTextMuted, fontWeight = FontWeight.SemiBold)
            }
            // Pause / Resume button
            val isPaused = client.status == "Paused"
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier.size(48.dp).clip(CircleShape)
                        .background(if (isPaused) CyberWarning.copy(alpha = 0.20f) else CyberBgCard)
                        .clickable { onTogglePause() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isPaused) Icons.Filled.AccessTime else Icons.Filled.Pause,
                        if (isPaused) "Resume" else "Pause",
                        tint = if (isPaused) CyberWarning else CyberTextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    if (isPaused) "Resume" else "Pause",
                    fontSize = 9.sp,
                    color = if (isPaused) CyberWarning else CyberTextMuted,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun DetailStatBox(modifier: Modifier, label: String, value: String, valueColor: Color) {
    Box(
        modifier = modifier.height(80.dp).clip(RoundedCornerShape(20.dp)).background(CyberBgCard).padding(14.dp)
    ) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = CyberTextMuted, letterSpacing = 0.6.sp)
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = valueColor)
        }
    }
}

@Composable
private fun WorkoutLogCard(log: WorkoutLog) {
    val dateStr = SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(log.sessionDateMillis)).uppercase()
    val parts = dateStr.split(" ")

    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(CyberBgCard).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(40.dp)) {
            Text(parts.getOrElse(0) { "" }, fontSize = 10.sp, color = CyberTextMuted, fontWeight = FontWeight.SemiBold)
            Text(parts.getOrElse(1) { "" }, fontSize = 16.sp, color = CyberAccent, fontWeight = FontWeight.ExtraBold)
        }
        Box(modifier = Modifier.width(1.dp).height(40.dp).background(CyberBgCardElevated))
        Column(modifier = Modifier.weight(1f)) {
            Text(log.sessionName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = CyberTextPrimary)
            if (log.exercises.isNotEmpty()) {
                Spacer(Modifier.height(2.dp))
                Text(log.exercises, fontSize = 11.sp, color = CyberTextMuted, maxLines = 1)
            }
            if (log.notes.isNotEmpty()) {
                Spacer(Modifier.height(2.dp))
                Text(log.notes, fontSize = 11.sp, color = CyberTextSecondary, maxLines = 1)
            }
        }
        if (log.durationMins > 0) {
            Box(
                modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(CyberAccent.copy(0.12f))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text("${log.durationMins}m", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CyberAccent)
            }
        }
    }
}

@Composable
private fun MeasurementCard(m: BodyMeasurement) {
    val dateStr = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(m.dateMillis))
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(CyberBgCard).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier.size(42.dp).clip(CircleShape).background(CyberAccent.copy(0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.MonitorWeight, null, tint = CyberAccent, modifier = Modifier.size(20.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(dateStr, fontSize = 12.sp, color = CyberTextMuted)
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (m.weightKg > 0f) {
                    Text("${"%.1f".format(m.weightKg)} kg", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = CyberTextPrimary)
                }
                if (m.bodyFatPct > 0f) {
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(CyberWarning.copy(0.12f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text("${"%.1f".format(m.bodyFatPct)}% BF", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = CyberWarning)
                    }
                }
            }
            if (m.notes.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(m.notes, fontSize = 12.sp, color = CyberTextSecondary)
            }
        }
    }
}

@Composable
private fun QuickNoteInput(clientId: String, onSave: (ClientNote) -> Unit) {
    var noteText by remember { mutableStateOf("") }
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(CyberBgCard).padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.foundation.text.BasicTextField(
            value = noteText,
            onValueChange = { noteText = it },
            modifier = Modifier.weight(1f),
            textStyle = androidx.compose.ui.text.TextStyle(color = CyberTextPrimary, fontSize = 14.sp),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(CyberAccent),
            decorationBox = { inner ->
                if (noteText.isEmpty()) Text("Add a note…", fontSize = 14.sp, color = CyberTextMuted)
                inner()
            }
        )
        if (noteText.isNotBlank()) {
            Box(
                modifier = Modifier.size(32.dp).clip(CircleShape).background(CyberAccent)
                    .clickable {
                        onSave(ClientNote(UUID.randomUUID().toString(), clientId, content = noteText.trim()))
                        noteText = ""
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, null, tint = CyberAccentDark, modifier = Modifier.size(15.dp))
            }
        }
    }
}

@Composable
private fun NoteCard(note: ClientNote, onDelete: () -> Unit) {
    val dateStr = SimpleDateFormat("MMM dd · hh:mm a", Locale.getDefault()).format(Date(note.dateMillis))
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(CyberBgCard).padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(dateStr, fontSize = 10.sp, color = CyberTextMuted)
            Spacer(Modifier.height(4.dp))
            Text(note.content, fontSize = 14.sp, color = CyberTextPrimary, lineHeight = 20.sp)
        }
        Box(
            modifier = Modifier.size(28.dp).clip(CircleShape).background(CyberDanger.copy(0.10f)).clickable { onDelete() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Delete, null, tint = CyberDanger, modifier = Modifier.size(13.dp))
        }
    }
}

@Composable
fun EmptyStateCard(emoji: String, title: String, subtitle: String) {
    Box(
        modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(24.dp)).background(CyberBgCard),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(emoji, fontSize = 32.sp)
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = CyberTextPrimary)
            Text(subtitle, fontSize = 12.sp, color = CyberTextMuted)
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Text(label, fontSize = 10.sp, color = CyberTextMuted)
    }
}

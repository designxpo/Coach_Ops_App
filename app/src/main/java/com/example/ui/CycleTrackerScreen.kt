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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.CycleEntry
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private val FMT = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
private val DISPLAY_FMT = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

private fun msToKey(ms: Long)     = FMT.format(Date(ms))
private fun msToDisplay(ms: Long) = DISPLAY_FMT.format(Date(ms))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CycleTrackerScreen(viewModel: HealthViewModel, onBack: () -> Unit) {
    val entries by viewModel.cycleEntries.collectAsState()
    var showDialog by remember { mutableStateOf(false) }

    // Entries from Firestore are DESCENDING (newest first).
    // Cycle length = newer.startDate - older.startDate  →  entries[i] - entries[i+1]
    val avgCycleLength = if (entries.size >= 2) {
        val lengths = (0 until entries.size - 1).mapNotNull { i ->
            try {
                val newer = FMT.parse(entries[i].startDate)
                val older = FMT.parse(entries[i + 1].startDate)
                if (newer != null && older != null)
                    ((newer.time - older.time) / (1000L * 60 * 60 * 24)).toInt()
                else null
            } catch (_: Exception) { null }
        }.filter { it in 20..45 }
        if (lengths.isNotEmpty()) lengths.average().roundToInt() else 28
    } else 28

    val nextPeriod: String? = if (entries.isNotEmpty()) {
        try {
            val last = FMT.parse(entries.first().startDate)
            if (last != null) {
                val cal = Calendar.getInstance().apply {
                    time = last
                    add(Calendar.DAY_OF_YEAR, avgCycleLength)
                }
                DISPLAY_FMT.format(cal.time)
            } else null
        } catch (_: Exception) { null }
    } else null

    // Days until next period
    val daysUntil: Int? = if (entries.isNotEmpty()) {
        try {
            val last = FMT.parse(entries.first().startDate)
            if (last != null) {
                val next = Calendar.getInstance().apply {
                    time = last
                    add(Calendar.DAY_OF_YEAR, avgCycleLength)
                }.timeInMillis
                val today = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                ((next - today) / (1000L * 60 * 60 * 24)).toInt()
            } else null
        } catch (_: Exception) { null }
    } else null

    Column(
        modifier = Modifier.fillMaxSize().background(CyberBgPrimary)
            .statusBarsPadding().navigationBarsPadding()
    ) {
        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape)
                    .background(CyberBgCard).clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = CyberTextPrimary, modifier = Modifier.size(20.dp))
            }
            Column(Modifier.weight(1f)) {
                Text("Cycle Tracker", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = CyberTextPrimary)
                Text("Track your menstrual cycle", fontSize = 12.sp, color = CyberTextMuted)
            }
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape)
                    .background(Color(0xFFE91E8C)).clickable { showDialog = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Add, null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Summary stats
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CycleStat("🔄 Avg Cycle", "${avgCycleLength}d", Modifier.weight(1f))
                    CycleStat("📅 Next Period", nextPeriod ?: "—", Modifier.weight(1f))
                    CycleStat("📝 Logged", "${entries.size}", Modifier.weight(1f))
                }
            }

            // Countdown banner
            if (daysUntil != null) {
                item {
                    val (bannerColor, bannerText) = when {
                        daysUntil < 0  -> Color(0xFFEF4444) to "Period may have started"
                        daysUntil == 0 -> Color(0xFFEF4444) to "Period expected today"
                        daysUntil <= 5 -> Color(0xFFF59E0B) to "Period in $daysUntil day${if (daysUntil == 1) "" else "s"}"
                        else           -> Color(0xFFE91E8C) to "$daysUntil days until next period"
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(bannerColor.copy(0.1f))
                            .border(1.dp, bannerColor.copy(0.3f), RoundedCornerShape(14.dp))
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🌸", fontSize = 22.sp)
                        Text(bannerText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = bannerColor)
                    }
                }
            }

            if (entries.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(top = 60.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("🌸", fontSize = 48.sp)
                            Text("No cycle logged yet", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = CyberTextSecondary)
                            Text("Tap + to log your last period", fontSize = 13.sp, color = CyberTextMuted)
                        }
                    }
                }
            } else {
                items(entries) { entry ->
                    CycleEntryCard(entry, onDelete = { viewModel.deleteCycleEntry(entry.id) })
                }
            }
        }
    }

    if (showDialog) {
        LogCycleDialog(
            onDismiss = { showDialog = false },
            onSave = { entry ->
                viewModel.saveCycleEntry(entry)
                showDialog = false
            }
        )
    }
}

@Composable
private fun CycleStat(label: String, value: String, modifier: Modifier) {
    Column(
        modifier = modifier.clip(RoundedCornerShape(14.dp))
            .background(CyberBgCard)
            .border(1.dp, Color(0xFFE91E8C).copy(0.2f), RoundedCornerShape(14.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFE91E8C))
        Text(label, fontSize = 10.sp, color = CyberTextMuted)
    }
}

@Composable
private fun CycleEntryCard(entry: CycleEntry, onDelete: () -> Unit) {
    val startDisplay = try { DISPLAY_FMT.format(FMT.parse(entry.startDate)!!) } catch (_: Exception) { entry.startDate }
    val endDisplay   = try { if (entry.endDate.isNotBlank()) DISPLAY_FMT.format(FMT.parse(entry.endDate)!!) else "" } catch (_: Exception) { entry.endDate }
    val duration: Int? = if (entry.startDate.isNotBlank() && entry.endDate.isNotBlank()) {
        try {
            val s = FMT.parse(entry.startDate)
            val e = FMT.parse(entry.endDate)
            if (s != null && e != null) ((e.time - s.time) / (1000L * 60 * 60 * 24)).toInt() + 1 else null
        } catch (_: Exception) { null }
    } else null

    Row(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(14.dp)).background(CyberBgCard)
            .border(1.dp, Color.White.copy(0.07f), RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("🌸", fontSize = 28.sp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(startDisplay, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = CyberTextPrimary)
            if (endDisplay.isNotBlank())
                Text("Ended: $endDisplay${if (duration != null) "  ($duration days)" else ""}", fontSize = 12.sp, color = CyberTextMuted)
            if (entry.notes.isNotBlank())
                Text(entry.notes, fontSize = 11.sp, color = CyberTextMuted)
        }
        Icon(Icons.Filled.Delete, null, tint = CyberDanger.copy(0.5f),
            modifier = Modifier.size(18.dp).clickable { onDelete() })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LogCycleDialog(onDismiss: () -> Unit, onSave: (CycleEntry) -> Unit) {
    var startMs  by remember { mutableStateOf(System.currentTimeMillis()) }
    var endMs    by remember { mutableStateOf<Long?>(null) }
    var notes    by remember { mutableStateOf("") }

    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker   by remember { mutableStateOf(false) }

    // Start date picker
    if (showStartPicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = startMs)
        DatePickerDialog(
            onDismissRequest = { showStartPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { startMs = it }
                    showStartPicker = false
                }) { Text("OK", color = Color(0xFFE91E8C)) }
            },
            dismissButton = {
                TextButton(onClick = { showStartPicker = false }) { Text("Cancel", color = CyberTextMuted) }
            },
            colors = DatePickerDefaults.colors(containerColor = CyberBgCard)
        ) {
            DatePicker(state = state, colors = DatePickerDefaults.colors(
                containerColor          = CyberBgCard,
                titleContentColor       = CyberTextMuted,
                headlineContentColor    = CyberTextPrimary,
                weekdayContentColor     = CyberTextMuted,
                dayContentColor         = CyberTextPrimary,
                selectedDayContainerColor = Color(0xFFE91E8C),
                todayContentColor       = Color(0xFFE91E8C),
                todayDateBorderColor    = Color(0xFFE91E8C)
            ))
        }
    }

    // End date picker
    if (showEndPicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = endMs ?: startMs
        )
        DatePickerDialog(
            onDismissRequest = { showEndPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    endMs = state.selectedDateMillis
                    showEndPicker = false
                }) { Text("OK", color = Color(0xFFE91E8C)) }
            },
            dismissButton = {
                TextButton(onClick = { showEndPicker = false }) { Text("Cancel", color = CyberTextMuted) }
            },
            colors = DatePickerDefaults.colors(containerColor = CyberBgCard)
        ) {
            DatePicker(state = state, colors = DatePickerDefaults.colors(
                containerColor            = CyberBgCard,
                titleContentColor         = CyberTextMuted,
                headlineContentColor      = CyberTextPrimary,
                weekdayContentColor       = CyberTextMuted,
                dayContentColor           = CyberTextPrimary,
                selectedDayContainerColor = Color(0xFFE91E8C),
                todayContentColor         = Color(0xFFE91E8C),
                todayDateBorderColor      = Color(0xFFE91E8C)
            ))
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
                .background(CyberBgCard).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Log Period", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = CyberTextPrimary)

            // Start date picker button
            DatePickerButton(
                label   = "Period started",
                display = msToDisplay(startMs),
                onClick = { showStartPicker = true }
            )

            // End date picker button
            DatePickerButton(
                label   = "Period ended (optional)",
                display = endMs?.let { msToDisplay(it) } ?: "Tap to select",
                onClick = { showEndPicker = true },
                placeholder = endMs == null
            )

            // Notes
            OutlinedTextField(
                value       = notes,
                onValueChange = { notes = it },
                label       = { Text("Notes (optional)", color = CyberTextMuted, fontSize = 12.sp) },
                modifier    = Modifier.fillMaxWidth(),
                singleLine  = true,
                colors      = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = Color(0xFFE91E8C),
                    unfocusedBorderColor = Color.White.copy(0.15f),
                    focusedTextColor     = CyberTextPrimary,
                    unfocusedTextColor   = CyberTextPrimary
                )
            )

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text("Cancel", color = CyberTextMuted)
                }
                Button(
                    onClick = {
                        onSave(CycleEntry(
                            startDate = msToKey(startMs),
                            endDate   = endMs?.let { msToKey(it) } ?: "",
                            notes     = notes.trim()
                        ))
                    },
                    modifier = Modifier.weight(1f),
                    colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E8C)),
                    shape    = RoundedCornerShape(12.dp)
                ) {
                    Text("Save", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun DatePickerButton(
    label: String,
    display: String,
    onClick: () -> Unit,
    placeholder: Boolean = false,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, fontSize = 11.sp, color = CyberTextMuted)
        Row(
            modifier = Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .border(1.dp, Color(0xFFE91E8C).copy(0.4f), RoundedCornerShape(10.dp))
                .background(CyberBgPrimary)
                .clickable { onClick() }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Default.CalendarToday, null,
                tint = Color(0xFFE91E8C), modifier = Modifier.size(16.dp))
            Text(display, fontSize = 14.sp,
                color = if (placeholder) CyberTextMuted else CyberTextPrimary,
                fontWeight = if (placeholder) FontWeight.Normal else FontWeight.SemiBold)
        }
    }
}

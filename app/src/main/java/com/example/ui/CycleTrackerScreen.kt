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

@Composable
fun CycleTrackerScreen(viewModel: HealthViewModel, onBack: () -> Unit) {
    val entries by viewModel.cycleEntries.collectAsState()
    var showDialog by remember { mutableStateOf(false) }

    val avgCycleLength = if (entries.size >= 2) {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val lengths = (0 until entries.size - 1).mapNotNull { i ->
            try {
                val a = fmt.parse(entries[i].startDate)
                val b = fmt.parse(entries[i + 1].startDate)
                if (a != null && b != null)
                    ((b.time - a.time) / (1000 * 60 * 60 * 24)).toInt()
                else null
            } catch (_: Exception) { null }
        }.filter { it in 20..40 }
        if (lengths.isNotEmpty()) lengths.average().roundToInt() else 28
    } else 28

    val nextPeriod: String? = if (entries.isNotEmpty()) {
        try {
            val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val last = fmt.parse(entries.first().startDate)
            if (last != null) {
                val cal = Calendar.getInstance().apply {
                    time = last
                    add(Calendar.DAY_OF_YEAR, avgCycleLength)
                }
                fmt.format(cal.time)
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
            // Summary card
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CycleStat("🔄 Avg Cycle", "${avgCycleLength} days", Modifier.weight(1f))
                    CycleStat("📅 Next Period", nextPeriod ?: "—", Modifier.weight(1f))
                    CycleStat("📝 Logged", "${entries.size} cycles", Modifier.weight(1f))
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
    Row(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(14.dp)).background(CyberBgCard)
            .border(1.dp, Color.White.copy(0.07f), RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("🌸", fontSize = 28.sp)
        Column(Modifier.weight(1f)) {
            Text("Started: ${entry.startDate}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = CyberTextPrimary)
            if (entry.endDate.isNotBlank())
                Text("Ended: ${entry.endDate}", fontSize = 12.sp, color = CyberTextMuted)
            if (entry.notes.isNotBlank())
                Text(entry.notes, fontSize = 11.sp, color = CyberTextMuted)
        }
        Icon(Icons.Filled.Delete, null, tint = CyberDanger.copy(0.5f),
            modifier = Modifier.size(18.dp).clickable { onDelete() })
    }
}

@Composable
private fun LogCycleDialog(onDismiss: () -> Unit, onSave: (CycleEntry) -> Unit) {
    val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    var startDate by remember { mutableStateOf(fmt.format(Date())) }
    var endDate   by remember { mutableStateOf("") }
    var notes     by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
                .background(CyberBgCard).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Log Period", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = CyberTextPrimary)

            CycleField("Start Date (YYYY-MM-DD)", startDate, { startDate = it })
            CycleField("End Date (optional)",     endDate,   { endDate   = it })
            CycleField("Notes (optional)",        notes,     { notes     = it })

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text("Cancel", color = CyberTextMuted)
                }
                Button(
                    onClick = { onSave(CycleEntry(startDate = startDate, endDate = endDate, notes = notes)) },
                    enabled  = startDate.length == 10,
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
private fun CycleField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value, onValueChange = onChange,
        label = { Text(label, color = CyberTextMuted, fontSize = 12.sp) },
        modifier = Modifier.fillMaxWidth(), singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor   = Color(0xFFE91E8C),
            unfocusedBorderColor = Color.White.copy(0.15f),
            focusedTextColor     = CyberTextPrimary,
            unfocusedTextColor   = CyberTextPrimary
        )
    )
}

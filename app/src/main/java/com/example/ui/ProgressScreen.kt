package com.example.ui

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
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FitnessGoalEntry
import com.example.data.WorkoutLogEntry
import com.example.ui.theme.CyberAccent
import com.example.ui.theme.CyberBgCard
import com.example.ui.theme.CyberBgCardElevated
import com.example.ui.theme.CyberBgPrimary
import com.example.ui.theme.CyberDanger
import com.example.ui.theme.CyberSuccess
import com.example.ui.theme.CyberTextMuted
import com.example.ui.theme.CyberTextPrimary
import com.example.ui.theme.CyberTextSecondary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun ProgressScreen(viewModel: FitnessViewModel, onBack: (() -> Unit)? = null) {
    // Real-time listeners in FitnessViewModel auto-populate logs and goals
    val logs      by viewModel.logs.collectAsState()
    val goals     by viewModel.goals.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // Memoize: only recomputes when logs changes
    val dayMap = remember(logs) { viewModel.workoutDayMap() }

    // Selected day state — tapping a calendar cell shows workouts for that date
    var selectedDateKey by remember { mutableStateOf("") }
    val dateFmt    = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val displayFmt = remember { SimpleDateFormat("EEE, d MMM", Locale.getDefault()) }

    val selectedLogs = remember(selectedDateKey, logs) {
        if (selectedDateKey.isEmpty()) emptyList()
        else logs.filter { dateFmt.format(Date(it.dateMillis)) == selectedDateKey }
    }

    // Streak: consecutive days with at least 1 workout ending today
    val streak = remember(dayMap) {
        val today = Calendar.getInstance()
        var streak = 0
        var checking = today.clone() as Calendar
        while (true) {
            val key = dateFmt.format(checking.time)
            if ((dayMap[key] ?: 0) > 0) {
                streak++
                checking.add(Calendar.DAY_OF_YEAR, -1)
            } else break
        }
        streak
    }

    val totalThisWeek = remember(dayMap) {
        val today = Calendar.getInstance()
        (0..6).count { offset ->
            val cal = today.clone() as Calendar
            cal.add(Calendar.DAY_OF_YEAR, -offset)
            (dayMap[dateFmt.format(cal.time)] ?: 0) > 0
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBgPrimary)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (onBack != null) {
                    Box(
                        modifier = Modifier
                            .size(40.dp).clip(CircleShape)
                            .background(CyberBgCard)
                            .clickable { onBack() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = CyberTextPrimary, modifier = Modifier.size(18.dp))
                    }
                }
                Text("My Progress", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = CyberTextPrimary,
                    modifier = Modifier.weight(1f))
            }
        }

        // ── Stats strip ───────────────────────────────────────────────────────
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(CyberBgCard)
                    .border(1.dp, Color.White.copy(0.06f), RoundedCornerShape(18.dp))
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem(value = "$streak", label = "Day Streak", emoji = "🔥")
                VerticalDivider()
                StatItem(value = "$totalThisWeek", label = "This Week", emoji = "📅")
                VerticalDivider()
                StatItem(value = "${logs.size}", label = "Total Logs", emoji = "📊")
                VerticalDivider()
                StatItem(value = "${goals.size}", label = "Goals Set", emoji = "🎯")
            }
            Spacer(Modifier.height(20.dp))
        }

        // ── Calendar heatmap ──────────────────────────────────────────────────
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(CyberBgCard)
                    .border(1.dp, Color.White.copy(0.06f), RoundedCornerShape(20.dp))
                    .padding(16.dp)
            ) {
                // Month label
                val monthFmt = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Workout Calendar", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = CyberTextPrimary)
                    Text(monthFmt.format(Date()), fontSize = 12.sp, color = CyberTextMuted)
                }
                Spacer(Modifier.height(14.dp))

                // Day-of-week headers
                val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    dayLabels.forEach { label ->
                        Text(
                            label,
                            modifier = Modifier.weight(1f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyberTextMuted,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))

                // Calendar grid — 5 weeks × 7 days = 35 cells
                WorkoutCalendarGrid(
                    dayMap = dayMap,
                    selectedKey = selectedDateKey,
                    onDayClick = { key ->
                        selectedDateKey = if (selectedDateKey == key) "" else key
                    }
                )

                Spacer(Modifier.height(12.dp))

                // Legend
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("No workout", fontSize = 10.sp, color = CyberTextMuted)
                    Spacer(Modifier.width(8.dp))
                    listOf(
                        CyberBgCardElevated,
                        CyberAccent.copy(alpha = 0.3f),
                        CyberAccent.copy(alpha = 0.65f),
                        CyberAccent
                    ).forEach { color ->
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 3.dp)
                                .size(12.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(color)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text("3+ workouts", fontSize = 10.sp, color = CyberTextMuted)
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        // ── Selected day detail ───────────────────────────────────────────────
        if (selectedDateKey.isNotEmpty()) {
            item {
                val cal = Calendar.getInstance().also {
                    it.time = dateFmt.parse(selectedDateKey) ?: Date()
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(CyberAccent.copy(0.08f))
                        .border(1.dp, CyberAccent.copy(0.3f), RoundedCornerShape(18.dp))
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            displayFmt.format(cal.time),
                            fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = CyberAccent
                        )
                        Text(
                            if (selectedLogs.isEmpty()) "No workouts" else "${selectedLogs.size} workout${if (selectedLogs.size > 1) "s" else ""}",
                            fontSize = 12.sp, color = CyberTextMuted
                        )
                    }

                    if (selectedLogs.isEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text("Rest day 😴", fontSize = 13.sp, color = CyberTextMuted)
                    } else {
                        Spacer(Modifier.height(10.dp))
                        selectedLogs.forEach { log ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(CyberBgCard)
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text("🏋️", fontSize = 18.sp)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(log.exerciseName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CyberTextPrimary)
                                    val detail = buildString {
                                        append("${log.setsCompleted} sets × ${log.repsCompleted} reps")
                                        if (log.weightKg > 0f) append(" @ ${log.weightKg}kg")
                                    }
                                    Text(detail, fontSize = 12.sp, color = CyberTextSecondary)
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }

        // ── Goals ─────────────────────────────────────────────────────────────
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(CyberBgCard)
                    .border(1.dp, Color.White.copy(0.06f), RoundedCornerShape(20.dp))
                    .padding(16.dp)
            ) {
                Text("Your Goals", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = CyberTextPrimary)
                Spacer(Modifier.height(12.dp))

                if (goals.isEmpty()) {
                    Text(
                        "No goals set yet.\nOpen any exercise → tap \"Set as Goal\".",
                        fontSize = 13.sp, color = CyberTextMuted, lineHeight = 20.sp
                    )
                } else {
                    goals.forEach { goal ->
                        GoalRow(goal = goal, onDelete = { viewModel.deleteGoal(goal.exerciseId) })
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }

        // ── Recent Workouts ───────────────────────────────────────────────────
        item {
            Text(
                "Recent Workouts",
                fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = CyberTextPrimary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        if (isLoading && logs.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = CyberAccent, modifier = Modifier.size(28.dp))
                }
            }
        } else if (logs.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(CyberBgCard)
                        .border(1.dp, Color.White.copy(0.06f), RoundedCornerShape(16.dp))
                        .padding(28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🏋️", fontSize = 36.sp)
                        Spacer(Modifier.height(10.dp))
                        Text("No workouts logged yet.", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = CyberTextPrimary)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Go to Fitness → pick a category → open an exercise → tap \"Log This Workout\"",
                            fontSize = 12.sp, color = CyberTextMuted, textAlign = TextAlign.Center, lineHeight = 18.sp
                        )
                    }
                }
            }
        } else {
            var lastDateKey = ""
            logs.take(20).forEach { log ->
                val dateKey = dateFmt.format(Date(log.dateMillis))
                if (dateKey != lastDateKey) {
                    lastDateKey = dateKey
                    item {
                        Text(
                            displayFmt.format(Date(log.dateMillis)),
                            fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CyberAccent,
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp)
                        )
                    }
                }
                item(key = log.id) { WorkoutLogRow(log = log) }
            }
        }
    }
}

// ─── Calendar grid ────────────────────────────────────────────────────────────

@Composable
private fun WorkoutCalendarGrid(
    dayMap: Map<String, Int>,
    selectedKey: String,
    onDayClick: (String) -> Unit
) {
    val today   = Calendar.getInstance()
    val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val todayKey = dateFmt.format(today.time)

    // Build 35 days: 4 completed weeks + current partial week
    // Start from the Monday of 4 weeks ago
    val startCal = today.clone() as Calendar
    startCal.add(Calendar.DAY_OF_YEAR, -34)

    val days = (0..34).map { offset ->
        val cal = startCal.clone() as Calendar
        cal.add(Calendar.DAY_OF_YEAR, offset)
        cal
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        days.chunked(7).forEach { week ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                week.forEach { cal ->
                    val dateKey  = dateFmt.format(cal.time)
                    val count    = dayMap[dateKey] ?: 0
                    val isFuture = cal.after(today)
                    val isToday  = dateKey == todayKey
                    val isSelected = dateKey == selectedKey
                    val dayNum   = cal.get(Calendar.DAY_OF_MONTH)

                    val bgColor = when {
                        isFuture -> CyberBgCardElevated.copy(alpha = 0.25f)
                        count == 0 -> CyberBgCardElevated
                        count == 1 -> CyberAccent.copy(alpha = 0.3f)
                        count == 2 -> CyberAccent.copy(alpha = 0.65f)
                        else       -> CyberAccent
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(bgColor)
                            .then(
                                if (isToday) Modifier.border(2.dp, CyberAccent, RoundedCornerShape(8.dp))
                                else if (isSelected) Modifier.border(2.dp, Color.White, RoundedCornerShape(8.dp))
                                else Modifier
                            )
                            .clickable(enabled = !isFuture) { onDayClick(dateKey) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "$dayNum",
                            fontSize = 9.sp,
                            fontWeight = if (isToday || count > 0) FontWeight.Bold else FontWeight.Normal,
                            color = when {
                                isFuture      -> CyberTextMuted.copy(alpha = 0.2f)
                                count >= 3    -> Color.Black.copy(alpha = 0.85f)
                                count > 0     -> CyberTextPrimary
                                else          -> CyberTextMuted
                            },
                            textAlign = TextAlign.Center
                        )
                    }
                }
                repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

// ─── Helper composables ───────────────────────────────────────────────────────

@Composable
private fun StatItem(value: String, label: String, emoji: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, fontSize = 18.sp)
        Spacer(Modifier.height(4.dp))
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = CyberAccent)
        Text(label, fontSize = 10.sp, color = CyberTextMuted, textAlign = TextAlign.Center)
    }
}

@Composable
private fun VerticalDivider() {
    Box(
        modifier = Modifier
            .height(36.dp)
            .width(1.dp)
            .background(Color.White.copy(alpha = 0.08f))
    )
}

@Composable
private fun GoalRow(goal: FitnessGoalEntry, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CyberBgCardElevated)
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("🎯", fontSize = 16.sp)
            Column {
                Text(goal.exerciseName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CyberTextPrimary)
                val weight = if (goal.targetWeightKg > 0f) " @ ${goal.targetWeightKg}kg" else ""
                Text(
                    "${goal.targetSets} sets × ${goal.targetReps} reps$weight",
                    fontSize = 12.sp, color = CyberTextMuted
                )
            }
        }
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(CyberDanger.copy(alpha = 0.12f))
                .clickable { onDelete() },
            contentAlignment = Alignment.Center
        ) {
            Text("✕", fontSize = 12.sp, color = CyberDanger, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun WorkoutLogRow(log: WorkoutLogEntry) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(CyberBgCard)
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(CyberAccent.copy(0.10f)),
            contentAlignment = Alignment.Center
        ) {
            Text("🏋️", fontSize = 16.sp)
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(log.exerciseName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CyberTextPrimary)
            val detail = buildString {
                append("${log.setsCompleted} sets × ${log.repsCompleted} reps")
                if (log.weightKg > 0f) append(" @ ${log.weightKg}kg")
            }
            Text(detail, fontSize = 12.sp, color = CyberTextSecondary)
            if (log.notes.isNotBlank()) {
                Text(log.notes, fontSize = 11.sp, color = CyberTextMuted, lineHeight = 16.sp)
            }
        }
    }
}

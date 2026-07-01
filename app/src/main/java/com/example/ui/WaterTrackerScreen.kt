package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppDatabase
import com.example.data.UserPreferences
import com.example.data.WaterEntry
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val TIME_FMT = SimpleDateFormat("hh:mm a", Locale.getDefault())

private fun startOfToday(): Long = Calendar.getInstance().apply {
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

@Composable
fun WaterTrackerScreen(
    onBack: () -> Unit,
    userPreferences: UserPreferences,
    db: AppDatabase
) {
    val scope = rememberCoroutineScope()
    val dao = remember { db.waterDao() }
    val startOfDay = remember { startOfToday() }

    val todayEntries by dao.getTodayEntries(startOfDay).collectAsState(initial = emptyList())
    val todayTotalMl by dao.getTodayTotal(startOfDay).collectAsState(initial = 0)

    var goalMl by remember { mutableIntStateOf(userPreferences.dailyWaterGoalMl) }
    var showGoalDialog by remember { mutableStateOf(false) }
    var showSuccessFlash by remember { mutableStateOf(false) }
    var flashAmount by remember { mutableIntStateOf(0) }

    // Animated progress fraction
    val progressFraction by animateFloatAsState(
        targetValue = if (goalMl > 0) (todayTotalMl.toFloat() / goalMl).coerceIn(0f, 1f) else 0f,
        animationSpec = tween(durationMillis = 800),
        label = "water_progress"
    )

    fun addWater(amountMl: Int) {
        scope.launch {
            dao.insert(WaterEntry(amountMl = amountMl))
            flashAmount = amountMl
            showSuccessFlash = true
            delay(1800)
            showSuccessFlash = false
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(CyberBgPrimary)) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // ── Header ────────────────────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(CyberBgCard)
                            .clickable { onBack() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = CyberTextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Water Tracker",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = CyberTextPrimary
                        )
                        Text(
                            "Daily hydration goal",
                            fontSize = 12.sp,
                            color = CyberTextMuted
                        )
                    }
                    // Change goal button
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(CyberBgCard)
                            .border(1.dp, CyberAccent.copy(alpha = 0.3f), CircleShape)
                            .clickable { showGoalDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Edit,
                            contentDescription = "Change goal",
                            tint = CyberAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // ── Progress Ring ─────────────────────────────────────────────────
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier.size(220.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.size(220.dp)) {
                            val strokeWidth = 22.dp.toPx()
                            val inset = strokeWidth / 2f
                            val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                            val topLeft = Offset(inset, inset)
                            val startAngle = 135f
                            val sweepAngle = 270f

                            // Background track
                            drawArc(
                                color = Color.White.copy(alpha = 0.08f),
                                startAngle = startAngle,
                                sweepAngle = sweepAngle,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )

                            // Filled arc
                            if (progressFraction > 0f) {
                                drawArc(
                                    color = CyberAccent,
                                    startAngle = startAngle,
                                    sweepAngle = sweepAngle * progressFraction,
                                    useCenter = false,
                                    topLeft = topLeft,
                                    size = arcSize,
                                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                )
                            }
                        }

                        // Center content
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                "${(progressFraction * 100).toInt()}%",
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Black,
                                color = CyberTextPrimary
                            )
                            Text(
                                "$todayTotalMl / $goalMl ml",
                                fontSize = 13.sp,
                                color = CyberTextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                if (todayTotalMl >= goalMl) "Goal reached!" else "${(goalMl - todayTotalMl)} ml to go",
                                fontSize = 11.sp,
                                color = if (todayTotalMl >= goalMl) CyberSuccess else CyberTextMuted
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Big bold ml label
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("💧", fontSize = 24.sp)
                        Text(
                            "$todayTotalMl ml today",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = CyberTextPrimary
                        )
                    }
                }
            }

            // ── Quick-add buttons ─────────────────────────────────────────────
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                    Text(
                        "Quick Add",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CyberTextMuted,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val quickAmounts = listOf(150, 250, 500, 1000)
                        quickAmounts.forEach { amount ->
                            QuickAddPill(
                                amount = amount,
                                modifier = Modifier.weight(1f),
                                onClick = { addWater(amount) }
                            )
                        }
                    }
                }
            }

            // ── Today's log header ─────────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Today's Log",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberTextPrimary
                    )
                    if (todayEntries.isNotEmpty()) {
                        Text(
                            "${todayEntries.size} entries",
                            fontSize = 12.sp,
                            color = CyberTextMuted
                        )
                    }
                }
            }

            // ── Empty state ────────────────────────────────────────────────────
            if (todayEntries.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("💧", fontSize = 48.sp)
                            Text(
                                "No water logged yet",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyberTextSecondary
                            )
                            Text(
                                "Tap a quick-add button to get started",
                                fontSize = 13.sp,
                                color = CyberTextMuted,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // ── Log entries ────────────────────────────────────────────────────
            items(todayEntries, key = { it.id }) { entry ->
                WaterLogItem(
                    entry = entry,
                    onDelete = { scope.launch { dao.delete(entry.id) } },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )
            }

            // ── Clear all button (only when there are entries) ─────────────────
            if (todayEntries.isNotEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        TextButton(
                            onClick = { scope.launch { dao.clearToday(startOfDay) } },
                            colors = ButtonDefaults.textButtonColors(contentColor = CyberDanger.copy(alpha = 0.7f))
                        ) {
                            Text("Clear all today's entries", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // ── Success flash toast ────────────────────────────────────────────────
        AnimatedVisibility(
            visible = showSuccessFlash,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(400)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 32.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50.dp))
                    .background(CyberAccent)
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("💧", fontSize = 16.sp)
                    Text(
                        "+$flashAmount ml added!",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
            }
        }
    }

    // ── Goal setting dialog ────────────────────────────────────────────────────
    if (showGoalDialog) {
        ChangeGoalDialog(
            currentGoal = goalMl,
            onDismiss = { showGoalDialog = false },
            onConfirm = { newGoal ->
                goalMl = newGoal
                userPreferences.dailyWaterGoalMl = newGoal
                showGoalDialog = false
            }
        )
    }
}

// ── Quick add pill button ──────────────────────────────────────────────────────

@Composable
private fun QuickAddPill(
    amount: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val label = when {
        amount >= 1000 -> "${amount / 1000}L"
        else -> "${amount}ml"
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50.dp))
            .background(CyberBgCard)
            .border(1.dp, CyberAccent.copy(alpha = 0.25f), RoundedCornerShape(50.dp))
            .clickable { onClick() }
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text("💧", fontSize = 16.sp)
            Text(
                "+$label",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = CyberAccent,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ── Water log entry row ────────────────────────────────────────────────────────

@Composable
private fun WaterLogItem(
    entry: com.example.data.WaterEntry,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val timeStr = remember(entry.timestampMillis) {
        TIME_FMT.format(Date(entry.timestampMillis))
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CyberBgCard)
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Drop icon circle
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(CyberAccent.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Text("💧", fontSize = 18.sp)
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                "${entry.amountMl} ml",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = CyberTextPrimary
            )
            Text(
                timeStr,
                fontSize = 12.sp,
                color = CyberTextMuted
            )
        }

        // Delete icon
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(CyberDanger.copy(alpha = 0.08f))
                .clickable { onDelete() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.Delete,
                contentDescription = "Delete entry",
                tint = CyberDanger.copy(alpha = 0.6f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// ── Change Goal Dialog ─────────────────────────────────────────────────────────

@Composable
private fun ChangeGoalDialog(
    currentGoal: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var text by remember { mutableStateOf(currentGoal.toString()) }
    val parsed = text.trim().toIntOrNull()
    val isValid = parsed != null && parsed in 500..10000

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CyberBgCard,
        titleContentColor = CyberTextPrimary,
        textContentColor = CyberTextSecondary,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("💧", fontSize = 20.sp)
                Text(
                    "Daily Water Goal",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyberTextPrimary
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Set your daily hydration target (ml).",
                    fontSize = 13.sp,
                    color = CyberTextMuted
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.filter { c -> c.isDigit() }.take(5) },
                    label = { Text("Goal (ml)", color = CyberTextMuted) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberAccent,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedTextColor = CyberTextPrimary,
                        unfocusedTextColor = CyberTextPrimary,
                        cursorColor = CyberAccent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                if (!isValid && text.isNotBlank()) {
                    Text(
                        "Enter a value between 500 and 10000 ml",
                        fontSize = 11.sp,
                        color = CyberDanger
                    )
                }
                // Preset chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(1500, 2000, 2500, 3000).forEach { preset ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    if (parsed == preset) CyberAccentDark else CyberBgCardElevated
                                )
                                .border(
                                    1.dp,
                                    if (parsed == preset) CyberAccent else Color.White.copy(alpha = 0.1f),
                                    RoundedCornerShape(20.dp)
                                )
                                .clickable { text = preset.toString() }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                "${preset / 1000.0}L",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (parsed == preset) CyberAccent else CyberTextMuted
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (isValid) onConfirm(parsed!!) },
                enabled = isValid
            ) {
                Text(
                    "Save",
                    color = if (isValid) CyberAccent else CyberTextMuted,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = CyberTextMuted)
            }
        }
    )
}

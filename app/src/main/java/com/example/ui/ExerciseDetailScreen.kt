package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.DifficultyLevel
import com.example.data.Exercise
import com.example.data.ExerciseRepository
import com.example.data.MuscleGroup
import com.example.data.WorkoutLogEntry
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import com.example.data.SetDetail
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private fun DifficultyLevel.detailLabel() = when (this) {
    DifficultyLevel.BEGINNER     -> "A Easy"
    DifficultyLevel.INTERMEDIATE -> "AA Middle"
    DifficultyLevel.ADVANCED     -> "AAA Hard"
}

@Composable
fun ExerciseDetailScreen(
    viewModel: FitnessViewModel,
    exerciseId: String,
    onBack: () -> Unit,
    isCoachMode: Boolean = false,
    onNavigate: (exerciseId: String) -> Unit = {}
) {
    LaunchedEffect(exerciseId) { viewModel.selectExercise(exerciseId) }

    val ex by viewModel.selectedExercise.collectAsState()
    val goals by viewModel.goals.collectAsState()
    val logs  by viewModel.logs.collectAsState()

    if (ex == null) {
        Box(Modifier.fillMaxSize().background(CyberBgPrimary), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = CyberAccent)
        }
        return
    }

    val exercise = ex!!
    val logsForThis = remember(logs, exerciseId) { logs.filter { it.exerciseId == exerciseId } }
    val goalForThis = remember(goals, exerciseId) { goals.find { it.exerciseId == exerciseId } }
    val progressPct = if (goalForThis != null && logsForThis.isNotEmpty()) {
        val latest = logsForThis.maxByOrNull { it.dateMillis }
        if (latest != null) {
            val target = goalForThis.targetReps.coerceAtLeast(1)
            ((latest.repsCompleted.toFloat() / target) * 100).coerceIn(0f, 100f).toInt()
        } else 0
    } else 0

    // Related exercises — same primary muscle, exclude self; uses live data so admin images show
    val allExercises by viewModel.allExercises.collectAsState()
    val related = remember(allExercises, exercise) {
        exercise.primaryMuscles.firstOrNull()?.let { muscle ->
            allExercises.filter { muscle in it.primaryMuscles && it.id != exercise.id }.take(4)
        } ?: emptyList()
    }

    val clipboardManager = LocalClipboardManager.current
    var showGuide     by remember { mutableStateOf(false) }
    var showMistakes  by remember { mutableStateOf(false) }
    var showLogDialog by remember { mutableStateOf(false) }
    var savedMsg      by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    if (showLogDialog) {
        LogWorkoutDialog(
            exercise      = exercise,
            previousLog   = logsForThis.firstOrNull(),
            onDismiss     = { showLogDialog = false },
            onLog         = { setDetails, notes ->
                viewModel.logWorkout(exercise.id, exercise.name, setDetails, notes)
                showLogDialog = false
                savedMsg = "Workout logged ✓"
                scope.launch { delay(2500); savedMsg = "" }
            },
            onGoal        = { sets, reps, weight ->
                viewModel.saveGoal(exercise.id, exercise.name, sets, reps, weight)
                showLogDialog = false
                savedMsg = "Goal saved ✓"
                scope.launch { delay(2500); savedMsg = "" }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(CyberBgPrimary)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            // ── Hero image ────────────────────────────────────────────────────
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                ) {
                    // Two layers so the media feels instant: the lightweight static
                    // thumbnail loads first, then the heavier animation GIF crossfades
                    // in on top once it's downloaded + decoded.
                    val hasMedia = exercise.imageUrl.isNotBlank() || exercise.gifUrl.isNotBlank()
                    if (hasMedia) {
                        if (exercise.imageUrl.isNotBlank()) {
                            AsyncImage(
                                model = exercise.imageUrl,
                                contentDescription = exercise.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        if (exercise.gifUrl.isNotBlank()) {
                            AsyncImage(
                                model = exercise.gifUrl,
                                contentDescription = exercise.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize()
                                .background(Brush.verticalGradient(listOf(Color(0xFF1A1A2E), Color(0xFF16213E)))),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(exercise.muscleEmoji, fontSize = 72.sp)
                        }
                    }

                    // Dark gradient overlay
                    Box(
                        modifier = Modifier.fillMaxSize().background(
                            Brush.verticalGradient(
                                0f    to Color.Black.copy(0.3f),
                                0.45f to Color.Transparent,
                                0.72f to Color.Black.copy(0.55f),
                                1f    to Color.Black.copy(0.93f)
                            )
                        )
                    )

                    // Required media credit (e.g. © Gym visual)
                    if (exercise.attribution.isNotBlank()) {
                        Text(
                            exercise.attribution,
                            fontSize = 9.sp,
                            color = Color.White.copy(0.65f),
                            modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp)
                        )
                    }

                    // Back button
                    Box(
                        modifier = Modifier
                            .statusBarsPadding()
                            .padding(16.dp)
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(0.45f))
                            .clickable { onBack() }
                            .align(Alignment.TopStart),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }

                    // Title + pills — bottom of hero
                    Column(
                        modifier = Modifier.align(Alignment.BottomStart).padding(horizontal = 16.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            exercise.name,
                            fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, lineHeight = 26.sp
                        )
                        exercise.sanskritName?.let {
                            Text(it, fontSize = 13.sp, color = Color.White.copy(0.7f), fontWeight = FontWeight.SemiBold)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            HeroPill(icon = {
                                Icon(Icons.Filled.PlayArrow, null, tint = Color.White, modifier = Modifier.size(13.dp))
                            }, label = "${exercise.estimatedMinutes} min")
                            HeroPill(icon = {
                                Icon(Icons.Filled.LocalFireDepartment, null, tint = Color.White, modifier = Modifier.size(13.dp))
                            }, label = exercise.caloriesBurned.replace("~", "").replace(" kcal/min", " kcal/min"))
                        }
                    }
                }
            }

            // ── About ─────────────────────────────────────────────────────────
            item {
                DetailSection(title = "About") {
                    Text(
                        exercise.bodyEffect,
                        fontSize = 14.sp, color = CyberTextSecondary, lineHeight = 22.sp
                    )
                }
            }

            // ── Level / Progress / Focus Area ─────────────────────────────────
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(CyberBgCard)
                        .border(1.dp, Color.White.copy(0.06f), RoundedCornerShape(18.dp))
                ) {
                    StatColumn("Level", exercise.difficulty.detailLabel(), CyberAccent, Modifier.weight(1f))
                    Box(Modifier.width(1.dp).height(60.dp).background(Color.White.copy(0.07f)).align(Alignment.CenterVertically))
                    StatColumn("Progress", "$progressPct%", Color.White, Modifier.weight(1f))
                    Box(Modifier.width(1.dp).height(60.dp).background(Color.White.copy(0.07f)).align(Alignment.CenterVertically))
                    StatColumn(
                        "Focus Area",
                        exercise.primaryMuscles.firstOrNull()?.label ?: "Full Body",
                        CyberAccent, Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(12.dp))
            }

            // ── Guide (expandable how-to) ─────────────────────────────────────
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    NavRow(
                        icon = { Icon(Icons.AutoMirrored.Filled.MenuBook, null, tint = CyberTextMuted, modifier = Modifier.size(18.dp)) },
                        title = "Guide",
                        subtitle = "${exercise.howTo.size} steps",
                        expanded = showGuide
                    ) { showGuide = !showGuide }

                    AnimatedVisibility(showGuide, enter = expandVertically(), exit = shrinkVertically()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                                .background(CyberBgCard)
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            exercise.howTo.forEachIndexed { index, step ->
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(CyberAccent),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "${index + 1}",
                                            fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = CyberAccentDark
                                        )
                                    }
                                    Text(step, fontSize = 13.sp, color = CyberTextSecondary, lineHeight = 20.sp, modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))

                    // Common mistakes
                    NavRow(
                        icon = { Icon(Icons.Filled.Warning, null, tint = CyberDanger, modifier = Modifier.size(18.dp)) },
                        title = "Common Mistakes",
                        subtitle = "${exercise.commonErrors.size} to avoid",
                        expanded = showMistakes
                    ) { showMistakes = !showMistakes }

                    AnimatedVisibility(showMistakes, enter = expandVertically(), exit = shrinkVertically()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                                .background(CyberBgCard)
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            exercise.commonErrors.forEachIndexed { index, mistake ->
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(CyberDanger.copy(0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "${index + 1}",
                                            fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = CyberDanger
                                        )
                                    }
                                    Text(mistake, fontSize = 13.sp, color = CyberTextSecondary, lineHeight = 20.sp, modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // ── Trainer card (linked coach or generic) ────────────────────────
            item {
                DetailSection(title = "Trainer") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(CyberBgCardElevated)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(52.dp).clip(CircleShape)
                                .background(CyberAccent.copy(0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(exercise.category.emoji, fontSize = 22.sp)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("ProCoach India Team", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = CyberTextPrimary)
                            Text("${exercise.category.label} Specialist", fontSize = 12.sp, color = CyberTextMuted)
                            Spacer(Modifier.height(3.dp))
                            Text(
                                "Certified · ${exercise.equipment.label}",
                                fontSize = 12.sp, color = CyberAccent, fontWeight = FontWeight.SemiBold
                            )
                        }
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = CyberTextMuted, modifier = Modifier.size(16.dp))
                    }
                }
            }

            // ── Rating strip ──────────────────────────────────────────────────
            item {
                val totalLogs = logsForThis.size
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(CyberBgCard)
                        .border(1.dp, Color.White.copy(0.06f), RoundedCornerShape(18.dp))
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column {
                            Text(
                                if (totalLogs > 0) "$totalLogs" else "0",
                                fontSize = 42.sp, fontWeight = FontWeight.ExtraBold, color = CyberTextPrimary
                            )
                            Text("times logged", fontSize = 11.sp, color = CyberTextMuted)
                        }
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            repeat(5) { row ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text("${5 - row}", fontSize = 10.sp, color = CyberTextMuted, modifier = Modifier.width(10.dp))
                                    val barW = when (row) {
                                        0 -> 0.7f; 1 -> 0.5f; 2 -> 0.3f; 3 -> 0.15f; else -> 0.05f
                                    }
                                    Box(
                                        modifier = Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp))
                                            .background(Color.White.copy(0.07f))
                                    ) {
                                        Box(Modifier.fillMaxWidth(barW).height(6.dp).clip(RoundedCornerShape(3.dp)).background(CyberAccent))
                                    }
                                }
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("$totalLogs workout logs", fontSize = 11.sp, color = CyberTextMuted)
                        Text("Benefits", fontSize = 11.sp, color = CyberAccent, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // ── Latest log / review ───────────────────────────────────────────
            if (logsForThis.isNotEmpty()) {
                item {
                    val latest = logsForThis.maxByOrNull { it.dateMillis } ?: return@item
                    val fmt = java.text.SimpleDateFormat("d MMM", java.util.Locale.getDefault())
                    DetailSection(title = "Last Session") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(CyberBgCardElevated)
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier.size(44.dp).clip(CircleShape).background(CyberAccent.copy(0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🏋️", fontSize = 18.sp)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("You", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = CyberTextPrimary)
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Box(
                                            modifier = Modifier.clip(RoundedCornerShape(6.dp))
                                                .background(CyberAccent).padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Filled.Star, null, tint = CyberAccentDark, modifier = Modifier.size(10.dp))
                                                Spacer(Modifier.width(2.dp))
                                                Text("${latest.setsCompleted}×${latest.repsCompleted}", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = CyberAccentDark)
                                            }
                                        }
                                        Text(fmt.format(java.util.Date(latest.dateMillis)), fontSize = 11.sp, color = CyberTextMuted)
                                    }
                                }
                                Spacer(Modifier.height(4.dp))
                                val detail = buildString {
                                    append("Completed ${latest.setsCompleted} sets × ${latest.repsCompleted} reps")
                                    if (latest.weightKg > 0f) append(" @ ${latest.weightKg}kg")
                                }
                                Text(detail, fontSize = 13.sp, color = CyberTextSecondary, lineHeight = 18.sp)
                                if (latest.notes.isNotBlank()) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(latest.notes, fontSize = 12.sp, color = CyberTextMuted, lineHeight = 16.sp)
                                }
                            }
                        }
                    }
                }
            }

            // ── Related Exercises ─────────────────────────────────────────────
            if (related.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Related Exercises", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = CyberTextPrimary)
                                Text(
                                    "${related.size} exercises  ·  ${exercise.primaryMuscles.firstOrNull()?.label}",
                                    fontSize = 11.sp, color = CyberTextMuted
                                )
                            }
                        }
                    }
                }
                items(related, key = { it.id }) { rel ->
                    RelatedExerciseRow(rel, onClick = { onNavigate(rel.id) })
                }
            }

            item { Spacer(Modifier.height(8.dp)) }
        }

        // ── Sticky bottom bar ─────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(listOf(Color.Transparent, CyberBgPrimary.copy(0.98f), CyberBgPrimary))
                )
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (savedMsg.isNotEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(CyberSuccess.copy(0.15f)).padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(savedMsg, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CyberSuccess)
                }
            }
            if (isCoachMode) {
                // Coach: copy the exercise prescription to clipboard for sharing with client
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(CyberAccent)
                        .clickable {
                            val steps = exercise.howTo.mapIndexed { i, s -> "${i + 1}. $s" }.joinToString("\n")
                            val text = buildString {
                                appendLine("📋 ${exercise.name}")
                                appendLine("Category: ${exercise.category.label}  |  Level: ${exercise.difficulty.label}")
                                appendLine()
                                appendLine("Prescription:")
                                appendLine("  Sets:  ${exercise.sets}")
                                appendLine("  Reps:  ${exercise.reps}")
                                appendLine("  Tempo: ${exercise.tempo}")
                                appendLine("  Rest:  ${exercise.rest}")
                                appendLine("  Est.:  ${exercise.estimatedMinutes} min")
                                appendLine()
                                appendLine("How To:")
                                appendLine(steps)
                                if (exercise.commonErrors.isNotEmpty()) {
                                    appendLine()
                                    appendLine("Common Mistakes:")
                                    exercise.commonErrors.forEach { appendLine("• $it") }
                                }
                            }
                            clipboardManager.setText(AnnotatedString(text))
                            savedMsg = "Prescription copied to clipboard ✓"
                            scope.launch { delay(2500); savedMsg = "" }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("Copy Prescription", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = CyberAccentDark)
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(CyberAccent)
                        .clickable { showLogDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    Text("Log This Workout", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = CyberAccentDark)
                }
            }
        }
    }
}

// ─── Related exercise row ─────────────────────────────────────────────────────

@Composable
private fun RelatedExerciseRow(exercise: Exercise, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(CyberBgCard)
            .border(1.dp, Color.White.copy(0.05f), RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier.size(64.dp).clip(RoundedCornerShape(12.dp)),
        ) {
            if (exercise.imageUrl.isNotBlank()) {
                AsyncImage(
                    model = exercise.imageUrl,
                    contentDescription = exercise.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    Modifier.fillMaxSize().background(CyberAccent.copy(0.12f)),
                    contentAlignment = Alignment.Center
                ) { Text(exercise.muscleEmoji, fontSize = 24.sp) }
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(exercise.name, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = CyberAccent)
            Text(exercise.reps, fontSize = 12.sp, color = CyberTextMuted)
        }
    }
}

// ─── Small layout helpers ─────────────────────────────────────────────────────

@Composable
private fun HeroPill(icon: @Composable () -> Unit, label: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color.Black.copy(0.45f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        icon()
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
    }
}

@Composable
private fun StatColumn(label: String, value: String, valueColor: Color, modifier: Modifier) {
    Column(
        modifier = modifier.padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(label, fontSize = 11.sp, color = CyberTextMuted)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = valueColor)
    }
}

@Composable
private fun NavRow(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    expanded: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CyberBgCard)
            .border(1.dp, Color.White.copy(0.06f), RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            icon()
            Column {
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = CyberTextPrimary)
                Text(subtitle, fontSize = 11.sp, color = CyberTextMuted)
            }
        }
        Box(
            modifier = Modifier.size(28.dp).clip(RoundedCornerShape(8.dp))
                .background(CyberBgCardElevated),
            contentAlignment = Alignment.Center
        ) {
            Text(if (expanded) "∧" else "›", fontSize = 14.sp, color = CyberAccent, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun DetailSection(
    title: String,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(CyberBgCard)
            .border(1.dp, Color.White.copy(0.06f), RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        Text(title, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = CyberTextPrimary)
        Spacer(Modifier.height(10.dp))
        content()
    }
}

// ─── Multi-set workout log dialog ─────────────────────────────────────────────

@Composable
private fun LogWorkoutDialog(
    exercise: Exercise,
    previousLog: WorkoutLogEntry?,
    onDismiss: () -> Unit,
    onLog: (setDetails: List<SetDetail>, notes: String) -> Unit,
    onGoal: (sets: Int, reps: Int, weight: Float) -> Unit
) {
    // Seed from previous session or exercise defaults
    val defaultSets = when {
        previousLog?.setDetails?.isNotEmpty() == true ->
            previousLog.setDetails.map { it.copy() }.toMutableList()
        else -> {
            val count = exercise.sets.filter { it.isDigit() || it == '–' }
                .split("–").firstOrNull()?.trim()?.toIntOrNull() ?: 3
            val defaultReps = exercise.reps.filter { it.isDigit() || it == '–' }
                .split("–").firstOrNull()?.trim()?.toIntOrNull() ?: 10
            val defaultWeight = previousLog?.weightKg ?: 0f
            (1..count).map { i -> SetDetail(i - 1, defaultReps, defaultWeight) }.toMutableList()
        }
    }

    var sets  by remember { mutableStateOf(defaultSets.toList()) }
    var notes by remember { mutableStateOf("") }
    val isTimeBased = exercise.reps.contains("sec", ignoreCase = true) ||
                      exercise.category.name == "YOGA" ||
                      exercise.category.name == "FLEXIBILITY"

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(CyberBgCard)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(exercise.name, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = CyberTextPrimary)
                    if (previousLog != null) {
                        val prev = previousLog.setDetails.filter { !it.isWarmup }
                        val prevStr = if (prev.isNotEmpty())
                            "Last: ${prev.size}×${prev.first().reps}@ ${prev.first().weightKg}kg"
                        else "Last: ${previousLog.setsCompleted}×${previousLog.repsCompleted}@ ${previousLog.weightKg}kg"
                        Text(prevStr, fontSize = 11.sp, color = CyberAccent)
                    }
                }
                Box(Modifier.size(32.dp).clip(CircleShape).background(CyberBgCardElevated)
                    .clickable { onDismiss() }, contentAlignment = Alignment.Center) {
                    Text("×", fontSize = 16.sp, color = CyberTextMuted, fontWeight = FontWeight.Bold)
                }
            }

            // Column headers
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text("SET",   fontSize = 10.sp, color = CyberTextMuted, modifier = Modifier.width(36.dp))
                Text(if (isTimeBased) "SECS" else "REPS", fontSize = 10.sp, color = CyberTextMuted, modifier = Modifier.weight(1f))
                Text("KG",   fontSize = 10.sp, color = CyberTextMuted, modifier = Modifier.weight(1f))
                Text("RPE",  fontSize = 10.sp, color = CyberTextMuted, modifier = Modifier.weight(0.8f))
                Spacer(Modifier.width(28.dp))
            }

            // Set rows (scrollable if many)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.heightIn(max = 260.dp)) {
                sets.forEachIndexed { idx, set ->
                    SetRow(
                        set = set,
                        isTimeBased = isTimeBased,
                        onChange = { updated ->
                            sets = sets.toMutableList().also { it[idx] = updated }
                        },
                        onDelete = if (sets.size > 1) ({
                            sets = sets.toMutableList().also { it.removeAt(idx) }
                                .mapIndexed { i, s -> s.copy(setIndex = i) }
                        }) else null
                    )
                }
            }

            // Add set buttons
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Add working set (copies last set's values)
                Box(
                    modifier = Modifier.weight(1f).height(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(CyberAccent.copy(0.15f))
                        .clickable {
                            val last = sets.lastOrNull { !it.isWarmup }
                            sets = sets + SetDetail(
                                setIndex = sets.size,
                                reps     = last?.reps ?: 10,
                                weightKg = last?.weightKg ?: 0f
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("+ Working Set", fontSize = 12.sp, color = CyberAccent, fontWeight = FontWeight.Bold)
                }
                // Add warmup set
                Box(
                    modifier = Modifier.weight(1f).height(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(0.06f))
                        .clickable {
                            val warmupIdx = sets.indexOfFirst { !it.isWarmup }.coerceAtLeast(0)
                            val list = sets.toMutableList()
                            list.add(warmupIdx, SetDetail(warmupIdx, reps = 15, weightKg = 0f, isWarmup = true))
                            sets = list.mapIndexed { i, s -> s.copy(setIndex = i) }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("+ Warmup", fontSize = 12.sp, color = CyberTextMuted, fontWeight = FontWeight.SemiBold)
                }
            }

            // Notes
            OutlinedTextField(
                value = notes, onValueChange = { notes = it },
                placeholder = { Text("Notes (optional)", fontSize = 12.sp, color = CyberTextMuted) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp), maxLines = 2,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyberAccent, unfocusedBorderColor = Color.White.copy(0.1f),
                    focusedTextColor = CyberTextPrimary, unfocusedTextColor = CyberTextPrimary,
                    focusedContainerColor = CyberBgCard, unfocusedContainerColor = CyberBgCard,
                    cursorColor = CyberAccent
                )
            )

            // Action buttons
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                val working = sets.filter { !it.isWarmup }
                Box(
                    modifier = Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(12.dp))
                        .background(CyberBgCardElevated)
                        .clickable {
                            val w = working
                            if (w.isNotEmpty()) onGoal(w.size, w.first().reps, w.first().weightKg)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("Set as Goal", fontSize = 13.sp, color = CyberAccent, fontWeight = FontWeight.Bold)
                }
                Box(
                    modifier = Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(12.dp))
                        .background(CyberAccent)
                        .clickable { onLog(sets, notes) },
                    contentAlignment = Alignment.Center
                ) {
                    Text("Save Log", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = CyberAccentDark)
                }
            }
        }
    }
}

// ── Individual set row ────────────────────────────────────────────────────────

@Composable
private fun SetRow(
    set: SetDetail,
    isTimeBased: Boolean,
    onChange: (SetDetail) -> Unit,
    onDelete: (() -> Unit)?
) {
    val bg = if (set.isWarmup) Color.White.copy(0.04f) else CyberBgCardElevated

    Row(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Set label
        Box(
            modifier = Modifier.width(36.dp).height(28.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(if (set.isWarmup) Color.White.copy(0.08f) else CyberAccent.copy(0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                if (set.isWarmup) "W" else "${set.setIndex + 1}",
                fontSize = 11.sp, fontWeight = FontWeight.ExtraBold,
                color = if (set.isWarmup) CyberTextMuted else CyberAccent
            )
        }

        // Reps / seconds
        SetMiniField(
            value = if (isTimeBased) set.durationSecs.toString() else set.reps.toString(),
            modifier = Modifier.weight(1f)
        ) { v ->
            val n = v.toIntOrNull() ?: 0
            onChange(if (isTimeBased) set.copy(durationSecs = n) else set.copy(reps = n))
        }

        // Weight
        SetMiniField(
            value = if (set.weightKg == 0f) "" else set.weightKg.toString(),
            placeholder = "0",
            modifier = Modifier.weight(1f),
            keyboardType = KeyboardType.Decimal
        ) { v -> onChange(set.copy(weightKg = v.toFloatOrNull() ?: 0f)) }

        // RPE (1–10)
        SetMiniField(
            value = if (set.rpe == 0) "" else set.rpe.toString(),
            placeholder = "—",
            modifier = Modifier.weight(0.8f)
        ) { v -> onChange(set.copy(rpe = (v.toIntOrNull() ?: 0).coerceIn(0, 10))) }

        // Delete
        Box(
            modifier = Modifier.size(28.dp)
                .clip(CircleShape)
                .background(if (onDelete != null) CyberDanger.copy(0.12f) else Color.Transparent)
                .clickable(enabled = onDelete != null) { onDelete?.invoke() },
            contentAlignment = Alignment.Center
        ) {
            if (onDelete != null)
                Text("×", fontSize = 14.sp, color = CyberDanger, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun SetMiniField(
    value: String,
    placeholder: String = "",
    modifier: Modifier,
    keyboardType: KeyboardType = KeyboardType.Number,
    onValueChange: (String) -> Unit
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.height(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(0.06f))
            .padding(horizontal = 6.dp, vertical = 4.dp),
        textStyle = androidx.compose.ui.text.TextStyle(
            color = CyberTextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        ),
        cursorBrush = SolidColor(CyberAccent),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        decorationBox = { inner ->
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                if (value.isEmpty()) Text(placeholder, fontSize = 12.sp, color = CyberTextMuted,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                inner()
            }
        }
    )
}

@Composable
private fun NumberField(label: String, value: String, modifier: Modifier, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange,
        label = { Text(label, fontSize = 11.sp) },
        modifier = modifier,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = CyberAccent, unfocusedBorderColor = Color.White.copy(0.12f),
            focusedTextColor = CyberTextPrimary, unfocusedTextColor = CyberTextPrimary,
            focusedLabelColor = CyberAccent, unfocusedLabelColor = CyberTextMuted,
            focusedContainerColor = CyberBgCard, unfocusedContainerColor = CyberBgCard,
            cursorColor = CyberAccent
        )
    )
}

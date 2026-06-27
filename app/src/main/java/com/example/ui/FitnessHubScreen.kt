package com.example.ui

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.example.ui.theme.CyberBgCardElevated
import com.example.ui.theme.CyberDanger
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.ExerciseCategory
import com.example.data.ExerciseRepository
import com.example.data.WorkoutLogEntry
import com.example.ui.theme.CyberAccent
import com.example.ui.theme.CyberAccentDark
import com.example.ui.theme.CyberBgCard
import com.example.ui.theme.CyberBgPrimary
import com.example.ui.theme.CyberTextMuted
import com.example.ui.theme.CyberTextPrimary
import com.example.ui.theme.CyberTextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val catImages = mapOf(
    ExerciseCategory.STRENGTH    to "https://images.unsplash.com/photo-1534438327276-14e5300c3a48?w=500&h=340&fit=crop&q=80",
    ExerciseCategory.YOGA        to "https://images.unsplash.com/photo-1506126613408-eca07ce68773?w=500&h=340&fit=crop&q=80",
    ExerciseCategory.CARDIO      to "https://images.unsplash.com/photo-1571019613454-1cb2f99b2d8b?w=500&h=340&fit=crop&q=80",
    ExerciseCategory.HIIT        to "https://images.unsplash.com/photo-1549060279-7e168fcee0c2?w=500&h=340&fit=crop&q=80",
    ExerciseCategory.FLEXIBILITY to "https://images.unsplash.com/photo-1552196563-55cd4e45efb3?w=500&h=340&fit=crop&q=80"
)

private val hubCatColors = mapOf(
    ExerciseCategory.STRENGTH    to Color(0xFF6366F1),
    ExerciseCategory.YOGA        to Color(0xFF10B981),
    ExerciseCategory.CARDIO      to Color(0xFFF59E0B),
    ExerciseCategory.HIIT        to Color(0xFFEF4444),
    ExerciseCategory.FLEXIBILITY to Color(0xFF8B5CF6)
)

@Composable
fun FitnessHubScreen(
    viewModel: FitnessViewModel,
    healthViewModel: HealthViewModel? = null,
    onCategoryClick: (ExerciseCategory) -> Unit,
    onNutritionClick: () -> Unit,
    onProgressClick: () -> Unit,
    onHealthMetricsClick: () -> Unit = {},
    onMyDietClick: () -> Unit = {},
    onFoodScanClick: () -> Unit = {},
    onBodyMeasurementsClick: () -> Unit = {},
    onProgressPhotosClick: () -> Unit = {},
    onCycleTrackerClick: () -> Unit = {}
) {
    val logs  by viewModel.logs.collectAsState()
    val goals by viewModel.goals.collectAsState()

    val rawName: String = viewModel.clientName
    val clientName: String = (if (rawName.isBlank()) "there" else rawName).split(" ").first()

    // Activity metrics from workout logs
    val dayMillis  = 24 * 3600_000L
    val todayLogs  = remember(logs) { logs.filter { System.currentTimeMillis() - it.dateMillis < dayMillis } }
    val todayCount = todayLogs.size
    val dailyGoal  = 3   // target exercises per day
    // Keep week logs for calorie estimate
    val weekMillis = 7 * 24 * 3600_000L
    val weekLogs   = remember(logs) { logs.filter { System.currentTimeMillis() - it.dateMillis < weekMillis } }

    // Live exercise list — used for calorie estimates and category counts
    val allExercises by viewModel.allExercises.collectAsState()

    // Estimated calories from logs
    val weekCalories = remember(weekLogs, allExercises) {
        weekLogs.sumOf { log ->
            val ex = allExercises.find { it.id == log.exerciseId }
            val kcalPerMin = ex?.caloriesBurned
                ?.let { Regex("(\\d+)").find(it.removePrefix("~"))?.value?.toIntOrNull() ?: 5 } ?: 5
            kcalPerMin * (log.setsCompleted.coerceAtLeast(1) * 3)
        }
    }
    val calGoal = 2000

    // Streak
    val dateFmt = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val streak = remember(logs) {
        val dayMap = logs.groupBy { dateFmt.format(Date(it.dateMillis)) }.mapValues { it.value.size }
        val today = java.util.Calendar.getInstance()
        var s = 0
        val checking = today.clone() as java.util.Calendar
        while (true) {
            if ((dayMap[dateFmt.format(checking.time)] ?: 0) > 0) {
                s++; checking.add(java.util.Calendar.DAY_OF_YEAR, -1)
            } else break
        }
        s
    }

    val recentLogs = remember(logs) { logs.take(5) }
    val timeFmt = remember { SimpleDateFormat("EEE, d MMM", Locale.getDefault()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBgPrimary)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // ── Greeting header ───────────────────────────────────────────────────
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Avatar circle — reactive StateFlow, updates without restart
                    val photoUrl by viewModel.profilePhotoUrl.collectAsState()
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(CyberAccent.copy(0.15f))
                            .border(2.dp, CyberAccent.copy(0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (photoUrl.isNotBlank()) {
                            AsyncImage(
                                model = photoUrl,
                                contentDescription = "Profile photo",
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                modifier = Modifier.size(46.dp).clip(CircleShape)
                            )
                        } else {
                            Text(
                                clientName.first().uppercaseChar().toString(),
                                fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = CyberAccent
                            )
                        }
                        // Green online dot
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981))
                                .border(2.dp, CyberBgPrimary, CircleShape)
                                .align(Alignment.BottomEnd)
                        )
                    }
                    Column {
                        Text(
                            "Hello $clientName!",
                            fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = CyberTextPrimary
                        )
                        Text("Let's start your day", fontSize = 12.sp, color = CyberTextMuted)
                    }
                }
                // Trophy / streak icon
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(CyberAccent.copy(0.12f))
                        .border(1.dp, CyberAccent.copy(0.3f), CircleShape)
                        .clickable { onProgressClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.EmojiEvents, null, tint = CyberAccent, modifier = Modifier.size(20.dp))
                }
            }
        }

        // ── Streak / weekly progress card ─────────────────────────────────────
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(CyberBgCard)
                    .border(1.dp, Color.White.copy(0.06f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 18.dp, vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Today's Exercises", fontSize = 13.sp, color = CyberTextMuted)
                        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                "$todayCount",
                                fontSize = 28.sp, fontWeight = FontWeight.ExtraBold,
                                color = if (todayCount >= dailyGoal) CyberAccent else CyberTextPrimary
                            )
                            Text(
                                "/ $dailyGoal",
                                fontSize = 16.sp, color = CyberTextMuted, fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("🔥 $streak day streak", fontSize = 13.sp, fontWeight = FontWeight.Bold,
                            color = if (streak > 0) CyberAccent else CyberTextMuted)
                        Text(
                            if (todayCount >= dailyGoal) "Daily goal hit! 🎉" else "${dailyGoal - todayCount} more today",
                            fontSize = 11.sp, color = CyberTextMuted
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { (todayCount.toFloat() / dailyGoal).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = if (todayCount >= dailyGoal) CyberAccent else CyberAccent.copy(0.7f),
                    trackColor = CyberBgCardElevated,
                    strokeCap = StrokeCap.Round
                )
            }
            Spacer(Modifier.height(14.dp))
        }

        // ── Daily Activity card with activity ring ────────────────────────────
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(CyberBgCard)
                    .border(1.dp, Color.White.copy(0.06f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Daily Activity", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = CyberTextPrimary)
                        Text(
                            "See all",
                            fontSize = 12.sp, color = CyberAccent, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable { onProgressClick() }
                        )
                    }
                    ActivityStatRow(label = "Today", value = "$todayCount", goal = "$dailyGoal", color = CyberAccent)
                    ActivityStatRow(label = "Calories", value = "$weekCalories", goal = "${calGoal} Cal", color = Color(0xFFEF4444))
                    ActivityStatRow(label = "Streak", value = "$streak", goal = "days", color = Color(0xFF3B82F6))
                }

                Spacer(Modifier.width(20.dp))

                // Activity rings
                ActivityRing(
                    workoutsProgress = (todayCount.toFloat() / dailyGoal).coerceIn(0f, 1f),
                    caloriesProgress = (weekCalories.toFloat() / calGoal).coerceIn(0f, 1f),
                    streakProgress   = (streak.toFloat() / 7).coerceIn(0f, 1f),
                    modifier = Modifier.size(110.dp)
                )
            }
            Spacer(Modifier.height(14.dp))
        }

        // ── Recent Workouts ───────────────────────────────────────────────────
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Workouts", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = CyberTextPrimary)
                Text(
                    "See all",
                    fontSize = 12.sp, color = CyberAccent, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { onProgressClick() }
                )
            }
            Spacer(Modifier.height(8.dp))
        }

        if (recentLogs.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(CyberBgCard)
                        .border(1.dp, Color.White.copy(0.06f), RoundedCornerShape(16.dp))
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No workouts logged yet.\nGo to Browse to get started →", fontSize = 13.sp, color = CyberTextMuted)
                }
            }
        } else {
            items(recentLogs, key = { it.id }) { log ->
                RecentWorkoutRow(log = log, timeFmt = timeFmt, allExercises = allExercises)
            }
        }

        item { Spacer(Modifier.height(20.dp)) }

        // ── BMI & Health Metrics banner ───────────────────────────────────────
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        androidx.compose.ui.graphics.Brush.horizontalGradient(
                            listOf(Color(0xFF1E1B4B), Color(0xFF312E81))
                        )
                    )
                    .border(1.dp, Color(0xFF818CF8).copy(0.35f), RoundedCornerShape(18.dp))
                    .clickable { onHealthMetricsClick() }
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("⚖️", fontSize = 28.sp)
                Column(Modifier.weight(1f)) {
                    Text("BMI · Body Fat % · Nutrition Plan",
                        fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    Text("Calculate your daily protein, calories & macros",
                        fontSize = 11.sp, color = Color.White.copy(0.6f))
                }
                Text("›", fontSize = 22.sp, color = Color(0xFF818CF8), fontWeight = FontWeight.ExtraBold)
            }
            Spacer(Modifier.height(20.dp))
        }

        // ── Browse categories (horizontal scroll, image cards) ───────────────
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Browse", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = CyberTextPrimary)
                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = CyberTextMuted, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.height(10.dp))
        }

        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(ExerciseCategory.entries) { cat ->
                    val color    = hubCatColors[cat] ?: CyberAccent
                    val count    = allExercises.count { it.category == cat }
                    val imageUrl = catImages[cat] ?: ""

                    Box(
                        modifier = Modifier
                            .width(160.dp)
                            .height(155.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(color.copy(0.18f))
                            .border(1.dp, color.copy(0.25f), RoundedCornerShape(22.dp))
                            .clickable { onCategoryClick(cat) }
                            .padding(14.dp)
                    ) {
                        // Circular workout image (replaces emoji)
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(color.copy(0.25f))
                                .align(Alignment.TopStart)
                        ) {
                            if (imageUrl.isNotBlank()) {
                                AsyncImage(
                                    model = imageUrl,
                                    contentDescription = cat.label,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                                )
                            }
                            // Tinted overlay so image doesn't overpower the card color
                            Box(Modifier.fillMaxSize().clip(CircleShape).background(color.copy(0.25f)))
                        }

                        // Arrow circle top-right
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(color.copy(0.35f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                null,
                                tint = color,
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        // Name + count bottom-left
                        Column(modifier = Modifier.align(Alignment.BottomStart)) {
                            Text(
                                cat.label,
                                fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Color.White
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                "$count exercises",
                                fontSize = 11.sp, color = color, fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }

        // ── Today's Health ────────────────────────────────────────────────────
        if (healthViewModel != null) {
            item {
                val log by healthViewModel.todayLog.collectAsState()
                val score = healthViewModel.weeklyHealthScore()
                var showStepsDialog by remember { mutableStateOf(false) }
                var stepsInput      by remember { mutableStateOf("") }
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("TODAY'S HEALTH", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold,
                            color = CyberAccent, letterSpacing = 1.sp)
                        Text("Weekly score: $score/100", fontSize = 11.sp, color = CyberTextMuted)
                    }

                    // Steps + Water
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Steps card — tap to enter count
                        Column(
                            modifier = Modifier.weight(1f).clip(RoundedCornerShape(16.dp))
                                .background(CyberBgCard)
                                .border(1.dp, Color(0xFF3B82F6).copy(0.2f), RoundedCornerShape(16.dp))
                                .clickable { stepsInput = if (log.stepsCount > 0) log.stepsCount.toString() else ""; showStepsDialog = true }
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("🏃 Steps", fontSize = 12.sp, color = CyberTextMuted)
                                Text("✏️", fontSize = 11.sp)
                            }
                            Text(
                                if (log.stepsCount == 0) "Tap to log" else "${log.stepsCount}",
                                fontSize = if (log.stepsCount == 0) 14.sp else 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (log.stepsCount == 0) CyberTextMuted else CyberTextPrimary
                            )
                            LinearProgressIndicator(
                                progress = { (log.stepsCount / 10_000f).coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(99.dp)),
                                color = Color(0xFF3B82F6), trackColor = Color(0xFF3B82F6).copy(0.15f)
                            )
                            Text("Goal: 10,000", fontSize = 10.sp, color = CyberTextMuted)
                        }
                        // Water card
                        Column(
                            modifier = Modifier.weight(1f).clip(RoundedCornerShape(16.dp))
                                .background(CyberBgCard)
                                .border(1.dp, Color.White.copy(0.07f), RoundedCornerShape(16.dp))
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("💧 Water", fontSize = 12.sp, color = CyberTextMuted)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(Modifier.size(26.dp).clip(CircleShape).background(CyberBgCardElevated)
                                    .clickable { healthViewModel.removeWater() }, contentAlignment = Alignment.Center) {
                                    Text("−", fontSize = 16.sp, color = CyberTextPrimary, fontWeight = FontWeight.Bold)
                                }
                                Text("${log.waterGlasses}", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF06B6D4))
                                Box(Modifier.size(26.dp).clip(CircleShape).background(Color(0xFF06B6D4).copy(0.2f))
                                    .clickable { healthViewModel.addWater() }, contentAlignment = Alignment.Center) {
                                    Text("+", fontSize = 16.sp, color = Color(0xFF06B6D4), fontWeight = FontWeight.Bold)
                                }
                            }
                            Text("${log.waterGlasses}/8 glasses", fontSize = 10.sp, color = CyberTextMuted)
                        }
                    }

                    // Sleep + Mood
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Sleep card
                        Column(
                            modifier = Modifier.weight(1f).clip(RoundedCornerShape(16.dp))
                                .background(CyberBgCard)
                                .border(1.dp, Color.White.copy(0.07f), RoundedCornerShape(16.dp))
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("😴 Sleep", fontSize = 12.sp, color = CyberTextMuted)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(Modifier.size(26.dp).clip(CircleShape).background(CyberBgCardElevated)
                                    .clickable { healthViewModel.setSleep((log.sleepHours - 0.5f).coerceAtLeast(0f)) },
                                    contentAlignment = Alignment.Center) {
                                    Text("−", fontSize = 16.sp, color = CyberTextPrimary, fontWeight = FontWeight.Bold)
                                }
                                Text(if (log.sleepHours == 0f) "—" else "${log.sleepHours}h", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF8B5CF6))
                                Box(Modifier.size(26.dp).clip(CircleShape).background(Color(0xFF8B5CF6).copy(0.2f))
                                    .clickable { healthViewModel.setSleep((log.sleepHours + 0.5f).coerceAtMost(14f)) },
                                    contentAlignment = Alignment.Center) {
                                    Text("+", fontSize = 16.sp, color = Color(0xFF8B5CF6), fontWeight = FontWeight.Bold)
                                }
                            }
                            Text("Goal: 7–9 hours", fontSize = 10.sp, color = CyberTextMuted)
                        }
                        // Mood card
                        Column(
                            modifier = Modifier.weight(1f).clip(RoundedCornerShape(16.dp))
                                .background(CyberBgCard)
                                .border(1.dp, Color.White.copy(0.07f), RoundedCornerShape(16.dp))
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("⚡ Energy", fontSize = 12.sp, color = CyberTextMuted)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf("😫","😔","😐","😊","🤩").forEachIndexed { i, emoji ->
                                    val rating = i + 1
                                    Box(
                                        modifier = Modifier.size(28.dp).clip(CircleShape)
                                            .background(if (log.moodRating == rating) CyberAccent.copy(0.2f) else Color.Transparent)
                                            .border(1.dp, if (log.moodRating == rating) CyberAccent else Color.Transparent, CircleShape)
                                            .clickable { healthViewModel.setMood(rating) },
                                        contentAlignment = Alignment.Center
                                    ) { Text(emoji, fontSize = 14.sp) }
                                }
                            }
                            Text(when(log.moodRating) { 1->"Low energy"; 2->"Tired"; 3->"Okay"; 4->"Good"; 5->"Great!"; else->"Tap to rate" }, fontSize = 10.sp, color = CyberTextMuted)
                        }
                    }

                    // Calories burned (if steps logged)
                    if (log.caloriesBurned > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFF59E0B).copy(0.1f))
                                .border(1.dp, Color(0xFFF59E0B).copy(0.2f), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("🔥", fontSize = 18.sp)
                            Text("~${log.caloriesBurned} kcal burned from ${log.stepsCount} steps today",
                                fontSize = 12.sp, color = Color(0xFFF59E0B), fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))

                // Steps input dialog
                if (showStepsDialog) {
                    AlertDialog(
                        onDismissRequest = { showStepsDialog = false },
                        containerColor = CyberBgCard,
                        title = {
                            Text("Log Steps", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = CyberTextPrimary)
                        },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("How many steps did you walk today?", fontSize = 13.sp, color = CyberTextMuted)
                                OutlinedTextField(
                                    value = stepsInput,
                                    onValueChange = { stepsInput = it.filter { c -> c.isDigit() }.take(6) },
                                    placeholder = { Text("e.g. 8500", color = CyberTextMuted) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor   = Color(0xFF3B82F6),
                                        unfocusedBorderColor = Color.White.copy(0.15f),
                                        focusedTextColor     = CyberTextPrimary,
                                        unfocusedTextColor   = CyberTextPrimary
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    val steps = stepsInput.toIntOrNull() ?: 0
                                    healthViewModel.setSteps(steps)
                                    showStepsDialog = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                                shape = RoundedCornerShape(12.dp)
                            ) { Text("Save", color = Color.White, fontWeight = FontWeight.Bold) }
                        },
                        dismissButton = {
                            TextButton(onClick = { showStepsDialog = false }) {
                                Text("Cancel", color = CyberTextMuted)
                            }
                        }
                    )
                }
            }

            // ── Body & Tracking ───────────────────────────────────────────────
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("BODY & TRACKING", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold,
                        color = CyberAccent, letterSpacing = 1.sp)
                    TrackingRow("📏", "Measurements", "Log weight, chest, waist & more", Color(0xFF3B82F6), onBodyMeasurementsClick)
                    TrackingRow("📸", "Progress Photos", "Visual transformation timeline", Color(0xFF10B981), onProgressPhotosClick)
                    TrackingRow("🌸", "Cycle Tracker", "Period tracking & cycle insights", Color(0xFFE91E8C), onCycleTrackerClick)
                }
                Spacer(Modifier.height(20.dp))
            }
        }

        // ── Quick cards — 2×2 grid so each card has enough space ─────────────
        item {
            val dietPlans by viewModel.myDietPlans.collectAsState()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickLinkCard(
                        imageUrl = "https://images.unsplash.com/photo-1490645935967-10de6ba17061?w=300&h=180&fit=crop",
                        title    = "Nutrition",
                        subtitle = "5 Indian diet plans",
                        color    = Color(0xFF065F46),
                        modifier = Modifier.weight(1f),
                        onClick  = onNutritionClick
                    )
                    QuickLinkCard(
                        imageUrl = "https://images.unsplash.com/photo-1512621776951-a57141f2eefd?w=300&h=180&fit=crop",
                        title    = "My Diet",
                        subtitle = if (dietPlans.isNotEmpty()) "Plan active ✓" else "From coach",
                        color    = Color(0xFF1A3A2A),
                        modifier = Modifier.weight(1f),
                        onClick  = onMyDietClick
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickLinkCard(
                        imageUrl = "https://images.unsplash.com/photo-1571019614242-c5c5dee9f50b?w=300&h=180&fit=crop",
                        title    = "Progress",
                        subtitle = if (goals.isEmpty()) "Set your goals" else "${goals.size} active goals",
                        color    = Color(0xFF1E3A5F),
                        modifier = Modifier.weight(1f),
                        onClick  = onProgressClick
                    )
                    QuickLinkCard(
                        imageUrl = "https://images.unsplash.com/photo-1559757175-5700dde675bc?w=300&h=180&fit=crop",
                        title    = "BMI & Plan",
                        subtitle = "Protein · Macros",
                        color    = Color(0xFF3B1F5E),
                        modifier = Modifier.weight(1f),
                        onClick  = onHealthMetricsClick
                    )
                }
            }
        }
    }
}

@Composable
private fun TrackingRow(emoji: String, title: String, subtitle: String, color: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CyberBgCard)
            .border(1.dp, color.copy(0.15f), RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp))
                .background(color.copy(0.12f)),
            contentAlignment = Alignment.Center
        ) { Text(emoji, fontSize = 22.sp) }
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = CyberTextPrimary)
            Text(subtitle, fontSize = 12.sp, color = CyberTextMuted)
        }
        Text("›", fontSize = 22.sp, color = color, fontWeight = FontWeight.Bold)
    }
}

// ─── Activity rings (3 concentric arcs) ──────────────────────────────────────

@Composable
private fun ActivityRing(
    workoutsProgress: Float,
    caloriesProgress: Float,
    streakProgress: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val stroke = 11.dp.toPx()
        val gap    = 7.dp.toPx()

        val r1 = (size.minDimension / 2f) - stroke / 2f
        val r2 = r1 - stroke - gap
        val r3 = r2 - stroke - gap

        fun drawRing(radius: Float, trackColor: Color, progressColor: Color, progress: Float) {
            val diameter = radius * 2f
            val topLeft  = center.copy(x = center.x - radius, y = center.y - radius)

            // Track
            drawCircle(color = trackColor, radius = radius, style = Stroke(stroke))

            // Progress arc
            if (progress > 0f) {
                drawArc(
                    color = progressColor,
                    startAngle = -90f,
                    sweepAngle = 360f * progress.coerceIn(0f, 1f),
                    useCenter = false,
                    topLeft = androidx.compose.ui.geometry.Offset(topLeft.x, topLeft.y),
                    size = androidx.compose.ui.geometry.Size(diameter, diameter),
                    style = Stroke(stroke, cap = StrokeCap.Round)
                )
            }
        }

        drawRing(r1, Color.White.copy(0.07f), Color(0xFFB5F023), workoutsProgress)  // lime (workouts)
        drawRing(r2, Color.White.copy(0.07f), Color(0xFFEF4444), caloriesProgress)  // red  (calories)
        drawRing(r3, Color.White.copy(0.07f), Color(0xFF3B82F6), streakProgress)    // blue (streak)
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

@Composable
private fun ActivityStatRow(label: String, value: String, goal: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Text(label, fontSize = 13.sp, color = CyberTextMuted, modifier = Modifier.width(70.dp))
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = color)
            Text("/ $goal", fontSize = 12.sp, color = CyberTextMuted, modifier = Modifier.padding(bottom = 2.dp))
        }
    }
}

@Composable
private fun RecentWorkoutRow(log: WorkoutLogEntry, timeFmt: SimpleDateFormat, allExercises: List<com.example.data.Exercise> = emptyList()) {
    val ex = remember(log.exerciseId, allExercises) { allExercises.find { it.id == log.exerciseId } }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(CyberBgCard)
            .border(1.dp, Color.White.copy(0.05f), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Exercise image or emoji icon
        Box(
            modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp))
        ) {
            if (ex?.imageUrl?.isNotBlank() == true) {
                AsyncImage(
                    model = ex.imageUrl, contentDescription = null,
                    contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    Modifier.fillMaxSize().background(CyberAccent.copy(0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(ex?.muscleEmoji ?: "🏋️", fontSize = 18.sp)
                }
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(log.exerciseName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = CyberTextPrimary)
            val detail = buildString {
                append("${log.setsCompleted} sets × ${log.repsCompleted} reps")
                if (log.weightKg > 0f) append(" @ ${log.weightKg}kg")
            }
            Text(detail, fontSize = 12.sp, color = CyberTextSecondary)
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(timeFmt.format(Date(log.dateMillis)), fontSize = 11.sp, color = CyberTextMuted)
            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = CyberTextMuted, modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
private fun QuickLinkCard(
    imageUrl: String, title: String, subtitle: String,
    color: Color, modifier: Modifier, onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(120.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(color)
            .clickable { onClick() }
    ) {
        if (imageUrl.isNotBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // Gradient overlay — darker at bottom for text legibility
            Box(
                Modifier.fillMaxSize().background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        listOf(color.copy(alpha = 0.25f), color.copy(alpha = 0.85f))
                    )
                )
            )
        }
        Column(
            modifier = Modifier.align(Alignment.BottomStart).padding(12.dp)
        ) {
            Text(
                title,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                maxLines = 1
            )
            Text(
                subtitle,
                fontSize = 11.sp,
                color = Color.White.copy(0.80f),
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}

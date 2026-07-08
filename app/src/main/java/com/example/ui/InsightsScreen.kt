package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppDatabase
import com.example.data.ClientGoal
import com.example.data.HealthCalculator
import com.example.data.HealthRepository
import com.example.data.ProgressAnalytics
import com.example.data.ProgressAnalytics.DayData
import com.example.data.UserPreferences
import com.example.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

private enum class InsightPeriod(val label: String, val days: Int) { DAY("Day", 1), WEEK("Week", 7), MONTH("Month", 30) }

private fun insightKeyDaysAgo(daysAgo: Int): String {
    val cal = Calendar.getInstance(); cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
    return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
}

private fun insightMapGoal(raw: String): ClientGoal =
    ClientGoal.entries.find { it.name.equals(raw, true) || it.label.equals(raw, true) } ?: ClientGoal.GENERAL_FITNESS

/**
 * Nutrition + lifestyle analytics: combines logged food, steps, water and sleep
 * against the user's goal to estimate fat/muscle change and an on-track score,
 * with Day / Week / Month views. Reached from the Food Diary.
 */
@Composable
fun NutritionInsightsScreen(
    onBack: () -> Unit,
    userPreferences: UserPreferences,
    db: AppDatabase,
) {
    val goal = remember { insightMapGoal(userPreferences.clientGoal) }
    val profile = remember { userPreferences.loadHealthProfile(goal) }
    val metrics = remember { HealthCalculator.calculate(profile) }

    var days30 by remember { mutableStateOf<List<DayData>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var period by remember { mutableStateOf(InsightPeriod.WEEK) }

    LaunchedEffect(Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        val logsByDate = if (uid.isNotEmpty())
            runCatching { HealthRepository(uid).getLastNDays(30) }.getOrDefault(emptyList()).associateBy { it.date }
        else emptyMap()
        val foods = runCatching { db.foodDiaryDao().entriesBetween(insightKeyDaysAgo(29), insightKeyDaysAgo(0)) }.getOrDefault(emptyList())
        val foodByDate = foods.groupBy { it.dateKey }
        days30 = (0 until 30).map { ago ->
            val d = insightKeyDaysAgo(ago)
            val f = foodByDate[d].orEmpty()
            val l = logsByDate[d]
            DayData(
                date = d,
                caloriesIn = f.sumOf { it.calories },
                proteinG = f.fold(0f) { a, e -> a + e.proteinG },
                carbsG = f.fold(0f) { a, e -> a + e.carbsG },
                fatG = f.fold(0f) { a, e -> a + e.fatG },
                steps = l?.stepsCount ?: 0,
                activeKcal = l?.caloriesBurned ?: 0,
                waterGlasses = l?.waterGlasses ?: 0,
                sleepHours = l?.sleepHours ?: 0f,
            )
        }
        loading = false
    }

    val subset = days30.take(period.days)
    val report = ProgressAnalytics.analyze(
        subset,
        bmrKcal = metrics?.bmrKcal ?: 0,
        tdeeKcal = metrics?.tdeeKcal ?: 0,
        weightKg = profile.weightKg,
        proteinTargetG = metrics?.dailyProteinG ?: 0,
        goal = goal,
    )

    Column(
        Modifier.fillMaxSize().background(CyberBgPrimary).statusBarsPadding()
            .verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 10.dp)) {
            Box(Modifier.size(36.dp).clip(CircleShape).background(CyberBgCard).clickable { onBack() },
                contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = CyberTextPrimary, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Insights", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = CyberTextPrimary)
                Text("${goal.emoji} ${goal.label}", fontSize = 12.sp, color = CyberTextMuted)
            }
        }

        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            InsightPeriod.entries.forEach { p ->
                val active = p == period
                Box(
                    Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                        .background(if (active) CyberAccent.copy(alpha = 0.15f) else CyberBgCard)
                        .clickable { period = p }.padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(p.label, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                        color = if (active) CyberAccent else CyberTextSecondary)
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        when {
            loading -> Box(Modifier.fillMaxWidth().padding(48.dp), Alignment.Center) {
                Text("Crunching your data…", color = CyberTextMuted, fontSize = 13.sp)
            }
            !report.hasData -> InsightEmpty()
            else -> {
                if (metrics == null) InsightProfilePrompt()
                InsightVerdictCard(report)
                Spacer(Modifier.height(12.dp))
                InsightBodyCompCard(report)
                Spacer(Modifier.height(12.dp))
                InsightMetricsCard(report)
                Spacer(Modifier.height(12.dp))
                if (report.balanceSeries.size > 1) InsightBalanceChart(report, goal)
                Spacer(Modifier.height(12.dp))
                Text(
                    "Estimates use energy-balance math (≈7,700 kcal ≈ 1 kg) from your logged food, steps, water and sleep. " +
                        "They're a guide, not a measurement — log consistently and record your weight for best accuracy.",
                    fontSize = 11.sp, color = CyberTextMuted, lineHeight = 16.sp
                )
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun InsightVerdictCard(r: ProgressAnalytics.Report) {
    val color = when {
        r.onTrackScore >= 75 -> CyberSuccess
        r.onTrackScore >= 50 -> CyberWarning
        else -> CyberDanger
    }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(CyberBgCard)
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(20.dp)).padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(r.verdict, fontSize = 20.sp, fontWeight = FontWeight.Black, color = color)
                Text("${r.loggedDays} of ${r.totalDays} days logged", fontSize = 11.sp, color = CyberTextMuted)
            }
            Box(
                Modifier.size(56.dp).clip(CircleShape).background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) { Text("${r.onTrackScore}", fontSize = 20.sp, fontWeight = FontWeight.Black, color = color) }
        }
        Spacer(Modifier.height(10.dp))
        Text(r.verdictDetail, fontSize = 12.sp, color = CyberTextSecondary, lineHeight = 18.sp)
    }
}

@Composable
private fun InsightBodyCompCard(r: ProgressAnalytics.Report) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        InsightEstTile("🔥", "Fat", r.estFatKg, Modifier.weight(1f))
        InsightEstTile("💪", "Muscle", r.estMuscleKg, Modifier.weight(1f))
    }
}

@Composable
private fun InsightEstTile(emoji: String, label: String, deltaKg: Float, modifier: Modifier) {
    val magnitude = abs(deltaKg)
    // Fat: losing (negative) is good. Muscle: gaining (positive) is good.
    val good = if (label == "Fat") deltaKg < 0f else deltaKg > 0f
    val word = when {
        magnitude < 0.02f -> "no change"
        (label == "Fat" && deltaKg < 0f) || (label == "Muscle" && deltaKg > 0f) ->
            if (label == "Fat") "lost" else "gained"
        else -> if (label == "Fat") "gained" else "lost"
    }
    val color = when {
        magnitude < 0.02f -> CyberTextMuted
        good -> if (label == "Fat") CyberDanger else CyberSuccess
        else -> CyberWarning
    }
    Column(
        modifier.clip(RoundedCornerShape(18.dp)).background(CyberBgCard)
            .border(1.dp, Color.White.copy(0.06f), RoundedCornerShape(18.dp)).padding(16.dp)
    ) {
        Text("$emoji $label", fontSize = 12.sp, color = CyberTextMuted, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text("≈ %.1f kg".format(magnitude), fontSize = 22.sp, fontWeight = FontWeight.Black, color = color)
        Text(word, fontSize = 11.sp, color = CyberTextSecondary)
    }
}

@Composable
private fun InsightMetricsCard(r: ProgressAnalytics.Report) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(CyberBgCard)
            .border(1.dp, Color.White.copy(0.06f), RoundedCornerShape(20.dp)).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Daily averages", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CyberTextMuted)
        r.metrics.forEach { m ->
            val pct = if (m.target > 0f) (m.value / m.target).coerceIn(0f, 1f) else 0f
            val barColor = if (m.onTrack) CyberSuccess else CyberWarning
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(m.label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = CyberTextSecondary)
                    Text(
                        "${m.value.roundToInt()}${if (m.unit.isNotEmpty()) " ${m.unit}" else ""}" +
                            (if (m.label == "Calories") " · TDEE ${m.target.roundToInt()}" else " / ${m.target.roundToInt()}"),
                        fontSize = 11.sp, color = CyberTextMuted
                    )
                }
                Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(99.dp)).background(Color.White.copy(0.06f))) {
                    Box(Modifier.fillMaxWidth(pct).height(6.dp).clip(RoundedCornerShape(99.dp)).background(barColor))
                }
            }
        }
    }
}

@Composable
private fun InsightBalanceChart(r: ProgressAnalytics.Report, goal: ClientGoal) {
    val series = r.balanceSeries
    val maxMag = (series.maxOfOrNull { abs(it) } ?: 1).coerceAtLeast(1)
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(CyberBgCard)
            .border(1.dp, Color.White.copy(0.06f), RoundedCornerShape(20.dp)).padding(18.dp)
    ) {
        Text("Daily energy balance", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CyberTextMuted)
        Text("Above line = surplus · below = deficit · green = matches your goal", fontSize = 10.sp, color = CyberTextMuted)
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.fillMaxWidth().height(90.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            series.forEach { bal ->
                val frac = (abs(bal).toFloat() / maxMag).coerceIn(0.04f, 1f)
                val goodDir = when (goal) {
                    ClientGoal.LOSE_FAT -> bal < 0
                    ClientGoal.BUILD_MUSCLE -> bal > 0
                    else -> abs(bal) <= 250
                }
                val c = if (goodDir) CyberSuccess else CyberWarning
                Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.Center) {
                    Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.BottomCenter) {
                        if (bal > 0) Box(Modifier.fillMaxWidth().fillMaxHeight(frac).clip(RoundedCornerShape(3.dp)).background(c))
                    }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(0.15f)))
                    Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.TopCenter) {
                        if (bal <= 0) Box(Modifier.fillMaxWidth().fillMaxHeight(frac).clip(RoundedCornerShape(3.dp)).background(c))
                    }
                }
            }
        }
    }
}

@Composable
private fun InsightProfilePrompt() {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(CyberWarning.copy(0.1f))
            .border(1.dp, CyberWarning.copy(0.3f), RoundedCornerShape(16.dp)).padding(14.dp)
    ) {
        Text("⚠ Set up your health profile", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CyberWarning)
        Text("Add your age, height and weight in Health so calorie & body-composition estimates are accurate.",
            fontSize = 11.sp, color = CyberTextSecondary, lineHeight = 16.sp)
    }
}

@Composable
private fun InsightEmpty() {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(CyberBgCard).padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("📊", fontSize = 40.sp)
        Spacer(Modifier.height(10.dp))
        Text("No data yet", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = CyberTextPrimary)
        Text("Log your meals and daily habits — your fat/muscle trend and on-track score appear here.",
            fontSize = 12.sp, color = CyberTextMuted, lineHeight = 18.sp)
    }
}

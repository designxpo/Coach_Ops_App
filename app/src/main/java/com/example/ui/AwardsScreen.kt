package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AwardCategory
import com.example.data.AwardDef
import com.example.data.AwardsEngine
import com.example.data.AwardsState
import com.example.data.EarnedAward
import com.example.data.UserPreferences
import com.example.ui.theme.CyberAccent
import com.example.ui.theme.CyberBgCard
import com.example.ui.theme.CyberBgCardElevated
import com.example.ui.theme.CyberBgPrimary
import com.example.ui.theme.CyberTextMuted
import com.example.ui.theme.CyberTextPrimary
import com.example.ui.theme.CyberTextSecondary
import com.example.ui.theme.CyberWarning
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Member awards — Apple-Fitness-style badges computed from step/water/workout
 * history. Evaluation runs on open (AwardsEngine.evaluate is idempotent), so
 * simply visiting this screen backfills streak counters and earns anything due.
 */
@Composable
fun AwardsScreen(
    userPreferences: UserPreferences,
    onBack: () -> Unit
) {
    var state by remember { mutableStateOf<AwardsState?>(null) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        state = AwardsEngine.evaluate(userPreferences.userId)
        loading = false
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(CyberBgPrimary)
    ) {
        // Header
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back",
                    tint = CyberTextPrimary)
            }
            Text("Awards", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold,
                color = CyberTextPrimary)
        }

        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = CyberAccent)
            }
            state == null -> Box(Modifier.fillMaxSize().padding(32.dp),
                contentAlignment = Alignment.Center) {
                Text("Couldn't load awards.\nCheck your connection and try again.",
                    fontSize = 14.sp, color = CyberTextMuted, textAlign = TextAlign.Center)
            }
            else -> AwardsContent(state!!)
        }
    }
}

@Composable
private fun AwardsContent(s: AwardsState) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 20.dp, end = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Fresh unlocks first — the celebration moment
        if (s.newlyEarned.isNotEmpty()) {
            item {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(CyberAccent.copy(alpha = 0.12f))
                        .border(1.dp, CyberAccent.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("🎉 New award${if (s.newlyEarned.size > 1) "s" else ""} unlocked!",
                        fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = CyberAccent)
                    Spacer(Modifier.height(6.dp))
                    s.newlyEarned.forEach { d ->
                        Text("${d.emoji}  ${d.title}", fontSize = 13.sp,
                            color = CyberTextPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Hero — live streak + today's progress
        item { StreakHero(s) }

        // Go For It — the 3 nearest unearned awards
        val goForIt = AwardsEngine.catalog
            .filter { s.earned[it.id] == null }
            .map { it to progressFor(it, s) }
            .filter { it.second > 0f }
            .sortedByDescending { it.second }
            .take(3)
        if (goForIt.isNotEmpty()) {
            item { SectionLabel("GO FOR IT") }
            items(goForIt.size) { i ->
                val (d, p) = goForIt[i]
                GoForItRow(d, p, s)
            }
        }

        // Category sections, three badges per row
        AwardCategory.entries.forEach { cat ->
            val defs = AwardsEngine.catalog.filter { it.category == cat }
            if (defs.isEmpty()) return@forEach
            item { SectionLabel(cat.label.uppercase()) }
            items((defs.size + 2) / 3) { row ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    for (col in 0..2) {
                        val idx = row * 3 + col
                        if (idx < defs.size) {
                            BadgeTile(defs[idx], s.earned[defs[idx].id],
                                progressFor(defs[idx], s), Modifier.weight(1f))
                        } else Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun StreakHero(s: AwardsState) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CyberBgCard)
            .border(1.dp, Color.White.copy(0.06f), RoundedCornerShape(20.dp))
            .padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("🔥", fontSize = 40.sp)
            Spacer(Modifier.size(14.dp))
            Column(Modifier.weight(1f)) {
                Text("${s.liveStreak}", fontSize = 34.sp, fontWeight = FontWeight.ExtraBold,
                    color = if (s.liveStreak > 0) CyberAccent else CyberTextMuted)
                Text("day streak", fontSize = 12.sp, color = CyberTextSecondary)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Best: ${maxOf(s.stats.bestStreak, s.liveStreak)} days",
                    fontSize = 12.sp, color = CyberTextMuted)
                Text("${s.stats.goalDaysTotal + if (s.todayGoalMet) 1 else 0} goal days total",
                    fontSize = 12.sp, color = CyberTextMuted)
            }
        }
        Spacer(Modifier.height(14.dp))
        val pct = (s.todaySteps.toFloat() / AwardsEngine.STEP_GOAL).coerceIn(0f, 1f)
        LinearProgressIndicator(
            progress = { pct },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(999.dp)),
            color = if (s.todayGoalMet) CyberAccent else CyberWarning,
            trackColor = CyberBgCardElevated
        )
        Spacer(Modifier.height(6.dp))
        Text(
            if (s.todayGoalMet) "Today's goal met — streak safe ✅"
            else "${"%,d".format(s.todaySteps)} / ${"%,d".format(AwardsEngine.STEP_GOAL)} steps today — " +
                "${"%,d".format(AwardsEngine.STEP_GOAL - s.todaySteps)} to keep the streak",
            fontSize = 11.sp,
            color = if (s.todayGoalMet) CyberAccent else CyberTextSecondary
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold,
        color = CyberAccent, letterSpacing = 1.sp,
        modifier = Modifier.padding(top = 6.dp))
}

@Composable
private fun GoForItRow(d: AwardDef, progress: Float, s: AwardsState) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CyberBgCard)
            .border(1.dp, Color.White.copy(0.06f), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            Modifier.size(44.dp).clip(RoundedCornerShape(12.dp))
                .background(CyberBgCardElevated),
            contentAlignment = Alignment.Center
        ) { Text(d.emoji, fontSize = 22.sp) }
        Column(Modifier.weight(1f)) {
            Text(d.title, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold,
                color = CyberTextPrimary)
            Text(progressText(d, s), fontSize = 11.sp, color = CyberTextMuted)
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(999.dp)),
                color = CyberAccent,
                trackColor = CyberBgCardElevated
            )
        }
    }
}

@Composable
private fun BadgeTile(d: AwardDef, e: EarnedAward?, progress: Float, modifier: Modifier) {
    val earned = e != null
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(CyberBgCard)
            .border(
                1.dp,
                if (earned) CyberAccent.copy(0.35f) else Color.White.copy(0.06f),
                RoundedCornerShape(16.dp)
            )
            .padding(vertical = 12.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.size(52.dp).clip(CircleShape)
                .background(if (earned) CyberAccent.copy(0.15f) else CyberBgCardElevated),
            contentAlignment = Alignment.Center
        ) {
            Text(d.emoji, fontSize = 26.sp,
                modifier = if (earned) Modifier else Modifier.alpha(0.35f))
        }
        Spacer(Modifier.height(8.dp))
        Text(d.title, fontSize = 11.sp, fontWeight = FontWeight.Bold,
            color = if (earned) CyberTextPrimary else CyberTextMuted,
            textAlign = TextAlign.Center, maxLines = 2, lineHeight = 13.sp)
        Spacer(Modifier.height(4.dp))
        if (earned) {
            val date = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
                .format(Date(e!!.earnedAt))
            Text(
                if (d.repeatable && e.timesEarned > 1) "×${e.timesEarned}" else date,
                fontSize = 9.sp, color = CyberAccent, fontWeight = FontWeight.Bold
            )
        } else {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(0.7f).height(3.dp)
                    .clip(RoundedCornerShape(999.dp)),
                color = CyberAccent.copy(0.7f),
                trackColor = CyberBgCardElevated
            )
        }
    }
}

// ── Progress helpers ──────────────────────────────────────────────────────────

private fun progressFor(d: AwardDef, s: AwardsState): Float {
    val todayBonus = if (s.todayGoalMet) 1 else 0
    val raw = when {
        d.id.startsWith("streak_")    -> s.liveStreak.toFloat() / d.target
        d.id.startsWith("goal_days_") -> (s.stats.goalDaysTotal + todayBonus).toFloat() / d.target
        d.id == "monthly_challenge"   -> (s.stats.monthGoalDays + todayBonus).toFloat() / d.target
        d.id == "hydration_7"         -> s.stats.waterStreak.toFloat() / d.target
        d.id == "perfect_week"        -> (s.stats.weekGoalDays + todayBonus).toFloat() / d.target
        d.id == "workout_week_5"      -> s.workoutsThisWeek.toFloat() / d.target
        d.id.startsWith("workout_")   -> s.workoutsTotal.toFloat() / d.target
        d.id == "step_record"         ->
            if (s.stats.bestSteps > 0) s.todaySteps.toFloat() / (s.stats.bestSteps + 1) else 0f
        else -> 0f
    }
    return raw.coerceIn(0f, 1f)
}

private fun progressText(d: AwardDef, s: AwardsState): String {
    val todayBonus = if (s.todayGoalMet) 1 else 0
    return when {
        d.id.startsWith("streak_")    -> "${s.liveStreak} of ${d.target} days"
        d.id.startsWith("goal_days_") -> "${s.stats.goalDaysTotal + todayBonus} of ${d.target} goal days"
        d.id == "monthly_challenge"   -> "${s.stats.monthGoalDays + todayBonus} of ${d.target} days this month"
        d.id == "hydration_7"         -> "${s.stats.waterStreak} of ${d.target} days"
        d.id == "perfect_week"        -> "${s.stats.weekGoalDays + todayBonus} of 7 days this week"
        d.id == "workout_week_5"      -> "${s.workoutsThisWeek} of ${d.target} workouts this week"
        d.id.startsWith("workout_")   -> "${s.workoutsTotal} of ${d.target} workouts"
        d.id == "step_record"         -> "Best: ${"%,d".format(s.stats.bestSteps)} steps"
        else -> d.description
    }
}

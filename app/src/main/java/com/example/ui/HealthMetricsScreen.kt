package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date

@Composable
fun HealthMetricsScreen(
    viewModel: FitnessViewModel,
    onBack: () -> Unit,
    onExerciseClick: (String) -> Unit
) {
    val records      by viewModel.healthRecords.collectAsState()
    val isSaving     by viewModel.isSavingHealth.collectAsState()

    var ageText    by remember { mutableStateOf(viewModel.healthProfile.ageYears.takeIf { it > 0 }?.toString() ?: "") }
    var heightText by remember { mutableStateOf(viewModel.healthProfile.heightCm.takeIf { it > 0f }?.toInt()?.toString() ?: "") }
    var weightText by remember { mutableStateOf(viewModel.healthProfile.weightKg.takeIf { it > 0f }?.toString() ?: "") }
    var waistText  by remember { mutableStateOf(viewModel.healthProfile.waistCm.takeIf { it > 0f }?.toString() ?: "") }
    var neckText   by remember { mutableStateOf(viewModel.healthProfile.neckCm.takeIf { it > 0f }?.toString() ?: "") }
    var hipText    by remember { mutableStateOf(viewModel.healthProfile.hipCm.takeIf { it > 0f }?.toString() ?: "") }
    var gender     by remember { mutableStateOf(viewModel.healthProfile.gender) }
    var activity   by remember { mutableStateOf(viewModel.healthProfile.activityLevel) }
    var goal       by remember { mutableStateOf(viewModel.healthProfile.goal) }
    var showNavy   by remember { mutableStateOf(false) }
    var savedMsg   by remember { mutableStateOf("") }

    var metrics by remember { mutableStateOf(viewModel.lastMetrics) }

    val ageGroup = remember(ageText) { ageText.toIntOrNull()?.let { HealthCalculator.ageGroupFor(it) } }
    val allExercises by viewModel.allExercises.collectAsState()
    val recommended = remember(ageGroup, allExercises) {
        ageGroup?.let { ag -> allExercises.filter { ag in it.ageGroups }.take(6) } ?: emptyList()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(CyberBgPrimary).statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 40.dp)
    ) {

        // ── Header ────────────────────────────────────────────────────────────
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp, 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(Modifier.size(40.dp).clip(CircleShape).background(CyberBgCard).clickable { onBack() },
                    contentAlignment = Alignment.Center) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = CyberTextPrimary, modifier = Modifier.size(18.dp))
                }
                Column {
                    Text("Health Metrics & BMI", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = CyberTextPrimary)
                    Text("Backed by ACSM · ISSN · EFSA standards", fontSize = 11.sp, color = CyberTextMuted)
                }
            }
        }

        // ── Input form ────────────────────────────────────────────────────────
        item {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(18.dp)).background(CyberBgCard)
                    .border(1.dp, Color.White.copy(0.07f), RoundedCornerShape(18.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("Personal Details", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = CyberTextPrimary)

                // Input validation warnings
                val heightVal = heightText.toFloatOrNull() ?: 0f
                val weightVal = weightText.toFloatOrNull() ?: 0f
                val heightWarning = heightVal in 1f..100f   // likely entered feet (e.g. 5.9)
                val weightWarning = weightVal > 200f        // likely entered lbs (e.g. 165)

                if (heightWarning) {
                    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF97316).copy(0.15f))
                        .border(1.dp, Color(0xFFF97316).copy(0.4f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Text("⚠ Enter height in cm (e.g. 175 for 5'9\")", fontSize = 11.sp, color = Color(0xFFF97316))
                    }
                }
                if (weightWarning) {
                    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF97316).copy(0.15f))
                        .border(1.dp, Color(0xFFF97316).copy(0.4f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Text("⚠ Enter weight in kg (e.g. 75, not lbs)", fontSize = 11.sp, color = Color(0xFFF97316))
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    HField("Age", ageText,    Modifier.weight(1f), "25")    { ageText    = it }
                    HField("Ht. (cm)", heightText, Modifier.weight(1f), "175") { heightText = it }
                    HField("Wt. (kg)", weightText, Modifier.weight(1f), "70")  { weightText = it }
                }
                Text("Height in centimetres · Weight in kilograms",
                    fontSize = 10.sp, color = CyberTextMuted)

                // Gender
                Text("Gender", fontSize = 12.sp, color = CyberTextMuted)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Gender.entries.forEach { g ->
                        val sel = gender == g
                        Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                            .background(if (sel) CyberAccent else CyberBgCardElevated)
                            .border(1.dp, if (sel) CyberAccent else Color.White.copy(0.1f), RoundedCornerShape(12.dp))
                            .clickable { gender = g }.padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center) {
                            Text(g.label, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                                color = if (sel) CyberAccentDark else CyberTextMuted)
                        }
                    }
                }

                // Navy method toggle
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                        .background(CyberBgCardElevated)
                        .clickable { showNavy = !showNavy }
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Add body measurements", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CyberTextPrimary)
                        Text("Enables U.S. Navy body fat method (±3% accuracy)", fontSize = 11.sp, color = CyberTextMuted)
                    }
                    Text(if (showNavy) "▲" else "▼", fontSize = 14.sp, color = CyberAccent, fontWeight = FontWeight.ExtraBold)
                }

                if (showNavy) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        HField("Waist cm", waistText, Modifier.weight(1f)) { waistText = it }
                        HField("Neck cm", neckText, Modifier.weight(1f)) { neckText = it }
                        if (gender == Gender.FEMALE)
                            HField("Hip cm", hipText, Modifier.weight(1f)) { hipText = it }
                    }
                    Text("Measure waist at navel, neck at narrowest point${if (gender == Gender.FEMALE) ", hips at widest" else ""}",
                        fontSize = 10.sp, color = CyberTextMuted)
                }

                // Activity level
                Text("Activity Level", fontSize = 12.sp, color = CyberTextMuted)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ActivityLevel.entries.forEach { level ->
                        val sel = activity == level
                        Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                            .background(if (sel) CyberAccent.copy(0.12f) else CyberBgCardElevated)
                            .border(1.dp, if (sel) CyberAccent.copy(0.5f) else Color.White.copy(0.06f), RoundedCornerShape(10.dp))
                            .clickable { activity = level }.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(Modifier.size(16.dp).clip(CircleShape)
                                .background(if (sel) CyberAccent else Color.White.copy(0.15f)),
                                contentAlignment = Alignment.Center) {
                                if (sel) Box(Modifier.size(6.dp).clip(CircleShape).background(CyberAccentDark))
                            }
                            Text(level.label, fontSize = 12.sp, modifier = Modifier.weight(1f),
                                color = if (sel) CyberAccent else CyberTextSecondary,
                                fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }

                // Goal — non-lazy 2-column grid (LazyVerticalGrid cannot be nested in LazyColumn)
                Text("Your Goal", fontSize = 12.sp, color = CyberTextMuted)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ClientGoal.entries.chunked(2).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            row.forEach { g ->
                                val sel = goal == g
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (sel) CyberAccent.copy(0.15f) else CyberBgCardElevated)
                                        .border(1.dp, if (sel) CyberAccent.copy(0.5f) else Color.White.copy(0.06f), RoundedCornerShape(10.dp))
                                        .clickable { goal = g }
                                        .padding(10.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(g.emoji, fontSize = 14.sp)
                                        Text(
                                            g.label, fontSize = 11.sp,
                                            modifier = Modifier.weight(1f),
                                            color = if (sel) CyberAccent else CyberTextSecondary,
                                            fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }

                // Calculate button
                Box(modifier = Modifier.fillMaxWidth().height(52.dp)
                    .clip(RoundedCornerShape(14.dp)).background(CyberAccent)
                    .clickable {
                        // Auto-correct common unit mistakes
                        var htCm = heightText.toFloatOrNull() ?: 0f
                        var wtKg = weightText.toFloatOrNull() ?: 0f
                        // Entered height in feet (e.g. 5.9) → convert to cm
                        if (htCm in 1f..9f) htCm = htCm * 30.48f   // e.g. 5 ft → 152cm
                        else if (htCm in 10f..100f) htCm = htCm * 2.54f // e.g. 69 inches → 175cm
                        // Entered weight in lbs (> 200)
                        if (wtKg > 200f) wtKg = wtKg * 0.453592f

                        val profile = HealthProfile(
                            ageYears      = ageText.toIntOrNull() ?: 0,
                            heightCm      = htCm,
                            weightKg      = wtKg,
                            gender        = gender,
                            activityLevel = activity,
                            goal          = goal,
                            waistCm       = waistText.toFloatOrNull() ?: 0f,
                            neckCm        = neckText.toFloatOrNull() ?: 0f,
                            hipCm         = hipText.toFloatOrNull() ?: 0f
                        )
                        // Reflect corrected values in the text fields
                        if (htCm != (heightText.toFloatOrNull() ?: 0f))
                            heightText = "%.0f".format(htCm)
                        if (wtKg != (weightText.toFloatOrNull() ?: 0f))
                            weightText = "%.1f".format(wtKg)
                        viewModel.calculateMetrics(profile)
                        metrics = HealthCalculator.calculate(profile)
                    }, contentAlignment = Alignment.Center) {
                    Text("Calculate My Plan", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = CyberAccentDark)
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // ── Results ───────────────────────────────────────────────────────────
        if (metrics != null) {
            val m = metrics!!
            val bmiColor = Color(m.bmiColor)
            val bfColor  = Color(m.bodyFatCategory.color)

            // BMI + Body Fat side by side
            item {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // BMI card
                    Column(modifier = Modifier.weight(1f).clip(RoundedCornerShape(18.dp))
                        .background(CyberBgCard)
                        .border(1.dp, bmiColor.copy(0.3f), RoundedCornerShape(18.dp))
                        .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("BMI", fontSize = 12.sp, color = CyberTextMuted)
                        Text("%.1f".format(m.bmi), fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, color = bmiColor)
                        Box(Modifier.clip(RoundedCornerShape(6.dp)).background(bmiColor.copy(0.15f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)) {
                            Text(m.bmiCategory, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = bmiColor)
                        }
                    }
                    // Body Fat card
                    Column(modifier = Modifier.weight(1f).clip(RoundedCornerShape(18.dp))
                        .background(CyberBgCard)
                        .border(1.dp, bfColor.copy(0.3f), RoundedCornerShape(18.dp))
                        .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Body Fat %", fontSize = 12.sp, color = CyberTextMuted)
                        Text("${"%.1f".format(m.bodyFatPercent)}%", fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, color = bfColor)
                        Box(Modifier.clip(RoundedCornerShape(6.dp)).background(bfColor.copy(0.15f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)) {
                            Text(m.bodyFatCategory.label, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = bfColor)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            // BMI scale bar
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    BmiScaleBar(m.bmi)
                    Spacer(Modifier.height(4.dp))
                    Text("Ideal weight for your height: ${"%.1f".format(m.idealWeightMinKg)}–${"%.1f".format(m.idealWeightMaxKg)} kg",
                        fontSize = 11.sp, color = CyberTextMuted)
                    Text("Method: ${m.bodyFatMethod}", fontSize = 10.sp, color = CyberTextMuted)
                }
                Spacer(Modifier.height(12.dp))
            }

            // Composition
            item {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CompCard("🦴 Lean Mass", "${"%.1f".format(m.leanMassKg)} kg",
                        "Muscle + bone + water", Color(0xFF10B981), Modifier.weight(1f))
                    CompCard("🫀 Fat Mass", "${"%.1f".format(m.fatMassKg)} kg",
                        "${"%.1f".format(m.bodyFatPercent)}% of body weight", Color(0xFFEF4444), Modifier.weight(1f))
                }
                Spacer(Modifier.height(12.dp))
            }

            // Daily targets
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(18.dp)).background(CyberBgCard)
                    .border(1.dp, Color.White.copy(0.07f), RoundedCornerShape(18.dp))
                    .padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Daily Targets", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = CyberTextPrimary)
                    Text("BMR: ${m.bmrKcal} kcal (at rest)  ·  TDEE: ${m.tdeeKcal} kcal (adjusted for goal)",
                        fontSize = 11.sp, color = CyberTextMuted)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MacroCard("🔥", "Calories", "${m.tdeeKcal}", "kcal/day", Color(0xFFEF4444), Modifier.weight(1f))
                        MacroCard("💧", "Water", "${"%.1f".format(m.dailyWaterL)}L", "per day", Color(0xFF3B82F6), Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MacroCard("🥩", "Protein", "${m.dailyProteinG}g",
                            "${"%.1f".format(m.proteinPerKg)}g/kg", Color(0xFFEF4444), Modifier.weight(1f))
                        MacroCard("🍚", "Carbs", "${m.dailyCarbsG}g", "per day", Color(0xFFF59E0B), Modifier.weight(1f))
                        MacroCard("🥑", "Fats", "${m.dailyFatG}g", "per day", Color(0xFF10B981), Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // Save button
            item {
                if (savedMsg.isNotEmpty()) {
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(12.dp)).background(CyberAccent.copy(0.15f)).padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CheckCircle, null, tint = CyberAccent, modifier = Modifier.size(16.dp))
                        Text(savedMsg, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CyberAccent)
                    }
                    Spacer(Modifier.height(8.dp))
                }
                Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isSaving) CyberBgCard else Color(0xFF16A34A))
                    .clickable(enabled = !isSaving) {
                        val profile = HealthProfile(
                            ageYears = ageText.toIntOrNull() ?: 0,
                            heightCm = heightText.toFloatOrNull() ?: 0f,
                            weightKg = weightText.toFloatOrNull() ?: 0f,
                            gender = gender, activityLevel = activity, goal = goal,
                            waistCm = waistText.toFloatOrNull() ?: 0f,
                            neckCm  = neckText.toFloatOrNull() ?: 0f,
                            hipCm   = hipText.toFloatOrNull() ?: 0f
                        )
                        viewModel.saveHealthRecord(profile, m)
                        savedMsg = "Health record saved to your profile ✓"
                    }, contentAlignment = Alignment.Center) {
                    if (isSaving) CircularProgressIndicator(color = CyberAccent, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    else Text("Save to My Profile", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                }
                Spacer(Modifier.height(12.dp))
            }

            // Indian protein sources
            item { ProteinSourcesCard(m.dailyProteinG) }
        }

        // ── History ───────────────────────────────────────────────────────────
        if (records.isNotEmpty()) {
            item {
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("History", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = CyberTextPrimary)
                    Text("${records.size} records", fontSize = 12.sp, color = CyberTextMuted)
                }
                Spacer(Modifier.height(8.dp))
            }
            items(records.take(10), key = { it.id }) { r -> HistoryRow(r) }
        }

        // ── Recommended exercises ─────────────────────────────────────────────
        if (recommended.isNotEmpty() && metrics != null) {
            item {
                val ag = ageGroup ?: return@item
                Spacer(Modifier.height(12.dp))
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text("${ag.emoji} Exercises for Your Age Group — ${ag.label} (${ag.range})",
                        fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = CyberTextPrimary)
                    Text("${allExercises.count { ag in it.ageGroups }} exercises matched",
                        fontSize = 11.sp, color = CyberTextMuted)
                }
                Spacer(Modifier.height(8.dp))
            }
            items(recommended, key = { it.id }) { ex ->
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(14.dp)).background(CyberBgCard)
                    .border(1.dp, Color.White.copy(0.05f), RoundedCornerShape(14.dp))
                    .clickable { onExerciseClick(ex.id) }.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)).background(CyberAccent.copy(0.1f)),
                        contentAlignment = Alignment.Center) { Text(ex.muscleEmoji, fontSize = 20.sp) }
                    Column(Modifier.weight(1f)) {
                        Text(ex.name, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = CyberTextPrimary)
                        Text("${ex.sets} · ${ex.reps} · ${ex.difficulty.label}", fontSize = 11.sp, color = CyberTextMuted)
                    }
                    Text("›", fontSize = 18.sp, color = CyberAccent, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

// ── History row ───────────────────────────────────────────────────────────────

@Composable
private fun HistoryRow(r: HealthRecord) {
    val fmt = remember { SimpleDateFormat("d MMM yyyy", java.util.Locale.getDefault()) }
    val bfColor = Color(when (r.bodyFatCategory) {
        "Athletic"      -> 0xFF10B981L
        "Fitness"       -> 0xFF84CC16L
        "Average"       -> 0xFFF59E0BL
        "Obese"         -> 0xFFEF4444L
        else            -> 0xFF3B82F6L
    })
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
        .clip(RoundedCornerShape(14.dp)).background(CyberBgCard)
        .border(1.dp, Color.White.copy(0.05f), RoundedCornerShape(14.dp)).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f)) {
            Text(fmt.format(Date(r.recordedAt)), fontSize = 12.sp, color = CyberTextMuted)
            Text("${r.weightKg}kg · BMI ${"%.1f".format(r.bmi)} · ${r.bmiCategory}",
                fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = CyberTextPrimary)
            Text("Body fat: ${"%.1f".format(r.bodyFatPercent)}%", fontSize = 11.sp, color = bfColor)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("${r.dailyProteinG}g", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFEF4444))
            Text("protein/day", fontSize = 9.sp, color = CyberTextMuted)
            Text("${r.tdeeKcal} kcal", fontSize = 11.sp, color = CyberTextMuted)
        }
    }
}

// ── Protein sources card ──────────────────────────────────────────────────────

@Composable
private fun ProteinSourcesCard(targetG: Int) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        .clip(RoundedCornerShape(18.dp)).background(Color(0xFF1C1F2E))
        .border(1.dp, Color(0xFFEF4444).copy(0.25f), RoundedCornerShape(18.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("🥩 Daily Protein Sources (Indian)", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = CyberTextPrimary)
        Text("Target: ${targetG}g/day", fontSize = 12.sp, color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
        listOf(
            "Paneer 100g" to "18g", "Chicken breast 100g" to "31g",
            "Eggs (1 whole)" to "6g",  "Dal / Lentils 1 cup cooked" to "18g",
            "Curd / Dahi 200g" to "7g","Rajma 1 cup cooked" to "15g",
            "Tofu 100g" to "8g",       "Milk 1 glass 250ml" to "8g",
            "Soya chunks 30g dry" to "15g", "Whey protein 1 scoop" to "25g",
            "Peanuts 30g" to "8g",     "Moong dal sprouts 100g" to "3g"
        ).forEach { (food, protein) ->
            Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                .background(CyberBgCard).padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text(food, fontSize = 12.sp, color = CyberTextSecondary)
                Text(protein, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFEF4444))
            }
        }
    }
    Spacer(Modifier.height(12.dp))
}

// ── BMI scale bar ─────────────────────────────────────────────────────────────

@Composable
private fun BmiScaleBar(bmi: Float) {
    val progress = ((bmi - 10f) / 30f).coerceIn(0f, 1f)
    Box(modifier = Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp))) {
        Box(Modifier.fillMaxSize().background(
            Brush.horizontalGradient(listOf(Color(0xFF3B82F6), Color(0xFF10B981), Color(0xFF10B981), Color(0xFFF59E0B), Color(0xFFEF4444)))
        ))
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val markerX = (progress * maxWidth.value).dp.coerceIn(0.dp, maxWidth)
            Box(Modifier.offset(x = markerX).fillMaxHeight().width(3.dp)
                .clip(RoundedCornerShape(2.dp)).background(Color.White))
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        listOf("10", "18.5", "25", "30", "40+").forEach {
            Text(it, fontSize = 9.sp, color = CyberTextMuted)
        }
    }
}

// ── Small composables ─────────────────────────────────────────────────────────

@Composable
private fun MacroCard(emoji: String, label: String, value: String, sub: String, accent: Color, modifier: Modifier) {
    Column(modifier = modifier.clip(RoundedCornerShape(14.dp))
        .background(accent.copy(0.1f)).border(1.dp, accent.copy(0.25f), RoundedCornerShape(14.dp)).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(emoji, fontSize = 18.sp)
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = accent)
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CyberTextSecondary)
        Text(sub, fontSize = 10.sp, color = CyberTextMuted)
    }
}

@Composable
private fun CompCard(label: String, value: String, sub: String, accent: Color, modifier: Modifier) {
    Column(modifier = modifier.clip(RoundedCornerShape(14.dp))
        .background(accent.copy(0.08f)).border(1.dp, accent.copy(0.2f), RoundedCornerShape(14.dp)).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, fontSize = 11.sp, color = CyberTextMuted)
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = accent)
        Text(sub, fontSize = 10.sp, color = CyberTextMuted)
    }
}

@Composable
private fun HField(label: String, value: String, modifier: Modifier, placeholder: String = "", onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value, onValueChange = onChange,
        label = { Text(label, fontSize = 10.sp) },
        placeholder = if (placeholder.isNotEmpty()) ({ Text(placeholder, fontSize = 13.sp, color = CyberTextMuted.copy(0.5f)) }) else null,
        modifier = modifier, singleLine = true,
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

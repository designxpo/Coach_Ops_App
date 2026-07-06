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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppDatabase
import com.example.data.FoodDiary
import com.example.data.FoodDiaryEntry
import com.example.data.UserPreferences
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
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val MEAL_ORDER = listOf(
    "BREAKFAST" to "🌅 Breakfast",
    "LUNCH"     to "☀️ Lunch",
    "SNACKS"    to "🫖 Snacks",
    "DINNER"    to "🌙 Dinner"
)

@Composable
fun FoodDiaryScreen(
    onBack: () -> Unit,
    userPreferences: UserPreferences,
    db: AppDatabase,
    onAddFood: () -> Unit
) {
    val dao = remember { db.foodDiaryDao() }
    val scope = rememberCoroutineScope()

    // Day being viewed (0 = today, -1 = yesterday…)
    var dayOffset by remember { mutableIntStateOf(0) }
    val viewedDate = remember(dayOffset) {
        Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, dayOffset) }.time
    }
    val dateKey = remember(dayOffset) { FoodDiary.dayKey(viewedDate) }
    val dateLabel = remember(dayOffset) {
        when (dayOffset) {
            0 -> "Today"
            -1 -> "Yesterday"
            else -> SimpleDateFormat("EEE, dd MMM", Locale.getDefault()).format(viewedDate)
        }
    }

    // remember() is critical: creating a new Flow every recomposition would
    // resubscribe endlessly (recomposition storm)
    val entriesFlow = remember(dateKey) { dao.entriesForDate(dateKey) }
    val entries by entriesFlow.collectAsState(initial = emptyList())

    // Cheat meals in the current week (Mon–Sun containing the viewed day)
    var weekCheatCount by remember { mutableIntStateOf(0) }
    LaunchedEffect(dateKey, entries.size) {
        val cal = Calendar.getInstance().apply {
            time = viewedDate
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        }
        val from = FoodDiary.dayKey(cal.time)
        cal.add(Calendar.DAY_OF_YEAR, 6)
        val to = FoodDiary.dayKey(cal.time)
        weekCheatCount = try {
            dao.entriesBetween(from, to).count { it.isCheatMeal }
        } catch (_: Exception) { 0 }
    }

    // Restore from cloud on first open (new device / reinstall)
    LaunchedEffect(Unit) { FoodDiary.pullToRoomIfEmpty(dao) }

    // Daily targets — the macro goals already stored in preferences
    val calTarget = userPreferences.trainingDayCalories
    val proTarget = userPreferences.trainingDayProteinG
    val carbTarget = userPreferences.trainingDayCarbsG
    val fatTarget = userPreferences.trainingDayFatG

    val totalCal = entries.sumOf { it.calories }
    val totalPro = entries.sumOf { it.proteinG.toDouble() }.toInt()
    val totalCarb = entries.sumOf { it.carbsG.toDouble() }.toInt()
    val totalFat = entries.sumOf { it.fatG.toDouble() }.toInt()
    val remaining = (calTarget - totalCal)

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(CyberBgPrimary),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        item {
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(CyberBgCard).clickable { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back",
                        tint = CyberTextPrimary, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(12.dp))
                Text("Food Diary", fontSize = 20.sp, fontWeight = FontWeight.Bold,
                    color = CyberTextPrimary, modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(CyberAccent)
                        .clickable { onAddFood() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Log food",
                        tint = CyberAccentDark, modifier = Modifier.size(20.dp))
                }
            }
        }

        // Date switcher
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier.size(32.dp).clip(CircleShape).background(CyberBgCard)
                        .clickable { dayOffset -= 1 },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.ChevronLeft, contentDescription = "Previous day",
                        tint = CyberTextSecondary, modifier = Modifier.size(18.dp))
                }
                Text(dateLabel, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = CyberTextPrimary)
                Box(
                    modifier = Modifier.size(32.dp).clip(CircleShape)
                        .background(if (dayOffset < 0) CyberBgCard else CyberBgCard.copy(alpha = 0.4f))
                        .clickable(enabled = dayOffset < 0) { dayOffset += 1 },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.ChevronRight, contentDescription = "Next day",
                        tint = if (dayOffset < 0) CyberTextSecondary else CyberTextMuted.copy(alpha = 0.4f),
                        modifier = Modifier.size(18.dp))
                }
            }
        }

        // Hero: calories vs target
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(CyberAccent)
                    .padding(20.dp)
            ) {
                Text("EATEN TODAY", fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0A0A0A).copy(alpha = 0.5f), letterSpacing = 0.8.sp)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("%,d".format(totalCal), fontSize = 34.sp,
                        fontWeight = FontWeight.ExtraBold, color = CyberAccentDark, lineHeight = 38.sp)
                    Text(" / %,d kcal".format(calTarget), fontSize = 14.sp,
                        color = Color(0xFF0A0A0A).copy(alpha = 0.55f),
                        modifier = Modifier.padding(bottom = 5.dp))
                }
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { (totalCal.toFloat() / calTarget).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(99.dp)),
                    color = CyberAccentDark,
                    trackColor = Color(0xFF0A0A0A).copy(alpha = 0.15f)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    when {
                        remaining > 0 -> "%,d kcal remaining".format(remaining)
                        remaining == 0 -> "Right on target 🎯"
                        else -> "%,d kcal over target".format(-remaining)
                    },
                    fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                    color = if (remaining >= 0) Color(0xFF166534) else Color(0xFF991B1B)
                )
            }
        }

        // Macro bars
        item {
            Column(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
                    .background(CyberBgCard).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MacroProgressRow("💪 Protein", totalPro, proTarget, "g", Color(0xFF3B82F6))
                MacroProgressRow("⚡ Carbs", totalCarb, carbTarget, "g", Color(0xFFF59E0B))
                MacroProgressRow("🥑 Fat", totalFat, fatTarget, "g", Color(0xFFEF4444))
            }
        }

        // Cheat meal chip
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (weekCheatCount > 2) CyberDanger.copy(alpha = 0.10f) else CyberBgCard)
                    .border(1.dp,
                        if (weekCheatCount > 2) CyberDanger.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.07f),
                        RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🍕", fontSize = 18.sp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        when (weekCheatCount) {
                            0 -> "No cheat meals this week — clean streak! 🔥"
                            1 -> "1 cheat meal this week — balanced"
                            2 -> "2 cheat meals this week — still okay"
                            else -> "$weekCheatCount cheat meals this week — rein it in"
                        },
                        fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                        color = if (weekCheatCount > 2) CyberDanger else CyberTextPrimary
                    )
                    Text("Tap 🍕 on any food to mark/unmark it as a cheat meal",
                        fontSize = 10.sp, color = CyberTextMuted)
                }
            }
        }

        // Meal sections
        if (entries.isEmpty()) {
            item {
                Column(
                    Modifier.fillMaxWidth().padding(vertical = 36.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("🍽️", fontSize = 44.sp)
                    Spacer(Modifier.height(12.dp))
                    Text("Nothing logged ${dateLabel.lowercase()}", fontSize = 15.sp,
                        fontWeight = FontWeight.Bold, color = CyberTextPrimary)
                    Spacer(Modifier.height(6.dp))
                    Text("Scan, speak or snap your food — then tap \"Add to Diary\"",
                        fontSize = 12.sp, color = CyberTextMuted)
                    Spacer(Modifier.height(16.dp))
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(14.dp)).background(CyberAccent)
                            .clickable { onAddFood() }.padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        Text("+ Log Food", fontWeight = FontWeight.Bold, color = CyberAccentDark, fontSize = 14.sp)
                    }
                }
            }
        } else {
            MEAL_ORDER.forEach { (key, label) ->
                val mealEntries = entries.filter { it.mealType == key }
                if (mealEntries.isNotEmpty()) {
                    item(key = "hdr_$key") {
                        Row(
                            Modifier.fillMaxWidth().padding(top = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(label, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = CyberTextPrimary)
                            Text("${mealEntries.sumOf { it.calories }} kcal",
                                fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CyberTextMuted)
                        }
                    }
                    mealEntries.forEach { entry ->
                        item(key = entry.id) {
                            DiaryEntryCard(
                                entry = entry,
                                onToggleCheat = {
                                    scope.launch {
                                        val updated = entry.copy(isCheatMeal = !entry.isCheatMeal)
                                        dao.update(updated)
                                        FoodDiary.syncSave(updated)
                                    }
                                },
                                onDelete = {
                                    scope.launch {
                                        dao.delete(entry.id)
                                        FoodDiary.syncDelete(entry.id)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MacroProgressRow(label: String, value: Int, target: Int, unit: String, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = CyberTextSecondary)
            Text("$value / $target$unit", fontSize = 12.sp, color = CyberTextMuted)
        }
        LinearProgressIndicator(
            progress = { (value.toFloat() / target.coerceAtLeast(1)).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(99.dp)),
            color = color,
            trackColor = color.copy(alpha = 0.15f)
        )
    }
}

@Composable
private fun DiaryEntryCard(
    entry: FoodDiaryEntry,
    onToggleCheat: () -> Unit,
    onDelete: () -> Unit
) {
    val timeFmt = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(CyberBgCard)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(entry.foodName, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                    color = CyberTextPrimary)
                if (entry.isCheatMeal) {
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(999.dp))
                            .background(CyberWarning.copy(alpha = 0.15f))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text("CHEAT", fontSize = 8.sp, fontWeight = FontWeight.Black, color = CyberWarning)
                    }
                }
            }
            Text(
                "${entry.servingDesc.ifEmpty { "1 serving" }} · P ${entry.proteinG.toInt()}g · C ${entry.carbsG.toInt()}g · F ${entry.fatG.toInt()}g · ${timeFmt.format(Date(entry.timeMillis))}",
                fontSize = 11.sp, color = CyberTextMuted
            )
        }
        Text("${entry.calories}", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = CyberAccent)
        Text("kcal", fontSize = 10.sp, color = CyberTextMuted)
        // Cheat toggle
        Box(
            modifier = Modifier.size(30.dp).clip(CircleShape)
                .background(if (entry.isCheatMeal) CyberWarning.copy(alpha = 0.18f) else CyberBgCardElevated)
                .clickable { onToggleCheat() },
            contentAlignment = Alignment.Center
        ) {
            Text("🍕", fontSize = 13.sp)
        }
        Box(
            modifier = Modifier.size(30.dp).clip(CircleShape).background(CyberBgCardElevated)
                .clickable { onDelete() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = CyberDanger, modifier = Modifier.size(13.dp))
        }
    }
}

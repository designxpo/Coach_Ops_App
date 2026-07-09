@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@Composable
fun MyDietScreen(
    viewModel: FitnessViewModel,
    onBack: () -> Unit
) {
    val plans by viewModel.myDietPlans.collectAsState()
    val logs   by viewModel.myDietLogs.collectAsState()
    val plan   = plans.firstOrNull()

    var selectedDay    by remember(plan) { mutableStateOf(0) }
    var showLogSheet   by remember { mutableStateOf(false) }

    if (showLogSheet && plan != null) {
        DietLogSheet(
            plan        = plan,
            selectedDay = selectedDay,
            logs        = logs,
            onDismiss   = { showLogSheet = false },
            onSave      = { log ->
                viewModel.saveDietLog(log)
                viewModel.loadMyDietLogs(plan.id)
                showLogSheet = false
            }
        )
    }

    Column(
        Modifier.fillMaxSize().background(CyberBgPrimary).statusBarsPadding()
    ) {
        // Top bar
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = CyberTextPrimary)
            }
            Column(Modifier.weight(1f)) {
                Text("My Diet Plan", color = CyberTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Black)
                if (plan != null) Text("by ${plan.clientName.ifBlank { "your coach" }}", color = CyberTextMuted, fontSize = 12.sp)
            }
            if (plan != null) {
                Button(
                    onClick = { showLogSheet = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CyberAccent),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Filled.Edit, null, tint = CyberAccentDark, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Log Today", color = CyberAccentDark, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(8.dp))
            }
        }

        if (plan == null) {
            // Empty state
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                    Text("🥗", fontSize = 56.sp)
                    Spacer(Modifier.height(16.dp))
                    Text("No Diet Plan Yet", color = CyberTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Your coach hasn't sent a diet plan yet. Once they create and send one, it will appear here.",
                        color = CyberTextMuted, fontSize = 13.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 20.sp
                    )
                }
            }
            return@MyDietScreen
        }

        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Plan header card
            item {
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                        .background(CyberBgCard).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(plan.title.ifBlank { "Diet Plan" }, color = CyberTextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Black)
                    if (plan.description.isNotBlank())
                        Text(plan.description, color = CyberTextMuted, fontSize = 13.sp, lineHeight = 20.sp)
                    androidx.compose.foundation.layout.FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (plan.totalDailyCalories > 0) MacroChip("${plan.totalDailyCalories}", "kcal/day", Color(0xFFF59E0B))
                        if (plan.totalDailyProteinG > 0) MacroChip("${plan.totalDailyProteinG}g", "protein", Color(0xFF818CF8))
                        MacroChip("${plan.days.size}", "days", CyberAccent)
                    }
                }
            }

            // Day selector
            if (plan.days.size > 1) {
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        itemsIndexed(plan.days) { idx, day ->
                            val sel = idx == selectedDay
                            Box(
                                Modifier.clip(RoundedCornerShape(10.dp))
                                    .background(if (sel) CyberAccent else CyberBgCard)
                                    .border(1.dp, if (sel) Color.Transparent else Color.White.copy(0.1f), RoundedCornerShape(10.dp))
                                    .clickable { selectedDay = idx }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(day.dayLabel, color = if (sel) CyberAccentDark else CyberTextPrimary,
                                    fontSize = 13.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }
                }
            }

            // Meals for selected day
            val currentDay = plan.days.getOrNull(selectedDay)
            if (currentDay != null) {
                if (currentDay.meals.isEmpty()) {
                    item {
                        Text("No meals planned for ${currentDay.dayLabel} yet.",
                            color = CyberTextMuted, fontSize = 13.sp,
                            modifier = Modifier.padding(vertical = 8.dp))
                    }
                } else {
                    items(currentDay.meals, key = { it.mealName + it.timeSlot }) { meal ->
                        MemberMealCard(meal = meal, logs = logs)
                    }
                }
            }

            // Coach notes
            if (plan.notes.isNotBlank()) {
                item {
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                            .background(CyberAccent.copy(0.07f))
                            .border(1.dp, CyberAccent.copy(0.2f), RoundedCornerShape(12.dp))
                            .padding(14.dp)
                    ) {
                        Text("Coach Notes", color = CyberAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text(plan.notes, color = CyberTextPrimary, fontSize = 13.sp, lineHeight = 20.sp)
                    }
                }
            }

            // Adherence history
            if (logs.isNotEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("My Progress", color = CyberTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        logs.take(10).forEach { log ->
                            DietLogRow(log)
                        }
                    }
                }
            } else {
                item {
                    Text("No logs yet — tap Log Today to get started", fontSize = 12.sp, color = CyberTextMuted, modifier = Modifier.padding(16.dp))
                }
            }
        }
    }
}

@Composable
private fun MacroChip(value: String, label: String, color: Color) {
    Column(
        Modifier.clip(RoundedCornerShape(10.dp)).background(color.copy(0.1f)).padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, color = color, fontSize = 14.sp, fontWeight = FontWeight.Black, maxLines = 1, softWrap = false)
        Text(label, color = color.copy(0.7f), fontSize = 10.sp, maxLines = 1, softWrap = false)
    }
}

@Composable
private fun MemberMealCard(meal: DietMeal, logs: List<DietLog>) {
    var expanded by remember { mutableStateOf(true) }

    val todayFollowed = logs.firstOrNull { it.date == todayDate() }
        ?.mealsFollowed?.find { it.mealName == meal.mealName }?.followed

    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(CyberBgCard)
            .border(1.dp, Color.White.copy(0.07f), RoundedCornerShape(12.dp))
    ) {
        Row(
            Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(meal.mealName, color = CyberTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    if (todayFollowed != null) {
                        Icon(
                            if (todayFollowed) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
                            null,
                            tint = if (todayFollowed) Color(0xFF22C55E) else Color(0xFFEF4444),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (meal.timeSlot.isNotBlank()) Text(meal.timeSlot, color = CyberTextMuted, fontSize = 12.sp)
                    if (meal.totalCalories > 0) Text("${meal.totalCalories} kcal", color = Color(0xFFF59E0B), fontSize = 12.sp)
                    if (meal.totalProteinG > 0) Text("${meal.totalProteinG}g protein", color = Color(0xFF818CF8), fontSize = 12.sp)
                }
            }
            Icon(if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                null, tint = CyberTextMuted, modifier = Modifier.size(18.dp))
        }

        AnimatedVisibility(visible = expanded, enter = expandVertically(), exit = shrinkVertically()) {
            Column(Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                meal.foods.forEach { food ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                            .background(CyberBgCardElevated).padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier.size(6.dp).clip(CircleShape).background(CyberAccent.copy(0.6f))
                                .align(Alignment.Top).padding(top = 6.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(food.name, color = CyberTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (food.quantity.isNotBlank()) Text(food.quantity, color = CyberTextMuted, fontSize = 11.sp)
                                if (food.calories > 0) Text("${food.calories} kcal", color = Color(0xFFF59E0B), fontSize = 11.sp)
                                if (food.proteinG > 0) Text("${food.proteinG}g P", color = Color(0xFF818CF8), fontSize = 11.sp)
                            }
                            if (food.notes.isNotBlank())
                                Text(food.notes, color = CyberTextMuted, fontSize = 11.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DietLogRow(log: DietLog) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(CyberBgCard).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(log.date, color = CyberTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            if (log.memberNote.isNotBlank())
                Text(log.memberNote, color = CyberTextMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (log.mealsFollowed.isNotEmpty()) {
                val followed = log.mealsFollowed.count { it.followed }
                Text("$followed/${log.mealsFollowed.size} meals followed",
                    color = CyberTextMuted, fontSize = 11.sp)
            }
        }
        AdherenceBadge(log.adherencePercent)
    }
}

// ─── Daily Log Bottom Sheet ───────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DietLogSheet(
    plan: DietPlan,
    selectedDay: Int,
    logs: List<DietLog>,
    onDismiss: () -> Unit,
    onSave: (DietLog) -> Unit
) {
    val today      = todayDate()
    val clientUid  = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""
    val existing   = logs.find { it.date == today }

    var adherence  by remember { mutableStateOf(existing?.adherencePercent?.toFloat() ?: 80f) }
    var note       by remember { mutableStateOf(existing?.memberNote ?: "") }

    val dayMeals   = plan.days.getOrNull(selectedDay)?.meals?.map { it.mealName } ?: emptyList()
    var mealStatus by remember {
        mutableStateOf(
            dayMeals.associateWith { name ->
                existing?.mealsFollowed?.find { it.mealName == name }?.followed ?: true
            }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor   = CyberBgCard,
        sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Log Today's Adherence", color = CyberTextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Black)
            Text(today, color = CyberTextMuted, fontSize = 13.sp)

            // Adherence slider
            Column {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Overall Adherence", color = CyberTextMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text("${adherence.toInt()}%", color = CyberAccent, fontSize = 14.sp, fontWeight = FontWeight.Black)
                }
                Slider(
                    value = adherence, onValueChange = { adherence = (it * 10).roundToInt() / 10f },
                    valueRange = 0f..100f, steps = 9,
                    colors = SliderDefaults.colors(thumbColor = CyberAccent, activeTrackColor = CyberAccent,
                        inactiveTrackColor = CyberBgCardElevated)
                )
            }

            // Meal toggles
            if (dayMeals.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Which meals did you follow?", color = CyberTextMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    dayMeals.forEach { mealName ->
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                                .background(CyberBgCardElevated).padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(mealName, color = CyberTextPrimary, fontSize = 13.sp)
                            Switch(
                                checked = mealStatus[mealName] ?: true,
                                onCheckedChange = { mealStatus = mealStatus + (mealName to it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = CyberAccentDark, checkedTrackColor = CyberAccent,
                                    uncheckedThumbColor = CyberTextMuted, uncheckedTrackColor = CyberBgCard)
                            )
                        }
                    }
                }
            }

            // Note
            Column {
                Text("Update your coach", color = CyberTextMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 4.dp))
                OutlinedTextField(
                    value = note, onValueChange = { note = it },
                    placeholder = { Text("How did it go? Any challenges?", color = CyberTextMuted.copy(0.5f), fontSize = 13.sp) },
                    maxLines = 3, modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = CyberAccent, unfocusedBorderColor = Color.White.copy(0.12f),
                        focusedTextColor     = CyberTextPrimary, unfocusedTextColor = CyberTextPrimary,
                        focusedContainerColor = CyberBgCard, unfocusedContainerColor = CyberBgCard
                    )
                )
            }

            Button(
                onClick = {
                    onSave(DietLog(
                        id                = "${plan.id}_${clientUid}_$today",
                        planId            = plan.id,
                        coachUid          = plan.coachUid,
                        clientId          = clientUid,
                        date              = today,
                        adherencePercent  = adherence.toInt(),
                        memberNote        = note.trim(),
                        mealsFollowed     = mealStatus.map { (name, followed) -> DietMealFollowed(name, followed) },
                        createdAt         = System.currentTimeMillis()
                    ))
                },
                modifier = Modifier.fillMaxWidth().height(52.dp).padding(bottom = 4.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CyberAccent),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Save Update", color = CyberAccentDark, fontWeight = FontWeight.Black, fontSize = 15.sp)
            }
        }
    }
}

private fun todayDate(): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

// ─── Coach: Diet Plan Tab Content (Column-based — safe inside parent LazyColumn) ─

@Composable
fun DietPlanTabContent(
    viewModel: MainViewModel,
    clientId: String,
    clientName: String,
    onNavigateToEditor: () -> Unit = {}
) {
    LaunchedEffect(clientId) { viewModel.loadDietPlansForClient(clientId) }
    val plans by viewModel.clientDietPlans.collectAsStateWithLifecycle()
    val logs   by viewModel.planDietLogs.collectAsStateWithLifecycle()
    var expandedPlanId by remember { mutableStateOf<String?>(null) }
    var showDeleteId   by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(expandedPlanId) { expandedPlanId?.let { viewModel.loadDietLogs(it) } }

    if (showDeleteId != null) {
        AlertDialog(
            onDismissRequest = { showDeleteId = null },
            title = { Text("Delete Plan?", color = CyberTextPrimary) },
            text  = { Text("This cannot be undone.", color = CyberTextMuted, fontSize = 13.sp) },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteDietPlan(showDeleteId!!); showDeleteId = null }) {
                    Text("Delete", color = CyberDanger)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteId = null }) { Text("Cancel", color = CyberTextMuted) }
            },
            containerColor = CyberBgCard
        )
    }

    // Column (not LazyColumn) — safe inside parent LazyColumn item {}
    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (plans.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("🥗", fontSize = 48.sp)
                Spacer(Modifier.height(12.dp))
                Text("No diet plan yet", color = CyberTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("Create a personalised plan for $clientName", color = CyberTextMuted, fontSize = 13.sp)
            }
        }

        plans.forEach { plan ->
            DietPlanCard(
                plan           = plan,
                logs           = if (expandedPlanId == plan.id) logs else emptyList(),
                expanded       = expandedPlanId == plan.id,
                onExpand       = { expandedPlanId = if (expandedPlanId == plan.id) null else plan.id },
                onEdit         = { viewModel.setEditingDietPlan(plan); onNavigateToEditor() },
                onDelete       = { showDeleteId = plan.id },
                onToggleActive = {
                    viewModel.saveDietPlan(plan.copy(
                        status = if (plan.isActive) "draft" else "active",
                        sentAt = if (!plan.isActive) System.currentTimeMillis() else plan.sentAt
                    ))
                }
            )
        }

        Spacer(Modifier.height(4.dp))
        Button(
            onClick = { viewModel.setEditingDietPlan(null); onNavigateToEditor() },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = CyberAccent),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Filled.Add, null, tint = CyberAccentDark, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Create New Diet Plan", color = CyberAccentDark, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun DietPlanCard(
    plan: DietPlan,
    logs: List<DietLog>,
    expanded: Boolean,
    onExpand: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleActive: () -> Unit
) {
    val fmt = remember { SimpleDateFormat("d MMM yyyy", Locale.getDefault()) }
    val avgAdherence = if (logs.isEmpty()) null else logs.take(7).map { it.adherencePercent }.average().toInt()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CyberBgCard)
            .border(1.dp, if (plan.isActive) CyberAccent.copy(0.4f) else Color.White.copy(0.06f), RoundedCornerShape(16.dp))
    ) {
        // Header row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onExpand() }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(plan.title.ifBlank { "Untitled Plan" }, color = CyberTextPrimary,
                        fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Box(
                        Modifier.clip(RoundedCornerShape(6.dp))
                            .background(if (plan.isActive) CyberAccent.copy(0.15f) else Color.White.copy(0.07f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            if (plan.isActive) "Active" else "Draft",
                            color = if (plan.isActive) CyberAccent else CyberTextMuted,
                            fontSize = 10.sp, fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("${plan.days.size} days", color = CyberTextMuted, fontSize = 12.sp)
                    if (plan.totalDailyCalories > 0)
                        Text("~${plan.totalDailyCalories} kcal/day", color = CyberTextMuted, fontSize = 12.sp)
                    if (plan.isActive && plan.sentAt > 0)
                        Text("Sent ${fmt.format(Date(plan.sentAt))}", color = CyberAccent.copy(0.7f), fontSize = 12.sp)
                }
            }
            Icon(
                if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                null, tint = CyberTextMuted, modifier = Modifier.size(20.dp)
            )
        }

        // Expanded section
        AnimatedVisibility(visible = expanded, enter = expandVertically(), exit = shrinkVertically()) {
            Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                // Days summary
                if (plan.days.isNotEmpty()) {
                    Text("Days", color = CyberTextMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 6.dp))
                    plan.days.forEach { day ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 3.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(day.dayLabel, color = CyberTextPrimary, fontSize = 13.sp)
                            Text("${day.meals.size} meals · ${day.totalCalories} kcal", color = CyberTextMuted, fontSize = 12.sp)
                        }
                    }
                }

                // Adherence logs
                if (logs.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Member Updates", color = CyberTextMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        if (avgAdherence != null)
                            Text("Avg ${avgAdherence}%", color = CyberAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(6.dp))
                    logs.take(5).forEach { log ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 3.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(log.date, color = CyberTextPrimary, fontSize = 12.sp)
                                if (log.memberNote.isNotBlank())
                                    Text(log.memberNote, color = CyberTextMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            AdherenceBadge(log.adherencePercent)
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))
                // Action buttons
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onEdit,
                        modifier = Modifier.weight(1f).height(40.dp),
                        shape = RoundedCornerShape(10.dp),
                        border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberTextPrimary)
                    ) {
                        Icon(Icons.Filled.Edit, null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Edit", fontSize = 13.sp)
                    }
                    Button(
                        onClick = onToggleActive,
                        modifier = Modifier.weight(1f).height(40.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (plan.isActive) CyberBgCardElevated else CyberAccent
                        )
                    ) {
                        Icon(
                            if (plan.isActive) Icons.Filled.PauseCircle else Icons.AutoMirrored.Filled.Send,
                            null, modifier = Modifier.size(14.dp),
                            tint = if (plan.isActive) CyberTextMuted else CyberAccentDark
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            if (plan.isActive) "Deactivate" else "Send to Member",
                            fontSize = 13.sp, fontWeight = FontWeight.Bold,
                            color = if (plan.isActive) CyberTextMuted else CyberAccentDark
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp))
                            .background(CyberDanger.copy(0.1f))
                    ) {
                        Icon(Icons.Filled.Delete, null, tint = CyberDanger, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun AdherenceBadge(percent: Int) {
    val color = when {
        percent >= 80 -> Color(0xFF22C55E)
        percent >= 50 -> Color(0xFFF59E0B)
        else          -> Color(0xFFEF4444)
    }
    Box(
        Modifier.clip(RoundedCornerShape(8.dp)).background(color.copy(0.15f)).padding(horizontal = 10.dp, vertical = 3.dp)
    ) {
        Text("$percent%", color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

// ─── Diet Plan Editor (full screen) ──────────────────────────────────────────

private val WEEK_DAYS = listOf("Monday","Tuesday","Wednesday","Thursday","Friday","Saturday","Sunday")

@Composable
fun DietPlanEditorScreen(
    viewModel: MainViewModel,
    clientId: String,
    clientName: String,
    onBack: () -> Unit
) {
    val uid         = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""
    val existingPlan by viewModel.editingDietPlan.collectAsStateWithLifecycle()

    var title       by remember(existingPlan) { mutableStateOf(existingPlan?.title ?: "") }
    var description by remember(existingPlan) { mutableStateOf(existingPlan?.description ?: "") }
    var notes       by remember(existingPlan) { mutableStateOf(existingPlan?.notes ?: "") }
    var days        by remember(existingPlan) { mutableStateOf(
        existingPlan?.days?.takeIf { it.isNotEmpty() }
            ?: WEEK_DAYS.map { DietDay(it) }
    )}
    var selectedDay  by remember { mutableStateOf(0) }
    var showAddMeal  by remember { mutableStateOf(false) }
    var newMealName  by remember { mutableStateOf("") }
    var newMealTime  by remember { mutableStateOf("") }

    val planId = remember { existingPlan?.id ?: UUID.randomUUID().toString() }

    fun save(status: String) {
        val plan = DietPlan(
            id         = planId,
            coachUid   = uid,
            clientId   = clientId,
            clientName = clientName,
            title      = title.trim().ifBlank { "Diet Plan" },
            description= description.trim(),
            notes      = notes.trim(),
            status     = status,
            createdAt  = existingPlan?.createdAt ?: System.currentTimeMillis(),
            sentAt     = if (status == "active") System.currentTimeMillis() else (existingPlan?.sentAt ?: 0L),
            days       = days
        )
        viewModel.saveDietPlan(plan)
        onBack()
    }

    Column(
        Modifier.fillMaxSize().background(CyberBgPrimary).statusBarsPadding()
    ) {
        // Top bar
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = CyberTextPrimary)
            }
            Column(Modifier.weight(1f)) {
                Text(if (existingPlan == null) "New Diet Plan" else "Edit Diet Plan",
                    color = CyberTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Black)
                Text("for $clientName", color = CyberTextMuted, fontSize = 12.sp)
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.weight(1f)
        ) {
            // Plan meta
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    DietTextField(value = title, label = "Plan Title", placeholder = "e.g. Weight Loss Plan") { title = it }
                    DietTextField(value = description, label = "Description", placeholder = "Brief overview of the plan...", maxLines = 3) { description = it }
                }
            }

            // Day tabs
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Days", color = CyberTextMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        TextButton(onClick = {
                            val nextLabel = WEEK_DAYS.getOrElse(days.size) { "Day ${days.size + 1}" }
                            days = days + DietDay(nextLabel)
                            selectedDay = days.size - 1
                        }) {
                            Icon(Icons.Filled.Add, null, modifier = Modifier.size(14.dp), tint = CyberAccent)
                            Spacer(Modifier.width(4.dp))
                            Text("Add Day", color = CyberAccent, fontSize = 12.sp)
                        }
                    }
                    androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        itemsIndexed(days) { idx, day ->
                            val sel = idx == selectedDay
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(10.dp))
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

            // Day label editor
            if (days.isNotEmpty()) {
                item {
                    DietTextField(
                        value = days[selectedDay].dayLabel,
                        label = "Day Label",
                        placeholder = "e.g. Monday or Day 1"
                    ) { newLabel ->
                        days = days.mapIndexed { i, d -> if (i == selectedDay) d.copy(dayLabel = newLabel) else d }
                    }
                }

                // Meals for selected day
                itemsIndexed(days[selectedDay].meals) { mealIdx, meal ->
                    MealEditorCard(
                        meal     = meal,
                        onUpdate = { updated ->
                            days = days.mapIndexed { i, d ->
                                if (i != selectedDay) d
                                else d.copy(meals = d.meals.mapIndexed { j, m -> if (j == mealIdx) updated else m })
                            }
                        },
                        onDelete = {
                            days = days.mapIndexed { i, d ->
                                if (i != selectedDay) d
                                else d.copy(meals = d.meals.filterIndexed { j, _ -> j != mealIdx })
                            }
                        }
                    )
                }

                item {
                    if (showAddMeal) {
                        Column(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                .background(CyberBgCard).padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            DietTextField(value = newMealName, label = "Meal Name", placeholder = "e.g. Breakfast") { newMealName = it }
                            DietTextField(value = newMealTime, label = "Time Slot", placeholder = "e.g. 8:00 – 9:00 AM") { newMealTime = it }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { showAddMeal = false; newMealName = ""; newMealTime = "" },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) { Text("Cancel", fontSize = 13.sp) }
                                Button(
                                    onClick = {
                                        if (newMealName.isNotBlank()) {
                                            val newMeal = DietMeal(newMealName.trim(), newMealTime.trim())
                                            days = days.mapIndexed { i, d ->
                                                if (i != selectedDay) d else d.copy(meals = d.meals + newMeal)
                                            }
                                            newMealName = ""; newMealTime = ""; showAddMeal = false
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = CyberAccent)
                                ) { Text("Add Meal", color = CyberAccentDark, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                            }
                        }
                    } else {
                        OutlinedButton(
                            onClick = { showAddMeal = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberTextMuted)
                        ) {
                            Icon(Icons.Filled.Add, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Add Meal", fontSize = 13.sp)
                        }
                    }
                }
            }

            // Coach notes
            item {
                DietTextField(value = notes, label = "Coach Notes (optional)", placeholder = "Additional tips or instructions for the member...", maxLines = 4) { notes = it }
            }
        }

        // Save buttons
        Column(
            Modifier.fillMaxWidth().background(CyberBgPrimary)
                .navigationBarsPadding().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { save("active") },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CyberAccent),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, null, tint = CyberAccentDark, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Save & Send to Member", color = CyberAccentDark, fontWeight = FontWeight.Black, fontSize = 15.sp)
            }
            OutlinedButton(
                onClick = { save("draft") },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberTextMuted)
            ) {
                Icon(Icons.Filled.Save, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Save as Draft", fontSize = 14.sp)
            }
        }
    }
}

// ─── Meal Editor Card ─────────────────────────────────────────────────────────

@Composable
private fun MealEditorCard(meal: DietMeal, onUpdate: (DietMeal) -> Unit, onDelete: () -> Unit) {
    var expanded    by remember { mutableStateOf(true) }
    var showAddFood by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(CyberBgCard)
            .border(1.dp, Color.White.copy(0.08f), RoundedCornerShape(12.dp))
    ) {
        // Meal header
        Row(
            Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(meal.mealName.ifBlank { "Unnamed Meal" }, color = CyberTextPrimary,
                    fontSize = 14.sp, fontWeight = FontWeight.Bold)
                if (meal.timeSlot.isNotBlank() || meal.foods.isNotEmpty())
                    Text(buildString {
                        if (meal.timeSlot.isNotBlank()) append(meal.timeSlot)
                        if (meal.foods.isNotEmpty()) {
                            if (meal.timeSlot.isNotBlank()) append(" · ")
                            append("${meal.foods.size} foods")
                            if (meal.totalCalories > 0) append(" · ${meal.totalCalories} kcal")
                        }
                    }, color = CyberTextMuted, fontSize = 12.sp)
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Filled.Delete, null, tint = CyberDanger.copy(0.7f), modifier = Modifier.size(16.dp))
            }
            Icon(if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                null, tint = CyberTextMuted, modifier = Modifier.size(18.dp))
        }

        AnimatedVisibility(visible = expanded, enter = expandVertically(), exit = shrinkVertically()) {
            Column(Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                meal.foods.forEachIndexed { idx, food ->
                    FoodItemRow(
                        food     = food,
                        onDelete = { onUpdate(meal.copy(foods = meal.foods.filterIndexed { i, _ -> i != idx })) }
                    )
                }
                if (showAddFood) {
                    FoodAddForm(
                        onSave   = { newFood -> onUpdate(meal.copy(foods = meal.foods + newFood)); showAddFood = false },
                        onCancel = { showAddFood = false }
                    )
                } else {
                    TextButton(
                        onClick = { showAddFood = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Add, null, modifier = Modifier.size(14.dp), tint = CyberAccent)
                        Spacer(Modifier.width(4.dp))
                        Text("Add Food Item", color = CyberAccent, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun FoodItemRow(food: DietFood, onDelete: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
            .background(CyberBgCardElevated).padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(food.name, color = CyberTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (food.quantity.isNotBlank()) Text(food.quantity, color = CyberTextMuted, fontSize = 11.sp)
                if (food.calories > 0) Text("${food.calories} kcal", color = Color(0xFFF59E0B), fontSize = 11.sp)
                if (food.proteinG > 0) Text("${food.proteinG}g P", color = Color(0xFF818CF8), fontSize = 11.sp)
            }
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Filled.Close, null, tint = CyberTextMuted, modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
private fun FoodAddForm(onSave: (DietFood) -> Unit, onCancel: () -> Unit) {
    var name     by remember { mutableStateOf("") }
    var qty      by remember { mutableStateOf("") }
    var calories by remember { mutableStateOf("") }
    var protein  by remember { mutableStateOf("") }
    var notes    by remember { mutableStateOf("") }

    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
            .background(CyberBgCardElevated).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DietTextField(Modifier.weight(2f), name, "Food Name *", "e.g. Brown Rice") { name = it }
            DietTextField(Modifier.weight(1f), qty, "Qty", "e.g. 1 cup") { qty = it }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DietNumberField(Modifier.weight(1f), calories, "Calories") { calories = it }
            DietNumberField(Modifier.weight(1f), protein, "Protein (g)") { protein = it }
        }
        DietTextField(value = notes, label = "Notes", placeholder = "Optional notes...") { notes = it }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp)) {
                Text("Cancel", fontSize = 12.sp)
            }
            Button(
                onClick = {
                    if (name.isNotBlank()) onSave(DietFood(name.trim(), qty.trim(),
                        calories.toIntOrNull() ?: 0, protein.toIntOrNull() ?: 0, notes.trim()))
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CyberAccent)
            ) { Text("Add", color = CyberAccentDark, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
        }
    }
}

// ─── Shared text field helpers ────────────────────────────────────────────────

@Composable
fun DietTextField(
    modifier: Modifier = Modifier,
    value: String,
    label: String,
    placeholder: String,
    maxLines: Int = 1,
    onChange: (String) -> Unit
) {
    Column(modifier) {
        Text(label, color = CyberTextMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            placeholder = { Text(placeholder, color = CyberTextMuted.copy(0.5f), fontSize = 13.sp) },
            maxLines = maxLines,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor   = CyberAccent,
                unfocusedBorderColor = Color.White.copy(0.12f),
                focusedTextColor     = CyberTextPrimary,
                unfocusedTextColor   = CyberTextPrimary,
                focusedContainerColor   = CyberBgCard,
                unfocusedContainerColor = CyberBgCard
            ),
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp)
        )
    }
}

@Composable
private fun DietNumberField(modifier: Modifier = Modifier, value: String, label: String, onChange: (String) -> Unit) {
    Column(modifier) {
        Text(label, color = CyberTextMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = { if (it.length <= 5) onChange(it.filter { c -> c.isDigit() }) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            maxLines = 1,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor   = CyberAccent,
                unfocusedBorderColor = Color.White.copy(0.12f),
                focusedTextColor     = CyberTextPrimary,
                unfocusedTextColor   = CyberTextPrimary,
                focusedContainerColor   = CyberBgCard,
                unfocusedContainerColor = CyberBgCard
            ),
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp)
        )
    }
}

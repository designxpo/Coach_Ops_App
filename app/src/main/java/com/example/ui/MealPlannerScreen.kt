@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MealPlannerAI
import com.example.data.PlannedDay
import com.example.data.PlannedMeal
import com.example.data.UserPreferences
import com.example.data.WeeklyMealPlan
import com.example.ui.theme.CyberAccent
import com.example.ui.theme.CyberBgCard
import com.example.ui.theme.CyberBgCardElevated
import com.example.ui.theme.CyberBgPrimary
import com.example.ui.theme.CyberDanger
import com.example.ui.theme.CyberSuccess
import com.example.ui.theme.CyberTextMuted
import com.example.ui.theme.CyberTextPrimary
import kotlinx.coroutines.launch

private val GOAL_OPTIONS = listOf("Weight Loss", "Muscle Gain", "Maintain Weight")

@Composable
fun MealPlannerScreen(
    onBack: () -> Unit,
    userPreferences: UserPreferences,
    onViewGroceryList: (WeeklyMealPlan) -> Unit
) {
    val scope = rememberCoroutineScope()

    // Settings state
    var calorieInput by rememberSaveable {
        mutableStateOf(userPreferences.trainingDayCalories.toString())
    }
    var proteinInput by rememberSaveable {
        mutableStateOf(userPreferences.trainingDayProteinG.toString())
    }
    var isVegetarian by rememberSaveable { mutableStateOf(false) }
    var selectedGoal by rememberSaveable { mutableStateOf(GOAL_OPTIONS[0]) }
    var goalDropdownExpanded by remember { mutableStateOf(false) }

    // UI state
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var mealPlan by remember { mutableStateOf<WeeklyMealPlan?>(null) }

    // Track which days are expanded
    val expandedDays = remember { mutableStateOf(setOf<Int>()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBgPrimary)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // ── Header ────────────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
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
                    modifier = Modifier.size(18.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "AI Meal Planner",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = CyberTextPrimary
                )
                Text(
                    "7-day Indian meal plan",
                    fontSize = 12.sp,
                    color = CyberTextMuted
                )
            }
            // ProCoach AI badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(CyberAccent.copy(alpha = 0.15f))
                    .border(1.dp, CyberAccent.copy(alpha = 0.4f), RoundedCornerShape(999.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    "✦ ProCoach AI",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyberAccent
                )
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            // ── Settings card ──────────────────────────────────────────────────
            if (mealPlan == null && !isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(CyberBgCard)
                            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(20.dp))
                            .padding(18.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text(
                                "Customize Your Plan",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = CyberTextPrimary
                            )

                            // Calorie goal input
                            OutlinedTextField(
                                value = calorieInput,
                                onValueChange = { calorieInput = it.filter { c -> c.isDigit() } },
                                label = { Text("Daily Calorie Goal (kcal)", color = CyberTextMuted, fontSize = 13.sp) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CyberAccent,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                                    focusedTextColor = CyberTextPrimary,
                                    unfocusedTextColor = CyberTextPrimary,
                                    cursorColor = CyberAccent
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )

                            // Protein target input
                            OutlinedTextField(
                                value = proteinInput,
                                onValueChange = { proteinInput = it.filter { c -> c.isDigit() } },
                                label = { Text("Protein Target (g/day)", color = CyberTextMuted, fontSize = 13.sp) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CyberAccent,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                                    focusedTextColor = CyberTextPrimary,
                                    unfocusedTextColor = CyberTextPrimary,
                                    cursorColor = CyberAccent
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )

                            // Vegetarian toggle
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        "Vegetarian Diet",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = CyberTextPrimary
                                    )
                                    Text(
                                        if (isVegetarian) "No meat, chicken or fish" else "Includes chicken, eggs & fish",
                                        fontSize = 11.sp,
                                        color = CyberTextMuted
                                    )
                                }
                                Switch(
                                    checked = isVegetarian,
                                    onCheckedChange = { isVegetarian = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = CyberBgPrimary,
                                        checkedTrackColor = CyberAccent,
                                        uncheckedThumbColor = CyberTextMuted,
                                        uncheckedTrackColor = CyberBgCardElevated
                                    )
                                )
                            }

                            // Goal dropdown
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    "Fitness Goal",
                                    fontSize = 13.sp,
                                    color = CyberTextMuted,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Box {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(CyberBgCardElevated)
                                            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                            .clickable { goalDropdownExpanded = true }
                                            .padding(horizontal = 14.dp, vertical = 14.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(selectedGoal, fontSize = 14.sp, color = CyberTextPrimary)
                                            Icon(
                                                if (goalDropdownExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                                contentDescription = null,
                                                tint = CyberTextMuted,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    DropdownMenu(
                                        expanded = goalDropdownExpanded,
                                        onDismissRequest = { goalDropdownExpanded = false },
                                        modifier = Modifier.background(CyberBgCardElevated)
                                    ) {
                                        GOAL_OPTIONS.forEach { option ->
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        option,
                                                        color = if (option == selectedGoal) CyberAccent else CyberTextPrimary,
                                                        fontWeight = if (option == selectedGoal) FontWeight.Bold else FontWeight.Normal
                                                    )
                                                },
                                                onClick = {
                                                    selectedGoal = option
                                                    goalDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            // Error message
                            if (errorMessage != null) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(CyberDanger.copy(alpha = 0.12f))
                                        .border(1.dp, CyberDanger.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                        .padding(12.dp)
                                ) {
                                    Text(errorMessage!!, fontSize = 13.sp, color = CyberDanger)
                                }
                            }

                            // Generate button
                            Button(
                                onClick = {
                                    val calories = calorieInput.toIntOrNull() ?: return@Button
                                    val protein = proteinInput.toIntOrNull() ?: return@Button
                                    errorMessage = null
                                    isLoading = true
                                    scope.launch {
                                        val result = MealPlannerAI.generate(
                                            goalCalories = calories,
                                            proteinG = protein,
                                            isVegetarian = isVegetarian,
                                            goal = selectedGoal.lowercase()
                                        )
                                        isLoading = false
                                        result.onSuccess { plan ->
                                            mealPlan = plan
                                            expandedDays.value = setOf(0) // expand first day by default
                                        }
                                        result.onFailure { e ->
                                            errorMessage = if (e is java.net.SocketTimeoutException)
                                                "This is taking longer than usual — check your connection and try again."
                                            else
                                                "Failed to generate plan. Please try again."
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CyberAccent,
                                    contentColor = CyberBgPrimary
                                )
                            ) {
                                Icon(
                                    Icons.Default.Restaurant,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Generate 7-Day Plan",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }
            }

            // ── Loading state ──────────────────────────────────────────────────
            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            CircularProgressIndicator(
                                color = CyberAccent,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(52.dp)
                            )
                            Text(
                                "✦ ProCoach AI is crafting your plan...",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = CyberAccent
                            )
                            Text(
                                "This may take up to 30 seconds",
                                fontSize = 12.sp,
                                color = CyberTextMuted
                            )
                        }
                    }
                }
            }

            // ── Plan display ───────────────────────────────────────────────────
            mealPlan?.let { plan ->
                item {
                    // Plan summary strip
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(CyberAccent.copy(alpha = 0.08f))
                            .border(1.dp, CyberAccent.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    "7-Day Plan Ready",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = CyberAccent
                                )
                                Text(
                                    "Goal: ${plan.goal}",
                                    fontSize = 12.sp,
                                    color = CyberTextMuted
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(CyberAccent.copy(alpha = 0.15f))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                        .clickable {
                                            mealPlan = null
                                            expandedDays.value = emptySet()
                                        }
                                ) {
                                    Text(
                                        "Regenerate",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CyberAccent
                                    )
                                }
                            }
                        }
                    }
                }

                itemsIndexed(plan.days) { index, day ->
                    DayCard(
                        day = day,
                        isExpanded = index in expandedDays.value,
                        onToggle = {
                            expandedDays.value = if (index in expandedDays.value) {
                                expandedDays.value - index
                            } else {
                                expandedDays.value + index
                            }
                        }
                    )
                }

                item { Spacer(Modifier.height(8.dp)) }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }

        // ── Bottom: View Grocery List button ──────────────────────────────────
        mealPlan?.let { plan ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CyberBgPrimary)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Button(
                    onClick = { onViewGroceryList(plan) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyberAccent,
                        contentColor = CyberBgPrimary
                    )
                ) {
                    Text(
                        "🛒  View Grocery List",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}

@Composable
private fun DayCard(
    day: PlannedDay,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(CyberBgCard)
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(18.dp))
            .animateContentSize(animationSpec = tween(250))
    ) {
        // Day header row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle() }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CyberAccent.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        day.dayLabel.take(2),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = CyberAccent
                    )
                }
                Column {
                    Text(
                        day.dayLabel,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = CyberTextPrimary
                    )
                    Text(
                        "${day.totalCalories} kcal total  ·  ${day.meals.size} meals",
                        fontSize = 11.sp,
                        color = CyberTextMuted
                    )
                }
            }
            Icon(
                if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                tint = CyberTextMuted,
                modifier = Modifier.size(20.dp)
            )
        }

        // Expandable meals list
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(animationSpec = tween(250)),
            exit = shrinkVertically(animationSpec = tween(200))
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
                day.meals.forEachIndexed { idx, meal ->
                    if (idx > 0) Spacer(Modifier.height(8.dp))
                    MealRow(meal = meal)
                }
            }
        }
    }
}

@Composable
private fun MealRow(meal: PlannedMeal) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CyberBgCardElevated)
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            // Time slot badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(CyberAccent.copy(alpha = 0.10f))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    meal.timeSlot,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyberAccent
                )
            }

            // Meal name
            Text(
                meal.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = CyberTextPrimary
            )

            // Macros row — FlowRow so all 4 chips wrap on narrow screens
            androidx.compose.foundation.layout.FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MacroChip("${meal.calories} kcal", Color(0xFFFACC15))
                MacroChip("P ${meal.proteinG}g", Color(0xFF6366F1))
                MacroChip("C ${meal.carbsG}g", Color(0xFF22C55E))
                MacroChip("F ${meal.fatG}g", Color(0xFFEF4444))
            }
        }
    }
}

@Composable
private fun MacroChip(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Text(
            text,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            maxLines = 1,
            softWrap = false
        )
    }
}

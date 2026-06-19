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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.ClientGoal
import com.example.data.IndianFoodItem
import com.example.data.IndianMeal
import com.example.data.IndianMealPlan
import com.example.data.NutritionRepository
import com.example.ui.theme.CyberAccent
import com.example.ui.theme.CyberAccentDark
import com.example.ui.theme.CyberBgCard
import com.example.ui.theme.CyberBgCardElevated
import com.example.ui.theme.CyberBgPrimary
import com.example.ui.theme.CyberTextMuted
import com.example.ui.theme.CyberTextPrimary
import com.example.ui.theme.CyberTextSecondary

// ─── Goal → header image fallbacks (used when admin hasn't set a custom URL) ──

private val goalImages = mapOf(
    ClientGoal.BUILD_MUSCLE        to "https://images.unsplash.com/photo-1581009137042-c552e485697a?w=600&h=280&fit=crop&q=80",
    ClientGoal.LOSE_FAT            to "https://images.unsplash.com/photo-1571019613454-1cb2f99b2d8b?w=600&h=280&fit=crop&q=80",
    ClientGoal.IMPROVE_CARDIO      to "https://images.unsplash.com/photo-1561214115-f2f134cc4912?w=600&h=280&fit=crop&q=80",
    ClientGoal.IMPROVE_FLEXIBILITY to "https://images.unsplash.com/photo-1552196563-55cd4e45efb3?w=600&h=280&fit=crop&q=80",
    ClientGoal.GENERAL_FITNESS     to "https://images.unsplash.com/photo-1490645935967-10de6ba17061?w=600&h=280&fit=crop&q=80"
)

// ─── Goal icon image fallbacks ────────────────────────────────────────────────

private val goalIconImages = mapOf(
    ClientGoal.BUILD_MUSCLE        to "https://images.unsplash.com/photo-1534438327276-14e5300c3a48?w=80&h=80&fit=crop&q=80",
    ClientGoal.LOSE_FAT            to "https://images.unsplash.com/photo-1549060279-7e168fcee0c2?w=80&h=80&fit=crop&q=80",
    ClientGoal.IMPROVE_CARDIO      to "https://images.unsplash.com/photo-1571019613454-1cb2f99b2d8b?w=80&h=80&fit=crop&q=80",
    ClientGoal.IMPROVE_FLEXIBILITY to "https://images.unsplash.com/photo-1575052814086-f385e2e2ad1b?w=80&h=80&fit=crop&q=80",
    ClientGoal.GENERAL_FITNESS     to "https://images.unsplash.com/photo-1490645935967-10de6ba17061?w=80&h=80&fit=crop&q=80"
)

// Use admin-set URL when available, fall back to hardcoded
private fun resolvedHeaderImage(plan: com.example.data.IndianMealPlan?): String =
    plan?.headerImageUrl?.takeIf { it.isNotBlank() } ?: goalImages[plan?.goal] ?: goalImages[ClientGoal.GENERAL_FITNESS]!!

private fun resolvedIconImage(plan: com.example.data.IndianMealPlan?): String =
    plan?.iconImageUrl?.takeIf { it.isNotBlank() } ?: goalIconImages[plan?.goal] ?: goalIconImages[ClientGoal.GENERAL_FITNESS]!!

// ─── Food category → image map ────────────────────────────────────────────────

private fun foodImageUrl(name: String): String {
    val lower = name.lowercase()
    return when {
        "banana"        in lower -> "https://images.unsplash.com/photo-1571771894821-ce9b6c11b08e?w=80&h=80&fit=crop&q=80"
        "peanut"        in lower -> "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=80&h=80&fit=crop&q=80"
        "milk"          in lower -> "https://images.unsplash.com/photo-1563636619-e9143da7973b?w=80&h=80&fit=crop&q=80"
        "egg"           in lower -> "https://images.unsplash.com/photo-1518569656558-1f25e69d2049?w=80&h=80&fit=crop&q=80"
        "paratha"       in lower -> "https://images.unsplash.com/photo-1565557623262-b51c2513a641?w=80&h=80&fit=crop&q=80"
        "curd"  in lower || "dahi" in lower || "raita" in lower ->
            "https://images.unsplash.com/photo-1571197826887-f3b6a3af5bd4?w=80&h=80&fit=crop&q=80"
        "rice"          in lower -> "https://images.unsplash.com/photo-1516684732162-798a0062be99?w=80&h=80&fit=crop&q=80"
        "dal"  in lower || "lentil" in lower ->
            "https://images.unsplash.com/photo-1546833999-b9f581a1996d?w=80&h=80&fit=crop&q=80"
        "chicken"       in lower -> "https://images.unsplash.com/photo-1598103442097-8b74394b95c3?w=80&h=80&fit=crop&q=80"
        "roti" in lower || "paratha" in lower || "jowar" in lower || "bajra" in lower ->
            "https://images.unsplash.com/photo-1565557623262-b51c2513a641?w=80&h=80&fit=crop&q=80"
        "salad"         in lower -> "https://images.unsplash.com/photo-1512621776951-a57141f2eefd?w=80&h=80&fit=crop&q=80"
        "paneer"        in lower -> "https://images.unsplash.com/photo-1565557623262-b51c2513a641?w=80&h=80&fit=crop&q=80"
        "oats"  in lower || "poha" in lower || "upma" in lower ->
            "https://images.unsplash.com/photo-1614961233913-a5113a4a34ed?w=80&h=80&fit=crop&q=80"
        "tea"   in lower || "coffee" in lower || "chai" in lower ->
            "https://images.unsplash.com/photo-1556742049-0cfed4f6a45d?w=80&h=80&fit=crop&q=80"
        "coconut"       in lower -> "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=80&h=80&fit=crop&q=80"
        "sprout"        in lower -> "https://images.unsplash.com/photo-1512621776951-a57141f2eefd?w=80&h=80&fit=crop&q=80"
        else -> "https://images.unsplash.com/photo-1490645935967-10de6ba17061?w=80&h=80&fit=crop&q=80"
    }
}

// ─── Screen ───────────────────────────────────────────────────────────────────

@Composable
fun NutritionScreen(
    viewModel: FitnessViewModel,
    goal: ClientGoal = viewModel.clientGoal,
    onBack: (() -> Unit)? = null,
    isCoachMode: Boolean = false
) {
    // Use live Firestore data from ViewModel (falls back to hardcoded if not yet synced)
    val planState by viewModel.mealPlan.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    var copiedMsg by remember { mutableStateOf("") }

    if (planState == null) {
        Box(Modifier.fillMaxSize().background(CyberBgPrimary), contentAlignment = Alignment.Center) {
            Text("No plan available", color = CyberTextMuted, fontSize = 16.sp)
        }
        return
    }
    // Local val enables smart casts throughout the rest of this composable
    val plan = planState!!

    Box(modifier = Modifier.fillMaxSize()) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(CyberBgPrimary).navigationBarsPadding(),
        contentPadding = PaddingValues(bottom = if (isCoachMode) 96.dp else 32.dp)
    ) {
        // ── Hero image + back button ──────────────────────────────────────────
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            ) {
                val heroUrl = resolvedHeaderImage(planState)
                if (heroUrl.isNotBlank()) {
                    AsyncImage(
                        model = heroUrl,
                        contentDescription = goal.label,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(Modifier.fillMaxSize().background(Color(0xFF1A2E1A)))
                }
                // Dark gradient overlay
                Box(
                    modifier = Modifier.fillMaxSize().background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            0f to Color.Black.copy(0.35f),
                            0.5f to Color.Black.copy(0.15f),
                            1f to Color.Black.copy(0.75f)
                        )
                    )
                )
                // Back button
                if (onBack != null) {
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
                }
                // Goal title on image
                Column(
                    modifier = Modifier.align(Alignment.BottomStart).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(goal.label, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    Text("${goal.emoji} Indian Diet Plan", fontSize = 12.sp, color = Color.White.copy(0.75f))
                }
            }
        }

        // ── Meal plan header card ─────────────────────────────────────────────
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(CyberBgCard)
                    .border(1.dp, Color.White.copy(0.06f), RoundedCornerShape(18.dp))
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Goal icon image (replaces emoji)
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(CyberAccent.copy(0.12f))
                    ) {
                        val iconUrl = resolvedIconImage(planState)
                        if (iconUrl.isNotBlank()) {
                            AsyncImage(
                                model = iconUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize().clip(CircleShape)
                            )
                            Box(Modifier.fillMaxSize().clip(CircleShape).background(Color.Black.copy(0.2f)))
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Meal Plan", fontSize = 11.sp, color = CyberTextMuted, fontWeight = FontWeight.SemiBold)
                        Text(goal.label, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = CyberTextPrimary)
                    }
                }
                // kcal badge (dark green pill)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0D3320))
                        .border(1.dp, Color(0xFF10B981).copy(0.3f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        "${plan.dailyCalories} kcal",
                        fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF10B981)
                    )
                }
            }
        }

        // ── Macro cards (3 in a row with colored backgrounds) ────────────────
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MacroCard(value = "${plan.proteinG}g", label = "Protein",
                    bg = Color(0xFF132A13), accent = CyberAccent, modifier = Modifier.weight(1f))
                MacroCard(value = "${plan.carbsG}g", label = "Carbs",
                    bg = Color(0xFF2A1E0A), accent = Color(0xFFF59E0B), modifier = Modifier.weight(1f))
                MacroCard(value = "${plan.fatG}g", label = "Fat",
                    bg = Color(0xFF0A2A1E), accent = Color(0xFF10B981), modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
        }

        // ── Hydration strip ───────────────────────────────────────────────────
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF0A1929))
                    .border(1.dp, Color(0xFF1E3A5F), RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("💧", fontSize = 18.sp)
                Text(
                    plan.hydration,
                    fontSize = 13.sp, color = Color(0xFF90CAF9), lineHeight = 18.sp
                )
            }
            Spacer(Modifier.height(12.dp))
        }

        // ── General Tips collapsible ──────────────────────────────────────────
        item {
            var expanded by remember { mutableStateOf(false) }
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(if (expanded) 14.dp else 14.dp))
                        .background(CyberBgCard)
                        .border(1.dp, Color.White.copy(0.06f), RoundedCornerShape(14.dp))
                        .clickable { expanded = !expanded }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("General Tips", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = CyberTextPrimary)
                    Icon(
                        if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                        null, tint = CyberTextMuted, modifier = Modifier.size(20.dp)
                    )
                }
                AnimatedVisibility(visible = expanded, enter = expandVertically(), exit = shrinkVertically()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(bottomStart = 14.dp, bottomEnd = 14.dp))
                            .background(CyberBgCard)
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        plan.generalTips.forEachIndexed { i, tip ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(
                                    modifier = Modifier.size(20.dp).clip(CircleShape).background(CyberAccent),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("${i + 1}", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = CyberAccentDark)
                                }
                                Text(tip, fontSize = 13.sp, color = CyberTextSecondary, lineHeight = 18.sp, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // ── Meal cards ────────────────────────────────────────────────────────
        items(plan.meals) { meal ->
            MealCard(meal = meal)
            Spacer(Modifier.height(12.dp))
        }
    } // end LazyColumn

    // ── Coach sticky bottom bar ───────────────────────────────────────────────
    if (isCoachMode) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        listOf(androidx.compose.ui.graphics.Color.Transparent, CyberBgPrimary.copy(0.97f), CyberBgPrimary)
                    )
                )
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (copiedMsg.isNotEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(CyberAccent.copy(0.15f)).padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(copiedMsg, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CyberAccent)
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(CyberAccent)
                    .clickable {
                        val text = buildString {
                            appendLine("🥗 ${goal.emoji} ${goal.label} — Indian Diet Plan")
                            appendLine("Daily: ${plan.dailyCalories} kcal · P:${plan.proteinG}g · C:${plan.carbsG}g · F:${plan.fatG}g")
                            appendLine()
                            plan.meals.forEach { meal ->
                                val mealKcal = meal.items.sumOf { it.calories }
                                appendLine("【${meal.name}】 ${meal.timeSlot} (~$mealKcal kcal)")
                                meal.items.forEach { item ->
                                    appendLine("  • ${item.name} — ${item.quantity} (${item.calories} kcal)")
                                }
                                appendLine()
                            }
                        }
                        clipboardManager.setText(AnnotatedString(text))
                        copiedMsg = "Meal plan copied to clipboard ✓"
                        scope.launch { delay(2500); copiedMsg = "" }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("Copy Meal Plan", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = CyberAccentDark)
            }
        }
    }
    } // end Box
}

// ─── Macro card ───────────────────────────────────────────────────────────────

@Composable
private fun MacroCard(value: String, label: String, bg: Color, accent: Color, modifier: Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .border(1.dp, accent.copy(0.3f), RoundedCornerShape(16.dp))
            .padding(vertical = 16.dp, horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = accent)
            Spacer(Modifier.height(4.dp))
            Text(label, fontSize = 11.sp, color = accent.copy(0.7f), fontWeight = FontWeight.SemiBold)
        }
    }
}

// ─── Meal card ────────────────────────────────────────────────────────────────

@Composable
private fun MealCard(meal: IndianMeal) {
    val totalCal = meal.items.sumOf { it.calories }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(CyberBgCard)
            .border(1.dp, Color.White.copy(0.06f), RoundedCornerShape(18.dp))
            .padding(bottom = 4.dp)
    ) {
        // Meal header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(meal.name, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = CyberTextPrimary)
            Text(meal.timeSlot, fontSize = 11.sp, color = CyberTextMuted)
        }

        // Food items
        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
            meal.items.forEachIndexed { index, food ->
                FoodItemRow(food = food)
                if (index < meal.items.lastIndex) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(0.5.dp)
                            .background(Color.White.copy(0.05f))
                    )
                }
            }
        }

        // Meal total
        Box(
            modifier = Modifier
                .align(Alignment.End)
                .padding(horizontal = 14.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(CyberBgCardElevated)
                .padding(horizontal = 12.dp, vertical = 5.dp)
        ) {
            Text(
                "Total: $totalCal kcal",
                fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CyberAccent
            )
        }
    }
}

// ─── Food item row ────────────────────────────────────────────────────────────

@Composable
private fun FoodItemRow(food: IndianFoodItem) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Food image (replaces veg/non-veg dot)
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(CyberBgCardElevated)
            ) {
                AsyncImage(
                    model = food.imageUrl.takeIf { it.isNotBlank() } ?: foodImageUrl(food.name),
                    contentDescription = food.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                // Veg/non-veg dot overlay (bottom-left)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(3.dp)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (food.isVegetarian) Color(0xFF10B981) else Color(0xFFEF4444))
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(food.name, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = CyberTextPrimary)
                Text(food.quantity, fontSize = 12.sp, color = CyberTextMuted)
            }

            // Calorie badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0D3320))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    "${food.calories} kcal",
                    fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981)
                )
            }
        }

        // Macro chips row
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            MacroChip("P:${food.proteinG}g", CyberAccent)
            MacroChip("C:${food.carbsG}g",  Color(0xFFF59E0B))
            MacroChip("F:${food.fatG}g",    Color(0xFF10B981))
        }

        // Benefits (expandable on tap)
        AnimatedVisibility(visible = expanded, enter = expandVertically(), exit = shrinkVertically()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(CyberBgCardElevated)
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Text(
                    food.benefits,
                    fontSize = 12.sp, color = CyberTextSecondary, lineHeight = 17.sp
                )
            }
        }
    }
}

// ─── Macro chip ───────────────────────────────────────────────────────────────

@Composable
private fun MacroChip(label: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

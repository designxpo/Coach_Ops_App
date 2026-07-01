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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.runtime.toMutableStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.WeeklyMealPlan
import com.example.ui.theme.CyberAccent
import com.example.ui.theme.CyberBgCard
import com.example.ui.theme.CyberBgCardElevated
import com.example.ui.theme.CyberBgPrimary
import com.example.ui.theme.CyberSuccess
import com.example.ui.theme.CyberTextMuted
import com.example.ui.theme.CyberTextPrimary
import kotlinx.coroutines.launch

// ── Category definitions ────────────────────────────────────────────────────

private enum class GroceryCategory(val label: String, val emoji: String, val color: Color) {
    VEGETABLES("Vegetables", "🥦", Color(0xFF22C55E)),
    PROTEINS("Proteins", "🥩", Color(0xFF6366F1)),
    GRAINS("Grains & Cereals", "🌾", Color(0xFFF59E0B)),
    DAIRY("Dairy", "🥛", Color(0xFF60A5FA)),
    PANTRY("Pantry & Spices", "🧂", Color(0xFFEC4899))
}

private val VEGETABLE_KEYWORDS = setOf(
    "onion", "tomato", "spinach", "potato", "carrot", "capsicum", "brinjal",
    "cauliflower", "peas", "cucumber", "ginger", "garlic", "chilli", "coriander",
    "methi", "palak", "gourd", "bhindi", "okra", "beans", "cabbage", "celery",
    "lettuce", "mushroom", "pepper", "zucchini", "beetroot", "radish", "turnip",
    "leek", "kale", "broccoli", "asparagus", "corn", "pumpkin"
)

private val PROTEIN_KEYWORDS = setOf(
    "chicken", "paneer", "dal", "lentil", "egg", "fish", "tofu", "rajma",
    "chana", "soya", "peanut", "curd", "yogurt", "mutton", "beef", "prawn",
    "shrimp", "tuna", "salmon", "moong", "urad", "masoor", "kidney", "chickpea",
    "protein", "soy", "tempeh", "sprouts", "legume", "bean"
)

private val GRAIN_KEYWORDS = setOf(
    "rice", "wheat", "roti", "bread", "oats", "poha", "upma", "semolina",
    "quinoa", "millet", "bajra", "jowar", "ragi", "dalia", "barley", "pasta",
    "noodle", "flour", "maida", "suji", "rava", "cornflour", "besan", "flattened"
)

private val DAIRY_KEYWORDS = setOf(
    "milk", "cheese", "butter", "ghee", "cream", "paneer", "curd", "yogurt",
    "buttermilk", "lassi", "whey", "condensed", "evaporated"
)

private fun categorize(ingredient: String): GroceryCategory {
    val lower = ingredient.lowercase()
    return when {
        DAIRY_KEYWORDS.any { lower.contains(it) }      -> GroceryCategory.DAIRY
        PROTEIN_KEYWORDS.any { lower.contains(it) }    -> GroceryCategory.PROTEINS
        GRAIN_KEYWORDS.any { lower.contains(it) }      -> GroceryCategory.GRAINS
        VEGETABLE_KEYWORDS.any { lower.contains(it) }  -> GroceryCategory.VEGETABLES
        else                                            -> GroceryCategory.PANTRY
    }
}

// ── Extract + deduplicate ingredients from the plan ─────────────────────────

private fun extractIngredients(plan: WeeklyMealPlan): Map<GroceryCategory, List<String>> {
    val raw = plan.days
        .flatMap { it.meals }
        .flatMap { it.ingredients }
        .map { it.trim() }
        .filter { it.isNotEmpty() }

    // Deduplicate: normalise to lowercase base word for comparison, keep first seen
    val seen = mutableSetOf<String>()
    val deduped = raw.filter { ingredient ->
        // Use first word (the ingredient name, ignoring quantity/unit)
        val key = ingredient.lowercase().split(" ").firstOrNull { it.length > 2 } ?: ingredient.lowercase()
        seen.add(key)
    }

    return deduped
        .groupBy { categorize(it) }
        .toSortedMap(compareBy { it.ordinal })
}

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
fun GroceryListScreen(
    onBack: () -> Unit,
    plan: WeeklyMealPlan
) {
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Build categorised ingredient map once
    val categorisedIngredients = remember(plan) { extractIngredients(plan) }

    // Flat list of all ingredients for "Copy All"
    val allIngredients = remember(categorisedIngredients) {
        categorisedIngredients.entries.flatMap { (cat, items) ->
            listOf("── ${cat.label} ──") + items
        }
    }

    // Checked state per ingredient (key = ingredient string)
    val checkedState: SnapshotStateMap<String, Boolean> = remember(plan) {
        allIngredients.map { it to false }.toMutableStateMap()
    }

    val totalItems = remember(categorisedIngredients) {
        categorisedIngredients.values.sumOf { it.size }
    }
    val checkedCount = checkedState.count { it.value }

    Box(modifier = Modifier.fillMaxSize().background(CyberBgPrimary)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // ── Header ───────────────────────────────────────────────────────
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
                        "Grocery List",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = CyberTextPrimary
                    )
                    Text(
                        "$totalItems items  ·  $checkedCount checked",
                        fontSize = 12.sp,
                        color = CyberTextMuted
                    )
                }
                // Copy all button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(CyberBgCard)
                        .clickable {
                            val text = allIngredients.joinToString("\n")
                            clipboard.setText(AnnotatedString(text))
                            scope.launch { snackbarHostState.showSnackbar("Grocery list copied!") }
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = "Copy",
                            tint = CyberAccent,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            "Copy All",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyberAccent
                        )
                    }
                }
            }

            // ── Progress strip ────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CyberBgCard)
                    .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (checkedCount > 0) CyberSuccess else CyberTextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            "$checkedCount / $totalItems items collected",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (checkedCount > 0) CyberSuccess else CyberTextMuted
                        )
                    }
                    if (checkedCount > 0) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(CyberBgCardElevated)
                                .clickable { checkedState.keys.forEach { checkedState[it] = false } }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("Reset", fontSize = 11.sp, color = CyberTextMuted)
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Categorised grocery list ───────────────────────────────────────
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                categorisedIngredients.forEach { (category, items) ->
                    if (items.isNotEmpty()) {
                        item(key = category.name + "_header") {
                            CategoryHeader(category = category, count = items.size)
                        }
                        items(items, key = { it }) { ingredient ->
                            val isChecked = checkedState[ingredient] ?: false
                            IngredientRow(
                                ingredient = ingredient,
                                isChecked = isChecked,
                                onCheckedChange = { checkedState[ingredient] = it }
                            )
                        }
                        item(key = category.name + "_spacer") {
                            Spacer(Modifier.height(4.dp))
                        }
                    }
                }

                item {
                    Spacer(Modifier.height(16.dp))
                }
            }
        }

        // Snackbar
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
        )
    }
}

@Composable
private fun CategoryHeader(category: GroceryCategory, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(category.color.copy(alpha = 0.10f))
            .border(1.dp, category.color.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(category.emoji, fontSize = 18.sp)
            Text(
                category.label,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                color = category.color
            )
        }
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(category.color.copy(alpha = 0.18f))
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(
                "$count",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = category.color
            )
        }
    }
}

@Composable
private fun IngredientRow(
    ingredient: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(CyberBgCard)
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(10.dp))
            .clickable { onCheckedChange(!isChecked) }
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(vertical = 6.dp)
        ) {
            Checkbox(
                checked = isChecked,
                onCheckedChange = onCheckedChange,
                colors = CheckboxDefaults.colors(
                    checkedColor = CyberAccent,
                    uncheckedColor = CyberTextMuted,
                    checkmarkColor = CyberBgPrimary
                )
            )
            Text(
                text = ingredient,
                fontSize = 14.sp,
                color = if (isChecked) CyberTextMuted else CyberTextPrimary,
                fontWeight = if (isChecked) FontWeight.Normal else FontWeight.Medium,
                textDecoration = if (isChecked) TextDecoration.LineThrough else TextDecoration.None,
                modifier = Modifier.weight(1f)
            )
            if (isChecked) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = CyberSuccess.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp).padding(end = 8.dp)
                )
            }
        }
    }
}

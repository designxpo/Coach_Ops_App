package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FoodDiary
import com.example.data.FoodDiaryEntry
import com.example.data.SupplementCategory
import com.example.data.SupplementDb
import com.example.data.SupplementProduct
import com.example.ui.theme.CyberAccent
import com.example.ui.theme.CyberAccentDark
import com.example.ui.theme.CyberBgCard
import com.example.ui.theme.CyberBgCardElevated
import com.example.ui.theme.CyberTextMuted
import com.example.ui.theme.CyberTextPrimary
import com.example.ui.theme.CyberTextSecondary

/**
 * Supplement logger — search the built-in brand database (whey, isolate,
 * casein, plant, gainers, creatine, bars, nut butters…), pick a serving
 * count and log it into the food diary. Protein counts toward the daily
 * macro totals automatically.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupplementPickerSheet(
    dateKey: String,
    onDismiss: () -> Unit,
    onSave: (FoodDiaryEntry) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf<SupplementCategory?>(null) }
    var selected by remember { mutableStateOf<SupplementProduct?>(null) }
    var servings by remember { mutableFloatStateOf(1f) }
    var mealType by remember { mutableStateOf(FoodDiary.mealTypeForNow()) }

    val results = remember(query, category) { SupplementDb.search(query, category) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = CyberBgCardElevated,
    ) {
        Column(
            Modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding()
        ) {
            Text("Log Supplement", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = CyberTextPrimary)
            Spacer(Modifier.height(4.dp))
            Text(
                "${SupplementDb.PRODUCTS.size} products · label values per serving (approximate)",
                fontSize = 12.sp, color = CyberTextMuted
            )
            Spacer(Modifier.height(14.dp))

            val sel = selected
            if (sel == null) {
                // ── Browse: search + category chips + results ────────────────
                GymTextField(query, { query = it }, "Search brand or product",
                    "e.g. muscleblaze whey, creatine, yoga bar")
                Spacer(Modifier.height(10.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        CategoryChip("All", category == null) { category = null }
                    }
                    items(SupplementCategory.entries) { c ->
                        CategoryChip("${c.emoji} ${c.label}", category == c) {
                            category = if (category == c) null else c
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))

                if (results.isEmpty()) {
                    Box(Modifier.fillMaxWidth().padding(vertical = 28.dp), contentAlignment = Alignment.Center) {
                        Text("No match — try just the brand name", fontSize = 13.sp, color = CyberTextMuted)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 420.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(results) { p ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(CyberBgCard)
                                    .clickable { selected = p; servings = 1f }
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(p.category.emoji, fontSize = 20.sp)
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("${p.brand} ${p.name}", fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold, color = CyberTextPrimary)
                                    Text(p.servingDesc + if (p.flavorNote.isNotEmpty()) " · ${p.flavorNote}" else "",
                                        fontSize = 11.sp, color = CyberTextMuted)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("${fmtG(p.proteinG)}g", fontSize = 15.sp,
                                        fontWeight = FontWeight.ExtraBold, color = CyberAccent)
                                    Text("protein", fontSize = 10.sp, color = CyberTextMuted)
                                }
                            }
                        }
                    }
                }
            } else {
                // ── Detail: serving stepper + meal + log ────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CyberBgCard)
                        .border(1.dp, CyberAccent.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("${sel.brand} ${sel.name}", fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold, color = CyberTextPrimary)
                            Text(sel.servingDesc + if (sel.flavorNote.isNotEmpty()) " · ${sel.flavorNote}" else "",
                                fontSize = 11.sp, color = CyberTextMuted)
                        }
                        Text("‹ Back", fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                            color = CyberTextSecondary,
                            modifier = Modifier.clickable { selected = null }.padding(4.dp))
                    }
                    Spacer(Modifier.height(10.dp))
                    val n = SupplementDb.toNutrition(sel, servings)
                    Text(
                        "🔥 ${n.calories} kcal · 💪 ${fmtG(n.proteinG)}g protein · ⚡ ${fmtG(n.carbsG)}g carbs · 🥑 ${fmtG(n.fatG)}g fat",
                        fontSize = 13.sp, color = CyberTextSecondary, lineHeight = 19.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("Ingredients: ${sel.ingredients}", fontSize = 11.sp,
                        color = CyberTextMuted, lineHeight = 16.sp)
                }

                Spacer(Modifier.height(14.dp))
                Text("Servings", fontSize = 12.sp, color = CyberTextMuted)
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    StepperButton("−") { if (servings > 0.5f) servings -= 0.5f }
                    Text(
                        if (servings == servings.toInt().toFloat()) "${servings.toInt()}"
                        else "%.1f".format(servings),
                        fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = CyberTextPrimary
                    )
                    StepperButton("＋") { if (servings < 5f) servings += 0.5f }
                    Text(sel.servingDesc.substringAfter("1 ").ifEmpty { "serving(s)" },
                        fontSize = 12.sp, color = CyberTextMuted)
                }

                Spacer(Modifier.height(14.dp))
                Text("Meal", fontSize = 12.sp, color = CyberTextMuted)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MEAL_ORDER.forEach { (key, label) ->
                        val active = mealType == key
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(if (active) CyberAccent.copy(alpha = 0.15f) else CyberBgCard)
                                .clickable { mealType = key }
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Text(label.substringAfter(" "), fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                                color = if (active) CyberAccent else CyberTextSecondary)
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))
                val n = SupplementDb.toNutrition(sel, servings)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(CyberAccent)
                        .clickable {
                            onSave(
                                FoodDiary.entryFrom(n, source = "SUPPLEMENT")
                                    .copy(
                                        dateKey = dateKey,
                                        mealType = mealType,
                                        // A 1250-kcal gainer shake is training fuel,
                                        // not a cheat meal — never auto-flag supplements
                                        isCheatMeal = false,
                                    )
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("✓ Log ${fmtG(n.proteinG)}g protein · ${n.calories} kcal",
                        fontSize = 15.sp, fontWeight = FontWeight.Bold, color = CyberAccentDark)
                }
            }
        }
    }
}

private fun fmtG(f: Float): String =
    if (f == f.toInt().toFloat()) f.toInt().toString() else "%.1f".format(f)

@Composable
private fun CategoryChip(label: String, active: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (active) CyberAccent.copy(alpha = 0.15f) else CyberBgCard)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
            color = if (active) CyberAccent else CyberTextSecondary)
    }
}

@Composable
private fun StepperButton(symbol: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(CyberBgCard)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(symbol, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = CyberTextPrimary)
    }
}

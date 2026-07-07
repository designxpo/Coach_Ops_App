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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
 * Supplement logger — drills down Brand → Category → Product, because each
 * brand carries different protein types and serving sizes. Pick a serving
 * count and it logs into the food diary; protein counts toward the daily
 * macro totals automatically.
 */
private enum class Level { BRAND, CATEGORY, PRODUCT, DETAIL }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupplementPickerSheet(
    dateKey: String,
    onDismiss: () -> Unit,
    onSave: (FoodDiaryEntry) -> Unit,
) {
    var level by remember { mutableStateOf(Level.BRAND) }
    var query by remember { mutableStateOf("") }
    var brand by remember { mutableStateOf("") }
    var category by remember { mutableStateOf<SupplementCategory?>(null) }
    var product by remember { mutableStateOf<SupplementProduct?>(null) }
    var servings by remember { mutableFloatStateOf(1f) }
    var mealType by remember { mutableStateOf(FoodDiary.mealTypeForNow()) }

    fun back() {
        when (level) {
            Level.DETAIL -> { level = Level.PRODUCT; product = null }
            Level.PRODUCT -> { level = Level.CATEGORY; category = null }
            Level.CATEGORY -> { level = Level.BRAND; brand = "" }
            Level.BRAND -> onDismiss()
        }
    }

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
            // Header with breadcrumb + back
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (level != Level.BRAND) {
                    Box(
                        modifier = Modifier.size(32.dp).clip(CircleShape).background(CyberBgCard)
                            .clickable { back() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back",
                            tint = CyberTextPrimary, modifier = Modifier.size(16.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        when (level) {
                            Level.BRAND -> "Log Supplement"
                            Level.CATEGORY -> brand
                            Level.PRODUCT -> brand
                            Level.DETAIL -> brand
                        },
                        fontSize = 19.sp, fontWeight = FontWeight.Bold, color = CyberTextPrimary
                    )
                    Text(
                        when (level) {
                            Level.BRAND -> "${SupplementDb.PRODUCTS.size} products · ${SupplementDb.brands().size} brands"
                            Level.CATEGORY -> "Choose a category"
                            Level.PRODUCT -> category?.let { "${it.emoji} ${it.label}" } ?: ""
                            Level.DETAIL -> "${category?.emoji} ${category?.label}"
                        },
                        fontSize = 12.sp, color = CyberTextMuted
                    )
                }
            }
            Spacer(Modifier.height(14.dp))

            when (level) {
                // ── 1. Brands ────────────────────────────────────────────────
                Level.BRAND -> {
                    GymTextField(query, { query = it }, "Search brand or product",
                        "e.g. muscleblaze, on, beast, yoga bar")
                    Spacer(Modifier.height(12.dp))
                    val brands = remember(query) { SupplementDb.searchBrands(query) }
                    if (brands.isEmpty()) {
                        EmptyRow("No brand matches — try a shorter name")
                    } else {
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 460.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(brands) { b ->
                                DrillRow(
                                    title = b.brand,
                                    subtitle = b.categories.joinToString(" · ") { it.label } +
                                        "  ·  ${b.productCount} product${if (b.productCount != 1) "s" else ""}",
                                ) { brand = b.brand; level = Level.CATEGORY }
                            }
                        }
                    }
                }

                // ── 2. Categories for the chosen brand ───────────────────────
                Level.CATEGORY -> {
                    val cats = remember(brand) { SupplementDb.categoriesForBrand(brand) }
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 460.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(cats) { c ->
                            val n = SupplementDb.productsFor(brand, c).size
                            DrillRow(
                                title = "${c.emoji}  ${c.label}",
                                subtitle = "$n product${if (n != 1) "s" else ""}",
                            ) { category = c; level = Level.PRODUCT }
                        }
                    }
                }

                // ── 3. Products in that brand+category ───────────────────────
                Level.PRODUCT -> {
                    val prods = remember(brand, category) {
                        category?.let { SupplementDb.productsFor(brand, it) } ?: emptyList()
                    }
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 460.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(prods) { p ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(CyberBgCard)
                                    .clickable { product = p; servings = 1f; level = Level.DETAIL }
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(p.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = CyberTextPrimary)
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

                // ── 4. Serving + meal + log ──────────────────────────────────
                Level.DETAIL -> product?.let { sel ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(CyberBgCard)
                            .border(1.dp, CyberAccent.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Text(sel.name, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = CyberTextPrimary)
                        Text(sel.servingDesc + if (sel.flavorNote.isNotEmpty()) " · ${sel.flavorNote}" else "",
                            fontSize = 11.sp, color = CyberTextMuted)
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
                        Text("× serving", fontSize = 12.sp, color = CyberTextMuted)
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
                                        .copy(dateKey = dateKey, mealType = mealType, isCheatMeal = false)
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
}

private fun fmtG(f: Float): String =
    if (f == f.toInt().toFloat()) f.toInt().toString() else "%.1f".format(f)

@Composable
private fun DrillRow(title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CyberBgCard)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = CyberTextPrimary)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, fontSize = 11.sp, color = CyberTextMuted)
        }
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = CyberTextMuted,
            modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun EmptyRow(msg: String) {
    Box(Modifier.fillMaxWidth().padding(vertical = 28.dp), contentAlignment = Alignment.Center) {
        Text(msg, fontSize = 13.sp, color = CyberTextMuted)
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

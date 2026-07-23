@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.IndianFoodCategory
import com.example.data.IndianFoodLibrary
import com.example.data.IndianRegion
import com.example.data.LibraryFood
import com.example.data.UserPreferences
import com.example.data.regionForLocation
import com.example.data.regionOfDish
import com.example.ui.theme.CyberAccent
import com.example.ui.theme.CyberAccentDark
import com.example.ui.theme.CyberBgCard
import com.example.ui.theme.CyberBgCardElevated
import com.example.ui.theme.CyberBgPrimary
import com.example.ui.theme.CyberTextMuted
import com.example.ui.theme.CyberTextPrimary
import com.example.ui.theme.CyberTextSecondary

private enum class VegFilter(val label: String) { ALL("All"), VEG("Veg"), NONVEG("Non-Veg") }

@Composable
fun IndianFoodsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    // Culinary region inferred from the member's saved city — used to surface
    // region-relevant dishes first.
    val userRegion = remember { regionForLocation(UserPreferences.getInstance(context).clientCity) }
    var regionFirst by remember { mutableStateOf(true) }

    var selectedCategory by remember { mutableStateOf<IndianFoodCategory?>(null) }
    var vegFilter        by remember { mutableStateOf(VegFilter.ALL) }
    var searchQuery      by remember { mutableStateOf("") }

    val foods = remember(selectedCategory, vegFilter, searchQuery, userRegion, regionFirst) {
        val list = IndianFoodLibrary.all
            .filter { selectedCategory == null || it.category == selectedCategory }
            .filter {
                when (vegFilter) {
                    VegFilter.ALL    -> true
                    VegFilter.VEG    -> it.isVegetarian
                    VegFilter.NONVEG -> !it.isVegetarian
                }
            }
            .filter { searchQuery.isBlank() || it.name.contains(searchQuery, ignoreCase = true) }
        // Stable sort: dishes from the user's region float to the top, order kept otherwise.
        if (userRegion != null && regionFirst && searchQuery.isBlank())
            list.sortedByDescending { regionOfDish(it.name) == userRegion }
        else list
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBgPrimary)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // ── Top bar ───────────────────────────────────────────────────────────
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(CyberBgCard)
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = CyberTextPrimary, modifier = Modifier.size(18.dp))
            }
            Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Indian Foods", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = CyberTextPrimary)
                Text("${IndianFoodLibrary.all.size} dishes · calories & macros", fontSize = 10.sp, color = CyberTextMuted)
            }
        }

        // ── Region banner (surfaces local favourites first) ──────────────────
        if (userRegion != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CyberAccent.copy(0.10f))
                    .border(1.dp, CyberAccent.copy(0.25f), RoundedCornerShape(12.dp))
                    .clickable { regionFirst = !regionFirst }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("📍", fontSize = 14.sp)
                Text(
                    if (regionFirst) "Showing ${userRegion.label} favourites first"
                    else "Tap to show ${userRegion.label} favourites first",
                    fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                    color = CyberAccent, modifier = Modifier.weight(1f)
                )
                Text(if (regionFirst) "ON" else "OFF", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold,
                    color = if (regionFirst) CyberAccent else CyberTextMuted)
            }
            Spacer(Modifier.height(10.dp))
        }

        // ── Search ────────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(CyberBgCard)
                .border(1.dp, Color.White.copy(0.07f), RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Filled.Search, null, tint = CyberTextMuted, modifier = Modifier.size(16.dp))
            BasicTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.weight(1f),
                textStyle = TextStyle(color = CyberTextPrimary, fontSize = 14.sp),
                cursorBrush = SolidColor(CyberAccent),
                singleLine = true,
                decorationBox = { inner ->
                    if (searchQuery.isEmpty()) Text("Search dishes…", fontSize = 14.sp, color = CyberTextMuted)
                    inner()
                }
            )
            if (searchQuery.isNotEmpty()) {
                Text("✕", fontSize = 14.sp, color = CyberTextMuted, modifier = Modifier.clickable { searchQuery = "" })
            }
        }

        Spacer(Modifier.height(10.dp))

        // ── Veg / Non-veg filter ──────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(CyberBgCard)
                .border(1.dp, Color.White.copy(0.06f), RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            VegFilter.entries.forEach { vf ->
                val sel = vegFilter == vf
                val dot = when (vf) {
                    VegFilter.VEG    -> Color(0xFF10B981)
                    VegFilter.NONVEG -> Color(0xFFEF4444)
                    VegFilter.ALL    -> null
                }
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (sel) CyberAccent else Color.Transparent)
                        .clickable { vegFilter = vf }
                        .padding(vertical = 9.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (dot != null) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(dot)); Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        vf.label, fontSize = 13.sp,
                        fontWeight = if (sel) FontWeight.ExtraBold else FontWeight.SemiBold,
                        color = if (sel) CyberAccentDark else CyberTextMuted
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // ── Category chips ────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CategoryChip("All", selectedCategory == null) { selectedCategory = null }
            IndianFoodCategory.entries.forEach { cat ->
                CategoryChip(cat.label, selectedCategory == cat) { selectedCategory = cat }
            }
        }

        Spacer(Modifier.height(4.dp))
        Text(
            "${foods.size} dishes",
            fontSize = 11.sp, color = CyberTextMuted,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // ── Food list ─────────────────────────────────────────────────────────
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(foods, key = { it.name }) { food ->
                FoodLibraryRow(food, isLocal = userRegion != null && regionOfDish(food.name) == userRegion)
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun CategoryChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) CyberAccent else CyberBgCard)
            .border(1.dp, if (selected) Color.Transparent else Color.White.copy(0.1f), RoundedCornerShape(999.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            label, fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Normal,
            color = if (selected) CyberAccentDark else CyberTextMuted
        )
    }
}

@Composable
private fun FoodLibraryRow(food: LibraryFood, isLocal: Boolean = false) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CyberBgCard)
            .border(1.dp, Color.White.copy(0.06f), RoundedCornerShape(16.dp))
            .clickable { expanded = !expanded }
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            // Category thumbnail + veg dot
            Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(CyberBgCardElevated)) {
                AsyncImage(
                    model = food.category.image,
                    contentDescription = food.category.label,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(3.dp)
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(if (food.isVegetarian) Color(0xFF10B981) else Color(0xFFEF4444))
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(food.name, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = CyberTextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    if (isLocal) {
                        Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(CyberAccent.copy(0.18f)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                            Text("📍 Local", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = CyberAccent, maxLines = 1)
                        }
                    }
                }
                Text(food.quantity, fontSize = 11.sp, color = CyberTextMuted)
            }
            Box(
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Color(0xFF0D3320)).padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text("${food.calories} kcal", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
            }
        }

        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            MacroPill("P ${food.proteinG}g", CyberAccent)
            MacroPill("C ${food.carbsG}g", Color(0xFFF59E0B))
            MacroPill("F ${food.fatG}g", Color(0xFF10B981))
        }

        AnimatedVisibility(visible = expanded, enter = expandVertically(), exit = shrinkVertically()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(CyberBgCardElevated)
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Text(food.benefits, fontSize = 12.sp, color = CyberTextSecondary, lineHeight = 17.sp)
            }
        }
    }
}

@Composable
private fun MacroPill(label: String, color: Color) {
    Box(
        modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(color.copy(0.12f)).padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color, maxLines = 1, softWrap = false)
    }
}

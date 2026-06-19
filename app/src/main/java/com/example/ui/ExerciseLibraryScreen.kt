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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.DifficultyLevel
import com.example.data.Exercise
import com.example.data.ExerciseCategory
import com.example.data.ExerciseRepository
import com.example.ui.theme.CyberAccent
import com.example.ui.theme.CyberAccentDark
import com.example.ui.theme.CyberBgCard
import com.example.ui.theme.CyberBgCardElevated
import com.example.ui.theme.CyberBgPrimary
import com.example.ui.theme.CyberTextMuted
import com.example.ui.theme.CyberTextPrimary

// ─── Category color map ───────────────────────────────────────────────────────

private val catColor = mapOf(
    ExerciseCategory.STRENGTH    to Color(0xFF6366F1),
    ExerciseCategory.YOGA        to Color(0xFF10B981),
    ExerciseCategory.CARDIO      to Color(0xFFF59E0B),
    ExerciseCategory.HIIT        to Color(0xFFEF4444),
    ExerciseCategory.FLEXIBILITY to Color(0xFF8B5CF6)
)

// ─── Difficulty label helper (AAA Hard / AA Middle / A Easy) ─────────────────

private fun DifficultyLevel.browseLabel() = when (this) {
    DifficultyLevel.BEGINNER     -> "A Easy"
    DifficultyLevel.INTERMEDIATE -> "AA Middle"
    DifficultyLevel.ADVANCED     -> "AAA Hard"
}

private enum class ExSortMode(val label: String) { DEFAULT("Default"), AZ("A – Z"), DURATION("Duration") }

@Composable
fun ExerciseCategoryScreen(
    viewModel: FitnessViewModel,
    category: ExerciseCategory,
    onExerciseClick: (String) -> Unit,
    onBack: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf(category) }
    var diffFilter  by remember { mutableStateOf<DifficultyLevel?>(null) }
    var sortMode    by remember { mutableStateOf<ExSortMode>(ExSortMode.DEFAULT) }
    var showSearch  by remember { mutableStateOf(false) }
    var showFilters by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val bookmarked  = remember { mutableSetOf<String>() }

    // Use live Firestore-merged data so admin image/howTo updates appear in real-time
    val allExercises by viewModel.allExercises.collectAsState()
    val exercises = remember(allExercises, selectedCategory, diffFilter, sortMode, searchQuery) {
        var list = allExercises.filter { it.category == selectedCategory }
        if (diffFilter != null) list = list.filter { it.difficulty == diffFilter }
        if (searchQuery.isNotBlank()) list = list.filter { it.name.contains(searchQuery, ignoreCase = true) }
        when (sortMode) {
            ExSortMode.AZ       -> list.sortedBy { it.name }
            ExSortMode.DURATION -> list.sortedBy { it.estimatedMinutes }
            else               -> list
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBgPrimary)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // ── Top bar ───────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
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
            Text(
                "Browse",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = CyberTextPrimary,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // ── Category chip tabs ────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ExerciseCategory.entries.forEach { cat ->
                val sel = selectedCategory == cat
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (sel) CyberAccent else CyberBgCard)
                        .border(
                            1.dp,
                            if (sel) Color.Transparent else Color.White.copy(0.1f),
                            RoundedCornerShape(999.dp)
                        )
                        .clickable { selectedCategory = cat; diffFilter = null }
                        .padding(horizontal = 18.dp, vertical = 10.dp)
                ) {
                    Text(
                        cat.label,
                        fontSize = 13.sp,
                        fontWeight = if (sel) FontWeight.ExtraBold else FontWeight.Normal,
                        color = if (sel) CyberAccentDark else CyberTextMuted
                    )
                }
            }
        }

        Spacer(Modifier.height(2.dp))

        // ── Thin divider ──────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color.White.copy(0.07f))
        )

        // ── Filter / Sort / Search row ────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 0.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ToolbarButton(
                icon = { Icon(Icons.Filled.FilterList, null, tint = CyberTextMuted, modifier = Modifier.size(16.dp)) },
                label = "Filters",
                active = showFilters || diffFilter != null,
                modifier = Modifier.weight(1f)
            ) { showFilters = !showFilters; showSearch = false }

            VerticalBar()

            ToolbarButton(
                icon = { Icon(Icons.AutoMirrored.Filled.Sort, null, tint = CyberTextMuted, modifier = Modifier.size(16.dp)) },
                label = sortMode.label,
                active = sortMode != ExSortMode.DEFAULT,
                modifier = Modifier.weight(1f)
            ) {
                sortMode = when (sortMode) {
                    ExSortMode.DEFAULT -> ExSortMode.AZ
                    ExSortMode.AZ      -> ExSortMode.DURATION
                    else             -> ExSortMode.DEFAULT
                }
            }

            VerticalBar()

            ToolbarButton(
                icon = { Icon(Icons.Filled.Search, null, tint = CyberTextMuted, modifier = Modifier.size(16.dp)) },
                label = "Search",
                active = showSearch,
                modifier = Modifier.weight(1f)
            ) { showSearch = !showSearch; showFilters = false; if (!showSearch) searchQuery = "" }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color.White.copy(0.07f))
        )

        // ── Expandable search bar ─────────────────────────────────────────────
        AnimatedVisibility(visible = showSearch, enter = expandVertically(), exit = shrinkVertically()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CyberBgCard)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
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
                            if (searchQuery.isEmpty()) Text("Search ${selectedCategory.label.lowercase()} exercises…", fontSize = 14.sp, color = CyberTextMuted)
                            inner()
                        }
                    )
                    if (searchQuery.isNotEmpty()) {
                        Text("✕", fontSize = 14.sp, color = CyberTextMuted, modifier = Modifier.clickable { searchQuery = "" })
                    }
                }
            }
        }

        // ── Expandable difficulty filter chips ────────────────────────────────
        AnimatedVisibility(visible = showFilters, enter = expandVertically(), exit = shrinkVertically()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CyberBgCard)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip("All", diffFilter == null) { diffFilter = null }
                DifficultyLevel.entries.forEach { diff ->
                    FilterChip(diff.label, diffFilter == diff) { diffFilter = diff }
                }
            }
        }

        // ── Results count ─────────────────────────────────────────────────────
        Text(
            "${exercises.size} exercises",
            fontSize = 11.sp,
            color = CyberTextMuted,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // ── 2-column photo grid ───────────────────────────────────────────────
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(exercises, key = { it.id }) { ex ->
                WorkoutPhotoCard(
                    exercise = ex,
                    isBookmarked = ex.id in bookmarked,
                    onBookmark = {
                        if (ex.id in bookmarked) bookmarked.remove(ex.id)
                        else bookmarked.add(ex.id)
                    },
                    onClick = { onExerciseClick(ex.id) }
                )
            }
            item { Spacer(Modifier.height(16.dp)) }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

// ─── Photo card — matches reference exactly ───────────────────────────────────

@Composable
private fun WorkoutPhotoCard(
    exercise: Exercise,
    isBookmarked: Boolean,
    onBookmark: () -> Unit,
    onClick: () -> Unit
) {
    val accentColor = catColor[exercise.category] ?: CyberAccent

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.72f)          // tall card — matches reference proportions
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() }
    ) {
        // ── Photo / gradient background ───────────────────────────────────────
        if (exercise.imageUrl.isNotBlank()) {
            AsyncImage(
                model = exercise.imageUrl,
                contentDescription = exercise.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Fallback: solid category-colour gradient
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(accentColor.copy(0.4f), accentColor.copy(0.15f))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(exercise.muscleEmoji, fontSize = 52.sp)
            }
        }

        // ── Dark gradient overlay (bottom 55% of card) ────────────────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f   to Color.Transparent,
                        0.35f to Color.Transparent,
                        0.7f  to Color.Black.copy(0.65f),
                        1f    to Color.Black.copy(0.92f)
                    )
                )
        )

        // ── Bookmark icon — top right ─────────────────────────────────────────
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp)
                .size(32.dp)
                .clip(CircleShape)
                .background(Color(0xFF1A1A1A).copy(0.7f))
                .clickable { onBookmark() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                contentDescription = null,
                tint = if (isBookmarked) CyberAccent else Color.White,
                modifier = Modifier.size(16.dp)
            )
        }

        // ── Text overlay — bottom ─────────────────────────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            // Exercise name (2 lines max)
            Text(
                exercise.name,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                lineHeight = 16.sp,
                maxLines = 2
            )

            // Duration + Difficulty row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(Icons.Filled.PlayArrow, null, tint = Color.White.copy(0.8f), modifier = Modifier.size(12.dp))
                    Text(
                        "${exercise.estimatedMinutes} min",
                        fontSize = 11.sp,
                        color = Color.White.copy(0.85f),
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(
                    exercise.difficulty.browseLabel(),
                    fontSize = 10.sp,
                    color = Color.White.copy(0.85f),
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

// ─── Small helpers ────────────────────────────────────────────────────────────

@Composable
private fun ToolbarButton(
    icon: @Composable () -> Unit,
    label: String,
    active: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon()
        Spacer(Modifier.width(5.dp))
        Text(
            label,
            fontSize = 13.sp,
            fontWeight = if (active) FontWeight.ExtraBold else FontWeight.Normal,
            color = if (active) CyberAccent else CyberTextMuted
        )
    }
}

@Composable
private fun VerticalBar() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(20.dp)
            .background(Color.White.copy(0.1f))
    )
}

@Composable
private fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) CyberAccent else CyberBgCardElevated)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Text(
            label,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Normal,
            color = if (selected) CyberAccentDark else CyberTextMuted
        )
    }
}

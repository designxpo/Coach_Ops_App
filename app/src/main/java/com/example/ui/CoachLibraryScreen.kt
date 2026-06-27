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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
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
import com.example.ui.theme.CyberTextSecondary

private data class CategoryInfo(
    val category: ExerciseCategory,
    val count: Int,
    val coverUrl: String,
    val accent: Color
)

@Composable
fun CoachLibraryScreen(
    viewModel: FitnessViewModel,
    onCategoryClick: (ExerciseCategory) -> Unit,
    onExerciseClick: (String) -> Unit,
    onNutritionClick: () -> Unit = {},
    onHealthMetricsClick: () -> Unit = {}
) {
    var search by remember { mutableStateOf("") }

    // Live Firestore-merged list so admin image/howTo updates appear without rebuild
    val allExercises by viewModel.allExercises.collectAsState()

    val categoryInfos = remember(allExercises) {
        listOf(
            CategoryInfo(ExerciseCategory.STRENGTH,    allExercises.count { it.category == ExerciseCategory.STRENGTH },    "https://images.unsplash.com/photo-1581009146145-b5ef050c2e1e?w=400",    Color(0xFF6366F1)),
            CategoryInfo(ExerciseCategory.YOGA,        allExercises.count { it.category == ExerciseCategory.YOGA },        "https://images.unsplash.com/photo-1506126613408-eca07ce68773?w=400",    Color(0xFF10B981)),
            CategoryInfo(ExerciseCategory.CARDIO,      allExercises.count { it.category == ExerciseCategory.CARDIO },      "https://images.unsplash.com/photo-1538805060514-97d9cc17730c?w=400",    Color(0xFFF59E0B)),
            CategoryInfo(ExerciseCategory.HIIT,        allExercises.count { it.category == ExerciseCategory.HIIT },        "https://images.unsplash.com/photo-1517963879433-6ad2b056d712?w=400",    Color(0xFFEF4444)),
            CategoryInfo(ExerciseCategory.FLEXIBILITY, allExercises.count { it.category == ExerciseCategory.FLEXIBILITY }, "https://images.unsplash.com/photo-1599901860904-17e6ed7083a0?w=400",    Color(0xFF8B5CF6)),
        )
    }

    val filtered = remember(allExercises, search) {
        if (search.isBlank()) allExercises
        else allExercises.filter { it.name.contains(search, ignoreCase = true) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(CyberBgPrimary),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(CyberAccent.copy(0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.FitnessCenter, null, tint = CyberAccent, modifier = Modifier.size(20.dp))
                    }
                    Column {
                        Text("Exercise Library", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = CyberTextPrimary)
                        Text("${allExercises.size} exercises · reference & plan", fontSize = 12.sp, color = CyberTextMuted)
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Search bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(CyberBgCard)
                        .border(1.dp, Color.White.copy(0.08f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Filled.Search, null, tint = CyberTextMuted, modifier = Modifier.size(18.dp))
                    BasicTextField(
                        value = search,
                        onValueChange = { search = it },
                        modifier = Modifier.weight(1f),
                        textStyle = TextStyle(color = CyberTextPrimary, fontSize = 14.sp),
                        cursorBrush = SolidColor(CyberAccent),
                        singleLine = true,
                        decorationBox = { inner ->
                            if (search.isEmpty()) Text("Search exercises…", fontSize = 14.sp, color = CyberTextMuted)
                            inner()
                        }
                    )
                }
            }
        }

        // ── Nutrition Plans card ──────────────────────────────────────────────
        if (search.isBlank()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            androidx.compose.ui.graphics.Brush.horizontalGradient(
                                listOf(Color(0xFF14532D), Color(0xFF166534))
                            )
                        )
                        .border(1.dp, Color(0xFF22C55E).copy(0.3f), RoundedCornerShape(20.dp))
                        .clickable { onNutritionClick() }
                        .padding(horizontal = 20.dp, vertical = 18.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF22C55E).copy(0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🥗", fontSize = 26.sp)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Nutrition Plans",
                                fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = Color.White
                            )
                            Text(
                                "5 Indian diet plans · Goal-based",
                                fontSize = 12.sp, color = Color.White.copy(0.65f)
                            )
                            Spacer(Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("Muscle", "Fat Loss", "Cardio", "Flex", "General").forEach { tag ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFF22C55E).copy(0.25f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(tag, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF86EFAC))
                                    }
                                }
                            }
                        }
                        Text("›", fontSize = 24.sp, color = Color(0xFF22C55E), fontWeight = FontWeight.ExtraBold)
                    }
                }
                Spacer(Modifier.height(12.dp))

                // BMI & Metrics card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            androidx.compose.ui.graphics.Brush.horizontalGradient(
                                listOf(Color(0xFF1E1B4B), Color(0xFF2D1B69))
                            )
                        )
                        .border(1.dp, Color(0xFF818CF8).copy(0.3f), RoundedCornerShape(20.dp))
                        .clickable { onHealthMetricsClick() }
                        .padding(horizontal = 20.dp, vertical = 18.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF818CF8).copy(0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("⚖️", fontSize = 26.sp)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("BMI & Health Metrics", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                            Text("Protein · Calories · Macros · Water", fontSize = 12.sp, color = Color.White.copy(0.65f))
                            Spacer(Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("Age 15–65+", "Indian sources", "Goal-based").forEach { tag ->
                                    Box(
                                        modifier = Modifier.clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFF818CF8).copy(0.25f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(tag, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC7D2FE))
                                    }
                                }
                            }
                        }
                        Text("›", fontSize = 24.sp, color = Color(0xFF818CF8), fontWeight = FontWeight.ExtraBold)
                    }
                }
                Spacer(Modifier.height(20.dp))
            }
        }

        // ── Category cards (only when not searching) ──────────────────────────
        if (search.isBlank()) {
            item {
                Text(
                    "Exercise Library",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = CyberTextPrimary,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )
                Spacer(Modifier.height(10.dp))
            }

            // 2-column grid via chunked list items
            val rows = categoryInfos.chunked(2)
            items(rows) { row ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    row.forEach { info ->
                        CoachCategoryCard(
                            info = info,
                            modifier = Modifier.weight(1f),
                            onClick = { onCategoryClick(info.category) }
                        )
                    }
                    // fill empty slot if odd number
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
                Spacer(Modifier.height(12.dp))
            }

            item {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("All Exercises", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = CyberTextPrimary)
                    Text("${allExercises.size} total", fontSize = 12.sp, color = CyberTextMuted)
                }
                Spacer(Modifier.height(10.dp))
            }
        }

        // ── Exercise list (only shown during search) ──────────────────────────
        if (search.isNotBlank()) {
            items(filtered, key = { it.id }) { ex ->
                CoachExerciseRow(exercise = ex, onClick = { onExerciseClick(ex.id) })
            }

            if (filtered.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                        Text(
                            if (search.isBlank()) "No exercises yet" else "No exercises match \"$search\"",
                            fontSize = 14.sp, color = CyberTextMuted
                        )
                    }
                }
            }
        }
    }
}

// ── Category card ─────────────────────────────────────────────────────────────

@Composable
private fun CoachCategoryCard(
    info: CategoryInfo,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .aspectRatio(1.1f)
            .clip(RoundedCornerShape(18.dp))
            .background(CyberBgCard)
            .border(1.dp, info.accent.copy(0.25f), RoundedCornerShape(18.dp))
            .clickable { onClick() }
    ) {
        AsyncImage(
            model = info.coverUrl,
            contentDescription = info.category.label,
            contentScale = ContentScale.Crop,
            placeholder = painterResource(android.R.drawable.ic_menu_gallery),
            modifier = Modifier.fillMaxSize()
        )
        // dark overlay
        Box(
            Modifier.fillMaxSize().background(
                androidx.compose.ui.graphics.Brush.verticalGradient(
                    listOf(Color.Black.copy(0.15f), Color.Black.copy(0.72f))
                )
            )
        )
        // text at bottom
        Column(
            modifier = Modifier.align(Alignment.BottomStart).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(info.category.emoji, fontSize = 22.sp)
            Text(info.category.label, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(info.accent.copy(0.85f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text("${info.count} exercises", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

// ── Exercise row in list ──────────────────────────────────────────────────────

@Composable
private fun CoachExerciseRow(exercise: Exercise, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(CyberBgCard)
            .border(1.dp, Color.White.copy(0.05f), RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Thumbnail
        Box(
            modifier = Modifier.size(60.dp).clip(RoundedCornerShape(12.dp))
        ) {
            if (exercise.imageUrl.isNotBlank()) {
                AsyncImage(
                    model = exercise.imageUrl,
                    contentDescription = exercise.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    Modifier.fillMaxSize().background(CyberAccent.copy(0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(exercise.muscleEmoji, fontSize = 24.sp)
                }
            }
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(exercise.name, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = CyberTextPrimary)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                CategoryPill(exercise.category)
                Text("·", fontSize = 11.sp, color = CyberTextMuted)
                Text(exercise.difficulty.label, fontSize = 11.sp, color = CyberTextMuted)
            }
            Text(
                "${exercise.sets} · ${exercise.reps}",
                fontSize = 11.sp,
                color = CyberTextSecondary
            )
        }

        Text("›", fontSize = 20.sp, color = CyberAccent, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun CategoryPill(category: ExerciseCategory) {
    val (bg, fg) = when (category) {
        ExerciseCategory.STRENGTH    -> Color(0xFF6366F1).copy(0.18f) to Color(0xFF818CF8)
        ExerciseCategory.YOGA        -> Color(0xFF10B981).copy(0.18f) to Color(0xFF34D399)
        ExerciseCategory.CARDIO      -> Color(0xFFF59E0B).copy(0.18f) to Color(0xFFFBBF24)
        ExerciseCategory.HIIT        -> Color(0xFFEF4444).copy(0.18f) to Color(0xFFF87171)
        ExerciseCategory.FLEXIBILITY -> Color(0xFF8B5CF6).copy(0.18f) to Color(0xFFA78BFA)
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(category.label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = fg)
    }
}

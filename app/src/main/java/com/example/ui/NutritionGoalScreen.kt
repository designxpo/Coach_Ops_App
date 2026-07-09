@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ClientGoal
import com.example.data.NutritionRepository
import com.example.ui.theme.CyberAccent
import com.example.ui.theme.CyberBgCard
import com.example.ui.theme.CyberBgCardElevated
import com.example.ui.theme.CyberBgPrimary
import com.example.ui.theme.CyberTextMuted
import com.example.ui.theme.CyberTextPrimary
import com.example.ui.theme.CyberTextSecondary

private val goalColors = mapOf(
    ClientGoal.BUILD_MUSCLE         to Color(0xFF6366F1),
    ClientGoal.LOSE_FAT             to Color(0xFFEF4444),
    ClientGoal.IMPROVE_CARDIO       to Color(0xFFF59E0B),
    ClientGoal.IMPROVE_FLEXIBILITY  to Color(0xFF8B5CF6),
    ClientGoal.GENERAL_FITNESS      to Color(0xFF10B981)
)

@Composable
fun NutritionGoalScreen(
    viewModel: FitnessViewModel,
    onGoalClick: (ClientGoal) -> Unit,
    onBack: () -> Unit
) {
    val currentGoal = viewModel.clientGoal

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBgPrimary)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // ── Header ────────────────────────────────────────────────────────────
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
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = CyberTextPrimary, modifier = Modifier.size(18.dp))
            }
            Column {
                Text("Nutrition Plans", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = CyberTextPrimary)
                Text("Indian diet tailored to your goal", fontSize = 12.sp, color = CyberTextMuted)
            }
        }

        // ── Info strip ────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(CyberAccent.copy(0.08f))
                .border(1.dp, CyberAccent.copy(0.2f), RoundedCornerShape(14.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("🎯", fontSize = 18.sp)
                Text(
                    "Your current goal: ${currentGoal.emoji} ${currentGoal.label}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CyberAccent
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── Goal cards list ───────────────────────────────────────────────────
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(ClientGoal.entries) { goal ->
                val plan  = NutritionRepository.forGoal(goal)
                val color = goalColors[goal] ?: CyberAccent
                val isActive = goal == currentGoal

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isActive) color.copy(0.12f) else CyberBgCard)
                        .border(
                            width = if (isActive) 1.5.dp else 1.dp,
                            color = if (isActive) color.copy(0.5f) else Color.White.copy(0.06f),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .clickable { onGoalClick(goal) }
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Emoji box
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(color.copy(0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(goal.emoji, fontSize = 26.sp)
                        }

                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(goal.label, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = CyberTextPrimary)
                                if (isActive) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(999.dp))
                                            .background(color.copy(0.20f))
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text("Active", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = color)
                                    }
                                }
                            }

                            if (plan != null) {
                                Text(
                                    "${plan.dailyCalories} kcal/day",
                                    fontSize = 12.sp,
                                    color = CyberTextMuted,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(Modifier.height(4.dp))
                                // Macro pills row
                                androidx.compose.foundation.layout.FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    MacroPill("P ${plan.proteinG}g", Color(0xFF6366F1))
                                    MacroPill("C ${plan.carbsG}g", Color(0xFFF59E0B))
                                    MacroPill("F ${plan.fatG}g", Color(0xFFEF4444))
                                    Text("· ${plan.meals.size} meals/day", fontSize = 11.sp, color = CyberTextMuted,
                                        maxLines = 1, softWrap = false)
                                }
                            }
                        }

                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward, null,
                            tint = if (isActive) color else CyberTextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun MacroPill(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(color.copy(0.12f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(text, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = color, maxLines = 1, softWrap = false)
    }
}

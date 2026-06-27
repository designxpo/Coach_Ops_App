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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.rememberCoroutineScope
import com.example.data.ProfileSync
import com.example.data.UserPreferences
import kotlinx.coroutines.launch
import com.example.ui.theme.CyberAccent
import com.example.ui.theme.CyberAccentDark
import com.example.ui.theme.CyberBgCard
import com.example.ui.theme.CyberBgCardElevated
import com.example.ui.theme.CyberBgPrimary
import com.example.ui.theme.CyberDanger
import com.example.ui.theme.CyberTextMuted
import com.example.ui.theme.CyberTextPrimary

private data class GoalOption(val emoji: String, val label: String)

@Composable
fun ClientOnboardingScreen(
    userPreferences: UserPreferences,
    onComplete: () -> Unit
) {
    var step by remember { mutableStateOf(1) }
    val scope = rememberCoroutineScope()
    var clientName by remember { mutableStateOf(userPreferences.clientName.ifEmpty { "" }) }
    var clientCity by remember { mutableStateOf(userPreferences.clientCity) }
    var selectedGoal by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf("") }

    val goals = listOf(
        GoalOption("🏋️", "Build Muscle"),
        GoalOption("🔥", "Lose Fat"),
        GoalOption("⚡", "Get Stronger"),
        GoalOption("🧘", "Improve Flexibility"),
        GoalOption("🏃", "Improve Cardio"),
        GoalOption("🌟", "General Fitness")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBgPrimary)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Back button row (step 2 only)
        if (step > 1) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 8.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CyberBgCard)
                        .clickable { step = 1 },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Go back",
                        tint = CyberTextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Progress bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = if (step > 1) 12.dp else 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(2) { index ->
                val isDone = index + 1 < step
                val isActive = index + 1 == step
                Box(
                    modifier = Modifier.weight(1f).height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            when {
                                isDone   -> CyberAccent
                                isActive -> CyberAccent.copy(alpha = 0.5f)
                                else     -> CyberBgCardElevated
                            }
                        )
                )
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            when (step) {
                1 -> ClientNameStep(
                    name = clientName,
                    city = clientCity,
                    onNameChange = { clientName = it },
                    onCityChange = { clientCity = it }
                )
                2 -> ClientGoalStep(
                    goals = goals,
                    selected = selectedGoal,
                    onSelect = { selectedGoal = it }
                )
            }
        }

        val canProceed = when (step) {
            1 -> clientName.isNotBlank()
            else -> selectedGoal.isNotEmpty()
        }

        if (saveError.isNotEmpty()) {
            Text(
                saveError,
                fontSize = 13.sp,
                color = CyberDanger,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .height(58.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(if (isLoading) CyberAccent.copy(alpha = 0.6f) else if (canProceed) CyberAccent else CyberBgCard)
                .clickable(enabled = canProceed && !isLoading) {
                    if (step < 2) {
                        step++
                    } else {
                        val trimmedName = clientName.trim()
                        userPreferences.clientName         = trimmedName
                        userPreferences.clientCity         = clientCity.trim()
                        userPreferences.clientGoal         = selectedGoal
                        userPreferences.onboardingComplete = true
                        isLoading = true
                        saveError = ""
                        // Persist full member profile to Firestore — enables any-device restore
                        scope.launch {
                            try {
                                ProfileSync.saveMemberProfile(userPreferences)
                                isLoading = false
                                onComplete()
                            } catch (e: Exception) {
                                isLoading = false
                                saveError = e.message ?: "Failed to save profile. Please try again."
                            }
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = CyberAccentDark,
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.5.dp
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        if (step < 2) "Continue" else "Find Trainers",
                        fontSize = 16.sp, fontWeight = FontWeight.ExtraBold,
                        color = if (canProceed) CyberAccentDark else CyberTextMuted
                    )
                    if (canProceed) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = CyberAccentDark, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ClientNameStep(
    name: String, city: String,
    onNameChange: (String) -> Unit, onCityChange: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(Modifier.height(8.dp))
        Text("Step 1 of 2", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = CyberAccent, letterSpacing = 0.8.sp)
        Spacer(Modifier.height(10.dp))
        Text("Tell us about yourself", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = CyberTextPrimary, lineHeight = 28.sp)
        Spacer(Modifier.height(8.dp))
        Text("This helps trainers know who they're working with.", fontSize = 14.sp, color = CyberTextMuted)
        Spacer(Modifier.height(32.dp))

        InputField(label = "Full Name *", value = name, placeholder = "e.g. Priyesh Mishra",
            keyboard = KeyboardType.Text, onValueChange = onNameChange)
        Spacer(Modifier.height(16.dp))
        InputField(label = "Your City (optional)", value = city, placeholder = "e.g. Mumbai",
            keyboard = KeyboardType.Text, onValueChange = onCityChange)
    }
}

@Composable
private fun ClientGoalStep(
    goals: List<GoalOption>,
    selected: String,
    onSelect: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(Modifier.height(8.dp))
        Text("Step 2 of 2", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = CyberAccent, letterSpacing = 0.8.sp)
        Spacer(Modifier.height(10.dp))
        Text("What's your fitness goal?", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = CyberTextPrimary, lineHeight = 28.sp)
        Spacer(Modifier.height(8.dp))
        Text("We'll match you with the right trainers.", fontSize = 14.sp, color = CyberTextMuted)
        Spacer(Modifier.height(28.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(0.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(goals) { goal ->
                val isSelected = goal.label == selected
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSelected) CyberAccent.copy(alpha = 0.10f) else CyberBgCard)
                        .border(2.dp, if (isSelected) CyberAccent else Color.White.copy(alpha = 0.06f), RoundedCornerShape(20.dp))
                        .clickable { onSelect(goal.label) }
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(goal.emoji, fontSize = 28.sp)
                        Text(
                            goal.label, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                            color = if (isSelected) CyberAccent else CyberTextPrimary, lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InputField(
    label: String, value: String, placeholder: String,
    keyboard: KeyboardType, onValueChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, fontSize = 12.sp, color = CyberTextMuted, fontWeight = FontWeight.SemiBold)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CyberBgCard)
                .border(1.5.dp,
                    if (value.isNotBlank()) CyberAccent.copy(0.5f) else Color.White.copy(0.08f),
                    RoundedCornerShape(16.dp))
                .padding(horizontal = 18.dp, vertical = 16.dp)
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(color = CyberTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
                cursorBrush = SolidColor(CyberAccent),
                keyboardOptions = KeyboardOptions(keyboardType = keyboard),
                singleLine = true,
                decorationBox = { inner ->
                    Box {
                        if (value.isEmpty()) Text(placeholder, fontSize = 16.sp, color = CyberTextMuted)
                        inner()
                    }
                }
            )
        }
    }
}

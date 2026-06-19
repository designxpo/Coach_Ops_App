package com.example.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyberAccent
import com.example.ui.theme.CyberAccentDark
import com.example.ui.theme.CyberBgCard
import com.example.ui.theme.CyberBgCardElevated
import com.example.ui.theme.CyberBgPrimary
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import com.example.ui.theme.CyberSuccess
import com.example.ui.theme.CyberTextMuted
import com.example.ui.theme.CyberTextPrimary
import com.example.ui.theme.CyberTextSecondary

private data class OnboardingOption(val emoji: String, val label: String, val sublabel: String = "")

@Composable
fun OnboardingScreen(
    viewModel: MainViewModel,
    initialName: String = "",
    initialPhone: String = "",
    onComplete: () -> Unit
) {
    var currentStep by remember { mutableStateOf(1) }
    var coachName by remember { mutableStateOf(initialName) }
    var coachPhone by remember { mutableStateOf(initialPhone) }
    var selectedSpecialties by remember { mutableStateOf(setOf<String>()) }
    var selectedClientRange by remember { mutableStateOf("") }
    var selectedChallenge by remember { mutableStateOf("") }

    val specialties = listOf(
        OnboardingOption("🏋️", "Strength Training",    "Powerlifting & barbell"),
        OnboardingOption("🔥", "Fat Loss",              "Body recomposition"),
        OnboardingOption("💪", "Hypertrophy",           "Muscle building"),
        OnboardingOption("⚡", "Sports Performance",    "Athletic conditioning"),
        OnboardingOption("🧘", "Yoga & Mobility",       "Flexibility & recovery"),
        OnboardingOption("🌟", "General Wellness",      "Lifestyle coaching"),
        OnboardingOption("🥊", "Boxing / MMA",          "Combat sports fitness"),
        OnboardingOption("🤸", "Calisthenics",          "Bodyweight mastery"),
        OnboardingOption("❤️", "Cardio & Endurance",   "Running & stamina"),
        OnboardingOption("🎽", "CrossFit / HIIT",       "High-intensity training"),
        OnboardingOption("💃", "Dance Fitness / Zumba", "Fun cardio"),
        OnboardingOption("🥗", "Nutrition Coaching",    "Diet & meal planning"),
        OnboardingOption("🦽", "Rehabilitation",        "Injury recovery"),
        OnboardingOption("👴", "Senior Fitness",        "60+ age group"),
        OnboardingOption("🤰", "Pre/Post Natal",        "Pregnancy fitness"),
        OnboardingOption("🧘", "Pilates",               "Core & posture"),
        OnboardingOption("🏃", "Personal Training",     "One-on-one coaching"),
        OnboardingOption("🔬", "Functional Fitness",    "Movement & mobility")
    )

    val clientRanges = listOf(
        OnboardingOption("👤", "1–5 clients", "Just getting started"),
        OnboardingOption("👥", "6–15 clients", "Growing roster"),
        OnboardingOption("🏢", "16–30 clients", "Established coach"),
        OnboardingOption("🚀", "30+ clients", "Full-time business")
    )

    val challenges = listOf(
        OnboardingOption("💳", "Payment collection", "Chasing invoices manually"),
        OnboardingOption("📊", "Tracking progress", "Client data scattered"),
        OnboardingOption("📋", "Managing programs", "Too many spreadsheets"),
        OnboardingOption("💬", "Client communication", "Missing check-ins")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBgPrimary)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Progress bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(5) { index ->
                val stepNum = index + 1
                val isActive = stepNum == currentStep
                val isDone = stepNum < currentStep
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            when {
                                isDone -> CyberAccent
                                isActive -> CyberAccent.copy(alpha = 0.5f)
                                else -> CyberBgCardElevated
                            }
                        )
                )
            }
        }

        AnimatedContent(
            targetState = currentStep,
            transitionSpec = {
                (slideInHorizontally { it } + fadeIn()) togetherWith (slideOutHorizontally { -it } + fadeOut())
            },
            modifier = Modifier.weight(1f),
            label = "onboarding_step"
        ) { step ->
            when (step) {
                1 -> OnboardingNameStep(
                    stepLabel = "Step 1 of 5",
                    name = coachName,
                    phone = coachPhone,
                    onNameChange = { coachName = it },
                    onPhoneChange = { coachPhone = it }
                )
                2 -> OnboardingMultiStep(
                    stepLabel = "Step 2 of 5",
                    title = "What are your coaching specialties?",
                    subtitle = "Select all that apply — you can always update this later.",
                    options = specialties,
                    selectedLabels = selectedSpecialties,
                    onToggle = { spec ->
                        selectedSpecialties = if (spec in selectedSpecialties)
                            selectedSpecialties - spec
                        else
                            selectedSpecialties + spec
                    }
                )
                3 -> OnboardingStep(
                    stepLabel = "Step 3 of 5",
                    title = "How many members are you coaching?",
                    subtitle = "This helps us set up your roster view.",
                    options = clientRanges,
                    selectedLabel = selectedClientRange,
                    onSelect = { selectedClientRange = it }
                )
                4 -> OnboardingStep(
                    stepLabel = "Step 4 of 5",
                    title = "What's your biggest challenge right now?",
                    subtitle = "ProCoach India is built to solve exactly this.",
                    options = challenges,
                    selectedLabel = selectedChallenge,
                    onSelect = { selectedChallenge = it }
                )
                5 -> OnboardingSummary(
                    name = coachName,
                    specialty = selectedSpecialties.joinToString(", "),
                    clientRange = selectedClientRange,
                    challenge = selectedChallenge
                )
            }
        }

        // CTA button
        val canProceed = when (currentStep) {
            1 -> coachName.isNotBlank()
            2 -> selectedSpecialties.isNotEmpty()
            3 -> selectedClientRange.isNotEmpty()
            4 -> selectedChallenge.isNotEmpty()
            else -> true
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .height(58.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(if (canProceed) CyberAccent else CyberBgCard)
                .clickable(enabled = canProceed) {
                    if (currentStep < 5) {
                        currentStep++
                    } else {
                        viewModel.saveOnboardingData(coachName.trim(), coachPhone.trim(), selectedSpecialties.toList(), selectedClientRange, selectedChallenge)
                        onComplete()
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    if (currentStep < 5) "Continue" else "Start Coaching",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (canProceed) CyberAccentDark else CyberTextMuted
                )
                if (canProceed) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = CyberAccentDark,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingNameStep(
    stepLabel: String,
    name: String,
    phone: String,
    onNameChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(Modifier.height(8.dp))
        Text(stepLabel, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = CyberAccent, letterSpacing = 0.8.sp)
        Spacer(Modifier.height(10.dp))
        Text("What should we call you?", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold,
            color = CyberTextPrimary, lineHeight = 28.sp)
        Spacer(Modifier.height(8.dp))
        Text("Your name and phone are shown to members in shared content.",
            fontSize = 14.sp, color = CyberTextMuted)
        Spacer(Modifier.height(32.dp))

        // Name field
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Full Name *", fontSize = 12.sp, color = CyberTextMuted, fontWeight = FontWeight.SemiBold)
            Box(
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CyberBgCard)
                    .border(1.5.dp,
                        if (name.isNotBlank()) CyberAccent.copy(0.5f) else Color.White.copy(0.08f),
                        RoundedCornerShape(16.dp))
                    .padding(horizontal = 18.dp, vertical = 16.dp)
            ) {
                BasicTextField(
                    value = name,
                    onValueChange = onNameChange,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(color = CyberTextPrimary, fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold),
                    cursorBrush = SolidColor(CyberAccent),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    singleLine = true,
                    decorationBox = { inner ->
                        if (name.isEmpty()) Text("e.g. Priyesh Mishra", fontSize = 16.sp, color = CyberTextMuted)
                        inner()
                    }
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Phone field
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Phone Number (optional)", fontSize = 12.sp, color = CyberTextMuted, fontWeight = FontWeight.SemiBold)
            Box(
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CyberBgCard)
                    .border(1.5.dp,
                        if (phone.isNotBlank()) CyberAccent.copy(0.5f) else Color.White.copy(0.08f),
                        RoundedCornerShape(16.dp))
                    .padding(horizontal = 18.dp, vertical = 16.dp)
            ) {
                BasicTextField(
                    value = phone,
                    onValueChange = onPhoneChange,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(color = CyberTextPrimary, fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold),
                    cursorBrush = SolidColor(CyberAccent),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    decorationBox = { inner ->
                        if (phone.isEmpty()) Text("e.g. 9876543210", fontSize = 16.sp, color = CyberTextMuted)
                        inner()
                    }
                )
            }
        }
    }
}

@Composable
private fun OnboardingStep(
    stepLabel: String,
    title: String,
    subtitle: String,
    options: List<OnboardingOption>,
    selectedLabel: String,
    onSelect: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(Modifier.height(8.dp))
        Text(stepLabel, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = CyberAccent, letterSpacing = 0.8.sp)
        Spacer(Modifier.height(10.dp))
        Text(title, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = CyberTextPrimary, lineHeight = 28.sp)
        Spacer(Modifier.height(8.dp))
        Text(subtitle, fontSize = 14.sp, color = CyberTextMuted)
        Spacer(Modifier.height(28.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(0.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(options) { option ->
                val isSelected = option.label == selectedLabel
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSelected) CyberAccent.copy(alpha = 0.10f) else CyberBgCard)
                        .border(
                            2.dp,
                            if (isSelected) CyberAccent else Color.White.copy(alpha = 0.06f),
                            RoundedCornerShape(20.dp)
                        )
                        .clickable { onSelect(option.label) }
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(option.emoji, fontSize = 28.sp)
                        Text(
                            option.label,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) CyberAccent else CyberTextPrimary,
                            lineHeight = 18.sp
                        )
                        if (option.sublabel.isNotEmpty()) {
                            Text(
                                option.sublabel,
                                fontSize = 11.sp,
                                color = if (isSelected) CyberAccent.copy(alpha = 0.7f) else CyberTextMuted
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Multi-select version for specialties ──────────────────────────────────────

@Composable
private fun OnboardingMultiStep(
    stepLabel: String,
    title: String,
    subtitle: String,
    options: List<OnboardingOption>,
    selectedLabels: Set<String>,
    onToggle: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(Modifier.height(8.dp))
        Text(stepLabel, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = CyberAccent, letterSpacing = 0.8.sp)
        Spacer(Modifier.height(10.dp))
        Text(title, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = CyberTextPrimary, lineHeight = 28.sp)
        Spacer(Modifier.height(8.dp))
        Text(subtitle, fontSize = 14.sp, color = CyberTextMuted)
        if (selectedLabels.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Text("✓ ${selectedLabels.size} selected", fontSize = 12.sp, color = CyberAccent, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(20.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(0.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(options) { option ->
                val isSelected = option.label in selectedLabels
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSelected) CyberAccent.copy(alpha = 0.12f) else CyberBgCard)
                        .border(
                            if (isSelected) 2.dp else 1.dp,
                            if (isSelected) CyberAccent else Color.White.copy(alpha = 0.06f),
                            RoundedCornerShape(20.dp)
                        )
                        .clickable { onToggle(option.label) }
                        .padding(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(option.emoji, fontSize = 24.sp)
                            if (isSelected) {
                                Box(
                                    Modifier.size(18.dp).clip(androidx.compose.foundation.shape.CircleShape)
                                        .background(CyberAccent),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("✓", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = CyberAccentDark)
                                }
                            }
                        }
                        Text(
                            option.label,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) CyberAccent else CyberTextPrimary,
                            lineHeight = 17.sp
                        )
                        if (option.sublabel.isNotEmpty()) {
                            Text(
                                option.sublabel,
                                fontSize = 10.sp,
                                color = if (isSelected) CyberAccent.copy(alpha = 0.7f) else CyberTextMuted
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingSummary(name: String, specialty: String, clientRange: String, challenge: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(CyberAccent),
            contentAlignment = Alignment.Center
        ) {
            Text("✓", fontSize = 36.sp)
        }

        Spacer(Modifier.height(24.dp))
        Text("You're all set!", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = CyberTextPrimary)
        Spacer(Modifier.height(8.dp))
        Text(
            "ProCoach India is configured for your coaching style.",
            fontSize = 14.sp,
            color = CyberTextMuted,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(36.dp))

        listOf(
            "Name" to name,
            "Specialty" to specialty,
            "Client range" to clientRange,
            "Focus area" to challenge
        ).forEach { (label, value) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CyberBgCard)
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(label, fontSize = 13.sp, color = CyberTextMuted, fontWeight = FontWeight.SemiBold)
                Text(value.ifEmpty { "—" }, fontSize = 13.sp, color = CyberAccent, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

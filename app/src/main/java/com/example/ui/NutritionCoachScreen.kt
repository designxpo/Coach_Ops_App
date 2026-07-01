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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AiChatMessage
import com.example.data.GeminiCoach
import com.example.data.UserPreferences
import com.example.ui.theme.CyberAccent
import com.example.ui.theme.CyberAccentDark
import com.example.ui.theme.CyberBgCard
import com.example.ui.theme.CyberBgCardElevated
import com.example.ui.theme.CyberBgPrimary
import com.example.ui.theme.CyberDanger
import com.example.ui.theme.CyberTextMuted
import com.example.ui.theme.CyberTextPrimary
import com.example.ui.theme.CyberTextSecondary
import kotlinx.coroutines.launch

private val QUICK_QUESTIONS = listOf(
    "What should I eat to build muscle?",
    "How many calories do I need daily?",
    "Best Indian foods for weight loss?",
    "Plan my pre-workout meal",
    "How much protein is enough?",
    "What to eat after a workout?"
)

@Composable
fun NutritionCoachScreen(onBack: () -> Unit, userPreferences: UserPreferences) {
    val scope      = rememberCoroutineScope()
    val listState  = rememberLazyListState()
    val messages   = remember { mutableStateListOf<AiChatMessage>() }
    var inputText  by remember { mutableStateOf("") }
    var isLoading  by remember { mutableStateOf(false) }
    var errorMsg   by remember { mutableStateOf("") }

    val userContext = buildString {
        val role = userPreferences.userRole
        if (role == "coach") {
            appendLine("Role: Fitness Coach")
            if (userPreferences.coachName.isNotBlank()) appendLine("Name: ${userPreferences.coachName}")
            if (userPreferences.coachSpecialty.isNotBlank()) appendLine("Specialty: ${userPreferences.coachSpecialty}")
        } else {
            appendLine("Role: Member / fitness enthusiast")
            if (userPreferences.clientName.isNotBlank()) appendLine("Name: ${userPreferences.clientName}")
            if (userPreferences.clientGoal.isNotBlank()) appendLine("Fitness goal: ${userPreferences.clientGoal}")
        }
        val height = userPreferences.healthHeightCm
        val weight = userPreferences.healthWeightKg
        val age    = userPreferences.healthAgeYears
        if (height > 0f) appendLine("Height: ${height}cm, Weight: ${weight}kg, Age: ${age}")
        appendLine("Target calories (training day): ${userPreferences.trainingDayCalories} kcal")
        appendLine("Target protein: ${userPreferences.trainingDayProteinG}g")
    }

    fun sendMessage(text: String) {
        val trimmed = text.trim().take(500)
        if (trimmed.isBlank() || isLoading) return
        messages.add(AiChatMessage("user", trimmed))
        inputText  = ""
        isLoading  = true
        errorMsg   = ""
        scope.launch {
            val history = messages.dropLast(1)
            val result  = GeminiCoach.chat(history, userContext, trimmed)
            isLoading   = false
            result.fold(
                onSuccess = { messages.add(AiChatMessage("model", it)) },
                onFailure = { errorMsg = it.message ?: "Something went wrong. Try again." }
            )
        }
    }

    LaunchedEffect(messages.size, isLoading) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBgPrimary)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
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
                    .size(40.dp).clip(CircleShape)
                    .background(CyberBgCard)
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = CyberTextPrimary, modifier = Modifier.size(20.dp))
            }
            Column(Modifier.weight(1f)) {
                Text("AI Nutrition Coach", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = CyberTextPrimary)
                Text("Powered by Gemini", fontSize = 11.sp, color = CyberTextMuted)
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Brush.horizontalGradient(listOf(Color(0xFF4285F4), Color(0xFF0F9D58))))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text("AI", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            }
        }

        // ── Chat area ─────────────────────────────────────────────────────────
        LazyColumn(
            state          = listState,
            modifier       = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Empty state — quick questions
            if (messages.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("🥗", fontSize = 40.sp)
                        Spacer(Modifier.height(4.dp))
                        Text("Ask your nutrition coach", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = CyberTextPrimary)
                        Text("Personalised advice based on your profile", fontSize = 13.sp, color = CyberTextMuted)
                        Spacer(Modifier.height(12.dp))
                        QUICK_QUESTIONS.chunked(2).forEach { pair ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                pair.forEach { q ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(CyberBgCard)
                                            .border(1.dp, Color.White.copy(0.08f), RoundedCornerShape(12.dp))
                                            .clickable { sendMessage(q) }
                                            .padding(10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(q, fontSize = 12.sp, color = CyberTextSecondary, fontWeight = FontWeight.Medium)
                                    }
                                }
                                if (pair.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            itemsIndexed(messages) { _, msg ->
                val isUser = msg.role == "user"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                ) {
                    if (!isUser) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(CyberAccent.copy(0.15f)),
                            contentAlignment = Alignment.Center
                        ) { Text("🤖", fontSize = 16.sp) }
                        Spacer(Modifier.width(8.dp))
                    }
                    Box(
                        modifier = Modifier
                            .widthIn(max = 280.dp)
                            .clip(RoundedCornerShape(
                                topStart = if (isUser) 18.dp else 4.dp,
                                topEnd   = if (isUser) 4.dp else 18.dp,
                                bottomStart = 18.dp, bottomEnd = 18.dp
                            ))
                            .background(if (isUser) CyberAccent else CyberBgCard)
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(
                            msg.text,
                            fontSize  = 14.sp,
                            color     = if (isUser) CyberAccentDark else CyberTextPrimary,
                            lineHeight = 20.sp
                        )
                    }
                }
            }

            // Loading bubble
            if (isLoading) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier.size(32.dp).clip(CircleShape).background(CyberAccent.copy(0.15f)),
                            contentAlignment = Alignment.Center
                        ) { Text("🤖", fontSize = 16.sp) }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp, 18.dp, 18.dp, 18.dp))
                                .background(CyberBgCard)
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                CircularProgressIndicator(color = CyberAccent, modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                Text("Thinking…", fontSize = 13.sp, color = CyberTextMuted)
                            }
                        }
                    }
                }
            }

            // Error
            if (errorMsg.isNotBlank()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(CyberDanger.copy(0.1f))
                            .border(1.dp, CyberDanger.copy(0.3f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Text("⚠ $errorMsg", fontSize = 12.sp, color = CyberDanger)
                    }
                }
            }
        }

        // ── Input bar ─────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CyberBgCard)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value         = inputText,
                onValueChange = { inputText = it },
                placeholder   = { Text("Ask about nutrition, meals, macros…", fontSize = 13.sp, color = CyberTextMuted) },
                modifier      = Modifier.weight(1f),
                shape         = RoundedCornerShape(16.dp),
                colors        = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = CyberAccent,
                    unfocusedBorderColor = Color.White.copy(0.1f),
                    focusedTextColor     = CyberTextPrimary,
                    unfocusedTextColor   = CyberTextPrimary,
                    cursorColor          = CyberAccent,
                    focusedContainerColor   = CyberBgCardElevated,
                    unfocusedContainerColor = CyberBgCardElevated,
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { sendMessage(inputText) }),
                maxLines = 4,
                singleLine = false
            )
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (inputText.isNotBlank() && !isLoading) CyberAccent else CyberBgCardElevated)
                    .clickable(enabled = inputText.isNotBlank() && !isLoading) { sendMessage(inputText) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send, null,
                    tint     = if (inputText.isNotBlank() && !isLoading) CyberAccentDark else CyberTextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

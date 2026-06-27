package com.example.ui

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle as UiTextStyle
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BodyMeasurement
import com.example.data.Client
import com.example.data.ClientNote
import com.example.data.Program
import com.example.data.Signal
import com.example.data.WorkoutLog
import com.example.ui.theme.CyberAccent
import com.example.ui.theme.CyberAccentDark
import com.example.ui.theme.CyberBgCard
import com.example.ui.theme.CyberBgCardElevated
import com.example.ui.theme.CyberBgPrimary
import com.example.ui.theme.CyberDanger
import com.example.ui.theme.CyberSuccess
import com.example.ui.theme.CyberTextMuted
import com.example.ui.theme.CyberTextPrimary
import com.example.ui.theme.CyberTextSecondary
import com.example.ui.theme.CyberWarning
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddClientSheet(
    programs: List<Program>,
    onDismiss: () -> Unit,
    onSave: (name: String, phone: String, goal: String, mrr: Int, paymentCycle: String, programId: String?, trainingStartDateMillis: Long, trainingEndDateMillis: Long) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var goal by remember { mutableStateOf("") }
    var mrrText by remember { mutableStateOf("") }
    var paymentCycle by remember { mutableStateOf("MONTHLY") }
    var selectedProgramId by remember { mutableStateOf<String?>(null) }
    var trainingStartDateMillis by remember { mutableStateOf(0L) }
    var trainingEndDateMillis by remember { mutableStateOf(0L) }
    val context = LocalContext.current
    val dateFmt = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = CyberBgCard,
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Add Member", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = CyberTextPrimary)
            Spacer(Modifier.height(2.dp))

            SheetTextField("Full Name *", name, KeyboardType.Text) { name = it }
            SheetTextField("Phone Number", phone, KeyboardType.Phone) { phone = it }
            SheetTextField("Goal", goal, KeyboardType.Text) { goal = it }
            SheetTextField("Retainer Amount (₹)", mrrText, KeyboardType.Number) { mrrText = it }

            // Payment cycle picker
            Text("Payment Cycle", fontSize = 13.sp, color = CyberTextMuted, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("WEEKLY", "MONTHLY", "QUARTERLY", "YEARLY").forEach { cycle ->
                    val sel = cycle == paymentCycle
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (sel) CyberAccent else CyberBgCardElevated)
                            .border(1.dp, if (sel) CyberAccent else Color.White.copy(0.08f), RoundedCornerShape(999.dp))
                            .clickable { paymentCycle = cycle }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(cycle.lowercase().replaceFirstChar { it.uppercase() },
                            fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                            color = if (sel) CyberAccentDark else CyberTextSecondary)
                    }
                }
            }

            // Training duration — start + end date side by side
            Text("Training Duration (optional)", fontSize = 13.sp, color = CyberTextMuted, fontWeight = FontWeight.SemiBold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // ── Start Date ────────────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CyberBgCardElevated)
                        .border(
                            1.dp,
                            if (trainingStartDateMillis > 0) CyberAccent.copy(0.4f) else Color.White.copy(0.08f),
                            RoundedCornerShape(12.dp)
                        )
                        .clickable {
                            val cal = Calendar.getInstance()
                            if (trainingStartDateMillis > 0) cal.timeInMillis = trainingStartDateMillis
                            DatePickerDialog(
                                context,
                                { _, year, month, day ->
                                    cal.set(year, month, day, 0, 0, 0)
                                    trainingStartDateMillis = cal.timeInMillis
                                },
                                cal.get(Calendar.YEAR),
                                cal.get(Calendar.MONTH),
                                cal.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }
                        .padding(horizontal = 14.dp, vertical = 14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Text("▶", fontSize = 9.sp, color = CyberAccent)
                            Text("Start Date", fontSize = 11.sp, color = CyberTextMuted)
                        }
                        Text(
                            if (trainingStartDateMillis > 0) dateFmt.format(Date(trainingStartDateMillis))
                            else "Set date",
                            fontSize = 13.sp,
                            fontWeight = if (trainingStartDateMillis > 0) FontWeight.Bold else FontWeight.Normal,
                            color = if (trainingStartDateMillis > 0) CyberTextPrimary else CyberTextMuted
                        )
                        if (trainingStartDateMillis > 0) {
                            Text(
                                "Tap to change  ×",
                                fontSize = 10.sp, color = CyberTextMuted,
                                modifier = Modifier.clickable { trainingStartDateMillis = 0L }
                            )
                        }
                    }
                }

                // ── End Date ──────────────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CyberBgCardElevated)
                        .border(
                            1.dp,
                            if (trainingEndDateMillis > 0) CyberAccent.copy(0.4f) else Color.White.copy(0.08f),
                            RoundedCornerShape(12.dp)
                        )
                        .clickable {
                            val cal = Calendar.getInstance()
                            if (trainingEndDateMillis > 0) cal.timeInMillis = trainingEndDateMillis
                            DatePickerDialog(
                                context,
                                { _, year, month, day ->
                                    cal.set(year, month, day, 23, 59, 59)
                                    trainingEndDateMillis = cal.timeInMillis
                                },
                                cal.get(Calendar.YEAR),
                                cal.get(Calendar.MONTH),
                                cal.get(Calendar.DAY_OF_MONTH)
                            ).apply {
                                if (trainingStartDateMillis > 0) datePicker.minDate = trainingStartDateMillis
                            }.show()
                        }
                        .padding(horizontal = 14.dp, vertical = 14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Text("■", fontSize = 9.sp, color = CyberDanger)
                            Text("End Date", fontSize = 11.sp, color = CyberTextMuted)
                        }
                        Text(
                            if (trainingEndDateMillis > 0) dateFmt.format(Date(trainingEndDateMillis))
                            else "Set date",
                            fontSize = 13.sp,
                            fontWeight = if (trainingEndDateMillis > 0) FontWeight.Bold else FontWeight.Normal,
                            color = if (trainingEndDateMillis > 0) CyberTextPrimary else CyberTextMuted
                        )
                        if (trainingEndDateMillis > 0) {
                            Text(
                                "Tap to change  ×",
                                fontSize = 10.sp, color = CyberTextMuted,
                                modifier = Modifier.clickable { trainingEndDateMillis = 0L }
                            )
                        }
                    }
                }
            }

            // Duration summary pill
            if (trainingStartDateMillis > 0 && trainingEndDateMillis > 0) {
                val days = ((trainingEndDateMillis - trainingStartDateMillis) / 86400000L).toInt()
                val weeks = days / 7
                val summary = when {
                    weeks >= 4 -> "${weeks / 4} month${if (weeks / 4 > 1) "s" else ""} (${weeks}w)"
                    weeks > 0  -> "$weeks week${if (weeks > 1) "s" else ""}"
                    else       -> "$days day${if (days > 1) "s" else ""}"
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(CyberAccent.copy(0.1f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("📅 $summary training period", fontSize = 12.sp, color = CyberAccent, fontWeight = FontWeight.SemiBold)
                }
            }

            if (programs.isNotEmpty()) {
                Text("Program (optional)", fontSize = 13.sp, color = CyberTextMuted, fontWeight = FontWeight.SemiBold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(programs) { prog ->
                        val sel = prog.id == selectedProgramId
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(if (sel) CyberAccent else CyberBgCardElevated)
                                .border(1.dp, if (sel) CyberAccent else Color.White.copy(0.08f), RoundedCornerShape(999.dp))
                                .clickable { selectedProgramId = if (sel) null else prog.id }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(prog.name.take(22), fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                                color = if (sel) CyberAccentDark else CyberTextSecondary)
                        }
                    }
                }
            }

            val canSave = name.isNotBlank()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (canSave) CyberAccent else CyberBgCardElevated)
                    .clickable(enabled = canSave) {
                        onSave(name.trim(), phone.trim(), goal.trim(), mrrText.toIntOrNull() ?: 0, paymentCycle, selectedProgramId, trainingStartDateMillis, trainingEndDateMillis)
                    }
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Add Member", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold,
                    color = if (canSave) CyberAccentDark else CyberTextMuted)
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditClientSheet(
    client: Client,
    programs: List<Program>,
    onDismiss: () -> Unit,
    onSave: (Client) -> Unit
) {
    var name by remember { mutableStateOf(client.name) }
    var phone by remember { mutableStateOf(client.phoneNumber) }
    var goal by remember { mutableStateOf(client.initialGoal) }
    var mrrText by remember { mutableStateOf(client.mrr.toString()) }
    var paymentCycle by remember { mutableStateOf(client.paymentCycle) }
    var selectedProgramId by remember { mutableStateOf(client.programId) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = CyberBgCard,
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Edit Member", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = CyberTextPrimary)
            Spacer(Modifier.height(2.dp))

            SheetTextField("Full Name", name, KeyboardType.Text) { name = it }
            SheetTextField("Phone Number", phone, KeyboardType.Phone) { phone = it }
            SheetTextField("Goal", goal, KeyboardType.Text) { goal = it }
            SheetTextField("Retainer Amount (₹)", mrrText, KeyboardType.Number) { mrrText = it }

            Text("Payment Cycle", fontSize = 13.sp, color = CyberTextMuted, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("WEEKLY", "MONTHLY", "QUARTERLY", "YEARLY").forEach { cycle ->
                    val sel = cycle == paymentCycle
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (sel) CyberAccent else CyberBgCardElevated)
                            .border(1.dp, if (sel) CyberAccent else Color.White.copy(0.08f), RoundedCornerShape(999.dp))
                            .clickable { paymentCycle = cycle }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(cycle.lowercase().replaceFirstChar { it.uppercase() },
                            fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                            color = if (sel) CyberAccentDark else CyberTextSecondary)
                    }
                }
            }

            if (programs.isNotEmpty()) {
                Text("Program", fontSize = 13.sp, color = CyberTextMuted, fontWeight = FontWeight.SemiBold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(programs) { prog ->
                        val sel = prog.id == selectedProgramId
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(if (sel) CyberAccent else CyberBgCardElevated)
                                .border(1.dp, if (sel) CyberAccent else Color.White.copy(0.08f), RoundedCornerShape(999.dp))
                                .clickable { selectedProgramId = if (sel) null else prog.id }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(prog.name.take(22), fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                                color = if (sel) CyberAccentDark else CyberTextSecondary)
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(999.dp))
                    .background(CyberAccent)
                    .clickable {
                        onSave(client.copy(
                            name = name.trim().ifBlank { client.name },
                            phoneNumber = phone.trim(),
                            initialGoal = goal.trim().ifBlank { client.initialGoal },
                            mrr = mrrText.toIntOrNull() ?: client.mrr,
                            programId = selectedProgramId,
                            paymentCycle = paymentCycle
                        ))
                    }
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Save Changes", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = CyberAccentDark)
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignProgramSheet(
    programs: List<Program>,
    clients: List<Client>,
    preselectedProgramId: String? = null,
    onDismiss: () -> Unit,
    onAssign: (clientIds: List<String>, programId: String) -> Unit
) {
    // Multi-select: Set of selected client IDs
    var selectedClientIds by remember { mutableStateOf(setOf<String>()) }
    // When program is pre-locked (called from ProgramDetailScreen), hide the program picker
    var selectedProgramId by remember { mutableStateOf(preselectedProgramId) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = CyberBgCard,
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                if (preselectedProgramId != null) "Enroll Clients" else "Assign Program",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = CyberTextPrimary
            )
            Spacer(Modifier.height(2.dp))

            // Client multi-select
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Select Members", fontSize = 13.sp, color = CyberTextMuted, fontWeight = FontWeight.SemiBold)
                if (selectedClientIds.isNotEmpty()) {
                    TextButton(onClick = { selectedClientIds = setOf() }) {
                        Text("Clear all", fontSize = 11.sp, color = CyberAccent)
                    }
                }
            }

            if (clients.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CyberBgCardElevated)
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No clients added yet", fontSize = 13.sp, color = CyberTextMuted)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    clients.forEach { client ->
                        val sel = client.id in selectedClientIds
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (sel) CyberAccent.copy(alpha = 0.12f) else CyberBgCardElevated)
                                .border(1.dp, if (sel) CyberAccent.copy(0.3f) else Color.Transparent, RoundedCornerShape(16.dp))
                                .clickable {
                                    selectedClientIds = if (sel)
                                        selectedClientIds - client.id
                                    else
                                        selectedClientIds + client.id
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(if (sel) CyberAccent else CyberBgCard),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    client.name.take(1),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (sel) CyberAccentDark else CyberAccent
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    client.name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (sel) CyberAccent else CyberTextPrimary
                                )
                                Text(client.status, fontSize = 11.sp, color = CyberTextMuted)
                            }
                            // Checkbox indicator
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (sel) CyberAccent else CyberBgCard)
                                    .border(1.dp, if (sel) CyberAccent else CyberTextMuted.copy(0.3f), RoundedCornerShape(6.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (sel) Icon(Icons.Filled.Check, null, tint = CyberAccentDark, modifier = Modifier.size(13.dp))
                            }
                        }
                    }
                }
            }

            // Program picker — hidden when program is pre-selected
            if (preselectedProgramId == null) {
                Text("Select Program", fontSize = 13.sp, color = CyberTextMuted, fontWeight = FontWeight.SemiBold)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    programs.forEach { prog ->
                        val sel = prog.id == selectedProgramId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (sel) CyberAccent.copy(alpha = 0.12f) else CyberBgCardElevated)
                                .border(1.dp, if (sel) CyberAccent.copy(0.3f) else Color.Transparent, RoundedCornerShape(16.dp))
                                .clickable { selectedProgramId = prog.id }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    prog.name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (sel) CyberAccent else CyberTextPrimary
                                )
                                Text("${prog.durationWeeks}w · ${prog.daysPerWeek}x/wk", fontSize = 11.sp, color = CyberTextMuted)
                            }
                            if (sel) Icon(Icons.Filled.Check, null, tint = CyberAccent, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            val canAssign = selectedClientIds.isNotEmpty() && selectedProgramId != null
            val btnLabel = when {
                selectedClientIds.isEmpty() -> "Select clients above"
                selectedClientIds.size == 1 -> "Enroll 1 Client"
                else -> "Enroll ${selectedClientIds.size} Clients"
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (canAssign) CyberAccent else CyberBgCardElevated)
                    .clickable(enabled = canAssign) {
                        onAssign(selectedClientIds.toList(), selectedProgramId!!)
                    }
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(btnLabel, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold,
                    color = if (canAssign) CyberAccentDark else CyberTextMuted)
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun EditProfileSheet(
    name: String,
    email: String,
    phone: String,
    specialty: String,
    clientRange: String = "",
    challenge: String = "",
    onDismiss: () -> Unit,
    onSave: (name: String, email: String, phone: String, specialty: String, clientRange: String, challenge: String) -> Unit
) {
    val specialties = listOf(
        "Strength Training", "Fat Loss", "Hypertrophy", "Sports Performance",
        "Yoga & Mobility", "General Wellness", "Boxing / MMA", "Calisthenics",
        "Cardio & Endurance", "CrossFit / HIIT", "Dance Fitness / Zumba",
        "Nutrition Coaching", "Rehabilitation", "Senior Fitness",
        "Pre/Post Natal", "Pilates", "Personal Training", "Functional Fitness"
    )
    val clientRanges = listOf("1–5 clients", "6–15 clients", "16–30 clients", "30+ clients")
    val challenges   = listOf("Payment collection", "Tracking progress", "Managing programs", "Client communication")

    var editName by remember { mutableStateOf(name) }
    var editEmail by remember { mutableStateOf(email) }
    var editPhone by remember { mutableStateOf(phone) }
    // Multi-select: parse existing comma-separated specialty
    var selectedSpecialties by remember {
        mutableStateOf(specialty.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet())
    }
    var selectedClientRange by remember { mutableStateOf(clientRange) }
    var selectedChallenge by remember { mutableStateOf(challenge) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = CyberBgCard,
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Edit Profile", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = CyberTextPrimary)
            Spacer(Modifier.height(2.dp))

            SheetTextField("Full Name", editName, KeyboardType.Text) { editName = it }
            SheetTextField("Email address", editEmail, KeyboardType.Email) { editEmail = it }
            SheetTextField("Phone number", editPhone, KeyboardType.Phone) { editPhone = it }

            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Specialties", fontSize = 13.sp, color = CyberTextMuted, fontWeight = FontWeight.SemiBold)
                    if (selectedSpecialties.isNotEmpty()) {
                        Text("${selectedSpecialties.size} selected", fontSize = 11.sp,
                            color = CyberAccent, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(8.dp))
                // Wrap-around chips grid
                androidx.compose.foundation.layout.FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    specialties.forEach { spec ->
                        val sel = spec in selectedSpecialties
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(if (sel) CyberAccent else CyberBgCardElevated)
                                .border(1.dp, if (sel) CyberAccent else Color.White.copy(0.08f), RoundedCornerShape(999.dp))
                                .clickable {
                                    selectedSpecialties = if (sel)
                                        selectedSpecialties - spec
                                    else
                                        selectedSpecialties + spec
                                }
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            Text(spec, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                                color = if (sel) CyberAccentDark else CyberTextSecondary)
                        }
                    }
                }
            }

            // Client range
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Member Range", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = CyberTextMuted)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(clientRanges) { range ->
                        val sel = range == selectedClientRange
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(if (sel) CyberAccent else CyberBgCardElevated)
                                .border(1.dp, if (sel) CyberAccent else Color.White.copy(0.08f), RoundedCornerShape(999.dp))
                                .clickable { selectedClientRange = if (sel) "" else range }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(range, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                                color = if (sel) CyberAccentDark else CyberTextSecondary)
                        }
                    }
                }
            }

            // Main focus
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Main Focus", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = CyberTextMuted)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(challenges) { ch ->
                        val sel = ch == selectedChallenge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(if (sel) CyberAccent else CyberBgCardElevated)
                                .border(1.dp, if (sel) CyberAccent else Color.White.copy(0.08f), RoundedCornerShape(999.dp))
                                .clickable { selectedChallenge = if (sel) "" else ch }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(ch, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                                color = if (sel) CyberAccentDark else CyberTextSecondary)
                        }
                    }
                }
            }

            val canSave = editName.isNotBlank()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (canSave) CyberAccent else CyberBgCardElevated)
                    .clickable(enabled = canSave) {
                        onSave(editName.trim(), editEmail.trim().lowercase(), editPhone.trim(),
                            selectedSpecialties.joinToString(","), selectedClientRange, selectedChallenge)
                    }
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Text("Save Changes", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold,
                    color = if (canSave) CyberAccentDark else CyberTextMuted)
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareProgramSheet(
    program: Program,
    enrolledClients: List<Client>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val whatsappGreen = Color(0xFF25D366)

    fun buildMessage(client: Client): String {
        val tagsDisplay = program.tags.split(",")
            .filter { it.isNotBlank() }.joinToString(", ") { it.trim() }
        return "Hey ${client.name}! 💪\n\nHere's your training program:\n" +
            "📋 *${program.name}*\n" +
            "📅 Duration: ${program.durationWeeks} weeks\n" +
            "⚡ ${program.daysPerWeek} sessions per week\n" +
            "🎯 Focus: $tagsDisplay\n\n" +
            "You're on *Week ${client.programWeek}* — keep going! 🔥"
    }

    fun openWhatsApp(client: Client) {
        val msg = buildMessage(client)
        val url = if (client.phoneNumber.isNotEmpty())
            "https://wa.me/91${client.phoneNumber}?text=${Uri.encode(msg)}"
        else "https://wa.me/?text=${Uri.encode(msg)}"
        try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } catch (_: Exception) {}
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = CyberBgCard,
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Send Program", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = CyberTextPrimary)
                    Text("via WhatsApp to enrolled members", fontSize = 12.sp, color = CyberTextMuted)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(CyberBgCardElevated)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("${enrolledClients.size} enrolled", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = CyberTextSecondary)
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(CyberAccent.copy(alpha = 0.08f))
                    .border(1.dp, CyberAccent.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(program.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = CyberAccent)
                    Text(
                        "${program.durationWeeks} weeks · ${program.daysPerWeek}x/week",
                        fontSize = 12.sp, color = CyberTextMuted
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(
                "Tap send to open WhatsApp for each client",
                fontSize = 12.sp, color = CyberTextMuted,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            Spacer(Modifier.height(10.dp))

            if (enrolledClients.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("👥", fontSize = 28.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("No members enrolled yet", fontSize = 14.sp, color = CyberTextMuted)
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(enrolledClients, key = { it.id }) { client ->
                        val consistencyColor = when {
                            client.consistencyScore >= 80 -> CyberSuccess
                            client.consistencyScore >= 60 -> CyberWarning
                            else -> CyberDanger
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(CyberBgCardElevated)
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(CyberAccent.copy(alpha = 0.12f))
                                    .border(1.dp, CyberAccent.copy(alpha = 0.3f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(client.name.take(1), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = CyberAccent)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(client.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = CyberTextPrimary)
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Week ${client.programWeek}", fontSize = 11.sp, color = CyberTextMuted)
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(999.dp))
                                            .background(consistencyColor.copy(alpha = 0.12f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("${client.consistencyScore}%", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = consistencyColor)
                                    }
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(whatsappGreen)
                                    .clickable { openWhatsApp(client) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SheetTextField(
    label: String,
    value: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = CyberAccent,
            unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
            focusedTextColor = CyberTextPrimary,
            unfocusedTextColor = CyberTextPrimary,
            cursorColor = CyberAccent,
            focusedLabelColor = CyberAccent,
            unfocusedLabelColor = CyberTextMuted,
            focusedContainerColor = CyberBgPrimary,
            unfocusedContainerColor = CyberBgPrimary,
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgramFormSheet(
    existingProgram: Program? = null,
    onDismiss: () -> Unit,
    onSave: (name: String, weeks: Int, daysPerWeek: Int, tags: String, workingDays: String) -> Unit
) {
    val isEdit = existingProgram != null

    val durationOptions = listOf(4, 6, 8, 10, 12, 16, 20, 24)
    val daysOptions = listOf(2, 3, 4, 5, 6)
    val tagOptions = listOf(
        "Strength", "Fat Loss", "Hypertrophy",
        "Sports Performance", "Mobility", "Wellness", "General Fitness"
    )
    val allDays = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")

    var name by remember { mutableStateOf(existingProgram?.name ?: "") }
    var selectedWeeks by remember {
        mutableStateOf(
            if (existingProgram != null && existingProgram.durationWeeks in durationOptions)
                existingProgram.durationWeeks else null as Int?
        )
    }
    var selectedDays by remember {
        mutableStateOf(
            if (existingProgram != null && existingProgram.daysPerWeek in daysOptions)
                existingProgram.daysPerWeek else null as Int?
        )
    }
    var selectedTags by remember {
        mutableStateOf(
            existingProgram?.tags?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() }?.toSet()
                ?: setOf()
        )
    }
    var workingDaySet by remember {
        mutableStateOf(
            existingProgram?.workingDays?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() }?.toSet()
                ?: setOf()
        )
    }

    val canSave = name.isNotBlank() && selectedWeeks != null && selectedDays != null

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = CyberBgCard,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text(
                if (isEdit) "Edit Program" else "Create Program",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = CyberTextPrimary
            )

            // Name
            SheetTextField("Program Name *", name, KeyboardType.Text) { name = it }

            // Duration
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Duration", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = CyberTextMuted)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(durationOptions) { weeks ->
                        val sel = weeks == selectedWeeks
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (sel) CyberAccent else CyberBgCardElevated)
                                .border(1.dp, if (sel) CyberAccent else Color.White.copy(0.08f), RoundedCornerShape(12.dp))
                                .clickable { selectedWeeks = weeks }
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Text(
                                "$weeks wks",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (sel) CyberAccentDark else CyberTextSecondary
                            )
                        }
                    }
                }
            }

            // Days per week
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Sessions / Week", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = CyberTextMuted)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    daysOptions.forEach { days ->
                        val sel = days == selectedDays
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (sel) CyberAccent else CyberBgCardElevated)
                                .border(1.dp, if (sel) CyberAccent else Color.White.copy(0.08f), RoundedCornerShape(12.dp))
                                .clickable { selectedDays = days }
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Text(
                                "${days}x",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (sel) CyberAccentDark else CyberTextSecondary
                            )
                        }
                    }
                }
            }

            // Tags
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Focus Tags", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = CyberTextMuted)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(tagOptions) { tag ->
                        val sel = tag in selectedTags
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(if (sel) CyberAccent else CyberBgCardElevated)
                                .border(1.dp, if (sel) CyberAccent else Color.White.copy(0.08f), RoundedCornerShape(999.dp))
                                .clickable {
                                    selectedTags = if (sel) selectedTags - tag else selectedTags + tag
                                }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                tag,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (sel) CyberAccentDark else CyberTextSecondary
                            )
                        }
                    }
                }
            }

            // Training days picker
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Training Days", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = CyberTextMuted)
                    if (workingDaySet.isNotEmpty()) {
                        Text(
                            "${workingDaySet.size} selected",
                            fontSize = 11.sp, color = CyberAccent, fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text("Optional", fontSize = 11.sp, color = CyberTextMuted)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    allDays.forEach { day ->
                        val sel = day in workingDaySet
                        val limitReached = selectedDays != null && workingDaySet.size >= selectedDays!! && !sel
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    when {
                                        sel -> CyberAccent
                                        limitReached -> CyberBgCardElevated.copy(alpha = 0.4f)
                                        else -> CyberBgCardElevated
                                    }
                                )
                                .border(1.dp,
                                    if (sel) CyberAccent else Color.White.copy(0.06f),
                                    RoundedCornerShape(10.dp))
                                .clickable(enabled = !limitReached) {
                                    workingDaySet = if (sel) workingDaySet - day else workingDaySet + day
                                }
                                .padding(horizontal = 8.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                day.take(1),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (sel) CyberAccentDark else if (limitReached) CyberTextMuted.copy(alpha = 0.4f) else CyberTextSecondary
                            )
                        }
                    }
                }
                if (selectedDays != null && workingDaySet.isNotEmpty() && workingDaySet.size != selectedDays) {
                    Text(
                        "Select exactly $selectedDays days to match your sessions/week setting",
                        fontSize = 11.sp, color = CyberWarning
                    )
                }
            }

            // Save
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (canSave) CyberAccent else CyberBgCardElevated)
                    .clickable(enabled = canSave) {
                        val tagsStr = selectedTags.joinToString(",").ifBlank { "General Fitness" }
                        val workingDaysStr = workingDaySet.sortedBy { allDays.indexOf(it) }.joinToString(",")
                        onSave(name.trim(), selectedWeeks!!, selectedDays!!, tagsStr, workingDaysStr)
                    }
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    if (isEdit) "Save Changes" else "Create Program",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (canSave) CyberAccentDark else CyberTextMuted
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsSheet(
    signals: List<Signal>,
    clients: List<Client>,
    programs: List<Program>,
    onDismiss: () -> Unit,
    onResolve: (signalId: String) -> Unit
) {
    val context = LocalContext.current

    fun waMessage(signal: Signal) = when (signal.type) {
        "MISSED_WORKOUT" -> "Hey ${signal.clientName}, noticed you haven't logged a session in a few days. Everything okay?"
        "UPI_EXPIRING" -> "Hi ${signal.clientName}, your auto-pay is due to renew soon. I'll send you the renewal link — takes 30 seconds."
        "FORM_CHECK" -> "Got your video, ${signal.clientName} — let me review it tonight and send you feedback."
        "PAYMENT_OVERDUE" -> "Hi ${signal.clientName}, just a quick reminder about the pending payment. Let me know if you have any issues."
        "MILESTONE" -> "Amazing work ${signal.clientName}! You just hit a major milestone. Keep it up — this is where the real results show!"
        else -> "Hi ${signal.clientName}, just checking in! Let me know if you need anything."
    }

    fun openWhatsApp(signal: Signal) {
        val phone = clients.find { it.id == signal.clientId }?.phoneNumber ?: ""
        val msg = waMessage(signal)
        val url = if (phone.isNotEmpty()) "https://wa.me/91$phone?text=${Uri.encode(msg)}"
                  else "https://wa.me/?text=${Uri.encode(msg)}"
        try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } catch (_: Exception) {}
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = CyberBgCard,
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Notifications", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = CyberTextPrimary)
                    if (signals.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(CyberDanger)
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("${signals.size}", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                        }
                    }
                }
                if (signals.isNotEmpty()) {
                    TextButton(onClick = { signals.forEach { onResolve(it.id) }; onDismiss() }) {
                        Text("Clear all", fontSize = 12.sp, color = CyberTextMuted)
                    }
                }
            }

            if (signals.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.CheckCircle, null, tint = CyberSuccess, modifier = Modifier.size(40.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("All caught up!", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = CyberTextPrimary)
                        Spacer(Modifier.height(4.dp))
                        Text("No pending notifications", fontSize = 13.sp, color = CyberTextMuted)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(signals, key = { it.id }) { signal ->
                        val severityColor = when (signal.severity) {
                            "RED" -> CyberDanger
                            "YELLOW" -> CyberWarning
                            else -> CyberAccent
                        }
                        val typeLabel = when (signal.type) {
                            "MISSED_WORKOUT" -> "Missed Workout"
                            "UPI_EXPIRING" -> "Payment"
                            "PAYMENT_OVERDUE" -> "Overdue"
                            "FORM_CHECK" -> "Check In"
                            "MILESTONE" -> "Milestone"
                            else -> signal.type
                        }
                        val typeColor = when (signal.type) {
                            "MISSED_WORKOUT" -> CyberWarning
                            "UPI_EXPIRING", "PAYMENT_OVERDUE" -> CyberDanger
                            "MILESTONE" -> CyberAccent
                            else -> CyberSuccess
                        }
                        val program = clients.find { it.id == signal.clientId }?.programId
                            ?.let { pid -> programs.find { it.id == pid } }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(CyberBgCardElevated)
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Severity dot + avatar
                            Box(contentAlignment = Alignment.BottomEnd) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(CyberAccent.copy(alpha = 0.12f))
                                        .border(1.dp, CyberAccent.copy(0.3f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(signal.clientName.take(1), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = CyberAccent)
                                }
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(severityColor, CircleShape)
                                        .border(1.5.dp, CyberBgCardElevated, CircleShape)
                                )
                            }

                            // Content
                            Column(modifier = Modifier.weight(1f)) {
                                Text(signal.clientName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CyberTextPrimary)
                                Spacer(Modifier.height(2.dp))
                                Text(signal.description, fontSize = 11.sp, color = CyberTextSecondary, lineHeight = 15.sp)
                                Spacer(Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(999.dp))
                                            .background(typeColor.copy(alpha = 0.12f))
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(typeLabel, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = typeColor)
                                    }
                                    if (program != null) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(999.dp))
                                                .background(CyberAccent.copy(alpha = 0.1f))
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Text(program.name.take(14), fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = CyberAccent)
                                        }
                                    }
                                }
                            }

                            // Action buttons
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF25D366))
                                        .clickable { openWhatsApp(signal) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Send, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(CyberSuccess.copy(alpha = 0.15f))
                                        .clickable { onResolve(signal.id) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.Check, null, tint = CyberSuccess, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── Log Session Sheet ────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogSessionSheet(
    clientId: String,
    programId: String?,
    programs: List<Program>,
    onDismiss: () -> Unit,
    onSave: (WorkoutLog) -> Unit
) {
    val sessionTemplates = listOf("Push Day", "Pull Day", "Leg Day", "Full Body", "HIIT", "Cardio", "Mobility", "Upper Body", "Lower Body", "Custom")

    var sessionName by remember { mutableStateOf("") }
    var exercises by remember { mutableStateOf("") }
    var durationText by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = CyberBgCard,
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Log Session", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = CyberTextPrimary)
            Spacer(Modifier.height(2.dp))

            Text("Session Type", fontSize = 13.sp, color = CyberTextMuted, fontWeight = FontWeight.SemiBold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(sessionTemplates) { t ->
                    val sel = t == sessionName
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (sel) CyberAccent else CyberBgCardElevated)
                            .border(1.dp, if (sel) CyberAccent else Color.White.copy(0.08f), RoundedCornerShape(999.dp))
                            .clickable { sessionName = if (sel) "" else t }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(t, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                            color = if (sel) CyberAccentDark else CyberTextSecondary)
                    }
                }
            }

            SheetTextField("Session Name *", sessionName, KeyboardType.Text) { sessionName = it }
            SheetTextField("Exercises (e.g. Squat · Bench · Row)", exercises, KeyboardType.Text) { exercises = it }
            SheetTextField("Duration (minutes)", durationText, KeyboardType.Number) { durationText = it }
            SheetTextField("Notes (optional)", notes, KeyboardType.Text) { notes = it }

            val canSave = sessionName.isNotBlank()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (canSave) CyberAccent else CyberBgCardElevated)
                    .clickable(enabled = canSave) {
                        onSave(WorkoutLog(
                            id = java.util.UUID.randomUUID().toString(),
                            clientId = clientId,
                            programId = programId,
                            sessionName = sessionName.trim(),
                            exercises = exercises.trim(),
                            durationMins = durationText.toIntOrNull() ?: 0,
                            notes = notes.trim()
                        ))
                    }
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Text("Save Session", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold,
                    color = if (canSave) CyberAccentDark else CyberTextMuted)
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

// ─── Log Measurement Sheet ────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogMeasurementSheet(
    clientId: String,
    onDismiss: () -> Unit,
    onSave: (BodyMeasurement) -> Unit
) {
    var weightText by remember { mutableStateOf("") }
    var bodyFatText by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = CyberBgCard,
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Log Measurement", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = CyberTextPrimary)
            Spacer(Modifier.height(2.dp))

            SheetTextField("Weight (kg)", weightText, KeyboardType.Decimal) { weightText = it }
            SheetTextField("Body Fat % (optional)", bodyFatText, KeyboardType.Decimal) { bodyFatText = it }
            SheetTextField("Notes (optional)", notes, KeyboardType.Text) { notes = it }

            val canSave = weightText.isNotBlank()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (canSave) CyberAccent else CyberBgCardElevated)
                    .clickable(enabled = canSave) {
                        onSave(BodyMeasurement(
                            id = java.util.UUID.randomUUID().toString(),
                            clientId = clientId,
                            weightKg = weightText.toFloatOrNull() ?: 0f,
                            bodyFatPct = bodyFatText.toFloatOrNull() ?: 0f,
                            notes = notes.trim()
                        ))
                    }
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Text("Save Measurement", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold,
                    color = if (canSave) CyberAccentDark else CyberTextMuted)
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

// ─── Broadcast Sheet ──────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BroadcastSheet(
    clients: List<Client>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val templates = listOf(
        "Check-in" to "Hey {name}, just checking in! How's the training going this week? Let me know if you need any adjustments.",
        "Payment reminder" to "Hi {name}, just a quick reminder about your upcoming payment. Let me know if you have any questions.",
        "Motivation" to "Hey {name}! You're doing amazing — keep pushing. Consistency is everything. Let's crush this week!",
        "Program update" to "Hi {name}, I've updated your training plan for next week. Check it out and let me know what you think!"
    )

    var selectedClientIds by remember { mutableStateOf(setOf<String>()) }
    var selectedTemplate by remember { mutableStateOf(0) }

    fun openWhatsApp(client: Client) {
        val msg = templates[selectedTemplate].second.replace("{name}", client.name)
        val url = if (client.phoneNumber.isNotEmpty())
            "https://wa.me/91${client.phoneNumber}?text=${Uri.encode(msg)}"
        else "https://wa.me/?text=${Uri.encode(msg)}"
        try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } catch (_: Exception) {}
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = CyberBgCard,
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Broadcast", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = CyberTextPrimary)
                    Text("Send a message to multiple members", fontSize = 12.sp, color = CyberTextMuted)
                }
                if (selectedClientIds.isNotEmpty()) {
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(999.dp))
                            .background(CyberAccent.copy(0.12f))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("${selectedClientIds.size} selected", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CyberAccent)
                    }
                }
            }

            // Template picker
            Column(modifier = Modifier.padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Message Template", fontSize = 13.sp, color = CyberTextMuted, fontWeight = FontWeight.SemiBold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(templates.size) { i ->
                        val sel = i == selectedTemplate
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(if (sel) CyberAccent else CyberBgCardElevated)
                                .border(1.dp, if (sel) CyberAccent else Color.White.copy(0.08f), RoundedCornerShape(999.dp))
                                .clickable { selectedTemplate = i }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(templates[i].first, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                                color = if (sel) CyberAccentDark else CyberTextSecondary)
                        }
                    }
                }

                Box(
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CyberBgCardElevated)
                        .padding(12.dp)
                ) {
                    Text(
                        templates[selectedTemplate].second.replace("{name}", "client"),
                        fontSize = 12.sp, color = CyberTextSecondary, lineHeight = 17.sp
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Client selection
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Select Members", fontSize = 13.sp, color = CyberTextMuted, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { selectedClientIds = clients.map { it.id }.toSet() }) {
                        Text("All", fontSize = 11.sp, color = CyberAccent)
                    }
                    if (selectedClientIds.isNotEmpty()) {
                        TextButton(onClick = { selectedClientIds = setOf() }) {
                            Text("Clear", fontSize = 11.sp, color = CyberTextMuted)
                        }
                    }
                }
            }

            LazyColumn(
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(clients, key = { it.id }) { client ->
                    val sel = client.id in selectedClientIds
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (sel) CyberAccent.copy(0.12f) else CyberBgCardElevated)
                            .border(1.dp, if (sel) CyberAccent.copy(0.3f) else Color.Transparent, RoundedCornerShape(16.dp))
                            .clickable {
                                selectedClientIds = if (sel) selectedClientIds - client.id else selectedClientIds + client.id
                            }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(34.dp).clip(CircleShape)
                                .background(if (sel) CyberAccent else CyberBgCard),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(client.name.take(1), fontSize = 13.sp, fontWeight = FontWeight.Bold,
                                color = if (sel) CyberAccentDark else CyberAccent)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(client.name, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                                color = if (sel) CyberAccent else CyberTextPrimary)
                            Text(client.status, fontSize = 11.sp, color = CyberTextMuted)
                        }
                        if (sel) {
                            Box(
                                modifier = Modifier.size(36.dp).clip(CircleShape)
                                    .background(Color(0xFF25D366))
                                    .clickable { openWhatsApp(client) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        } else {
                            Box(
                                modifier = Modifier.size(20.dp).clip(RoundedCornerShape(6.dp))
                                    .background(CyberBgCard)
                                    .border(1.dp, CyberTextMuted.copy(0.3f), RoundedCornerShape(6.dp))
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─── Log Day Sheet ────────────────────────────────────────────────────────────
// Coach taps a training day cell to mark it: Completed / Leave / Absent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogDaySheet(
    dateMillis: Long,           // which day is being logged
    clientId: String,
    programId: String?,
    existingLog: WorkoutLog?,   // non-null = already logged, shows current state
    onDismiss: () -> Unit,
    onLog: (type: String, notes: String) -> Unit  // type = "COMPLETED" | "LEAVE" | "ABSENT"
) {
    val dateStr = SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(Date(dateMillis))
    var notes by remember { mutableStateOf(existingLog?.missedReason?.removePrefix("LEAVE: ")?.removePrefix("ABSENT: ") ?: existingLog?.notes ?: "") }

    val currentType = when {
        existingLog == null -> null
        !existingLog.isMissed -> "COMPLETED"
        existingLog.missedReason.startsWith("LEAVE") -> "LEAVE"
        else -> "ABSENT"
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = CyberBgCard,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Handle
            Box(
                modifier = Modifier.width(40.dp).height(4.dp).clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.15f)).align(Alignment.CenterHorizontally)
            )
            Spacer(Modifier.height(4.dp))

            // Date header
            Column {
                Text("Log Training Day", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = CyberTextPrimary)
                Text(dateStr, fontSize = 13.sp, color = CyberTextMuted)
            }

            if (currentType != null) {
                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                        .background(
                            when (currentType) {
                                "COMPLETED" -> CyberSuccess.copy(alpha = 0.12f)
                                "LEAVE"     -> CyberWarning.copy(alpha = 0.12f)
                                else        -> CyberDanger.copy(alpha = 0.12f)
                            }
                        ).padding(12.dp)
                ) {
                    Text(
                        when (currentType) {
                            "COMPLETED" -> "✓  Already marked as completed"
                            "LEAVE"     -> "🏖  Already marked as leave"
                            else        -> "✗  Already marked as absent"
                        },
                        fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                        color = when (currentType) {
                            "COMPLETED" -> CyberSuccess
                            "LEAVE"     -> CyberWarning
                            else        -> CyberDanger
                        }
                    )
                }
            }

            // Notes
            Box(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                    .background(CyberBgCardElevated)
                    .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                androidx.compose.foundation.text.BasicTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = CyberTextPrimary, fontSize = 14.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Default
                    ),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(CyberAccent),
                    decorationBox = { inner ->
                        if (notes.isEmpty()) Text("Notes or reason (optional)", fontSize = 14.sp, color = CyberTextMuted)
                        inner()
                    }
                )
            }

            // Three action buttons
            Text("Mark as:", fontSize = 12.sp, color = CyberTextMuted, fontWeight = FontWeight.SemiBold)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                // Completed
                Box(
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(16.dp))
                        .background(CyberSuccess.copy(alpha = if (currentType == "COMPLETED") 0.25f else 0.10f))
                        .border(1.dp, CyberSuccess.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                        .clickable { onLog("COMPLETED", notes) }
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("✓", fontSize = 18.sp, color = CyberSuccess)
                        Text("Done", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CyberSuccess)
                    }
                }
                // Leave
                Box(
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(16.dp))
                        .background(CyberWarning.copy(alpha = if (currentType == "LEAVE") 0.25f else 0.10f))
                        .border(1.dp, CyberWarning.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                        .clickable { onLog("LEAVE", notes) }
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("🏖", fontSize = 18.sp)
                        Text("Leave", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CyberWarning)
                    }
                }
                // Absent
                Box(
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(16.dp))
                        .background(CyberDanger.copy(alpha = if (currentType == "ABSENT") 0.25f else 0.10f))
                        .border(1.dp, CyberDanger.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                        .clickable { onLog("ABSENT", notes) }
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("✗", fontSize = 18.sp, color = CyberDanger)
                        Text("Absent", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CyberDanger)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

// ─── Public Profile Sheet ────────────────────────────────────────────────────
// Coach uses this to publish their profile to the client marketplace

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublicProfileSheet(
    initialIsPublic: Boolean,
    initialCity: String,
    initialBio: String,
    initialWorkDescription: String = "",
    initialFeeSession: Int,
    initialFeeMonthly: Int,
    initialAvailDays: String,
    initialYearsExp: Int = 0,
    initialProfileImageUrl: String = "",
    initialPortfolioImages: String = "",
    onDismiss: () -> Unit,
    onSave: (isPublic: Boolean, city: String, bio: String, workDescription: String,
             feeSession: Int, feeMonthly: Int,
             availDays: String, yearsExp: Int, profileImageUrl: String, portfolioImages: String,
             lat: Double, lng: Double) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope   = rememberCoroutineScope()
    val allDays = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")

    var isPublic          by remember { mutableStateOf(initialIsPublic) }
    var city              by remember { mutableStateOf(initialCity) }
    var bio               by remember { mutableStateOf(initialBio) }
    var workDescription   by remember { mutableStateOf(initialWorkDescription) }

    // Location search state
    var isLocationFocused by remember { mutableStateOf(false) }
    var nearbyAreas       by remember { mutableStateOf<List<com.example.data.NearbyArea>>(emptyList()) }
    var isFetchingNearby  by remember { mutableStateOf(false) }
    var feeSessionText by remember { mutableStateOf(if (initialFeeSession > 0) initialFeeSession.toString() else "") }
    var feeMonthlyText by remember { mutableStateOf(if (initialFeeMonthly > 0) initialFeeMonthly.toString() else "") }
    var yearsExpText   by remember { mutableStateOf(if (initialYearsExp > 0) initialYearsExp.toString() else "") }

    // GPS state for coach location
    var gpsLat        by remember { mutableStateOf(0.0) }
    var gpsLng        by remember { mutableStateOf(0.0) }
    var isGpsLocating by remember { mutableStateOf(false) }
    var gpsLocated    by remember { mutableStateOf(false) }

    val locationPermLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants[android.Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                      grants[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            isGpsLocating = true
            scope.launch {
                val coords = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    com.example.data.GeoUtils.getDeviceLocation(context)
                }
                isGpsLocating = false
                if (coords != null) {
                    gpsLat = coords.first
                    gpsLng = coords.second
                    gpsLocated = true
                    val areaName = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        com.example.data.GeoUtils.reverseGeocode(context, coords.first, coords.second)
                    }
                    city = areaName
                }
            }
        }
    }

    fun useGpsLocation() {
        val fine = androidx.core.content.ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = androidx.core.content.ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.ACCESS_COARSE_LOCATION)
        if (fine == android.content.pm.PackageManager.PERMISSION_GRANTED ||
            coarse == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            isGpsLocating = true
            scope.launch {
                val coords = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    com.example.data.GeoUtils.getDeviceLocation(context)
                }
                isGpsLocating = false
                if (coords != null) {
                    gpsLat = coords.first
                    gpsLng = coords.second
                    gpsLocated = true
                    val areaName = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        com.example.data.GeoUtils.reverseGeocode(context, coords.first, coords.second)
                    }
                    city = areaName
                }
            }
        } else {
            locationPermLauncher.launch(arrayOf(
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ))
        }
    }
    var selectedDays   by remember {
        mutableStateOf(initialAvailDays.split(",").map { it.trim() }.filter { it.isNotBlank() }.toSet())
    }

    // Image state
    var profileImageUrl    by remember { mutableStateOf(initialProfileImageUrl) }
    var portfolioImageUrls by remember {
        mutableStateOf(initialPortfolioImages.split(",").filter { it.isNotBlank() }.toMutableList())
    }
    var uploadingProfile   by remember { mutableStateOf(false) }
    var uploadingPortfolio by remember { mutableStateOf(-1) } // index being uploaded, -1 = none
    var uploadError        by remember { mutableStateOf("") }
    var pendingSlot        by remember { mutableStateOf(-1) }

    // Profile photo picker
    val profilePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            uploadingProfile = true
            uploadError = ""
            scope.launch {
                try {
                    profileImageUrl = com.example.data.FirestoreSync.uploadProfileImage(context, uri)
                } catch (e: Exception) {
                    android.util.Log.e("ProCoach", "Trainer photo upload failed: ${e.message}", e)
                    uploadError = e.message ?: "Profile photo upload failed"
                } finally {
                    uploadingProfile = false
                }
            }
        }
    }

    // Portfolio photo picker
    val portfolioPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val idx = if (pendingSlot >= 0) pendingSlot else if (portfolioImageUrls.size < 3) portfolioImageUrls.size else 2
            uploadingPortfolio = idx
            uploadError = ""
            scope.launch {
                try {
                    val url = com.example.data.FirestoreSync.uploadPortfolioImage(context, uri, idx)
                    val updated = portfolioImageUrls.toMutableList()
                    if (idx < updated.size) updated[idx] = url else updated.add(url)
                    portfolioImageUrls = updated
                } catch (e: Exception) {
                    android.util.Log.e("ProCoach", "Portfolio upload failed: ${e.message}", e)
                    uploadError = e.message ?: "Portfolio upload failed"
                } finally {
                    uploadingPortfolio = -1
                    pendingSlot = -1
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = CyberBgCard,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Marketplace Profile", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = CyberTextPrimary)

            // ── Public toggle ─────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isPublic) CyberSuccess.copy(0.10f) else CyberBgCardElevated)
                    .border(1.dp, if (isPublic) CyberSuccess.copy(0.3f) else Color.White.copy(0.08f), RoundedCornerShape(16.dp))
                    .clickable { isPublic = !isPublic }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        if (isPublic) "Visible to clients" else "Profile is hidden",
                        fontSize = 14.sp, fontWeight = FontWeight.Bold,
                        color = if (isPublic) CyberSuccess else CyberTextMuted
                    )
                    Text(
                        if (isPublic) "Clients can discover and book you" else "Tap to go public",
                        fontSize = 12.sp, color = CyberTextMuted
                    )
                }
                Box(
                    modifier = Modifier
                        .size(width = 48.dp, height = 28.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (isPublic) CyberSuccess else CyberBgCard),
                    contentAlignment = if (isPublic) Alignment.CenterEnd else Alignment.CenterStart
                ) {
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(20.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color.White)
                    )
                }
            }

            if (isPublic) {
                // ── Profile photo ─────────────────────────────────────────────
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Profile Photo", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = CyberTextMuted)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Avatar preview
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(CyberBgCardElevated)
                                .border(2.dp, if (profileImageUrl.isNotBlank()) CyberAccent.copy(0.4f) else Color.White.copy(0.08f),
                                    CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (profileImageUrl.isNotBlank()) {
                                coil.compose.AsyncImage(
                                    model = profileImageUrl,
                                    contentDescription = "Profile",
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                                )
                            } else if (uploadingProfile) {
                                androidx.compose.material3.CircularProgressIndicator(
                                    color = CyberAccent, modifier = Modifier.size(24.dp), strokeWidth = 2.dp
                                )
                            } else {
                                Text("📷", fontSize = 24.sp)
                            }
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(CyberAccent.copy(0.12f))
                                    .border(1.dp, CyberAccent.copy(0.3f), RoundedCornerShape(10.dp))
                                    .clickable(enabled = !uploadingProfile) { profilePicker.launch("image/*") }
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    if (profileImageUrl.isBlank()) "Upload Photo" else "Change Photo",
                                    fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = CyberAccent
                                )
                            }
                            if (profileImageUrl.isNotBlank()) {
                                Text("✓ Photo uploaded", fontSize = 11.sp, color = CyberSuccess)
                            }
                        }
                    }
                }

                // ── Portfolio images ──────────────────────────────────────────
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Work Photos (up to 3)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = CyberTextMuted)
                        Text("${portfolioImageUrls.size}/3", fontSize = 11.sp, color = CyberAccent, fontWeight = FontWeight.Bold)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        repeat(3) { idx ->
                            val url = portfolioImageUrls.getOrNull(idx)
                            val isUploading = uploadingPortfolio == idx
                            Box(
                                modifier = Modifier
                                    .size(88.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(CyberBgCardElevated)
                                    .border(1.dp,
                                        if (url != null) CyberAccent.copy(0.3f) else Color.White.copy(0.06f),
                                        RoundedCornerShape(14.dp))
                                    .clickable(enabled = !isUploading && uploadingPortfolio == -1 && !uploadingProfile) {
                                        pendingSlot = idx
                                        portfolioPicker.launch("image/*")
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                when {
                                    isUploading -> androidx.compose.material3.CircularProgressIndicator(
                                        color = CyberAccent, modifier = Modifier.size(22.dp), strokeWidth = 2.dp
                                    )
                                    url != null -> coil.compose.AsyncImage(
                                        model = url,
                                        contentDescription = "Portfolio $idx",
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp))
                                    )
                                    else -> Text("+", fontSize = 22.sp, color = CyberTextMuted, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // ── Location search bar ───────────────────────────────────────
                Text("Your Location *", fontSize = 13.sp, color = CyberTextMuted, fontWeight = FontWeight.SemiBold)
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Search field
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(if (isLocationFocused)
                                RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)
                            else RoundedCornerShape(14.dp))
                            .background(CyberBgCardElevated)
                            .border(
                                1.dp,
                                if (isLocationFocused) CyberAccent.copy(0.5f) else Color.White.copy(0.1f),
                                if (isLocationFocused)
                                    RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)
                                else RoundedCornerShape(14.dp)
                            )
                            .padding(horizontal = 14.dp, vertical = 13.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            androidx.compose.material3.Icon(
                                Icons.Filled.LocationOn, null,
                                tint = if (gpsLocated) CyberAccent else CyberTextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                            BasicTextField(
                                value = city,
                                onValueChange = {
                                    city = it
                                    gpsLocated = false
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .onFocusChanged { state ->
                                        if (state.isFocused && !isLocationFocused) {
                                            isLocationFocused = true
                                            if (nearbyAreas.isEmpty() && !isFetchingNearby) {
                                                val fine = androidx.core.content.ContextCompat.checkSelfPermission(
                                                    context, android.Manifest.permission.ACCESS_FINE_LOCATION)
                                                val coarse = androidx.core.content.ContextCompat.checkSelfPermission(
                                                    context, android.Manifest.permission.ACCESS_COARSE_LOCATION)
                                                if (fine == android.content.pm.PackageManager.PERMISSION_GRANTED ||
                                                    coarse == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                                    isFetchingNearby = true
                                                    scope.launch {
                                                        val coords = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                                            com.example.data.GeoUtils.getDeviceLocation(context)
                                                        }
                                                        if (coords != null) {
                                                            val areas = com.example.data.GeoUtils.getNearbyAreas(context, coords.first, coords.second)
                                                            nearbyAreas = areas
                                                            if (!gpsLocated && city.isBlank()) {
                                                                val label = areas.firstOrNull()?.name
                                                                    ?: com.example.data.GeoUtils.reverseGeocode(context, coords.first, coords.second)
                                                                // don't auto-fill — let user choose
                                                            }
                                                            gpsLat = coords.first
                                                            gpsLng = coords.second
                                                        }
                                                        isFetchingNearby = false
                                                    }
                                                }
                                            }
                                        } else if (!state.isFocused) {
                                            isLocationFocused = false
                                        }
                                    },
                                textStyle = UiTextStyle(
                                    color = CyberTextPrimary, fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                cursorBrush = SolidColor(CyberAccent),
                                singleLine = true,
                                decorationBox = { inner ->
                                    if (city.isEmpty()) Text("City or area (e.g. Koramangala)…",
                                        fontSize = 14.sp, color = CyberTextMuted)
                                    inner()
                                }
                            )
                            if (gpsLocated) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(CyberAccent.copy(0.15f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("GPS", fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = CyberAccent)
                                }
                            }
                        }
                    }

                    // Dropdown with "Use Current Location" + nearby suggestions
                    AnimatedVisibility(
                        visible = isLocationFocused,
                        enter = expandVertically() + fadeIn(),
                        exit  = shrinkVertically() + fadeOut()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(
                                    bottomStart = 14.dp, bottomEnd = 14.dp))
                                .background(CyberBgCardElevated)
                                .border(1.dp, CyberAccent.copy(0.3f),
                                    RoundedCornerShape(
                                        bottomStart = 14.dp, bottomEnd = 14.dp))
                        ) {
                            // Use Current Location row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        isLocationFocused = false
                                        useGpsLocation()
                                    }
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier.size(32.dp)
                                        .clip(CircleShape)
                                        .background(CyberAccent.copy(0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isGpsLocating) {
                                        androidx.compose.material3.CircularProgressIndicator(
                                            color = CyberAccent, modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                    } else {
                                        androidx.compose.material3.Icon(
                                            Icons.Filled.MyLocation, null,
                                            tint = CyberAccent, modifier = Modifier.size(15.dp))
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Use Current Location",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CyberAccent)
                                    Text("Detect your exact location",
                                        fontSize = 11.sp, color = CyberTextMuted)
                                }
                            }

                            // Nearby suggestions
                            if (isFetchingNearby) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    androidx.compose.material3.CircularProgressIndicator(
                                        color = CyberAccent, modifier = Modifier.size(12.dp), strokeWidth = 2.dp)
                                    Text("Finding nearby areas…", fontSize = 11.sp, color = CyberTextMuted)
                                }
                            } else {
                                val filtered = if (city.isBlank()) nearbyAreas
                                    else nearbyAreas.filter {
                                        it.name.contains(city, ignoreCase = true) ||
                                        it.cityName.contains(city, ignoreCase = true)
                                    }
                                if (filtered.isNotEmpty()) {
                                    androidx.compose.material3.HorizontalDivider(color = Color.White.copy(0.06f))
                                    Text("NEARBY AREAS", fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = CyberTextMuted,
                                        modifier = Modifier.padding(start = 14.dp, top = 8.dp, bottom = 2.dp))
                                    filtered.forEach { area ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    city = area.name
                                                    gpsLat = area.lat
                                                    gpsLng = area.lng
                                                    gpsLocated = true
                                                    isLocationFocused = false
                                                }
                                                .padding(horizontal = 14.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            androidx.compose.material3.Icon(
                                                Icons.Filled.LocationOn, null,
                                                tint = CyberTextMuted, modifier = Modifier.size(15.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(area.name, fontSize = 13.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = CyberTextPrimary)
                                                if (area.cityName.isNotBlank() && area.cityName != area.name) {
                                                    Text(area.cityName, fontSize = 11.sp, color = CyberTextMuted)
                                                }
                                            }
                                            Text("%.1f km".format(area.distKm),
                                                fontSize = 10.sp, color = CyberTextMuted)
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                        }
                    }

                    // GPS confirmed banner (below dropdown when not focused)
                    if (gpsLocated && !isLocationFocused) {
                        Spacer(Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(CyberAccent.copy(0.1f))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("✓", fontSize = 11.sp, color = CyberAccent)
                            Text("GPS location pinned — members near you will find you first",
                                fontSize = 11.sp, color = CyberAccent)
                        }
                    }
                }

                SheetTextField("Bio / About (optional)", bio, KeyboardType.Text) { bio = it }
                SheetTextField("Work Description (services, training style, achievements…)", workDescription, KeyboardType.Text) { workDescription = it }
                SheetTextField("Per Session Fee (₹) *", feeSessionText, KeyboardType.Number) { feeSessionText = it }
                SheetTextField("Monthly Fee (₹, optional)", feeMonthlyText, KeyboardType.Number) { feeMonthlyText = it }
                SheetTextField("Years of Experience", yearsExpText, KeyboardType.Number) { yearsExpText = it }

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Available Days", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = CyberTextMuted)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        allDays.forEach { day ->
                            val sel = day in selectedDays
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (sel) CyberAccent else CyberBgCardElevated)
                                    .border(1.dp, if (sel) CyberAccent else Color.White.copy(0.06f), RoundedCornerShape(10.dp))
                                    .clickable { selectedDays = if (sel) selectedDays - day else selectedDays + day }
                                    .padding(horizontal = 8.dp, vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    day.take(1), fontSize = 12.sp, fontWeight = FontWeight.ExtraBold,
                                    color = if (sel) CyberAccentDark else CyberTextSecondary
                                )
                            }
                        }
                    }
                }
            }

            // Upload error
            if (uploadError.isNotBlank()) {
                Text(uploadError, fontSize = 12.sp, color = CyberDanger)
            }

            val isBusy = uploadingProfile || uploadingPortfolio != -1
            val canSave = !isBusy && (!isPublic || (city.isNotBlank() && feeSessionText.isNotBlank()))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (canSave) CyberAccent else CyberBgCardElevated)
                    .clickable(enabled = canSave) {
                        scope.launch {
                            // Use pinned GPS coordinates; fall back to geocoding the typed city
                            val (lat, lng) = if (gpsLocated && gpsLat != 0.0) {
                                gpsLat to gpsLng
                            } else {
                                val coords = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                    com.example.data.GeoUtils.geocodeCity(context, city.trim())
                                }
                                (coords?.first ?: 0.0) to (coords?.second ?: 0.0)
                            }
                            onSave(
                                isPublic, city.trim(), bio.trim(), workDescription.trim(),
                                feeSessionText.toIntOrNull() ?: 0,
                                feeMonthlyText.toIntOrNull() ?: 0,
                                selectedDays.sortedBy { allDays.indexOf(it) }.joinToString(","),
                                yearsExpText.toIntOrNull() ?: 0,
                                profileImageUrl,
                                portfolioImageUrls.joinToString(","),
                                lat, lng
                            )
                        }
                    }
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isBusy) {
                    androidx.compose.material3.CircularProgressIndicator(
                        color = CyberAccentDark, modifier = Modifier.size(18.dp), strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        if (isPublic) "Publish Profile" else "Save",
                        fontSize = 15.sp, fontWeight = FontWeight.ExtraBold,
                        color = if (canSave) CyberAccentDark else CyberTextMuted
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

// ─── Missed Session Sheet ─────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MissedSessionSheet(
    clientName: String,
    onDismiss: () -> Unit,
    onConfirm: (reason: String) -> Unit
) {
    val reasons = listOf("Sick / Injury", "Work / Busy", "Travel", "Personal", "No energy", "Other")
    var selectedReason by remember { mutableStateOf("") }
    var customNote by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = CyberBgCard,
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(CyberDanger.copy(0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("❌", fontSize = 16.sp)
                }
                Column {
                    Text("Mark Session Missed", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = CyberTextPrimary)
                    Text(clientName, fontSize = 13.sp, color = CyberTextMuted)
                }
            }
            Spacer(Modifier.height(2.dp))
            Text("Reason", fontSize = 13.sp, color = CyberTextMuted, fontWeight = FontWeight.SemiBold)
            val rows = reasons.chunked(3)
            rows.forEach { rowItems ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    rowItems.forEach { r ->
                        val sel = r == selectedReason
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (sel) CyberDanger.copy(0.15f) else CyberBgCardElevated)
                                .border(1.dp, if (sel) CyberDanger.copy(0.5f) else Color.Transparent, RoundedCornerShape(12.dp))
                                .clickable { selectedReason = if (sel) "" else r }
                                .padding(horizontal = 8.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(r, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                                color = if (sel) CyberDanger else CyberTextSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                    }
                }
            }
            SheetTextField("Additional note (optional)", customNote, KeyboardType.Text) { customNote = it }
            val finalReason = when {
                selectedReason.isNotBlank() && customNote.isNotBlank() -> "$selectedReason — $customNote"
                selectedReason.isNotBlank() -> selectedReason
                customNote.isNotBlank() -> customNote
                else -> "No reason given"
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(999.dp))
                    .background(CyberDanger)
                    .clickable { onConfirm(finalReason) }
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Text("Mark as Missed", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

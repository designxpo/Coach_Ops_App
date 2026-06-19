package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.UserBodyStats
import com.example.ui.theme.*

@Composable
fun BodyMeasurementsScreen(viewModel: HealthViewModel, onBack: () -> Unit) {
    val measurements by viewModel.measurements.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    var showDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBgPrimary)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape)
                    .background(CyberBgCard).clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = CyberTextPrimary, modifier = Modifier.size(20.dp))
            }
            Column(Modifier.weight(1f)) {
                Text("Body Measurements", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = CyberTextPrimary)
                Text("Track your body changes over time", fontSize = 12.sp, color = CyberTextMuted)
            }
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape)
                    .background(CyberAccent).clickable { showDialog = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Add, null, tint = CyberAccentDark, modifier = Modifier.size(20.dp))
            }
        }

        if (measurements.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("📏", fontSize = 48.sp)
                    Text("No measurements yet", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = CyberTextSecondary)
                    Text("Tap + to log your first measurement", fontSize = 13.sp, color = CyberTextMuted)
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(measurements) { m ->
                    MeasurementCard(m, onDelete = { viewModel.deleteMeasurement(m.id) })
                }
            }
        }
    }

    if (showDialog) {
        LogMeasurementDialog(
            onDismiss = { showDialog = false },
            onSave = { m ->
                viewModel.saveMeasurement(m)
                showDialog = false
            },
            isSaving = isSaving
        )
    }
}

@Composable
private fun MeasurementCard(m: UserBodyStats, onDelete: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CyberBgCard)
            .border(1.dp, Color.White.copy(0.07f), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(m.date, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = CyberAccent)
            Icon(Icons.Filled.Delete, null, tint = CyberDanger.copy(0.6f),
                modifier = Modifier.size(18.dp).clickable { onDelete() })
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (m.weightKg > 0) MeasureBadge("⚖️ Weight", "${m.weightKg}kg", Modifier.weight(1f))
            if (m.chestCm > 0)  MeasureBadge("💪 Chest",  "${m.chestCm}cm",  Modifier.weight(1f))
            if (m.waistCm > 0)  MeasureBadge("📐 Waist",  "${m.waistCm}cm",  Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (m.hipsCm > 0)   MeasureBadge("🫀 Hips",   "${m.hipsCm}cm",   Modifier.weight(1f))
            if (m.armsCm > 0)   MeasureBadge("💪 Arms",   "${m.armsCm}cm",   Modifier.weight(1f))
            if (m.thighsCm > 0) MeasureBadge("🦵 Thighs", "${m.thighsCm}cm", Modifier.weight(1f))
        }
        if (m.notes.isNotBlank()) {
            Text(m.notes, fontSize = 12.sp, color = CyberTextMuted)
        }
    }
}

@Composable
private fun MeasureBadge(label: String, value: String, modifier: Modifier) {
    Column(
        modifier = modifier.clip(RoundedCornerShape(10.dp))
            .background(CyberBgCardElevated).padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = CyberTextPrimary)
        Text(label, fontSize = 10.sp, color = CyberTextMuted)
    }
}

@Composable
private fun LogMeasurementDialog(
    onDismiss: () -> Unit,
    onSave: (UserBodyStats) -> Unit,
    isSaving: Boolean
) {
    var weight by remember { mutableStateOf("") }
    var chest  by remember { mutableStateOf("") }
    var waist  by remember { mutableStateOf("") }
    var hips   by remember { mutableStateOf("") }
    var arms   by remember { mutableStateOf("") }
    var thighs by remember { mutableStateOf("") }
    var notes  by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(CyberBgCard)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Log Measurements", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = CyberTextPrimary)

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MeasureField("Weight (kg)", weight, { weight = it }, Modifier.weight(1f))
                MeasureField("Chest (cm)",  chest,  { chest  = it }, Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MeasureField("Waist (cm)", waist, { waist = it }, Modifier.weight(1f))
                MeasureField("Hips (cm)",  hips,  { hips  = it }, Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MeasureField("Arms (cm)",   arms,   { arms   = it }, Modifier.weight(1f))
                MeasureField("Thighs (cm)", thighs, { thighs = it }, Modifier.weight(1f))
            }

            OutlinedTextField(
                value = notes, onValueChange = { notes = it },
                label = { Text("Notes (optional)", color = CyberTextMuted, fontSize = 12.sp) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyberAccent, unfocusedBorderColor = Color.White.copy(0.15f),
                    focusedTextColor = CyberTextPrimary, unfocusedTextColor = CyberTextPrimary
                )
            )

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text("Cancel", color = CyberTextMuted)
                }
                Button(
                    onClick = {
                        onSave(UserBodyStats(
                            weightKg = weight.toFloatOrNull() ?: 0f,
                            chestCm  = chest.toFloatOrNull()  ?: 0f,
                            waistCm  = waist.toFloatOrNull()  ?: 0f,
                            hipsCm   = hips.toFloatOrNull()   ?: 0f,
                            armsCm   = arms.toFloatOrNull()   ?: 0f,
                            thighsCm = thighs.toFloatOrNull() ?: 0f,
                            notes    = notes
                        ))
                    },
                    enabled  = !isSaving,
                    modifier = Modifier.weight(1f),
                    colors   = ButtonDefaults.buttonColors(containerColor = CyberAccent),
                    shape    = RoundedCornerShape(12.dp)
                ) {
                    if (isSaving) CircularProgressIndicator(modifier = Modifier.size(18.dp), color = CyberAccentDark)
                    else Text("Save", color = CyberAccentDark, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun MeasureField(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier) {
    OutlinedTextField(
        value = value, onValueChange = onChange,
        label = { Text(label, color = CyberTextMuted, fontSize = 11.sp) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier,
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = CyberAccent, unfocusedBorderColor = Color.White.copy(0.15f),
            focusedTextColor = CyberTextPrimary, unfocusedTextColor = CyberTextPrimary
        )
    )
}

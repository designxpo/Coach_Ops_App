package com.example.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.GymPlan
import com.example.ui.theme.CyberAccent
import com.example.ui.theme.CyberAccentDark
import com.example.ui.theme.CyberBgCard
import com.example.ui.theme.CyberBgCardElevated
import com.example.ui.theme.CyberBgPrimary
import com.example.ui.theme.CyberDanger
import com.example.ui.theme.CyberTextMuted
import com.example.ui.theme.CyberTextPrimary
import com.example.ui.theme.CyberTextSecondary

@Composable
fun GymPlansScreen(viewModel: GymViewModel, onBack: () -> Unit) {
    val plans by viewModel.plans.collectAsStateWithLifecycle()

    var editingPlan by remember { mutableStateOf<GymPlan?>(null) }
    var showSheet by remember { mutableStateOf(false) }
    var deletingPlan by remember { mutableStateOf<GymPlan?>(null) }

    if (showSheet) {
        GymPlanSheet(
            existing = editingPlan,
            onDismiss = { showSheet = false; editingPlan = null },
            onSave = { name, days, price, desc ->
                viewModel.savePlan(editingPlan, name, days, price, desc)
                showSheet = false
                editingPlan = null
            }
        )
    }

    deletingPlan?.let { plan ->
        AlertDialog(
            onDismissRequest = { deletingPlan = null },
            containerColor = CyberBgCardElevated,
            title = { Text("Remove \"${plan.name}\"?", color = CyberTextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("Existing members keep their validity. The plan just won't be offered for new payments.", color = CyberTextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deactivatePlan(plan)
                    deletingPlan = null
                }) { Text("Remove", color = CyberDanger, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { deletingPlan = null }) { Text("Cancel", color = CyberTextMuted) }
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(CyberBgPrimary),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(CyberBgCard).clickable { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back",
                        tint = CyberTextPrimary, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(12.dp))
                Text("Membership Plans", fontSize = 20.sp, fontWeight = FontWeight.Bold,
                    color = CyberTextPrimary, modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(CyberAccent)
                        .clickable { editingPlan = null; showSheet = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add plan", tint = CyberAccentDark, modifier = Modifier.size(20.dp))
                }
            }
        }

        item {
            Text("Fees your members pay. Tap a plan to edit its price.", fontSize = 13.sp, color = CyberTextMuted)
        }

        if (plans.isEmpty()) {
            item {
                Column(
                    Modifier.fillMaxWidth().padding(vertical = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("📋", fontSize = 40.sp)
                    Spacer(Modifier.height(12.dp))
                    Text("No plans yet", color = CyberTextSecondary, fontSize = 14.sp)
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { editingPlan = null; showSheet = true },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberAccent),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("+ Create Plan", fontWeight = FontWeight.Bold, color = CyberAccentDark)
                    }
                }
            }
        }

        items(plans, key = { it.id }) { plan ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(CyberBgCard)
                    .clickable { editingPlan = plan; showSheet = true }
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(Modifier.weight(1f)) {
                    Text(plan.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = CyberTextPrimary)
                    Text(
                        "${plan.durationDays} days" + if (plan.description.isNotEmpty()) " · ${plan.description}" else "",
                        fontSize = 12.sp, color = CyberTextMuted
                    )
                }
                Text("₹${"%,d".format(plan.priceInr)}", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = CyberAccent)
                Box(
                    modifier = Modifier.size(32.dp).clip(CircleShape).background(CyberBgCardElevated)
                        .clickable { deletingPlan = plan },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = "Remove", tint = CyberDanger, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GymPlanSheet(
    existing: GymPlan?,
    onDismiss: () -> Unit,
    onSave: (name: String, durationDays: Int, priceInr: Int, description: String) -> Unit
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var daysText by remember { mutableStateOf(existing?.durationDays?.toString() ?: "30") }
    var priceText by remember { mutableStateOf(existing?.priceInr?.toString() ?: "") }
    var description by remember { mutableStateOf(existing?.description ?: "") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = CyberBgCardElevated
    ) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
            Text(if (existing == null) "New Plan" else "Edit Plan",
                fontSize = 20.sp, fontWeight = FontWeight.Bold, color = CyberTextPrimary)
            Spacer(Modifier.height(20.dp))

            GymTextField(name, { name = it }, "Plan name *", "e.g. Monthly, Couple Annual")
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.weight(1f)) {
                    GymTextField(daysText, { daysText = it.filter { c -> c.isDigit() }.take(4) }, "Duration (days) *",
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                }
                Box(Modifier.weight(1f)) {
                    GymTextField(priceText, { priceText = it.filter { c -> c.isDigit() }.take(7) }, "Price (₹) *",
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                }
            }
            Spacer(Modifier.height(12.dp))
            GymTextField(description, { description = it }, "Tagline (optional)", "e.g. Best value — save ₹4,400")
            Spacer(Modifier.height(24.dp))

            val days = daysText.toIntOrNull() ?: 0
            val price = priceText.toIntOrNull() ?: 0
            val valid = name.isNotBlank() && days > 0 && price > 0
            Button(
                onClick = { if (valid) onSave(name, days, price, description) },
                enabled = valid,
                colors = ButtonDefaults.buttonColors(containerColor = CyberAccent, disabledContainerColor = CyberBgCard),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text(if (existing == null) "Create Plan" else "Save Changes",
                    fontWeight = FontWeight.Bold, fontSize = 15.sp,
                    color = if (valid) CyberAccentDark else CyberTextMuted)
            }
        }
    }
}

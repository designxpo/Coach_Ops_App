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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.GymMember
import com.example.data.GymPlan
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

@Composable
fun GymMembersScreen(
    viewModel: GymViewModel,
    onBack: () -> Unit,
    onMemberClick: (String) -> Unit
) {
    val members by viewModel.members.collectAsStateWithLifecycle()
    val plans by viewModel.plans.collectAsStateWithLifecycle()

    val context = androidx.compose.ui.platform.LocalContext.current
    val snackbarMsg by viewModel.snackbar.collectAsStateWithLifecycle()
    androidx.compose.runtime.LaunchedEffect(snackbarMsg) {
        if (snackbarMsg.isNotEmpty()) {
            android.widget.Toast.makeText(context, snackbarMsg, android.widget.Toast.LENGTH_SHORT).show()
            viewModel.clearSnackbar()
        }
    }

    var searchQuery by remember { mutableStateOf("") }
    var activeFilter by remember { mutableStateOf("All") }
    var showAddSheet by remember { mutableStateOf(false) }
    val filters = listOf("All", "Active", "Expiring", "Expired", "Frozen")

    val filtered = members.filter { m ->
        val matchesSearch = searchQuery.isEmpty() ||
            m.name.contains(searchQuery, ignoreCase = true) ||
            m.phone.contains(searchQuery)
        val matchesFilter = when (activeFilter) {
            "Active"   -> m.status == "ACTIVE" && !m.isExpired
            "Expiring" -> m.expiringSoon && m.status == "ACTIVE"
            "Expired"  -> m.isExpired || m.status == "EXPIRED"
            "Frozen"   -> m.status == "FROZEN"
            else       -> true
        }
        matchesSearch && matchesFilter
    }

    if (showAddSheet) {
        GymMemberSheet(
            plans = plans,
            onDismiss = { showAddSheet = false },
            onSave = { name, phone, gender, plan, notes, joinDate, collectPayment, amount, method ->
                viewModel.addMember(name, phone, gender, plan, notes, joinDate, collectPayment, amount, method)
                showAddSheet = false
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
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(CyberBgCard)
                        .clickable { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back",
                        tint = CyberTextPrimary, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(12.dp))
                Text("Gym Members", fontSize = 20.sp, fontWeight = FontWeight.Bold,
                    color = CyberTextPrimary, modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(CyberAccent)
                        .clickable { showAddSheet = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add member",
                        tint = CyberAccentDark, modifier = Modifier.size(20.dp))
                }
            }
        }

        // Search
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search name or phone…", color = CyberTextMuted, fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = CyberTextMuted, modifier = Modifier.size(18.dp)) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = CyberTextPrimary,
                    unfocusedTextColor = CyberTextPrimary,
                    focusedBorderColor = CyberAccent,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                    cursorColor = CyberAccent,
                    focusedContainerColor = CyberBgCard,
                    unfocusedContainerColor = CyberBgCard
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Filter chips
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filters) { filter ->
                    val isActive = filter == activeFilter
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (isActive) CyberAccent.copy(alpha = 0.12f) else CyberBgCard)
                            .clickable { activeFilter = filter }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(filter, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                            color = if (isActive) CyberAccent else CyberTextSecondary)
                    }
                }
            }
        }

        item {
            Text(
                "${filtered.size} member${if (filtered.size == 1) "" else "s"}",
                fontSize = 13.sp, color = CyberTextMuted
            )
        }

        if (filtered.isEmpty()) {
            item {
                Column(
                    Modifier.fillMaxWidth().padding(vertical = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("👥", fontSize = 40.sp)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        if (members.isEmpty()) "Add your first member to get started"
                        else "No members match this filter",
                        color = CyberTextSecondary, fontSize = 14.sp
                    )
                    if (members.isEmpty()) {
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { showAddSheet = true },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberAccent),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("+ Add Member", fontWeight = FontWeight.Bold, color = CyberAccentDark)
                        }
                    }
                }
            }
        }

        items(filtered, key = { it.id }) { member ->
            GymMemberCard(member = member, onClick = { onMemberClick(member.id) })
        }
    }
}

@Composable
fun GymMemberCard(member: GymMember, onClick: () -> Unit) {
    val (statusColor, statusText) = when {
        member.status == "FROZEN" -> CyberTextMuted to "Frozen"
        member.isExpired          -> CyberDanger to "Expired ${-member.daysLeft}d ago"
        member.planEndMillis == 0L -> CyberTextMuted to "No plan"
        member.expiringSoon       -> CyberWarning to "${member.daysLeft}d left"
        else                      -> CyberSuccess to "${member.daysLeft}d left"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(CyberBgCard)
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Avatar with initials
        Box(
            modifier = Modifier.size(44.dp).clip(CircleShape).background(CyberAccent.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                member.name.split(" ").take(2).mapNotNull { it.firstOrNull()?.uppercase() }.joinToString(""),
                fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = CyberAccent
            )
        }
        Column(Modifier.weight(1f)) {
            Text(member.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = CyberTextPrimary)
            Text(member.planName.ifEmpty { "No active plan" }, fontSize = 12.sp, color = CyberTextSecondary)
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(statusColor.copy(alpha = 0.14f))
                .padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Text(statusText, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = statusColor)
        }
    }
}

// ─── Add / edit member sheet ──────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GymMemberSheet(
    plans: List<GymPlan>,
    existing: GymMember? = null,
    onDismiss: () -> Unit,
    onSave: (name: String, phone: String, gender: String, plan: GymPlan?, notes: String,
             joinDateMillis: Long, collectPayment: Boolean, amountInr: Int, method: String) -> Unit
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var phone by remember { mutableStateOf(existing?.phone ?: "") }
    var gender by remember { mutableStateOf(existing?.gender ?: "") }
    var notes by remember { mutableStateOf(existing?.notes ?: "") }
    var selectedPlan by remember { mutableStateOf<GymPlan?>(null) }

    // Joining date — plan validity & fee schedule auto-calculate from this
    var joinDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var collectPayment by remember { mutableStateOf(true) }
    var amountText by remember { mutableStateOf("") }
    var payMethod by remember { mutableStateOf("UPI") }
    val dateFmt = remember { java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()) }

    if (showDatePicker) {
        val dateState = androidx.compose.material3.rememberDatePickerState(initialSelectedDateMillis = joinDateMillis)
        androidx.compose.material3.DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    dateState.selectedDateMillis?.let { joinDateMillis = it }
                    showDatePicker = false
                }) { Text("OK", color = CyberAccent, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", color = CyberTextMuted)
                }
            }
        ) {
            androidx.compose.material3.DatePicker(state = dateState, showModeToggle = false)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = CyberBgCardElevated
    ) {
        LazyColumn(
            modifier = Modifier.padding(horizontal = 24.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                Text(
                    if (existing == null) "Add Member" else "Edit Member",
                    fontSize = 20.sp, fontWeight = FontWeight.Bold, color = CyberTextPrimary
                )
                Spacer(Modifier.height(20.dp))
                GymTextField(name, { name = it }, "Full name *", "e.g. Rahul Sharma")
                Spacer(Modifier.height(12.dp))
                GymTextField(phone, { phone = it.filter { c -> c.isDigit() }.take(10) }, "Phone (WhatsApp)", "10-digit mobile",
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone)
                Spacer(Modifier.height(12.dp))

                // Gender chips
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("MALE" to "Male", "FEMALE" to "Female", "OTHER" to "Other").forEach { (key, label) ->
                        val active = gender == key
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(if (active) CyberAccent.copy(alpha = 0.15f) else CyberBgCard)
                                .clickable { gender = if (active) "" else key }
                                .padding(horizontal = 16.dp, vertical = 9.dp)
                        ) {
                            Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                                color = if (active) CyberAccent else CyberTextSecondary)
                        }
                    }
                }

                // Joining date + plan selector — only when adding (renewals via Collect Fee)
                if (existing == null) {
                    Spacer(Modifier.height(18.dp))
                    Text("Joining date", fontSize = 13.sp, color = CyberTextMuted)
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(CyberBgCard)
                            .clickable { showDatePicker = true }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("📅", fontSize = 16.sp)
                        Spacer(Modifier.width(10.dp))
                        Text(dateFmt.format(java.util.Date(joinDateMillis)),
                            fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                            color = CyberTextPrimary, modifier = Modifier.weight(1f))
                        Text("Change", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CyberAccent)
                    }
                }

                if (existing == null && plans.isNotEmpty()) {
                    Spacer(Modifier.height(18.dp))
                    Text("Membership plan (optional)", fontSize = 13.sp, color = CyberTextMuted)
                    Spacer(Modifier.height(8.dp))
                    plans.forEach { plan ->
                        val active = selectedPlan?.id == plan.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (active) CyberAccent.copy(alpha = 0.12f) else CyberBgCard)
                                .clickable {
                                    selectedPlan = if (active) null else plan
                                    amountText = if (active) "" else plan.priceInr.toString()
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(plan.name, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                                    color = if (active) CyberAccent else CyberTextPrimary)
                                Text("${plan.durationDays} days", fontSize = 11.sp, color = CyberTextMuted)
                            }
                            Text("₹${"%,d".format(plan.priceInr)}", fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (active) CyberAccent else CyberTextSecondary)
                        }
                    }

                    // Auto fee collection — validity computed from joining date
                    selectedPlan?.let { plan ->
                        val validTill = joinDateMillis + plan.durationDays * 86400000L
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "✅ Valid ${dateFmt.format(java.util.Date(joinDateMillis))} → ${dateFmt.format(java.util.Date(validTill))} · next fee auto-reminded",
                            fontSize = 11.sp, color = CyberSuccess, lineHeight = 16.sp
                        )
                        Spacer(Modifier.height(14.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (collectPayment) CyberAccent.copy(alpha = 0.10f) else CyberBgCard)
                                .clickable { collectPayment = !collectPayment }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(if (collectPayment) "☑" else "☐", fontSize = 18.sp,
                                color = if (collectPayment) CyberAccent else CyberTextMuted)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Collect joining fee now", fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                                    color = CyberTextPrimary)
                                Text("Receipt is generated automatically", fontSize = 11.sp, color = CyberTextMuted)
                            }
                        }
                        if (collectPayment) {
                            Spacer(Modifier.height(12.dp))
                            GymTextField(
                                amountText,
                                { amountText = it.filter { c -> c.isDigit() }.take(7) },
                                "Amount (₹)",
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                            )
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("UPI" to "📱 UPI", "CASH" to "💵 Cash", "CARD" to "💳 Card").forEach { (key, label) ->
                                    val active = payMethod == key
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(999.dp))
                                            .background(if (active) CyberAccent.copy(alpha = 0.15f) else CyberBgCard)
                                            .clickable { payMethod = key }
                                            .padding(horizontal = 14.dp, vertical = 9.dp)
                                    ) {
                                        Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                                            color = if (active) CyberAccent else CyberTextSecondary)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                GymTextField(notes, { notes = it }, "Notes (optional)", "Injuries, goals, referrals…")
                Spacer(Modifier.height(24.dp))

                val payAmount = amountText.toIntOrNull() ?: 0
                val payValid = !collectPayment || selectedPlan == null || payAmount > 0
                Button(
                    onClick = {
                        if (name.isNotBlank() && payValid) onSave(
                            name, phone, gender, selectedPlan, notes,
                            joinDateMillis, collectPayment && selectedPlan != null, payAmount, payMethod
                        )
                    },
                    enabled = name.isNotBlank() && payValid,
                    colors = ButtonDefaults.buttonColors(containerColor = CyberAccent, disabledContainerColor = CyberBgCard),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Text(
                        when {
                            existing != null -> "Save Changes"
                            collectPayment && selectedPlan != null && payAmount > 0 ->
                                "Add Member · Collect ₹${"%,d".format(payAmount)}"
                            else -> "Add Member"
                        },
                        fontWeight = FontWeight.Bold, fontSize = 15.sp,
                        color = if (name.isNotBlank() && payValid) CyberAccentDark else CyberTextMuted
                    )
                }
            }
        }
    }
}

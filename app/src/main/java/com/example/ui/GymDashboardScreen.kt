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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Storefront
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.GymMember
import com.example.data.PlanFeature
import com.example.data.has
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

/**
 * Gym Suite home — the gym owner's operations dashboard.
 * Three states:
 *   1. Locked   — plan doesn't include Gym Suite and trial is over → upsell
 *   2. Setup    — first run, gym profile not created yet
 *   3. Dashboard — KPIs, quick actions, expiring memberships
 */
@Composable
fun GymDashboardScreen(
    viewModel: GymViewModel,
    onMembersClick: () -> Unit,
    onMemberClick: (String) -> Unit,
    onPlansClick: () -> Unit,
    onAttendanceClick: () -> Unit,
    onUpgradeClick: () -> Unit
) {
    val entitlements by viewModel.entitlements.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val members by viewModel.members.collectAsStateWithLifecycle()

    var showSetupSheet by remember { mutableStateOf(false) }
    var setupDone by remember { mutableStateOf(viewModel.gymSetupComplete) }

    // ── State 1: locked ────────────────────────────────────────────────────────
    if (!entitlements.gymUnlocked) {
        GymLockedContent(
            trialUsed = entitlements.gymTrialUsed,
            onUpgradeClick = onUpgradeClick,
            onStartTrial = {
                // Trial only becomes available after gym profile setup
                showSetupSheet = true
            }
        )
        if (showSetupSheet) {
            GymSetupSheet(
                onDismiss = { showSetupSheet = false },
                onSave = { name, address, gstin ->
                    viewModel.saveGymProfile(name, address, gstin)
                    setupDone = true
                    showSetupSheet = false
                }
            )
        }
        return
    }

    // ── State 2: unlocked but gym profile missing ─────────────────────────────
    if (!setupDone) {
        GymSetupEmptyState(onSetupClick = { showSetupSheet = true })
        if (showSetupSheet) {
            GymSetupSheet(
                onDismiss = { showSetupSheet = false },
                onSave = { name, address, gstin ->
                    viewModel.saveGymProfile(name, address, gstin)
                    setupDone = true
                    showSetupSheet = false
                }
            )
        }
        return
    }

    // ── State 3: dashboard ────────────────────────────────────────────────────
    val expiringMembers = members
        .filter { (it.expiringSoon || it.isExpired) && it.status == "ACTIVE" }
        .sortedBy { it.planEndMillis }
        .take(8)

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(CyberBgPrimary),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(CyberBgCard),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Storefront, contentDescription = null, tint = CyberTextPrimary, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    viewModel.userPreferences.gymName.ifEmpty { "My Gym" },
                    fontSize = 20.sp, fontWeight = FontWeight.Bold, color = CyberTextPrimary,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Trial banner
        if (entitlements.gymTrialActive && !entitlements.plan.has(PlanFeature.GYM_SUITE)) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CyberWarning.copy(alpha = 0.12f))
                        .border(1.dp, CyberWarning.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                        .clickable { onUpgradeClick() }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("⏳", fontSize = 18.sp)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Free trial — ${entitlements.gymTrialDaysLeft} days left",
                            fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CyberWarning
                        )
                        Text("Upgrade to Business to keep the Gym Suite", fontSize = 11.sp, color = CyberTextSecondary)
                    }
                    Text("›", fontSize = 20.sp, color = CyberWarning, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Hero: this month's collections
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(CyberAccent)
                    .padding(20.dp)
            ) {
                Column {
                    Text("COLLECTED THIS MONTH", fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF0A0A0A).copy(alpha = 0.5f), letterSpacing = 0.8.sp)
                    Text(formatInr(stats.monthCollectedInr), fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold, color = CyberAccentDark, lineHeight = 36.sp)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BillingChip("${stats.activeMembers} Active", Color(0xFF166534))
                        BillingChip("${stats.expiringSoon} Expiring", Color(0xFF92400E))
                        BillingChip("${stats.expired} Expired", Color(0xFF991B1B))
                    }
                }
            }
        }

        // KPI row
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GymKpiCard(Modifier.weight(1f), "${stats.todayCheckIns}", "Check-ins today", Icons.Filled.CheckCircle, CyberSuccess)
                GymKpiCard(Modifier.weight(1f), "${stats.activeMembers}", "Active members", Icons.Filled.FitnessCenter, CyberAccent)
            }
        }

        // Quick actions
        item {
            Text("Quick Actions", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = CyberTextPrimary)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GymActionCard(Modifier.weight(1f), Icons.Filled.PersonAdd, "Members", onMembersClick)
                GymActionCard(Modifier.weight(1f), Icons.Filled.CheckCircle, "Attendance", onAttendanceClick)
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GymActionCard(Modifier.weight(1f), Icons.Filled.CalendarMonth, "Plans", onPlansClick)
                GymActionCard(Modifier.weight(1f), Icons.Filled.CurrencyRupee, "Collect Fees", onMembersClick)
            }
        }

        // Expiring memberships — the money list
        item {
            Text("Renewals Due", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = CyberTextPrimary,
                modifier = Modifier.padding(top = 4.dp))
        }
        if (expiringMembers.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(vertical = 20.dp), contentAlignment = Alignment.Center) {
                    Text("No renewals due — all memberships current 🎉", color = CyberTextSecondary, fontSize = 14.sp)
                }
            }
        } else {
            expiringMembers.forEach { member ->
                item(key = "exp_${member.id}") {
                    ExpiringMemberCard(member = member, onClick = { onMemberClick(member.id) })
                }
            }
        }
    }
}

// ─── Locked / upsell state ────────────────────────────────────────────────────

@Composable
fun GymLockedContent(trialUsed: Boolean, onUpgradeClick: () -> Unit, onStartTrial: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBgPrimary)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(48.dp))
        Text("🏢", fontSize = 56.sp)
        Spacer(Modifier.height(16.dp))
        Text("Run Your Gym From Your Pocket", fontSize = 22.sp, fontWeight = FontWeight.Black,
            color = CyberTextPrimary, lineHeight = 30.sp,
            modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        Text(
            "Replace the register and Excel sheet. Everything a gym owner needs, built for India.",
            fontSize = 14.sp, color = CyberTextMuted, lineHeight = 21.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(Modifier.height(28.dp))

        val features = listOf(
            "👥" to "Member registry with plan validity tracking",
            "💰" to "Record cash, UPI & card fee payments",
            "🧾" to "GST-ready receipts shared on WhatsApp",
            "⏰" to "Expiry alerts — never miss a renewal again",
            "✅" to "Daily attendance check-ins",
            "📈" to "Monthly collections at a glance"
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(CyberBgCard)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            features.forEach { (emoji, text) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(emoji, fontSize = 18.sp)
                    Spacer(Modifier.width(12.dp))
                    Text(text, fontSize = 14.sp, color = CyberTextPrimary, lineHeight = 20.sp)
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        if (!trialUsed) {
            Button(
                onClick = onStartTrial,
                colors = ButtonDefaults.buttonColors(containerColor = CyberAccent),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text("Start 30-Day Free Trial", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = CyberAccentDark)
            }
            Spacer(Modifier.height(10.dp))
            Text("No card needed · Full access · Cancel anytime", fontSize = 12.sp, color = CyberTextMuted)
            Spacer(Modifier.height(16.dp))
            Text(
                "Already convinced? Upgrade to Business →",
                fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = CyberAccent,
                modifier = Modifier.clickable { onUpgradeClick() }.padding(8.dp)
            )
        } else {
            Text("Your free trial has ended", fontSize = 13.sp, color = CyberWarning, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onUpgradeClick,
                colors = ButtonDefaults.buttonColors(containerColor = CyberAccent),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text("Upgrade to Business — ₹2,499/mo", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = CyberAccentDark)
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}

// ─── First-run setup ──────────────────────────────────────────────────────────

@Composable
private fun GymSetupEmptyState(onSetupClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(CyberBgPrimary).padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🏢", fontSize = 56.sp)
        Spacer(Modifier.height(16.dp))
        Text("Set Up Your Gym", fontSize = 22.sp, fontWeight = FontWeight.Black, color = CyberTextPrimary)
        Spacer(Modifier.height(8.dp))
        Text("Add your gym's name and details to start managing members and collecting fees.",
            fontSize = 14.sp, color = CyberTextMuted, lineHeight = 21.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onSetupClick,
            colors = ButtonDefaults.buttonColors(containerColor = CyberAccent),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Text("Set Up Gym Profile", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = CyberAccentDark)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GymSetupSheet(onDismiss: () -> Unit, onSave: (name: String, address: String, gstin: String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var gstin by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = CyberBgCardElevated
    ) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
            Text("Gym Profile", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = CyberTextPrimary)
            Spacer(Modifier.height(4.dp))
            Text("Shown on receipts sent to members", fontSize = 13.sp, color = CyberTextMuted)
            Spacer(Modifier.height(20.dp))

            GymTextField(name, { name = it }, "Gym name *", "e.g. Iron Paradise Fitness")
            Spacer(Modifier.height(12.dp))
            GymTextField(address, { address = it }, "Address / area", "e.g. Andheri West, Mumbai")
            Spacer(Modifier.height(12.dp))
            GymTextField(gstin, { gstin = it }, "GSTIN (optional)", "Shown on receipts if provided")
            Spacer(Modifier.height(24.dp))

            Button(
                onClick = { if (name.isNotBlank()) onSave(name, address, gstin) },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = CyberAccent, disabledContainerColor = CyberBgCard),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text("Save & Start", fontWeight = FontWeight.Bold, fontSize = 15.sp,
                    color = if (name.isNotBlank()) CyberAccentDark else CyberTextMuted)
            }
        }
    }
}

// ─── Shared small components ──────────────────────────────────────────────────

@Composable
fun GymTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = CyberTextMuted, fontSize = 13.sp) },
        placeholder = { Text(placeholder, color = CyberTextMuted.copy(alpha = 0.6f), fontSize = 14.sp) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = CyberTextPrimary,
            unfocusedTextColor = CyberTextPrimary,
            focusedBorderColor = CyberAccent,
            unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
            cursorColor = CyberAccent
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun GymKpiCard(modifier: Modifier, value: String, label: String, icon: ImageVector, tint: Color) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(CyberBgCard)
            .padding(16.dp)
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(10.dp))
        Text(value, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = CyberTextPrimary)
        Text(label, fontSize = 12.sp, color = CyberTextMuted)
    }
}

@Composable
private fun GymActionCard(modifier: Modifier, icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(CyberBgCard)
            .border(1.dp, Color.White.copy(alpha = 0.07f), RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(icon, contentDescription = null, tint = CyberAccent, modifier = Modifier.size(20.dp))
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = CyberTextPrimary)
    }
}

@Composable
fun ExpiringMemberCard(member: GymMember, onClick: () -> Unit) {
    val statusColor = if (member.isExpired) CyberDanger else CyberWarning
    val statusText = when {
        member.isExpired -> "Expired ${-member.daysLeft}d ago"
        member.daysLeft == 0 -> "Expires today"
        else -> "Expires in ${member.daysLeft}d"
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
        Box(
            modifier = Modifier.width(4.dp).height(40.dp).clip(RoundedCornerShape(2.dp)).background(statusColor)
        )
        Column(Modifier.weight(1f)) {
            Text(member.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = CyberTextPrimary)
            Text("${member.planName.ifEmpty { "No plan" }} · $statusText", fontSize = 12.sp, color = statusColor, fontWeight = FontWeight.SemiBold)
        }
        Text("›", fontSize = 20.sp, color = CyberTextMuted)
    }
}

internal fun formatInr(amount: Int): String = when {
    amount >= 100000 -> "₹%.2fL".format(amount / 100000f)
    amount >= 1000   -> "₹%,d".format(amount)
    else             -> "₹$amount"
}

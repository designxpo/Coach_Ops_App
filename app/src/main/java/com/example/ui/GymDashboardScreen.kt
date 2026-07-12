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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storefront
import kotlinx.coroutines.launch
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
    val claims by viewModel.paymentClaims.collectAsStateWithLifecycle()
    val gyms by viewModel.gyms.collectAsStateWithLifecycle()
    val activeGym by viewModel.activeGym.collectAsStateWithLifecycle()

    var showSetupSheet by remember { mutableStateOf(false) }
    var showGymSwitcher by remember { mutableStateOf(false) }
    var confirmDeleteActive by remember { mutableStateOf(false) }
    var setupDone by remember { mutableStateOf(viewModel.gymSetupComplete) }

    if (confirmDeleteActive) {
        // Deleting the ACTIVE gym from Gym Settings — falls back to the default
        // gym identity for owners whose profile predates multi-gym
        val target = activeGym ?: com.example.data.Gym(
            id = com.example.data.DEFAULT_GYM_ID,
            name = viewModel.userPreferences.gymName.ifBlank { "My Gym" }
        )
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { confirmDeleteActive = false },
            title = { Text("Delete ${target.name}?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "This permanently deletes this gym with ALL its members, plans, " +
                    "payment history and attendance — on this device and in the cloud. " +
                    "This cannot be undone." +
                    if (gyms.size <= 1)
                        "\n\nThis is your only gym — deleting it removes your gym account " +
                        "and the Gym Suite returns to setup."
                    else ""
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    confirmDeleteActive = false
                    val wasLast = gyms.count { it.id != target.id } == 0
                    viewModel.deleteGym(target)
                    if (wasLast) setupDone = false
                }) { Text("Delete Permanently", color = CyberDanger) }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { confirmDeleteActive = false }) {
                    Text("Cancel", color = CyberAccent)
                }
            }
        )
    }

    if (showGymSwitcher) {
        GymSwitcherSheet(
            gyms = gyms,
            activeGymId = activeGym?.id ?: "",
            onSelect = { viewModel.switchGym(it); showGymSwitcher = false },
            onAdd = { name, city, address, upi -> viewModel.addGym(name, city, address, upi) },
            onDelete = { gym ->
                val wasLast = gyms.count { it.id != gym.id } == 0
                viewModel.deleteGym(gym)
                if (wasLast) {
                    // Gym account removed — back to first-run setup
                    showGymSwitcher = false
                    setupDone = false
                }
            },
            onDismiss = { showGymSwitcher = false }
        )
    }

    // Action feedback (claim confirmations etc.)
    val dashContext = LocalContext.current
    val snackbarMsg by viewModel.snackbar.collectAsStateWithLifecycle()
    androidx.compose.runtime.LaunchedEffect(snackbarMsg) {
        if (snackbarMsg.isNotEmpty()) {
            android.widget.Toast.makeText(dashContext, snackbarMsg, android.widget.Toast.LENGTH_SHORT).show()
            viewModel.clearSnackbar()
        }
    }

    val prefs = viewModel.userPreferences
    val setupSheet: @Composable () -> Unit = {
        GymSetupSheet(
            initialName = prefs.gymName,
            initialAddress = prefs.gymAddress,
            initialGstin = prefs.gymGstin,
            initialUpiId = prefs.gymUpiId,
            initialCity = prefs.gymCity,
            initialLat = prefs.gymLat,
            initialLng = prefs.gymLng,
            isEdit = setupDone,
            onDismiss = { showSetupSheet = false },
            onSave = { name, address, gstin, upiId, city, lat, lng ->
                viewModel.saveGymProfile(name, address, gstin, upiId, city, lat, lng)
                setupDone = true
                showSetupSheet = false
            },
            onDeleteGym = {
                showSetupSheet = false
                confirmDeleteActive = true
            }
        )
    }

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
        if (showSetupSheet) setupSheet()
        return
    }

    // ── State 2: unlocked but gym profile missing ─────────────────────────────
    if (!setupDone) {
        GymSetupEmptyState(onSetupClick = { showSetupSheet = true })
        if (showSetupSheet) setupSheet()
        return
    }

    if (showSetupSheet) setupSheet()

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
                // Gym switcher — tap to switch location, add a gym, or delete one
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showGymSwitcher = true }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Text(
                            activeGym?.name ?: viewModel.userPreferences.gymName.ifEmpty { "My Gym" },
                            fontSize = 20.sp, fontWeight = FontWeight.Bold, color = CyberTextPrimary,
                            maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        if ((activeGym?.city ?: "").isNotBlank()) {
                            Text("📍 ${activeGym?.city}", fontSize = 11.sp, color = CyberTextMuted, maxLines = 1)
                        }
                    }
                    Spacer(Modifier.width(6.dp))
                    Text("▾", fontSize = 16.sp, color = CyberTextMuted)
                }
                Box(
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(CyberBgCard)
                        .clickable { showSetupSheet = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(androidx.compose.material.icons.Icons.Filled.Settings,
                        contentDescription = "Gym settings",
                        tint = CyberTextSecondary, modifier = Modifier.size(18.dp))
                }
            }
        }

        // ── UPI payment confirmations from members ─────────────────────────────
        if (claims.isNotEmpty()) {
            item {
                Text("Payment Confirmations", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = CyberTextPrimary)
            }
            claims.forEach { claim ->
                item(key = "claim_${claim.id}") {
                    PaymentClaimCard(
                        claim = claim,
                        onConfirm = { viewModel.confirmClaim(claim) },
                        onReject = { viewModel.rejectClaim(claim) }
                    )
                }
            }
        }

        // Trial banner
        if (entitlements.gymTrialActive && !entitlements.gymSuiteEntitled) {
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
                    androidx.compose.foundation.layout.FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
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
fun GymSetupSheet(
    initialName: String = "",
    initialAddress: String = "",
    initialGstin: String = "",
    initialUpiId: String = "",
    initialCity: String = "",
    initialLat: Double = 0.0,
    initialLng: Double = 0.0,
    isEdit: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (name: String, address: String, gstin: String, upiId: String,
             city: String, lat: Double, lng: Double) -> Unit,
    onDeleteGym: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    var name by remember { mutableStateOf(initialName) }
    var address by remember { mutableStateOf(initialAddress) }
    var gstin by remember { mutableStateOf(initialGstin) }
    var upiId by remember { mutableStateOf(initialUpiId) }

    // Location — same Places autocomplete + GPS system as the trainer marketplace
    var cityQuery by remember { mutableStateOf(initialCity) }
    var pickedCity by remember { mutableStateOf(initialCity) }
    var pickedLat by remember { mutableStateOf(initialLat) }
    var pickedLng by remember { mutableStateOf(initialLng) }
    var suggestions by remember { mutableStateOf<List<com.example.data.NearbyArea>>(emptyList()) }
    var locating by remember { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(cityQuery) {
        if (cityQuery.length < 2 || cityQuery == pickedCity) { suggestions = emptyList(); return@LaunchedEffect }
        kotlinx.coroutines.delay(400)   // debounce typing
        suggestions = com.example.data.GeoUtils.searchLocations(context, cityQuery)
    }

    fun useDeviceLocation() {
        locating = true
        scope.launch {
            val coords = com.example.data.GeoUtils.getDeviceLocation(context)
            if (coords != null) {
                val label = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    com.example.data.GeoUtils.reverseGeocode(context, coords.first, coords.second)
                }
                pickedLat = coords.first
                pickedLng = coords.second
                pickedCity = label
                cityQuery = label
                suggestions = emptyList()
            }
            locating = false
        }
    }

    val locationPermLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) useDeviceLocation() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = CyberBgCardElevated
    ) {
        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier.padding(horizontal = 24.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 32.dp)
        ) {
            item {
                Text(if (isEdit) "Gym Settings" else "Gym Profile",
                    fontSize = 20.sp, fontWeight = FontWeight.Bold, color = CyberTextPrimary)
                Spacer(Modifier.height(4.dp))
                Text("Shown on receipts & used for member payments", fontSize = 13.sp, color = CyberTextMuted)
                Spacer(Modifier.height(20.dp))

                GymTextField(name, { name = it }, "Gym name *", "e.g. Iron Paradise Fitness")
                Spacer(Modifier.height(12.dp))

                // ── Location (autocomplete via marketplace map system) ─────────
                GymTextField(cityQuery, { cityQuery = it }, "Location / area *", "Type to search — e.g. Andheri West")
                suggestions.forEach { area ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(CyberBgCard)
                            .clickable {
                                scope.launch {
                                    val coords = if (area.placeId.isNotEmpty())
                                        com.example.data.GeoUtils.resolvePlace(area.placeId, context)
                                    else area.lat to area.lng
                                    pickedLat = coords?.first ?: area.lat
                                    pickedLng = coords?.second ?: area.lng
                                    pickedCity = area.name
                                    cityQuery = area.name
                                    suggestions = emptyList()
                                }
                            }
                            .padding(horizontal = 14.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("📍", fontSize = 14.sp)
                        Spacer(Modifier.width(8.dp))
                        Text(area.name, fontSize = 13.sp, color = CyberTextPrimary,
                            maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(CyberAccent.copy(alpha = 0.12f))
                        .clickable(enabled = !locating) {
                            val granted = androidx.core.content.ContextCompat.checkSelfPermission(
                                context, android.Manifest.permission.ACCESS_FINE_LOCATION
                            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                            if (granted) useDeviceLocation()
                            else locationPermLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
                        }
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(if (locating) "⏳ Locating…" else "📍 Use current location",
                        fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = CyberAccent)
                }
                if (pickedCity.isNotEmpty() && pickedLat != 0.0) {
                    Spacer(Modifier.height(6.dp))
                    Text("✓ Location set: $pickedCity", fontSize = 11.sp, color = CyberSuccess)
                }

                Spacer(Modifier.height(12.dp))
                GymTextField(address, { address = it }, "Full address (optional)", "Building, street, landmark")
                Spacer(Modifier.height(12.dp))

                // ── UPI collection ─────────────────────────────────────────────
                GymTextField(upiId, { upiId = it }, "UPI ID for fee collection", "e.g. ironparadise@okhdfcbank")
                Text(
                    "💰 Members can pay fees from their app straight to your bank — 0% commission",
                    fontSize = 11.sp, color = CyberTextMuted, lineHeight = 16.sp,
                    modifier = Modifier.padding(top = 6.dp)
                )
                Spacer(Modifier.height(12.dp))
                GymTextField(gstin, { gstin = it }, "GSTIN (optional)", "Shown on receipts if provided")
                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = {
                        if (name.isNotBlank()) onSave(name, address, gstin, upiId,
                            pickedCity.ifEmpty { cityQuery }, pickedLat, pickedLng)
                    },
                    enabled = name.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = CyberAccent, disabledContainerColor = CyberBgCard),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Text(if (isEdit) "Save Changes" else "Save & Start",
                        fontWeight = FontWeight.Bold, fontSize = 15.sp,
                        color = if (name.isNotBlank()) CyberAccentDark else CyberTextMuted)
                }

                // Delete this gym — visible in edit mode; confirmation + cascade
                // happen at the dashboard level
                if (isEdit && onDeleteGym != null) {
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(CyberDanger.copy(alpha = 0.10f))
                            .border(1.dp, CyberDanger.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                            .clickable { onDeleteGym() }
                            .padding(vertical = 14.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = null,
                            tint = CyberDanger, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Delete This Gym", fontSize = 14.sp,
                            fontWeight = FontWeight.Bold, color = CyberDanger)
                    }
                }
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
fun PaymentClaimCard(claim: PaymentClaim, onConfirm: () -> Unit, onReject: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(CyberBgCard)
            .border(1.dp, CyberAccent.copy(alpha = 0.35f), RoundedCornerShape(24.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("📱", fontSize = 20.sp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("${claim.memberName} says they paid", fontSize = 14.sp,
                    fontWeight = FontWeight.Bold, color = CyberTextPrimary)
                Text("₹${"%,d".format(claim.amountInr)} · ${claim.planName} · via UPI",
                    fontSize = 12.sp, color = CyberTextSecondary)
            }
        }
        Spacer(Modifier.height(6.dp))
        Text("Check your UPI app for the credit, then confirm to extend their membership.",
            fontSize = 11.sp, color = CyberTextMuted, lineHeight = 15.sp)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = CyberAccent),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("✓ Received", fontWeight = FontWeight.Bold, color = CyberAccentDark, fontSize = 13.sp)
            }
            Button(
                onClick = onReject,
                colors = ButtonDefaults.buttonColors(containerColor = CyberBgCardElevated),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("Not received", fontWeight = FontWeight.Bold, color = CyberDanger, fontSize = 13.sp)
            }
        }
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

// ─── Gym switcher — one owner, multiple locations ─────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GymSwitcherSheet(
    gyms: List<com.example.data.Gym>,
    activeGymId: String,
    onSelect: (com.example.data.Gym) -> Unit,
    onAdd: (name: String, city: String, address: String, upi: String) -> Unit,
    onDelete: (com.example.data.Gym) -> Unit,
    onDismiss: () -> Unit
) {
    var showAddForm by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var newCity by remember { mutableStateOf("") }
    var newAddress by remember { mutableStateOf("") }
    var newUpi by remember { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<com.example.data.Gym?>(null) }

    deleteTarget?.let { gym ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete ${gym.name}?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "This permanently deletes this gym with ALL its members, plans, " +
                    "payment history and attendance — on this device and in the cloud. " +
                    "This cannot be undone." +
                    if (gyms.size <= 1)
                        "\n\nThis is your only gym — deleting it removes your gym account " +
                        "and the Gym Suite returns to setup."
                    else ""
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    onDelete(gym); deleteTarget = null
                }) { Text("Delete Permanently", color = CyberDanger) }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { deleteTarget = null }) {
                    Text("Cancel", color = CyberAccent)
                }
            }
        )
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("My Gyms", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = CyberTextPrimary)

            gyms.forEach { gym ->
                val active = gym.id == activeGymId
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (active) CyberAccent.copy(0.10f) else CyberBgCardElevated)
                        .border(
                            1.dp,
                            if (active) CyberAccent.copy(0.35f) else Color.White.copy(0.06f),
                            RoundedCornerShape(16.dp)
                        )
                        .clickable { onSelect(gym) }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(if (active) "✓" else "🏋️", fontSize = 16.sp,
                        color = if (active) CyberAccent else CyberTextSecondary)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(gym.name, fontSize = 15.sp, fontWeight = FontWeight.Bold,
                            color = if (active) CyberAccent else CyberTextPrimary,
                            maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                        if (gym.city.isNotBlank() || gym.address.isNotBlank()) {
                            Text(
                                gym.city.ifBlank { gym.address },
                                fontSize = 12.sp, color = CyberTextMuted,
                                maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                    }
                    Box(
                        modifier = Modifier.size(32.dp).clip(CircleShape)
                            .background(CyberDanger.copy(0.10f))
                            .clickable { deleteTarget = gym },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete gym",
                            tint = CyberDanger, modifier = Modifier.size(16.dp))
                    }
                }
            }

            if (!showAddForm) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CyberAccent.copy(0.08f))
                        .border(1.dp, CyberAccent.copy(0.25f), RoundedCornerShape(16.dp))
                        .clickable { showAddForm = true }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("＋", fontSize = 18.sp, color = CyberAccent)
                    Text("Add another gym", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = CyberAccent)
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CyberBgCardElevated)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("New Gym", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = CyberTextPrimary)
                    GymSwitcherField("Gym name *", newName) { newName = it }
                    GymSwitcherField("City / area *", newCity) { newCity = it }
                    GymSwitcherField("Address (optional)", newAddress) { newAddress = it }
                    GymSwitcherField("UPI ID for payments (optional)", newUpi) { newUpi = it }
                    val canAdd = newName.isNotBlank() && newCity.isNotBlank()
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (canAdd) CyberAccent else CyberBgCard)
                            .clickable(enabled = canAdd) {
                                onAdd(newName, newCity, newAddress, newUpi)
                                newName = ""; newCity = ""; newAddress = ""; newUpi = ""
                                showAddForm = false
                                onDismiss()
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Create Gym", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold,
                            color = if (canAdd) CyberAccentDark else CyberTextMuted)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun GymSwitcherField(label: String, value: String, onChange: (String) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(CyberBgCard)
            .border(1.dp, Color.White.copy(0.06f), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 11.dp)
    ) {
        androidx.compose.foundation.text.BasicTextField(
            value = value,
            onValueChange = onChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(color = CyberTextPrimary, fontSize = 13.sp),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(CyberAccent),
            decorationBox = { inner ->
                if (value.isEmpty()) Text(label, fontSize = 13.sp, color = CyberTextMuted)
                inner()
            }
        )
    }
}

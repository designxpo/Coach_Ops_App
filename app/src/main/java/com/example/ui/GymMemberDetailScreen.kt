package com.example.ui

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.GymMember
import com.example.data.GymPayment
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun GymMemberDetailScreen(
    viewModel: GymViewModel,
    memberId: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val member by viewModel.getMember(memberId).collectAsStateWithLifecycle(initialValue = null)
    val payments by viewModel.getPaymentsForMember(memberId).collectAsStateWithLifecycle(initialValue = emptyList())
    val plans by viewModel.plans.collectAsStateWithLifecycle()

    var showPaymentSheet by remember { mutableStateOf(false) }
    var showEditSheet by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val m = member ?: return
    val dateFmt = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    if (showPaymentSheet) {
        RecordPaymentSheet(
            member = m,
            plans = plans,
            onDismiss = { showPaymentSheet = false },
            onSave = { plan, amount, method, notes ->
                viewModel.recordPayment(m, plan, amount, method, notes)
                showPaymentSheet = false
            }
        )
    }

    if (showEditSheet) {
        GymMemberSheet(
            plans = plans,
            existing = m,
            onDismiss = { showEditSheet = false },
            onSave = { name, phone, gender, _, notes ->
                viewModel.updateMember(m.copy(name = name, phone = phone, gender = gender, notes = notes))
                showEditSheet = false
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = CyberBgCardElevated,
            title = { Text("Remove ${m.name}?", color = CyberTextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("This deletes the member along with their payment and attendance history. This can't be undone.", color = CyberTextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    viewModel.deleteMember(m.id)
                    onBack()
                }) { Text("Remove", color = CyberDanger, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel", color = CyberTextMuted) }
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(CyberBgPrimary),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
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
                Text("Member", fontSize = 20.sp, fontWeight = FontWeight.Bold,
                    color = CyberTextPrimary, modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(CyberBgCard).clickable { showEditSheet = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit", tint = CyberTextSecondary, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(CyberBgCard).clickable { showDeleteDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = CyberDanger, modifier = Modifier.size(16.dp))
                }
            }
        }

        // Profile card
        item {
            Column(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(CyberBgCard).padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.size(64.dp).clip(CircleShape).background(CyberAccent.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        m.name.split(" ").take(2).mapNotNull { it.firstOrNull()?.uppercase() }.joinToString(""),
                        fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = CyberAccent
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(m.name, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = CyberTextPrimary)
                if (m.phone.isNotEmpty()) {
                    Text("+91 ${m.phone}", fontSize = 13.sp, color = CyberTextSecondary)
                }
                Text("Joined ${dateFmt.format(Date(m.joinDateMillis))}", fontSize = 12.sp, color = CyberTextMuted)
                if (m.notes.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(m.notes, fontSize = 13.sp, color = CyberTextSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
        }

        // Membership status card
        item {
            val (statusColor, statusText) = when {
                m.planEndMillis == 0L -> CyberTextMuted to "No active plan"
                m.isExpired           -> CyberDanger to "Expired ${-m.daysLeft} days ago"
                m.expiringSoon        -> CyberWarning to "Expires in ${m.daysLeft} days"
                else                  -> CyberSuccess to "${m.daysLeft} days remaining"
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(CyberBgCard)
                    .border(1.dp, statusColor.copy(alpha = 0.3f), RoundedCornerShape(28.dp))
                    .padding(20.dp)
            ) {
                Text("MEMBERSHIP", fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                    color = CyberTextMuted, letterSpacing = 0.8.sp)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(m.planName.ifEmpty { "—" }, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = CyberTextPrimary)
                        Text(statusText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = statusColor)
                        if (m.planEndMillis > 0L) {
                            Text("Valid till ${dateFmt.format(Date(m.planEndMillis))}", fontSize = 12.sp, color = CyberTextMuted)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { showPaymentSheet = true },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberAccent),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.CurrencyRupee, contentDescription = null, tint = CyberAccentDark, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (m.isExpired || m.planEndMillis == 0L) "Collect Fee" else "Renew",
                            fontWeight = FontWeight.Bold, color = CyberAccentDark, fontSize = 13.sp)
                    }
                    if (m.phone.isNotEmpty()) {
                        Button(
                            onClick = {
                                val msg = if (m.isExpired)
                                    "Hi ${m.name}, your ${m.planName} membership at ${viewModel.userPreferences.gymName} has expired. Renew today to keep your streak going! 💪"
                                else
                                    "Hi ${m.name}, your ${m.planName} membership at ${viewModel.userPreferences.gymName} expires on ${dateFmt.format(Date(m.planEndMillis))}. Renew in advance to avoid a break! 💪"
                                openWhatsApp(context, m.phone, msg)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberBgCardElevated),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("💬 Remind", fontWeight = FontWeight.Bold, color = CyberTextPrimary, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // Quick check-in
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(CyberBgCard)
                    .clickable { viewModel.checkIn(m) }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = CyberSuccess, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text("Mark attendance for today", fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                    color = CyberTextPrimary, modifier = Modifier.weight(1f))
                Text("›", fontSize = 20.sp, color = CyberTextMuted)
            }
        }

        // Payment history
        item {
            Text("Payment History", fontSize = 18.sp, fontWeight = FontWeight.Bold,
                color = CyberTextPrimary, modifier = Modifier.padding(top = 4.dp))
        }
        if (payments.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                    Text("No payments recorded yet", color = CyberTextSecondary, fontSize = 14.sp)
                }
            }
        } else {
            payments.forEach { payment ->
                item(key = payment.id) {
                    GymPaymentCard(
                        payment = payment,
                        onShare = {
                            shareReceipt(context, m.phone, viewModel.receiptText(payment))
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun GymPaymentCard(payment: GymPayment, onShare: () -> Unit) {
    val dateFmt = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val methodEmoji = when (payment.method) {
        "UPI"  -> "📱"
        "CARD" -> "💳"
        else   -> "💵"
    }
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(CyberBgCard).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(methodEmoji, fontSize = 22.sp)
        Column(Modifier.weight(1f)) {
            Text("₹${"%,d".format(payment.amountInr)} · ${payment.planName}", fontSize = 14.sp,
                fontWeight = FontWeight.Bold, color = CyberTextPrimary)
            Text("${payment.receiptNo} · ${dateFmt.format(Date(payment.dateMillis))} · ${payment.method}",
                fontSize = 11.sp, color = CyberTextMuted)
        }
        Box(
            modifier = Modifier.size(34.dp).clip(CircleShape).background(CyberBgCardElevated).clickable { onShare() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Share, contentDescription = "Share receipt", tint = CyberAccent, modifier = Modifier.size(15.dp))
        }
    }
}

// ─── Record payment sheet ─────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordPaymentSheet(
    member: GymMember,
    plans: List<GymPlan>,
    onDismiss: () -> Unit,
    onSave: (plan: GymPlan, amountInr: Int, method: String, notes: String) -> Unit
) {
    var selectedPlan by remember { mutableStateOf(plans.find { it.id == member.planId } ?: plans.firstOrNull()) }
    var amountText by remember { mutableStateOf((plans.find { it.id == member.planId } ?: plans.firstOrNull())?.priceInr?.toString() ?: "") }
    var method by remember { mutableStateOf("UPI") }
    var notes by remember { mutableStateOf("") }

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
                Text("Collect Fee — ${member.name}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = CyberTextPrimary)
                Spacer(Modifier.height(4.dp))
                Text("Membership extends automatically from the current expiry", fontSize = 12.sp, color = CyberTextMuted)
                Spacer(Modifier.height(20.dp))

                if (plans.isEmpty()) {
                    Text("Create a membership plan first (Gym → Plans)", fontSize = 14.sp, color = CyberWarning)
                } else {
                    Text("Plan", fontSize = 13.sp, color = CyberTextMuted)
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
                                    selectedPlan = plan
                                    amountText = plan.priceInr.toString()
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(plan.name, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                                    color = if (active) CyberAccent else CyberTextPrimary)
                                Text("${plan.durationDays} days", fontSize = 11.sp, color = CyberTextMuted)
                            }
                            Text("₹${"%,d".format(plan.priceInr)}", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold,
                                color = if (active) CyberAccent else CyberTextSecondary)
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    GymTextField(
                        amountText,
                        { amountText = it.filter { c -> c.isDigit() }.take(7) },
                        "Amount collected (₹) *",
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    )

                    Spacer(Modifier.height(14.dp))
                    Text("Payment method", fontSize = 13.sp, color = CyberTextMuted)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("UPI" to "📱 UPI", "CASH" to "💵 Cash", "CARD" to "💳 Card").forEach { (key, label) ->
                            val active = method == key
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(if (active) CyberAccent.copy(alpha = 0.15f) else CyberBgCard)
                                    .clickable { method = key }
                                    .padding(horizontal = 16.dp, vertical = 10.dp)
                            ) {
                                Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                                    color = if (active) CyberAccent else CyberTextSecondary)
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    GymTextField(notes, { notes = it }, "Notes (optional)", "e.g. ₹200 discount applied")
                    Spacer(Modifier.height(24.dp))

                    val amount = amountText.toIntOrNull() ?: 0
                    val valid = selectedPlan != null && amount > 0
                    Button(
                        onClick = { if (valid) onSave(selectedPlan!!, amount, method, notes) },
                        enabled = valid,
                        colors = ButtonDefaults.buttonColors(containerColor = CyberAccent, disabledContainerColor = CyberBgCard),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) {
                        Text("Record Payment", fontWeight = FontWeight.Bold, fontSize = 15.sp,
                            color = if (valid) CyberAccentDark else CyberTextMuted)
                    }
                }
            }
        }
    }
}

// ─── WhatsApp helpers ─────────────────────────────────────────────────────────

internal fun openWhatsApp(context: android.content.Context, phone10Digit: String, message: String) {
    val url = if (phone10Digit.isNotEmpty())
        "https://wa.me/91$phone10Digit?text=${Uri.encode(message)}"
    else
        "https://wa.me/?text=${Uri.encode(message)}"
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (_: Exception) { }
}

internal fun shareReceipt(context: android.content.Context, phone10Digit: String, receiptText: String) {
    if (phone10Digit.isNotEmpty()) {
        openWhatsApp(context, phone10Digit, receiptText)
    } else {
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, receiptText)
            }
            context.startActivity(Intent.createChooser(intent, "Share receipt"))
        } catch (_: Exception) { }
    }
}

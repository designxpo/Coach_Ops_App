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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Booking
import com.example.ui.theme.CyberAccent
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
fun ClientDashboardScreen(viewModel: ClientViewModel) {
    val bookings by viewModel.myBookings.collectAsState()

    // Gym membership (auto-matched by phone number) — plan status + renewal countdown
    var memberships by remember {
        mutableStateOf<List<com.example.data.GymMembershipLookup.Membership>>(emptyList())
    }
    LaunchedEffect(Unit) {
        viewModel.loadMyBookings()
        memberships = com.example.data.GymMembershipLookup.findByPhone(viewModel.userPhone())
    }

    val upcoming = bookings.filter { it.status in listOf("PENDING", "CONFIRMED") }
    val past     = bookings.filter { it.status in listOf("COMPLETED", "DECLINED", "CANCELLED") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBgPrimary)
            .statusBarsPadding()
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            Text("My Bookings", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = CyberTextPrimary)
            Spacer(Modifier.height(4.dp))
            Text("${bookings.size} total sessions", fontSize = 13.sp, color = CyberTextMuted)
        }

        memberships.forEach { ms ->
            GymMembershipCard(
                membership = ms,
                onClaimSubmitted = {
                    // Optimistically flip the card into "awaiting confirmation"
                    memberships = memberships.map {
                        if (it.ownerUid == ms.ownerUid) it.copy(claimStatus = "PENDING") else it
                    }
                },
                modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 12.dp)
            )
        }

        if (bookings.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                    Text("📅", fontSize = 44.sp)
                    Spacer(Modifier.height(12.dp))
                    Text("No bookings yet", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = CyberTextPrimary)
                    Spacer(Modifier.height(6.dp))
                    Text("Go to Discover to find and request a trainer", fontSize = 13.sp, color = CyberTextMuted)
                }
            }
            return
        }

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (upcoming.isNotEmpty()) {
                item {
                    Text(
                        "Upcoming", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CyberTextMuted,
                        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                    )
                }
                items(upcoming, key = { it.id }) { booking ->
                    BookingCard(booking = booking, viewModel = viewModel)
                }
            }

            if (past.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Past", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CyberTextMuted,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                items(past, key = { it.id }) { booking ->
                    BookingCard(booking = booking, viewModel = viewModel, dimmed = booking.status != "COMPLETED")
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun GymMembershipCard(
    membership: com.example.data.GymMembershipLookup.Membership,
    onClaimSubmitted: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val dateFmt = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    var submitting by remember { mutableStateOf(false) }

    val (statusColor, statusText) = when {
        membership.planEndMillis <= 0L -> CyberTextMuted to "No active plan"
        membership.isExpired           -> CyberDanger to "Expired"
        membership.daysLeft <= 7       -> CyberWarning to "${membership.daysLeft} day(s) left"
        else                           -> CyberSuccess to "${membership.daysLeft} days left"
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(CyberBgCard)
            .border(1.dp, statusColor.copy(alpha = 0.35f), RoundedCornerShape(24.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("🏋️", fontSize = 22.sp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("MY GYM MEMBERSHIP", fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
                    color = CyberTextMuted, letterSpacing = 0.8.sp)
                Text(membership.gymName, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = CyberTextPrimary)
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
        if (membership.planName.isNotEmpty() && membership.planEndMillis > 0L) {
            Spacer(Modifier.height(8.dp))
            Text(
                "${membership.planName} · valid till ${dateFmt.format(Date(membership.planEndMillis))}",
                fontSize = 12.sp, color = CyberTextSecondary
            )
        }

        // ── Pay from app: UPI straight to the gym's bank ──────────────────────
        when {
            membership.claimStatus == "PENDING" -> {
                Spacer(Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CyberWarning.copy(alpha = 0.12f))
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Text("⏳ Payment sent — awaiting gym confirmation",
                        fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = CyberWarning)
                }
            }
            membership.canPayInApp && (membership.isExpired || membership.daysLeft <= 7) -> {
                if (membership.claimStatus == "REJECTED") {
                    Spacer(Modifier.height(8.dp))
                    Text("⚠️ Gym couldn't confirm your last payment — contact them or pay again",
                        fontSize = 11.sp, color = CyberDanger, lineHeight = 15.sp)
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(CyberAccent)
                            .clickable { launchUpiPayment(context, membership) }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (membership.renewalAmountInr > 0)
                                "Pay ₹${"%,d".format(membership.renewalAmountInr)} via UPI"
                            else "Pay via UPI",
                            fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2D4800)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(CyberBgCardElevated)
                            .clickable(enabled = !submitting) {
                                submitting = true
                                scope.launch {
                                    val ok = com.example.data.GymMembershipLookup.submitPaymentClaim(membership)
                                    submitting = false
                                    if (ok) onClaimSubmitted()
                                    else android.widget.Toast.makeText(context,
                                        "Couldn't notify gym — check connection", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(if (submitting) "…" else "I've paid ✓",
                            fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CyberTextPrimary)
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text("Pays directly to the gym's UPI. After paying, tap \"I've paid\" so the gym can confirm.",
                    fontSize = 10.sp, color = CyberTextMuted, lineHeight = 14.sp)
            }
        }
    }
}

/** Opens the member's UPI app (GPay/PhonePe/Paytm…) with gym VPA + amount prefilled. */
private fun launchUpiPayment(
    context: android.content.Context,
    ms: com.example.data.GymMembershipLookup.Membership
) {
    val uri = android.net.Uri.parse(
        "upi://pay?pa=${android.net.Uri.encode(ms.upiId)}" +
        "&pn=${android.net.Uri.encode(ms.gymName)}" +
        (if (ms.renewalAmountInr > 0) "&am=${ms.renewalAmountInr}" else "") +
        "&cu=INR" +
        "&tn=${android.net.Uri.encode("${ms.planName} fee - ${ms.memberName}")}"
    )
    try {
        context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, uri))
    } catch (_: Exception) {
        android.widget.Toast.makeText(context, "No UPI app found on this device", android.widget.Toast.LENGTH_SHORT).show()
    }
}

@Composable
private fun BookingCard(
    booking: Booking,
    viewModel: ClientViewModel,
    dimmed: Boolean = false
) {
    val (statusColor, statusLabel) = when (booking.status) {
        "CONFIRMED"  -> CyberSuccess to "Confirmed"
        "PENDING"    -> CyberWarning to "Pending"
        "DECLINED"   -> CyberDanger  to "Declined"
        "COMPLETED"  -> CyberAccent  to "Completed"
        else         -> CyberTextMuted to booking.status
    }

    val sessionDateStr = if (booking.sessionDateMillis > 0)
        SimpleDateFormat("EEE, MMM d yyyy", Locale.getDefault()).format(Date(booking.sessionDateMillis))
    else null

    val createdStr = if (booking.createdAtMillis > 0)
        SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(booking.createdAtMillis))
    else "—"

    var pendingRating    by remember(booking.id) { mutableStateOf(0f) }
    var ratingSubmitted  by remember(booking.id) { mutableStateOf(booking.clientRating > 0f) }
    var showCancelDialog by remember { mutableStateOf(false) }
    var cancelError      by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    // 24hr cancellation window check
    val canCancel = booking.status in listOf("PENDING", "CONFIRMED") &&
        (booking.sessionDateMillis == 0L ||
         (booking.sessionDateMillis - System.currentTimeMillis()) > 24 * 60 * 60 * 1000L)

    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Cancel Booking", fontWeight = FontWeight.Bold) },
            text  = { Text("Are you sure you want to cancel this booking with ${booking.coachName}?") },
            confirmButton = {
                TextButton(onClick = {
                    showCancelDialog = false
                    scope.launch {
                        val success = viewModel.cancelBooking(booking.id)
                        if (!success) cancelError = "Cannot cancel within 24 hours of the session."
                    }
                }) { Text("Cancel Booking", color = CyberDanger, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) {
                    Text("Keep", color = CyberAccent)
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(if (dimmed) CyberBgCard.copy(alpha = 0.75f) else CyberBgCard)
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Header row: coach name + status badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    booking.coachName.ifEmpty { "Coach" },
                    fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = CyberTextPrimary
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(statusColor.copy(alpha = 0.12f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(statusLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = statusColor)
                }
            }

            // Session date (prominent if set)
            if (sessionDateStr != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(CyberAccent.copy(alpha = 0.08f))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("📅", fontSize = 13.sp)
                    Text("Session: $sessionDateStr", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = CyberAccent)
                }
            }

            // Fee + created date
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (booking.feeAmount > 0) {
                    Text("₹${booking.feeAmount}/session", fontSize = 12.sp, color = CyberTextSecondary, fontWeight = FontWeight.SemiBold)
                }
                Text("Requested $createdStr", fontSize = 12.sp, color = CyberTextMuted)
            }

            // Client's notes
            if (booking.notes.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(CyberBgCardElevated)
                        .padding(10.dp)
                ) {
                    Text("\"${booking.notes}\"", fontSize = 12.sp, color = CyberTextSecondary, lineHeight = 16.sp)
                }
            }

            // Coach response
            if (booking.coachResponse.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(statusColor.copy(alpha = 0.08f))
                        .padding(10.dp)
                ) {
                    Text("Coach: ${booking.coachResponse}", fontSize = 12.sp, color = statusColor, lineHeight = 16.sp)
                }
            }

            // Cancel option (PENDING or CONFIRMED, outside 24hr window)
            if (canCancel) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, CyberDanger.copy(0.4f), RoundedCornerShape(10.dp))
                        .clickable { showCancelDialog = true }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Cancel Booking", fontSize = 12.sp, color = CyberDanger, fontWeight = FontWeight.Bold)
                }
            } else if (booking.status in listOf("PENDING", "CONFIRMED") && booking.sessionDateMillis > 0) {
                // Within 24hr — show lock message
                Text(
                    "⏳ Cannot cancel within 24 hours of session",
                    fontSize = 11.sp, color = CyberTextMuted
                )
            }
            if (cancelError.isNotEmpty()) {
                Text(cancelError, fontSize = 11.sp, color = CyberDanger)
            }

            // Star rating for COMPLETED bookings not yet rated
            if (booking.status == "COMPLETED") {
                if (ratingSubmitted || booking.clientRating > 0f) {
                    val displayRating = if (booking.clientRating > 0f) booking.clientRating else pendingRating
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("Your rating:", fontSize = 12.sp, color = CyberTextMuted)
                        repeat(5) { i ->
                            Text(if (i < displayRating.toInt()) "⭐" else "☆", fontSize = 14.sp)
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Rate this session:", fontSize = 12.sp, color = CyberTextMuted, fontWeight = FontWeight.SemiBold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf(1f, 2f, 3f, 4f, 5f).forEach { star ->
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (pendingRating >= star) CyberAccent.copy(0.15f)
                                            else CyberBgCardElevated
                                        )
                                        .border(
                                            1.dp,
                                            if (pendingRating >= star) CyberAccent.copy(0.4f)
                                            else Color.White.copy(0.06f),
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable { pendingRating = star },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("⭐", fontSize = 16.sp)
                                }
                            }
                            if (pendingRating > 0f) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(CyberAccent)
                                        .clickable {
                                            viewModel.rateCoach(booking.id, booking.coachId, pendingRating) { success ->
                                                if (success) ratingSubmitted = true
                                            }
                                        }
                                        .padding(horizontal = 14.dp, vertical = 10.dp)
                                ) {
                                    Text("Submit", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0A0A0A))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

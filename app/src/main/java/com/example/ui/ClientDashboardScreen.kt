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

    LaunchedEffect(Unit) { viewModel.loadMyBookings() }

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

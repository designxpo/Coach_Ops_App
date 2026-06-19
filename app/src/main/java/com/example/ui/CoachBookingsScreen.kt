package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Booking
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
fun CoachBookingsScreen(viewModel: MainViewModel) {
    val bookings by viewModel.coachBookings.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadCoachBookings() }

    val pending   = bookings.filter { it.status == "PENDING" }
    val confirmed = bookings.filter { it.status == "CONFIRMED" }
    val past      = bookings.filter { it.status in listOf("DECLINED", "COMPLETED", "CANCELLED") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBgPrimary)
            .statusBarsPadding()
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Booking Requests", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = CyberTextPrimary)
                    Text("From members finding you on ProCoach India", fontSize = 12.sp, color = CyberTextMuted)
                }
                if (pending.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(CyberWarning.copy(0.15f))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("${pending.size} new", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CyberWarning)
                    }
                }
            }
        }

        if (bookings.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                    Text("📭", fontSize = 44.sp)
                    Spacer(Modifier.height(12.dp))
                    Text("No bookings yet", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = CyberTextPrimary)
                    Spacer(Modifier.height(6.dp))
                    Text("Go to Profile → Marketplace to make your profile public", fontSize = 13.sp, color = CyberTextMuted)
                }
            }
            return
        }

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (pending.isNotEmpty()) {
                item { SectionLabel("New Requests") }
                items(pending, key = { it.id }) { booking ->
                    CoachBookingCard(booking = booking, onAccept = { response ->
                        viewModel.respondToBooking(booking.id, true, response)
                    }, onDecline = { response ->
                        viewModel.respondToBooking(booking.id, false, response)
                    })
                }
            }

            if (confirmed.isNotEmpty()) {
                item { SectionLabel("Confirmed Sessions") }
                items(confirmed, key = { it.id }) { booking ->
                    CoachBookingCard(
                        booking   = booking,
                        readOnly  = true,
                        onCancel  = { viewModel.viewModelScope.launch {
                            viewModel.cancelBooking(booking.id, booking.sessionDateMillis)
                        }}
                    )
                }
            }

            if (past.isNotEmpty()) {
                item { SectionLabel("Past") }
                items(past, key = { it.id }) { booking ->
                    CoachBookingCard(booking = booking, readOnly = true, dimmed = true)
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun SectionLabel(label: String) {
    Text(
        label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CyberTextMuted,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}

@Composable
private fun CoachBookingCard(
    booking: Booking,
    readOnly: Boolean = false,
    dimmed: Boolean = false,
    onAccept: (String) -> Unit = {},
    onDecline: (String) -> Unit = {},
    onCancel: (() -> Unit)? = null
) {
    var responseText    by remember { mutableStateOf("") }
    var showCancelDialog by remember { mutableStateOf(false) }

    val canCancel = onCancel != null && booking.status in listOf("PENDING", "CONFIRMED") &&
        (booking.sessionDateMillis == 0L ||
         (booking.sessionDateMillis - System.currentTimeMillis()) > 24 * 60 * 60 * 1000L)

    if (showCancelDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Cancel Booking?", fontWeight = FontWeight.Bold) },
            text  = { Text("Cancel the confirmed session with ${booking.clientName}?") },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    showCancelDialog = false; onCancel?.invoke()
                }) { Text("Cancel Session", color = CyberDanger, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showCancelDialog = false }) {
                    Text("Keep", color = CyberAccent)
                }
            }
        )
    }

    val (statusColor, statusLabel) = when (booking.status) {
        "CONFIRMED" -> CyberSuccess to "Confirmed"
        "PENDING"   -> CyberWarning to "Pending"
        "DECLINED"  -> CyberDanger  to "Declined"
        "COMPLETED" -> CyberAccent  to "Completed"
        else        -> CyberTextMuted to booking.status
    }

    val requestedStr = if (booking.createdAtMillis > 0)
        SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(booking.createdAtMillis))
    else "—"

    val sessionDateStr = if (booking.sessionDateMillis > 0)
        SimpleDateFormat("EEE, MMM d yyyy", Locale.getDefault()).format(Date(booking.sessionDateMillis))
    else null

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(if (dimmed) CyberBgCard.copy(alpha = 0.7f) else CyberBgCard)
            .border(
                width = if (!readOnly && booking.status == "PENDING") 1.5.dp else 1.dp,
                color = if (!readOnly && booking.status == "PENDING") CyberWarning.copy(0.35f) else Color.White.copy(0.06f),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(booking.clientName.ifEmpty { "Client" }, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = CyberTextPrimary)
                    Text("Requested $requestedStr", fontSize = 11.sp, color = CyberTextMuted)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(statusColor.copy(0.12f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(statusLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = statusColor)
                }
            }

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

            if (booking.feeAmount > 0) {
                Text("₹${booking.feeAmount}/session", fontSize = 13.sp, color = CyberAccent, fontWeight = FontWeight.Bold)
            }

            if (booking.notes.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CyberBgCardElevated)
                        .padding(12.dp)
                ) {
                    Text("\"${booking.notes}\"", fontSize = 12.sp, color = CyberTextSecondary, lineHeight = 16.sp)
                }
            }

            if (!readOnly && booking.status == "PENDING") {
                // Response input
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CyberBgCardElevated)
                        .border(1.dp, Color.White.copy(0.06f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    BasicTextField(
                        value = responseText,
                        onValueChange = { responseText = it },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(color = CyberTextPrimary, fontSize = 13.sp),
                        cursorBrush = SolidColor(CyberAccent),
                        decorationBox = { inner ->
                            if (responseText.isEmpty()) Text("Add a note to member (optional)", fontSize = 13.sp, color = CyberTextMuted)
                            inner()
                        }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(CyberDanger.copy(0.12f))
                            .border(1.dp, CyberDanger.copy(0.3f), RoundedCornerShape(12.dp))
                            .clickable { onDecline(responseText) }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Decline", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = CyberDanger)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(CyberAccent)
                            .clickable { onAccept(responseText) }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Accept", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = CyberAccentDark)
                    }
                }
            }

            // Cancel button (confirmed sessions only, outside 24hr window)
            if (canCancel) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(10.dp))
                        .border(1.dp, CyberDanger.copy(0.4f), androidx.compose.foundation.shape.RoundedCornerShape(10.dp))
                        .clickable { showCancelDialog = true }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Cancel Session", fontSize = 12.sp, color = CyberDanger, fontWeight = FontWeight.Bold)
                }
            } else if (booking.status == "CONFIRMED" && booking.sessionDateMillis > 0 &&
                       (booking.sessionDateMillis - System.currentTimeMillis()) <= 24 * 60 * 60 * 1000L) {
                Text("⏳ Cannot cancel within 24 hours of session", fontSize = 11.sp, color = CyberTextMuted)
            }
        }
    }
}

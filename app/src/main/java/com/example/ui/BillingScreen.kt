package com.example.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.Canvas
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import com.example.data.Payment
import com.example.data.RevenueSnapshot
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
fun BillingScreen(viewModel: MainViewModel) {
    val payments by viewModel.payments.collectAsStateWithLifecycle()
    val clients by viewModel.clients.collectAsStateWithLifecycle()
    val snapshots by viewModel.revenueSnapshots.collectAsStateWithLifecycle()

    val totalMrr = clients.sumOf { it.mrr }
    val activeCount = payments.count { it.mandateStatus == "ACTIVE" }
    val expiringCount = payments.count { it.mandateStatus == "EXPIRING" }
    val failedCount = payments.count { it.mandateStatus == "FAILED" }

    // Compute real month-over-month MRR change from snapshots (sorted newest-first)
    val mrrGrowthText: String? = if (snapshots.size >= 2) {
        val thisMonth = snapshots[0].totalMrr
        val lastMonth = snapshots[1].totalMrr
        val diff = thisMonth - lastMonth
        when {
            lastMonth == 0 && diff > 0 -> "+₹${formatAmount(diff).drop(1)} this month"
            lastMonth == 0             -> null
            diff > 0 -> "+${formatAmount(diff)} from last month"
            diff < 0 -> { val absFormatted = formatAmount(kotlin.math.abs(diff)); "-$absFormatted from last month" }
            else     -> "Same as last month"
        }
    } else null

    val expectedMrr = payments.filter { it.mandateStatus == "ACTIVE" }.sumOf { it.amount }
    val atRiskMrr = payments.filter { it.mandateStatus == "EXPIRING" || it.mandateStatus == "FAILED" || it.daysOverdue > 0 }.sumOf { it.amount }
    val forecastTotal = expectedMrr + atRiskMrr
    val forecastPct = if (forecastTotal > 0) expectedMrr.toFloat() / forecastTotal.toFloat() else 0f

    val mrrDisplay = formatAmount(totalMrr)

    var activeFilter by remember { mutableStateOf("All") }
    val filters = listOf("All", "Overdue", "Due This Week", "Mandate Expiring", "Failed")

    val filteredPayments = payments.filter { payment ->
        when (activeFilter) {
            "Overdue" -> payment.daysOverdue > 0
            "Due This Week" -> payment.daysOverdue == 0 && payment.dueDateMillis < System.currentTimeMillis() + 7 * 86400000L
            "Mandate Expiring" -> payment.mandateStatus == "EXPIRING"
            "Failed" -> payment.mandateStatus == "FAILED"
            else -> true
        }
    }

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
                    Icon(Icons.Filled.AttachMoney, contentDescription = null, tint = CyberTextPrimary, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(12.dp))
                Text("Revenue & Billing", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = CyberTextPrimary, modifier = Modifier.weight(1f))
            }
        }

        // MRR Widget — lime hero card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(CyberAccent)
                    .padding(20.dp)
            ) {
                Column {
                    Text("COLLECTED MRR", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0A0A0A).copy(alpha = 0.5f), letterSpacing = 0.8.sp)
                    Text(mrrDisplay, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = CyberAccentDark, lineHeight = 36.sp)

                    if (mrrGrowthText != null) {
                        Spacer(Modifier.height(4.dp))
                        val isPositive = mrrGrowthText.startsWith("+")
                        val isNegative = mrrGrowthText.startsWith("-") || mrrGrowthText.startsWith("−")
                        val growthColor = when {
                            isPositive -> Color(0xFF166534)
                            isNegative -> Color(0xFF991B1B)
                            else       -> Color(0xFF0A0A0A).copy(alpha = 0.5f)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (isPositive) Icon(Icons.Filled.ArrowUpward, contentDescription = null, tint = growthColor, modifier = Modifier.size(12.dp))
                            Text(mrrGrowthText, fontSize = 12.sp, color = growthColor, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BillingChip("$activeCount Active", Color(0xFF166534))
                        BillingChip("$expiringCount Expiring", Color(0xFF92400E))
                        BillingChip("$failedCount Failed", Color(0xFF991B1B))
                    }
                }
            }
        }

        // MRR Trend Chart
        if (snapshots.size >= 2) {
            item { MrrTrendChart(snapshots) }
        }

        // Revenue Forecast Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(CyberBgCard)
                    .padding(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("30-DAY FORECAST", fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                        color = CyberTextMuted, letterSpacing = 0.8.sp)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Expected", fontSize = 12.sp, color = CyberTextMuted)
                            Text(formatAmount(expectedMrr), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = CyberSuccess)
                        }
                        Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                            Text("At Risk", fontSize = 12.sp, color = CyberTextMuted)
                            Text(formatAmount(atRiskMrr), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = CyberDanger)
                        }
                    }
                    Box(
                        modifier = Modifier.fillMaxWidth().height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(CyberBgCardElevated)
                    ) {
                        if (forecastPct > 0f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(forecastPct)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(CyberAccent)
                            )
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${(forecastPct * 100).toInt()}% collection confidence",
                            fontSize = 12.sp, color = CyberTextSecondary)
                        Text("$activeCount active mandates",
                            fontSize = 12.sp, color = CyberTextMuted)
                    }
                }
            }
        }

        // Filter chips
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(horizontal = 20.dp)) {
                items(filters) { filter ->
                    val isActive = filter == activeFilter
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (isActive) CyberAccent.copy(alpha = 0.12f) else CyberBgCard)
                            .clickable { activeFilter = filter }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            filter,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isActive) CyberAccent else CyberTextSecondary
                        )
                    }
                }
            }
        }

        // Section header
        item {
            Text("Collection Queue", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = CyberTextPrimary, modifier = Modifier.padding(top = 4.dp))
        }

        if (filteredPayments.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                    Text("No payments in this category", color = CyberTextSecondary, fontSize = 14.sp)
                }
            }
        }

        items(filteredPayments, key = { it.id }) { payment ->
            val client = clients.find { it.id == payment.clientId }
            PaymentCard(
                payment = payment,
                clientName = client?.name ?: "Unknown",
                clientPhone = client?.phoneNumber ?: ""
            )
        }
    }
}

@Composable
fun PaymentCard(payment: Payment, clientName: String, clientPhone: String) {
    val context = LocalContext.current
    val formatter = SimpleDateFormat("MMM dd", Locale.getDefault())
    val isFailed = payment.mandateStatus == "FAILED"
    val isExpiring = payment.mandateStatus == "EXPIRING"
    val computedDaysOverdue = remember(payment.dueDateMillis) { maxOf(0, ((System.currentTimeMillis() - payment.dueDateMillis) / 86400000L).toInt()) }
    val isOverdue = computedDaysOverdue > 0 && !isFailed

    val statusColor = when {
        isFailed -> CyberDanger
        isOverdue && computedDaysOverdue > 7 -> CyberDanger
        isOverdue || isExpiring -> CyberWarning
        else -> CyberSuccess
    }
    val statusText = when {
        isFailed -> "Payment failed"
        isOverdue -> "$computedDaysOverdue days overdue"
        isExpiring -> "Mandate expiring"
        else -> "Due ${formatter.format(Date(payment.dueDateMillis))}"
    }
    val amountDisplay = "₹${"%,d".format(payment.amount)}"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(CyberBgCard)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Left status bar
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(44.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(statusColor)
        )

        // Status icon
        Box(
            modifier = Modifier.size(40.dp).clip(CircleShape).background(statusColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            val icon = when {
                isFailed -> Icons.Filled.Warning
                isExpiring -> Icons.Filled.CreditCard
                else -> Icons.Filled.AttachMoney
            }
            Icon(icon, contentDescription = null, tint = statusColor, modifier = Modifier.size(18.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(clientName, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = CyberTextPrimary)
            Text(statusText, fontSize = 12.sp, color = statusColor, fontWeight = FontWeight.SemiBold)
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(amountDisplay, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = CyberTextPrimary)
            Spacer(Modifier.height(6.dp))

            // Action button
            val buttonLabel = if (isFailed) "Retry" else "Send Link"
            val buttonIcon = if (isFailed) Icons.Filled.Refresh else Icons.AutoMirrored.Filled.Send
            val waMsg = if (isFailed)
                "Hi $clientName, your payment of $amountDisplay failed. Could you please retry? I'll send you the link."
            else
                "Hi $clientName, here's your payment link for $amountDisplay. Please complete at your earliest: [link]"

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(CyberAccent)
                    .clickable {
                        val url = if (clientPhone.isNotEmpty())
                            "https://wa.me/91$clientPhone?text=${android.net.Uri.encode(waMsg)}"
                        else "https://wa.me/?text=${android.net.Uri.encode(waMsg)}"
                        try { context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))) } catch (_: Exception) {}
                    }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(buttonIcon, contentDescription = null, tint = CyberAccentDark, modifier = Modifier.size(12.dp))
                Text(buttonLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CyberAccentDark)
            }
        }
    }
}

private fun formatAmount(amount: Int): String = when {
    amount >= 100000 -> "₹%.2fL".format(amount / 100000f)
    amount >= 1000 -> "₹${amount / 1000}K"
    else -> "₹$amount"
}

@Composable
fun MrrTrendChart(snapshots: List<RevenueSnapshot>) {
    // Snapshots are DESC (newest first) — reverse to get oldest→newest left→right
    val sorted = snapshots.reversed()
    val maxMrr = sorted.maxOf { it.totalMrr }.coerceAtLeast(1)
    val accentColor = androidx.compose.ui.graphics.Color(0xFFC5F23E)
    val mutedColor = androidx.compose.ui.graphics.Color(0xFF3A3A3A)

    Box(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(CyberBgCard).padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("MRR TREND", fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                color = CyberTextMuted, letterSpacing = 0.8.sp)

            Canvas(modifier = Modifier.fillMaxWidth().height(80.dp)) {
                val w = size.width
                val h = size.height
                val n = sorted.size
                if (n < 2) return@Canvas

                val step = w / (n - 1).toFloat()
                val points = sorted.mapIndexed { i, snap ->
                    Offset(i * step, h - (snap.totalMrr.toFloat() / maxMrr) * h * 0.9f)
                }

                // Draw bar columns (subtle fill)
                sorted.forEachIndexed { i, snap ->
                    val barW = (step * 0.4f).coerceAtLeast(8f)
                    val barH = (snap.totalMrr.toFloat() / maxMrr) * h * 0.9f
                    drawRect(
                        color = accentColor.copy(alpha = 0.15f),
                        topLeft = Offset(i * step - barW / 2, h - barH),
                        size = androidx.compose.ui.geometry.Size(barW, barH)
                    )
                }

                // Draw trend line
                val path = Path().apply {
                    moveTo(points[0].x, points[0].y)
                    for (i in 1 until points.size) {
                        val cpX = (points[i - 1].x + points[i].x) / 2
                        cubicTo(cpX, points[i - 1].y, cpX, points[i].y, points[i].x, points[i].y)
                    }
                }
                drawPath(path, color = accentColor, style = Stroke(width = 3f, cap = StrokeCap.Round))

                // Dots
                points.forEach { pt ->
                    drawCircle(accentColor, radius = 5f, center = pt)
                    drawCircle(mutedColor, radius = 2.5f, center = pt)
                }
            }

            // Month labels
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                sorted.forEach { snap ->
                    val label = snap.monthYear.takeLast(5).replace("-", "/")
                    Text(label, fontSize = 9.sp, color = CyberTextMuted, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun BillingChip(text: String, textColor: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(textColor.copy(alpha = 0.15f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(text, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = textColor)
    }
}

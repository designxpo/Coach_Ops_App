package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.rememberDatePickerState
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
import com.example.ui.theme.CyberAccent
import com.example.ui.theme.CyberAccentDark
import com.example.ui.theme.CyberBgCard
import com.example.ui.theme.CyberBgCardElevated
import com.example.ui.theme.CyberBgPrimary
import com.example.ui.theme.CyberSuccess
import com.example.ui.theme.CyberTextMuted
import com.example.ui.theme.CyberTextPrimary
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import com.example.ui.theme.CyberTextSecondary
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import com.example.data.isCertVerified
import com.example.data.isFeatured
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

@Composable
fun TrainerDetailScreen(
    viewModel: ClientViewModel,
    trainerId: String,
    onBack: () -> Unit
) {
    val trainer by viewModel.selectedTrainer.collectAsState()

    var requestNotes by remember { mutableStateOf("") }
    var bookingState by remember { mutableStateOf<BookingState>(BookingState.Idle) }
    var sessionDateMillis by remember { mutableStateOf(0L) }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis >= System.currentTimeMillis() - 86400000L
        }
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    sessionDateMillis = datePickerState.selectedDateMillis ?: 0L
                    showDatePicker = false
                }) { Text("OK", color = CyberAccent) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel", color = CyberTextMuted) }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    LaunchedEffect(trainerId) {
        viewModel.loadTrainerById(trainerId)
    }

    if (trainer == null) {
        Box(Modifier.fillMaxSize().background(CyberBgPrimary), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = CyberAccent)
        }
        return
    }

    val t = trainer!!
    val days = t.availabilityDays.split(",").filter { it.isNotBlank() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBgPrimary)
            .statusBarsPadding()
    ) {
        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(CyberBgCard).clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = CyberTextPrimary, modifier = Modifier.size(18.dp))
            }
            Text(t.name, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = CyberTextPrimary, modifier = Modifier.weight(1f))
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            // Profile header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(CyberAccent.copy(alpha = 0.12f))
                        .border(2.dp, CyberAccent.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (t.profileImageUrl.isNotBlank()) {
                        AsyncImage(
                            model = t.profileImageUrl,
                            contentDescription = t.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().clip(CircleShape)
                        )
                    } else {
                        Text(t.name.take(1).uppercase(), fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = CyberAccent)
                    }
                }
                Column {
                    Text(t.name, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = CyberTextPrimary)
                    val specs = t.specialty.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    if (specs.isEmpty()) {
                        Text("Personal Trainer", fontSize = 13.sp, color = CyberAccent, fontWeight = FontWeight.SemiBold)
                    } else {
                        androidx.compose.foundation.layout.FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            specs.forEach { spec ->
                                Box(
                                    modifier = Modifier
                                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(999.dp))
                                        .background(CyberAccent.copy(0.12f))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(spec, fontSize = 11.sp, color = CyberAccent, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (t.city.isNotBlank()) {
                            Text("📍 ${t.city.replaceFirstChar { it.uppercase() }}", fontSize = 12.sp, color = CyberTextMuted)
                        }
                        if (t.rating > 0f) {
                            Text("⭐ ${"%.1f".format(t.rating)}", fontSize = 12.sp, color = CyberTextPrimary, fontWeight = FontWeight.SemiBold)
                        }
                        if (t.yearsExperience > 0) {
                            Text("🏅 ${t.yearsExperience} yrs", fontSize = 12.sp, color = CyberTextMuted)
                        }
                    }
                }
            }

            if (t.headline.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(t.headline, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = CyberTextSecondary, lineHeight = 19.sp)
            }

            // Trust signals — the at-a-glance credibility bar
            val trustChips = buildList {
                if (t.isFeatured) add("⚡ Featured" to true)
                if (t.profileScore >= com.example.data.PortfolioScoring.TIER_ELITE) add("🏆 Elite Profile" to true)
                when {
                    t.isCertVerified                 -> add("✓ Certificate Verified" to true)
                    t.certStatus == "pending"        -> add("Certificate under review" to false)
                    t.certifications.isNotBlank()    -> add("Certified (self-declared)" to false)
                    t.yearsExperience > 0 || t.mentorship.isNotBlank() ->
                        add("Experience self-declared" to false)
                }
                if (t.cprCertified) add("✚ CPR / First-aid" to false)
                if (t.assessmentIncluded) add("Assessment included" to false)
                t.trainingModes.split(",").map { it.trim() }.filter { it.isNotBlank() }.forEach { add(it to false) }
                if (t.languages.isNotBlank()) add("🗣 ${t.languages}" to false)
            }
            if (trustChips.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                androidx.compose.foundation.layout.FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    trustChips.forEach { (label, highlight) -> TrustChip(label, highlight) }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Bio
            if (t.bio.isNotBlank()) {
                SectionCard {
                    Text("About", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CyberTextMuted)
                    Spacer(Modifier.height(8.dp))
                    Text(t.bio, fontSize = 14.sp, color = CyberTextPrimary, lineHeight = 20.sp)
                }
                Spacer(Modifier.height(12.dp))
            }

            // Credentials & experience
            val hasCredentials = t.education.isNotBlank() || t.certifications.isNotBlank() ||
                t.mentorship.isNotBlank() || t.gymsWorked.isNotBlank() ||
                t.clientsCoached > 0 || t.clientTypes.isNotBlank()
            if (hasCredentials) {
                SectionCard {
                    Text("Credentials & Experience", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CyberTextMuted)
                    Spacer(Modifier.height(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (t.education.isNotBlank()) DetailRow("Education", t.education)
                        if (t.certifications.isNotBlank()) DetailRow("Certifications", t.certifications)
                        if (t.mentorship.isNotBlank()) DetailRow("Trained under", "${t.mentorship} (self-declared)")
                        if (t.gymsWorked.isNotBlank()) DetailRow("Gyms worked at", t.gymsWorked)
                        if (t.clientsCoached > 0) DetailRow("Clients coached", "${t.clientsCoached}+ (self-declared)")
                        if (t.clientTypes.isNotBlank()) DetailRow("Trains", t.clientTypes)
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // Coaching approach
            if (t.workDescription.isNotBlank() || t.nutritionSupport.isNotBlank() || t.assessmentIncluded) {
                SectionCard {
                    Text("Coaching Approach", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CyberTextMuted)
                    Spacer(Modifier.height(8.dp))
                    if (t.workDescription.isNotBlank()) {
                        Text(t.workDescription, fontSize = 14.sp, color = CyberTextPrimary, lineHeight = 20.sp)
                        Spacer(Modifier.height(8.dp))
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (t.assessmentIncluded) DetailRow("First session", "Fitness assessment included")
                        if (t.nutritionSupport.isNotBlank()) DetailRow("Nutrition support", t.nutritionSupport)
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // What clients say — verified member reviews first (rating flow),
            // the coach's self-added quotes only as fallback
            val memberReviews by viewModel.coachReviews.collectAsState()
            val realReviews = memberReviews.filter { it.review.isNotBlank() }
            if (realReviews.isNotEmpty()) {
                SectionCard {
                    Text("What Clients Say", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CyberTextMuted)
                    Spacer(Modifier.height(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        realReviews.take(5).forEach { r ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(CyberBgCardElevated)
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("⭐".repeat(r.rating.toInt().coerceIn(1, 5)), fontSize = 11.sp)
                                    Text(r.clientName, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CyberTextMuted,
                                        maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                    Box(
                                        Modifier.clip(RoundedCornerShape(999.dp))
                                            .background(CyberSuccess.copy(0.12f))
                                            .padding(horizontal = 6.dp, vertical = 1.dp)
                                    ) {
                                        Text("✓ Booked here", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = CyberSuccess,
                                            maxLines = 1, softWrap = false)
                                    }
                                }
                                Text(r.review, fontSize = 13.sp, color = CyberTextSecondary, lineHeight = 18.sp)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            } else {
                val quotes = t.testimonials.split("\n").filter { it.isNotBlank() }
                if (quotes.isNotEmpty()) {
                    SectionCard {
                        Text("What Clients Say", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CyberTextMuted)
                        Spacer(Modifier.height(10.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            quotes.forEach { q ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(CyberBgCardElevated)
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("❝", fontSize = 14.sp, color = CyberAccent)
                                    Text(q, fontSize = 13.sp, color = CyberTextSecondary, lineHeight = 18.sp, modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }

            // Instagram
            if (t.instagramUrl.isNotBlank()) {
                val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(CyberBgCard)
                        .border(1.dp, Color.White.copy(0.06f), RoundedCornerShape(14.dp))
                        .clickable {
                            val raw = t.instagramUrl.trim()
                            val url = when {
                                raw.startsWith("http") -> raw
                                raw.startsWith("@")    -> "https://instagram.com/${raw.drop(1)}"
                                else                   -> "https://$raw"
                            }
                            runCatching { uriHandler.openUri(url) }
                        }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("📸", fontSize = 16.sp)
                    Text("See more on Instagram", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = CyberTextPrimary, modifier = Modifier.weight(1f))
                    Text("→", fontSize = 14.sp, color = CyberTextMuted)
                }
                Spacer(Modifier.height(12.dp))
            }

            // Portfolio gallery
            val portfolioUrls = t.portfolioImages.split(",").filter { it.isNotBlank() }
            if (portfolioUrls.isNotEmpty()) {
                SectionCard {
                    Text("Work Photos", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CyberTextMuted)
                    Spacer(Modifier.height(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(portfolioUrls) { url ->
                            AsyncImage(
                                model = url,
                                contentDescription = "Portfolio",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(width = 140.dp, height = 100.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // Fees
            SectionCard {
                Text("Pricing", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CyberTextMuted)
                Spacer(Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (t.feePerSession > 0) {
                        FeeBox(label = "Per Session", amount = "₹${t.feePerSession}", modifier = Modifier.weight(1f))
                    }
                    if (t.feeMonthly > 0) {
                        FeeBox(label = "Monthly", amount = "₹${t.feeMonthly}", modifier = Modifier.weight(1f))
                    }
                }
            }
            Spacer(Modifier.height(12.dp))

            // Availability
            if (days.isNotEmpty()) {
                SectionCard {
                    Text("Availability", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CyberTextMuted)
                    Spacer(Modifier.height(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val allDays = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
                        items(allDays) { day ->
                            val active = day in days
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (active) CyberSuccess.copy(0.15f) else CyberBgCardElevated)
                                    .border(1.dp, if (active) CyberSuccess.copy(0.4f) else Color.White.copy(0.06f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    day.take(1),
                                    fontSize = 11.sp, fontWeight = FontWeight.ExtraBold,
                                    color = if (active) CyberSuccess else CyberTextMuted
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // Book session
            SectionCard {
                Text("Request a Session", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CyberTextMuted)
                Spacer(Modifier.height(12.dp))

                // Date picker row
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(CyberBgCardElevated)
                        .border(1.dp, if (sessionDateMillis > 0) CyberAccent.copy(0.4f) else Color.White.copy(0.06f), RoundedCornerShape(14.dp))
                        .clickable { showDatePicker = true }
                        .padding(horizontal = 14.dp, vertical = 14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Filled.CalendarMonth, null, tint = if (sessionDateMillis > 0) CyberAccent else CyberTextMuted, modifier = Modifier.size(18.dp))
                        Text(
                            if (sessionDateMillis > 0)
                                SimpleDateFormat("EEE, MMM d yyyy", Locale.getDefault()).format(Date(sessionDateMillis))
                            else "Pick a preferred session date",
                            fontSize = 14.sp,
                            color = if (sessionDateMillis > 0) CyberTextPrimary else CyberTextMuted,
                            fontWeight = if (sessionDateMillis > 0) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(CyberBgCardElevated)
                        .border(1.dp, Color.White.copy(0.06f), RoundedCornerShape(14.dp))
                        .height(80.dp)
                        .padding(14.dp)
                ) {
                    BasicTextField(
                        value = requestNotes,
                        onValueChange = { requestNotes = it },
                        modifier = Modifier.fillMaxSize(),
                        textStyle = TextStyle(color = CyberTextPrimary, fontSize = 14.sp),
                        cursorBrush = SolidColor(CyberAccent),
                        decorationBox = { inner ->
                            if (requestNotes.isEmpty()) Text("Describe your goals or preferred schedule...", fontSize = 14.sp, color = CyberTextMuted)
                            inner()
                        }
                    )
                }
                Spacer(Modifier.height(12.dp))

                when (bookingState) {
                    BookingState.Success -> {
                        Box(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                                .background(CyberSuccess.copy(0.12f))
                                .border(1.dp, CyberSuccess.copy(0.3f), RoundedCornerShape(14.dp))
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("✓  Request sent! Coach will confirm soon.", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = CyberSuccess)
                        }
                    }
                    BookingState.Loading -> {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = CyberAccent, modifier = Modifier.size(24.dp))
                        }
                    }
                    else -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(CyberAccent)
                                .clickable {
                                    bookingState = BookingState.Loading
                                    viewModel.requestBooking(t, requestNotes, sessionDateMillis) { success ->
                                        bookingState = if (success) BookingState.Success else BookingState.Error
                                    }
                                }
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Send Request", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = CyberAccentDark)
                        }
                        if (bookingState == BookingState.Error) {
                            Spacer(Modifier.height(8.dp))
                            Text("Failed to send. Please try again.", fontSize = 12.sp, color = com.example.ui.theme.CyberDanger)
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

private sealed interface BookingState {
    object Idle : BookingState
    object Loading : BookingState
    object Success : BookingState
    object Error : BookingState
}

@Composable
private fun TrustChip(label: String, highlight: Boolean) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (highlight) CyberAccent.copy(0.15f) else CyberBgCard)
            .border(1.dp, if (highlight) CyberAccent.copy(0.4f) else Color.White.copy(0.08f), RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            label, fontSize = 11.sp, fontWeight = FontWeight.Bold,
            maxLines = 1, softWrap = false,
            color = if (highlight) CyberAccent else CyberTextSecondary
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, fontSize = 12.sp, color = CyberTextMuted, modifier = Modifier.weight(0.36f))
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = CyberTextPrimary,
            lineHeight = 16.sp, modifier = Modifier.weight(0.64f))
    }
}

@Composable
private fun SectionCard(content: @Composable ColumnScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CyberBgCard)
            .border(1.dp, Color.White.copy(0.06f), RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Column { content() }
    }
}

@Composable
private fun FeeBox(label: String, amount: String, modifier: Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(CyberAccent.copy(0.08f))
            .border(1.dp, CyberAccent.copy(0.2f), RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Column {
            Text(amount, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = CyberAccent)
            Text(label, fontSize = 11.sp, color = CyberTextMuted, fontWeight = FontWeight.SemiBold)
        }
    }
}

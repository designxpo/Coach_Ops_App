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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.AuthRepository
import com.example.data.SubscriptionPlan
import com.example.data.UserPreferences
import com.example.ui.theme.CyberAccent
import com.example.ui.theme.CyberAccentDark
import com.example.ui.theme.CyberBgCard
import com.example.ui.theme.CyberBgCardElevated
import com.example.ui.theme.CyberBgPrimary
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import com.example.ui.theme.CyberDanger
import com.example.ui.theme.CyberSuccess
import com.example.ui.theme.CyberTextMuted
import com.example.ui.theme.CyberTextPrimary
import com.example.ui.theme.CyberTextSecondary

@Composable
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
fun ProfileScreen(
    viewModel: MainViewModel,
    userPreferences: UserPreferences,
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onPrivacyPolicyClick: () -> Unit = {},
    onTermsClick: () -> Unit = {},
    onDeleteAccountClick: () -> Unit = {},
    onAdminAccess: () -> Unit = {},  // reserved for future deep-link
    onManagePlanClick: () -> Unit = {}
) {
    val clients by viewModel.clients.collectAsStateWithLifecycle()
    val programs by viewModel.programs.collectAsStateWithLifecycle()
    val coachName      by viewModel.coachName.collectAsStateWithLifecycle()
    val coachPhone     by viewModel.coachPhone.collectAsStateWithLifecycle()
    val coachSpecialty by viewModel.coachSpecialty.collectAsStateWithLifecycle()
    val currentPlan    by viewModel.currentPlan.collectAsStateWithLifecycle()

    val totalMrr = clients.sumOf { it.mrr }

    val mrrDisplay = when {
        totalMrr >= 100000 -> "₹%.2fL".format(totalMrr / 100000f)
        totalMrr >= 1000 -> "₹${totalMrr / 1000}K"
        else -> "₹$totalMrr"
    }

    var showEditSheet by remember { mutableStateOf(false) }
    var showMarketplaceSheet by remember { mutableStateOf(false) }
    var showReportSheet by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var publishBanner by remember { mutableStateOf("") }   // "" = hidden, else message
    var publishBannerOk by remember { mutableStateOf(true) }
    val publishState by viewModel.publishState.collectAsState()

    LaunchedEffect(publishState) {
        when (publishState) {
            true -> {
                showMarketplaceSheet = false
                publishBannerOk = true
                publishBanner = if (userPreferences.trainerIsPublic)
                    "Profile is now live on marketplace ✓"
                else
                    "Profile hidden from marketplace ✓"
                viewModel.clearPublishState()
            }
            false -> {
                showMarketplaceSheet = false
                publishBannerOk = false
                publishBanner = "Failed to update — check connection and try again"
                viewModel.clearPublishState()
            }
            null -> { /* idle */ }
        }
    }

    val firebaseEmail = AuthRepository.currentUser?.email ?: userPreferences.coachEmail

    if (showMarketplaceSheet) {
        PublicProfileSheet(
            initialIsPublic = userPreferences.trainerIsPublic,
            initialCity = userPreferences.trainerCity,
            initialBio = userPreferences.trainerBio,
            initialWorkDescription = userPreferences.trainerWorkDescription,
            initialFeeSession = userPreferences.trainerFeePerSession,
            initialFeeMonthly = userPreferences.trainerFeeMonthly,
            initialAvailDays = userPreferences.trainerAvailabilityDays,
            initialYearsExp = userPreferences.trainerYearsExperience,
            initialProfileImageUrl = userPreferences.trainerProfileImageUrl,
            initialPortfolioImages = userPreferences.trainerPortfolioImages,
            onDismiss = { showMarketplaceSheet = false },
            onSave = { isPublic, city, bio, workDesc, feeSession, feeMonthly, availDays, yearsExp, profileImg, portfolioImgs, lat, lng ->
                viewModel.updatePublicProfile(isPublic, city, bio, feeSession, feeMonthly, availDays, yearsExp, profileImg, portfolioImgs, lat, lng, workDesc)
            }
        )
    }

    if (showEditSheet) {
        EditProfileSheet(
            name = coachName,
            email = firebaseEmail,
            phone = coachPhone,
            specialty = coachSpecialty,
            clientRange = userPreferences.coachClientRange,
            challenge = userPreferences.coachChallenge,
            onDismiss = { showEditSheet = false },
            onSave = { name, email, phone, specialty, clientRange, challenge ->
                viewModel.updateProfile(name, email, phone, specialty, clientRange, challenge)
                showEditSheet = false
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBgPrimary)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(CyberBgCard)
                        .clickable { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back",
                        tint = CyberTextPrimary, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(12.dp))
                Text("Profile", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = CyberTextPrimary,
                    modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(CyberAccent)
                        .clickable { showEditSheet = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit",
                        tint = CyberAccentDark, modifier = Modifier.size(16.dp))
                }
            }
        }

        // Publish result banner
        if (publishBanner.isNotEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (publishBannerOk) CyberSuccess.copy(alpha = 0.12f)
                            else CyberDanger.copy(alpha = 0.12f)
                        )
                        .border(
                            1.dp,
                            if (publishBannerOk) CyberSuccess.copy(alpha = 0.35f)
                            else CyberDanger.copy(alpha = 0.35f),
                            RoundedCornerShape(14.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            publishBanner,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (publishBannerOk) CyberSuccess else CyberDanger,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            "✕",
                            fontSize = 14.sp,
                            color = CyberTextMuted,
                            modifier = Modifier.clickable { publishBanner = "" }.padding(start = 12.dp)
                        )
                    }
                }
            }
        }

        // Avatar + name card
        item {
            var photoUrl by remember { mutableStateOf(userPreferences.profilePhotoUrl) }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(CyberAccent)
                    .padding(24.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    ProfileAvatarPicker(
                        photoUrl         = photoUrl,
                        initials         = coachName.firstOrNull()?.toString() ?: "C",
                        size             = 90.dp,
                        borderColor      = CyberAccentDark,
                        initialsColor    = CyberAccentDark,
                        initialsBg       = CyberAccentDark.copy(0.15f),
                        userPreferences  = userPreferences,
                        onPhotoUploaded  = { url -> photoUrl = url }
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        coachName.ifEmpty { "Coach" },
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = CyberAccentDark
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        firebaseEmail.ifEmpty { "No email set" },
                        fontSize = 13.sp,
                        color = Color(0xFF0A0A0A).copy(alpha = 0.6f)
                    )
                    // Show all specialties as chips
                    val specialtyList = coachSpecialty.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    if (specialtyList.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        androidx.compose.foundation.layout.FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            specialtyList.forEach { spec ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(CyberAccentDark.copy(alpha = 0.12f))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(spec, fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold, color = CyberAccentDark)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color(currentPlan.color).copy(alpha = 0.18f))
                            .clickable { onManagePlanClick() }
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            "${currentPlan.emoji} ${currentPlan.displayName} Plan · Upgrade ›",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(currentPlan.color)
                        )
                    }
                }
            }
        }

        // Stats row
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ProfileStatCard(modifier = Modifier.weight(1f), value = "${clients.size}", label = "Members")
                ProfileStatCard(modifier = Modifier.weight(1f), value = mrrDisplay, label = "MRR")
                ProfileStatCard(modifier = Modifier.weight(1f), value = "${programs.size}", label = "Programs")
            }
        }

        // About section
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(CyberBgCard)
                    .padding(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Profile Details", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = CyberTextPrimary)

                    listOf(
                        "Phone" to coachPhone.ifEmpty { "Not set" },
                        "Specialties" to coachSpecialty.split(",").map { it.trim() }.filter { it.isNotEmpty() }.let {
                            if (it.isEmpty()) "Not set" else it.take(2).joinToString(", ") + if (it.size > 2) " +${it.size - 2}" else ""
                        },
                        "Client range" to userPreferences.coachClientRange.ifEmpty { "Not set" },
                        "Main focus" to userPreferences.coachChallenge.ifEmpty { "Not set" }
                    ).forEach { (label, value) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(label, fontSize = 13.sp, color = CyberTextMuted)
                            Text(value, fontSize = 13.sp, color = CyberTextSecondary, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // Marketplace
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(CyberBgCard)
                    .padding(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Marketplace", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = CyberTextPrimary)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(if (userPreferences.trainerIsPublic) CyberSuccess.copy(alpha = 0.12f) else CyberBgCardElevated)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                if (userPreferences.trainerIsPublic) "Public" else "Hidden",
                                fontSize = 11.sp, fontWeight = FontWeight.Bold,
                                color = if (userPreferences.trainerIsPublic) CyberSuccess else CyberTextMuted
                            )
                        }
                    }

                    if (userPreferences.trainerIsPublic) {
                        val daysCount = userPreferences.trainerAvailabilityDays
                            .split(",").filter { it.isNotBlank() }.size
                        listOf(
                            "City" to userPreferences.trainerCity.ifEmpty { "Not set" },
                            "Session fee" to if (userPreferences.trainerFeePerSession > 0) "₹${userPreferences.trainerFeePerSession}" else "Not set",
                            "Experience" to if (userPreferences.trainerYearsExperience > 0) "${userPreferences.trainerYearsExperience} yrs" else "Not set",
                            "Available" to if (daysCount > 0) "$daysCount days/week" else "Not set"
                        ).forEach { (label, value) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(label, fontSize = 13.sp, color = CyberTextMuted)
                                Text(value, fontSize = 13.sp, color = CyberTextSecondary, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    } else {
                        Text(
                            "Go public so clients can discover and book you.",
                            fontSize = 13.sp, color = CyberTextMuted, lineHeight = 18.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(CyberAccent.copy(alpha = 0.10f))
                            .border(1.dp, CyberAccent.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                            .clickable { showMarketplaceSheet = true }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (userPreferences.trainerIsPublic) "Edit Marketplace Profile" else "Set Up Marketplace Profile",
                            fontSize = 14.sp, fontWeight = FontWeight.Bold, color = CyberAccent
                        )
                    }
                }
            }
        }

        // Support
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(CyberBgCard)
                    .padding(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                    Text("Support", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = CyberTextPrimary,
                        modifier = Modifier.padding(bottom = 12.dp))
                    LegalLinkRow(label = "Report a Problem", onClick = { showReportSheet = true })
                }
            }
        }

        // Legal
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(CyberBgCard)
                    .padding(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                    Text("Legal", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = CyberTextPrimary,
                        modifier = Modifier.padding(bottom = 12.dp))
                    LegalLinkRow(label = "Privacy Policy", onClick = onPrivacyPolicyClick)
                    HorizontalDivider(color = CyberBgCardElevated, modifier = Modifier.padding(vertical = 4.dp))
                    LegalLinkRow(label = "Terms of Service", onClick = onTermsClick)
                    HorizontalDivider(color = CyberBgCardElevated, modifier = Modifier.padding(vertical = 4.dp))
                    LegalLinkRow(label = "Delete My Account", onClick = { showDeleteDialog = true }, danger = true)
                }
            }
        }

        // Logout
        item {
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CyberDanger.copy(alpha = 0.10f))
                    .border(1.dp, CyberDanger.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                    .clickable { onLogout() }
                    .padding(vertical = 18.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null,
                        tint = CyberDanger, modifier = Modifier.size(18.dp))
                    Text("Sign Out", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = CyberDanger)
                }
            }
        }

        item {
            Spacer(Modifier.height(8.dp))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    "ProCoach India v${com.example.BuildConfig.VERSION_NAME}  ·  Build ${com.example.BuildConfig.VERSION_CODE}",
                    fontSize = 11.sp, color = CyberTextMuted
                )
            }
        }
    }

    if (showReportSheet) ReportIssueSheet(onDismiss = { showReportSheet = false })
    if (showDeleteDialog) DeleteAccountDialog(
        onDismiss = { showDeleteDialog = false },
        onDeleted = onLogout   // session is gone — reuse logout to wipe local state + navigate
    )
}

@Composable
private fun ProfileStatCard(modifier: Modifier, value: String, label: String) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(CyberBgCard)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = CyberAccent)
            Spacer(Modifier.height(2.dp))
            Text(label, fontSize = 11.sp, color = CyberTextMuted, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun LegalLinkRow(label: String, onClick: () -> Unit, danger: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 14.sp, color = if (danger) CyberDanger else CyberTextSecondary)
        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null,
            tint = if (danger) CyberDanger else CyberTextMuted, modifier = Modifier.size(16.dp))
    }
}

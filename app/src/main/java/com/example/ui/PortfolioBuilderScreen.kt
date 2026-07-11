@file:OptIn(
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.example.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.PortfolioScoring
import com.example.data.SubscriptionPlan
import com.example.data.TrainerProfile
import com.example.data.UserPreferences
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Chip option catalogs — kept short and India-relevant. Custom values already
// saved on a profile are merged in so nothing a coach picked ever disappears.
private val SPECIALIZATION_OPTIONS = listOf(
    "Weight Loss", "Muscle Gain", "Strength", "Functional", "Yoga",
    "Sports Performance", "Rehab & Mobility", "Women's Fitness",
    "Senior Fitness", "Bodybuilding", "CrossFit", "Zumba / Dance"
)
private val CLIENT_TYPE_OPTIONS = listOf(
    "Beginners", "Intermediate", "Advanced", "Women", "Seniors", "Teens", "Athletes", "Post-injury"
)
private val MODE_OPTIONS = listOf("Gym", "Home", "Online", "Outdoor")
private val LANGUAGE_OPTIONS = listOf(
    "Hindi", "English", "Marathi", "Tamil", "Telugu", "Kannada",
    "Bengali", "Gujarati", "Punjabi", "Malayalam"
)
private val EDUCATION_OPTIONS = listOf(
    "12th Pass", "Graduate", "Post-Graduate", "B.P.Ed / M.P.Ed", "Diploma", "Other"
)
private val NUTRITION_OPTIONS = listOf("Workout only", "Diet guidance", "Meal planning")
private val ALL_DAYS = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")

private const val MAX_SPECIALIZATIONS = 4

@Composable
fun PortfolioBuilderScreen(
    viewModel: MainViewModel,
    userPreferences: UserPreferences,
    onBack: () -> Unit,
    onUpgradeClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }

    // ── Form state ───────────────────────────────────────────────────────────
    var isPublic by remember { mutableStateOf(userPreferences.trainerIsPublic) }
    var profileImageUrl by remember { mutableStateOf("") }
    var headline by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var specializations by remember { mutableStateOf(setOf<String>()) }
    var clientTypes by remember { mutableStateOf(setOf<String>()) }
    var modes by remember { mutableStateOf(setOf<String>()) }
    var languages by remember { mutableStateOf(setOf<String>()) }
    var education by remember { mutableStateOf("") }
    var certifications by remember { mutableStateOf("") }
    var certDocUrl by remember { mutableStateOf("") }
    var certStatus by remember { mutableStateOf("") }
    var uploadingCert by remember { mutableStateOf(false) }
    var mentorship by remember { mutableStateOf("") }
    var cprCertified by remember { mutableStateOf(false) }
    var yearsExpText by remember { mutableStateOf("") }
    var gymsWorked by remember { mutableStateOf("") }
    var clientsCoachedText by remember { mutableStateOf("") }
    var workDescription by remember { mutableStateOf("") }
    var assessmentIncluded by remember { mutableStateOf(false) }
    var nutritionSupport by remember { mutableStateOf("") }
    var portfolioImageUrls by remember { mutableStateOf(listOf<String>()) }
    var reviewCount by remember { mutableStateOf(0) }
    var reviewAvg by remember { mutableStateOf(0f) }
    var instagramUrl by remember { mutableStateOf("") }
    var feeSessionText by remember { mutableStateOf("") }
    var feeMonthlyText by remember { mutableStateOf("") }
    var selectedDays by remember { mutableStateOf(setOf<String>()) }

    // Location: keep prior coordinates unless the coach changes city or pins GPS
    var initialCity by remember { mutableStateOf("") }
    var savedLat by remember { mutableStateOf(0.0) }
    var savedLng by remember { mutableStateOf(0.0) }
    var gpsLat by remember { mutableStateOf(0.0) }
    var gpsLng by remember { mutableStateOf(0.0) }
    var gpsLocated by remember { mutableStateOf(false) }
    var isGpsLocating by remember { mutableStateOf(false) }

    var uploadingProfile by remember { mutableStateOf(false) }
    var uploadingPortfolio by remember { mutableStateOf(-1) }
    var uploadError by remember { mutableStateOf("") }
    var pendingSlot by remember { mutableStateOf(-1) }

    LaunchedEffect(Unit) {
        val p = viewModel.loadMyPortfolio()
        profileImageUrl = p.profileImageUrl
        headline = p.headline
        bio = p.bio
        city = p.city
        initialCity = p.city
        savedLat = p.lat
        savedLng = p.lng
        specializations = p.specialty.splitToSet()
        clientTypes = p.clientTypes.splitToSet()
        modes = p.trainingModes.splitToSet()
        languages = p.languages.splitToSet()
        education = p.education
        certifications = p.certifications
        certDocUrl = p.certDocUrl
        certStatus = p.certStatus
        mentorship = p.mentorship
        cprCertified = p.cprCertified
        yearsExpText = if (p.yearsExperience > 0) p.yearsExperience.toString() else ""
        gymsWorked = p.gymsWorked
        clientsCoachedText = if (p.clientsCoached > 0) p.clientsCoached.toString() else ""
        workDescription = p.workDescription
        assessmentIncluded = p.assessmentIncluded
        nutritionSupport = p.nutritionSupport
        portfolioImageUrls = p.portfolioImages.split(",").filter { it.isNotBlank() }
        reviewCount = p.ratingCount
        reviewAvg = if (p.ratingCount > 0) p.ratingSum / p.ratingCount else 0f
        instagramUrl = p.instagramUrl
        feeSessionText = if (p.feePerSession > 0) p.feePerSession.toString() else ""
        feeMonthlyText = if (p.feeMonthly > 0) p.feeMonthly.toString() else ""
        selectedDays = p.availabilityDays.splitToSet()
        loading = false
    }

    fun buildDraft() = TrainerProfile(
        name = userPreferences.coachName,
        specialty = specializations.joinToString(", "),
        bio = bio.trim(),
        workDescription = workDescription.trim(),
        city = city.trim(),
        feePerSession = feeSessionText.toIntOrNull() ?: 0,
        feeMonthly = feeMonthlyText.toIntOrNull() ?: 0,
        availabilityDays = selectedDays.sortedBy { ALL_DAYS.indexOf(it) }.joinToString(","),
        yearsExperience = yearsExpText.toIntOrNull() ?: 0,
        profileImageUrl = profileImageUrl,
        portfolioImages = portfolioImageUrls.joinToString(","),
        headline = headline.trim(),
        languages = languages.joinToString(", "),
        education = education,
        certifications = certifications.trim(),
        mentorship = mentorship.trim(),
        gymsWorked = gymsWorked.trim(),
        clientsCoached = clientsCoachedText.toIntOrNull() ?: 0,
        clientTypes = clientTypes.joinToString(", "),
        trainingModes = modes.joinToString(", "),
        assessmentIncluded = assessmentIncluded,
        cprCertified = cprCertified,
        nutritionSupport = nutritionSupport,
        instagramUrl = instagramUrl.trim(),
        certDocUrl = certDocUrl,
        certStatus = if (certifications.isBlank()) "" else certStatus,
        // Earned, never typed: real member reviews feed the Proof score
        ratingSum = reviewAvg * reviewCount,
        ratingCount = reviewCount
    )

    val draft = buildDraft()
    val sections = PortfolioScoring.sections(draft)
    val score = sections.sumOf { it.earned }
    val plan = remember {
        runCatching { SubscriptionPlan.valueOf(userPreferences.subscriptionPlan) }
            .getOrDefault(SubscriptionPlan.STARTER)
    }

    // ── Image pickers ────────────────────────────────────────────────────────
    val profilePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            uploadingProfile = true
            uploadError = ""
            scope.launch {
                try {
                    profileImageUrl = com.example.data.FirestoreSync.uploadProfileImage(context, uri)
                } catch (e: Exception) {
                    uploadError = e.message ?: "Photo upload failed"
                } finally {
                    uploadingProfile = false
                }
            }
        }
    }
    val certPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            uploadingCert = true
            uploadError = ""
            scope.launch {
                try {
                    // OCR auto-review runs on-device before the upload finishes
                    val check = com.example.data.CertVerifier.autoReview(
                        context, uri, userPreferences.coachName)
                    certDocUrl = com.example.data.SupabaseStorage.uploadCertificatePhoto(context, uri)
                    certStatus = check.status
                } catch (e: Exception) {
                    uploadError = e.message ?: "Certificate upload failed"
                } finally {
                    uploadingCert = false
                }
            }
        }
    }
    val portfolioPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val idx = if (pendingSlot >= 0) pendingSlot else minOf(portfolioImageUrls.size, 2)
            uploadingPortfolio = idx
            uploadError = ""
            scope.launch {
                try {
                    val url = com.example.data.FirestoreSync.uploadPortfolioImage(context, uri, idx)
                    val updated = portfolioImageUrls.toMutableList()
                    if (idx < updated.size) updated[idx] = url else updated.add(url)
                    portfolioImageUrls = updated
                } catch (e: Exception) {
                    uploadError = e.message ?: "Photo upload failed"
                } finally {
                    uploadingPortfolio = -1
                    pendingSlot = -1
                }
            }
        }
    }

    // ── GPS pin ──────────────────────────────────────────────────────────────
    fun locateViaGps() {
        isGpsLocating = true
        scope.launch {
            val coords = withContext(Dispatchers.IO) {
                com.example.data.GeoUtils.getDeviceLocation(context)
            }
            if (coords != null) {
                gpsLat = coords.first
                gpsLng = coords.second
                gpsLocated = true
                val areaName = withContext(Dispatchers.IO) {
                    com.example.data.GeoUtils.reverseGeocode(context, coords.first, coords.second)
                }
                if (areaName.isNotBlank()) city = areaName
            }
            isGpsLocating = false
        }
    }
    val locationPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants[android.Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) locateViaGps()
    }
    fun useGpsLocation() {
        val fine = androidx.core.content.ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = androidx.core.content.ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.ACCESS_COARSE_LOCATION)
        if (fine == android.content.pm.PackageManager.PERMISSION_GRANTED ||
            coarse == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            locateViaGps()
        } else {
            locationPermLauncher.launch(arrayOf(
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ))
        }
    }

    if (loading) {
        Box(Modifier.fillMaxSize().background(CyberBgPrimary), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = CyberAccent)
        }
        return
    }

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
            Text("Trainer Portfolio", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = CyberTextPrimary, modifier = Modifier.weight(1f))
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ── Score hero ────────────────────────────────────────────────────
            PBCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            progress = { score / 100f },
                            modifier = Modifier.size(74.dp),
                            color = when {
                                score >= PortfolioScoring.TIER_ELITE  -> CyberSuccess
                                score >= PortfolioScoring.TIER_STRONG -> CyberAccent
                                else                                  -> CyberTextMuted
                            },
                            trackColor = CyberBgCardElevated,
                            strokeWidth = 7.dp
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("$score", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = CyberTextPrimary)
                            Text("/100", fontSize = 10.sp, color = CyberTextMuted)
                        }
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(PortfolioScoring.tierLabel(score), fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = CyberTextPrimary)
                        Text(
                            "Stronger profiles rank higher in Discover and win more bookings.",
                            fontSize = 12.sp, color = CyberTextMuted, lineHeight = 16.sp
                        )
                    }
                }
                val actions = PortfolioScoring.nextActions(draft, limit = 3)
                if (actions.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        actions.forEach { a ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(CyberBgCardElevated)
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(a.label, fontSize = 12.sp, color = CyberTextSecondary, modifier = Modifier.weight(1f))
                                Text("+${a.points} pts", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CyberAccent)
                            }
                        }
                    }
                }
            }

            // ── Featured upsell / status ──────────────────────────────────────
            if (plan == SubscriptionPlan.STARTER) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(CyberAccent.copy(0.08f))
                        .border(1.dp, CyberAccent.copy(0.25f), RoundedCornerShape(20.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("⚡ Get Featured", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = CyberAccent)
                    Text(
                        "Pro & Business coaches appear at the top of Discover with a Featured badge — before every free profile.",
                        fontSize = 12.sp, color = CyberTextSecondary, lineHeight = 17.sp
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(CyberAccent)
                            .clickable { onUpgradeClick() }
                            .padding(horizontal = 18.dp, vertical = 10.dp)
                    ) {
                        Text("Upgrade to Pro", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = CyberAccentDark)
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(CyberSuccess.copy(0.08f))
                        .border(1.dp, CyberSuccess.copy(0.25f), RoundedCornerShape(20.dp))
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("⚡", fontSize = 18.sp)
                    Column {
                        Text("Featured placement active", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = CyberSuccess)
                        Text("Your profile is boosted to the top of Discover.", fontSize = 12.sp, color = CyberTextMuted)
                    }
                }
            }

            // ── 1. Basics ─────────────────────────────────────────────────────
            PBSection("Basics", sections[0]) {
                Text("Profile Photo", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = CyberTextMuted)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(CyberBgCardElevated)
                            .border(2.dp, if (profileImageUrl.isNotBlank()) CyberAccent.copy(0.4f) else Color.White.copy(0.08f), CircleShape)
                            .clickable(enabled = !uploadingProfile) { profilePicker.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            uploadingProfile -> CircularProgressIndicator(color = CyberAccent, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                            profileImageUrl.isNotBlank() -> AsyncImage(
                                model = profileImageUrl, contentDescription = "Profile",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize().clip(CircleShape)
                            )
                            else -> Text("📷", fontSize = 22.sp)
                        }
                    }
                    Text(
                        if (profileImageUrl.isBlank()) "Tap to upload — profiles with a photo get far more bookings"
                        else "Tap to change photo",
                        fontSize = 12.sp, color = CyberTextMuted, modifier = Modifier.weight(1f), lineHeight = 16.sp
                    )
                }
                PBField("Headline (one line, e.g. \"Fat-loss coach for busy professionals\")", headline, maxChars = 90) { headline = it }
                PBField("Bio / About you", bio, singleLine = false, minHeight = 80) { bio = it }
                Text("Location", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = CyberTextMuted)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        PBField("City / Area *", city) { city = it; gpsLocated = false }
                    }
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (gpsLocated) CyberAccent.copy(0.15f) else CyberBgCardElevated)
                            .border(1.dp, if (gpsLocated) CyberAccent.copy(0.4f) else Color.White.copy(0.08f), RoundedCornerShape(12.dp))
                            .clickable(enabled = !isGpsLocating) { useGpsLocation() },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isGpsLocating) CircularProgressIndicator(color = CyberAccent, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        else Icon(Icons.Filled.MyLocation, "Use GPS", tint = if (gpsLocated) CyberAccent else CyberTextMuted, modifier = Modifier.size(20.dp))
                    }
                }
                if (gpsLocated) {
                    Text("✓ GPS pinned — members near you will find you first", fontSize = 11.sp, color = CyberAccent)
                }
            }

            // ── 2. Specializations & clients ─────────────────────────────────
            PBSection("Who You Train", null) {
                Text("Specializations (up to $MAX_SPECIALIZATIONS — pick your strongest)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = CyberTextMuted)
                PBChips(
                    options = (SPECIALIZATION_OPTIONS + specializations).distinct(),
                    selected = specializations,
                    onToggle = { opt ->
                        specializations = when {
                            opt in specializations -> specializations - opt
                            specializations.size < MAX_SPECIALIZATIONS -> specializations + opt
                            else -> specializations
                        }
                    }
                )
                Text("Client types", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = CyberTextMuted)
                PBChips((CLIENT_TYPE_OPTIONS + clientTypes).distinct(), clientTypes) { opt ->
                    clientTypes = if (opt in clientTypes) clientTypes - opt else clientTypes + opt
                }
                Text("Training modes", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = CyberTextMuted)
                PBChips((MODE_OPTIONS + modes).distinct(), modes) { opt ->
                    modes = if (opt in modes) modes - opt else modes + opt
                }
                Text("Languages", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = CyberTextMuted)
                PBChips((LANGUAGE_OPTIONS + languages).distinct(), languages) { opt ->
                    languages = if (opt in languages) languages - opt else languages + opt
                }
            }

            // ── 3. Credentials ────────────────────────────────────────────────
            PBSection("Credentials", sections[1]) {
                Text("Highest education", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = CyberTextMuted)
                PBChips(
                    options = (EDUCATION_OPTIONS + listOf(education).filter { it.isNotBlank() }).distinct(),
                    selected = if (education.isBlank()) emptySet() else setOf(education),
                    onToggle = { opt -> education = if (education == opt) "" else opt }
                )
                PBField("Certifications (e.g. K11 Certified PT 2021, ACE CPT)", certifications) { certifications = it }
                if (certifications.isNotBlank()) {
                    // Verification: upload the certificate → OCR auto-check → admin review
                    val (statusText, statusColor) = when {
                        uploadingCert                  -> "Checking document…" to CyberTextMuted
                        certStatus == "verified"       -> "✓ Certificate verified" to CyberSuccess
                        certStatus == "verified_auto"  -> "✓ Auto-verified — admin may re-check" to CyberSuccess
                        certStatus == "pending"        -> "⏳ Under review — badge appears once approved" to CyberTextSecondary
                        certStatus == "rejected"       -> "✗ Rejected — upload a clearer photo" to CyberDanger
                        else                           -> "Upload your certificate to earn the ✓ Verified badge" to CyberTextMuted
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(CyberBgCardElevated)
                            .clickable(enabled = !uploadingCert) { certPicker.launch("image/*") }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (uploadingCert) {
                            CircularProgressIndicator(color = CyberAccent, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Text("📄", fontSize = 16.sp)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(statusText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = statusColor, lineHeight = 16.sp)
                            Text(
                                if (certDocUrl.isBlank()) "Members trust verified coaches far more"
                                else "Tap to replace the document",
                                fontSize = 11.sp, color = CyberTextMuted
                            )
                        }
                    }
                }
                if (certifications.isBlank()) {
                    PBField("No certificate? Who did you train under / gym internship", mentorship) { mentorship = it }
                    Text(
                        "Honest self-declared experience builds trust — members see it clearly labelled.",
                        fontSize = 11.sp, color = CyberTextMuted, lineHeight = 15.sp
                    )
                }
                PBToggle("CPR / First-aid certified", "A key safety signal for members", cprCertified) { cprCertified = it }
            }

            // ── 4. Experience ────────────────────────────────────────────────
            PBSection("Experience", sections[2]) {
                PBField("Years of experience", yearsExpText, keyboard = KeyboardType.Number) { yearsExpText = it }
                PBField("Gyms / studios you've worked at", gymsWorked) { gymsWorked = it }
                PBField("Clients coached so far (approx.)", clientsCoachedText, keyboard = KeyboardType.Number) { clientsCoachedText = it }
            }

            // ── 5. Coaching approach ─────────────────────────────────────────
            PBSection("Coaching Approach", sections[3]) {
                PBField("How you coach — services, plan style, tracking, what makes you different", workDescription, singleLine = false, minHeight = 96) { workDescription = it }
                PBToggle("Fitness assessment before training", "Posture, mobility & goal check in the first session", assessmentIncluded) { assessmentIncluded = it }
                Text("Nutrition support", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = CyberTextMuted)
                PBChips(
                    options = NUTRITION_OPTIONS,
                    selected = if (nutritionSupport.isBlank()) emptySet() else setOf(nutritionSupport),
                    onToggle = { opt -> nutritionSupport = if (nutritionSupport == opt) "" else opt }
                )
            }

            // ── 6. Proof & results ───────────────────────────────────────────
            PBSection("Proof & Results", sections[4]) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Work photos", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = CyberTextMuted)
                    Text("${portfolioImageUrls.size}/3", fontSize = 11.sp, color = CyberAccent, fontWeight = FontWeight.Bold)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    repeat(3) { idx ->
                        val url = portfolioImageUrls.getOrNull(idx)
                        val isUploading = uploadingPortfolio == idx
                        Box(
                            modifier = Modifier
                                .size(84.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(CyberBgCardElevated)
                                .border(1.dp, if (url != null) CyberAccent.copy(0.3f) else Color.White.copy(0.08f), RoundedCornerShape(14.dp))
                                .clickable(enabled = uploadingPortfolio == -1) {
                                    pendingSlot = idx
                                    portfolioPicker.launch("image/*")
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            when {
                                isUploading -> CircularProgressIndicator(color = CyberAccent, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                url != null -> AsyncImage(
                                    model = url, contentDescription = "Work photo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp))
                                )
                                else -> Text("＋", fontSize = 22.sp, color = CyberTextMuted)
                            }
                        }
                    }
                }
                Text("Client reviews", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = CyberTextMuted)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CyberBgCardElevated)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("⭐", fontSize = 18.sp)
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (reviewCount > 0)
                                "%.1f from $reviewCount member review${if (reviewCount > 1) "s" else ""}".format(reviewAvg)
                            else "No reviews yet",
                            fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CyberTextPrimary
                        )
                        Text(
                            "Reviews come only from members who booked you — they can't be " +
                            "written or edited. Complete sessions and ask clients to rate you.",
                            fontSize = 11.sp, color = CyberTextMuted, lineHeight = 15.sp
                        )
                    }
                }
                PBField("Instagram link (optional)", instagramUrl, keyboard = KeyboardType.Uri) { instagramUrl = it }
            }

            // ── 7. Pricing & availability ────────────────────────────────────
            PBSection("Pricing & Availability", sections[5]) {
                PBField("Per session fee (₹) *", feeSessionText, keyboard = KeyboardType.Number) { feeSessionText = it }
                PBField("Monthly package (₹, optional)", feeMonthlyText, keyboard = KeyboardType.Number) { feeMonthlyText = it }
                Text("Available days", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = CyberTextMuted)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ALL_DAYS.forEach { day ->
                        val sel = day in selectedDays
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (sel) CyberAccent else CyberBgCardElevated)
                                .border(1.dp, if (sel) CyberAccent else Color.White.copy(0.06f), RoundedCornerShape(10.dp))
                                .clickable { selectedDays = if (sel) selectedDays - day else selectedDays + day }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                day.take(1), fontSize = 12.sp, fontWeight = FontWeight.ExtraBold,
                                color = if (sel) CyberAccentDark else CyberTextSecondary
                            )
                        }
                    }
                }
            }

            // ── Visibility + save ────────────────────────────────────────────
            PBToggle(
                if (isPublic) "Visible on Discover" else "Profile hidden",
                if (isPublic) "Members can find and book you" else "Turn on to appear in the marketplace",
                isPublic
            ) { isPublic = it }

            if (uploadError.isNotBlank()) {
                Text(uploadError, fontSize = 12.sp, color = CyberDanger)
            }

            val isBusy = saving || uploadingProfile || uploadingPortfolio != -1
            val canSave = !isBusy && (!isPublic || (city.isNotBlank() && feeSessionText.isNotBlank()))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (canSave) CyberAccent else CyberBgCardElevated)
                    .clickable(enabled = canSave) {
                        saving = true
                        scope.launch {
                            val (lat, lng) = when {
                                gpsLocated && gpsLat != 0.0 -> gpsLat to gpsLng
                                city.trim().equals(initialCity.trim(), ignoreCase = true) &&
                                    (savedLat != 0.0 || savedLng != 0.0) -> savedLat to savedLng
                                else -> {
                                    val coords = withContext(Dispatchers.IO) {
                                        com.example.data.GeoUtils.geocodeCity(context, city.trim())
                                    }
                                    (coords?.first ?: 0.0) to (coords?.second ?: 0.0)
                                }
                            }
                            viewModel.publishPortfolio(buildDraft().copy(lat = lat, lng = lng), isPublic)
                            onBack()
                        }
                    }
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isBusy) {
                    CircularProgressIndicator(color = CyberAccentDark, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text(
                        if (isPublic) "Publish Portfolio" else "Save",
                        fontSize = 15.sp, fontWeight = FontWeight.ExtraBold,
                        color = if (canSave) CyberAccentDark else CyberTextMuted
                    )
                }
            }
            Spacer(Modifier.navigationBarsPadding().height(16.dp))
        }
    }
}

// ─── Small building blocks ────────────────────────────────────────────────────

private fun String.splitToSet(): Set<String> =
    split(",").map { it.trim() }.filter { it.isNotBlank() }.toSet()

@Composable
private fun PBCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CyberBgCard)
            .border(1.dp, Color.White.copy(0.06f), RoundedCornerShape(20.dp))
            .padding(16.dp),
        content = content
    )
}

@Composable
private fun PBSection(
    title: String,
    section: com.example.data.ScoreSection?,
    content: @Composable ColumnScope.() -> Unit
) {
    PBCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = CyberTextPrimary)
            if (section != null) {
                val complete = section.earned >= section.max
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (complete) CyberSuccess.copy(0.12f) else CyberBgCardElevated)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        "${section.earned}/${section.max}",
                        fontSize = 11.sp, fontWeight = FontWeight.Bold,
                        color = if (complete) CyberSuccess else CyberTextMuted
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { content() }
    }
}

@Composable
private fun PBField(
    label: String,
    value: String,
    keyboard: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    minHeight: Int = 0,
    maxChars: Int = Int.MAX_VALUE,
    onChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(CyberBgCardElevated)
                .border(1.dp, Color.White.copy(0.06f), RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp)
                .let { if (minHeight > 0) it.height(minHeight.dp) else it }
        ) {
            BasicTextField(
                value = value,
                onValueChange = { onChange(it.take(maxChars)) },
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(color = CyberTextPrimary, fontSize = 14.sp),
                cursorBrush = SolidColor(CyberAccent),
                singleLine = singleLine,
                keyboardOptions = KeyboardOptions(keyboardType = keyboard),
                decorationBox = { inner ->
                    if (value.isEmpty()) Text(label, fontSize = 13.sp, color = CyberTextMuted, lineHeight = 17.sp)
                    inner()
                }
            )
        }
    }
}

@Composable
private fun PBChips(
    options: List<String>,
    selected: Set<String>,
    onToggle: (String) -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { opt ->
            val sel = opt in selected
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (sel) CyberAccent else CyberBgCardElevated)
                    .border(1.dp, if (sel) CyberAccent else Color.White.copy(0.08f), RoundedCornerShape(999.dp))
                    .clickable { onToggle(opt) }
                    .padding(horizontal = 12.dp, vertical = 7.dp)
            ) {
                Text(
                    opt, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                    maxLines = 1, softWrap = false,
                    color = if (sel) CyberAccentDark else CyberTextSecondary
                )
            }
        }
    }
}

@Composable
private fun PBToggle(
    title: String,
    subtitle: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (checked) CyberSuccess.copy(0.08f) else CyberBgCardElevated)
            .border(1.dp, if (checked) CyberSuccess.copy(0.3f) else Color.White.copy(0.08f), RoundedCornerShape(14.dp))
            .clickable { onChange(!checked) }
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (checked) CyberSuccess else CyberTextPrimary)
            Text(subtitle, fontSize = 11.sp, color = CyberTextMuted)
        }
        Box(
            modifier = Modifier
                .size(width = 44.dp, height = 26.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(if (checked) CyberSuccess else CyberBgCard),
            contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            )
        }
    }
}

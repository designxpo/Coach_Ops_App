package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.GeoUtils
import com.example.data.NearbyArea
import com.example.ui.theme.CyberAccent
import com.example.ui.theme.CyberAccentDark
import com.example.ui.theme.CyberBgCard
import com.example.ui.theme.CyberBgCardElevated
import com.example.ui.theme.CyberBgPrimary
import com.example.ui.theme.CyberDanger
import com.example.ui.theme.CyberTextMuted
import com.example.ui.theme.CyberTextPrimary
import com.example.ui.theme.CyberTextSecondary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ClientProfileScreen(
    viewModel: ClientViewModel,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    var name by remember { mutableStateOf(viewModel.clientName) }
    var city by remember { mutableStateOf(viewModel.clientCity) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showReportSheet by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) }

    // ── Location state ────────────────────────────────────────────────────────
    var isGpsLocating    by remember { mutableStateOf(false) }
    var isGeocoding      by remember { mutableStateOf(false) }
    var permDenied       by remember { mutableStateOf(false) }
    var nearbyAreas      by remember { mutableStateOf<List<NearbyArea>>(emptyList()) }
    var showSuggestions  by remember { mutableStateOf(false) }
    // Places autocomplete on typed input
    var searchResults    by remember { mutableStateOf<List<NearbyArea>>(emptyList()) }
    var isSearchingPlace by remember { mutableStateOf(false) }

    LaunchedEffect(city, showSuggestions) {
        if (!showSuggestions || city.length < 2) {
            searchResults    = emptyList()
            isSearchingPlace = false
            return@LaunchedEffect
        }
        isSearchingPlace = true
        delay(300)
        searchResults    = withContext(Dispatchers.IO) { GeoUtils.searchLocations(context, city) }
        isSearchingPlace = false
    }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                      grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            permDenied = false
            scope.launch {
                isGpsLocating = true
                val coords = withContext(Dispatchers.IO) { GeoUtils.getDeviceLocation(context) }
                isGpsLocating = false
                if (coords != null) {
                    val areas = withContext(Dispatchers.IO) {
                        GeoUtils.getNearbyAreas(context, coords.first, coords.second)
                    }
                    val label = areas.firstOrNull()?.name
                        ?: withContext(Dispatchers.IO) {
                            GeoUtils.reverseGeocode(context, coords.first, coords.second)
                        }
                    city = label
                    nearbyAreas = areas
                    showSuggestions = areas.size > 1
                    viewModel.setClientCoordinates(coords.first, coords.second)
                    saved = false
                }
            }
        } else {
            permDenied = true
        }
    }

    fun detectGpsLocation() {
        val fine   = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED) {
            scope.launch {
                isGpsLocating = true
                val coords = withContext(Dispatchers.IO) { GeoUtils.getDeviceLocation(context) }
                isGpsLocating = false
                if (coords != null) {
                    val areas = withContext(Dispatchers.IO) {
                        GeoUtils.getNearbyAreas(context, coords.first, coords.second)
                    }
                    val label = areas.firstOrNull()?.name
                        ?: withContext(Dispatchers.IO) {
                            GeoUtils.reverseGeocode(context, coords.first, coords.second)
                        }
                    city = label
                    nearbyAreas = areas
                    showSuggestions = areas.size > 1
                    viewModel.setClientCoordinates(coords.first, coords.second)
                    saved = false
                }
            }
        } else {
            permLauncher.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ))
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Sign Out", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to sign out?") },
            confirmButton = {
                TextButton(onClick = onLogout) { Text("Sign Out", color = CyberDanger) }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text("Cancel", color = CyberAccent) }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBgPrimary)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(20.dp))

        Text("My Profile", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = CyberTextPrimary)
        Spacer(Modifier.height(4.dp))
        Text("Manage your account", fontSize = 13.sp, color = CyberTextMuted)

        Spacer(Modifier.height(24.dp))

        // Avatar with upload
        var photoUrl by remember { mutableStateOf(viewModel.userPreferences.profilePhotoUrl) }
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            ProfileAvatarPicker(
                photoUrl        = photoUrl,
                initials        = name.take(1).ifEmpty { "?" },
                size            = 90.dp,
                userPreferences = viewModel.userPreferences,
                onPhotoUploaded = { url -> photoUrl = url }
            )
        }

        Spacer(Modifier.height(24.dp))

        // Fields
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            ProfileFieldSection(label = "Full Name") {
                AuthTextField(
                    value = name,
                    onValueChange = { name = it; saved = false },
                    label = "Full Name",
                    keyboardType = KeyboardType.Text
                )
            }

            ProfileFieldSection(label = "Your Location") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // City text field + GPS button row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(Modifier.weight(1f)) {
                            AuthTextField(
                                value = city,
                                onValueChange = {
                                    city = it
                                    saved = false
                                    showSuggestions = it.length >= 2
                                },
                                label = "City or area",
                                keyboardType = KeyboardType.Text
                            )
                        }
                        // GPS detect button
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(CyberAccent.copy(if (isGpsLocating) 0.4f else 1f))
                                .clickable(enabled = !isGpsLocating) { detectGpsLocation() },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isGpsLocating) {
                                androidx.compose.material3.CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = CyberAccentDark
                                )
                            } else {
                                Icon(
                                    Icons.Filled.MyLocation,
                                    contentDescription = "Detect location",
                                    tint = CyberAccentDark,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }

                    // GPS hint / error
                    if (permDenied) {
                        Text(
                            "Location permission denied. Type your city manually.",
                            color = CyberDanger, fontSize = 11.sp
                        )
                    } else if (city.isNotBlank() && !isGpsLocating) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Filled.LocationOn, null,
                                tint = CyberAccent, modifier = Modifier.size(13.dp))
                            Text(city, color = CyberTextMuted, fontSize = 11.sp)
                        }
                    } else if (!isGpsLocating) {
                        Text(
                            "Tap 📍 to auto-detect, or type your city",
                            color = CyberTextMuted, fontSize = 11.sp
                        )
                    }

                    // Location suggestions dropdown
                    AnimatedVisibility(visible = showSuggestions &&
                        (isSearchingPlace || searchResults.isNotEmpty() || nearbyAreas.isNotEmpty())) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(CyberBgCard)
                                .border(1.dp, Color.White.copy(0.08f), RoundedCornerShape(14.dp))
                        ) {
                            // ── Search results from typed text ────────────────
                            if (isSearchingPlace) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CircularProgressIndicator(
                                        color = CyberAccent,
                                        modifier = Modifier.size(13.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Text("Searching…", color = CyberTextMuted, fontSize = 12.sp)
                                }
                            } else if (searchResults.isNotEmpty()) {
                                Text(
                                    "🔍  SEARCH RESULTS",
                                    color = CyberTextMuted, fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                )
                                searchResults.forEach { area ->
                                    HorizontalDivider(color = Color.White.copy(0.05f))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                city = area.name
                                                showSuggestions = false
                                                searchResults = emptyList()
                                                saved = false
                                                if (area.placeId.isNotEmpty()) {
                                                    scope.launch {
                                                        val coords = withContext(Dispatchers.IO) {
                                                            GeoUtils.resolvePlace(area.placeId, context)
                                                        }
                                                        if (coords != null)
                                                            viewModel.setClientCoordinates(coords.first, coords.second)
                                                    }
                                                } else {
                                                    viewModel.setClientCoordinates(area.lat, area.lng)
                                                }
                                            }
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(Icons.Filled.Search, null,
                                            tint = CyberAccent, modifier = Modifier.size(14.dp))
                                        Column {
                                            Text(area.name, color = CyberTextPrimary,
                                                fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                            if (area.cityName.isNotBlank() && area.cityName != area.name)
                                                Text(area.cityName, color = CyberTextMuted, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }

                            // ── GPS nearby areas ──────────────────────────────
                            if (nearbyAreas.isNotEmpty()) {
                                if (searchResults.isNotEmpty() || isSearchingPlace)
                                    HorizontalDivider(color = Color.White.copy(0.08f))
                                Text(
                                    "📍  NEARBY AREAS",
                                    color = CyberTextMuted, fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                )
                                nearbyAreas.take(5).forEach { area ->
                                    HorizontalDivider(color = Color.White.copy(0.04f))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                city = area.name
                                                showSuggestions = false
                                                searchResults = emptyList()
                                                saved = false
                                                viewModel.setClientCoordinates(area.lat, area.lng)
                                            }
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(Icons.Filled.LocationOn, null,
                                            tint = CyberAccent, modifier = Modifier.size(15.dp))
                                        Column {
                                            Text(area.name, color = CyberTextPrimary,
                                                fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                            if (area.distKm > 0)
                                                Text("%.1f km away".format(area.distKm),
                                                    color = CyberTextMuted, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(4.dp))
                        }
                    }
                }
            }

            ProfileFieldSection(label = "Email") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(CyberBgCard)
                        .border(1.dp, Color.White.copy(0.06f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Text(
                        viewModel.userPreferences.coachEmail.ifEmpty { "Not set" },
                        fontSize = 14.sp, color = CyberTextSecondary
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Save button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(if (saved) CyberBgCardElevated else CyberAccent)
                .clickable(enabled = !saved && !isGeocoding && !isGpsLocating) {
                    val trimmedCity = city.trim()
                    viewModel.updateClientName(name.trim())
                    viewModel.updateClientCity(trimmedCity)
                    // Geocode the typed city to get coordinates for distance search
                    if (trimmedCity.isNotBlank() &&
                        viewModel.clientLat == 0.0 && viewModel.clientLng == 0.0) {
                        scope.launch {
                            isGeocoding = true
                            val coords = withContext(Dispatchers.IO) {
                                GeoUtils.geocodeCity(context, trimmedCity)
                            }
                            isGeocoding = false
                            if (coords != null) {
                                viewModel.setClientCoordinates(coords.first, coords.second)
                            }
                        }
                    }
                    viewModel.loadTrainers()
                    saved = true
                }
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                when {
                    isGeocoding  -> "Locating…"
                    isGpsLocating -> "Detecting GPS…"
                    saved        -> "Saved ✓"
                    else         -> "Save Changes"
                },
                fontSize = 15.sp, fontWeight = FontWeight.ExtraBold,
                color = if (saved) CyberTextMuted else CyberAccentDark
            )
        }

        Spacer(Modifier.height(16.dp))

        // Report a problem
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CyberBgCard)
                .clickable { showReportSheet = true }
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Report a Problem", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = CyberTextPrimary)
        }

        Spacer(Modifier.height(12.dp))

        // Logout
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CyberDanger.copy(alpha = 0.10f))
                .border(1.dp, CyberDanger.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                .clickable { showLogoutDialog = true }
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Sign Out", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = CyberDanger)
        }

        Spacer(Modifier.height(12.dp))

        // Play policy: account deletion must be reachable in-app for members too
        Text(
            "Delete My Account",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = CyberDanger.copy(alpha = 0.8f),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showDeleteDialog = true }
                .padding(vertical = 8.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(Modifier.height(24.dp))
    }

    if (showReportSheet) ReportIssueSheet(onDismiss = { showReportSheet = false })
    if (showDeleteDialog) DeleteAccountDialog(
        onDismiss = { showDeleteDialog = false },
        onDeleted = onLogout   // session is gone — reuse logout to wipe local state + navigate
    )
}

@Composable
private fun ProfileFieldSection(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = CyberTextMuted)
        content()
    }
}

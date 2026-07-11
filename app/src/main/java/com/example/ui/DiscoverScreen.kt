package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.data.Booking
import com.example.data.GeoUtils
import com.example.data.NearbyArea
import com.example.data.TrainerProfile
import com.example.data.isFeatured
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val RADIUS_OPTIONS = listOf(5, 10, 25, 50, 0)
private fun radiusLabel(km: Int) = if (km == 0) "Anywhere" else "$km km"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreen(
    viewModel: ClientViewModel,
    onTrainerClick: (String) -> Unit,
    onAvatarClick: () -> Unit = {}
) {
    val context   = LocalContext.current
    val scope     = rememberCoroutineScope()
    val trainers             by viewModel.trainers.collectAsState()
    val bookings             by viewModel.myBookings.collectAsState()
    val isLoading            by viewModel.isLoadingTrainers.collectAsState()
    val radiusKm             by viewModel.radiusKm.collectAsState()
    val loadError            by viewModel.loadError.collectAsState()
    val isExpandingFeed      by viewModel.isExpandingFeed.collectAsState()
    val hasMoreCoaches       by viewModel.hasMoreCoaches.collectAsState()
    val autoFeedRadius       by viewModel.autoFeedRadius.collectAsState()
    val searchOverrideLabel  by viewModel.searchOverrideLabel.collectAsState()

    var cityQuery              by remember { mutableStateOf(viewModel.clientCity) }
    var isGeocoding            by remember { mutableStateOf(false) }
    var isGpsLocating          by remember { mutableStateOf(false) }
    var locationLabel          by remember { mutableStateOf(viewModel.clientCity) }
    var isGpsMode              by remember { mutableStateOf(false) }
    var permDenied             by remember { mutableStateOf(false) }
    var showLocationRationale  by remember { mutableStateOf(false) }

    // Suggestion state
    var isSearchFocused   by remember { mutableStateOf(false) }
    var nearbyAreas       by remember { mutableStateOf<List<NearbyArea>>(emptyList()) }
    var isFetchingNearby  by remember { mutableStateOf(false) }
    var gpsCoords         by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    // Places autocomplete — triggered by typing
    var searchResults     by remember { mutableStateOf<List<NearbyArea>>(emptyList()) }
    var isSearchingPlaces by remember { mutableStateOf(false) }

    // ── Permission launcher ───────────────────────────────────────────────────
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                      grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            permDenied = false
            fetchGpsAndNearby(
                context, scope,
                onLocating = { isGpsLocating = it },
                onFetching = { isFetchingNearby = it },
                onResult = { coords, areas, label ->
                    gpsCoords = coords
                    nearbyAreas = areas
                    if (coords != null) {
                        isGpsMode = true
                        locationLabel = label
                        cityQuery = label
                        viewModel.updateClientCity(label)
                        viewModel.setClientCoordinates(coords.first, coords.second)
                    }
                }
            )
        } else {
            permDenied = true
        }
    }

    fun requestGpsWithSuggestions() {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED) {
            fetchGpsAndNearby(
                context, scope,
                onLocating = { isGpsLocating = it },
                onFetching = { isFetchingNearby = it },
                onResult = { coords, areas, label ->
                    gpsCoords = coords
                    nearbyAreas = areas
                }
            )
        } else {
            showLocationRationale = true
        }
    }

    // ── Places autocomplete: debounced 300 ms after user stops typing ────────
    LaunchedEffect(cityQuery, isSearchFocused) {
        if (!isSearchFocused || cityQuery.length < 2) {
            searchResults     = emptyList()
            isSearchingPlaces = false
            return@LaunchedEffect
        }
        isSearchingPlaces = true
        delay(300)
        searchResults     = withContext(Dispatchers.IO) { GeoUtils.searchLocations(context, cityQuery) }
        isSearchingPlaces = false
    }

    // ── Initial load ──────────────────────────────────────────────────────────
    LaunchedEffect(Unit) {
        val savedCity = viewModel.clientCity
        if (savedCity.isNotBlank() && viewModel.clientLat == 0.0 && viewModel.clientLng == 0.0) {
            isGeocoding = true
            val coords = withContext(Dispatchers.IO) { GeoUtils.geocodeCity(context, savedCity) }
            isGeocoding = false
            if (coords != null) {
                // setClientCoordinates already reloads the feed
                viewModel.setClientCoordinates(coords.first, coords.second)
                locationLabel = savedCity
            } else {
                viewModel.loadTrainers()
            }
        } else {
            viewModel.loadTrainers()
        }
    }

    val activeBookings = remember(bookings) {
        bookings.filter { it.status in listOf("PENDING", "CONFIRMED") }
    }
    // trainers is already sorted by distance/rating from ClientViewModel — don't re-sort
    val topTrainers = trainers

    // Scroll state + near-end detection for infinite feed
    val listState = rememberLazyListState()
    val shouldExpand by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            val total = info.totalItemsCount
            // Trigger when within 3 items of the end
            total > 0 && lastVisible >= total - 3
        }
    }
    LaunchedEffect(shouldExpand) {
        if (shouldExpand && hasMoreCoaches && !isExpandingFeed && !isLoading) {
            viewModel.expandFeedRadius()
        }
    }

    // Filtered suggestions — matches typed text or shows all when query blank
    val filteredAreas = remember(cityQuery, nearbyAreas, isSearchFocused) {
        if (!isSearchFocused) emptyList()
        else if (cityQuery.isBlank()) nearbyAreas
        else nearbyAreas.filter { it.name.contains(cityQuery, ignoreCase = true) ||
                                  it.cityName.contains(cityQuery, ignoreCase = true) }
    }

    // ── Location rationale dialog ─────────────────────────────────────────────
    if (showLocationRationale) {
        AlertDialog(
            onDismissRequest = { showLocationRationale = false },
            title = { Text("Location Required", color = CyberTextPrimary) },
            text = { Text("ProCoach India needs your location to show nearby coaches. Your location is only used within the app and never shared.", color = CyberTextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    showLocationRationale = false
                    permLauncher.launch(arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ))
                }) { Text("Allow", color = CyberAccent) }
            },
            dismissButton = {
                TextButton(onClick = { showLocationRationale = false }) { Text("Not now", color = CyberTextMuted) }
            },
            containerColor = CyberBgCard
        )
    }

    val pullState = rememberPullToRefreshState()

    PullToRefreshBox(
        isRefreshing = isLoading,
        onRefresh    = { viewModel.loadTrainers() },
        state        = pullState,
        modifier     = Modifier.fillMaxSize().background(CyberBgPrimary).statusBarsPadding()
    ) {
        LazyColumn(
            state          = listState,
            modifier       = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {

            // ── Greeting ──────────────────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ProfileAvatar(
                        name = viewModel.clientName.ifBlank { "U" },
                        size = 46.dp,
                        onClick = onAvatarClick
                    )
                    Column(Modifier.weight(1f)) {
                        Text("Hi, ${viewModel.clientName.ifBlank { "there" }} 👋",
                            fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = CyberTextPrimary,
                            maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                        Spacer(Modifier.height(2.dp))
                        Text("Find your perfect fitness coach", fontSize = 13.sp, color = CyberTextMuted)
                    }
                }
            }

            // ── Search bar + suggestions ──────────────────────────────────────
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    // ── Search field ──────────────────────────────────────────
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(if (isSearchFocused) RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                                  else RoundedCornerShape(16.dp))
                            .background(CyberBgCard)
                            .border(
                                1.dp,
                                if (isSearchFocused) CyberAccent.copy(0.5f) else Color.White.copy(0.08f),
                                if (isSearchFocused) RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                                else RoundedCornerShape(16.dp)
                            )
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                Icons.Filled.Search, null,
                                tint = if (isSearchFocused) CyberAccent else CyberTextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                            BasicTextField(
                                value = cityQuery,
                                onValueChange = {
                                    cityQuery = it
                                    isGpsMode = false
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .onFocusChanged { state ->
                                        if (state.isFocused && !isSearchFocused) {
                                            isSearchFocused = true
                                            // Auto-fetch suggestions when search opens
                                            if (nearbyAreas.isEmpty() && !isFetchingNearby) {
                                                requestGpsWithSuggestions()
                                            }
                                        } else if (!state.isFocused && isSearchFocused) {
                                            isSearchFocused = false
                                        }
                                    },
                                textStyle = TextStyle(
                                    color = CyberTextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                cursorBrush = SolidColor(CyberAccent),
                                singleLine = true,
                                decorationBox = { inner ->
                                    if (cityQuery.isEmpty()) Text(
                                        "City or area (e.g. Koramangala)…",
                                        fontSize = 14.sp, color = CyberTextMuted,
                                        maxLines = 1, softWrap = false,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                    inner()
                                }
                            )
                            // GPS badge when active
                            if (isGpsMode && !isSearchFocused) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(CyberAccent.copy(0.18f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("GPS", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = CyberAccent)
                                }
                            }
                            // Close / Search button
                            if (isSearchFocused) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(CyberBgCardElevated)
                                        .clickable {
                                            isSearchFocused = false
                                            if (cityQuery.isBlank()) {
                                                locationLabel = ""
                                                // setClientCoordinates already reloads — a
                                                // second loadTrainers() double-fetched the feed
                                                viewModel.setClientCoordinates(0.0, 0.0)
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.Close, null, tint = CyberTextMuted, modifier = Modifier.size(14.dp))
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(if (isGeocoding) CyberBgCardElevated else CyberAccent)
                                        .clickable(enabled = !isGeocoding) {
                                            scope.launch {
                                                isSearchFocused = false
                                                viewModel.updateClientCity(cityQuery)
                                                if (cityQuery.isBlank()) {
                                                    isGpsMode = false
                                                    viewModel.setClientCoordinates(0.0, 0.0)
                                                    locationLabel = ""
                                                } else {
                                                    isGeocoding = true
                                                    val coords = withContext(Dispatchers.IO) {
                                                        GeoUtils.geocodeCity(context, cityQuery)
                                                    }
                                                    isGeocoding = false
                                                    if (coords != null) {
                                                        viewModel.setClientCoordinates(coords.first, coords.second)
                                                        locationLabel = cityQuery
                                                    } else {
                                                        // setClientCoordinates already reloads
                                                        viewModel.setClientCoordinates(0.0, 0.0)
                                                        locationLabel = ""
                                                    }
                                                }
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isGeocoding) CircularProgressIndicator(
                                        color = CyberAccent, modifier = Modifier.size(18.dp), strokeWidth = 2.dp
                                    )
                                    else Icon(Icons.Filled.Search, null, tint = CyberAccentDark, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    // ── Suggestions dropdown (Rapido-style) ───────────────────
                    AnimatedVisibility(
                        visible = isSearchFocused,
                        enter   = expandVertically() + fadeIn(),
                        exit    = shrinkVertically() + fadeOut()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                                .background(CyberBgCard)
                                .border(
                                    1.dp,
                                    CyberAccent.copy(0.3f),
                                    RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
                                )
                        ) {
                            // ── "Use Current Location" row ────────────────────
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        isSearchFocused = false
                                        val fine = ContextCompat.checkSelfPermission(
                                            context, Manifest.permission.ACCESS_FINE_LOCATION)
                                        val coarse = ContextCompat.checkSelfPermission(
                                            context, Manifest.permission.ACCESS_COARSE_LOCATION)
                                        if (fine == PackageManager.PERMISSION_GRANTED ||
                                            coarse == PackageManager.PERMISSION_GRANTED) {
                                            // Use already-fetched GPS coords if available
                                            val cached = gpsCoords
                                            if (cached != null) {
                                                val label = nearbyAreas.firstOrNull()?.name ?: "Near You"
                                                isGpsMode = true
                                                locationLabel = label
                                                cityQuery = label
                                                viewModel.updateClientCity(label)
                                                viewModel.setClientCoordinates(cached.first, cached.second)
                                            } else {
                                                // Fetch GPS now
                                                isGpsLocating = true
                                                scope.launch {
                                                    val coords = withContext(Dispatchers.IO) {
                                                        GeoUtils.getDeviceLocation(context)
                                                    }
                                                    isGpsLocating = false
                                                    if (coords != null) {
                                                        val areaName = withContext(Dispatchers.IO) {
                                                            GeoUtils.reverseGeocode(context, coords.first, coords.second)
                                                        }
                                                        isGpsMode = true
                                                        locationLabel = areaName
                                                        cityQuery = areaName
                                                        viewModel.updateClientCity(areaName)
                                                        viewModel.setClientCoordinates(coords.first, coords.second)
                                                    }
                                                }
                                            }
                                        } else {
                                            showLocationRationale = true
                                        }
                                    }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(CyberAccent.copy(0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isGpsLocating) {
                                        CircularProgressIndicator(
                                            color = CyberAccent,
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Icon(Icons.Filled.MyLocation, null,
                                            tint = CyberAccent, modifier = Modifier.size(16.dp))
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Use Current Location",
                                        fontSize = 14.sp, fontWeight = FontWeight.Bold, color = CyberAccent)
                                    Text(
                                        if (isGpsMode && locationLabel.isNotBlank()) locationLabel
                                        else "Detect your location automatically",
                                        fontSize = 11.sp, color = CyberTextMuted
                                    )
                                }
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, null,
                                    tint = CyberTextMuted, modifier = Modifier.size(14.dp))
                            }

                            HorizontalDivider(color = Color.White.copy(0.06f))

                            // ── Places autocomplete section ───────────────────
                            if (isSearchingPlaces) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    CircularProgressIndicator(
                                        color = CyberAccent,
                                        modifier = Modifier.size(14.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Text("Searching locations…", fontSize = 12.sp, color = CyberTextMuted)
                                }
                            } else if (searchResults.isNotEmpty()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text("🔍", fontSize = 11.sp)
                                    Text(
                                        "SEARCH RESULTS",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = CyberTextMuted,
                                        letterSpacing = 1.sp
                                    )
                                }
                                searchResults.forEachIndexed { index, area ->
                                    if (index > 0) HorizontalDivider(
                                        modifier = Modifier.padding(start = 64.dp),
                                        color = Color.White.copy(0.04f)
                                    )
                                    NearbyAreaRow(
                                        area = area,
                                        onClick = {
                                            isSearchFocused = false
                                            cityQuery = area.name
                                            locationLabel = area.name
                                            isGpsMode = false
                                            searchResults = emptyList()
                                            // Temporary area search — does NOT change the user's saved city
                                            if (area.placeId.isNotEmpty()) {
                                                scope.launch {
                                                    val coords = withContext(Dispatchers.IO) {
                                                        GeoUtils.resolvePlace(area.placeId, context)
                                                    }
                                                    if (coords != null)
                                                        viewModel.searchInArea(coords.first, coords.second, area.name)
                                                }
                                            } else {
                                                viewModel.searchInArea(area.lat, area.lng, area.name)
                                            }
                                        }
                                    )
                                }
                            }

                            // ── Nearby GPS areas section ──────────────────────
                            if (isFetchingNearby) {
                                HorizontalDivider(color = Color.White.copy(0.06f))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    CircularProgressIndicator(
                                        color = CyberAccent,
                                        modifier = Modifier.size(14.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Text("Finding nearby areas…", fontSize = 12.sp, color = CyberTextMuted)
                                }
                            } else if (filteredAreas.isNotEmpty()) {
                                if (searchResults.isNotEmpty() || isSearchingPlaces)
                                    HorizontalDivider(color = Color.White.copy(0.06f))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text("📍", fontSize = 11.sp)
                                    Text(
                                        if (cityQuery.isBlank()) "NEARBY AREAS" else "NEARBY MATCHES",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = CyberTextMuted,
                                        letterSpacing = 1.sp
                                    )
                                }
                                filteredAreas.forEachIndexed { index, area ->
                                    if (index > 0) HorizontalDivider(
                                        modifier = Modifier.padding(start = 64.dp),
                                        color = Color.White.copy(0.04f)
                                    )
                                    NearbyAreaRow(
                                        area = area,
                                        onClick = {
                                            isSearchFocused = false
                                            cityQuery = area.name
                                            locationLabel = area.name
                                            isGpsMode = false
                                            searchResults = emptyList()
                                            viewModel.updateClientCity(area.name)
                                            viewModel.setClientCoordinates(area.lat, area.lng)
                                        }
                                    )
                                }
                            }

                            // ── Empty state ───────────────────────────────────
                            if (!isSearchingPlaces && !isFetchingNearby &&
                                searchResults.isEmpty() && filteredAreas.isEmpty() &&
                                cityQuery.isNotBlank()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(Icons.Filled.Search, null,
                                        tint = CyberTextMuted, modifier = Modifier.size(16.dp))
                                    Text("No results for \"$cityQuery\"",
                                        fontSize = 13.sp, color = CyberTextMuted)
                                }
                            }

                            Spacer(Modifier.height(4.dp))
                        }
                    }

                    // ── Area search override banner ────────────────────────────
                    if (searchOverrideLabel.isNotBlank() && !isSearchFocused) {
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(CyberAccent.copy(0.12f))
                                .border(1.dp, CyberAccent.copy(0.35f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Search, null,
                                tint = CyberAccent, modifier = Modifier.size(14.dp))
                            Text(
                                "Searching in: $searchOverrideLabel",
                                fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                                color = CyberAccent, modifier = Modifier.weight(1f)
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(CyberAccent.copy(0.2f))
                                    .clickable {
                                        cityQuery = viewModel.clientCity
                                        locationLabel = viewModel.clientCity
                                        isGpsMode = false
                                        viewModel.clearSearchOverride()
                                    }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("✕ Reset", fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold, color = CyberAccent)
                            }
                        }
                    }

                    // ── Permission denied warning ──────────────────────────────
                    if (permDenied) {
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(CyberDanger.copy(0.1f))
                                .border(1.dp, CyberDanger.copy(0.3f), RoundedCornerShape(10.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("⚠", fontSize = 12.sp)
                            Text("Location permission denied. Enable it in Settings.",
                                fontSize = 11.sp, color = CyberDanger, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            // ── Radius chips (hidden while suggestions open) ──────────────────
            if (!isSearchFocused) {
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(RADIUS_OPTIONS) { km ->
                            val selected = radiusKm == km
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(if (selected) CyberAccent else CyberBgCard)
                                    .border(
                                        if (selected) 0.dp else 1.dp,
                                        Color.White.copy(0.08f), RoundedCornerShape(999.dp)
                                    )
                                    .clickable { viewModel.setRadius(km) }
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text(radiusLabel(km), fontSize = 12.sp,
                                    maxLines = 1, softWrap = false,
                                    fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Normal,
                                    color = if (selected) CyberAccentDark else CyberTextMuted)
                            }
                        }
                    }
                }
            }

            // ── Error banner ──────────────────────────────────────────────────
            if (loadError.isNotEmpty() && !isSearchFocused) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(CyberDanger.copy(0.12f))
                            .border(1.dp, CyberDanger.copy(0.3f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("⚠ $loadError", fontSize = 12.sp, color = CyberDanger,
                            modifier = Modifier.weight(1f))
                        Text("Retry", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CyberAccent,
                            modifier = Modifier.clickable { viewModel.loadTrainers() }.padding(start = 12.dp))
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }

            // ── Active Bookings (hidden while suggestions open) ───────────────
            if (!isSearchFocused && activeBookings.isNotEmpty()) {
                item { HomeSectionHeader(title = "My Active Bookings", badge = "${activeBookings.size}") }
                itemsIndexed(activeBookings.take(2), key = { _, b -> "bk_${b.id}" }) { _, booking ->
                    CompactBookingCard(booking, Modifier.padding(horizontal = 20.dp).padding(bottom = 8.dp))
                }
                if (activeBookings.size > 2) {
                    item {
                        Box(Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                            Text("+${activeBookings.size - 2} more — see Bookings tab",
                                fontSize = 12.sp, color = CyberAccent, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                item { Spacer(Modifier.height(8.dp)) }
            }

            // ── Top Trainers (hidden while suggestions open) ──────────────────
            if (!isSearchFocused) {
                item {
                    val isAutoMode = autoFeedRadius > 0 && locationLabel.isNotBlank()
                    val subtitle = when {
                        radiusKm > 0 && locationLabel.isNotBlank() ->
                            "Within ${radiusLabel(radiusKm)} of ${if (isGpsMode) "your location" else locationLabel}"
                        isAutoMode ->
                            "Within ${autoFeedRadius}km · scroll for more"
                        locationLabel.isNotBlank() ->
                            if (isGpsMode) "Near your GPS location" else "Near $locationLabel"
                        else -> "Top rated coaches · all locations"
                    }
                    HomeSectionHeader(
                        title    = "Top Trainers",
                        badge    = if (topTrainers.isNotEmpty()) "${topTrainers.size}" else null,
                        subtitle = subtitle
                    )
                }

                when {
                    isLoading || isGeocoding -> item {
                        Box(Modifier.fillMaxWidth().padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = CyberAccent, modifier = Modifier.size(32.dp))
                        }
                    }
                    topTrainers.isEmpty() -> item {
                        Column(Modifier.fillMaxWidth().padding(vertical = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🔍", fontSize = 40.sp)
                            Spacer(Modifier.height(12.dp))
                            Text(
                                if (radiusKm > 0 && locationLabel.isNotBlank())
                                    "No coaches within ${radiusLabel(radiusKm)}"
                                else "No coaches found",
                                fontSize = 15.sp, fontWeight = FontWeight.Bold, color = CyberTextPrimary
                            )
                            Spacer(Modifier.height(6.dp))
                            Text("Try a wider radius or tap Anywhere",
                                fontSize = 13.sp, color = CyberTextMuted)
                        }
                    }
                    else -> {
                        itemsIndexed(topTrainers, key = { _, t -> t.uid }) { index, trainer ->
                            TrainerCard(
                                trainer  = trainer,
                                rank     = index + 1,
                                onClick  = { onTrainerClick(trainer.uid) },
                                modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 12.dp)
                            )
                        }

                        // ── Infinite-feed footer ──────────────────────────────
                        item {
                            when {
                                isExpandingFeed -> {
                                    // Spinner while expanding
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 20.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(
                                            color = CyberAccent,
                                            modifier = Modifier.size(20.dp),
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Text(
                                            "Expanding to ${autoFeedRadius}km…",
                                            fontSize = 12.sp, color = CyberTextMuted
                                        )
                                    }
                                }
                                hasMoreCoaches -> {
                                    // Subtle "more nearby" hint
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 20.dp, vertical = 12.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(CyberBgCard)
                                            .border(1.dp, CyberAccent.copy(0.15f), RoundedCornerShape(12.dp))
                                            .clickable { viewModel.expandFeedRadius() }
                                            .padding(horizontal = 16.dp, vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text("📍", fontSize = 13.sp)
                                            Text(
                                                "Show coaches beyond ${autoFeedRadius}km",
                                                fontSize = 12.sp,
                                                color = CyberAccent,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                                topTrainers.isNotEmpty() -> {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("You've seen all available coaches",
                                            fontSize = 12.sp, color = CyberTextMuted)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Helper: fetch GPS + nearby areas in one go ────────────────────────────────

private fun fetchGpsAndNearby(
    context: android.content.Context,
    scope: kotlinx.coroutines.CoroutineScope,
    onLocating: (Boolean) -> Unit,
    onFetching: (Boolean) -> Unit,
    onResult: (coords: Pair<Double, Double>?, areas: List<NearbyArea>, label: String) -> Unit
) {
    scope.launch {
        onLocating(true)
        val coords = withContext(Dispatchers.IO) { GeoUtils.getDeviceLocation(context) }
        onLocating(false)
        if (coords == null) {
            onResult(null, emptyList(), "")
            return@launch
        }
        onFetching(true)
        val (areas, label) = withContext(Dispatchers.IO) {
            val a = GeoUtils.getNearbyAreas(context, coords.first, coords.second)
            val l = a.firstOrNull()?.name ?: GeoUtils.reverseGeocode(context, coords.first, coords.second)
            a to l
        }
        onFetching(false)
        onResult(coords, areas, label)
    }
}

// ── Nearby area row ────────────────────────────────────────────────────────────

@Composable
private fun NearbyAreaRow(area: NearbyArea, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(CyberBgCardElevated),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.LocationOn, null, tint = CyberTextMuted, modifier = Modifier.size(16.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(area.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = CyberTextPrimary)
            Text(
                buildString {
                    if (area.cityName.isNotBlank() && area.cityName != area.name) append(area.cityName)
                    if (area.distKm > 0.05) {
                        if (isNotEmpty()) append(" · ")
                        append("%.1f km away".format(area.distKm))
                    }
                },
                fontSize = 11.sp, color = CyberTextMuted
            )
        }
        Icon(Icons.AutoMirrored.Filled.ArrowForward, null,
            tint = CyberTextMuted, modifier = Modifier.size(14.dp))
    }
}

// ─── Section header ───────────────────────────────────────────────────────────

@Composable
private fun HomeSectionHeader(title: String, badge: String? = null, subtitle: String? = null) {
    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = CyberTextPrimary)
            if (badge != null) {
                Box(modifier = Modifier.clip(RoundedCornerShape(999.dp))
                    .background(CyberAccent.copy(0.12f)).padding(horizontal = 8.dp, vertical = 2.dp)) {
                    Text(badge, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CyberAccent)
                }
            }
        }
        if (subtitle != null) Text(subtitle, fontSize = 11.sp, color = CyberTextMuted)
    }
}

// ─── Compact booking card ─────────────────────────────────────────────────────

@Composable
private fun CompactBookingCard(booking: Booking, modifier: Modifier = Modifier) {
    val (statusColor, statusLabel) = when (booking.status) {
        "CONFIRMED" -> CyberSuccess to "Confirmed"
        "PENDING"   -> CyberWarning to "Pending"
        "DECLINED"  -> CyberDanger  to "Declined"
        else        -> CyberTextMuted to booking.status
    }
    Box(modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
        .background(CyberBgCard)
        .border(1.dp, statusColor.copy(0.25f), RoundedCornerShape(16.dp)).padding(14.dp)) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(40.dp).clip(CircleShape).background(statusColor.copy(0.12f)),
                    contentAlignment = Alignment.Center) {
                    Text(booking.coachName.take(1).uppercase(),
                        fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = statusColor)
                }
                Column {
                    Text(booking.coachName.ifEmpty { "Coach" }, fontSize = 14.sp,
                        fontWeight = FontWeight.Bold, color = CyberTextPrimary)
                    Text(if (booking.feeAmount > 0) "₹${booking.feeAmount}/session" else "Session request",
                        fontSize = 12.sp, color = CyberTextMuted)
                }
            }
            Box(Modifier.clip(RoundedCornerShape(999.dp)).background(statusColor.copy(0.12f))
                .padding(horizontal = 10.dp, vertical = 4.dp)) {
                Text(statusLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = statusColor)
            }
        }
    }
}

// ─── Trainer card ─────────────────────────────────────────────────────────────

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun TrainerCard(
    trainer: TrainerProfile,
    rank: Int = 0,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rankEmoji = when (rank) { 1 -> "🥇"; 2 -> "🥈"; 3 -> "🥉"; else -> null }
    Box(
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
            .background(CyberBgCard)
            .border(
                if (rank in 1..3) 1.5.dp else 1.dp,
                if (rank == 1) CyberAccent.copy(0.35f) else Color.White.copy(0.06f),
                RoundedCornerShape(20.dp)
            )
            .clickable { onClick() }.padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box {
                    Box(
                        modifier = Modifier.size(52.dp).clip(CircleShape)
                            .background(CyberAccent.copy(0.12f))
                            .border(1.dp, CyberAccent.copy(0.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (trainer.profileImageUrl.isNotBlank()) {
                            AsyncImage(
                                model = trainer.profileImageUrl,
                                contentDescription = trainer.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize().clip(CircleShape)
                            )
                        } else {
                            Text(trainer.name.take(1).uppercase(),
                                fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = CyberAccent)
                        }
                    }
                    if (rankEmoji != null) Text(rankEmoji, fontSize = 14.sp, modifier = Modifier.align(Alignment.BottomEnd))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            trainer.name, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold,
                            color = CyberTextPrimary, maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (trainer.isFeatured) {
                            Box(
                                Modifier.clip(RoundedCornerShape(999.dp))
                                    .background(CyberAccent.copy(0.15f))
                                    .border(1.dp, CyberAccent.copy(0.4f), RoundedCornerShape(999.dp))
                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                            ) {
                                Text("⚡ FEATURED", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold,
                                    color = CyberAccent, maxLines = 1, softWrap = false)
                            }
                        } else if (trainer.profileScore >= com.example.data.PortfolioScoring.TIER_ELITE) {
                            Box(
                                Modifier.clip(RoundedCornerShape(999.dp))
                                    .background(CyberSuccess.copy(0.12f))
                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                            ) {
                                Text("🏆 ELITE", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold,
                                    color = CyberSuccess, maxLines = 1, softWrap = false)
                            }
                        }
                    }
                    val specs = trainer.specialty.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    Text(
                        if (specs.isEmpty()) "Personal Training"
                        else specs.take(2).joinToString(" · ") + if (specs.size > 2) " +${specs.size - 2}" else "",
                        fontSize = 12.sp, color = CyberAccent, fontWeight = FontWeight.SemiBold,
                        maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
                if (trainer.rating > 0f) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("⭐", fontSize = 14.sp)
                        Text("%.1f".format(trainer.rating), fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = CyberTextPrimary)
                        if (trainer.ratingCount > 0) {
                            Text("${trainer.ratingCount} review${if (trainer.ratingCount > 1) "s" else ""}",
                                fontSize = 9.sp, color = CyberTextMuted, maxLines = 1, softWrap = false)
                        }
                    }
                } else {
                    Box(Modifier.clip(RoundedCornerShape(10.dp)).background(CyberSuccess.copy(0.12f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)) {
                        Text("Available", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CyberSuccess)
                    }
                }
            }
            val blurb = trainer.headline.ifBlank { trainer.bio }
            if (blurb.isNotBlank()) {
                Text(blurb.take(100) + if (blurb.length > 100) "…" else "",
                    fontSize = 12.sp, color = CyberTextSecondary, lineHeight = 16.sp)
            }
            // FlowRow so chips wrap to the next line on narrow screens / large
            // font scales instead of crushing the last chip into a vertical strip
            androidx.compose.foundation.layout.FlowRow(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (trainer.city.isNotBlank()) InfoChip("📍", trainer.city.replaceFirstChar { it.uppercase() })
                if (trainer.feePerSession > 0) InfoChip("💰", "₹${trainer.feePerSession}/session")
                if (trainer.yearsExperience > 0) InfoChip("🏅", "${trainer.yearsExperience} yrs exp")
                else if (trainer.availabilityDays.isNotBlank()) {
                    val days = trainer.availabilityDays.split(",").filter { it.isNotBlank() }.size
                    InfoChip("📅", "$days days/wk")
                }
            }
        }
    }
}

@Composable
private fun InfoChip(icon: String, label: String) {
    Box(Modifier.clip(RoundedCornerShape(999.dp)).background(CyberBgCardElevated)
        .padding(horizontal = 10.dp, vertical = 5.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(icon, fontSize = 11.sp)
            // maxLines + no soft wrap: a squeezed chip must never wrap char-by-char
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = CyberTextSecondary,
                maxLines = 1, softWrap = false)
        }
    }
}

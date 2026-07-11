package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.Booking
import com.example.data.FirestoreSync
import com.example.data.GeoUtils
import com.example.data.TrainerProfile
import com.example.data.UserPreferences
import com.example.data.isFeatured
import com.example.data.meritScore
import com.example.data.tierRank
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Discover ranking: subscription league first (Business > Pro > Free), then
 * EARNED merit within the league (smoothed member rating + review volume +
 * profile strength). See [com.example.data.meritScore].
 */
private val trainerRank = compareByDescending<TrainerProfile> { it.tierRank }
    .thenByDescending { it.meritScore }
    .thenByDescending { it.rating }

class ClientViewModel(val userPreferences: UserPreferences) : ViewModel() {

    // ── Displayed feed (append-only for infinite scroll, replaced on new search) ──
    private val _trainers = MutableStateFlow<List<TrainerProfile>>(emptyList())
    val trainers: StateFlow<List<TrainerProfile>> = _trainers.asStateFlow()

    // ── Infinite-feed state ───────────────────────────────────────────────────────
    /** All public coaches fetched from Firestore — filtered client-side for the feed */
    private var allCoachesCache: List<TrainerProfile> = emptyList()

    /** UIDs already in the displayed list — prevents duplicates when appending */
    private var displayedUids = mutableSetOf<String>()

    /** Current auto-expansion radius (ignored in manual mode) */
    private var autoFeedRadiusKm: Int = 10

    /** True when user has tapped a radius chip — disables auto-expansion */
    private var isManualRadiusMode: Boolean = false

    private val _isExpandingFeed = MutableStateFlow(false)
    val isExpandingFeed: StateFlow<Boolean> = _isExpandingFeed.asStateFlow()

    private val _hasMoreCoaches = MutableStateFlow(false)
    val hasMoreCoaches: StateFlow<Boolean> = _hasMoreCoaches.asStateFlow()

    /** Exposed so the UI can show "Showing coaches within Xkm" */
    private val _autoFeedRadius = MutableStateFlow(10)
    val autoFeedRadius: StateFlow<Int> = _autoFeedRadius.asStateFlow()

    // ── Other state ───────────────────────────────────────────────────────────────
    private val _myBookings = MutableStateFlow<List<Booking>>(emptyList())
    val myBookings: StateFlow<List<Booking>> = _myBookings.asStateFlow()

    private val _isLoadingTrainers = MutableStateFlow(false)
    val isLoadingTrainers: StateFlow<Boolean> = _isLoadingTrainers.asStateFlow()

    private val _loadError = MutableStateFlow("")
    val loadError: StateFlow<String> = _loadError.asStateFlow()

    private val _selectedTrainer = MutableStateFlow<TrainerProfile?>(null)
    val selectedTrainer: StateFlow<TrainerProfile?> = _selectedTrainer.asStateFlow()

    private val _coachReviews = MutableStateFlow<List<FirestoreSync.CoachReview>>(emptyList())
    val coachReviews: StateFlow<List<FirestoreSync.CoachReview>> = _coachReviews.asStateFlow()

    private val _clientLat = MutableStateFlow(userPreferences.clientLat)
    private val _clientLng = MutableStateFlow(userPreferences.clientLng)
    private val _radiusKm  = MutableStateFlow(userPreferences.clientRadiusKm)
    val radiusKm: StateFlow<Int> = _radiusKm.asStateFlow()

    // ── Temporary area search (does NOT save to profile) ─────────────────────────
    private val _searchOverrideLabel = MutableStateFlow("")
    val searchOverrideLabel: StateFlow<String> = _searchOverrideLabel.asStateFlow()

    val clientName: String get() = userPreferences.clientName.ifEmpty { userPreferences.coachName }
    val clientCity: String get() = userPreferences.clientCity
    val clientLat:  Double get() = _clientLat.value
    val clientLng:  Double get() = _clientLng.value

    init {
        loadTrainers()
        loadMyBookings()
    }

    /**
     * Fetches ALL public coaches, caches them, then applies the initial
     * 10 km auto-feed filter (or shows all when no location is set).
     * Resets auto-expansion state completely.
     */
    fun loadTrainers() {
        isManualRadiusMode = false
        autoFeedRadiusKm   = 10
        _autoFeedRadius.value = 10
        displayedUids.clear()

        viewModelScope.launch {
            _isLoadingTrainers.value = true
            _loadError.value = ""
            _trainers.value = emptyList()
            try {
                // Fetch ALL coaches without server-side radius so we can
                // expand the feed client-side without re-fetching.
                allCoachesCache = FirestoreSync.getPublicTrainers(
                    clientLat = 0.0, clientLng = 0.0, radiusKm = 0
                )
                applyFeed(append = false)
            } catch (e: Exception) {
                _trainers.value = emptyList()
                _loadError.value = e.message ?: "Unknown error"
                android.util.Log.e("CoachOps", "loadTrainers failed: ${e.message}", e)
            } finally {
                _isLoadingTrainers.value = false
            }
        }
    }

    /**
     * User tapped a radius chip (manual filter).
     * Switches out of auto-feed mode and shows exactly [km] km (0 = all).
     */
    fun setRadius(km: Int) {
        isManualRadiusMode = true
        _radiusKm.value = km
        userPreferences.clientRadiusKm = km
        displayedUids.clear()
        _trainers.value = emptyList()
        applyFeed(append = false)
    }

    /**
     * Called from the scroll listener when the user is near the bottom of the
     * list. Silently appends the next radius band (+5 km) of coaches.
     */
    fun expandFeedRadius() {
        if (isManualRadiusMode) return
        if (_isExpandingFeed.value) return
        if (!_hasMoreCoaches.value) return
        val lat = _clientLat.value
        val lng = _clientLng.value
        if (lat == 0.0 && lng == 0.0) return   // no location — nothing to expand

        autoFeedRadiusKm += 5
        _autoFeedRadius.value = autoFeedRadiusKm

        viewModelScope.launch {
            _isExpandingFeed.value = true
            applyFeed(append = true)
            _isExpandingFeed.value = false
        }
    }

    /**
     * Applies client-side filtering from [allCoachesCache].
     *
     * [append] = false → replace the list (new search / radius chip)
     * [append] = true  → add only coaches NOT already shown (scroll expansion)
     *
     * Coaches that have no stored coordinates are always surfaced after all
     * geo-filtered coaches so existing registrations stay discoverable.
     */
    private fun applyFeed(append: Boolean) {
        val lat = _clientLat.value
        val lng = _clientLng.value
        val hasLocation = lat != 0.0 || lng != 0.0

        if (!hasLocation) {
            // No location — featured (paying) coaches first, then profile strength, then rating
            val all = allCoachesCache.sortedWith(trainerRank)
            _trainers.value = all
            displayedUids.addAll(all.map { it.uid })
            _hasMoreCoaches.value = false
            _autoFeedRadius.value = 0
            return
        }

        val radiusToUse = when {
            isManualRadiusMode && _radiusKm.value == 0 -> Int.MAX_VALUE  // Anywhere
            isManualRadiusMode                          -> _radiusKm.value
            else                                        -> autoFeedRadiusKm
        }

        // Split cache: coaches with coordinates vs without
        val withCoords    = allCoachesCache.filter { it.lat != 0.0 || it.lng != 0.0 }
        val withoutCoords = allCoachesCache.filter { it.lat == 0.0 && it.lng == 0.0 }

        // Sort by distance from member
        val sorted = withCoords
            .map { c -> c to GeoUtils.distanceKm(lat, lng, c.lat, c.lng) }
            .sortedBy { (_, dist) -> dist }

        // Coaches that fall within the current radius and aren't shown yet
        val inRadius = sorted
            .filter { (c, dist) -> dist <= radiusToUse && c.uid !in displayedUids }
            .map { (c, _) -> c }

        // Coaches without coordinates (legacy) — appended at the end, once per session
        val noCoordNew = withoutCoords.filter { it.uid !in displayedUids }

        // Featured coaches surface first within each band; the sort is stable, so
        // distance order is preserved inside the featured and non-featured groups.
        val newCoaches = if (!append && !isManualRadiusMode) {
            // Fresh auto-feed load: show in-radius + no-coord coaches
            inRadius.sortedWith(trainerRank) + noCoordNew.sortedWith(trainerRank)
        } else if (!append) {
            // Manual radius chip: show exactly what's in range
            inRadius.sortedWith(trainerRank) +
                if (radiusToUse == Int.MAX_VALUE) noCoordNew.sortedWith(trainerRank) else emptyList()
        } else {
            // Scroll expansion: only append the new in-radius band
            inRadius.sortedWith(trainerRank)
        }

        displayedUids.addAll(newCoaches.map { it.uid })

        _trainers.value = if (append) _trainers.value + newCoaches else newCoaches

        // Still more coaches outside current radius?
        val remaining = sorted.count { (c, dist) ->
            c.uid !in displayedUids && dist > radiusToUse
        }
        _hasMoreCoaches.value = !isManualRadiusMode && remaining > 0
    }

    fun setClientCoordinates(lat: Double, lng: Double) {
        _searchOverrideLabel.value = ""          // clear any area override
        _clientLat.value = lat
        _clientLng.value = lng
        userPreferences.clientLat = lat
        userPreferences.clientLng = lng
        loadTrainers()
    }

    /** Search coaches in [label] area without changing the user's saved profile location. */
    fun searchInArea(lat: Double, lng: Double, label: String) {
        _searchOverrideLabel.value = label
        _clientLat.value = lat
        _clientLng.value = lng
        loadTrainers()
    }

    /** Restore the user's saved profile location and clear the area override. */
    fun clearSearchOverride() {
        _searchOverrideLabel.value = ""
        _clientLat.value = userPreferences.clientLat
        _clientLng.value = userPreferences.clientLng
        loadTrainers()
    }

    fun loadTrainerById(uid: String) {
        viewModelScope.launch {
            _selectedTrainer.value = FirestoreSync.getTrainerById(uid)
            _coachReviews.value = FirestoreSync.getCoachReviews(uid)
        }
    }

    fun loadMyBookings() {
        viewModelScope.launch {
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
            try {
                _myBookings.value = FirestoreSync.getClientBookings(uid)
                    .sortedByDescending { it.createdAtMillis }
            } catch (_: Exception) {
                _myBookings.value = emptyList()
            }
        }
    }

    suspend fun cancelBooking(bookingId: String): Boolean {
        val name = userPreferences.clientName.ifEmpty { "Member" }
        val result = FirestoreSync.cancelBooking(bookingId, name)
        if (result) loadMyBookings()
        return result
    }

    fun requestBooking(
        trainer: TrainerProfile,
        notes: String,
        sessionDateMillis: Long = 0L,
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
            try {
                FirestoreSync.createBooking(
                    coachId           = trainer.uid,
                    coachName         = trainer.name,
                    clientId          = uid,
                    // Members store their name in clientName, not coachName — using
                    // coachName sent a blank name on the request AND the public review
                    clientName        = clientName,
                    feeAmount         = trainer.feePerSession,
                    notes             = notes,
                    sessionDateMillis = sessionDateMillis
                )
                loadMyBookings()
                onResult(true)
            } catch (_: Exception) {
                onResult(false)
            }
        }
    }

    fun rateCoach(bookingId: String, coachId: String, rating: Float, review: String = "", onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                FirestoreSync.rateBooking(bookingId, rating, review)
                loadMyBookings()
                onResult(true)
            } catch (_: Exception) {
                onResult(false)
            }
        }
    }

    fun updateClientCity(city: String) { userPreferences.clientCity = city }
    fun updateClientName(name: String) { userPreferences.coachName = name }

    /** Phone used to auto-match this member's gym membership (shared phone field). */
    fun userPhone(): String = userPreferences.coachPhone

    /**
     * Signs out and THEN invokes [onDone] — the caller navigates only after the
     * session is fully cleared. Navigating first caused the "sign-out keeps me
     * in the app" bug: Activity.recreate() cancelled this coroutine before
     * signOut/clearLocalSession ran, so the relaunch saw a live session.
     */
    fun logout(context: android.content.Context, onDone: () -> Unit = {}) {
        com.example.data.EntitlementManager.stop()
        viewModelScope.launch {
            try {
                // Needs auth to write — bounded so offline sign-out never hangs
                kotlinx.coroutines.withTimeout(4_000) { FirestoreSync.clearFcmToken() }
            } catch (_: Exception) { }
            com.example.data.AuthRepository.signOut(context)
            userPreferences.clearLocalSession()
            onDone()
        }
    }
}

class ClientViewModelFactory(private val userPreferences: UserPreferences) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        ClientViewModel(userPreferences) as T
}

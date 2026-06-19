package com.example.data

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import com.example.BuildConfig
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest
import java.util.Locale
import kotlin.math.*

data class NearbyArea(
    val name: String,       // Display name (e.g. "Koramangala, Bengaluru, Karnataka")
    val cityName: String,   // Parent city (e.g. "Bengaluru")
    val distKm: Double,     // Distance from GPS point; 0.0 for search results
    val lat: Double,
    val lng: Double,
    val placeId: String = "" // Google place_id; set for Places API results, empty for GPS areas
)

object GeoUtils {

    /** Haversine formula — returns distance in km between two lat/lng points */
    fun distanceKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val R = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2).pow(2)
        return R * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    /**
     * Geocodes a city/area name → (lat, lng).
     * Tries raw query first; if that fails, appends ", India" for area context.
     * Must be called off the main thread.
     */
    @Suppress("DEPRECATION")
    fun geocodeCity(context: Context, location: String): Pair<Double, Double>? {
        if (location.isBlank()) return null
        val geocoder = Geocoder(context, Locale.getDefault())
        val queries = buildList {
            add(location.trim())
            if (!location.contains("india", ignoreCase = true))
                add("${location.trim()}, India")
        }
        for (query in queries) {
            try {
                val addr = geocoder.getFromLocationName(query, 1)?.firstOrNull()
                if (addr != null) return addr.latitude to addr.longitude
            } catch (_: Exception) { }
        }
        return null
    }

    /**
     * Gets the device's current GPS location.
     * Always requests a fresh fix with HIGH_ACCURACY so we never return a
     * stale cached location (e.g. emulator's Mountain View default).
     * Falls back to lastLocation only if it is less than 2 minutes old.
     */
    @SuppressLint("MissingPermission")
    suspend fun getDeviceLocation(context: Context): Pair<Double, Double>? {
        return try {
            val client = LocationServices.getFusedLocationProviderClient(context)
            val cts = CancellationTokenSource()
            // Request a fresh GPS fix first
            val fresh = client.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cts.token
            ).await()
            if (fresh != null) {
                fresh.latitude to fresh.longitude
            } else {
                // Fallback: use lastLocation only if it's recent (< 2 min)
                val last = client.lastLocation.await()
                val ageMs = System.currentTimeMillis() - (last?.time ?: 0L)
                if (last != null && ageMs < 2 * 60 * 1000L) {
                    last.latitude to last.longitude
                } else null
            }
        } catch (_: Exception) { null }
    }

    /**
     * Reverse geocodes coordinates → human-readable area/city name.
     * Returns sub-locality (area) when available, otherwise city.
     */
    @Suppress("DEPRECATION")
    fun reverseGeocode(context: Context, lat: Double, lng: Double): String {
        return try {
            val addr = Geocoder(context, Locale.getDefault()).getFromLocation(lat, lng, 1)?.firstOrNull()
            addr?.subLocality?.takeIf { it.isNotBlank() }
                ?: addr?.locality?.takeIf { it.isNotBlank() }
                ?: addr?.subAdminArea?.takeIf { it.isNotBlank() }
                ?: "Near You"
        } catch (_: Exception) { "Near You" }
    }

    /**
     * Google Places Autocomplete — returns up to 5 location suggestions for [query].
     * Includes Android certificate headers so the key restriction works with REST calls.
     * Falls back to Android Geocoder if Places API fails (key not set up, no network, etc.)
     */
    suspend fun searchLocations(context: Context, query: String): List<NearbyArea> =
        withContext(Dispatchers.IO) {
            if (query.length < 2) return@withContext emptyList()
            // Try Places API first
            val placesResult = searchViaPlacesApi(context, query)
            if (placesResult.isNotEmpty()) return@withContext placesResult
            // Fallback: Android Geocoder
            return@withContext searchViaGeocoder(context, query)
        }

    private fun searchViaPlacesApi(context: Context, query: String): List<NearbyArea> {
        return try {
            val encoded = URLEncoder.encode(query.trim(), "UTF-8")
            val url = "https://maps.googleapis.com/maps/api/place/autocomplete/json" +
                "?input=$encoded&types=geocode&components=country:in&language=en" +
                "&key=${BuildConfig.MAPS_API_KEY}"
            val json = JSONObject(httpGet(url, androidHeaders(context)))
            if (json.optString("status") != "OK") return emptyList()
            val predictions = json.getJSONArray("predictions")
            (0 until minOf(predictions.length(), 5)).map { i ->
                val pred = predictions.getJSONObject(i)
                val sf   = pred.optJSONObject("structured_formatting")
                val sec  = sf?.optString("secondary_text") ?: ""
                val city = sec.split(",").firstOrNull()?.trim() ?: ""
                NearbyArea(
                    name     = pred.getString("description").removeSuffix(", India").trim(),
                    cityName = city,
                    distKm   = 0.0,
                    lat      = 0.0,
                    lng      = 0.0,
                    placeId  = pred.getString("place_id")
                )
            }
        } catch (_: Exception) { emptyList() }
    }

    @Suppress("DEPRECATION")
    private fun searchViaGeocoder(context: Context, query: String): List<NearbyArea> {
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val queries = listOf(query.trim(), "${query.trim()}, India")
            val seen = mutableSetOf<String>()
            val results = mutableListOf<NearbyArea>()
            for (q in queries) {
                val addrs = geocoder.getFromLocationName(q, 5) ?: continue
                for (addr in addrs) {
                    val parts = listOfNotNull(
                        addr.subLocality?.takeIf { it.isNotBlank() },
                        addr.locality?.takeIf    { it.isNotBlank() },
                        addr.adminArea?.takeIf   { it.isNotBlank() }
                    ).distinct()
                    val name = parts.take(3).joinToString(", ")
                    if (name.isBlank() || (addr.latitude == 0.0 && addr.longitude == 0.0)) continue
                    val key = "%.4f,%.4f".format(addr.latitude, addr.longitude)
                    if (seen.add(key)) {
                        results.add(NearbyArea(
                            name     = name,
                            cityName = addr.locality?.takeIf { it.isNotBlank() } ?: "",
                            distKm   = 0.0,
                            lat      = addr.latitude,
                            lng      = addr.longitude
                        ))
                    }
                }
                if (results.size >= 5) break
            }
            results.take(5)
        } catch (_: Exception) { emptyList() }
    }

    /**
     * Resolves a Google place_id → (lat, lng) via Place Details API.
     * Pass context so Android certificate headers are included.
     */
    suspend fun resolvePlace(placeId: String, context: Context? = null): Pair<Double, Double>? =
        withContext(Dispatchers.IO) {
            try {
                val url = "https://maps.googleapis.com/maps/api/place/details/json" +
                    "?place_id=$placeId&fields=geometry&key=${BuildConfig.MAPS_API_KEY}"
                val headers = if (context != null) androidHeaders(context) else emptyMap()
                val json    = JSONObject(httpGet(url, headers))
                if (json.optString("status") != "OK") return@withContext null
                val loc = json.getJSONObject("result")
                    .getJSONObject("geometry").getJSONObject("location")
                loc.getDouble("lat") to loc.getDouble("lng")
            } catch (_: Exception) { null }
        }

    /**
     * Android app restriction headers — required when the API key has
     * "Android apps" restriction set in Google Cloud Console.
     */
    private fun androidHeaders(context: Context): Map<String, String> = try {
        @Suppress("DEPRECATION")
        val sigs = context.packageManager
            .getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
            .signatures ?: return emptyMap()
        val sha1 = MessageDigest.getInstance("SHA-1").digest(sigs[0].toByteArray())
            .joinToString("") { "%02X".format(it) }
        mapOf("X-Android-Package" to context.packageName, "X-Android-Cert" to sha1)
    } catch (_: Exception) { emptyMap() }

    /** Minimal HTTP GET — must run on IO thread. */
    private fun httpGet(url: String, headers: Map<String, String> = emptyMap()): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        headers.forEach { (k, v) -> conn.setRequestProperty(k, v) }
        conn.connectTimeout = 8_000
        conn.readTimeout    = 8_000
        return try { conn.inputStream.bufferedReader().readText() }
        finally   { conn.disconnect() }
    }

    /**
     * Probes 12 offset points around [lat,lng] and reverse-geocodes each to collect
     * nearby distinct sub-locality / locality names — the same trick Rapido uses.
     * Returns up to 8 unique areas sorted by distance, deduplicated by name.
     * Must be called from a coroutine; runs all probes in parallel on IO dispatcher.
     */
    @Suppress("DEPRECATION")
    suspend fun getNearbyAreas(context: Context, lat: Double, lng: Double): List<NearbyArea> =
        withContext(Dispatchers.IO) {
            // 1° lat ≈ 111 km, so 0.009° ≈ 1 km
            val offsets = listOf(
                0.000 to 0.000,   // exact location
                0.009 to 0.000,   // 1 km N
               -0.009 to 0.000,   // 1 km S
                0.000 to 0.009,   // 1 km E
                0.000 to -0.009,  // 1 km W
                0.007 to 0.007,   // ~1 km NE
               -0.007 to 0.007,   // ~1 km SE
               -0.007 to -0.007,  // ~1 km SW
                0.007 to -0.007,  // ~1 km NW
                0.018 to 0.000,   // 2 km N
                0.000 to 0.018,   // 2 km E
               -0.018 to 0.000,   // 2 km S
            )
            val geocoder = Geocoder(context, Locale.getDefault())
            val seenNames = mutableSetOf<String>()
            val results = mutableListOf<NearbyArea>()

            // Run all probes in parallel
            offsets.map { (dLat, dLng) ->
                async {
                    try {
                        val pLat = lat + dLat
                        val pLng = lng + dLng
                        val addr = geocoder.getFromLocation(pLat, pLng, 1)?.firstOrNull()
                        val area = addr?.subLocality?.takeIf { it.isNotBlank() }
                            ?: addr?.locality?.takeIf { it.isNotBlank() }
                        val city = addr?.locality?.takeIf { it.isNotBlank() } ?: ""
                        if (area != null) {
                            NearbyArea(
                                name    = area,
                                cityName = city,
                                distKm  = distanceKm(lat, lng, pLat, pLng),
                                lat     = pLat,
                                lng     = pLng
                            )
                        } else null
                    } catch (_: Exception) { null }
                }
            }.awaitAll()
                .filterNotNull()
                .sortedBy { it.distKm }
                .forEach { area ->
                    if (seenNames.add(area.name)) results.add(area)
                }

            results.take(8)
        }
}

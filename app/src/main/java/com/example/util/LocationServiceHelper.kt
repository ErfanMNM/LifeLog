package com.example.util

import android.annotation.SuppressLint
import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult as GmsLocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

data class LocationResult(
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val speedKmh: Double = 0.0
)

data class LocationTrackPoint(
    val latitude: Double,
    val longitude: Double,
    val speedKmh: Double,
    val timestamp: Long,
    val accuracyMeters: Float = 0f
)

class LocationServiceHelper(private val context: Context) {
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): LocationResult? = withContext(Dispatchers.IO) {
        try {
            val location: Location? = suspendCancellableCoroutine { continuation ->
                val cts = CancellationTokenSource()
                continuation.invokeOnCancellation { cts.cancel() }

                fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                    .addOnSuccessListener { loc ->
                        if (loc != null) {
                            continuation.resume(loc)
                        } else {
                            // Fallback to last known location
                            fusedLocationClient.lastLocation
                                .addOnSuccessListener { lastLoc -> continuation.resume(lastLoc) }
                                .addOnFailureListener { continuation.resume(null) }
                        }
                    }
                    .addOnFailureListener {
                        fusedLocationClient.lastLocation
                            .addOnSuccessListener { lastLoc -> continuation.resume(lastLoc) }
                            .addOnFailureListener { continuation.resume(null) }
                    }
            }

            if (location != null) {
                val address = resolveAddress(location.latitude, location.longitude)
                val speedKmh = if (location.hasSpeed()) (location.speed * 3.6).coerceAtLeast(0.0) else 0.0
                LocationResult(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    address = address,
                    speedKmh = speedKmh
                )
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    @SuppressLint("MissingPermission")
    fun trackLocationFlow(intervalMs: Long = 4000L): Flow<LocationTrackPoint> = callbackFlow {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMs)
            .setMinUpdateIntervalMillis(2000L)
            .setMinUpdateDistanceMeters(3f)
            .build()

        var lastLocation: Location? = null
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: GmsLocationResult) {
                val loc = result.lastLocation ?: return
                val now = System.currentTimeMillis()
                var speedKmh = if (loc.hasSpeed() && loc.speed > 0f) {
                    (loc.speed * 3.6).coerceIn(0.0, 250.0)
                } else {
                    0.0
                }

                val prev = lastLocation
                if (speedKmh <= 0.0 && prev != null) {
                    val distKm = calculateDistanceKm(prev.latitude, prev.longitude, loc.latitude, loc.longitude)
                    val dtHours = ((loc.time - prev.time).coerceAtLeast(1000L)).toDouble() / 3_600_000.0
                    if (distKm >= 0.003 && dtHours > 0.0) {
                        speedKmh = (distKm / dtHours).coerceIn(0.0, 250.0)
                    }
                }
                lastLocation = loc

                trySend(
                    LocationTrackPoint(
                        latitude = loc.latitude,
                        longitude = loc.longitude,
                        speedKmh = speedKmh,
                        timestamp = now,
                        accuracyMeters = if (loc.hasAccuracy()) loc.accuracy else 0f
                    )
                )
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                request,
                callback,
                Looper.getMainLooper()
            )
        } catch (_: SecurityException) {
            close()
        } catch (_: Exception) {
            close()
        }

        awaitClose {
            try {
                fusedLocationClient.removeLocationUpdates(callback)
            } catch (_: Exception) {}
        }
    }

    fun resolveAddressSync(latitude: Double, longitude: Double): String {
        return resolveAddress(latitude, longitude)
    }

    private fun resolveAddress(latitude: Double, longitude: Double): String {
        return try {
            val geocoder = Geocoder(context, Locale("vi", "VN"))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                formatAddressList(addresses)
            } else {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                formatAddressList(addresses)
            }
        } catch (e: Exception) {
            "GPS: ${String.format(Locale.US, "%.5f, %.5f", latitude, longitude)}"
        }
    }

    private fun formatAddressList(addresses: List<Address>?): String {
        if (addresses.isNullOrEmpty()) return "Vị trí không xác định"
        val addr = addresses[0]
        val feature = addr.subThoroughfare ?: addr.featureName
        val thoroughfare = addr.thoroughfare
        val subAdmin = addr.subAdminArea ?: addr.subLocality
        val admin = addr.adminArea

        val parts = mutableListOf<String>()
        if (!feature.isNullOrBlank()) parts.add(feature)
        if (!thoroughfare.isNullOrBlank() && thoroughfare != feature) parts.add(thoroughfare)
        if (!subAdmin.isNullOrBlank()) parts.add(subAdmin)
        if (!admin.isNullOrBlank()) parts.add(admin)

        return if (parts.isNotEmpty()) {
            parts.joinToString(", ")
        } else {
            addr.getAddressLine(0) ?: "Vị trí đã định vị"
        }
    }

    companion object {
        fun calculateDistanceKm(
            lat1: Double,
            lon1: Double,
            lat2: Double,
            lon2: Double
        ): Double {
            val results = FloatArray(1)
            Location.distanceBetween(lat1, lon1, lat2, lon2, results)
            return (results[0].toDouble() / 1000.0).coerceAtLeast(0.0)
        }
    }
}

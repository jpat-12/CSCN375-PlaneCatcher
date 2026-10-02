package com.planecatcher.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import com.planecatcher.core.model.GeoPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.merge
import javax.inject.Inject
import javax.inject.Singleton

data class DeviceLocation(val point: GeoPoint, val isMock: Boolean)

/**
 * Device location. Uses Google's Fused Location Provider when Play services is
 * present, and always also listens to Android's own LocationManager, so location
 * works on devices without Google (Windows Subsystem for Android, de-Googled phones).
 */
@Singleton
class LocationSource @Inject constructor(
    @ApplicationContext private val context: Context,
    private val client: FusedLocationProviderClient,
) {
    fun hasPermission(): Boolean =
        listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION).any {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }

    /** Emits location updates; completes immediately if permission is missing. */
    fun updates(): Flow<DeviceLocation> {
        if (!hasPermission()) return emptyFlow()
        return if (hasPlayServices()) merge(fusedUpdates(), platformUpdates()) else platformUpdates()
    }

    private fun hasPlayServices(): Boolean = try {
        GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS
    } catch (_: Exception) {
        false
    }

    @SuppressLint("MissingPermission")
    private fun fusedUpdates(): Flow<DeviceLocation> = callbackFlow {
        val request = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, UPDATE_MS)
            .setMinUpdateDistanceMeters(MIN_DISTANCE_M)
            .build()
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { trySend(it.toDevice()) }
            }
        }
        try {
            client.lastLocation.addOnSuccessListener { it?.let { loc -> trySend(loc.toDevice()) } }
            client.requestLocationUpdates(request, callback, Looper.getMainLooper())
        } catch (_: Exception) {
            // Play services misbehaving; the platform listener still runs.
        }
        awaitClose { client.removeLocationUpdates(callback) }
    }

    @SuppressLint("MissingPermission")
    private fun platformUpdates(): Flow<DeviceLocation> = callbackFlow {
        val manager = context.getSystemService(LocationManager::class.java)
        if (manager == null) {
            close()
            return@callbackFlow
        }
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                trySend(location.toDevice())
            }

            // Needed on API < 30, where these are abstract.
            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
            override fun onProviderEnabled(provider: String) = Unit
            override fun onProviderDisabled(provider: String) = Unit
        }
        val providers = manager.allProviders.filter { it != LocationManager.PASSIVE_PROVIDER }
        // Start with the freshest last-known fix so the radar can begin right away.
        providers
            .mapNotNull { runCatching { manager.getLastKnownLocation(it) }.getOrNull() }
            .maxByOrNull { it.time }
            ?.let { trySend(it.toDevice()) }
        for (provider in providers) {
            try {
                manager.requestLocationUpdates(provider, UPDATE_MS, MIN_DISTANCE_M, listener, Looper.getMainLooper())
            } catch (_: Exception) {
                // Provider unavailable or not permitted (e.g. GPS with coarse permission only).
            }
        }
        awaitClose { manager.removeUpdates(listener) }
    }

    private fun Location.toDevice() = DeviceLocation(
        point = GeoPoint(latitude, longitude),
        isMock = if (Build.VERSION.SDK_INT >= 31) isMock else @Suppress("DEPRECATION") isFromMockProvider,
    )

    private companion object {
        const val UPDATE_MS = 30_000L
        const val MIN_DISTANCE_M = 100f
    }
}

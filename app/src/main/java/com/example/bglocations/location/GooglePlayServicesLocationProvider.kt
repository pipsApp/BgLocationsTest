package com.example.bglocations.location

import android.Manifest.permission
import android.app.PendingIntent
import android.location.Location
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresPermission
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationAvailability
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class GooglePlayServicesLocationProvider(
    private val fusedLocationProviderClient: FusedLocationProviderClient,
    private val locationRequest: LocationRequest, private val locationUpdateTimeout: Long) :
    LocationProvider {

    @RequiresPermission(
        anyOf = [permission.ACCESS_COARSE_LOCATION, permission.ACCESS_FINE_LOCATION])
    override fun requestLocationUpdates(isWithTimeout: Boolean): Flow<Location> {
        return fusedLocationProviderClient
            .locationFlow(ignoreLastKnownLocation = false, isWithTimeout)
    }

    @RequiresPermission(
        anyOf = [permission.ACCESS_COARSE_LOCATION, permission.ACCESS_FINE_LOCATION])
    override suspend fun requestSingleUpdateWithTimeoutOrNull(): Location? {
        return fusedLocationProviderClient
            .locationFlow(ignoreLastKnownLocation = true, isWithTimeout = true)
            .catch { cause -> Log.e(TAG, "requestSingleUpdateWithTimeoutOrNull, on error", cause) }
            .firstOrNull()
            ?: fusedLocationProviderClient.lastLocationOrNull()
    }

    @RequiresPermission(
        anyOf = [permission.ACCESS_COARSE_LOCATION, permission.ACCESS_FINE_LOCATION])
    private fun FusedLocationProviderClient.locationFlow(ignoreLastKnownLocation: Boolean,
                                                         isWithTimeout: Boolean): Flow<Location> =
        callbackFlow {
            var currentLocation: Location? = null
            val callback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    Log.d(TAG, "LocationCallback, onLocationResult, result: $result")
                    currentLocation = result.lastLocation
                    currentLocation?.let { trySend(it) }
                }

                override fun onLocationAvailability(locationAvailability: LocationAvailability) {
                    Log.d(TAG,
                        "LocationCallback, onLocationAvailability, locationAvailability: $locationAvailability")
                }
            }

            if (isWithTimeout) {
                launch {
                    delay(locationUpdateTimeout)
                    if (isActive && currentLocation == null) {
                        Log.e(TAG, "locationFlow, Location is not available")
                        close(Exception("Location is not available"))
                    }
                }
            }

            if (!ignoreLastKnownLocation) {
                lastLocationOrNull()?.let { location ->
                    currentLocation = location
                    trySend(location)
                }
            }
            requestLocationUpdates(locationRequest, callback, Looper.getMainLooper())
            awaitClose {
                removeLocationUpdates(callback)
            }
        }

    @RequiresPermission(
        anyOf = [permission.ACCESS_COARSE_LOCATION, permission.ACCESS_FINE_LOCATION])
    private suspend fun FusedLocationProviderClient.lastLocationOrNull(): Location? =
        suspendCancellableCoroutine { continuation ->
            lastLocation
                .addOnSuccessListener { location: Location? ->
                    if (continuation.isActive) {
                        Log.d(TAG, "lastLocationOrNull, location: $location")
                        continuation.resume(location)
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "lastLocationOrNull, exception: $exception")
                    if (continuation.isActive) {
                        continuation.resume(null)
                    }
                }
        }

    @RequiresPermission(
        anyOf = [permission.ACCESS_COARSE_LOCATION, permission.ACCESS_FINE_LOCATION])
    override fun requestLocationUpdatesViaBroadcastReceiver(pendingIntent: PendingIntent) {
        Log.d(TAG, "requestLocationUpdatesViaBroadcastReceiver")
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, pendingIntent)
    }

    companion object {
        private const val TAG = "GooglePlayServicesLocationProvider"
        const val SMALLEST_DISPLACEMENT_LOCATION_REQUEST = 100F
        const val FASTEST_INTERVAL_LOCATION_REQUEST: Long = 10 * 1000L
        const val INTERVAL_LOCATION_REQUEST: Long = 1000
        const val WEAR_SMALLEST_DISPLACEMENT_LOCATION_REQUEST = 500F
        const val WEAR_FASTEST_INTERVAL_LOCATION_REQUEST: Long = 60 * 1000L
        const val WEAR_INTERVAL_LOCATION_REQUEST: Long = 5 * 1000L
    }
}
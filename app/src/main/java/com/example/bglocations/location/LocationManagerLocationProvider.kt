package com.example.bglocations.location

import android.Manifest
import android.app.PendingIntent
import android.location.Criteria
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val UPDATE_INTERVAL_LOCATION_PROVIDER: Long = 1000
private const val MIN_DISTANCE_LOCATION_PROVIDER: Float = 100F

class LocationManagerLocationProvider(private val locationManager: LocationManager,
                                      private val locationUpdateTimeout: Long) : LocationProvider {

    private val tag = "LocationManagerLocationProvider"

    private val bestEnabledProviderWithFineAccuracy: String?
        get() = locationManager.getBestProvider(
            Criteria().apply { accuracy = Criteria.ACCURACY_FINE }, true)

    private val lastKnownLocation: Location?
        @RequiresPermission(
            anyOf = [Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION])
        get() {
            return bestEnabledProviderWithFineAccuracy?.let { provider ->
                val location = locationManager.getLastKnownLocation(provider)
                location
            }
        }

    @RequiresPermission(
        anyOf = [Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION])
    override fun requestLocationUpdates(isWithTimeout: Boolean): Flow<Location> {
        return locationManager
            .locationFlow(ignoreLastKnownLocation = false, isWithTimeout)
    }

    @RequiresPermission(
        anyOf = [Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION])
    override suspend fun requestSingleUpdateWithTimeoutOrNull(): Location? {
        return locationManager.locationFlow(ignoreLastKnownLocation = true, isWithTimeout = true)
            .firstOrNull() ?: lastKnownLocation
    }

    @RequiresPermission(
        anyOf = [Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION])
    override fun requestLocationUpdatesViaBroadcastReceiver(pendingIntent: PendingIntent) {
        locationManager.requestLocationUpdates(
            bestEnabledProviderWithFineAccuracy ?: LocationManager.GPS_PROVIDER,
            UPDATE_INTERVAL_LOCATION_PROVIDER, MIN_DISTANCE_LOCATION_PROVIDER, pendingIntent)
    }

    @RequiresPermission(
        anyOf = [Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION])
    private fun LocationManager.locationFlow(ignoreLastKnownLocation: Boolean,
                                             isWithTimeout: Boolean): Flow<Location> =
        callbackFlow {
            var currentLocation: Location? = null
            fun startRequestingLocationUpdatesWithBestProvider(locationListener: LocationListener) {
                val provider = bestEnabledProviderWithFineAccuracy
                if (provider == null) {
                    Log.e(tag, "No enabled location provider found")
                    close(Exception("No enabled location provider found"))
                } else {
                    requestLocationUpdates(provider, UPDATE_INTERVAL_LOCATION_PROVIDER,
                        MIN_DISTANCE_LOCATION_PROVIDER, locationListener)
                }
            }

            val locationListener: LocationListener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    Log.d(tag, "onLocationChanged: $location")
                    currentLocation = location
                    trySend(location)
                }

                override fun onProviderDisabled(provider: String) {
                    Log.d(tag, "onProviderDisabled: $provider")
                    startRequestingLocationUpdatesWithBestProvider(this)
                }

                override fun onProviderEnabled(provider: String) {
                    Log.d(tag, "onProviderEnabled: $provider")
                    startRequestingLocationUpdatesWithBestProvider(this)
                }

                @Deprecated("Deprecated in Java")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
                }
            }

            if (isWithTimeout) {
                launch {
                    delay(locationUpdateTimeout)
                    if (isActive && currentLocation == null) {
                        Log.e(tag, "Unable to retrieve a location within $locationUpdateTimeout")
                        close(Exception(
                            "Unable to retrieve a location within $locationUpdateTimeout"))
                    }
                }
            }

            if (!ignoreLastKnownLocation) {
                lastKnownLocation?.let { location ->
                    currentLocation = location
                    trySend(location)
                }
            }
            startRequestingLocationUpdatesWithBestProvider(locationListener)
            awaitClose { removeUpdates(locationListener) }
        }.flowOn(Dispatchers.Main)
}
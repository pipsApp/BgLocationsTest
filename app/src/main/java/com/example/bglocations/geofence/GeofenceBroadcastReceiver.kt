package com.example.bglocations.geofence

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.Location
import android.util.Log
import com.example.bglocations.location.LocationProvider
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class GeofenceBroadcastReceiver : BroadcastReceiver() {

    private val tag = "GeofenceBroadcastReceiver"
    private val requestCode = 11
    private val geofenceRadius = 1000F

    @Inject lateinit var locationProvider: LocationProvider

    private fun getGeofencePendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when calling
        // addGeofences() and removeGeofences().
        return PendingIntent.getBroadcast(context, requestCode, intent,
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(tag, "onReceive, intent: $intent")
        CoroutineScope(SupervisorJob()).launch {
            val location =
                locationProvider.requestSingleUpdateWithTimeoutOrNull()
            Log.d(tag, "onReceive, got location: $location")

            location?.let {
                LocationServices.getGeofencingClient(context).run {
                    //remove all previous geofences
                    removeGeofences(getGeofencePendingIntent(context))
                        .addOnSuccessListener { Log.d(tag, "onReceive, removeGeofences, success") }
                        .addOnFailureListener { Log.e(tag, "onReceive, removeGeofences, failure", it) }

                    //request new geofence
                    addGeofences(getGeofencingRequest(location), getGeofencePendingIntent(context))
                        .addOnSuccessListener { Log.d(tag, "onReceive, addGeofences, success") }
                        .addOnFailureListener { Log.e(tag, "onReceive, addGeofences, failure", it) }
                }
            }
        }
    }

    private fun getGeofencingRequest(location: Location): GeofencingRequest {
        Log.d(tag, "getGeofencingRequest")
        val geofence: Geofence = Geofence.Builder().apply {
            setRequestId("1")
            setCircularRegion(location.latitude, location.longitude, geofenceRadius)
            setExpirationDuration(Geofence.NEVER_EXPIRE)
            setTransitionTypes(
                Geofence.GEOFENCE_TRANSITION_EXIT or
                        Geofence.GEOFENCE_TRANSITION_ENTER)
        }.build()

        return GeofencingRequest.Builder().apply {
            setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            addGeofences(listOf(geofence))
        }.build()
    }
}
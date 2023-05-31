package com.example.bglocations.broadcastreceivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.LocationResult

class LocationUpdatesBroadcastReceiver: BroadcastReceiver() {

    private val tag = "LocationUpdatesBroadcastReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(tag, "onReceive, intent: $intent")
        val locationResult: LocationResult? = LocationResult.extractResult(intent)
        Log.d(tag, "onReceive, locationResult: $locationResult")
        locationResult?.let {
            val locations = locationResult.locations
            Log.d(tag, "onReceive, locations: $locations")
        }
    }
}
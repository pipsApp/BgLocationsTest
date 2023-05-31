package com.example.bglocations.broadcastreceivers

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.bglocations.location.LocationProvider
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class BootCompletedBroadcastReceiver : BroadcastReceiver() {

    private val tag = "BootCompletedBroadcastReceiver"

    @Inject lateinit var locationProvider: LocationProvider

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(tag, "onReceive, intent: $intent")
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED ||
                action == Intent.ACTION_LOCKED_BOOT_COMPLETED) {
            locationProvider.requestLocationUpdatesViaBroadcastReceiver(
                PendingIntent.getBroadcast(context, 10, intent,
                    PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
            context.sendBroadcast(Intent(context, GeofenceBroadcastReceiver::class.java))
        }
    }
}
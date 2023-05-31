package com.example.bglocations.ui.activity

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.bglocations.broadcastreceivers.GeofenceBroadcastReceiver
import com.example.bglocations.broadcastreceivers.LocationUpdatesBroadcastReceiver
import com.example.bglocations.location.LocationProvider
import com.example.bglocations.ui.theme.BgLocationsTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val requiredPermissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_BACKGROUND_LOCATION)

    private val allPermissionsGranted
        get() = requiredPermissions.all {
            ContextCompat.checkSelfPermission(this, it) ==
                    PackageManager.PERMISSION_GRANTED
        }

    @Inject lateinit var locationProvider: LocationProvider

    @SuppressLint("MissingPermission") override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BgLocationsTheme {

                val text = remember {
                    val initText = if (allPermissionsGranted) {
                        "All permissions granted!"
                    } else "Permissions not granted! Navigate to settings and grant permissions."
                    mutableStateOf(initText)
                }

                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxSize()) {

                    Text(
                        text = text.value,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp))
                    if (allPermissionsGranted) {
                        Button(onClick = {
//                            LocationCoroutineWorker.startWorker(this@MainActivity)
                            requestLocationUpdatesViaBroadcastReceiver()
                            sendBroadcast(Intent(this@MainActivity, GeofenceBroadcastReceiver::class.java))
                        }) {
                            Text(text = "Start location updates")
                        }
                    }
                }
            }
        }
    }

    @RequiresPermission(
        anyOf = [Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION])
    private fun requestLocationUpdatesViaBroadcastReceiver() {
        Log.d("MainActivity", "requestLocationUpdatesViaBroadcastReceiver")
        val intent = Intent(this, LocationUpdatesBroadcastReceiver::class.java)
        locationProvider.requestLocationUpdatesViaBroadcastReceiver(
            PendingIntent.getBroadcast(this, 10, intent,
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
    }

}
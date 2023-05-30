package com.example.bglocations.ui.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import com.example.bglocations.geofence.GeofenceBroadcastReceiver
import com.example.bglocations.ui.theme.BgLocationsTheme
import dagger.hilt.android.AndroidEntryPoint

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

    override fun onCreate(savedInstanceState: Bundle?) {
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
                            sendBroadcast(Intent(this@MainActivity, GeofenceBroadcastReceiver::class.java))
                        }) {
                            Text(text = "Start location updates")
                        }
                    }
                }
            }
        }
    }
}
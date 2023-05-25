package com.example.bglocations.location

import android.location.Location
import kotlinx.coroutines.flow.Flow

interface LocationProvider {

    fun requestLocationUpdates(isWithTimeout: Boolean): Flow<Location>

    suspend fun requestSingleUpdateWithTimeoutOrNull(): Location?
}
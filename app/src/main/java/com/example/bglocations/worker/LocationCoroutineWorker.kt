package com.example.bglocations.worker

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.bglocations.location.LocationProvider
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class LocationCoroutineWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted params: WorkerParameters,
    private val locationProvider: LocationProvider) : CoroutineWorker(appContext, params) {

    private val locationUpdatesChannelId = "LocationUpdatesNotification"
    private val locationUpdatesNotificationId = 1000

    override suspend fun doWork(): Result {
        Log.d(TAG, "doWork")
        try {
            setForeground(createForegroundInfo())
        } catch (e: Throwable) {
            Log.e(TAG, "doWork, unable to create foreground info", e)
            return Result.failure()
        }

        val location = locationProvider.requestSingleUpdateWithTimeoutOrNull()
        return if (location == null) {
            Log.e(TAG, "doWork, location is null")
            Result.retry()
        } else {
            Log.d(TAG, "doWork, got location: $location")
            Result.success()
        }
    }

    private fun createForegroundInfo(): ForegroundInfo {
        val notification = NotificationCompat.Builder(appContext, locationUpdatesChannelId)
            .setContentText("Requesting location updates")
            .setOngoing(true)
            .setVibrate(longArrayOf(0))
            .setLocalOnly(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(locationUpdatesNotificationId, notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            ForegroundInfo(locationUpdatesNotificationId, notification)
        }
    }

    companion object {
        private const val TAG = "LocationCoroutineWorker"
        private const val WORK_NAME = "LocationUpdatesWork"

        /**
         * Create and start worker for updates
         */
        fun startWorker(context: Context) {
            Log.d(TAG, "startWorker")
            //Create a request to start every half hour with retry policy
            //Taking into account that the WorkManager uses the run attempt count as reference,
            // for a BackoffPolicy of 1 minute, will be as next:
            //For linear: work start time + (1 * run attempt count)
            //For exponential: work start time + Math.scalb(1, run attempt count - 1)
            //The work start time, is when the work was first executed (the 1st run attempt).
            //Run attempt count is how many times the WorkManager has tried to execute an specific Work.
            //Also note that the maximum delay will be capped at WorkRequest.MAX_BACKOFF_MILLIS.
            //Take into consideration that a retry will only happen if you specify that the Work requires it
            // by returning WorkerResult.RETRY
            val workRequest: PeriodicWorkRequest =
                PeriodicWorkRequestBuilder<LocationCoroutineWorker>(1, TimeUnit.HOURS)
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
                    .setConstraints(Constraints.Builder()
                        .build())
                    .build()

            //Enqueue work
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
                    workRequest)
        }
    }
}
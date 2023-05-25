package com.example.bglocations.di

import android.content.Context
import android.location.LocationManager
import com.example.bglocations.location.GooglePlayServicesLocationProvider
import com.example.bglocations.location.LocationManagerLocationProvider
import com.example.bglocations.location.LocationProvider
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.util.DeviceProperties
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

private const val LOCATION_UPDATE_TIMEOUT_MILLIS = 10 * 1000L

@Retention(AnnotationRetention.RUNTIME)
@Qualifier
annotation class HighAccuracyLocationRequest

@Retention(AnnotationRetention.RUNTIME)
@Qualifier
annotation class BalancedPowerAccuracyLocationRequest

@Module
@InstallIn(SingletonComponent::class)
object LocationProviderModule {

    @Singleton
    @Provides
    @BalancedPowerAccuracyLocationRequest
    fun provideBalancedPowerAccuracyLocationRequest(): LocationRequest = LocationRequest.create()
        .setPriority(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
        .setInterval(GooglePlayServicesLocationProvider.WEAR_INTERVAL_LOCATION_REQUEST)
        .setSmallestDisplacement(
            GooglePlayServicesLocationProvider.WEAR_SMALLEST_DISPLACEMENT_LOCATION_REQUEST)
        .setFastestInterval(
            GooglePlayServicesLocationProvider.WEAR_FASTEST_INTERVAL_LOCATION_REQUEST)


    @Singleton
    @Provides
    @HighAccuracyLocationRequest
    fun provideHighAccuracyLocationRequest(): LocationRequest = LocationRequest.create()
        .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
        .setInterval(GooglePlayServicesLocationProvider.INTERVAL_LOCATION_REQUEST)
        .setFastestInterval(GooglePlayServicesLocationProvider.FASTEST_INTERVAL_LOCATION_REQUEST)
        .setSmallestDisplacement(
            GooglePlayServicesLocationProvider.SMALLEST_DISPLACEMENT_LOCATION_REQUEST)

    @Singleton
    @Provides
    fun provideGooglePlayServicesLocationProvider(
        @ApplicationContext context: Context,
        fusedLocationProviderClient: FusedLocationProviderClient,
        @HighAccuracyLocationRequest highAccuracyLocationRequest: LocationRequest,
        @BalancedPowerAccuracyLocationRequest balancedPowerAccuracyLocationRequest: LocationRequest): GooglePlayServicesLocationProvider {
        val locationRequest = if (DeviceProperties.isWearable(context)) {
            balancedPowerAccuracyLocationRequest
        } else highAccuracyLocationRequest
        return GooglePlayServicesLocationProvider(fusedLocationProviderClient, locationRequest,
            LOCATION_UPDATE_TIMEOUT_MILLIS)
    }

    @Singleton
    @Provides
    fun provideFusedLocationProviderClient(
        @ApplicationContext context: Context): FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    @Singleton
    @Provides
    fun provideLocationManagerLocationProvider(
        locationManager: LocationManager): LocationManagerLocationProvider =
        LocationManagerLocationProvider(locationManager, LOCATION_UPDATE_TIMEOUT_MILLIS)

    @Singleton
    @Provides
    fun provideLocationManager(@ApplicationContext context: Context): LocationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    @Singleton
    @Provides
    fun provideLocationProvider(@ApplicationContext context: Context,
                                googlePlayServicesLocationProvider: GooglePlayServicesLocationProvider,
                                locationManagerLocationProvider: LocationManagerLocationProvider): LocationProvider {
        val playServicesAvailable = GoogleApiAvailability.getInstance()
            .isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS
        return if (playServicesAvailable) {
            googlePlayServicesLocationProvider
        } else locationManagerLocationProvider
    }
}
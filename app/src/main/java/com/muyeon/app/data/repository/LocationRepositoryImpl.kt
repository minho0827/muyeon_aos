package com.muyeon.app.data.repository

import android.app.Activity
import android.content.Context
import android.content.IntentSender
import android.os.Handler
import android.os.Looper
import com.muyeon.app.domain.models.location.LocationResult
import com.muyeon.app.domain.repositories.LocationRepository
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority
import com.google.android.gms.location.SettingsClient

class LocationRepositoryImpl(
    private val context: Context,
    private val activity: Any?
) : LocationRepository {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private var currentLocationCallback: LocationCallback? = null
    private val handler = Handler(Looper.getMainLooper())
    private var timeoutRunnable: Runnable? = null

    override fun requestLocation(callback: (LocationResult) -> Unit) {
        val activity = activity as? Activity ?: run {
            callback(LocationResult(status = "Reject", message = "Activity is null"))
            return
        }

        stopLocationUpdates()

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 5000L
        ).setMinUpdateIntervalMillis(500)
            .setMaxUpdateDelayMillis(2000)
            .setMinUpdateDistanceMeters(5f)
            .build()

        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val client: SettingsClient = LocationServices.getSettingsClient(context)

        client.checkLocationSettings(builder.build())
            .addOnSuccessListener { onLocationSettingsSuccess(locationRequest, callback) }
            .addOnFailureListener { onLocationSettingsFailure(it, activity, callback) }
    }

    var timeLastLocation = 0
    private fun onLocationSettingsSuccess(
        locationRequest: LocationRequest,
        callback: (LocationResult) -> Unit
    ) {
        try {
            tryGetLastLocation(locationRequest, callback)
        } catch (e: Exception) {
            cancelTimeout() // Cancel timeout on exception
            callback(
                LocationResult(
                    latitude = null,
                    longitude = null,
                    status = "Reject",
                    message = e.localizedMessage ?: "Unknown error"
                )
            )
        }
    }

    private fun onLocationSettingsFailure(
        exception: Exception,
        activity: Activity,
        callback: (LocationResult) -> Unit
    ) {
        cancelTimeout() // Cancel timeout on failure
        if (exception is ResolvableApiException) {
            try {
                exception.startResolutionForResult(activity, 0)
                callback(
                    LocationResult(
                        status = "Reject",
                        message = "Location services are disabled. Please enable to continue."
                    )
                )
            } catch (sendIntentException: IntentSender.SendIntentException) {
                callback(
                    LocationResult(
                        status = "Reject",
                        message = "Cannot display enable location services dialog"
                    )
                )
            }
        } else {
            callback(
                LocationResult(
                    latitude = null,
                    longitude = null,
                    status = "Reject",
                    message = exception.localizedMessage ?: "Unknown installation error"
                )
            )
        }
    }

    private fun tryGetLastLocation(
        locationRequest: LocationRequest,
        callback: (LocationResult) -> Unit
    ) {
        try {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    if (location != null && (timeLastLocation == 0 || System.currentTimeMillis().toInt() - timeLastLocation < 4000)) {
                        timeLastLocation = System.currentTimeMillis().toInt()
                        cancelTimeout() // Cancel timeout on successful location
                        stopLocationUpdates()
                        callback(
                            LocationResult(
                                latitude = location.latitude,
                                longitude = location.longitude,
                                status = "Forever",
                                message = "GPS information has been updated"
                            )
                        )
                    } else {
                        val locationCallback = object : LocationCallback() {
                            override fun onLocationResult(locationResult: com.google.android.gms.location.LocationResult) {
                                val mostRecentLocation = locationResult.locations.maxByOrNull { it.time }
                                if (mostRecentLocation != null) {
                                    timeLastLocation = System.currentTimeMillis().toInt()
                                    cancelTimeout() // Cancel timeout on fresh location
                                    stopLocationUpdates()
                                    callback(
                                        LocationResult(
                                            latitude = mostRecentLocation.latitude,
                                            longitude = mostRecentLocation.longitude,
                                            status = "Forever",
                                            message = "GPS information has been updated"
                                        )
                                    )
                                }
                            }
                        }
                        currentLocationCallback = locationCallback
                        fusedLocationClient.requestLocationUpdates(
                            locationRequest,
                            locationCallback,
                            Looper.getMainLooper()
                        )

                        // Set up timeout
                        timeoutRunnable = Runnable {
                            cancelTimeout() // Ensure timeout is canceled
                            callback(
                                LocationResult(
                                    latitude = null,
                                    longitude = null,
                                    status = "Reject",
                                    message = "Unable to get location. Please try again."
                                )
                            )
                            stopLocationUpdates()
                            tryGetLastLocation(locationRequest, callback) // Retry
                        }
                        handler.postDelayed(timeoutRunnable!!, com.muyeon.app.utils.Constants.LOCATION_TIMEOUT)
                    }
                }
                .addOnFailureListener { exception ->
                    cancelTimeout() // Cancel timeout on failure
                    callback(
                        LocationResult(
                            latitude = null,
                            longitude = null,
                            status = "Reject",
                            message = "Error getting last position: ${exception.localizedMessage}"
                        )
                    )
                }
        } catch (e: SecurityException) {
            cancelTimeout() // Cancel timeout on security exception
            callback(
                LocationResult(
                    latitude = null,
                    longitude = null,
                    status = "Reject",
                    message = "Location access denied: ${e.localizedMessage}"
                )
            )
        }
    }

    private fun cancelTimeout() {
        timeoutRunnable?.let {
            handler.removeCallbacks(it)
            timeoutRunnable = null
        }
    }

    fun stopLocationUpdates() {
        currentLocationCallback?.let { callback ->
            fusedLocationClient.removeLocationUpdates(callback)
            currentLocationCallback = null
        }
    }
}
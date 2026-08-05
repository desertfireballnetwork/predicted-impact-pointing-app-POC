package com.dfn.fireballpoc.sensors

import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationListener
import android.location.LocationManager
import com.dfn.fireballpoc.model.ObserverState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Wraps [LocationManager] to expose the observer's position as a [StateFlow]. */
class LocationTracker(context: Context) {
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private val _observerState = MutableStateFlow<ObserverState?>(null)
    val observerState: StateFlow<ObserverState?> = _observerState.asStateFlow()

    private val listener = LocationListener { location ->
        _observerState.value = ObserverState(location.latitude, location.longitude, location.altitude)
    }

    @SuppressLint("MissingPermission")
    fun start() {
        val provider = when {
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            else -> return
        }

        locationManager.getLastKnownLocation(provider)?.let { location ->
            _observerState.value = ObserverState(location.latitude, location.longitude, location.altitude)
        }

        locationManager.requestLocationUpdates(provider, 1000L, 0f, listener)
    }

    fun stop() {
        locationManager.removeUpdates(listener)
    }
}

package com.davidferrand.confinemap.model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.maps.model.LatLng

/**
 * Keep stuff across restarts of the activity but not the process
 * @see PersistentDataRepository
 */
object VolatileDataRepository {
    fun factoryReset() {
        myLocationInternal.postValue(null)
        locationRequestLevel.postValue(LocationRequestLevel.NONE)
        baladeStatus = BaladeStatus.AtHome
    }

    fun updateMyLocation(location: LatLng) {
        // Update the real location
        myLocationInternal.postValue(location)
    }

    private val myLocationInternal = MutableLiveData<LatLng?>()

    val myLocation: LiveData<LatLng?> get() = myLocationInternal

    val locationRequestLevel = MutableLiveData<LocationRequestLevel>()

    enum class LocationRequestLevel {
        NONE, FOREGROUND_ONLY, FOREGROUND_BACKGROUND
    }

    var baladeStatus: BaladeStatus = BaladeStatus.AtHome

    sealed class BaladeStatus {
        object AtHome : BaladeStatus()
        class Balading(val startTime: Long) : BaladeStatus()
    }

    /**
     * Contains parameters used by [com.google.android.gms.location.FusedLocationProviderApi].
     */
    val locationRequest: LocationRequest = LocationRequest.create().apply {
        interval = 10_000
        fastestInterval = 5_000
        priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }

}
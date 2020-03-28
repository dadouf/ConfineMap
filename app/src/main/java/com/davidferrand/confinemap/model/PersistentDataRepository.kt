package com.davidferrand.confinemap.model

import android.content.Context
import androidx.preference.PreferenceManager
import com.google.android.gms.maps.model.LatLng

/**
 * Keep stuff across restarts of the activity
 */
class PersistentDataRepository(private val context: Context) {

    fun factoryReset() {
        PreferenceManager.getDefaultSharedPreferences(context)
            .edit()
            .clear()
            .apply()
    }

    var savedHomeLocation: LatLng? by LatLngPref(
        PreferenceManager.getDefaultSharedPreferences(
            context
        ), "savedHomeLocation"
    )

    var userHasSeenDemo: Boolean by BooleanPref(
        PreferenceManager.getDefaultSharedPreferences(
            context
        ), "userHasSeenDemo"
    )

    var userHasSeenPermissionOnboarding by BooleanPref(
        PreferenceManager.getDefaultSharedPreferences(
            context
        ), "userHasSeenPermissionOnboarding"
    )
}
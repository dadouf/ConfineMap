package com.davidferrand.confinemap.model

import android.content.SharedPreferences
import com.google.android.gms.maps.model.LatLng
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class BooleanPref(private val prefs: SharedPreferences, private val key: String) :
    ReadWriteProperty<Any, Boolean> {

    override fun getValue(thisRef: Any, property: KProperty<*>) =
        prefs.getBoolean(key, false)

    override fun setValue(thisRef: Any, property: KProperty<*>, value: Boolean) {
        prefs.edit()
            .putBoolean(key, value)
            .apply()
    }
}


class LatLngPref(private val prefs: SharedPreferences, key: String) :
    ReadWriteProperty<Any, LatLng?> {

    private val latKey = "${key}_latitude"
    private val lngKey = "${key}_longitude"

    override fun getValue(thisRef: Any, property: KProperty<*>): LatLng? {
        if (!prefs.contains(latKey) || !prefs.contains(lngKey)) {
            return null
        }

        val lat = prefs.getFloat(latKey,0f)
        val lng = prefs.getFloat(lngKey,0f)

        return LatLng(lat.toDouble(), lng.toDouble())
    }

    override fun setValue(thisRef: Any, property: KProperty<*>, value: LatLng?) {
        if (value == null) {
            prefs.edit()
                .remove(latKey)
                .remove(lngKey)
                .apply()
        } else {
            prefs.edit()
                .putFloat(latKey, value.latitude.toFloat())
                .putFloat(lngKey, value.longitude.toFloat())
                .apply()
        }
    }

}

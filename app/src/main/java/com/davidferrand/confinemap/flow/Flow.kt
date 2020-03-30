package com.davidferrand.confinemap.flow

import android.content.Intent
import android.util.Log
import android.view.View
import androidx.annotation.CallSuper
import com.davidferrand.confinemap.MapsActivity
import com.davidferrand.confinemap.logTag
import com.davidferrand.confinemap.withLatitudeOffset
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import kotlinx.android.synthetic.main.activity_maps.*

/**
 * Contract:
 * - The app goes through the same succession of flows on init. Flows should know how to quick-complete.
 */
abstract class Flow(protected val activity: MapsActivity) {
    /** Latitude offset from the real camera target */
    protected open val viewportLatitudeOffset: Double = 0.0

    abstract val cameraSettings: CameraSettings

    protected val perceivedCameraLocation: LatLng
        get() {
            val target = activity.map.cameraPosition.target
            return LatLng(
                target.latitude + viewportLatitudeOffset,
                target.longitude
            )
        }

    fun animateCameraToPerceivedLocation(location: LatLng) {
        activity.map.animateCamera(
            CameraUpdateFactory.newLatLngZoom(
                location.withLatitudeOffset(-viewportLatitudeOffset),
                cameraSettings.zoom
            )
        )
    }

    fun start() {
        if (onQuickStart()) {
            Log.v(logTag, "flow.onQuickStart() returned true - skipping onStart()")
            return
        }

        onStart()
    }

    fun resume() = onResume()
    fun pause() = onPause()
    fun stop() = onStop()

    protected open fun onQuickStart(): Boolean {
        return false
    }

    @CallSuper
    protected open fun onStart() {
        Log.v(logTag, "flow.onStart()")

        // Hide all UI by default. Subclasses will decide what they need
        activity.shouldShowMyLocationButton = false
        activity.btn_center_home.visibility = View.GONE
        activity.btn_reset_home.visibility = View.GONE
        activity.btn_start.visibility = View.GONE
        activity.btn_settings.visibility = View.GONE

        cameraSettings.let {
            val location = when (it.target) {
                CameraTarget.Home -> activity.homeZone.center
                CameraTarget.MyLocation -> activity.myLocation
                is CameraTarget.Custom -> it.target.location
            }

            if (location != null) {
                val cameraUpdate = CameraUpdateFactory.newLatLngZoom(location, it.zoom)
                if (it.animate) {
                    activity.map.animateCamera(cameraUpdate)
                } else {
                    activity.map.moveCamera(cameraUpdate)
                }
            }
        }
    }

    /**
     * This is called only when the Activity's onStart() is called again.
     * It doesn't get called when the flow starts.
     */
    @CallSuper
    protected open fun onResume() {
        Log.v(logTag, "flow.onResume()")
    }

    @CallSuper
    protected open fun onPause() {
        Log.v(logTag, "flow.onPause()")
    }

    @CallSuper
    protected open fun onStop() {
        Log.v(logTag, "flow.onStop()")
    }

    open fun onLocationUpdate(location: LatLng) {}

    open fun onRequestPermissionsResult(reqCode: Int, perms: Array<String>, results: IntArray) {}

    open fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {}
}

const val defaultOnboardingLatitudeOffset = 0.0062
const val defaultLatitudeOffset = 0.0020

package com.davidferrand.confinemap.flow

import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.TextView
import com.davidferrand.confinemap.MapsActivity
import com.davidferrand.confinemap.R
import com.davidferrand.confinemap.distanceTo
import com.davidferrand.confinemap.tintWithColorRes
import com.google.android.gms.maps.model.LatLng
import kotlinx.android.synthetic.main.activity_maps.*

class DefineHomeFlow(activity: MapsActivity) : Flow(activity) {
    override val viewportLatitudeOffset = 0.0

    override val cameraSettings = CameraSettings(
        target = CameraTarget.MyLocation,
        zoom = 16f,
        animate = true
    )

    private var button: Button? = null
    private var warning: TextView? = null

    private var warned = false

    override fun onStart() {
        super.onStart()
        activity.persistentData.savedHomeLocation?.let { homeLocation ->
            // We have a saved home, load it and return without showing any UI
            activity.homeZone.center = homeLocation
            activity.onChooseHomeFlowFinished()
            return@onStart
        }

        activity.shouldShowMyLocationButton = true

        LayoutInflater.from(activity)
            .inflate(R.layout.onboarding_home, activity.frame_onboarding, true)

        button = activity.findViewById(R.id.onboarding_home_button)
        warning = activity.findViewById(R.id.onboarding_home_warning)

        activity.unlockMap()

        activity.homeZone.isVisible = false
        activity.define_home_marker.visibility = View.VISIBLE
        activity.define_home_target.visibility = View.VISIBLE

        activity.map.setOnCameraIdleListener {
            maybeClearWarning()
        }

        button?.setOnClickListener { validateAndFinish() }
    }

    private fun validateAndFinish() {
        val myLocation = activity.myLocation
        // FIXME this seems broken
        if (myLocation != null && myLocation.distanceTo(activity.map.cameraPosition.target) > 1_000 && !warned) {
            warning?.visibility = View.VISIBLE
            button?.apply {
                text = "J'en suis sûr"
                tintWithColorRes(R.color.warning)
            }

            warned = true
            return
        }

        val location = activity.map.cameraPosition.target

        activity.homeZone.center = location
        activity.persistentData.savedHomeLocation = location
        activity.onChooseHomeFlowFinished()
    }

    private fun maybeClearWarning() {
        if (warned) {
            val myLocation = activity.myLocation
            if (myLocation != null && myLocation.distanceTo(activity.map.cameraPosition.target) < 1_000) {
                warning?.visibility = View.GONE

                button?.apply {
                    text = "J'habite ici"
                    tintWithColorRes(R.color.okay)
                }

                warned = false
            }
        }
    }

    override fun onLocationUpdate(location: LatLng) {
        super.onLocationUpdate(location)
        maybeClearWarning()
    }

    override fun onStop() {
        activity.frame_onboarding.removeAllViews()
        button = null
        warning = null

        activity.homeZone.isVisible = true
        activity.define_home_marker.visibility = View.GONE
        activity.define_home_target.visibility = View.GONE

        warned = false

        activity.map.setOnCameraIdleListener(null)

        super.onStop()
    }
}
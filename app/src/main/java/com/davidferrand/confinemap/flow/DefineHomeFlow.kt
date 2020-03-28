package com.davidferrand.confinemap.flow

import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.TextView
import com.davidferrand.confinemap.HomeZone.Companion.defaultZoom
import com.davidferrand.confinemap.MapsActivity
import com.davidferrand.confinemap.R
import com.davidferrand.confinemap.tintWithColorRes
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import kotlinx.android.synthetic.main.activity_maps.*

class DefineHomeFlow(activity: MapsActivity) : Flow(activity) {
    override val viewportLatitudeOffset = defaultOnboardingLatitudeOffset

    private var button: Button? = null
    private var warning: TextView? = null

    private var warned = false

    override fun onStart() {
        super.onStart()
        activity.persistentData.savedHomeLocation?.let { homeLocation ->
            // We have a saved home, load it and return without showing any UI
            activity.homeZone.center = homeLocation
            activity.map.moveCamera(CameraUpdateFactory.newLatLngZoom(homeLocation, defaultZoom))
            activity.onChooseHomeFlowFinished()
            return@onStart
        }

        activity.shouldShowMyLocationButton = true

        LayoutInflater.from(activity)
            .inflate(R.layout.onboarding_home, activity.frame_onboarding, true)

        button = activity.findViewById(R.id.onboarding_home_button)
        warning = activity.findViewById(R.id.onboarding_home_warning)

        activity.unlockMap()

        activity.homeZone.pending = true
        activity.homeZone.center = perceivedCameraLocation

        activity.map.setOnCameraMoveListener {
            activity.homeZone.center = perceivedCameraLocation
            maybeClearWarning()
        }

        button?.setOnClickListener { validateAndFinish() }
    }

    private fun validateAndFinish() {
        val myLocation = activity.myLocation
        if (myLocation != null && myLocation !in activity.homeZone && !warned) {
            warning?.visibility = View.VISIBLE
            button?.apply {
                text = "J'en suis sûr"
                tintWithColorRes(R.color.warning)
            }

            warned = true
            return
        }

        activity.persistentData.savedHomeLocation = activity.homeZone.center
        activity.onChooseHomeFlowFinished()
    }

    private fun maybeClearWarning() {
        if (warned) {
            val myLocation = activity.myLocation
            if (myLocation != null && myLocation in activity.homeZone) {
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

        activity.homeZone.pending = false
        warned = false

        activity.map.setOnCameraMoveListener(null)

        super.onStop()
    }
}
package com.davidferrand.confinemap.flow

import android.view.View
import com.davidferrand.confinemap.MapsActivity
import com.davidferrand.confinemap.R
import com.davidferrand.confinemap.tintWithColorRes
import com.google.android.gms.maps.model.LatLng
import kotlinx.android.synthetic.main.activity_maps.*

class BaladeSetupFlow(activity: MapsActivity) : Flow(activity) {
    override val viewportLatitudeOffset: Double = defaultLatitudeOffset

    override fun onStart() {
        super.onStart()

        activity.btn_settings.visibility = View.VISIBLE
        activity.btn_center_home.visibility = View.VISIBLE
        activity.shouldShowMyLocationButton = true

        activity.btn_start.visibility = View.VISIBLE
        activity.btn_start.apply {
            text = "Démarrer la balade"
            visibility = View.VISIBLE
            tintWithColorRes(R.color.okay)

            setOnClickListener { activity.onBaladeSetupFlowFinished() }
        }

        activity.myLocation?.let { updateButtonResetHome(it) }

        activity.unlockMap()

        activity.homeZone.locked = false

        animateCameraToPerceivedLocation(activity.homeZone.center)
    }


    override fun onLocationUpdate(location: LatLng) {
        updateButtonResetHome(location)
    }

    private fun updateButtonResetHome(location: LatLng) {
        activity.btn_reset_home.visibility =
            if (location !in activity.homeZone) {
                View.VISIBLE
            } else {
                View.GONE
            }
    }
}
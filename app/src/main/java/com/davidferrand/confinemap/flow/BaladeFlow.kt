package com.davidferrand.confinemap.flow

import android.view.View
import com.davidferrand.confinemap.MapsActivity
import com.davidferrand.confinemap.R
import com.davidferrand.confinemap.model.VolatileDataRepository
import com.davidferrand.confinemap.tintWithColorRes
import kotlinx.android.synthetic.main.activity_maps.*

class BaladeFlow(activity: MapsActivity) : Flow(activity) {
    override val viewportLatitudeOffset: Double = defaultLatitudeOffset

    override val cameraSettings = CameraSettings(
        target = CameraTarget.MyLocation,
        zoom = 15f,
        animate = true
    )

    override fun onStart() {
        super.onStart()

        VolatileDataRepository.locationRequestLevel.postValue(VolatileDataRepository.LocationRequestLevel.FOREGROUND_BACKGROUND)

        if (VolatileDataRepository.baladeStatus !is VolatileDataRepository.BaladeStatus.Balading) {
            VolatileDataRepository.baladeStatus =
                VolatileDataRepository.BaladeStatus.Balading(System.currentTimeMillis())
        }

        activity.btn_center_home.visibility = View.VISIBLE
        activity.shouldShowMyLocationButton = true

        activity.btn_start.visibility = View.VISIBLE
        activity.btn_start.apply {
            text = "Stop"
            visibility = View.VISIBLE
            tintWithColorRes(R.color.not_okay)

            setOnClickListener { activity.onBaladeFlowFinished() }
        }

        activity.unlockMap()
        activity.homeZone.locked = true
    }

    override fun onResume() {
        super.onResume()

        if (VolatileDataRepository.baladeStatus !is VolatileDataRepository.BaladeStatus.Balading) {
            // Came back to the Activity but user has stopped the balade
            activity.onBaladeFlowFinished()
        }
    }

    override fun onStop() {
        VolatileDataRepository.locationRequestLevel.postValue(VolatileDataRepository.LocationRequestLevel.FOREGROUND_ONLY)

        VolatileDataRepository.baladeStatus = VolatileDataRepository.BaladeStatus.AtHome

        super.onStop()
    }
}
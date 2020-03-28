package com.davidferrand.confinemap.flow

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Button
import com.davidferrand.confinemap.*
import com.davidferrand.confinemap.HomeZone.Companion.defaultZoom
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.android.synthetic.main.activity_maps.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch

class DemoFlow(activity: MapsActivity) : Flow(activity), CoroutineScope by MainScope() {
    override val viewportLatitudeOffset = defaultOnboardingLatitudeOffset

    private var button: Button? = null

    private var joggerMarker: Marker? = null

    private val initialCameraPosition = LatLng(47.2115571267272, -1.576287969946861) // Procé

    /**
     * Set the camera position before calling this.
     * [onStart] will take care of resetting the [HomeZone].
     */
    override fun onStart() {
        super.onStart()

        activity.map.moveCamera(
            CameraUpdateFactory.newLatLngZoom(
                initialCameraPosition,
                defaultZoom
            )
        )

        if (activity.persistentData.userHasSeenDemo) {
            // Quick return: do not display anything
            activity.onDemoFlowFinished()
            return
        }

        LayoutInflater.from(activity)
            .inflate(R.layout.onboarding_demo, activity.frame_onboarding, true)

        button = activity.findViewById(R.id.onboarding_demo_button)
        button?.setOnClickListener {
            activity.persistentData.userHasSeenDemo = true
            activity.onDemoFlowFinished()
        }

        activity.lockMap()

        // Offset a little compared to the real camera position to take views into account
        val homeLocation = perceivedCameraLocation
        activity.homeZone.center = homeLocation
        activity.homeZone.locked = true

        val joggerLocations = listOf(
            homeLocation,
            LatLng(homeLocation.latitude + 0.0030, homeLocation.longitude),
            LatLng(homeLocation.latitude + 0.0045, homeLocation.longitude),
            LatLng(homeLocation.latitude + 0.0060, homeLocation.longitude),
            LatLng(homeLocation.latitude + 0.0065, homeLocation.longitude - 0.0025),
            LatLng(homeLocation.latitude + 0.0070, homeLocation.longitude - 0.0050),
            // Going out
            LatLng(homeLocation.latitude + 0.0075, homeLocation.longitude - 0.0075),
            LatLng(homeLocation.latitude + 0.0080, homeLocation.longitude - 0.0100),
            LatLng(homeLocation.latitude + 0.0075, homeLocation.longitude - 0.0125),
            LatLng(homeLocation.latitude + 0.0062, homeLocation.longitude - 0.0125),
            LatLng(homeLocation.latitude + 0.0050, homeLocation.longitude - 0.0110),
            // Going back in
            LatLng(homeLocation.latitude + 0.0040, homeLocation.longitude - 0.0090),
            LatLng(homeLocation.latitude + 0.0030, homeLocation.longitude - 0.0070),
            LatLng(homeLocation.latitude + 0.0020, homeLocation.longitude - 0.0050),
            LatLng(homeLocation.latitude + 0.0010, homeLocation.longitude - 0.0030)
        )

        joggerMarker = activity.map.addMarker(
            MarkerOptions()
                .icon(
                    BitmapDescriptorFactory.fromBitmap(
                        getBitmapFromVectorDrawable(
                            activity,
                            R.drawable.marker_jogger
                        )
                    )
                )
                .position(homeLocation) // start at home
        )

        launch {
            joggerMarker?.animate(joggerLocations) { i, location ->
                Log.v(this@DemoFlow.logTag, "Jogger at i=$i: $location")
                joggerMarker?.isVisible = location != homeLocation

                activity.homeZone.trackedMemberLocation = location

                if (location in activity.homeZone) {
                    activity.onboarding_alert.visibility = View.GONE
                    activity.onboarding_alert.clearAnimation()
                } else {
                    activity.onboarding_alert.visibility = View.VISIBLE
                    activity.onboarding_alert.startAnimation(
                        AnimationUtils.loadAnimation(
                            activity,
                            R.anim.shake
                        )
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        onStart()
    }

    override fun onPause() {
        super.onPause()
        onStop()
    }

    override fun onStop() {
        activity.frame_onboarding.removeAllViews()
        button = null

        joggerMarker?.let {
            it.remove()
            joggerMarker = null
        }

        coroutineContext.cancelChildren()

        activity.onboarding_alert.clearAnimation()
        activity.onboarding_alert.visibility = View.GONE

        activity.homeZone.locked = false
        activity.homeZone.trackedMemberLocation = null

        super.onStop()
    }
}
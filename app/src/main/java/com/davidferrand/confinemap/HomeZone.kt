package com.davidferrand.confinemap

import android.content.Context
import android.graphics.Color
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.*

class HomeZone(context: Context, map: GoogleMap) {

    private val circle: Circle
    private val marker: Marker
//    private val debugMarker: Circle

    private val unlockedPattern = listOf(Dot(), Gap(20f))

    private val defaultStrokeWidth = 7f

    var center: LatLng
        get() = marker.position
        set(value) {
            circle.center = value
            marker.position = value
//            debugMarker.center = value
            updateCircleColor()
        }

    /**
     * Updates the zone UI depending on the [trackedMemberLocation]
     */
    var trackedMemberLocation: LatLng? = null
        set(value) {
            field = value
            updateCircleColor()
        }

    var locked: Boolean = false
        set(value) {
            field = value
            circle.strokePattern = if (value) null else unlockedPattern
        }

    var pending: Boolean = false
        set(value) {
            field = value
            if (value) {
                circle.strokeWidth = 0f
            } else {
                circle.strokeWidth = defaultStrokeWidth
            }
        }

    var isVisible: Boolean
        get() = marker.isVisible
        set(value) {
            circle.isVisible = value
            marker.isVisible = value
        }


    init {
        // We need some initial position for the markers but the value doesn't matter:
        // it should get updated right after
        val initialPosition = map.cameraPosition.target

        circle = map.addCircle(
            CircleOptions()
                .center(initialPosition)
                .radius(1000.0)
                .strokeWidth(defaultStrokeWidth)
                .strokeColor(Color.parseColor("#FF00A040"))
                .fillColor(Color.parseColor("#2000A040"))
                .strokePattern(unlockedPattern)
        )

        marker = map.addMarker(
            MarkerOptions()
                .position(initialPosition)
                .icon(
                    BitmapDescriptorFactory.fromBitmap(
                        getBitmapFromVectorDrawable(
                            context,
                            R.drawable.home_marker
                        )
                    )
                )
        )

//        debugMarker = map.addCircle(
//            CircleOptions()
//                .center(initialPosition)
//                .radius(1.0)
//        )
    }

    private fun updateCircleColor() {
        // TODO group in resources

        val latestLocation = trackedMemberLocation

        val baseColor = when {
            latestLocation == null -> "0040A0"
            latestLocation in this -> "00A040"
            else -> "A00040"
        }
        circle.strokeColor = Color.parseColor("#FF$baseColor")
        circle.fillColor = Color.parseColor("#20$baseColor")
    }


    operator fun contains(location: LatLng) = location in circle

    companion object {
        //TODO expecting this to change for a given radius
        const val defaultZoom = 14f
    }
}

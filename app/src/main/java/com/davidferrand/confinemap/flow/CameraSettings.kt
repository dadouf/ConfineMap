package com.davidferrand.confinemap.flow

import com.google.android.gms.maps.model.LatLng

data class CameraSettings(
    val target: CameraTarget,
    val zoom: Float,
    val animate: Boolean
)

sealed class CameraTarget {
    object Home : CameraTarget()
    object MyLocation : CameraTarget()
    class Custom(val location: LatLng) : CameraTarget()
}

val locationParcDeProce = LatLng(47.2115571267272, -1.576287969946861)
val initialCameraSettings = CameraSettings(
    target = CameraTarget.Custom(locationParcDeProce),
    zoom = 14f,
    animate = false
)
package com.davidferrand.confinemap

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Canvas
import android.location.Location
import android.os.Build
import android.view.View
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.ViewCompat
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import kotlinx.coroutines.delay


fun LatLng.distanceTo(other: LatLng): Float {
    val results = FloatArray(1)
    Location.distanceBetween(
        this.latitude, this.longitude,
        other.latitude, other.longitude, results
    )

    return results[0]
}


fun LatLng.withLatitudeOffset(offset: Double): LatLng = LatLng(latitude + offset, longitude)

operator fun Circle.contains(element: LatLng) = center.distanceTo(element) < radius

fun getBitmapFromVectorDrawable(context: Context, drawableId: Int): Bitmap {
    var drawable = ContextCompat.getDrawable(context, drawableId)!!
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
        drawable = DrawableCompat.wrap(drawable).mutate()
    }

    val bitmap = Bitmap.createBitmap(
        drawable.intrinsicWidth,
        drawable.intrinsicHeight, Bitmap.Config.ARGB_8888
    )

    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)

    return bitmap
}

suspend fun Marker.animate(
    positions: List<LatLng>,
    doAfterMove: (Int, LatLng) -> Unit = { _, _ -> }
) {
    var i = 0
    while (true) {
        val newPosition = positions[i]

        position = newPosition

        doAfterMove(i, newPosition)

        delay(1_000)
        i = (i + 1) % positions.size
    }
}

fun View.tintWithColorRes(@ColorRes okay: Int) {
    ViewCompat.setBackgroundTintList(
        this, ColorStateList.valueOf(
            ResourcesCompat.getColor(
                context.resources,
                okay,
                context.theme
            )
        )
    )
}

fun Location.toLatLng() = LatLng(latitude, longitude)

val Any.logTag get() = "CM--" + this.javaClass.simpleName
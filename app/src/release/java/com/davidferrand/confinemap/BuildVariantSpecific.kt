package com.davidferrand.confinemap

import android.view.View
import kotlinx.android.synthetic.main.activity_maps.*

object BuildVariantSpecific {
    fun onCreateActivity(mapsActivity: MapsActivity) {
        mapsActivity.btn_debug.apply {
            visibility = View.GONE
        }
    }
}
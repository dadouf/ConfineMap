package com.davidferrand.confinemap

import android.view.View
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_maps.*

object BuildVariantSpecific {

    fun onCreateActivity(mapsActivity: MapsActivity) {
        mapsActivity.btn_debug.apply {
            visibility = View.VISIBLE
            setOnClickListener {
                val willBeMocking = mapsActivity.locationUpdatesService?.toggleMockingLocation()
                Toast.makeText(mapsActivity, "Mocking location: $willBeMocking", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }
}
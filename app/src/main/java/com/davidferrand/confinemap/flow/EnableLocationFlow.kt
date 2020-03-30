package com.davidferrand.confinemap.flow

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.davidferrand.confinemap.*
import com.davidferrand.confinemap.model.VolatileDataRepository
import com.davidferrand.confinemap.model.VolatileDataRepository.locationRequest
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.android.synthetic.main.activity_maps.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch


@Suppress("ConstantConditionIf")
class EnableLocationFlow(activity: MapsActivity) : Flow(activity), CoroutineScope by MainScope() {
    override val viewportLatitudeOffset = defaultOnboardingLatitudeOffset

    override val cameraSettings = CameraSettings(
        target = CameraTarget.MyLocation, // Likely not ready yet but will be loaded asap
        zoom = 14f,
        animate = false
    )

    private val checkLocationSettingsTask = LocationServices.getSettingsClient(activity)
        .checkLocationSettings(
            LocationSettingsRequest.Builder().addLocationRequest(locationRequest).build()
        )

    private var button: Button? = null
    private var explanationText: TextView? = null
    private var warningText: TextView? = null
    private var successText: TextView? = null
    private var progress: ProgressBar? = null

    private var unknownPositionMarker: Marker? = null

    private val userHasGrantedPermission: Boolean
        get() = ContextCompat.checkSelfPermission(activity, PERMISSION) == PERMISSION_GRANTED

    private var permissionRequestTime: Long? = null

    private lateinit var resolutionGraph: MutableList<ResolutionStep>

    override fun onStart() {
        super.onStart()

        if (!checkLocationSettingsTask.isComplete) {
            // Not ready to build graph yet, wait for it
            checkLocationSettingsTask.addOnCompleteListener { onStart() }
            return
        }

        resolutionGraph = buildResolutionGraph()
        Log.d(this@EnableLocationFlow.logTag, "Resolution graph: $resolutionGraph")
        onFlowProgress()
    }

    private fun buildResolutionGraph(): MutableList<ResolutionStep> {
        val list = mutableListOf<ResolutionStep>()

        // Core conditions we need to meet
        if (!userHasGrantedPermission) list.add(ResolutionStep.REQUEST_PERMISSION)
        if (!checkLocationSettingsTask.isSuccessful) list.add(ResolutionStep.FIX_LOCATION_SETTINGS)

        if (list.isNotEmpty() || !activity.persistentData.userHasSeenPermissionOnboarding) {
            // Verbose flow: we need to explain to the user and let them interact
            // Start with an onboarding
            list.add(0, ResolutionStep.SHOW_ONBOARDING_AND_WAIT)

            list.add(ResolutionStep.KICK_OFF_LOCATION_MANAGER)

            // End with an acknowledgement
            list.add(ResolutionStep.SHOW_SUCCESS_AND_WAIT)

        } else {
            // Immediate flow: no UI is shown, no user action is needed
            list.add(ResolutionStep.KICK_OFF_LOCATION_MANAGER)
        }

        return list
    }

    enum class ResolutionStep {
        SHOW_ONBOARDING_AND_WAIT {
            override fun resolve(flow: EnableLocationFlow) {
                flow.activity.lockMap()
                flow.activity.homeZone.isVisible = false

                val initialPosition = flow.perceivedCameraLocation
                val latRate = 0.0015
                val lngRate = 0.0020
                val positions = listOf(
                    LatLng(initialPosition.latitude + latRate, initialPosition.longitude - lngRate),
                    LatLng(initialPosition.latitude + latRate, initialPosition.longitude + lngRate),
                    LatLng(initialPosition.latitude - latRate, initialPosition.longitude - lngRate),
                    LatLng(initialPosition.latitude - latRate, initialPosition.longitude + lngRate)
                )

                flow.unknownPositionMarker = flow.activity.map.addMarker(
                    MarkerOptions()
                        .icon(
                            BitmapDescriptorFactory.fromBitmap(
                                getBitmapFromVectorDrawable(
                                    flow.activity,
                                    R.drawable.ic_help_outline_black_24dp
                                )
                            )
                        )
                        .position(positions[0])
                )
                flow.launch { flow.unknownPositionMarker?.animate(positions) }

                LayoutInflater.from(flow.activity)
                    .inflate(R.layout.onboarding_permission, flow.activity.frame_onboarding, true)

                flow.button = flow.activity.findViewById(R.id.onboarding_permission_button)
                flow.explanationText = flow.activity.findViewById(R.id.onboarding_permission_body)
                flow.warningText = flow.activity.findViewById(R.id.onboarding_permission_warning)
                flow.successText = flow.activity.findViewById(R.id.onboarding_permission_success)
                flow.progress = flow.activity.findViewById(R.id.onboarding_permission_progress)

                flow.button?.apply {
                    text = "Autoriser"
                    setOnClickListener { flow.onFlowProgress() }
                }

                flow.activity.persistentData.userHasSeenPermissionOnboarding = true
                flow.resolutionGraph.remove(this)
            }
        },

        REQUEST_PERMISSION {
            override fun resolve(flow: EnableLocationFlow) {
                flow.permissionRequestTime = System.currentTimeMillis()

                ActivityCompat.requestPermissions(
                    flow.activity,
                    arrayOf(PERMISSION),
                    REQUEST_PERMISSION_LOCATION
                )

                // Don't remove from graph yet, wait for the result
            }
        },

        FIX_LOCATION_SETTINGS {
            /**
             * The obvious case is launch a dialog for resolution on error, but this step is also
             * scheduled if location settings were not initialized on time. In which case resolution
             * is just waiting for success.
             */
            override fun resolve(flow: EnableLocationFlow) {
                if (flow.checkLocationSettingsTask.isSuccessful) {
                    // We probably never get here, but just in case...
                    flow.resolutionGraph.remove(this)
                    flow.onFlowProgress()

                } else {
                    // There was an error, try to recover
                    val exception = flow.checkLocationSettingsTask.exception

                    try {
                        (exception as ResolvableApiException).startResolutionForResult(
                            flow.activity,
                            REQUEST_LOCATION_SETTINGS
                        )

                        // Don't remove from graph yet, wait for the result
                    } catch (e: Exception) {
                        // We won't recover
                        flow.onFlowImpossible()
                    }
                }
            }
        },

        KICK_OFF_LOCATION_MANAGER {
            override fun resolve(flow: EnableLocationFlow) {
                VolatileDataRepository.locationRequestLevel.postValue(VolatileDataRepository.LocationRequestLevel.FOREGROUND_ONLY)
                flow.activity.map.isMyLocationEnabled = true

                flow.resolutionGraph.remove(this)
                flow.onFlowProgress()
            }
        },

        SHOW_SUCCESS_AND_WAIT {
            override fun resolve(flow: EnableLocationFlow) {
                flow.button?.text = "Continuer"
                flow.explanationText?.visibility = View.INVISIBLE

                if (flow.activity.myLocation == null) {
                    flow.progress?.visibility = View.VISIBLE
                    // onFirstLocationReported will come
                } else {
                    flow.successText?.visibility = View.VISIBLE
                }

                flow.resolutionGraph.remove(this)
            }
        };

        /**
         * @return true if this is resolved immediately, in which case it should be removed from the graph
         */
        abstract fun resolve(flow: EnableLocationFlow)
    }

    private fun onFlowImpossible() {
        warningText?.visibility = View.VISIBLE

        button?.apply {
            text = "Continuer sans localisation"
            tintWithColorRes(R.color.warning)
            setOnClickListener { activity.onEnableLocationFlowFinished(false) }
        }
    }

    /**
     * Resolve steps one by one until the graph is empty.
     * Call this method as many times as needed during the user flow.
     */
    private fun onFlowProgress() {
        if (resolutionGraph.isEmpty()) {
            Log.d(this@EnableLocationFlow.logTag, "onFlowProgress: graph is empty")
            activity.onEnableLocationFlowFinished(true)

        } else {
            val nextStep = resolutionGraph[0]
            Log.d(
                this@EnableLocationFlow.logTag,
                "onFlowProgress: next step is $nextStep, remaining steps after that: ${resolutionGraph.size}"
            )
            nextStep.resolve(this)
        }
    }

    override fun onRequestPermissionsResult(reqCode: Int, perms: Array<String>, results: IntArray) {
        when (reqCode) {
            REQUEST_PERMISSION_LOCATION -> {
                if ((results.isNotEmpty() && results[0] == PERMISSION_GRANTED)) {
                    Log.e(
                        this@EnableLocationFlow.logTag,
                        "onRequestPermissionsResult: user granted"
                    )

                    resolutionGraph.removeAll { it == ResolutionStep.REQUEST_PERMISSION }
                    onFlowProgress()

                } else {
                    Log.e(this@EnableLocationFlow.logTag, "onRequestPermissionsResult: user denied")
                    val requestTime = permissionRequestTime
                    val thresholdMs = 100

                    if (requestTime == null || System.currentTimeMillis() - requestTime < thresholdMs) {
                        // Denied response came in less than 100ms: it probably means the user has checked "Never ask again"
                        onFlowImpossible()
                    }
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_LOCATION_SETTINGS -> when (resultCode) {
                Activity.RESULT_OK -> {
                    Log.e(this@EnableLocationFlow.logTag, "onActivityResult: user accepted")

                    resolutionGraph.removeAll { it == ResolutionStep.FIX_LOCATION_SETTINGS }
                    onFlowProgress()
                }
                Activity.RESULT_CANCELED -> {
                    Log.e(this@EnableLocationFlow.logTag, "onActivityResult: user refused")
                    // TODO maybe after multiple denials, onFlowImpossible()
                }
            }
        }
    }

    override fun onLocationUpdate(location: LatLng) {
        stopUnknownMarkerAnimation()

        progress?.visibility = View.GONE
        successText?.visibility = View.VISIBLE
    }

    override fun onStop() {
        activity.frame_onboarding.removeAllViews()
        button = null
        warningText = null
        explanationText = null
        successText = null
        progress = null

        stopUnknownMarkerAnimation()

        activity.homeZone.isVisible = true

        permissionRequestTime = null

        super.onStop()
    }

    private fun stopUnknownMarkerAnimation() {
        unknownPositionMarker?.let {
            it.remove()
            unknownPositionMarker = null
        }
        coroutineContext.cancelChildren()
    }

    @Suppress("RedundantOverride")
    override fun onResume() {
        super.onResume()
        // TODO restart animation. until we do, we've just lost the animation but the rest works: it's ok
    }

    override fun onPause() {
        stopUnknownMarkerAnimation()
        super.onPause()
    }

    companion object {
        private const val REQUEST_PERMISSION_LOCATION: Int = 29
        private const val REQUEST_LOCATION_SETTINGS: Int = 9

        const val PERMISSION = Manifest.permission.ACCESS_FINE_LOCATION

    }
}
package com.davidferrand.confinemap

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Observer
import com.davidferrand.confinemap.flow.*
import com.davidferrand.confinemap.model.PersistentDataRepository
import com.davidferrand.confinemap.model.VolatileDataRepository
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import kotlinx.android.synthetic.main.activity_maps.*
import kotlinx.android.synthetic.main.drawer.*

class MapsActivity : AppCompatActivity() {

    private lateinit var demoFlow: DemoFlow
    private lateinit var enableLocationFlow: EnableLocationFlow
    private lateinit var defineHomeFlow: DefineHomeFlow
    private lateinit var baladeSetupFlow: BaladeSetupFlow
    private lateinit var baladeFlow: BaladeFlow

    lateinit var persistentData: PersistentDataRepository
        private set

    private var currentFlow: Flow? = null
        set(value) {
            field?.stop()
            field = value
            field?.start()
        }

    lateinit var map: GoogleMap
    lateinit var homeZone: HomeZone

    var myLocation: LatLng? = null
        private set(value) {
            val myLocationWasUnknown = field == null

            field = value

            homeZone.trackedMemberLocation = value
            value?.let { myLoc ->
                currentFlow?.let { flow ->
                    if (myLocationWasUnknown && flow.cameraSettings.target == CameraTarget.MyLocation) {
                        flow.animateCameraToPerceivedLocation(myLoc)
                    }
                    flow.onLocationUpdate(myLoc)
                }
            }

            // Reset itself to ensure UI is correct
            shouldShowMyLocationButton = shouldShowMyLocationButton
        }

    var shouldShowMyLocationButton = false
        set(value) {
            field = value

            btn_center_my_location.visibility =
                if (myLocation != null && value) View.VISIBLE
                else View.GONE
        }

    var locationUpdatesService: LocationUpdatesService? = null
        private set

    // Monitors the state of the connection to the service.
    private val locationUpdatesServiceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder: LocationUpdatesService.LocalBinder =
                service as LocationUpdatesService.LocalBinder
            locationUpdatesService = binder.service
        }

        override fun onServiceDisconnected(name: ComponentName) {
            locationUpdatesService = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        val mapFragment =
            supportFragmentManager.findFragmentById(R.id.map_fragment) as SupportMapFragment
        mapFragment.getMapAsync { initMap(it) }

        btn_reset_home.setOnClickListener { restartDefineHomeFlow() }
        btn_drawer_reset_home.setOnClickListener {
            drawer_layout.closeDrawer(drawer)
            restartDefineHomeFlow()
        }
        btn_center_home.setOnLongClickListener {
            restartDefineHomeFlow()
            true
        }

        btn_settings.setOnClickListener { drawer_layout.openDrawer(drawer) }

        btn_drawer_factory_reset.setOnClickListener {
            drawer_layout.closeDrawer(drawer)

            factoryReset()
        }

        btn_drawer_attestation.setOnClickListener {
            drawer_layout.closeDrawer(drawer)
            val webpage: Uri =
                Uri.parse("https://mobile.interieur.gouv.fr/Actualites/L-actu-du-Ministere/Attestation-de-deplacement-derogatoire-et-justificatif-de-deplacement-professionnel")
            val intent = Intent(Intent.ACTION_VIEW, webpage)
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            }
        }

        btn_center_my_location.setOnClickListener {
            val flow = currentFlow
            val myLoc = myLocation

            if (flow != null && myLoc != null) {
                flow.animateCameraToPerceivedLocation(myLoc)
            }
        }
        btn_center_home.setOnClickListener {
            currentFlow?.animateCameraToPerceivedLocation(homeZone.center)
        }

        drawer_layout.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerStateChanged(newState: Int) {}
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {}

            override fun onDrawerClosed(drawerView: View) {
                drawer_layout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
            }

            override fun onDrawerOpened(drawerView: View) {
                drawer_layout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
            }

        })
        drawer_layout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)

        demoFlow = DemoFlow(this)
        enableLocationFlow = EnableLocationFlow(this)
        defineHomeFlow = DefineHomeFlow(this)
        baladeSetupFlow = BaladeSetupFlow(this)
        baladeFlow = BaladeFlow(this)

        persistentData = PersistentDataRepository(this)

        BuildVariantSpecific.onCreateActivity(this)
    }

    override fun onStart() {
        super.onStart()

        // Bind to the service. If the service is in foreground mode, this signals to the service
        // that since this activity is in the foreground, the service can exit foreground mode.
        bindService(
            Intent(this, LocationUpdatesService::class.java), locationUpdatesServiceConnection,
            Context.BIND_AUTO_CREATE
        )

        currentFlow?.resume()
    }

    override fun onStop() {
        currentFlow?.pause()

        if (locationUpdatesService != null) {
            // Unbind from the service. This signals to the service that this activity is no longer
            // in the foreground, and the service can respond by promoting itself to a foreground
            // service.
            unbindService(locationUpdatesServiceConnection)
            locationUpdatesService = null
        }

        super.onStop()
    }

    private fun restartDefineHomeFlow() {
        myLocation?.let { defineHomeFlow.animateCameraToPerceivedLocation(it) }
        persistentData.savedHomeLocation = null
        currentFlow = defineHomeFlow
    }

    private fun factoryReset() {
        // Separate null and reassignment to ensure currentFlow.stop() doesn't have any side effects
        currentFlow = null

        map.isMyLocationEnabled = false
        VolatileDataRepository.factoryReset()
        persistentData.factoryReset()

        currentFlow = demoFlow
    }

    @Suppress("ConstantConditionIf", "SENSELESS_COMPARISON")
    private fun initMap(map: GoogleMap) {
        this.map = map
        this.homeZone = HomeZone(this, map)
//        this.myLocationMarker = map.addCircle(
//            CircleOptions()
//                .center(map.cameraPosition.target)
//                .radius(25.0)
//                .strokeWidth(0f)
//                .fillColor(Color.parseColor("#4040FF"))
//        )

        // Debug
        map.setOnMapClickListener { Log.v(this@MapsActivity.logTag, "Map clicked at $it") }

        // General settings
        map.uiSettings.isMyLocationButtonEnabled = false
        map.setOnMarkerClickListener { true }

        // We need a map before we can move myPosition
        VolatileDataRepository.myLocation.observe(this, Observer {
            Log.v(this@MapsActivity.logTag, "onReportedLocationUpdate: $it")
            myLocation = it
        })

        // Initial flow: always start with demoFlow although it might be skipped instantly
        currentFlow = demoFlow
    }

    fun onDemoFlowFinished() {
        currentFlow = enableLocationFlow
    }

    fun onEnableLocationFlowFinished(successfully: Boolean) {
        currentFlow = defineHomeFlow
    }

    fun onChooseHomeFlowFinished() {
        currentFlow =
            if (VolatileDataRepository.baladeStatus is VolatileDataRepository.BaladeStatus.Balading) {
                // Probable case: MainActivity was recreated while the foreground service was running
                baladeFlow
            } else {
                baladeSetupFlow
            }
    }

    fun onBaladeSetupFlowFinished() {
        currentFlow = baladeFlow
    }

    fun onBaladeFlowFinished() {
        currentFlow = baladeSetupFlow
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        Log.v(
            logTag,
            "onRequestPermissionsResult: $requestCode, $permissions, $grantResults"
        )

        currentFlow?.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.v(
            logTag,
            "onActivityResult: $requestCode, $resultCode, $data"
        )

        currentFlow?.onActivityResult(requestCode, resultCode, data)
        super.onActivityResult(requestCode, resultCode, data)
    }

    fun lockMap() {
        map.uiSettings.setAllGesturesEnabled(false)
    }

    fun unlockMap() {
        map.uiSettings.setAllGesturesEnabled(true)
        map.uiSettings.isRotateGesturesEnabled = false
    }

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(drawer)) {
            drawer_layout.closeDrawer(drawer)
        } else {
            super.onBackPressed()
        }
    }
}

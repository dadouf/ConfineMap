/**
 * Copyright 2017 Google Inc. All Rights Reserved.
 *
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.davidferrand.confinemap

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.Observer
import com.davidferrand.confinemap.model.PersistentDataRepository
import com.davidferrand.confinemap.model.VolatileDataRepository
import com.davidferrand.confinemap.model.VolatileDataRepository.LocationRequestLevel.FOREGROUND_BACKGROUND
import com.davidferrand.confinemap.model.VolatileDataRepository.LocationRequestLevel.FOREGROUND_ONLY
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import kotlin.math.roundToInt

class LocationUpdatesService : LifecycleService() {

    private val binder: IBinder = LocalBinder()

    inner class LocalBinder : Binder() {
        val service: LocationUpdatesService get() = this@LocationUpdatesService
    }

    private lateinit var notificationManager: NotificationManagerCompat
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var persistentData: PersistentDataRepository

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            super.onLocationResult(locationResult)
            Log.i(
                this@LocationUpdatesService.logTag,
                "onRealLocationUpdate: ${locationResult.lastLocation}"
            )

            VolatileDataRepository.updateMyLocation(locationResult.lastLocation.toLatLng())
        }
    }

    private val requestLevelObserver = Observer<VolatileDataRepository.LocationRequestLevel> {
        Log.v(logTag, "Location request level is now: $it")
        when (it) {
            FOREGROUND_ONLY, FOREGROUND_BACKGROUND -> startLocationUpdates()
            else -> stopLocationUpdates()
        }
    }

    private var myLastKnowLocation: LatLng? = null

    /** 10s vibration */
    private val alarmVibrationPattern = longArrayOf(
        500, 500, 500, 500, 500,
        500, 500, 500, 500, 500,
        500, 500, 500, 500, 500,
        500, 500, 500, 500, 500
    )
    private val alarmSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)

    override fun onCreate() {
        super.onCreate()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        notificationManager = NotificationManagerCompat.from(this)
        persistentData = PersistentDataRepository(this)

        initNotificationChannels()

        VolatileDataRepository.myLocation.observe(this, Observer {
            myLastKnowLocation = it

            if (serviceIsRunningInForeground(this@LocationUpdatesService)) {
                val (mainNotification, shouldBuildAlert) = buildMainNotification()

                notificationManager.notify(MAIN_NOTIFICATION_ID, mainNotification)

                if (shouldBuildAlert) {
                    notificationManager.notify(ALERT_NOTIFICATION_ID, buildAlertNotification())
                } else {
                    notificationManager.cancel(ALERT_NOTIFICATION_ID)
                }
            }
        })
    }

    /**
     * TODO ffs changing any of these settings
     */
    private fun initNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(
                NotificationChannel(
                    ID_CHANNEL_OK, "État", NotificationManager.IMPORTANCE_LOW
                )
            )

            notificationManager.createNotificationChannel(
                NotificationChannel(
                    ID_CHANNEL_NOT_OK, "Alerte", NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    vibrationPattern = alarmVibrationPattern
                    setSound(
                        alarmSoundUri,
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION).build()
                    )
                }
            )
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        Log.i(logTag, "Service started")

        val startedFromNotification = intent?.getBooleanExtra(
            EXTRA_STARTED_FROM_NOTIFICATION,
            false
        ) ?: false

        if (startedFromNotification) {
            stopLocationUpdates()

            VolatileDataRepository.baladeStatus = VolatileDataRepository.BaladeStatus.AtHome
        }

        // Tells the system to not try to recreate the service after it has been killed.
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        Log.i(logTag, "onBind: $intent")
        onBindOrRebind()

        return binder
    }

    override fun onRebind(intent: Intent) {
        Log.i(logTag, "onRebind: $intent")
        onBindOrRebind()
        super.onRebind(intent)
    }

    private fun onBindOrRebind() {
        Log.i(logTag, "stopForeground")
        stopForeground(true)
        notificationManager.cancel(ALERT_NOTIFICATION_ID)

        // Important to restart observing in onBindOrRebind to cover both main cases:
        // - if this is the first start, trivial
        // - but this is an Activity resume, we want to start requesting again now
        VolatileDataRepository.locationRequestLevel.observe(this, requestLevelObserver)
    }

    override fun onUnbind(intent: Intent): Boolean {
        Log.i(logTag, "onUnbind: $intent")

        VolatileDataRepository.locationRequestLevel.removeObserver(requestLevelObserver)

        if (VolatileDataRepository.locationRequestLevel.value == FOREGROUND_BACKGROUND) {
            Log.i(logTag, "startForeground")
            startForeground(MAIN_NOTIFICATION_ID, buildMainNotification().first)

        } else {
            stopLocationUpdates()
        }

        // Ensures onRebind() is called when a client re-binds
        return true
    }

    private fun startLocationUpdates() {
        Log.i(logTag, "startLocationUpdates")

        // The service MUST be started if we're going set it as a foreground service
        startService(Intent(applicationContext, LocationUpdatesService::class.java))

        try {
            fusedLocationClient.requestLocationUpdates(
                VolatileDataRepository.locationRequest,
                locationCallback,
                Looper.myLooper()
            )
        } catch (unlikely: SecurityException) {
            Log.e(logTag, "Lost location permission. Could not request updates.", unlikely)
        }
    }

    private fun stopLocationUpdates() {
        Log.i(logTag, "stopLocationUpdates")

        fusedLocationClient.removeLocationUpdates(locationCallback)

        stopSelf()
    }

    /**
     * @return The main notification and whether we should also build an alert
     */
    private fun buildMainNotification(): Pair<Notification, Boolean> {
        val home = persistentData.savedHomeLocation
        val me = myLastKnowLocation

        val distanceToHome: Int? = if (home == null || me == null) {
            null
        } else {
            home.distanceTo(me).toInt()
        }
        val formattedDistance = distanceToHome?.let { (it / 50f).roundToInt() * 50 }

        // Consider that "unknown" is okay
        val inZone = distanceToHome == null || distanceToHome <= 1_000

        Log.v(
            logTag,
            "buildNotification: distance=$distanceToHome, formattedDistance=$formattedDistance inZone=$inZone"
        )

        val title = "En balade"

        val builder =
            NotificationCompat.Builder(this, ID_CHANNEL_OK)
                .setSmallIcon(R.drawable.ic_directions_run_white_24dp)
                .setContentTitle(title)
                .setTicker(title)
                .setContentIntent(createPendingIntentToActivity())
                .addAction(
                    R.drawable.ic_stop_black_24dp,
                    "Stop",
                    PendingIntent.getService(
                        this, 0,
                        Intent(this, LocationUpdatesService::class.java).putExtra(
                            EXTRA_STARTED_FROM_NOTIFICATION,
                            true
                        ),
                        PendingIntent.FLAG_UPDATE_CURRENT
                    )
                )
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOnlyAlertOnce(true)

        if (formattedDistance != null) {
            builder.setContentText("À ${formattedDistance}m du domicile")
        }

        return builder.build() to !inZone
    }

    private fun createPendingIntentToActivity(): PendingIntent {
        // TODO study flags to reopen the same one
        return PendingIntent.getActivity(
            this, 0, Intent(this, MapsActivity::class.java), 0
        )
    }

    private fun buildAlertNotification(): Notification {
        val title = "Rentrez !"

        val builder =
            NotificationCompat.Builder(this, ID_CHANNEL_NOT_OK)
                .setSmallIcon(R.drawable.ic_directions_run_white_24dp_flipped)
                .setContentTitle(title)
                .setContentIntent(createPendingIntentToActivity())
                .setTicker(title)
                .setContentText("Vous avez dépassé le périmètre raisonnable")
                .setOngoing(true)
                .setAutoCancel(true)
                .setColorized(true)
                .setColor(ResourcesCompat.getColor(resources, R.color.not_okay, theme))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setOnlyAlertOnce(true)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                // The below doesn't work on API 26+: it needs to be set at the channel level (apparently)
                .setSound(alarmSoundUri)
                .setVibrate(alarmVibrationPattern)

        return builder.build()
    }

    private fun serviceIsRunningInForeground(context: Context): Boolean {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (javaClass.name == service.service.className) {
                if (service.foreground) {
                    return true
                }
            }
        }

        return false
    }

    override fun onDestroy() {
        Log.v(logTag, "onDestroy")
        super.onDestroy()
    }

    companion object {
        private const val ID_CHANNEL_OK = "channel_ok"
        private const val ID_CHANNEL_NOT_OK = "channel_not_ok"

        private const val EXTRA_STARTED_FROM_NOTIFICATION = BuildConfig.APPLICATION_ID +
                ".started_from_notification"

        private const val MAIN_NOTIFICATION_ID = 1234
        private const val ALERT_NOTIFICATION_ID = 5678
    }
}
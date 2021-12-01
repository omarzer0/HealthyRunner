package az.zero.healthyrunner.services

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_LOW
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import az.zero.healthyrunner.R
import az.zero.healthyrunner.ui.MainActivity
import az.zero.healthyrunner.utils.Constants.ACTION_PAUSE_SERVICE
import az.zero.healthyrunner.utils.Constants.ACTION_SHOW_TRACKING_FRAGMENT
import az.zero.healthyrunner.utils.Constants.ACTION_START_OR_RESUME_SERVICE
import az.zero.healthyrunner.utils.Constants.ACTION_STOP_SERVICE
import az.zero.healthyrunner.utils.Constants.FASTEST_LOCATION_UPDATE_INTERVAL
import az.zero.healthyrunner.utils.Constants.LOCATION_UPDATE_INTERVAL
import az.zero.healthyrunner.utils.Constants.NOTIFICATION_CHANNEL_ID
import az.zero.healthyrunner.utils.Constants.NOTIFICATION_CHANNEL_NAME
import az.zero.healthyrunner.utils.Constants.NOTIFICATION_ID
import az.zero.healthyrunner.utils.TrackingUtils
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY
import com.google.android.gms.location.LocationResult
import com.google.android.gms.maps.model.LatLng
import timber.log.Timber

typealias Polyline = MutableList<LatLng>
typealias Polylines = MutableList<Polyline>

class TrackingService : LifecycleService() {
    private var isFirstRun = true
    private lateinit var fuseLocationProvider: FusedLocationProviderClient

    // singleton pattern
    companion object {
        val isTracking = MutableLiveData<Boolean>()
        val pathPoints = MutableLiveData<Polylines>()
    }

    private fun postInitValues() {
        isTracking.postValue(false)
        pathPoints.postValue(mutableListOf())
    }

    @SuppressLint("VisibleForTests")
    override fun onCreate() {
        super.onCreate()
        postInitValues()
        fuseLocationProvider = FusedLocationProviderClient(this)

        isTracking.observe(this) {
            updateLocationTracking(it)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let { passedIntent ->
            when (passedIntent.action) {
                ACTION_START_OR_RESUME_SERVICE -> {
                    if (isFirstRun) {
                        startForegroundService()
                        isFirstRun = false
                    } else {
                        Timber.d("Resuming service")
                    }
                }
                ACTION_PAUSE_SERVICE -> {
                    Timber.d("ACTION_PAUSE_SERVICE")
                }
                ACTION_STOP_SERVICE -> {
                    Timber.d("ACTION_STOP_SERVICE")
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun addEmptyPolyline() = pathPoints.value?.apply {
        add(mutableListOf())
        pathPoints.postValue(this)
    } ?: pathPoints.postValue(mutableListOf(mutableListOf()))

    @SuppressLint("MissingPermission")
    private fun updateLocationTracking(isTracking: Boolean) {
        if (isTracking) {
            if (TrackingUtils.hasLocationPermission(this)) {
                val request = LocationRequest.create().apply {
                    interval = LOCATION_UPDATE_INTERVAL
                    fastestInterval = FASTEST_LOCATION_UPDATE_INTERVAL
                    priority = PRIORITY_HIGH_ACCURACY
                }

                fuseLocationProvider.requestLocationUpdates(
                    request, locationCallback, Looper.getMainLooper()
                )
            }
        } else {
            // if not tracking remove the callback that updates the PathPoint
            fuseLocationProvider.removeLocationUpdates(locationCallback)
        }
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            super.onLocationResult(result)
            isTracking.value?.let {
                if (it) {
                    for (location in result.locations) {
                        addPathPoint(location)
                        Timber.d("new location ${location.latitude} : ${location.longitude}")
                    }
                }
            }
        }
    }

    private fun addPathPoint(location: Location?) {
        location?.let {
            val position = LatLng(location.latitude, location.longitude)
            pathPoints.value?.apply {
                last().add(position)
                pathPoints.postValue(this)
            }
        }
    }

    private fun startForegroundService() {
        addEmptyPolyline()
        isTracking.postValue(true)
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(notificationManager)
        }
        val notificationBuilder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setAutoCancel(false)
            // notification can't be swiped away with setOngoing(true)
            .setOngoing(true)
            .setSmallIcon(R.drawable.ic_directions_run_black_24dp)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("00:00:00")
            // TODO 1: Try DeepLinks with pending intent
            .setContentIntent(getMainActivityPendingIntent())
            .build()

        startForeground(NOTIFICATION_ID, notificationBuilder)
    }

    // TODO 1 solved: Try DeepLinks with pending intent

//    private fun pendingIntent(): PendingIntent = NavDeepLinkBuilder(this)
//        .setGraph(R.navigation.nav_graph)
//        .setComponentName(MainActivity::class.java)
//        .setDestination(R.id.trackingFragment)
//        .createPendingIntent()


    private fun getMainActivityPendingIntent() = PendingIntent.getActivity(
        this, 0,
        Intent(this, MainActivity::class.java).also {
            it.action = ACTION_SHOW_TRACKING_FRAGMENT
        },
        FLAG_UPDATE_CURRENT
    )

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(notificationManager: NotificationManager) {
        val channel =
            NotificationChannel(NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_NAME, IMPORTANCE_LOW)
        notificationManager.createNotificationChannel(channel)
    }
}
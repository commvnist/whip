package com.whip.app.reminders

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.whip.app.WhipApplication
import com.whip.app.domain.LocationTrigger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class LocationReminderScheduler(private val context: Context) {
    private val appContext = context.applicationContext
    private val client = LocationServices.getGeofencingClient(appContext)

    @SuppressLint("MissingPermission")
    suspend fun syncTask(taskId: Long) {
        client.removeGeofences(listOf(requestId(taskId)))
        val app = appContext as WhipApplication
        if (!app.settingsRepository.current().locationRemindersEnabled || !hasPermissions()) return
        val task = app.taskRepository.getTask(taskId) ?: return
        val location = task.locationReminder ?: return
        if (task.archived || task.completedAtMillis != null) return
        val transition = if (location.trigger == LocationTrigger.Arrive) {
            Geofence.GEOFENCE_TRANSITION_ENTER
        } else {
            Geofence.GEOFENCE_TRANSITION_EXIT
        }
        val geofence = Geofence.Builder()
            .setRequestId(requestId(taskId))
            .setCircularRegion(location.latitude, location.longitude, location.radiusMeters)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(transition)
            .build()
        val request = GeofencingRequest.Builder().addGeofence(geofence).build()
        client.addGeofences(request, pendingIntent())
    }

    suspend fun syncAll() {
        val app = appContext as WhipApplication
        if (!app.settingsRepository.current().locationRemindersEnabled || !hasPermissions()) {
            clearAll()
            return
        }
        // Removing a geofence for every ordinary task creates one Binder object
        // per row and can make Android kill the process on large databases.
        // Mutation paths clear individual stale geofences; bulk sync only needs
        // records that can actually register one.
        app.database.taskDao().getLocationReminderTaskIds().forEach { syncTask(it) }
    }

    fun clearAll() {
        client.removeGeofences(pendingIntent())
    }

    private fun hasPermissions(): Boolean {
        val fine = ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        val background = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_BACKGROUND_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        return fine && background
    }

    private fun pendingIntent(): PendingIntent = PendingIntent.getBroadcast(
        appContext,
        0x57484C4F,
        Intent(appContext, LocationReminderReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0,
    )

    companion object {
        fun requestId(taskId: Long): String = "whip-task-location-$taskId"
        fun taskId(requestId: String): Long? = requestId.removePrefix("whip-task-location-")
            .takeIf { requestId.startsWith("whip-task-location-") }?.toLongOrNull()
    }
}

class LocationReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError()) return
        val result = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as WhipApplication
                event.triggeringGeofences.orEmpty().forEach { geofence ->
                    val taskId = LocationReminderScheduler.taskId(geofence.requestId) ?: return@forEach
                    val task = app.taskRepository.getTask(taskId) ?: return@forEach
                    if (!task.archived && task.completedAtMillis == null && task.locationReminder != null) {
                        ReminderNotifications.showLocation(context, task)
                    }
                }
            } finally {
                result.finish()
            }
        }
    }
}

class LocationReminderBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val result = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                LocationReminderScheduler(context).syncAll()
            } finally {
                result.finish()
            }
        }
    }
}

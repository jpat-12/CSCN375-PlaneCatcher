package com.planecatcher.notify

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.planecatcher.MainActivity
import com.planecatcher.R
import com.planecatcher.core.catalog.AircraftCatalog
import com.planecatcher.core.radar.NearbyPlane
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Notifier @Inject constructor(@ApplicationContext private val context: Context) {

    fun createChannels() {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_NEARBY, context.getString(R.string.channel_nearby_name), NotificationManager.IMPORTANCE_HIGH)
                .apply { description = context.getString(R.string.channel_nearby_desc) },
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_RADAR, context.getString(R.string.channel_radar_name), NotificationManager.IMPORTANCE_LOW)
                .apply { description = context.getString(R.string.channel_radar_desc) },
        )
    }

    fun canPost(): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    fun notifyPlane(plane: NearbyPlane) {
        if (!canPost()) return
        val a = plane.aircraft
        val type = AircraftCatalog.lookup(a.typeCode)?.fullName ?: a.typeCode ?: "Unknown type"
        val n = NotificationCompat.Builder(context, CHANNEL_NEARBY)
            .setSmallIcon(R.drawable.ic_stat_plane)
            .setContentTitle("${plane.tier.displayName} plane nearby: ${a.displayName}")
            .setContentText("$type, %.1f mi away. Tap to catch it.".format(plane.distanceMiles))
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent(a.hex))
            .build()
        try {
            NotificationManagerCompat.from(context).notify(a.hex.hashCode(), n)
        } catch (_: SecurityException) {
            // Permission revoked between the check and the call.
        }
    }

    fun radarNotification(text: String): Notification =
        NotificationCompat.Builder(context, CHANNEL_RADAR)
            .setSmallIcon(R.drawable.ic_stat_plane)
            .setContentTitle("PlaneCatcher radar is on")
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openAppIntent(null))
            .build()

    private fun openAppIntent(hex: String?): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .apply { if (hex != null) putExtra(MainActivity.EXTRA_PLANE_HEX, hex) }
        return PendingIntent.getActivity(
            context, hex?.hashCode() ?: 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        const val CHANNEL_NEARBY = "planes_nearby"
        const val CHANNEL_RADAR = "radar_running"
        const val RADAR_NOTIFICATION_ID = 1
    }
}

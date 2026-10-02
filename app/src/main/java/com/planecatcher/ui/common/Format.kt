package com.planecatcher.ui.common

import com.planecatcher.core.geo.Geo
import java.text.DateFormat
import java.util.Date
import java.util.Locale

/** "2h 14m", "14m 05s", "42s". */
fun formatDuration(ms: Long): String {
    val totalSec = (ms.coerceAtLeast(0) + 999) / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return when {
        h > 0 -> String.format(Locale.US, "%dh %02dm", h, m)
        m > 0 -> String.format(Locale.US, "%dm %02ds", m, s)
        else -> "${s}s"
    }
}

fun formatDistance(miles: Double, metric: Boolean): String =
    if (metric) String.format(Locale.getDefault(), "%.1f km", Geo.milesToKm(miles))
    else String.format(Locale.getDefault(), "%.1f mi", miles)

fun formatAltitude(feet: Int?, metric: Boolean): String = when {
    feet == null -> "Altitude unknown"
    metric -> String.format(Locale.getDefault(), "%,d m", (feet * 0.3048).toInt())
    else -> String.format(Locale.getDefault(), "%,d ft", feet)
}

fun formatSpeed(knots: Double?, metric: Boolean): String = when {
    knots == null -> "—"
    metric -> String.format(Locale.getDefault(), "%.0f km/h", knots * 1.852)
    else -> String.format(Locale.getDefault(), "%.0f mph", knots * 1.150779)
}

fun formatDateTime(epochMs: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(epochMs))

fun formatDate(epochMs: Long): String =
    DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(epochMs))

fun formatLatLon(lat: Double, lon: Double): String =
    String.format(Locale.US, "%.3f°%s, %.3f°%s", kotlin.math.abs(lat), if (lat >= 0) "N" else "S", kotlin.math.abs(lon), if (lon >= 0) "E" else "W")

package com.planecatcher.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.planecatcher.core.catalog.AircraftCatalog
import com.planecatcher.ui.common.PlanePhoto
import com.planecatcher.ui.common.TierBadge
import com.planecatcher.ui.common.formatAltitude
import com.planecatcher.ui.common.formatDistance
import com.planecatcher.ui.common.formatDuration

@Composable
fun PlanePopup(
    popup: PopupState,
    nowMs: Long,
    useMetric: Boolean,
    onStartCatch: () -> Unit,
    onDismiss: () -> Unit,
) {
    val a = popup.plane.aircraft
    val info = AircraftCatalog.lookup(a.typeCode)
    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Plane overhead!", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    TierBadge(popup.plane.tier)
                }
                PlanePhoto(
                    url = popup.photo?.url,
                    tier = popup.plane.tier,
                    modifier = Modifier.fillMaxWidth().aspectRatio(16f / 10f),
                )
                val credit = when {
                    popup.photoLoading -> "Loading photo…"
                    popup.photo == null -> "No photo available"
                    else -> "Photo © ${popup.photo.photographer ?: "unknown"} · planespotters.net"
                }
                Text(credit, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                Text(info?.fullName ?: a.typeCode ?: "Unknown type", style = MaterialTheme.typography.headlineSmall)
                Text(
                    listOfNotNull(a.callsign, a.registration).joinToString(" · ").ifEmpty { a.hex.uppercase() },
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    "${formatDistance(popup.plane.distanceMiles, useMetric)} away · ${formatAltitude(a.altitudeFt, useMetric)}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "Worth ${popup.plane.tier.points} ${if (popup.plane.tier.points == 1) "point" else "points"}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(Modifier.height(4.dp))
                when {
                    popup.caught -> Text("Already in your collection ✓", color = MaterialTheme.colorScheme.primary)
                    popup.lockedUntilMs != null -> Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Text(
                            "  Quiz locked. Try again in ${formatDuration(popup.lockedUntilMs - nowMs)}",
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Not now") }
                    Button(
                        onClick = onStartCatch,
                        enabled = !popup.caught && popup.lockedUntilMs == null,
                        modifier = Modifier.weight(1f),
                    ) { Text("Start Catch") }
                }
            }
        }
    }
}

package com.planecatcher.ui.collection

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.planecatcher.core.catalog.AircraftCatalog
import com.planecatcher.core.catalog.Airlines
import com.planecatcher.core.catalog.RegistrationCountries
import com.planecatcher.ui.common.PlanePhoto
import com.planecatcher.ui.common.TierBadge
import com.planecatcher.ui.common.formatAltitude
import com.planecatcher.ui.common.formatDateTime
import com.planecatcher.ui.common.formatDistance
import com.planecatcher.ui.common.formatLatLon

@Composable
fun PlaneDetailScreen(onBack: () -> Unit, vm: PlaneDetailViewModel = hiltViewModel()) {
    val plane by vm.plane.collectAsStateWithLifecycle()
    val metric by vm.useMetric.collectAsStateWithLifecycle()
    val uri = LocalUriHandler.current

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            Text("Plane details", style = MaterialTheme.typography.titleLarge)
        }
        val p = plane ?: return@Column
        val info = AircraftCatalog.lookup(p.typeCode)

        PlanePhoto(p.photoUrl, p.tier, Modifier.fillMaxWidth().aspectRatio(16f / 10f))
        if (p.photoUrl != null) {
            Text(
                "Photo © ${p.photographer ?: "unknown"} · planespotters.net",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
                textDecoration = if (p.photoLink != null) TextDecoration.Underline else null,
                modifier = p.photoLink?.let { link -> Modifier.clickable { uri.openUri(link) } } ?: Modifier,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TierBadge(p.tier)
            Text("${p.points} ${if (p.points == 1) "point" else "points"}", style = MaterialTheme.typography.titleMedium)
        }
        Text(p.typeName ?: info?.fullName ?: p.typeCode ?: "Unknown type", style = MaterialTheme.typography.headlineSmall)

        HorizontalDivider()
        StatRow("Registration", p.registration)
        StatRow("Registered in", RegistrationCountries.fromRegistration(p.registration))
        StatRow("Callsign", p.callsign)
        StatRow("Operator", Airlines.fromCallsign(p.callsign))
        StatRow("Type code", p.typeCode)
        StatRow("Category", info?.category?.label)
        StatRow("Engines", info?.let { "${it.engines} × ${it.engineType.label.lowercase()}" })
        StatRow("Typical seats", info?.typicalSeats?.takeIf { it > 0 }?.toString())
        StatRow("Typical range", info?.rangeNm?.takeIf { it > 0 }?.let { "%,d nm".format(it) })
        StatRow("Military", if (p.isMilitary) "Yes" else null)
        StatRow("ICAO address", p.hex.uppercase())
        HorizontalDivider()
        StatRow("Caught", formatDateTime(p.caughtAt))
        StatRow("Caught from", formatLatLon(p.lat, p.lon))
        StatRow("Via Location Jump", p.jumpCode)
        StatRow("Altitude at catch", p.altitudeFt?.let { formatAltitude(it, metric) })
        StatRow("Distance at catch", formatDistance(p.distanceMiles, metric))
    }
}

@Composable
private fun StatRow(label: String, value: String?) {
    if (value.isNullOrBlank()) return
    Row(Modifier.fillMaxWidth()) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

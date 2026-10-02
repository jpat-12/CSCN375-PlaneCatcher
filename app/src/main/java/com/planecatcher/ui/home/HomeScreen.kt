package com.planecatcher.ui.home

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.planecatcher.core.catalog.AircraftCatalog
import com.planecatcher.core.radar.NearbyPlane
import com.planecatcher.core.rules.JumpStatus
import com.planecatcher.ui.common.TierBadge
import com.planecatcher.ui.common.formatAltitude
import com.planecatcher.ui.common.formatDistance
import com.planecatcher.ui.common.formatDuration
import com.planecatcher.ui.theme.color

@Composable
fun HomeScreen(
    requestedHex: String?,
    onRequestHandled: () -> Unit,
    onStartCatch: (String) -> Unit,
    vm: HomeViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    var showJumpSheet by remember { mutableStateOf(false) }

    // Poll only while this screen is on screen.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> vm.onVisible()
                Lifecycle.Event.ON_STOP -> vm.onHidden()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            vm.onHidden()
        }
    }

    LaunchedEffect(requestedHex) {
        if (requestedHex != null) {
            vm.requestPopup(requestedHex)
            onRequestHandled()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { vm.refresh() }

    val radar = state.radar
    val inRange = radar.planes.filter { it.inRange }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("PlaneCatcher", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        if (radar.jumpTarget != null) "Radar at ${radar.jumpTarget.name}" else "Radar around you · 10 mi",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = vm::refresh) { Icon(Icons.Filled.Refresh, contentDescription = "Refresh now") }
            }
        }

        item { JumpCard(state.jump, onOpen = { showJumpSheet = true }, onEnd = vm::endJump) }

        item {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                RadarView(
                    planes = radar.planes,
                    caughtHexes = state.caughtHexes,
                    isJump = radar.jumpTarget != null,
                    onPlaneTap = vm::onPlaneTapped,
                    modifier = Modifier.widthIn(max = 420.dp),
                )
            }
        }

        item {
            StatusLine(
                state = state,
                inRangeCount = inRange.size,
                onGrantLocation = {
                    permissionLauncher.launch(
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                    )
                },
            )
        }

        if (radar.mockLocation) {
            item {
                Notice(
                    "Mock location detected. Catching is disabled until real GPS is used. " +
                        "Use Location Jump to explore other areas.",
                )
            }
        }

        items(inRange, key = { it.hex }) { plane ->
            PlaneRow(
                plane = plane,
                caught = plane.hex in state.caughtHexes,
                lockedUntil = state.lockedUntil(plane.hex),
                nowMs = state.nowMs,
                useMetric = state.settings.useMetric,
                onClick = { vm.onPlaneTapped(plane.hex) },
            )
        }
    }

    state.popup?.let { popup ->
        PlanePopup(
            popup = popup,
            nowMs = state.nowMs,
            useMetric = state.settings.useMetric,
            onStartCatch = {
                val hex = popup.plane.hex
                vm.dismissPopup()
                onStartCatch(hex)
            },
            onDismiss = vm::dismissPopup,
        )
    }

    if (showJumpSheet) {
        JumpSheet(
            onPick = {
                showJumpSheet = false
                vm.activateJump(it)
            },
            onDismiss = { showJumpSheet = false },
        )
    }
}

@Composable
private fun JumpCard(status: JumpStatus, onOpen: () -> Unit, onEnd: () -> Unit) {
    val active = status is JumpStatus.Active
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.TravelExplore, contentDescription = null)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                when (status) {
                    is JumpStatus.Active -> {
                        Text("Jumped to ${status.target.code}", style = MaterialTheme.typography.titleMedium)
                        Text("Ends in ${formatDuration(status.remainingMs)} or on your next catch")
                    }
                    is JumpStatus.Cooldown -> {
                        Text("Location Jump", style = MaterialTheme.typography.titleMedium)
                        Text("Available in ${formatDuration(status.remainingMs)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    JumpStatus.Ready -> {
                        Text("Location Jump ready", style = MaterialTheme.typography.titleMedium)
                        Text("Catch planes at a busy airport", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            when (status) {
                is JumpStatus.Active -> OutlinedButton(onClick = onEnd) { Text("End") }
                is JumpStatus.Cooldown -> Icon(Icons.Filled.Lock, contentDescription = "Locked")
                JumpStatus.Ready -> FilledTonalButton(onClick = onOpen) { Text("Jump") }
            }
        }
    }
}

@Composable
private fun StatusLine(state: HomeUiState, inRangeCount: Int, onGrantLocation: () -> Unit) {
    val radar = state.radar
    when {
        radar.jumpTarget == null && !radar.hasLocationPermission -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("PlaneCatcher needs your location to find planes above you.")
            Button(onClick = onGrantLocation) { Text("Allow location") }
        }
        radar.center == null -> Text("Finding your location…", color = MaterialTheme.colorScheme.onSurfaceVariant)
        radar.error != null -> Notice(radar.error)
        radar.lastUpdatedMs == null -> Text("Scanning the sky…", color = MaterialTheme.colorScheme.onSurfaceVariant)
        else -> {
            val ago = ((state.nowMs - radar.lastUpdatedMs) / 1000).coerceAtLeast(0)
            Text(
                when (inRangeCount) {
                    0 -> "No planes in range right now · updated ${ago}s ago"
                    1 -> "1 plane in range · updated ${ago}s ago"
                    else -> "$inRangeCount planes in range · updated ${ago}s ago"
                },
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@Composable
private fun Notice(text: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f), MaterialTheme.shapes.medium)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
        Text(text, modifier = Modifier.padding(start = 10.dp))
    }
}

@Composable
private fun PlaneRow(
    plane: NearbyPlane,
    caught: Boolean,
    lockedUntil: Long?,
    nowMs: Long,
    useMetric: Boolean,
    onClick: () -> Unit,
) {
    val a = plane.aircraft
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(Modifier.height(IntrinsicSize.Min), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(plane.tier.color),
            )
            Column(Modifier.weight(1f).padding(14.dp)) {
                Text(
                    AircraftCatalog.lookup(a.typeCode)?.fullName ?: a.typeCode ?: "Unknown type",
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${a.displayName} · ${formatDistance(plane.distanceMiles, useMetric)} · ${formatAltitude(a.altitudeFt, useMetric)}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                when {
                    caught -> Text("Caught ✓", color = MaterialTheme.colorScheme.primary)
                    lockedUntil != null -> Text("Locked · ${formatDuration(lockedUntil - nowMs)}", color = MaterialTheme.colorScheme.error)
                }
            }
            TierBadge(plane.tier, Modifier.padding(end = 14.dp))
        }
    }
}

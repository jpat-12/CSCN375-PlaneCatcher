package com.planecatcher.ui.home

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.planecatcher.core.rules.JumpDestinations
import com.planecatcher.core.rules.JumpStatus
import com.planecatcher.ui.common.formatDuration

/**
 * Keeps the radar polling while the calling screen is visible, keeps the screen awake
 * if the user wants that, and opens a pop-up requested by a notification.
 */
@Composable
fun RadarScreenEffects(
    vm: HomeViewModel,
    state: HomeUiState,
    requestedHex: String?,
    onRequestHandled: () -> Unit,
) {
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

    val view = LocalView.current
    DisposableEffect(view, state.settings.keepScreenOn) {
        view.keepScreenOn = state.settings.keepScreenOn
        onDispose { view.keepScreenOn = false }
    }

    LaunchedEffect(requestedHex) {
        if (requestedHex != null) {
            vm.requestPopup(requestedHex)
            onRequestHandled()
        }
    }
}

/** The plane pop-up and the Location Jump sheet, shared by Camera and Map. */
@Composable
fun RadarOverlays(
    vm: HomeViewModel,
    state: HomeUiState,
    showJumpSheet: Boolean,
    onJumpSheetClosed: () -> Unit,
    onStartCatch: (String) -> Unit,
) {
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
                onJumpSheetClosed()
                vm.activateJump(it)
            },
            onDismiss = onJumpSheetClosed,
        )
    }
}

@Composable
fun JumpCard(status: JumpStatus, onOpen: () -> Unit, onEnd: () -> Unit, modifier: Modifier = Modifier) {
    val active = status is JumpStatus.Active
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
            contentColor = if (active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.TravelExplore, contentDescription = null)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                when (status) {
                    is JumpStatus.Active -> {
                        Text(
                            if (status.target.code == JumpDestinations.CUSTOM_CODE) "Jumped to your map pin" else "Jumped to ${status.target.code}",
                            style = MaterialTheme.typography.titleMedium,
                        )
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

/**
 * Why there's nothing to catch yet (no permission, no fix, an error), or null when the
 * radar is working. [onGrantLocation] is shown as a button when permission is missing.
 */
@Composable
fun RadarStatus(state: HomeUiState, inRangeCount: Int, textColor: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    val radar = state.radar
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }
    when {
        radar.jumpTarget == null && !radar.hasLocationPermission -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Blip needs your location to find planes above you.", color = textColor)
            Button(onClick = {
                launcher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
            }) { Text("Allow location") }
        }
        radar.center == null -> Text(
            "Finding your location… If this takes a while, make sure location is on " +
                "(on Windows: Settings → Privacy → Location), or use a Location Jump.",
            color = textColor,
        )
        radar.error != null -> Notice(radar.error)
        radar.lastUpdatedMs == null -> Text("Scanning the sky…", color = textColor)
        else -> {
            val ago = ((state.nowMs - radar.lastUpdatedMs) / 1000).coerceAtLeast(0)
            Text(
                when (inRangeCount) {
                    0 -> "No aircraft within 10 miles right now · updated ${ago}s ago"
                    1 -> "1 aircraft in range · updated ${ago}s ago"
                    else -> "$inRangeCount aircraft in range · updated ${ago}s ago"
                },
                color = textColor,
            )
        }
    }
    if (radar.mockLocation) {
        Notice("Mock location detected. Catching is off until real GPS is used. Location Jump still works.")
    }
}

@Composable
fun Notice(text: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
        Text(text, modifier = Modifier.padding(start = 10.dp))
    }
}

/** Remembers whether the Location Jump sheet is open. */
@Composable
fun rememberJumpSheetState() = remember { mutableStateOf(false) }

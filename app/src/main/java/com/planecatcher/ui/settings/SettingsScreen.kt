package com.planecatcher.ui.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.planecatcher.BuildConfig
import com.planecatcher.core.model.Tier
import com.planecatcher.service.RadarService

@Composable
fun SettingsScreen(onReplayTutorial: () -> Unit, vm: SettingsViewModel = hiltViewModel()) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val s = settings ?: return
    val context = LocalContext.current
    val uri = LocalUriHandler.current

    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    fun askForNotifications() {
        if (Build.VERSION.SDK_INT >= 33) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)

        Section("Alerts")
        ToggleRow("Plane nearby alerts", "Notify me once when a plane enters range", s.alertsEnabled) {
            vm.setAlerts(it)
            if (it) askForNotifications()
        }
        Text("Only alert for", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Tier.entries.forEach { t ->
                FilterChip(
                    selected = s.minAlertTier == t,
                    onClick = { vm.setMinTier(t) },
                    enabled = s.alertsEnabled,
                    label = { Text(if (t == Tier.COMMON) "All planes" else "${t.displayName}+") },
                )
            }
        }
        ToggleRow(
            "Background radar",
            "Keep scanning with an ongoing notification while the app is closed. Uses more battery.",
            s.backgroundRadar,
        ) {
            vm.setBackgroundRadar(it)
            if (it) {
                askForNotifications()
                RadarService.start(context)
            } else {
                RadarService.stop(context)
            }
        }

        Section("Sound and vibration")
        ToggleRow("Sounds", "Radar ping, catch and miss sounds", s.soundsEnabled, vm::setSounds)
        ToggleRow("Vibration", "Buzz on new planes, catches and misses", s.hapticsEnabled, vm::setHaptics)

        Section("Display and outdoors")
        ToggleRow("Sunlight mode", "Bright, extra-high-contrast colours that are easier to read outside", s.sunlightMode, vm::setSunlight)
        ToggleRow("Larger text and buttons", "Makes everything easier to read and tap", s.largeText, vm::setLargeText)
        ToggleRow("Keep screen on", "Stop the screen sleeping while the radar is open", s.keepScreenOn, vm::setKeepScreenOn)
        ToggleRow("Metric units", "Kilometres and metres instead of miles and feet", s.useMetric, vm::setMetric)

        Section("Help")
        Link("Replay the practice catch", onReplayTutorial)

        Section("Safety")
        Text(
            "Never use Blip while driving. You never need to move toward a plane to catch it: " +
                "planes come to you.",
        )

        Section("Data and credits")
        Link("Flight data: adsb.fi (backup: adsb.lol)") { uri.openUri("https://adsb.fi") }
        Link("Map tiles: © OpenStreetMap contributors") { uri.openUri("https://www.openstreetmap.org/copyright") }
        Link("Aircraft photos: planespotters.net and their photographers") { uri.openUri("https://www.planespotters.net") }
        Text(
            "Airline and aircraft names are used only to describe real aircraft. Blip is not affiliated with any airline or manufacturer.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Section("Privacy")
        Text(
            "Your location is used on this phone to find planes near you. It is sent to adsb.fi (or adsb.lol) only as " +
                "coordinates in plane-data requests, rounded to about 10 metres. Your collection is stored only on this " +
                "device and works offline. There are no accounts or ads.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text("Version ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun Section(title: String) {
    HorizontalDivider()
    Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
}

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun Link(text: String, onClick: () -> Unit) {
    androidx.compose.material3.TextButton(onClick = onClick) { Text(text) }
}

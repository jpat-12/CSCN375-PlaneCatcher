package com.planecatcher.ui.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.planecatcher.R

/** Explains the app, then asks for location, then notifications (Android 13+). */
@Composable
fun OnboardingScreen(onFinished: () -> Unit, vm: OnboardingViewModel = hiltViewModel()) {
    // Steps: 0 welcome, 1 practice catch, 2 location, 3 notifications (Android 13+ only).
    var step by rememberSaveable { mutableIntStateOf(0) }
    val needsNotificationStep = Build.VERSION.SDK_INT >= 33

    fun next() {
        val last = if (needsNotificationStep) 3 else 2
        if (step >= last) onFinished() else step++
    }

    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { next() }
    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { next() }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp, Alignment.CenterVertically),
    ) {
        Spacer(Modifier.size(24.dp))
        if (step != 1) {
            Image(
                painter = painterResource(R.drawable.app_logo),
                contentDescription = null,
                modifier = Modifier.size(140.dp).clip(RoundedCornerShape(32.dp)),
            )
        }
        when (step) {
            0 -> {
                Title("Catch the planes above you")
                Body(
                    "Real aircraft flying within 10 miles show up on your radar. Answer a quick question " +
                        "about a plane to add it to your collection. Rarer planes are worth more points.",
                )
                Body(
                    "Once a day you can use a Location Jump to hunt at a busy airport somewhere else in the world.",
                )
                Body("Please never play while driving. Planes come to you; you don't need to chase them.")
                Button(onClick = ::next, modifier = Modifier.fillMaxWidth()) { Text("Get started") }
            }
            1 -> TutorialCatch(onPlay = vm::play, onDone = ::next)
            2 -> {
                Title("Find planes near you")
                Body(
                    "Blip uses your location to look up aircraft overhead. It is only used while " +
                        "the radar is running and is never shared beyond plane-data requests.",
                )
                Button(
                    onClick = {
                        locationLauncher.launch(
                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Allow location") }
                TextButton(onClick = ::next) { Text("Not now") }
            }
            else -> {
                Title("Know when a plane is overhead")
                Body("Get one alert per plane when it comes into range. You can filter alerts by rarity in Settings.")
                Button(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= 33) {
                            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            next()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Allow notifications") }
                TextButton(onClick = ::next) { Text("Not now") }
            }
        }
    }
}

@Composable
private fun Title(text: String) {
    Text(text, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
}

@Composable
private fun Body(text: String) {
    Text(text, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

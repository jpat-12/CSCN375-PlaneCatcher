package com.planecatcher

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.compose.ui.Modifier
import com.planecatcher.data.location.LocationSource
import com.planecatcher.data.prefs.UserPrefs
import com.planecatcher.data.prefs.UserSettings
import com.planecatcher.service.RadarService
import com.planecatcher.ui.nav.AppNav
import com.planecatcher.ui.theme.PlaneCatcherTheme
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(private val prefs: UserPrefs) : ViewModel() {
    val settings: StateFlow<UserSettings?> = prefs.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Plane hex from a tapped "plane nearby" notification, until Home consumes it. */
    val requestedHex = MutableStateFlow<String?>(null)

    fun finishOnboarding() {
        viewModelScope.launch { prefs.setOnboardingDone() }
    }
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var prefs: UserPrefs
    @Inject lateinit var location: LocationSource

    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)

        // Resume the background radar if the user turned it on earlier.
        lifecycleScope.launch {
            if (prefs.settings.first().backgroundRadar && location.hasPermission()) RadarService.start(this@MainActivity)
        }

        setContent {
            PlaneCatcherTheme {
                val settings by vm.settings.collectAsStateWithLifecycle()
                val requestedHex by vm.requestedHex.collectAsStateWithLifecycle()
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                    settings?.let { s ->
                        AppNav(
                            onboardingDone = s.onboardingDone,
                            onOnboardingFinished = vm::finishOnboarding,
                            requestedHex = requestedHex,
                            onRequestHandled = { vm.requestedHex.value = null },
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        intent?.getStringExtra(EXTRA_PLANE_HEX)?.let { vm.requestedHex.value = it }
    }

    companion object {
        const val EXTRA_PLANE_HEX = "plane_hex"
    }
}

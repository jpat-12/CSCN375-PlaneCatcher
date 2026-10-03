package com.planecatcher.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.planecatcher.core.model.Tier
import com.planecatcher.data.prefs.UserPrefs
import com.planecatcher.data.prefs.UserSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(private val prefs: UserPrefs) : ViewModel() {
    val settings: StateFlow<UserSettings?> = prefs.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setAlerts(v: Boolean) {
        viewModelScope.launch { prefs.setAlertsEnabled(v) }
    }
    fun setMinTier(t: Tier) {
        viewModelScope.launch { prefs.setMinAlertTier(t) }
    }
    fun setMetric(v: Boolean) {
        viewModelScope.launch { prefs.setUseMetric(v) }
    }
    fun setBackgroundRadar(v: Boolean) {
        viewModelScope.launch { prefs.setBackgroundRadar(v) }
    }
    fun setSounds(v: Boolean) {
        viewModelScope.launch { prefs.setSounds(v) }
    }
    fun setHaptics(v: Boolean) {
        viewModelScope.launch { prefs.setHaptics(v) }
    }
    fun setSunlight(v: Boolean) {
        viewModelScope.launch { prefs.setSunlightMode(v) }
    }
    fun setLargeText(v: Boolean) {
        viewModelScope.launch { prefs.setLargeText(v) }
    }
    fun setKeepScreenOn(v: Boolean) {
        viewModelScope.launch { prefs.setKeepScreenOn(v) }
    }
}

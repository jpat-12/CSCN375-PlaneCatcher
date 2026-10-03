package com.planecatcher.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.planecatcher.core.model.GeoPoint
import com.planecatcher.core.model.Tier
import com.planecatcher.core.rules.JumpState
import com.planecatcher.core.rules.JumpTarget
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class UserSettings(
    val onboardingDone: Boolean = false,
    val alertsEnabled: Boolean = true,
    val minAlertTier: Tier = Tier.COMMON,
    val useMetric: Boolean = false,
    /** Keep the radar running in a foreground service while the app is closed. */
    val backgroundRadar: Boolean = false,
    val soundsEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    /** Light, extra-high-contrast colours for bright sunlight. */
    val sunlightMode: Boolean = false,
    /** Bigger text (and so bigger buttons) for use outdoors or at arm's length. */
    val largeText: Boolean = false,
    /** Keep the screen on while the radar is open. */
    val keepScreenOn: Boolean = true,
)

private val Context.dataStore by preferencesDataStore(name = "user_state")

@Singleton
class UserPrefs @Inject constructor(@ApplicationContext private val context: Context) {
    private object Keys {
        val onboardingDone = booleanPreferencesKey("onboarding_done")
        val alertsEnabled = booleanPreferencesKey("alerts_enabled")
        val minAlertTier = stringPreferencesKey("min_alert_tier")
        val useMetric = booleanPreferencesKey("use_metric")
        val backgroundRadar = booleanPreferencesKey("background_radar")
        val sounds = booleanPreferencesKey("sounds")
        val haptics = booleanPreferencesKey("haptics")
        val sunlight = booleanPreferencesKey("sunlight_mode")
        val largeText = booleanPreferencesKey("large_text")
        val keepScreenOn = booleanPreferencesKey("keep_screen_on")

        val jumpCode = stringPreferencesKey("jump_code")
        val jumpName = stringPreferencesKey("jump_name")
        val jumpLat = doublePreferencesKey("jump_lat")
        val jumpLon = doublePreferencesKey("jump_lon")
        val jumpActiveUntil = longPreferencesKey("jump_active_until")
        val jumpEndedAt = longPreferencesKey("jump_ended_at")
    }

    private val data: Flow<Preferences> = context.dataStore.data

    val settings: Flow<UserSettings> = data.map { p ->
        UserSettings(
            onboardingDone = p[Keys.onboardingDone] ?: false,
            alertsEnabled = p[Keys.alertsEnabled] ?: true,
            minAlertTier = Tier.fromName(p[Keys.minAlertTier]) ?: Tier.COMMON,
            useMetric = p[Keys.useMetric] ?: false,
            backgroundRadar = p[Keys.backgroundRadar] ?: false,
            soundsEnabled = p[Keys.sounds] ?: true,
            hapticsEnabled = p[Keys.haptics] ?: true,
            sunlightMode = p[Keys.sunlight] ?: false,
            largeText = p[Keys.largeText] ?: false,
            keepScreenOn = p[Keys.keepScreenOn] ?: true,
        )
    }.distinctUntilChanged()

    val jumpState: Flow<JumpState> = data.map { p ->
        val code = p[Keys.jumpCode]
        val lat = p[Keys.jumpLat]
        val lon = p[Keys.jumpLon]
        val until = p[Keys.jumpActiveUntil]
        val target = if (code != null && lat != null && lon != null && until != null) {
            JumpTarget(code, p[Keys.jumpName] ?: code, GeoPoint(lat, lon))
        } else {
            null
        }
        JumpState(
            target = target,
            activeUntilMs = until.takeIf { target != null },
            endedAtMs = p[Keys.jumpEndedAt],
        )
    }.distinctUntilChanged()

    suspend fun setOnboardingDone() = context.dataStore.edit { it[Keys.onboardingDone] = true }
    suspend fun setAlertsEnabled(v: Boolean) = context.dataStore.edit { it[Keys.alertsEnabled] = v }
    suspend fun setMinAlertTier(t: Tier) = context.dataStore.edit { it[Keys.minAlertTier] = t.name }
    suspend fun setUseMetric(v: Boolean) = context.dataStore.edit { it[Keys.useMetric] = v }
    suspend fun setBackgroundRadar(v: Boolean) = context.dataStore.edit { it[Keys.backgroundRadar] = v }
    suspend fun setSounds(v: Boolean) = context.dataStore.edit { it[Keys.sounds] = v }
    suspend fun setHaptics(v: Boolean) = context.dataStore.edit { it[Keys.haptics] = v }
    suspend fun setSunlightMode(v: Boolean) = context.dataStore.edit { it[Keys.sunlight] = v }
    suspend fun setLargeText(v: Boolean) = context.dataStore.edit { it[Keys.largeText] = v }
    suspend fun setKeepScreenOn(v: Boolean) = context.dataStore.edit { it[Keys.keepScreenOn] = v }

    suspend fun setJumpState(s: JumpState) = context.dataStore.edit { p ->
        val t = s.target
        if (t != null && s.activeUntilMs != null) {
            p[Keys.jumpCode] = t.code
            p[Keys.jumpName] = t.name
            p[Keys.jumpLat] = t.point.lat
            p[Keys.jumpLon] = t.point.lon
            p[Keys.jumpActiveUntil] = s.activeUntilMs!!
        } else {
            p -= Keys.jumpCode
            p -= Keys.jumpName
            p -= Keys.jumpLat
            p -= Keys.jumpLon
            p -= Keys.jumpActiveUntil
        }
        val ended = s.endedAtMs
        if (ended != null) p[Keys.jumpEndedAt] = ended else p -= Keys.jumpEndedAt
    }
}

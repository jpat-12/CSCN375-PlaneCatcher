package com.planecatcher.feedback

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.planecatcher.R
import com.planecatcher.data.prefs.UserPrefs
import com.planecatcher.data.prefs.UserSettings
import com.planecatcher.di.ApplicationScope
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

enum class Sfx { PING, CATCH, MISS, REWARD }

/** Sound effects and vibration, each respecting its own setting. */
@Singleton
class Feedback @Inject constructor(
    @ApplicationContext private val context: Context,
    prefs: UserPrefs,
    @ApplicationScope scope: CoroutineScope,
) {
    private val settings = prefs.settings.stateIn(scope, SharingStarted.Eagerly, UserSettings())

    private val pool: SoundPool = SoundPool.Builder()
        .setMaxStreams(3)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()

    private val ids: Map<Sfx, Int> = mapOf(
        Sfx.PING to pool.load(context, R.raw.radar_ping, 1),
        Sfx.CATCH to pool.load(context, R.raw.catch_success, 1),
        Sfx.MISS to pool.load(context, R.raw.catch_miss, 1),
        Sfx.REWARD to pool.load(context, R.raw.reward, 1),
    )

    private val vibrator: Vibrator? =
        if (Build.VERSION.SDK_INT >= 31) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        }

    fun play(sfx: Sfx) {
        if (settings.value.soundsEnabled) ids[sfx]?.let { pool.play(it, 1f, 1f, 1, 0, 1f) }
        if (settings.value.hapticsEnabled) vibrate(sfx)
    }

    private fun vibrate(sfx: Sfx) {
        val v = vibrator?.takeIf { it.hasVibrator() } ?: return
        val effect = when (sfx) {
            Sfx.PING -> VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE)
            Sfx.CATCH -> VibrationEffect.createWaveform(longArrayOf(0, 60, 60, 60, 60, 120), -1)
            Sfx.MISS -> VibrationEffect.createWaveform(longArrayOf(0, 180, 80, 180), -1)
            Sfx.REWARD -> VibrationEffect.createWaveform(longArrayOf(0, 30, 40, 30, 40, 30), -1)
        }
        try {
            v.vibrate(effect)
        } catch (_: SecurityException) {
            // VIBRATE permission missing; stay silent.
        }
    }
}

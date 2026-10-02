package com.planecatcher.data.time

import android.content.Context
import android.os.SystemClock
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A clock the user cannot wind back by changing the phone's time.
 *
 * Time is kept as an anchor (trusted epoch ms at a known [SystemClock.elapsedRealtime])
 * and advanced with elapsedRealtime, which ignores the wall clock. The anchor is
 * refreshed from server `Date` headers whenever the API is reached, and saved so it
 * survives app restarts within the same boot. After a reboot it falls back to the
 * phone clock, but never earlier than the last trusted time saved.
 */
@Singleton
class TrustedClock @Inject constructor(@ApplicationContext context: Context) {
    private val prefs = context.getSharedPreferences("trusted_clock", Context.MODE_PRIVATE)
    private val bootCount = Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, -1)

    @Volatile private var anchorEpochMs: Long
    @Volatile private var anchorElapsedMs: Long

    init {
        val elapsed = SystemClock.elapsedRealtime()
        val savedBoot = prefs.getInt(KEY_BOOT, Int.MIN_VALUE)
        val savedEpoch = prefs.getLong(KEY_EPOCH, 0L)
        val savedElapsed = prefs.getLong(KEY_ELAPSED, 0L)
        if (bootCount != -1 && savedBoot == bootCount && savedEpoch > 0 && elapsed >= savedElapsed) {
            anchorEpochMs = savedEpoch
            anchorElapsedMs = savedElapsed
        } else {
            anchorEpochMs = maxOf(System.currentTimeMillis(), savedEpoch)
            anchorElapsedMs = elapsed
            save()
        }
    }

    fun now(): Long = anchorEpochMs + (SystemClock.elapsedRealtime() - anchorElapsedMs)

    /** Called with the time from a server response. */
    fun onServerTime(epochMs: Long) {
        anchorEpochMs = epochMs
        anchorElapsedMs = SystemClock.elapsedRealtime()
        save()
    }

    private fun save() {
        prefs.edit()
            .putInt(KEY_BOOT, bootCount)
            .putLong(KEY_EPOCH, anchorEpochMs)
            .putLong(KEY_ELAPSED, anchorElapsedMs)
            .apply()
    }

    private companion object {
        const val KEY_BOOT = "boot"
        const val KEY_EPOCH = "epoch"
        const val KEY_ELAPSED = "elapsed"
    }
}

package com.planecatcher.core.progress

import com.planecatcher.core.model.Tier

/** The facts about one catch that progress (sets, challenges, streaks, levels) is computed from. */
data class CatchRecord(
    val hex: String,
    val typeCode: String?,
    val tier: Tier,
    val caughtAt: Long,
    val callsign: String? = null,
    val altitudeFt: Int? = null,
    val distanceMiles: Double = 0.0,
)

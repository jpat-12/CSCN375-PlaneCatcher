package com.planecatcher.core.rules

import kotlin.math.ceil
import com.planecatcher.core.geo.Geo

/** The settled game rules from the PlaneCatcher outline, in one place. */
object GameRules {
    /** A plane can be caught when it is within this many statute miles. */
    const val CATCH_RANGE_MILES = 10.0

    /**
     * The ADS-B API takes a radius in nautical miles. 10 mi is about 8.69 nm,
     * so we ask for a little more and filter precisely on the device.
     */
    val QUERY_RADIUS_NM: Int = ceil(Geo.milesToNauticalMiles(CATCH_RANGE_MILES)).toInt()

    const val QUIZ_OPTION_COUNT = 4

    const val MINUTE_MS = 60_000L
    const val HOUR_MS = 60 * MINUTE_MS

    /** A wrong answer locks that plane's quiz for this long. */
    const val QUIZ_LOCK_MS = 1 * HOUR_MS

    /** A Location Jump lasts at most this long. */
    const val JUMP_DURATION_MS = 3 * HOUR_MS

    /** After a jump ends, the next one is available after this long. */
    const val JUMP_COOLDOWN_MS = 24 * HOUR_MS

    /** Normal poll interval while the radar is active. */
    const val POLL_INTERVAL_MS = 30_000L

    /** Back-off after an error or HTTP 429. */
    const val ERROR_BACKOFF_MS = 60_000L

    /** A dismissed pop-up does not reappear for the same plane for this long. */
    const val DISMISS_SNOOZE_MS = 30 * MINUTE_MS
}

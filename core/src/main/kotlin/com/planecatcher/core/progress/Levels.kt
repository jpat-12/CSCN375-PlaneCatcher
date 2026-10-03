package com.planecatcher.core.progress

data class LevelInfo(
    val level: Int,
    /** XP earned inside the current level. */
    val xpIntoLevel: Int,
    /** XP needed to go from this level to the next. */
    val xpForNext: Int,
) {
    val title: String get() = Levels.titleFor(level)
    val fraction: Float get() = if (xpForNext == 0) 0f else xpIntoLevel.toFloat() / xpForNext
}

/** Points double as XP. Level L to L+1 costs 10 x L XP, so early levels come quickly. */
object Levels {
    fun xpToAdvance(level: Int): Int = 10 * level

    fun infoFor(points: Int): LevelInfo {
        var level = 1
        var remaining = points.coerceAtLeast(0)
        while (remaining >= xpToAdvance(level)) {
            remaining -= xpToAdvance(level)
            level++
        }
        return LevelInfo(level, remaining, xpToAdvance(level))
    }

    fun titleFor(level: Int): String = when {
        level < 3 -> "Spotter"
        level < 6 -> "Plane Watcher"
        level < 10 -> "Sky Scout"
        level < 15 -> "Radar Ace"
        else -> "Sky Legend"
    }
}

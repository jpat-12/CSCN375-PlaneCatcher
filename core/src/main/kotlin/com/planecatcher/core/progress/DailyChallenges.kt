package com.planecatcher.core.progress

import com.planecatcher.core.catalog.Airlines
import com.planecatcher.core.model.Tier
import java.time.LocalDate
import kotlin.random.Random

enum class ChallengeKind { CATCH_COUNT, CATCH_TIER, NEW_TYPE, AIRLINES, HIGH_FLYER, LOW_FLYER, CLOSE_PASS }

data class Challenge(
    val kind: ChallengeKind,
    val title: String,
    val goal: Int,
    val reward: Int,
    val tier: Tier? = null,
)

data class ChallengeProgress(val challenge: Challenge, val progress: Int) {
    val complete: Boolean get() = progress >= challenge.goal
}

/**
 * Three challenges per day, chosen deterministically from the date so every
 * player sees the same ones and nothing needs storing. Progress is computed
 * from that day's catches.
 */
object DailyChallenges {
    const val PER_DAY = 3

    fun forDate(date: LocalDate): List<Challenge> {
        val random = Random(date.toEpochDay() * 7919 + 17)
        val kinds = ChallengeKind.entries.shuffled(random).take(PER_DAY)
        return kinds.map { kind ->
            when (kind) {
                ChallengeKind.CATCH_COUNT -> {
                    val n = 2 + random.nextInt(3)
                    Challenge(kind, "Catch $n planes", n, 5 * n)
                }
                ChallengeKind.CATCH_TIER -> if (random.nextInt(4) == 0) {
                    Challenge(kind, "Catch an Epic or better plane", 1, 30, Tier.EPIC)
                } else {
                    Challenge(kind, "Catch a Rare or better plane", 1, 15, Tier.RARE)
                }
                ChallengeKind.NEW_TYPE -> Challenge(kind, "Catch a type you've never caught before", 1, 15)
                ChallengeKind.AIRLINES -> Challenge(kind, "Catch planes from 2 different airlines", 2, 10)
                ChallengeKind.HIGH_FLYER -> Challenge(kind, "Catch a plane flying above 30,000 ft", 1, 10)
                ChallengeKind.LOW_FLYER -> Challenge(kind, "Catch a plane flying below 10,000 ft", 1, 10)
                ChallengeKind.CLOSE_PASS -> Challenge(kind, "Catch a plane within 3 miles", 1, 10)
            }
        }
    }

    /**
     * @param dayCatches catches made on the challenge's day.
     * @param typesBefore type codes caught before that day.
     */
    fun evaluate(challenge: Challenge, dayCatches: List<CatchRecord>, typesBefore: Set<String>): ChallengeProgress {
        val progress = when (challenge.kind) {
            ChallengeKind.CATCH_COUNT -> dayCatches.size
            ChallengeKind.CATCH_TIER -> dayCatches.count { it.tier.atLeast(challenge.tier ?: Tier.RARE) }
            ChallengeKind.NEW_TYPE -> dayCatches.mapNotNull { it.typeCode }.distinct().count { it !in typesBefore }
            ChallengeKind.AIRLINES -> dayCatches.mapNotNull { Airlines.fromCallsign(it.callsign) }.distinct().size
            ChallengeKind.HIGH_FLYER -> dayCatches.count { (it.altitudeFt ?: 0) > 30_000 }
            ChallengeKind.LOW_FLYER -> dayCatches.count { it.altitudeFt != null && it.altitudeFt in 1 until 10_000 }
            ChallengeKind.CLOSE_PASS -> dayCatches.count { it.distanceMiles <= 3.0 }
        }
        return ChallengeProgress(challenge, progress.coerceAtMost(challenge.goal))
    }
}

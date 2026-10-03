package com.planecatcher.core.progress

import com.planecatcher.core.catalog.AircraftCatalog
import com.planecatcher.core.catalog.Airlines
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class AirlineBadge(val airline: String, val count: Int)

/** One entry in the collection book: an aircraft model and whether it has been caught. */
data class BookEntry(val name: String, val codes: Set<String>, val category: String, val caught: Boolean)

data class ProgressSummary(
    val catchPoints: Int,
    val setBonus: Int,
    val challengeBonus: Int,
    val level: LevelInfo,
    val streakDays: Int,
    val bestStreakDays: Int,
    val caughtToday: Boolean,
    val sets: List<SetProgress>,
    val today: List<ChallengeProgress>,
    val book: List<BookEntry>,
    val airlines: List<AirlineBadge>,
) {
    val totalPoints: Int get() = catchPoints + setBonus + challengeBonus
    val typesCaught: Int get() = book.count { it.caught }
}

/** Turns the list of catches into everything on the progress screens. Pure and deterministic. */
object Progress {

    fun summarize(catches: List<CatchRecord>, today: LocalDate, zone: ZoneId): ProgressSummary {
        val byDay: Map<LocalDate, List<CatchRecord>> = catches.groupBy { dateOf(it.caughtAt, zone) }

        // Challenge rewards for every day the player caught something.
        var challengeBonus = 0
        val typesSoFar = mutableSetOf<String>()
        var todayProgress: List<ChallengeProgress>? = null
        for (day in byDay.keys.sorted()) {
            val dayCatches = byDay.getValue(day)
            val results = DailyChallenges.forDate(day).map { DailyChallenges.evaluate(it, dayCatches, typesSoFar) }
            challengeBonus += results.filter { it.complete }.sumOf { it.challenge.reward }
            if (day == today) todayProgress = results
            typesSoFar += dayCatches.mapNotNull { it.typeCode?.uppercase() }
        }
        val todayResults = todayProgress ?: DailyChallenges.forDate(today).map {
            DailyChallenges.evaluate(it, emptyList(), typesSoFar)
        }

        val caughtTypes = catches.mapNotNull { it.typeCode?.uppercase() }.toSet()
        val sets = CollectionSets.progress(caughtTypes)
        val setBonus = sets.filter { it.complete }.sumOf { it.set.bonus }
        val catchPoints = catches.sumOf { it.tier.points }
        val (streak, best) = streaks(byDay.keys, today)

        return ProgressSummary(
            catchPoints = catchPoints,
            setBonus = setBonus,
            challengeBonus = challengeBonus,
            level = Levels.infoFor(catchPoints + setBonus + challengeBonus),
            streakDays = streak,
            bestStreakDays = best,
            caughtToday = today in byDay,
            sets = sets,
            today = todayResults,
            book = book(caughtTypes),
            airlines = catches.mapNotNull { Airlines.fromCallsign(it.callsign) }
                .groupingBy { it }.eachCount()
                .map { (name, n) -> AirlineBadge(name, n) }
                .sortedWith(compareByDescending<AirlineBadge> { it.count }.thenBy { it.airline }),
        )
    }

    fun dateOf(epochMs: Long, zone: ZoneId): LocalDate = Instant.ofEpochMilli(epochMs).atZone(zone).toLocalDate()

    /**
     * Current streak: consecutive days with a catch, ending today, or yesterday if
     * nothing has been caught yet today (the streak is still alive until midnight).
     */
    fun streaks(days: Set<LocalDate>, today: LocalDate): Pair<Int, Int> {
        var current = 0
        var cursor = if (today in days) today else today.minusDays(1)
        while (cursor in days) {
            current++
            cursor = cursor.minusDays(1)
        }
        var best = 0
        var run = 0
        var previous: LocalDate? = null
        for (d in days.sorted()) {
            run = if (previous != null && d == previous.plusDays(1)) run + 1 else 1
            best = maxOf(best, run)
            previous = d
        }
        return current to best
    }

    /** Every catalog model, one entry per distinct name (e.g. E75L and E75S are both "E175"). */
    fun book(caughtTypes: Set<String>): List<BookEntry> =
        AircraftCatalog.all
            .groupBy { it.fullName }
            .map { (name, infos) ->
                val codes = infos.map { it.code }.toSet()
                BookEntry(name, codes, infos.first().category.label, codes.any { it in caughtTypes })
            }
}

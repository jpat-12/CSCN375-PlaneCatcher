package com.planecatcher.domain

import com.planecatcher.core.progress.CatchRecord
import com.planecatcher.core.progress.Progress
import com.planecatcher.core.progress.ProgressSummary
import com.planecatcher.data.local.CaughtPlaneDao
import com.planecatcher.data.local.CaughtPlaneEntity
import com.planecatcher.data.time.TrustedClock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Levels, sets, daily challenges and streaks, all derived from the collection.
 * Nothing extra is stored: the same catches always give the same progress.
 */
@Singleton
class ProgressRepository @Inject constructor(
    private val caughtDao: CaughtPlaneDao,
    private val clock: TrustedClock,
) {
    private fun today(): LocalDate = Progress.dateOf(clock.now(), ZoneId.systemDefault())

    /** Re-evaluates when the collection changes or the day rolls over. */
    val summary: Flow<ProgressSummary> = combine(
        caughtDao.observeAll(),
        ticker(60_000L).map { today() }.distinctUntilChanged(),
    ) { catches, day -> summarize(catches, day) }

    suspend fun current(): ProgressSummary = summarize(caughtDao.observeAll().first(), today())

    private fun summarize(catches: List<CaughtPlaneEntity>, day: LocalDate) =
        Progress.summarize(catches.map { it.toRecord() }, day, ZoneId.systemDefault())

    private fun CaughtPlaneEntity.toRecord() = CatchRecord(
        hex = hex,
        typeCode = typeCode,
        tier = tier,
        caughtAt = caughtAt,
        callsign = callsign,
        altitudeFt = altitudeFt,
        distanceMiles = distanceMiles,
    )
}

/** What a catch unlocked, for the celebration on the quiz result screen. */
data class Rewards(
    val levelUpTo: Int?,
    val challengesDone: List<Pair<String, Int>>,
    val setsDone: List<Pair<String, Int>>,
) {
    val any: Boolean get() = levelUpTo != null || challengesDone.isNotEmpty() || setsDone.isNotEmpty()

    companion object {
        fun between(before: ProgressSummary, after: ProgressSummary): Rewards {
            val doneBefore = before.today.filter { it.complete }.map { it.challenge.title }.toSet()
            val setsBefore = before.sets.filter { it.complete }.map { it.set.id }.toSet()
            return Rewards(
                levelUpTo = after.level.level.takeIf { it > before.level.level },
                challengesDone = after.today
                    .filter { it.complete && it.challenge.title !in doneBefore }
                    .map { it.challenge.title to it.challenge.reward },
                setsDone = after.sets
                    .filter { it.complete && it.set.id !in setsBefore }
                    .map { it.set.name to it.set.bonus },
            )
        }
    }
}

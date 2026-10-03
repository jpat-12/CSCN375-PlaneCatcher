package com.planecatcher.core

import com.planecatcher.core.model.Tier
import com.planecatcher.core.progress.CatchRecord
import com.planecatcher.core.progress.ChallengeKind
import com.planecatcher.core.progress.CollectionSets
import com.planecatcher.core.progress.DailyChallenges
import com.planecatcher.core.progress.Levels
import com.planecatcher.core.progress.Progress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

class ProgressTest {
    private val zone: ZoneId = ZoneOffset.UTC
    private val today = LocalDate.of(2026, 10, 3)

    private fun at(date: LocalDate, hour: Int = 12) =
        date.atTime(hour, 0).toInstant(ZoneOffset.UTC).toEpochMilli()

    private fun catch(hex: String, type: String?, tier: Tier = Tier.COMMON, date: LocalDate = today, callsign: String? = null) =
        CatchRecord(hex, type, tier, at(date), callsign, altitudeFt = 20_000, distanceMiles = 5.0)

    @Test
    fun levels_progress_and_titles() {
        assertEquals(1, Levels.infoFor(0).level)
        assertEquals(1, Levels.infoFor(9).level)
        assertEquals(2, Levels.infoFor(10).level)
        // 10 + 20 = 30 points reaches level 3.
        val l3 = Levels.infoFor(35)
        assertEquals(3, l3.level)
        assertEquals(5, l3.xpIntoLevel)
        assertEquals(30, l3.xpForNext)
        assertEquals("Plane Watcher", l3.title)
    }

    @Test
    fun set_completes_when_every_slot_filled() {
        val partial = CollectionSets.progress(setOf("B788", "B789")).first { it.set.id == "dreamliner" }
        assertEquals(2, partial.have)
        assertFalse(partial.complete)
        val full = CollectionSets.progress(setOf("B788", "B789", "B78X")).first { it.set.id == "dreamliner" }
        assertTrue(full.complete)
    }

    @Test
    fun any_code_fills_a_grouped_slot() {
        val regional = CollectionSets.progress(setOf("CRJ9", "E75S", "AT76", "DH8D")).first { it.set.id == "regional" }
        assertTrue(regional.complete)
    }

    @Test
    fun summary_adds_set_bonus_to_points() {
        val catches = listOf(catch("1", "B788", Tier.EPIC), catch("2", "B789", Tier.EPIC), catch("3", "B78X", Tier.EPIC))
        val s = Progress.summarize(catches, today, zone)
        assertEquals(60, s.catchPoints)
        assertEquals(40, s.setBonus)
        assertEquals(s.catchPoints + s.setBonus + s.challengeBonus, s.totalPoints)
        assertEquals(3, s.typesCaught)
    }

    @Test
    fun streak_counts_consecutive_days_and_survives_until_midnight() {
        val days = setOf(today.minusDays(1), today.minusDays(2), today.minusDays(5))
        assertEquals(2 to 2, Progress.streaks(days, today))
        assertEquals(3 to 3, Progress.streaks(days + today, today))
        assertEquals(0 to 1, Progress.streaks(setOf(today.minusDays(3)), today))
        assertEquals(0 to 0, Progress.streaks(emptySet(), today))
    }

    @Test
    fun daily_challenges_are_stable_and_distinct() {
        repeat(60) { i ->
            val d = today.plusDays(i.toLong())
            val a = DailyChallenges.forDate(d)
            assertEquals(a, DailyChallenges.forDate(d))
            assertEquals(3, a.map { it.kind }.distinct().size)
        }
    }

    @Test
    fun challenge_evaluation() {
        val dayCatches = listOf(
            catch("1", "B738", callsign = "DAL1"),
            catch("2", "A320", Tier.COMMON, callsign = "UAL2"),
            catch("3", "CRJ9", Tier.RARE, callsign = "DAL3"),
        )
        fun eval(kind: ChallengeKind) = DailyChallenges.forDate(today).let { _ ->
            val c = when (kind) {
                ChallengeKind.AIRLINES -> com.planecatcher.core.progress.Challenge(kind, "", 2, 10)
                ChallengeKind.NEW_TYPE -> com.planecatcher.core.progress.Challenge(kind, "", 1, 15)
                else -> com.planecatcher.core.progress.Challenge(kind, "", 1, 15, Tier.RARE)
            }
            DailyChallenges.evaluate(c, dayCatches, typesBefore = setOf("B738", "A320"))
        }
        assertTrue(eval(ChallengeKind.AIRLINES).complete)
        assertTrue(eval(ChallengeKind.NEW_TYPE).complete) // CRJ9 is new
        assertTrue(eval(ChallengeKind.CATCH_TIER).complete)
        assertFalse(eval(ChallengeKind.HIGH_FLYER).complete)
    }

    @Test
    fun book_merges_duplicate_models_and_airlines_are_counted() {
        val s = Progress.summarize(listOf(catch("1", "E75S", callsign = "SKW1"), catch("2", "E75L", callsign = "SKW2")), today, zone)
        val e175 = s.book.single { it.name == "Embraer E175" }
        assertTrue(e175.caught)
        assertEquals(1, s.typesCaught)
        assertEquals("SkyWest Airlines", s.airlines.single().airline)
        assertEquals(2, s.airlines.single().count)
    }
}

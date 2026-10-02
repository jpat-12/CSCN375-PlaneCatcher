package com.planecatcher.core

import com.planecatcher.core.catalog.Airlines
import com.planecatcher.core.catalog.RegistrationCountries
import com.planecatcher.core.model.Aircraft
import com.planecatcher.core.quiz.QuizGenerator
import com.planecatcher.core.rules.QuizLock
import com.planecatcher.core.rules.GameRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class QuizGeneratorTest {
    private fun plane(type: String?, callsign: String? = null, reg: String? = null, alt: Int? = 35_000) =
        Aircraft("abc123", callsign, reg, type, 40.0, -74.0, alt)

    @Test
    fun always_four_distinct_options_with_valid_answer() {
        val samples = listOf(
            plane("B738", "DAL123", "N123DL"),
            plane("A388", "UAE201", "A6-EDA"),
            plane("C172", null, "N12345", 2_500),
            plane("A225"),
            plane("ZZZZ", "N123AB", null),
            plane(null, null, null, null),
        )
        repeat(200) { seed ->
            val gen = QuizGenerator(Random(seed))
            samples.forEach { p ->
                val q = gen.generate(p)
                assertEquals(4, q.options.size)
                assertEquals(4, q.options.toSet().size)
                assertTrue(q.correctIndex in 0..3)
            }
        }
    }

    @Test
    fun retry_avoids_failed_kind() {
        val p = plane("B738", "DAL123", "N123DL")
        repeat(200) { seed ->
            val gen = QuizGenerator(Random(seed))
            val first = gen.generate(p)
            val retry = gen.generate(p, avoidKind = first.kind)
            assertNotEquals(first.kind, retry.kind)
        }
    }

    @Test
    fun airline_question_has_correct_answer() {
        val p = plane(null, "UAL900", null, null)
        val q = QuizGenerator(Random(1)).generate(p)
        assertEquals(QuizGenerator.KIND_AIRLINE, q.kind)
        assertEquals("United Airlines", q.correctAnswer)
    }

    @Test
    fun unknown_plane_with_altitude_gets_altitude_question() {
        val q = QuizGenerator(Random(1)).generate(plane(null, alt = 12_000))
        assertEquals(QuizGenerator.KIND_ALTITUDE, q.kind)
        assertEquals("5,000 to 15,000 ft", q.correctAnswer)
    }

    @Test
    fun airline_lookup_rules() {
        assertEquals("Delta Air Lines", Airlines.fromCallsign("DAL1234"))
        assertNull(Airlines.fromCallsign("N123AB"))
        assertNull(Airlines.fromCallsign("XYZ12"))
    }

    @Test
    fun registration_country_lookup() {
        assertEquals("United States", RegistrationCountries.fromRegistration("N767AX"))
        assertEquals("Australia", RegistrationCountries.fromRegistration("VH-OQA"))
        assertEquals("Japan", RegistrationCountries.fromRegistration("JA8089"))
        assertEquals("United Kingdom", RegistrationCountries.fromRegistration("g-xlea"))
        assertNull(RegistrationCountries.fromRegistration("NOPE"))
    }

    @Test
    fun quiz_lock_lasts_one_hour() {
        val lock = QuizLock.afterWrongAnswer("abc123", 0L, "model")
        assertTrue(lock.isLocked(GameRules.HOUR_MS - 1))
        assertFalse(lock.isLocked(GameRules.HOUR_MS))
        assertEquals(GameRules.HOUR_MS, lock.remainingMs(0))
    }
}

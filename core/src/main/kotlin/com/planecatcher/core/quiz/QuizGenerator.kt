package com.planecatcher.core.quiz

import com.planecatcher.core.catalog.AircraftCatalog
import com.planecatcher.core.catalog.Airlines
import com.planecatcher.core.catalog.RegistrationCountries
import com.planecatcher.core.catalog.TypeInfo
import com.planecatcher.core.model.Aircraft
import kotlin.random.Random

/**
 * Builds a 4-option question about a specific aircraft.
 *
 * Curated questions for the type are preferred. Otherwise a question is generated from the
 * aircraft's data: model, manufacturer, engine count, airline (callsign prefix), registration
 * country, typical seats, or current altitude. Distractors come from similar aircraft.
 *
 * Pass the failed question's kind in [avoidKind] so a retry gets a different question.
 */
class QuizGenerator(private val random: Random = Random.Default) {

    fun generate(aircraft: Aircraft, avoidKind: String? = null): QuizQuestion {
        val info = AircraftCatalog.lookup(aircraft.typeCode)

        val curated = CuratedQuestions.forType(aircraft.typeCode)
            .filter { kindOf(it) != avoidKind }
        if (curated.isNotEmpty() && random.nextInt(100) < CURATED_PERCENT) {
            return fromCurated(curated.random(random))
        }

        val generated = listOfNotNull(
            info?.let { modelQuestion(it) },
            info?.let { manufacturerQuestion(it) },
            info?.let { enginesQuestion(it) },
            info?.let { seatsQuestion(it) },
            airlineQuestion(aircraft),
            countryQuestion(aircraft),
        ).filter { it.kind != avoidKind }

        if (generated.isNotEmpty()) return generated.random(random)
        if (curated.isNotEmpty()) return fromCurated(curated.random(random))
        return altitudeQuestion(aircraft)?.takeIf { it.kind != avoidKind } ?: generalQuestion(avoidKind)
    }

    // ---- Generated question families ----

    private fun modelQuestion(info: TypeInfo): QuizQuestion? {
        val distractors = AircraftCatalog.similarTo(info).map { it.fullName }.distinct()
            .ifEmpty { return null }
        return build(KIND_MODEL, "What type of aircraft is this?", info.fullName, distractors, 2)
    }

    private fun manufacturerQuestion(info: TypeInfo): QuizQuestion? {
        val distractors = AircraftCatalog.all.map { it.manufacturer }.distinct() - info.manufacturer
        return build(KIND_MANUFACTURER, "Who built this ${info.category.label.lowercase()}?", info.manufacturer, distractors, 1)
    }

    private fun enginesQuestion(info: TypeInfo): QuizQuestion {
        val options = (1..4).toMutableList()
        if (info.engines !in options) options[options.lastIndex] = info.engines
        val shuffled = options.shuffled(random)
        return QuizQuestion(
            kind = KIND_ENGINES,
            prompt = "How many engines does the ${info.fullName} have?",
            options = shuffled.map { if (it == 1) "1 engine" else "$it engines" },
            correctIndex = shuffled.indexOf(info.engines),
            difficulty = 1,
        )
    }

    private fun seatsQuestion(info: TypeInfo): QuizQuestion? {
        if (info.typicalSeats <= 0) return null
        val bands = SEAT_BANDS
        val correct = bands.indexOfFirst { info.typicalSeats in it.first }
        if (correct < 0) return null
        // Pick 4 consecutive bands that include the correct one.
        val start = (correct - random.nextInt(4)).coerceIn(0, bands.size - 4)
        val window = bands.subList(start, start + 4)
        return QuizQuestion(
            kind = KIND_SEATS,
            prompt = "About how many passengers does a typical ${info.fullName} seat?",
            options = window.map { it.second },
            correctIndex = correct - start,
            difficulty = 2,
        )
    }

    private fun airlineQuestion(aircraft: Aircraft): QuizQuestion? {
        val airline = Airlines.fromCallsign(aircraft.callsign) ?: return null
        val distractors = Airlines.byIcao.values.distinct() - airline
        val callsign = aircraft.callsign!!.trim().uppercase()
        return build(KIND_AIRLINE, "Which airline flies callsign $callsign?", airline, distractors, 1)
    }

    private fun countryQuestion(aircraft: Aircraft): QuizQuestion? {
        val country = RegistrationCountries.fromRegistration(aircraft.registration) ?: return null
        val distractors = RegistrationCountries.allCountries - country
        val reg = aircraft.registration!!.trim().uppercase()
        return build(KIND_COUNTRY, "This plane is registered as $reg. Which country is it registered in?", country, distractors, 2)
    }

    private fun altitudeQuestion(aircraft: Aircraft): QuizQuestion? {
        val alt = aircraft.altitudeFt ?: return null
        val correct = ALTITUDE_BANDS.indexOfFirst { alt in it.first }
        if (correct < 0) return null
        return QuizQuestion(
            kind = KIND_ALTITUDE,
            prompt = "About how high is this plane flying right now?",
            options = ALTITUDE_BANDS.map { it.second },
            correctIndex = correct,
            difficulty = 1,
        )
    }

    /** Last resort when we know nothing useful about the aircraft. */
    private fun generalQuestion(avoidKind: String?): QuizQuestion {
        val pool = GENERAL.filter { "$KIND_GENERAL:${it.id}" != avoidKind }.ifEmpty { GENERAL }
        val q = pool.random(random)
        return shuffled("$KIND_GENERAL:${q.id}", q.prompt, q.answer, q.distractors, q.difficulty)
    }

    // ---- Helpers ----

    private fun fromCurated(q: CuratedQuestion): QuizQuestion =
        shuffled(kindOf(q), q.prompt, q.answer, q.distractors, q.difficulty)

    private fun build(kind: String, prompt: String, answer: String, pool: List<String>, difficulty: Int): QuizQuestion? {
        val distractors = pool.filter { it != answer }.distinct().shuffled(random).take(3)
        if (distractors.size < 3) return null
        return shuffled(kind, prompt, answer, distractors, difficulty)
    }

    private fun shuffled(kind: String, prompt: String, answer: String, distractors: List<String>, difficulty: Int): QuizQuestion {
        val options = (listOf(answer) + distractors.take(3)).shuffled(random)
        return QuizQuestion(kind, prompt, options, options.indexOf(answer), difficulty)
    }

    companion object {
        const val KIND_MODEL = "model"
        const val KIND_MANUFACTURER = "manufacturer"
        const val KIND_ENGINES = "engines"
        const val KIND_SEATS = "seats"
        const val KIND_AIRLINE = "airline"
        const val KIND_COUNTRY = "country"
        const val KIND_ALTITUDE = "altitude"
        const val KIND_GENERAL = "general"
        const val KIND_CURATED = "curated"

        /** How often a curated question is used when one exists for the type. */
        private const val CURATED_PERCENT = 50

        fun kindOf(q: CuratedQuestion): String = "$KIND_CURATED:${q.id}"

        private val SEAT_BANDS: List<Pair<IntRange, String>> = listOf(
            0..9 to "Under 10",
            10..49 to "10 to 49",
            50..99 to "50 to 99",
            100..149 to "100 to 149",
            150..199 to "150 to 199",
            200..299 to "200 to 299",
            300..399 to "300 to 399",
            400..Int.MAX_VALUE to "400 or more",
        )

        private val ALTITUDE_BANDS: List<Pair<IntRange, String>> = listOf(
            Int.MIN_VALUE..4_999 to "Under 5,000 ft",
            5_000..14_999 to "5,000 to 15,000 ft",
            15_000..29_999 to "15,000 to 30,000 ft",
            30_000..Int.MAX_VALUE to "Over 30,000 ft",
        )

        private val GENERAL: List<CuratedQuestion> = listOf(
            CuratedQuestion(
                "icao-hex", emptySet(),
                "Every aircraft broadcasts a unique ID called an ICAO address. What format is it?",
                "Six hexadecimal digits", listOf("A 10-digit phone number", "Four letters", "The pilot's name"),
                1,
            ),
            CuratedQuestion(
                "adsb", emptySet(),
                "Which technology lets apps like this one see where planes are?",
                "ADS-B", listOf("GPS jamming", "Sonar", "Bluetooth"),
                1,
            ),
            CuratedQuestion(
                "squawk-7700", emptySet(),
                "What does transponder code 7700 mean?",
                "General emergency", listOf("Hijacking", "Radio failure", "Landing soon"),
                2,
            ),
            CuratedQuestion(
                "contrail", emptySet(),
                "What are the white trails behind high-flying jets mostly made of?",
                "Ice crystals", listOf("Smoke", "Fuel vapour", "Dust"),
                1,
            ),
            CuratedQuestion(
                "flight-level", emptySet(),
                "A plane at 'flight level 350' is flying at about what altitude?",
                "35,000 ft", listOf("3,500 ft", "350 ft", "350,000 ft"),
                1,
            ),
        )
    }
}

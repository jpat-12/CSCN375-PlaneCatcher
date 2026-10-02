package com.planecatcher.core.quiz

/** A hand-written question about a type. The first option is the correct one; options are shuffled when asked. */
data class CuratedQuestion(
    val id: String,
    val typeCodes: Set<String>,
    val prompt: String,
    val answer: String,
    val distractors: List<String>,
    val difficulty: Int = 2,
)

/** Curated question bank for popular and notable types. The generator covers everything else. */
object CuratedQuestions {
    val all: List<CuratedQuestion> = listOf(
        CuratedQuestion(
            "a380-decks", setOf("A388"),
            "How many full-length passenger decks does the Airbus A380 have?",
            "Two", listOf("One", "Three", "One and a half"),
        ),
        CuratedQuestion(
            "a380-first-operator", setOf("A388"),
            "Which airline flew the first A380 passenger service, in 2007?",
            "Singapore Airlines", listOf("Emirates", "Lufthansa", "Qantas"),
        ),
        CuratedQuestion(
            "747-nickname", setOf("B744", "B748", "BLCF"),
            "What is the Boeing 747's best-known nickname?",
            "Queen of the Skies", listOf("Dreamliner", "Jumbo Mouse", "Stratocruiser"),
        ),
        CuratedQuestion(
            "747-hump", setOf("B744", "B748"),
            "What sits in the 747's famous 'hump'?",
            "The upper deck", listOf("The fuel tanks", "The cargo door", "The auxiliary engine"),
        ),
        CuratedQuestion(
            "787-material", setOf("B788", "B789", "B78X"),
            "The 787 Dreamliner's fuselage is mostly made of what?",
            "Carbon-fibre composite", listOf("Aluminium", "Titanium", "Steel"),
        ),
        CuratedQuestion(
            "787-windows", setOf("B788", "B789", "B78X"),
            "How do passengers dim the windows on a 787?",
            "Electrochromic buttons", listOf("Pull-down shades", "Sliding covers", "They cannot"),
        ),
        CuratedQuestion(
            "737-first-flight", setOf("B737", "B738", "B739", "B38M", "B39M"),
            "In which decade did the Boeing 737 first fly?",
            "1960s", listOf("1950s", "1970s", "1980s"),
        ),
        CuratedQuestion(
            "737max-winglets", setOf("B38M", "B39M"),
            "What are the split-tip wing ends on the 737 MAX called?",
            "Advanced Technology winglets", listOf("Sharklets", "Raked wingtips", "Wing fences"),
        ),
        CuratedQuestion(
            "a320-sharklets", setOf("A320", "A321", "A20N", "A21N", "A319"),
            "What does Airbus call the upturned wingtips on newer A320-family jets?",
            "Sharklets", listOf("Winglets", "Wingtip fences", "Scimitars"),
        ),
        CuratedQuestion(
            "a320-flybywire", setOf("A320", "A321", "A20N", "A21N", "A319"),
            "The A320 was the first airliner with what cockpit control?",
            "Digital fly-by-wire with sidesticks", listOf("A yoke and cables", "Voice control", "A single throttle"),
        ),
        CuratedQuestion(
            "a350-mask", setOf("A359", "A35K"),
            "What gives the A350 its 'raccoon mask' look?",
            "Dark cockpit window frames", listOf("Painted nose cone", "Tinted cabin windows", "Black engine cowls"),
        ),
        CuratedQuestion(
            "777-engines", setOf("B77W", "B77L", "B772"),
            "The 777's GE90 engines are famous for being what?",
            "Among the most powerful jet engines", listOf("The quietest engines", "Propeller-driven", "Rear-mounted"),
        ),
        CuratedQuestion(
            "an225-unique", setOf("A225"),
            "How many An-225 Mriyas were completed?",
            "One", listOf("Two", "Twelve", "Fifty"),
        ),
        CuratedQuestion(
            "beluga-job", setOf("A3ST", "A337"),
            "What does the Airbus Beluga mainly carry?",
            "Aircraft parts between factories", listOf("Passengers", "Fuel for other jets", "Mail"),
        ),
        CuratedQuestion(
            "dreamlifter-job", setOf("BLCF"),
            "What was the 747 Dreamlifter built to carry?",
            "787 parts", listOf("Space shuttles", "Racehorses", "Oil"),
        ),
        CuratedQuestion(
            "c130-first-flight", setOf("C130", "C30J"),
            "The C-130 Hercules has been in production since which decade?",
            "1950s", listOf("1970s", "1990s", "2010s"),
        ),
        CuratedQuestion(
            "kc135-role", setOf("K35R", "KC46"),
            "What is the main job of this aircraft?",
            "Air-to-air refuelling", listOf("Passenger transport", "Firefighting", "Weather research"),
        ),
        CuratedQuestion(
            "c172-record", setOf("C172"),
            "The Cessna 172 holds which record?",
            "Most-produced aircraft ever", listOf("Fastest piston plane", "Highest-flying propeller plane", "First plane with a jet engine"),
        ),
        CuratedQuestion(
            "dash8-tail", setOf("DH8D", "DH8A"),
            "What kind of tail does the Dash 8 have?",
            "T-tail", listOf("V-tail", "Twin tail", "No tail"),
        ),
    )

    private val byType: Map<String, List<CuratedQuestion>> =
        all.flatMap { q -> q.typeCodes.map { it to q } }.groupBy({ it.first }, { it.second })

    fun forType(typeCode: String?): List<CuratedQuestion> =
        typeCode?.trim()?.uppercase()?.let { byType[it] }.orEmpty()
}

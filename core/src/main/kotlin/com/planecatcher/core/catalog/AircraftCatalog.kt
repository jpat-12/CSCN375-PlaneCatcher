package com.planecatcher.core.catalog

enum class AircraftCategory(val label: String) {
    NARROWBODY("Narrowbody airliner"),
    WIDEBODY("Widebody airliner"),
    REGIONAL_JET("Regional jet"),
    TURBOPROP("Turboprop"),
    BUSINESS_JET("Business jet"),
    GENERAL_AVIATION("Light aircraft"),
    HELICOPTER("Helicopter"),
    MILITARY("Military aircraft"),
    SPECIAL("Special or outsize aircraft"),
}

enum class EngineType(val label: String) { JET("Jet"), TURBOPROP("Turboprop"), PISTON("Piston"), TURBOSHAFT("Turboshaft") }

/** Facts about one ICAO aircraft type, used for labels and quiz questions. */
data class TypeInfo(
    val code: String,
    val manufacturer: String,
    val model: String,
    val category: AircraftCategory,
    val engines: Int,
    val engineType: EngineType,
    /** Typical passenger seats; 0 for freighters, military and others where it does not apply. */
    val typicalSeats: Int = 0,
    /** Typical range in nautical miles. */
    val rangeNm: Int = 0,
) {
    val fullName: String get() = "$manufacturer $model"
}

/** A small offline catalog of common and notable types. Unknown types are still catchable. */
object AircraftCatalog {
    private fun t(
        code: String, mfr: String, model: String, cat: AircraftCategory,
        engines: Int, type: EngineType, seats: Int = 0, range: Int = 0,
    ) = TypeInfo(code, mfr, model, cat, engines, type, seats, range)

    private val N = AircraftCategory.NARROWBODY
    private val W = AircraftCategory.WIDEBODY
    private val R = AircraftCategory.REGIONAL_JET
    private val TP = AircraftCategory.TURBOPROP
    private val BJ = AircraftCategory.BUSINESS_JET
    private val GA = AircraftCategory.GENERAL_AVIATION
    private val H = AircraftCategory.HELICOPTER
    private val M = AircraftCategory.MILITARY
    private val S = AircraftCategory.SPECIAL
    private val JET = EngineType.JET
    private val PROP = EngineType.TURBOPROP
    private val PISTON = EngineType.PISTON
    private val SHAFT = EngineType.TURBOSHAFT

    val all: List<TypeInfo> = listOf(
        // Narrowbody airliners
        t("B737", "Boeing", "737-700", N, 2, JET, 140, 3000),
        t("B738", "Boeing", "737-800", N, 2, JET, 175, 2900),
        t("B739", "Boeing", "737-900", N, 2, JET, 180, 2950),
        t("B38M", "Boeing", "737 MAX 8", N, 2, JET, 175, 3550),
        t("B39M", "Boeing", "737 MAX 9", N, 2, JET, 190, 3550),
        t("B752", "Boeing", "757-200", N, 2, JET, 200, 3900),
        t("B753", "Boeing", "757-300", N, 2, JET, 240, 3400),
        t("A319", "Airbus", "A319", N, 2, JET, 128, 3700),
        t("A320", "Airbus", "A320", N, 2, JET, 160, 3300),
        t("A321", "Airbus", "A321", N, 2, JET, 190, 3200),
        t("A20N", "Airbus", "A320neo", N, 2, JET, 165, 3500),
        t("A21N", "Airbus", "A321neo", N, 2, JET, 200, 4000),
        t("BCS1", "Airbus", "A220-100", N, 2, JET, 115, 3400),
        t("BCS3", "Airbus", "A220-300", N, 2, JET, 140, 3450),
        t("MD88", "McDonnell Douglas", "MD-88", N, 2, JET, 150, 2050),
        // Regional jets
        t("CRJ2", "Bombardier", "CRJ200", R, 2, JET, 50, 1700),
        t("CRJ7", "Bombardier", "CRJ700", R, 2, JET, 70, 1400),
        t("CRJ9", "Bombardier", "CRJ900", R, 2, JET, 76, 1550),
        t("E170", "Embraer", "E170", R, 2, JET, 72, 2150),
        t("E75L", "Embraer", "E175", R, 2, JET, 76, 2200),
        t("E75S", "Embraer", "E175", R, 2, JET, 76, 2200),
        t("E190", "Embraer", "E190", R, 2, JET, 100, 2450),
        t("E195", "Embraer", "E195", R, 2, JET, 120, 2300),
        t("E290", "Embraer", "E190-E2", R, 2, JET, 104, 2850),
        // Turboprops
        t("AT72", "ATR", "ATR 72", TP, 2, PROP, 70, 825),
        t("AT76", "ATR", "ATR 72-600", TP, 2, PROP, 72, 825),
        t("AT45", "ATR", "ATR 42-500", TP, 2, PROP, 48, 700),
        t("DH8D", "De Havilland Canada", "Dash 8-400", TP, 2, PROP, 78, 1100),
        t("DH8A", "De Havilland Canada", "Dash 8-100", TP, 2, PROP, 37, 1000),
        t("SF34", "Saab", "340", TP, 2, PROP, 34, 930),
        t("C208", "Cessna", "208 Caravan", TP, 1, PROP, 9, 1000),
        t("PC12", "Pilatus", "PC-12", TP, 1, PROP, 9, 1800),
        t("B350", "Beechcraft", "King Air 350", TP, 2, PROP, 9, 1800),
        // Business jets
        t("C68A", "Cessna", "Citation Latitude", BJ, 2, JET, 9, 2700),
        t("C560", "Cessna", "Citation V", BJ, 2, JET, 8, 1900),
        t("E55P", "Embraer", "Phenom 300", BJ, 2, JET, 8, 2000),
        t("CL35", "Bombardier", "Challenger 350", BJ, 2, JET, 10, 3200),
        t("GLF5", "Gulfstream", "G550", BJ, 2, JET, 16, 6750),
        t("GLF6", "Gulfstream", "G650", BJ, 2, JET, 18, 7000),
        t("GL7T", "Bombardier", "Global 7500", BJ, 2, JET, 19, 7700),
        t("LJ45", "Learjet", "45", BJ, 2, JET, 8, 2000),
        t("FA7X", "Dassault", "Falcon 7X", BJ, 3, JET, 14, 5950),
        t("HDJT", "Honda", "HondaJet", BJ, 2, JET, 6, 1400),
        // Widebody airliners
        t("B763", "Boeing", "767-300", W, 2, JET, 220, 5900),
        t("B764", "Boeing", "767-400", W, 2, JET, 245, 5600),
        t("B772", "Boeing", "777-200", W, 2, JET, 310, 5200),
        t("B77W", "Boeing", "777-300ER", W, 2, JET, 370, 7350),
        t("B77L", "Boeing", "777-200LR", W, 2, JET, 300, 8550),
        t("B788", "Boeing", "787-8 Dreamliner", W, 2, JET, 245, 7300),
        t("B789", "Boeing", "787-9 Dreamliner", W, 2, JET, 290, 7550),
        t("B78X", "Boeing", "787-10 Dreamliner", W, 2, JET, 330, 6300),
        t("A332", "Airbus", "A330-200", W, 2, JET, 250, 7250),
        t("A333", "Airbus", "A330-300", W, 2, JET, 290, 6350),
        t("A339", "Airbus", "A330-900neo", W, 2, JET, 290, 7200),
        t("A359", "Airbus", "A350-900", W, 2, JET, 315, 8100),
        t("A35K", "Airbus", "A350-1000", W, 2, JET, 360, 8700),
        t("MD11", "McDonnell Douglas", "MD-11", W, 3, JET, 0, 6700),
        // Legendary / special
        t("A388", "Airbus", "A380", W, 4, JET, 525, 8000),
        t("B744", "Boeing", "747-400", W, 4, JET, 416, 7260),
        t("B748", "Boeing", "747-8", W, 4, JET, 410, 7730),
        t("A343", "Airbus", "A340-300", W, 4, JET, 295, 7300),
        t("A346", "Airbus", "A340-600", W, 4, JET, 380, 7800),
        t("A124", "Antonov", "An-124 Ruslan", S, 4, JET, 0, 2800),
        t("A225", "Antonov", "An-225 Mriya", S, 6, JET, 0, 2200),
        t("BLCF", "Boeing", "747 Dreamlifter", S, 4, JET, 0, 4200),
        t("A3ST", "Airbus", "Beluga", S, 2, JET, 0, 900),
        t("A337", "Airbus", "BelugaXL", S, 2, JET, 0, 2200),
        // Military
        t("C17", "Boeing", "C-17 Globemaster III", M, 4, JET, 0, 2400),
        t("C130", "Lockheed", "C-130 Hercules", M, 4, PROP, 0, 2050),
        t("C30J", "Lockheed Martin", "C-130J Super Hercules", M, 4, PROP, 0, 1800),
        t("C5M", "Lockheed", "C-5M Super Galaxy", M, 4, JET, 0, 4800),
        t("K35R", "Boeing", "KC-135 Stratotanker", M, 4, JET, 0, 1300),
        t("KC46", "Boeing", "KC-46 Pegasus", M, 2, JET, 0, 6400),
        t("E3TF", "Boeing", "E-3 Sentry", M, 4, JET, 0, 4000),
        t("P8", "Boeing", "P-8 Poseidon", M, 2, JET, 0, 1200),
        t("F16", "General Dynamics", "F-16 Fighting Falcon", M, 1, JET, 0, 2200),
        t("F35", "Lockheed Martin", "F-35 Lightning II", M, 1, JET, 0, 1200),
        t("V22", "Bell Boeing", "V-22 Osprey", M, 2, SHAFT, 0, 880),
        t("H60", "Sikorsky", "UH-60 Black Hawk", M, 2, SHAFT, 0, 320),
        // Light aircraft and helicopters
        t("C172", "Cessna", "172 Skyhawk", GA, 1, PISTON, 3, 640),
        t("C182", "Cessna", "182 Skylane", GA, 1, PISTON, 3, 915),
        t("PA28", "Piper", "PA-28 Cherokee", GA, 1, PISTON, 3, 500),
        t("SR22", "Cirrus", "SR22", GA, 1, PISTON, 4, 1000),
        t("BE36", "Beechcraft", "Bonanza", GA, 1, PISTON, 5, 900),
        t("DA40", "Diamond", "DA40", GA, 1, PISTON, 3, 700),
        t("R44", "Robinson", "R44", H, 1, PISTON, 3, 300),
        t("EC35", "Airbus Helicopters", "H135", H, 2, SHAFT, 6, 340),
        t("AS50", "Airbus Helicopters", "H125", H, 1, SHAFT, 5, 340),
        t("B06", "Bell", "206 JetRanger", H, 1, SHAFT, 4, 370),
    )

    private val byCode: Map<String, TypeInfo> = all.associateBy { it.code }

    fun lookup(typeCode: String?): TypeInfo? = typeCode?.trim()?.uppercase()?.let(byCode::get)

    /** Types in the same category as [info] with a different model name, for quiz distractors. */
    fun similarTo(info: TypeInfo): List<TypeInfo> =
        all.filter { it.category == info.category && it.model != info.model }
}

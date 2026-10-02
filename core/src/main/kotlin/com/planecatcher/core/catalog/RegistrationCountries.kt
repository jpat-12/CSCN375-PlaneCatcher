package com.planecatcher.core.catalog

/** Finds the country of registration from an aircraft registration's nationality prefix. */
object RegistrationCountries {
    // Longest prefixes are matched first, so "VH-" wins over "V".
    private val prefixes: List<Pair<String, String>> = listOf(
        "N" to "United States",
        "C-" to "Canada",
        "XA-" to "Mexico", "XB-" to "Mexico", "XC-" to "Mexico",
        "G-" to "United Kingdom",
        "EI-" to "Ireland",
        "D-" to "Germany",
        "F-" to "France",
        "EC-" to "Spain",
        "I-" to "Italy",
        "PH-" to "Netherlands",
        "OO-" to "Belgium",
        "HB-" to "Switzerland",
        "OE-" to "Austria",
        "SE-" to "Sweden",
        "LN-" to "Norway",
        "OY-" to "Denmark",
        "OH-" to "Finland",
        "SP-" to "Poland",
        "TC-" to "Turkey",
        "A6-" to "United Arab Emirates",
        "A7-" to "Qatar",
        "9V-" to "Singapore",
        "B-" to "China / Taiwan / Hong Kong",
        "JA" to "Japan",
        "HL" to "South Korea",
        "VH-" to "Australia",
        "ZK-" to "New Zealand",
        "VT-" to "India",
        "PR-" to "Brazil", "PT-" to "Brazil", "PP-" to "Brazil", "PS-" to "Brazil",
        "LV-" to "Argentina",
        "CC-" to "Chile",
        "HK-" to "Colombia",
        "ZS-" to "South Africa",
        "4X-" to "Israel",
        "9H-" to "Malta",
        "EW-" to "Belarus",
        "UR-" to "Ukraine",
    ).sortedByDescending { it.first.length }

    val allCountries: List<String> = prefixes.map { it.second }.distinct()

    fun fromRegistration(registration: String?): String? {
        val reg = registration?.trim()?.uppercase()?.takeIf { it.isNotEmpty() } ?: return null
        // US registrations are "N" followed by a digit, e.g. N12345.
        if (reg.length >= 2 && reg[0] == 'N' && reg[1].isDigit()) return "United States"
        return prefixes.firstOrNull { (p, _) -> p != "N" && reg.startsWith(p) }?.second
    }
}

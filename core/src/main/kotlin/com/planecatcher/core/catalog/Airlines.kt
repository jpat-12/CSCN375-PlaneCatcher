package com.planecatcher.core.catalog

/** Maps the 3-letter ICAO airline prefix of a callsign (e.g. "DAL" in "DAL123") to a name. */
object Airlines {
    val byIcao: Map<String, String> = mapOf(
        "AAL" to "American Airlines",
        "DAL" to "Delta Air Lines",
        "UAL" to "United Airlines",
        "SWA" to "Southwest Airlines",
        "ASA" to "Alaska Airlines",
        "JBU" to "JetBlue",
        "NKS" to "Spirit Airlines",
        "FFT" to "Frontier Airlines",
        "AAY" to "Allegiant Air",
        "HAL" to "Hawaiian Airlines",
        "SKW" to "SkyWest Airlines",
        "RPA" to "Republic Airways",
        "ENY" to "Envoy Air",
        "EDV" to "Endeavor Air",
        "JIA" to "PSA Airlines",
        "FDX" to "FedEx Express",
        "UPS" to "UPS Airlines",
        "GTI" to "Atlas Air",
        "ACA" to "Air Canada",
        "WJA" to "WestJet",
        "BAW" to "British Airways",
        "VIR" to "Virgin Atlantic",
        "EZY" to "easyJet",
        "RYR" to "Ryanair",
        "DLH" to "Lufthansa",
        "AFR" to "Air France",
        "KLM" to "KLM",
        "IBE" to "Iberia",
        "SWR" to "Swiss",
        "THY" to "Turkish Airlines",
        "UAE" to "Emirates",
        "QTR" to "Qatar Airways",
        "ETD" to "Etihad Airways",
        "SIA" to "Singapore Airlines",
        "CPA" to "Cathay Pacific",
        "JAL" to "Japan Airlines",
        "ANA" to "All Nippon Airways",
        "KAL" to "Korean Air",
        "QFA" to "Qantas",
        "ANZ" to "Air New Zealand",
        "AMX" to "Aeroméxico",
        "VOI" to "Volaris",
        "LAN" to "LATAM Airlines",
        "AVA" to "Avianca",
    )

    /** Returns the airline for an airline-style callsign ("DAL123"), or null for private/other callsigns. */
    fun fromCallsign(callsign: String?): String? {
        val cs = callsign?.trim()?.uppercase() ?: return null
        if (cs.length < 4 || !cs.take(3).all { it.isLetter() } || !cs[3].isDigit()) return null
        return byIcao[cs.take(3)]
    }
}

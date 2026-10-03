package com.planecatcher.core.progress

import com.planecatcher.core.catalog.AircraftCatalog

/** One thing to collect in a set: any of [codes] fills it. */
data class SetSlot(val label: String, val codes: Set<String>)

/** A themed group of aircraft. Completing every slot earns [bonus] points. */
data class CollectionSet(
    val id: String,
    val name: String,
    val description: String,
    val slots: List<SetSlot>,
    val bonus: Int,
)

data class SetProgress(val set: CollectionSet, val filled: List<Boolean>) {
    val have: Int get() = filled.count { it }
    val total: Int get() = filled.size
    val complete: Boolean get() = have == total
}

object CollectionSets {
    /** One slot per type, labelled with its catalog name. */
    private fun types(vararg codes: String) =
        codes.map { SetSlot(AircraftCatalog.lookup(it)?.fullName ?: it, setOf(it)) }

    private fun slot(label: String, vararg codes: String) = SetSlot(label, codes.toSet())

    val all: List<CollectionSet> = listOf(
        CollectionSet(
            "b737", "737 Family", "Every generation of Boeing's best-selling jet.",
            types("B737", "B738", "B739", "B38M", "B39M"), bonus = 15,
        ),
        CollectionSet(
            "a320", "A320 Family", "Airbus's single-aisle workhorses, old and new.",
            types("A319", "A320", "A321", "A20N", "A21N"), bonus = 15,
        ),
        CollectionSet(
            "regional", "Regional Runners", "The small jets and props that link smaller cities.",
            listOf(
                slot("Bombardier CRJ", "CRJ2", "CRJ7", "CRJ9"),
                slot("Embraer E-Jet", "E170", "E75L", "E75S", "E190", "E195", "E290"),
                slot("ATR 42/72", "AT45", "AT72", "AT76"),
                slot("Dash 8", "DH8A", "DH8D"),
            ),
            bonus = 25,
        ),
        CollectionSet(
            "ga", "Weekend Flyers", "Light aircraft and helicopters.",
            listOf(
                slot("Cessna single", "C172", "C182", "C208"),
                slot("Piper", "PA28"),
                slot("Cirrus", "SR22"),
                slot("Helicopter", "R44", "EC35", "AS50", "B06"),
            ),
            bonus = 15,
        ),
        CollectionSet(
            "bizjet", "Bizjet Club", "Private jets from the big business-aviation makers.",
            listOf(
                slot("Cessna Citation", "C68A", "C560"),
                slot("Gulfstream", "GLF5", "GLF6"),
                slot("Bombardier Challenger/Global", "CL35", "GL7T"),
                slot("Embraer Phenom", "E55P"),
                slot("Dassault Falcon", "FA7X"),
            ),
            bonus = 40,
        ),
        CollectionSet(
            "dreamliner", "Dreamliners", "All three sizes of the Boeing 787.",
            types("B788", "B789", "B78X"), bonus = 40,
        ),
        CollectionSet(
            "triple7", "Triple Seven", "Boeing's big twin-engine long-haulers.",
            types("B772", "B77W", "B77L"), bonus = 40,
        ),
        CollectionSet(
            "military", "On Duty", "Military transports, tankers and fighters.",
            listOf(
                slot("Transport", "C17", "C130", "C30J", "C5M"),
                slot("Tanker", "K35R", "KC46"),
                slot("Fighter", "F16", "F35"),
            ),
            bonus = 60,
        ),
        CollectionSet(
            "airbus-wide", "Airbus Widebodies", "Every Airbus twin-aisle family, up to the A380.",
            listOf(
                slot("A330", "A332", "A333", "A339"),
                slot("A340", "A343", "A346"),
                slot("A350", "A359", "A35K"),
                slot("A380", "A388"),
            ),
            bonus = 150,
        ),
        CollectionSet(
            "queens", "Queens of the Sky", "The Boeing 747, in passenger and freighter form.",
            listOf(slot("747-400", "B744"), slot("747-8", "B748"), slot("747 Dreamlifter", "BLCF")),
            bonus = 150,
        ),
    )

    fun progress(caughtTypeCodes: Set<String>): List<SetProgress> = all.map { set ->
        SetProgress(set, set.slots.map { slot -> slot.codes.any { it in caughtTypeCodes } })
    }
}

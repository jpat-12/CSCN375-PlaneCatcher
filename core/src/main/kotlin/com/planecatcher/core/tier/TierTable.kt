package com.planecatcher.core.tier

import com.planecatcher.core.catalog.AircraftCatalog
import com.planecatcher.core.catalog.AircraftCategory
import com.planecatcher.core.model.Tier
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Assigns a [Tier] from an aircraft's type code and military flag.
 *
 * The explicit table comes from JSON (bundled as an asset, and replaceable later
 * from a server) so tiers can change without an app release. Types not listed
 * fall back to their catalog category, then to [defaultTier].
 */
class TierTable(
    private val byType: Map<String, Tier>,
    private val defaultTier: Tier = Tier.COMMON,
    val version: Int = 1,
) {
    fun classify(typeCode: String?, isMilitary: Boolean): Tier {
        val code = typeCode?.trim()?.uppercase()
        val base = code?.let(byType::get)
            ?: AircraftCatalog.lookup(code)?.category?.let(::tierForCategory)
            ?: defaultTier
        // Military aircraft are always at least Epic.
        return if (isMilitary && !base.atLeast(Tier.EPIC)) Tier.EPIC else base
    }

    @Serializable
    private data class TierFile(
        val version: Int = 1,
        val default: String = "COMMON",
        val tiers: Map<String, List<String>> = emptyMap(),
    )

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun tierForCategory(category: AircraftCategory): Tier = when (category) {
            AircraftCategory.NARROWBODY,
            AircraftCategory.GENERAL_AVIATION,
            AircraftCategory.HELICOPTER -> Tier.COMMON
            AircraftCategory.REGIONAL_JET,
            AircraftCategory.TURBOPROP,
            AircraftCategory.BUSINESS_JET -> Tier.RARE
            AircraftCategory.WIDEBODY,
            AircraftCategory.MILITARY -> Tier.EPIC
            AircraftCategory.SPECIAL -> Tier.LEGENDARY
        }

        /**
         * Parses a tier file:
         * `{"version":1,"default":"COMMON","tiers":{"LEGENDARY":["A388","B744"],"EPIC":[...]}}`
         * Unknown tier names are ignored.
         */
        fun fromJson(text: String): TierTable {
            val file = json.decodeFromString<TierFile>(text)
            val map = buildMap {
                for ((tierName, codes) in file.tiers) {
                    val tier = Tier.fromName(tierName) ?: continue
                    codes.forEach { put(it.trim().uppercase(), tier) }
                }
            }
            return TierTable(map, Tier.fromName(file.default) ?: Tier.COMMON, file.version)
        }

        /** Category-based table with no explicit overrides. */
        val categoryOnly: TierTable = TierTable(emptyMap())
    }
}

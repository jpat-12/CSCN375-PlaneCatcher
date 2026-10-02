package com.planecatcher.core

import com.planecatcher.core.model.Tier
import com.planecatcher.core.tier.TierTable
import org.junit.Assert.assertEquals
import org.junit.Test

class TierTableTest {
    private val table = TierTable.fromJson(
        """
        {
          "version": 3,
          "default": "COMMON",
          "tiers": {
            "LEGENDARY": ["a388", "B744"],
            "EPIC": ["B77W"],
            "NOT_A_TIER": ["XXXX"]
          }
        }
        """.trimIndent(),
    )

    @Test
    fun explicit_entries_win_and_are_case_insensitive() {
        assertEquals(Tier.LEGENDARY, table.classify("A388", false))
        assertEquals(Tier.LEGENDARY, table.classify("b744", false))
        assertEquals(Tier.EPIC, table.classify("B77W", false))
        assertEquals(3, table.version)
    }

    @Test
    fun unlisted_types_fall_back_to_catalog_category() {
        assertEquals(Tier.COMMON, table.classify("B738", false))
        assertEquals(Tier.RARE, table.classify("CRJ9", false))
        assertEquals(Tier.RARE, table.classify("GLF6", false))
        assertEquals(Tier.EPIC, table.classify("A359", false))
        assertEquals(Tier.LEGENDARY, table.classify("A225", false))
    }

    @Test
    fun unknown_types_use_default() {
        assertEquals(Tier.COMMON, table.classify("ZZZZ", false))
        assertEquals(Tier.COMMON, table.classify(null, false))
        assertEquals(Tier.COMMON, table.classify("XXXX", false))
    }

    @Test
    fun military_is_at_least_epic_but_never_downgraded() {
        assertEquals(Tier.EPIC, table.classify("C172", true))
        assertEquals(Tier.EPIC, table.classify(null, true))
        assertEquals(Tier.LEGENDARY, table.classify("A388", true))
    }

    @Test
    fun points_match_outline() {
        assertEquals(listOf(1, 5, 20, 100), Tier.entries.map { it.points })
    }
}

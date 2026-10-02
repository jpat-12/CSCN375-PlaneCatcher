package com.planecatcher.core.model

enum class Tier(val displayName: String, val points: Int) {
    COMMON("Common", 1),
    RARE("Rare", 5),
    EPIC("Epic", 20),
    LEGENDARY("Legendary", 100);

    fun atLeast(other: Tier): Boolean = ordinal >= other.ordinal

    companion object {
        fun fromName(name: String?): Tier? =
            entries.firstOrNull { it.name.equals(name?.trim(), ignoreCase = true) }
    }
}

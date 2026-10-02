package com.planecatcher.data

import android.content.Context
import com.planecatcher.core.tier.TierTable
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Supplies the tier lookup table from `assets/tiers.json`. Replacing this file (or
 * later, downloading it from a server) changes tiers without code changes.
 */
@Singleton
class TierProvider @Inject constructor(@ApplicationContext private val context: Context) {
    val table: TierTable by lazy {
        runCatching {
            context.assets.open("tiers.json").bufferedReader().use { TierTable.fromJson(it.readText()) }
        }.getOrDefault(TierTable.categoryOnly)
    }
}

package com.planecatcher.domain

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** Emits immediately and then every [periodMs]; used for countdowns. */
fun ticker(periodMs: Long = 1_000L): Flow<Unit> = flow {
    while (true) {
        emit(Unit)
        delay(periodMs)
    }
}

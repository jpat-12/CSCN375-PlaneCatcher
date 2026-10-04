package com.planecatcher.domain

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Which planes have already popped up automatically, and which the user dismissed.
 * Shared by the Camera and Map screens so switching tabs doesn't re-show a pop-up.
 */
@Singleton
class PopupMemory @Inject constructor() {
    val autoShown: MutableSet<String> = mutableSetOf()
    val dismissedAt: MutableMap<String, Long> = mutableMapOf()
}

package com.planecatcher.core.rules

/** A plane whose quiz was answered wrong; it can be retried at [retryAtMs]. */
data class QuizLock(
    val planeHex: String,
    val retryAtMs: Long,
    /** The kind of the question that was failed, so a retry asks something different. */
    val failedKind: String? = null,
) {
    fun isLocked(nowMs: Long): Boolean = nowMs < retryAtMs
    fun remainingMs(nowMs: Long): Long = (retryAtMs - nowMs).coerceAtLeast(0)

    companion object {
        fun afterWrongAnswer(planeHex: String, nowMs: Long, failedKind: String?): QuizLock =
            QuizLock(planeHex, nowMs + GameRules.QUIZ_LOCK_MS, failedKind)
    }
}

package com.planecatcher.core.quiz

/**
 * One multiple-choice question. [kind] identifies the question family (or the
 * specific curated question) so a retry after a wrong answer can ask something else.
 */
data class QuizQuestion(
    val kind: String,
    val prompt: String,
    val options: List<String>,
    val correctIndex: Int,
    val difficulty: Int = 1,
) {
    init {
        require(options.size == com.planecatcher.core.rules.GameRules.QUIZ_OPTION_COUNT) {
            "Expected 4 options, got ${options.size}"
        }
        require(options.toSet().size == options.size) { "Options must be distinct: $options" }
        require(correctIndex in options.indices)
    }

    val correctAnswer: String get() = options[correctIndex]
    fun isCorrect(choice: Int): Boolean = choice == correctIndex
}

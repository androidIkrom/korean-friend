package uz.hangulfriend.hangul

enum class Feedback { CORRECT, SPACING, FINAL_CONSONANT, VOWEL, ONE_LETTER, WRONG }

data class CheckResult(val feedback: Feedback, val closest: String) {
    val correct: Boolean get() = feedback == Feedback.CORRECT
}

object AnswerChecker {
    fun check(input: String, answers: List<String>): CheckResult {
        require(answers.isNotEmpty()) { "answers must not be empty" }
        val given = Hangul.normalize(input)
        val normalized = answers.map { Hangul.normalize(it) }
        normalized.firstOrNull { it == given }?.let { return CheckResult(Feedback.CORRECT, it) }

        val givenJamo = Hangul.jamo(given)
        val closest = normalized.minBy { levenshtein(givenJamo, Hangul.jamo(it)) }
        val feedback = when {
            given.replace(" ", "") == closest.replace(" ", "") -> Feedback.SPACING
            differsOnly(given, closest) { a, b -> a.initial == b.initial && a.medial == b.medial } ->
                Feedback.FINAL_CONSONANT
            differsOnly(given, closest) { a, b -> a.initial == b.initial && a.final == b.final } ->
                Feedback.VOWEL
            levenshtein(givenJamo, Hangul.jamo(closest)) == 1 -> Feedback.ONE_LETTER
            else -> Feedback.WRONG
        }
        return CheckResult(feedback, closest)
    }

    /**
     * True when both strings have the same length and every differing position is a pair of
     * syllables that [sameOtherwise] accepts, with at least one such position.
     */
    private fun differsOnly(a: String, b: String, sameOtherwise: (SyllableParts, SyllableParts) -> Boolean): Boolean {
        if (a.length != b.length) return false
        var diffs = 0
        for (i in a.indices) {
            if (a[i] == b[i]) continue
            val pa = Hangul.parts(a[i]) ?: return false
            val pb = Hangul.parts(b[i]) ?: return false
            if (!sameOtherwise(pa, pb)) return false
            diffs++
        }
        return diffs > 0
    }

    private fun levenshtein(a: List<Char>, b: List<Char>): Int {
        var prev = IntArray(b.size + 1) { it }
        for (i in 1..a.size) {
            val cur = IntArray(b.size + 1)
            cur[0] = i
            for (j in 1..b.size) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                cur[j] = minOf(prev[j] + 1, cur[j - 1] + 1, prev[j - 1] + cost)
            }
            prev = cur
        }
        return prev[b.size]
    }
}

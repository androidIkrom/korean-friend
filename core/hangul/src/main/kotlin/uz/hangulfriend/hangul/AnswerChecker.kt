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
        normalized.firstOrNull { it == given || withAuxiliaryAttached(it) == given }
            ?.let { return CheckResult(Feedback.CORRECT, it) }

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

    private val auxiliaryStarts = setOf('보', '봐', '봤', '볼', '봅', '본')
    private val connectiveVowels = setOf('ㅏ', 'ㅓ', 'ㅕ', 'ㅘ', 'ㅝ', 'ㅐ', 'ㅙ')

    /**
     * [answer] with the space before an auxiliary 보다 removed when it follows an -아/어 form
     * (입어 보세요 → 입어보세요). 한글 맞춤법 제47항 allows both spellings.
     */
    private fun withAuxiliaryAttached(answer: String): String {
        val words = answer.split(" ")
        val sb = StringBuilder(words.first())
        for (i in 1 until words.size) {
            val prev = Hangul.parts(words[i - 1].last())
            val joins = words[i].first() in auxiliaryStarts &&
                prev != null && prev.final == null && prev.medial in connectiveVowels
            if (!joins) sb.append(' ')
            sb.append(words[i])
        }
        return sb.toString()
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
}

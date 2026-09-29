package uz.hangulfriend.hangul

/** [mismatched] holds positions in the expected text of syllables that were said wrong or not at all. */
data class SpeechScore(val heard: String, val similarity: Double, val passed: Boolean, val mismatched: Set<Int>)

object SpeechScorer {
    const val PASS = 0.85

    private val ignored = Regex("[\\s.,?!]")

    /** Jamo-level similarity in [0, 1]; spacing and punctuation do not count. */
    fun similarity(expected: String, heard: String): Double {
        val a = Hangul.jamo(strip(expected))
        val b = Hangul.jamo(strip(heard))
        val longest = maxOf(a.size, b.size, 1)
        return 1.0 - levenshtein(a, b).toDouble() / longest
    }

    fun score(expected: String, candidates: List<String>): SpeechScore {
        val best = candidates.maxByOrNull { similarity(expected, it) }
        if (best == null) {
            val all = expected.indices.filter { Hangul.isSyllable(expected[it]) }.toSet()
            return SpeechScore(heard = "", similarity = 0.0, passed = false, mismatched = all)
        }
        val sim = similarity(expected, best)
        return SpeechScore(best, sim, sim >= PASS, mismatchedSyllables(expected, strip(best)))
    }

    private fun strip(s: String) = Hangul.normalize(s).replace(ignored, "")

    /** Aligns expected syllables with heard ones (Levenshtein backtrace) and returns expected positions not matched. */
    private fun mismatchedSyllables(expected: String, heard: String): Set<Int> {
        val positions = expected.indices.filter { Hangul.isSyllable(expected[it]) }
        val a = positions.map { expected[it] }
        val b = heard.filter { Hangul.isSyllable(it) }.toList()
        val d = Array(a.size + 1) { i -> IntArray(b.size + 1) { j -> if (i == 0) j else if (j == 0) i else 0 } }
        for (i in 1..a.size) for (j in 1..b.size) {
            val cost = if (a[i - 1] == b[j - 1]) 0 else 1
            d[i][j] = minOf(d[i - 1][j] + 1, d[i][j - 1] + 1, d[i - 1][j - 1] + cost)
        }
        val wrong = mutableSetOf<Int>()
        var i = a.size
        var j = b.size
        while (i > 0) {
            when {
                j > 0 && d[i][j] == d[i - 1][j - 1] + (if (a[i - 1] == b[j - 1]) 0 else 1) -> {
                    if (a[i - 1] != b[j - 1]) wrong += positions[i - 1]
                    i--
                    j--
                }
                d[i][j] == d[i - 1][j] + 1 -> {
                    wrong += positions[i - 1]
                    i--
                }
                else -> j--
            }
        }
        return wrong
    }
}

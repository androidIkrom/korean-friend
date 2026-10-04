package uz.hangulfriend.hangul

import java.text.Normalizer

/** A precomposed Hangul syllable split into Hangul Compatibility Jamo. */
data class SyllableParts(val initial: Char, val medial: Char, val final: Char?)

object Hangul {
    private const val BASE = 0xAC00
    private const val LAST = 0xD7A3

    private const val INITIALS = "ㄱㄲㄴㄷㄸㄹㅁㅂㅃㅅㅆㅇㅈㅉㅊㅋㅌㅍㅎ"
    private const val MEDIALS = "ㅏㅐㅑㅒㅓㅔㅕㅖㅗㅘㅙㅚㅛㅜㅝㅞㅟㅠㅡㅢㅣ"
    private const val FINALS = "ㄱㄲㄳㄴㄵㄶㄷㄹㄺㄻㄼㄽㄾㄿㅀㅁㅂㅄㅅㅆㅇㅈㅊㅋㅌㅍㅎ"

    private val whitespace = Regex("\\s+")
    private val trailingPunctuation = Regex("[.?!]+$")

    fun isSyllable(c: Char): Boolean = c.code in BASE..LAST

    fun parts(c: Char): SyllableParts? {
        if (!isSyllable(c)) return null
        val code = c.code - BASE
        val finalIndex = code % 28
        return SyllableParts(
            initial = INITIALS[code / 588],
            medial = MEDIALS[(code % 588) / 28],
            final = if (finalIndex == 0) null else FINALS[finalIndex - 1],
        )
    }

    /** The syllable of [initial] + [medial] (+ [final]), or null when a part cannot take that place. */
    fun compose(initial: Char, medial: Char, final: Char? = null): Char? {
        val i = INITIALS.indexOf(initial)
        val m = MEDIALS.indexOf(medial)
        val f = if (final == null) 0 else FINALS.indexOf(final) + 1
        if (i < 0 || m < 0 || (final != null && f == 0)) return null
        return (BASE + i * 588 + m * 28 + f).toChar()
    }

    fun jamo(s: String): List<Char> = buildList {
        for (c in s) {
            val p = parts(c)
            if (p == null) {
                add(c)
            } else {
                add(p.initial)
                add(p.medial)
                p.final?.let { add(it) }
            }
        }
    }

    fun normalize(s: String): String =
        Normalizer.normalize(s, Normalizer.Form.NFC)
            .trim()
            .replace(whitespace, " ")
            .replace(trailingPunctuation, "")
            .trim()
            .lowercase()
}

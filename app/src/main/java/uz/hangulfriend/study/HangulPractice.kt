package uz.hangulfriend.study

import kotlin.random.Random
import uz.hangulfriend.content.Letter
import uz.hangulfriend.content.LetterKind
import uz.hangulfriend.hangul.Hangul

/** Silent ㅇ writes every vowel from the first lesson on, so it counts as known before it is taught. */
private const val SILENT_INITIAL = 'ㅇ'

/**
 * One Hangul practice round for the letters of a lesson ([lesson]); [known] adds the letters of earlier
 * lessons, used for wrong options and for building syllables. Every lesson letter gets one listening or
 * sound item; readable example words and syllable building come on top.
 */
fun hangulPractice(lesson: List<Letter>, known: List<Letter>, random: Random): List<ExerciseItem> {
    val pool = (known + lesson).distinctBy { it.id }
    val basics = lesson.shuffled(random).mapIndexed { i, letter ->
        if (i % 2 == 0) {
            ExerciseItem.LetterSound(letter, options(letter, pool, random) { it.roman })
        } else {
            ExerciseItem.LetterListen(letter, options(letter, pool, random) { it.jamo })
        }
    }
    val reading = lesson.filter { readable(it.example.ko, pool) }.shuffled(random).take(HangulCaps.READ)
        .map { letter ->
            val others = pool.filter { it.example.roman != letter.example.roman }.distinctBy { it.example.roman }
                .shuffled(random).take(HangulCaps.OPTIONS - 1)
            ExerciseItem.ReadWord(letter, (others + letter).shuffled(random))
        }
    val building = lesson.shuffled(random).take(HangulCaps.BUILD).mapNotNull { buildItem(it, pool, random) }
    return arrange(basics + reading + building, random)
}

object HangulCaps {
    const val OPTIONS = 4
    const val READ = 4
    const val BUILD = 4
    const val PART_CHOICES = 3
}

/** [letter] and up to three letters of the same kind whose [key] differs from it and from each other. */
private fun options(letter: Letter, pool: List<Letter>, random: Random, key: (Letter) -> String): List<Letter> {
    val others = pool.filter { it.kind == letter.kind && key(it) != key(letter) }.distinctBy(key)
        .shuffled(random).take(HangulCaps.OPTIONS - 1)
    return (others + letter).shuffled(random)
}

/** Whether every syllable of [word] is made of letters in [pool], each in its own place. */
fun readable(word: String, pool: List<Letter>): Boolean {
    val initials = pool.filter { it.kind == LetterKind.CONSONANT }.map { it.jamo.single() }.toSet() + SILENT_INITIAL
    val medials = pool.filter { it.kind == LetterKind.VOWEL }.map { it.jamo.single() }.toSet()
    val finals = pool.filter { it.kind == LetterKind.FINAL }.map { it.jamo.single() }.toSet()
    return word.isNotEmpty() && word.all { c ->
        val p = Hangul.parts(c) ?: return@all false
        p.initial in initials && p.medial in medials && (p.final == null || p.final in finals)
    }
}

/**
 * A syllable around [letter]: a consonant takes a known vowel, a vowel takes a known consonant, a final
 * takes both. Null when the pool lacks the other parts (the first lesson teaches vowels only).
 */
private fun buildItem(letter: Letter, pool: List<Letter>, random: Random): ExerciseItem.BuildSyllable? {
    val consonants = pool.filter { it.kind == LetterKind.CONSONANT }.map { it.jamo.single() }.distinct()
    val vowels = pool.filter { it.kind == LetterKind.VOWEL }.map { it.jamo.single() }.distinct()
    val finals = pool.filter { it.kind == LetterKind.FINAL }.map { it.jamo.single() }.distinct()
    if (consonants.isEmpty() || vowels.isEmpty()) return null
    val jamo = letter.jamo.single()
    val initial = if (letter.kind == LetterKind.CONSONANT) jamo else consonants.random(random)
    val medial = if (letter.kind == LetterKind.VOWEL) jamo else vowels.random(random)
    val final = if (letter.kind == LetterKind.FINAL) jamo else null
    val target = Hangul.compose(initial, medial, final) ?: return null
    val finalChoices = final?.let { choices(it, finals, random) }.orEmpty()
    return ExerciseItem.BuildSyllable(
        target = target,
        letters = listOf(letter),
        initials = choices(initial, consonants, random),
        medials = choices(medial, vowels, random),
        finals = finalChoices,
        audio = pool.find { it.say == target.toString() }?.audio,
    )
}

/** [answer] and up to two other parts, shuffled. */
private fun choices(answer: Char, all: List<Char>, random: Random): List<Char> =
    ((all - answer).shuffled(random).take(HangulCaps.PART_CHOICES - 1) + answer).shuffled(random)

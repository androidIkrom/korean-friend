package uz.hangulfriend.study

import uz.hangulfriend.content.Exercise
import uz.hangulfriend.content.Lesson
import uz.hangulfriend.content.Letter
import uz.hangulfriend.content.Word
import uz.hangulfriend.data.CardIds
import uz.hangulfriend.data.CardKind

/** One step of a session. [cardIds] are the FSRS cards its result grades. */
sealed interface ExerciseItem {
    val typeKey: String
    val cardIds: List<String>

    data class Flashcard(val word: Word) : ExerciseItem {
        override val typeKey get() = "flashcard"
        override val cardIds get() = listOf(CardIds.of(word.id, CardKind.RECOGNIZE))
    }

    data class WordTyping(val word: Word) : ExerciseItem {
        override val typeKey get() = "reverse_typing"
        override val cardIds get() = listOf(CardIds.of(word.id, CardKind.PRODUCE))
    }

    /** Hear [word], pick its meaning among [options] (4 words of the lesson, [word] included). */
    data class ListenChoose(val word: Word, val options: List<Word>) : ExerciseItem {
        override val typeKey get() = "listen_choose"
        override val cardIds get() = listOf(CardIds.of(word.id, CardKind.RECOGNIZE))
    }

    /** Read [word] in Korean, pick its meaning among [options] ([word] included). */
    data class WordChoose(val word: Word, val options: List<Word>) : ExerciseItem {
        override val typeKey get() = "word_choose"
        override val cardIds get() = listOf(CardIds.of(word.id, CardKind.RECOGNIZE))
    }

    /** Hear [word], type it in Korean. */
    data class Dictation(val word: Word) : ExerciseItem {
        override val typeKey get() = "dictation"
        override val cardIds get() = listOf(CardIds.of(word.id, CardKind.PRODUCE))
    }

    /** Say a sentence aloud. Pronunciation practice only: it grades no FSRS card. */
    data class Speak(val ko: String, val uz: String, val audio: String?) : ExerciseItem {
        override val typeKey get() = "speak"
        override val cardIds get() = emptyList<String>()
    }

    data class Match(val words: List<Word>) : ExerciseItem {
        override val typeKey get() = "match"
        override val cardIds get() = words.map { CardIds.of(it.id, CardKind.RECOGNIZE) }
    }

    /** Hear [letter], pick its jamo among [options] ([letter] included). */
    data class LetterListen(val letter: Letter, val options: List<Letter>) : ExerciseItem {
        override val typeKey get() = "letter_listen"
        override val cardIds get() = listOf(CardIds.of(letter.cardItemId, CardKind.RECOGNIZE))
    }

    /** See [letter], pick its romanization among [options] ([letter] included). */
    data class LetterSound(val letter: Letter, val options: List<Letter>) : ExerciseItem {
        override val typeKey get() = "letter_sound"
        override val cardIds get() = listOf(CardIds.of(letter.cardItemId, CardKind.RECOGNIZE))
    }

    /** Read the example word of [letter], pick its romanization among the examples of [options]. */
    data class ReadWord(val letter: Letter, val options: List<Letter>) : ExerciseItem {
        override val typeKey get() = "read_word"
        override val cardIds get() = listOf(CardIds.of(letter.cardItemId, CardKind.RECOGNIZE))
    }

    /**
     * Build [target] by tapping one of [initials], one of [medials] and, when [finals] is not empty, one of
     * [finals]. [letters] are the target's parts taught in this lesson; their cards are graded.
     */
    data class BuildSyllable(
        val target: Char,
        val letters: List<Letter>,
        val initials: List<Char>,
        val medials: List<Char>,
        val finals: List<Char>,
        val audio: String? = null,
    ) : ExerciseItem {
        override val typeKey get() = "build_syllable"
        override val cardIds get() = letters.map { CardIds.of(it.cardItemId, CardKind.RECOGNIZE) }
    }

    data class Authored(val exercise: Exercise, override val cardIds: List<String>) : ExerciseItem {
        override val typeKey get() = exercise.type.name.lowercase()

        companion object {
            /** Grammar targets grade the grammar card; word targets grade the word's recognition card. */
            fun of(lesson: Lesson, exercise: Exercise): Authored {
                val grammarIds = lesson.grammar.map { it.id }.toSet()
                val cardIds = exercise.targets.map { t ->
                    CardIds.of(t, if (t in grammarIds) CardKind.GRAMMAR else CardKind.RECOGNIZE)
                }
                return Authored(exercise, cardIds)
            }
        }
    }
}

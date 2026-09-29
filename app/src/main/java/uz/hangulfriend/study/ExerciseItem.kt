package uz.hangulfriend.study

import uz.hangulfriend.content.Exercise
import uz.hangulfriend.content.Lesson
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

    data class Match(val words: List<Word>) : ExerciseItem {
        override val typeKey get() = "match"
        override val cardIds get() = words.map { CardIds.of(it.id, CardKind.RECOGNIZE) }
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

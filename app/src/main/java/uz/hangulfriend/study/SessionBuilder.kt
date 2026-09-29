package uz.hangulfriend.study

import kotlin.random.Random
import uz.hangulfriend.content.Lesson
import uz.hangulfriend.content.Word
import uz.hangulfriend.data.CardEntity
import uz.hangulfriend.data.CardKind

/**
 * Shuffles [items] so that neighbours differ in [ExerciseItem.typeKey] whenever the counts allow it.
 * Greedy: always take from the largest remaining type other than the previous one.
 */
fun arrange(items: List<ExerciseItem>, random: Random): List<ExerciseItem> {
    val byType = items.shuffled(random).groupBy { it.typeKey }.mapValues { it.value.toMutableList() }
    val result = ArrayList<ExerciseItem>(items.size)
    var last: String? = null
    while (result.size < items.size) {
        val left = byType.filterValues { it.isNotEmpty() }
        val candidates = left.filterKeys { it != last }.ifEmpty { left }
        val max = candidates.values.maxOf { it.size }
        val type = candidates.filterValues { it.size == max }.keys.random(random)
        result += byType.getValue(type).removeAt(0)
        last = type
    }
    return result
}

class SessionBuilder(private val random: Random) {
    fun vocabChunks(lesson: Lesson): List<List<Word>> = lesson.words.chunked(VOCAB_CHUNK)

    /**
     * One practice round of at most [AUTHORED_CAP] + [TYPING_CAP] + [MATCH_CAP] items (spec: 15–25).
     * Every grammar point gets up to [PER_GRAMMAR] exercises first; later rounds draw a new random mix.
     */
    fun lessonPractice(lesson: Lesson): List<ExerciseItem> {
        val pool = practiceExercises(lesson).shuffled(random)
        val guaranteed = lesson.grammar.flatMap { g -> pool.filter { g.id in it.targets }.take(PER_GRAMMAR) }.distinct()
        val chosen = (guaranteed + (pool - guaranteed.toSet())).take(maxOf(AUTHORED_CAP, guaranteed.size))
        val words = lesson.words.shuffled(random)
        val typing = words.take(TYPING_CAP).map { ExerciseItem.WordTyping(it) }
        val matches = words.drop(TYPING_CAP).ifEmpty { words }
            .chunked(MATCH_SIZE).filter { it.size >= 2 }.take(MATCH_CAP).map { ExerciseItem.Match(it) }
        return arrange(chosen.map { ExerciseItem.Authored.of(lesson, it) } + typing + matches, random)
    }

    fun lessonTest(lesson: Lesson): List<ExerciseItem> {
        val byId = lesson.exercises.associateBy { it.id }
        return arrange(lesson.test.mapNotNull { byId[it] }.map { ExerciseItem.Authored.of(lesson, it) }, random)
    }

    fun lessonReview(lesson: Lesson): List<ExerciseItem> {
        val cards = lesson.words.map { ExerciseItem.Flashcard(it) }
        val grammar = lesson.grammar.mapNotNull { g -> grammarExercise(lesson, g.id) }
        return arrange(cards + grammar, random)
    }

    fun review(cards: List<CardEntity>, lessons: Map<String, Lesson>): List<ExerciseItem> {
        val items = cards.mapNotNull { card ->
            val lesson = lessons[card.lessonId] ?: return@mapNotNull null
            when (card.kind) {
                CardKind.RECOGNIZE -> lesson.words.find { it.id == card.itemId }?.let { ExerciseItem.Flashcard(it) }
                CardKind.PRODUCE -> lesson.words.find { it.id == card.itemId }?.let { ExerciseItem.WordTyping(it) }
                CardKind.GRAMMAR -> grammarExercise(lesson, card.itemId)
            }
        }
        return arrange(items, random)
    }

    private fun practiceExercises(lesson: Lesson) = lesson.exercises.filter { it.id !in lesson.test }

    /** A random practice (non-test) exercise that targets [grammarId], or null when there is none. */
    fun grammarExercise(lesson: Lesson, grammarId: String): ExerciseItem.Authored? =
        practiceExercises(lesson)
            .filter { grammarId in it.targets }
            .randomOrNull(random)
            ?.let { ExerciseItem.Authored.of(lesson, it) }

    companion object {
        const val VOCAB_CHUNK = 6
        const val MATCH_SIZE = 5
        const val AUTHORED_CAP = 14
        const val PER_GRAMMAR = 2
        const val TYPING_CAP = 6
        const val MATCH_CAP = 2
    }
}

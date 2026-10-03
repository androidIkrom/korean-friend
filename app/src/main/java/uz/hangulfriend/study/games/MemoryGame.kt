package uz.hangulfriend.study.games

import kotlin.random.Random
import uz.hangulfriend.content.Word

data class MemoryCard(val wordId: String, val text: String, val korean: Boolean, val faceUp: Boolean = false, val matched: Boolean = false)

enum class FlipResult { FIRST, MATCH, MISMATCH, IGNORED }

/** Pairs a Korean word with its meaning. A mismatch stays visible until [hideMismatch]. */
class MemoryGame(words: List<Word>, random: Random) {
    var cards: List<MemoryCard> = words.shuffled(random).take(PAIRS)
        .flatMap { listOf(MemoryCard(it.id, it.ko, korean = true), MemoryCard(it.id, it.uz, korean = false)) }
        .shuffled(random)
        private set
    var moves = 0
        private set
    val done: Boolean get() = cards.all { it.matched }

    fun flip(index: Int): FlipResult {
        val card = cards[index]
        val open = cards.withIndex().filter { it.value.faceUp && !it.value.matched }
        if (card.faceUp || card.matched || open.size >= 2) return FlipResult.IGNORED
        cards = cards.toMutableList().also { it[index] = card.copy(faceUp = true) }
        val other = open.singleOrNull() ?: return FlipResult.FIRST
        moves++
        if (other.value.wordId != card.wordId) return FlipResult.MISMATCH
        cards = cards.mapIndexed { i, c -> if (i == index || i == other.index) c.copy(matched = true) else c }
        return FlipResult.MATCH
    }

    fun hideMismatch() {
        cards = cards.map { if (it.faceUp && !it.matched) it.copy(faceUp = false) else it }
    }

    companion object {
        const val PAIRS = 6

        /** 100 for a perfect game, 5 less per move beyond the [PAIRS] needed, never below 10. */
        fun score(moves: Int): Int = (100 - (moves - PAIRS) * 5).coerceAtLeast(10)
    }
}

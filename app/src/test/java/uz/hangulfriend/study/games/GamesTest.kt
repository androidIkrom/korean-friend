package uz.hangulfriend.study.games

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import uz.hangulfriend.Fixtures
import uz.hangulfriend.content.ExerciseType

class GamesTest {
    private val words = (1..8).map { Fixtures.word("w$it", "말$it", "so'z$it") }

    @Test fun memory_hasSixPairs() {
        val game = MemoryGame(words, Random(1))
        assertEquals(12, game.cards.size)
        assertEquals(6, game.cards.map { it.wordId }.toSet().size)
        game.cards.groupBy { it.wordId }.values.forEach { pair -> assertEquals(setOf(true, false), pair.map { it.korean }.toSet()) }
    }

    @Test fun memory_matchAndMismatch() {
        val game = MemoryGame(words, Random(1))
        val first = game.cards.indexOfFirst { it.korean }
        val partner = game.cards.indexOfFirst { !it.korean && it.wordId == game.cards[first].wordId }
        val other = game.cards.indexOfFirst { it.wordId != game.cards[first].wordId }
        assertEquals(FlipResult.FIRST, game.flip(first))
        assertEquals(FlipResult.MISMATCH, game.flip(other))
        assertEquals(FlipResult.IGNORED, game.flip(partner)) // two cards still open
        game.hideMismatch()
        assertEquals(FlipResult.FIRST, game.flip(first))
        assertEquals(FlipResult.MATCH, game.flip(partner))
        assertTrue(game.cards[first].matched && game.cards[partner].matched)
        assertEquals(2, game.moves)
    }

    @Test fun memory_doneWhenAllMatched() {
        val game = MemoryGame(words, Random(2))
        game.cards.map { it.wordId }.distinct().forEach { id ->
            val (a, b) = game.cards.withIndex().filter { it.value.wordId == id }.map { it.index }
            game.flip(a)
            game.flip(b)
        }
        assertTrue(game.done)
    }

    @Test fun memory_score() {
        assertEquals(100, MemoryGame.score(6))
        assertEquals(95, MemoryGame.score(7))
        assertEquals(10, MemoryGame.score(40))
    }

    @Test fun speed_timeFraction() {
        assertEquals(1f, SpeedRound.timeFraction(SpeedRound.DURATION_MS))
        assertEquals(0.5f, SpeedRound.timeFraction(SpeedRound.DURATION_MS / 2))
        assertEquals(0f, SpeedRound.timeFraction(-100))
        assertEquals(1f, SpeedRound.timeFraction(SpeedRound.DURATION_MS * 2))
    }

    @Test fun speed_optionsContainAnswer() {
        val round = SpeedRound(words, Random(3))
        repeat(20) {
            val q = round.next()
            assertEquals(4, q.options.toSet().size)
            assertTrue(q.word.uz in q.options)
        }
    }

    @Test fun speed_scoreCountsCorrect() {
        val round = SpeedRound(words, Random(3))
        val q1 = round.next()
        assertTrue(round.answer(q1.word.uz))
        val q2 = round.next()
        assertFalse(round.answer(q2.options.first { it != q2.word.uz }))
        assertEquals(1, round.score)
    }

    private val lesson = Fixtures.validLesson()

    @Test fun chain_onlyConjugate() {
        val chain = GrammarChain(lesson.exercises, Random(4))
        assertEquals(2, chain.total)
        assertTrue(chain.current()!!.type == ExerciseType.CONJUGATE)
    }

    @Test fun chain_endsOnFirstMistake() {
        val chain = GrammarChain(lesson.exercises, Random(4))
        assertTrue(chain.submit(chain.current()!!.answers.first()))
        assertEquals(1, chain.length)
        assertFalse(chain.submit("틀린 답"))
        assertTrue(chain.over)
        assertNull(chain.current())
        assertEquals(1, chain.length)
    }

    @Test fun chain_overWhenAllAnswered() {
        val chain = GrammarChain(lesson.exercises, Random(4))
        repeat(2) { chain.submit(chain.current()!!.answers.first()) }
        assertTrue(chain.over)
        assertEquals(2, chain.length)
    }
}

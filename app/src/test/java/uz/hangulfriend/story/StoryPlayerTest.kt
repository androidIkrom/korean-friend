package uz.hangulfriend.story

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import uz.hangulfriend.content.ChooseReply
import uz.hangulfriend.content.Story
import uz.hangulfriend.content.StoryLine
import uz.hangulfriend.content.StoryQuiz

class StoryPlayerTest {
    private val l1 = StoryLine("minji", "안녕하세요.", "Salom.")
    private val choose = ChooseReply("aziz", "P", listOf("네, 좋아요.", "네, 좋아요요.", "네, 좋으러 가요."), "네, 좋아요.", "Ha.", "W1")
    private val l2 = StoryLine("minji", "가요.", "Ketdik.")
    private val quiz = StoryQuiz("Q", listOf("Ha", "Yo'q", "Bilmayman"), "Ha", "W2")
    private val player = StoryPlayer(Story("T", listOf(l1, choose, l2, quiz)))

    @Test fun startsOnFirstLine() {
        assertEquals(l1, player.current)
        assertEquals(listOf(l1), player.shown)
        assertFalse(player.finished)
    }

    @Test fun nextRevealsFollowingStep() {
        val p = player.next()
        assertEquals(choose, p.current)
        assertEquals(listOf(l1, choose), p.shown)
    }

    @Test fun wrongChoiceEliminatesAndExplains() {
        val p = player.next().choose("네, 좋아요요.")
        assertEquals(choose, p.current)
        assertEquals(setOf("네, 좋아요요."), p.eliminated)
        assertEquals("W1", p.lastWrongWhy)
    }

    @Test fun rightChoiceAdvances() {
        val p = player.next().choose("네, 좋아요요.").choose("네, 좋아요.")
        assertEquals(l2, p.current)
        assertEquals(emptySet<String>(), p.eliminated)
        assertNull(p.lastWrongWhy)
        assertEquals(listOf(l1, choose, l2), p.shown)
    }

    @Test fun bothWrongOptionsEliminatedThenAnswerAdvances() {
        val p = player.next().choose("네, 좋아요요.").choose("네, 좋으러 가요.")
        assertEquals(setOf("네, 좋아요요.", "네, 좋으러 가요."), p.eliminated)
        assertEquals(l2, p.choose("네, 좋아요.").current)
    }

    @Test fun finishedAfterLastStep() {
        val p = player.next().choose("네, 좋아요.").next().choose("Ha")
        assertTrue(p.finished)
        assertNull(p.current)
        assertEquals(1f, p.progress, 0.0001f)
    }

    @Test fun progressCountsRevealedSteps() = assertEquals(0.25f, player.progress, 0.0001f)
}

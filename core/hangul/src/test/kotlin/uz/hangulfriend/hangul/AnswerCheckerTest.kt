package uz.hangulfriend.hangul

import org.junit.Assert.assertEquals
import org.junit.Test

class AnswerCheckerTest {
    private fun fb(input: String, vararg answers: String) =
        AnswerChecker.check(input, answers.toList()).feedback

    @Test fun correct_exact() = assertEquals(Feedback.CORRECT, fb("가면", "가면"))

    @Test fun correct_anyOfAnswers() = assertEquals(Feedback.CORRECT, fb("입어 봐요", "입어 보세요", "입어 봐요"))

    @Test fun correct_ignoresTrailingPunctuation() = assertEquals(Feedback.CORRECT, fb("가면.", "가면"))

    @Test fun spacing() = assertEquals(Feedback.SPACING, fb("입어보세요", "입어 보세요"))

    @Test fun finalConsonant() = assertEquals(Feedback.FINAL_CONSONANT, fb("갔면", "가면"))

    @Test fun vowel() = assertEquals(Feedback.VOWEL, fb("거면", "가면"))

    @Test fun oneLetter() = assertEquals(Feedback.ONE_LETTER, fb("가면ㅇ", "가면"))

    @Test fun wrong() = assertEquals(Feedback.WRONG, fb("학교", "가면"))

    @Test fun closest_picksNearest() =
        assertEquals("입어 봐요", AnswerChecker.check("입어 박요", listOf("입어 보세요", "입어 봐요")).closest)

    @Test fun correct_flagMatchesFeedback() {
        assertEquals(true, AnswerChecker.check("가면", listOf("가면")).correct)
        assertEquals(false, AnswerChecker.check("거면", listOf("가면")).correct)
    }
}

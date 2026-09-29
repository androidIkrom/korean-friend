package uz.hangulfriend.hangul

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeechScoreTest {
    @Test fun similarity_identical() = assertEquals(1.0, SpeechScorer.similarity("입어 보세요", "입어 보세요"), 1e-9)

    @Test fun similarity_ignoresSpacesAndPunctuation() =
        assertEquals(1.0, SpeechScorer.similarity("입어 보세요.", "입어보세요"), 1e-9)

    @Test fun similarity_oneJamoOff() = assertTrue(SpeechScorer.similarity("가면", "거면") in 0.7..0.9)

    @Test fun similarity_emptyHeardIsZero() = assertEquals(0.0, SpeechScorer.similarity("가면", ""), 1e-9)

    @Test fun score_picksBestCandidate() =
        assertEquals("입어 보세요", SpeechScorer.score("입어 보세요", listOf("이버 보세", "입어 보세요")).heard)

    @Test fun score_passThreshold() {
        assertTrue(SpeechScorer.score("치마를 입어 보세요", listOf("치마를 입어 보세요")).passed)
        assertFalse(SpeechScorer.score("치마를 입어 보세요", listOf("바지")).passed)
    }

    @Test fun score_marksMismatchedSyllables() =
        assertEquals(setOf(1), SpeechScorer.score("가면", listOf("가먼")).mismatched)

    @Test fun score_mismatchIndexesSkipSpaces() =
        assertEquals(setOf(3), SpeechScorer.score("입어 보세요", listOf("입어 바세요")).mismatched)

    @Test fun score_missingSyllableMarked() =
        assertEquals(setOf(4), SpeechScorer.score("입어 보세요", listOf("입어 보요")).mismatched)

    @Test fun score_emptyCandidates() {
        val s = SpeechScorer.score("가면", emptyList())
        assertEquals(0.0, s.similarity, 0.0)
        assertEquals("", s.heard)
        assertEquals(setOf(0, 1), s.mismatched)
    }
}

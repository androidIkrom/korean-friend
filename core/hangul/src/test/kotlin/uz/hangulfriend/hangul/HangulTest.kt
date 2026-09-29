package uz.hangulfriend.hangul

import java.text.Normalizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HangulTest {
    @Test fun parts_withFinal() = assertEquals(SyllableParts('ㄱ', 'ㅏ', 'ㅆ'), Hangul.parts('갔'))

    @Test fun parts_noFinal() = assertEquals(SyllableParts('ㅁ', 'ㅕ', null), Hangul.parts('며'))

    @Test fun parts_nonHangul() = assertNull(Hangul.parts('a'))

    @Test fun jamo_mixed() =
        assertEquals(listOf('ㄱ', 'ㅏ', ' ', 'ㅁ', 'ㅕ', 'ㄴ', '?'), Hangul.jamo("가 면?"))

    @Test fun normalize_spacesAndPunctuation() =
        assertEquals("입어 보세요", Hangul.normalize("  입어   보세요. "))

    @Test fun normalize_nfdInput() =
        assertEquals("가면", Hangul.normalize(Normalizer.normalize("가면", Normalizer.Form.NFD)))

    @Test fun normalize_latinLowercase() = assertEquals("abc", Hangul.normalize("ABC"))
}

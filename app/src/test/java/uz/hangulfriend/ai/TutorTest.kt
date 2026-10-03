package uz.hangulfriend.ai

import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import uz.hangulfriend.Fixtures
import uz.hangulfriend.i18n.AppLanguage

class TutorTest {
    private val lesson = Fixtures.validLesson()
    private val translate = lesson.exercises.first { it.id == "u02_l1_e006" }

    @Test fun grammarContext_containsPatternExamplesAndWords() {
        val ctx = TutorPrompts.grammarContext(lesson.grammar[0], lesson)
        assertTrue(ctx.contains("-아/어 보다"))
        assertTrue(ctx.contains("입어 보세요."))
        assertTrue(ctx.contains("치마 — yubka"))
        assertTrue(ctx.startsWith(TutorPrompts.ROLE))
    }

    @Test fun mistakeQuestion_containsAnswerAndCorrect() {
        val q = TutorPrompts.mistakeQuestion(translate, "입 보세요")
        assertTrue(q.contains("입 보세요"))
        assertTrue(q.contains("입어 보세요"))
        assertTrue(q.contains("Kiyib ko'ring"))
    }

    @Test fun englishPrompts() {
        assertTrue(TutorPrompts.role(AppLanguage.EN).contains("Answer only in English"))
        assertEquals(TutorPrompts.ROLE, TutorPrompts.role(AppLanguage.UZ))
        val q = TutorPrompts.mistakeQuestion(translate, "입 보세요", AppLanguage.EN)
        assertTrue(q.contains("Correct answer: 입어 보세요"))
        assertTrue(q.contains("My answer: 입 보세요"))
        val ctx = TutorPrompts.grammarContext(lesson.grammar[0], lesson, AppLanguage.EN)
        assertTrue(ctx.startsWith(TutorPrompts.ROLE_EN))
        assertTrue(ctx.contains("Lesson words:"))
        assertTrue(TutorPrompts.translationCheck(translate, "x", AppLanguage.EN).contains("Return only JSON"))
    }

    @Test fun cleanAiText_latex() {
        assertEquals("가다 → 가 보다", cleanAiText("가다 \$\\rightarrow\$ 가 보다"))
        assertEquals("A → B", cleanAiText("A \\to B"))
        assertEquals("x + y", cleanAiText("\$x + y\$"))
        assertEquals("30,000 won and 5 dollars", cleanAiText("30,000 won and 5 dollars"))
    }

    @Test fun promptsForbidLatexAndIncludeExplanation() {
        assertTrue(TutorPrompts.ROLE_EN.contains("LaTeX"))
        assertTrue(TutorPrompts.ROLE.contains("LaTeX"))
        assertTrue(TutorPrompts.grammarContext(lesson.grammar[0], lesson).contains(lesson.grammar[0].explanationMd))
    }

    @Test fun parseVerdict_valid() = assertEquals(
        TranslationVerdict(true, "입어 보세요", "To'g'ri"),
        parseVerdict("""{"correct":true,"corrected_ko":"입어 보세요","explanation_uz":"To'g'ri"}"""),
    )

    @Test fun parseVerdict_malformedIsNull() {
        assertNull(parseVerdict("Ha, to'g'ri"))
        assertNull(parseVerdict("""{"correct":"maybe"}"""))
    }

    @Test fun checkTranslation_usesJsonAndParses() = runTest {
        val bodies = mutableListOf<String>()
        val transport = object : GeminiTransport {
            override suspend fun post(model: String, key: String, body: String): HttpResult {
                bodies += body
                val json = """{\"correct\":false,\"corrected_ko\":\"입어 보세요\",\"explanation_uz\":\"-어 kerak\"}"""
                return HttpResult(200, """{"candidates":[{"content":{"parts":[{"text":"$json"}]}}]}""")
            }
        }
        val verdict = TutorService(GeminiClient(listOf("k1"), transport)).checkTranslation(translate, "입 보세요")
        assertEquals(TranslationVerdict(false, "입어 보세요", "-어 kerak"), verdict)
        assertTrue(bodies.single().contains("application/json"))
    }

    @Test fun checkTranslation_failureReturnsNull() = runTest {
        val transport = object : GeminiTransport {
            override suspend fun post(model: String, key: String, body: String): HttpResult = throw IOException()
        }
        assertNull(TutorService(GeminiClient(listOf("k1"), transport)).checkTranslation(translate, "x"))
    }
}

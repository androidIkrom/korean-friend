package uz.hangulfriend.content

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import uz.hangulfriend.Fixtures
import uz.hangulfriend.i18n.AppLanguage

class LocalizeTest {
    private val base = Fixtures.validLesson()

    /** [base] with every English sibling filled in. */
    private val english = base.copy(
        titleEn = "Try it on",
        topicEn = "Shopping",
        words = base.words.map { it.copy(en = "EN ${it.uz}", exampleEn = "EN ${it.exampleUz}", pos = "ot") },
        grammar = base.grammar.map { g ->
            g.copy(
                meaningEn = "to try",
                explanationMdEn = "**-아/어 보다** — trying.",
                compareEn = "English: 'try doing'",
                formation = g.formation.map { it.copy(conditionEn = "ㅏ/ㅗ vowel") },
                examples = g.examples.map { it.copy(en = "Try it on.") },
                mistakes = g.mistakes.map { it.copy(whyEn = "-어 is missing") },
            )
        },
        dialogue = Dialogue(base.dialogue.lines.map { it.copy(en = "May I try this skirt on?") }),
        exercises = base.exercises.map { e ->
            e.copy(
                promptEn = "Exercise",
                sourceEn = e.sourceUz?.let { "Try it on" },
                optionsEn = if (e.type == ExerciseType.LISTEN_QUESTION) listOf("Try it on", "Try eating it") else null,
            )
        },
        story = Story(
            titleUz = "Do'konda",
            titleEn = "At the shop",
            steps = listOf(
                StoryLine("minji", "안녕하세요", "Salom", en = "Hello"),
                ChooseReply("aziz", "Nima deydi?", listOf("네", "아니요", "몰라요"), "네", "Ha", "Rozi", promptEn = "What does he say?", en = "Yes", whyEn = "He agrees"),
                StoryQuiz("Kim?", listOf("Buvisi", "Onasi", "Opasi"), "Onasi", "Sabab", promptEn = "Who?", optionsEn = listOf("His grandmother", "His mother", "His sister"), whyEn = "Because"),
            ),
        ),
    )

    @Test fun uzbekIsUnchanged() = assertSame(english, english.localized(AppLanguage.UZ))

    @Test fun everyFieldSwaps() {
        val l = english.localized(AppLanguage.EN)
        assertEquals("Try it on", l.titleUz)
        assertEquals("Shopping", l.topicUz)
        assertEquals("EN yubka", l.words[0].uz)
        assertEquals("EN yubka bor.", l.words[0].exampleUz)
        assertEquals("noun", l.words[0].pos)
        val g = l.grammar[0]
        assertEquals("to try", g.meaningUz)
        assertEquals("**-아/어 보다** — trying.", g.explanationMd)
        assertEquals("English: 'try doing'", g.uzCompare)
        assertEquals("ㅏ/ㅗ vowel", g.formation[0].conditionUz)
        assertEquals("Try it on.", g.examples[0].uz)
        assertEquals("-어 is missing", g.mistakes[0].whyUz)
        assertEquals("May I try this skirt on?", l.dialogue.lines[0].uz)
        assertEquals("Exercise", l.exercises[0].promptUz)
        assertEquals("Try it on", l.exercises.first { it.type == ExerciseType.TRANSLATE }.sourceUz)
        val story = l.story!!
        assertEquals("At the shop", story.titleUz)
        assertEquals("Hello", (story.steps[0] as StoryLine).uz)
        val reply = story.steps[1] as ChooseReply
        assertEquals(listOf("What does he say?", "Yes", "He agrees"), listOf(reply.promptUz, reply.uz, reply.whyUz))
        assertEquals("네", reply.answer)
    }

    @Test fun missingEnglishFallsBack() {
        val l = base.localized(AppLanguage.EN)
        assertEquals(base.titleUz, l.titleUz)
        assertEquals(base.words, l.words)
        assertEquals(base.exercises, l.exercises)
    }

    @Test fun optionsSwapByIndexAndAnswersFollow() {
        val listen = english.localized(AppLanguage.EN).exercises.first { it.type == ExerciseType.LISTEN_QUESTION }
        assertEquals(listOf("Try it on", "Try eating it"), listen.options)
        assertEquals(listOf("Try it on"), listen.answers)
        val quiz = english.localized(AppLanguage.EN).story!!.steps[2] as StoryQuiz
        assertEquals(listOf("His grandmother", "His mother", "His sister"), quiz.options)
        assertEquals("His mother", quiz.answer)
        assertEquals(listOf("Who?", "Because"), listOf(quiz.promptUz, quiz.whyUz))
    }

    @Test fun koreanOptionsKeep() {
        val choice = english.localized(AppLanguage.EN).exercises.first { it.type == ExerciseType.SITUATION_CHOICE }
        assertEquals(listOf("치마", "바지"), choice.options)
        assertEquals(listOf("치마"), choice.answers)
    }

    @Test fun wrongSizeOptionsAreIgnored() {
        val e = Fixtures.exercise("x", ExerciseType.LISTEN_QUESTION, emptyList(), listOf("A")) {
            copy(options = listOf("A", "B"), optionsEn = listOf("only one"))
        }
        assertEquals(listOf("A", "B"), e.localized(AppLanguage.EN).options)
    }

    @Test fun posMaps() {
        assertEquals("verb", posLabelEn("fe'l"))
        assertEquals("determiner", posLabelEn("aniqlovchi"))
        assertEquals("other", posLabelEn("other"))
    }

    @Test fun catalogAndCharacters() {
        val e = CatalogEntry("u01_l1", 1, 1, "가족", "Oila", "Oila", titleEn = "Family", topicEn = "Family")
        assertEquals("Family", e.localized(AppLanguage.EN).titleUz)
        val c = Character("seller", "Sotuvchi", "점원", "female", nameEn = "Clerk")
        assertEquals("Clerk", c.localized(AppLanguage.EN).nameUz)
        assertEquals("Sotuvchi", c.localized(AppLanguage.UZ).nameUz)
    }

    @Test fun englishErrorsListsMissing() {
        assertEquals(emptyList<String>(), LessonValidator.englishErrors(english))
        val errors = LessonValidator.englishErrors(base)
        assert(errors.any { it == "u02_l1: title_en" }) { errors }
        assert(errors.any { it == "u02_l1_w001: en" }) { errors }
        assert(errors.any { it == "u02_l1_e008: options_en" }) { errors }
        assert(errors.none { it.startsWith("u02_l1_e001: options_en") }) { errors }
        val badSize = english.copy(
            exercises = english.exercises.map { if (it.type == ExerciseType.LISTEN_QUESTION) it.copy(optionsEn = listOf("one")) else it },
        )
        assert(LessonValidator.englishErrors(badSize).any { it == "u02_l1_e008: options_en" })
    }
}

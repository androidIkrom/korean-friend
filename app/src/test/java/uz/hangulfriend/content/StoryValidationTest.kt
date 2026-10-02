package uz.hangulfriend.content

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import uz.hangulfriend.Fixtures

class StoryValidationTest {
    private fun line(n: Int) = StoryLine("minji", "문장 $n", "Gap $n")

    private val choose = ChooseReply(
        speaker = "aziz",
        promptUz = "Aziz nima deydi?",
        options = listOf("네, 좋아요.", "네, 좋아해요요.", "네, 좋으러 가요."),
        answer = "네, 좋아요.",
        uz = "Ha, yaxshi.",
        whyUz = "좋다 → 좋아요.",
    )

    private val quiz = StoryQuiz("Minji qayerga bordi?", listOf("Do'kon", "Maktab", "Uy"), "Do'kon", "가게 = do'kon.")

    private fun story(steps: List<StoryStep>) = Story("Sinov", steps)

    private val valid = story((1..10).map(::line) + choose + quiz)

    private fun errors(s: Story) = LessonValidator.validate(listOf(Fixtures.validLesson().copy(story = s)))

    private fun assertStoryError(s: Story, fragment: String) {
        val errs = errors(s)
        assertTrue("expected \"$fragment\" in $errs", errs.any { it.startsWith("u02_l1: story:") && fragment in it })
    }

    @Test fun chooseReplyMustBeAziz() =
        assertStoryError(story((1..10).map(::line) + choose.copy(speaker = "minji") + quiz), "choose_reply speaker must be aziz")

    @Test fun storyValidPasses() = assertEquals(emptyList<String>(), errors(valid))

    @Test fun storyTooFewLines() = assertStoryError(story((1..9).map(::line) + choose + quiz), "10–16 line steps")

    @Test fun storyNoChooseReply() = assertStoryError(story((1..10).map(::line) + quiz + quiz), "needs a choose_reply")

    @Test fun storyTooManyInteractive() =
        assertStoryError(story((1..10).map(::line) + choose + quiz + quiz + quiz), "2–3 interactive steps")

    @Test fun storyFirstStepMustBeLine() =
        assertStoryError(story(listOf(choose) + (1..10).map(::line) + quiz), "first step must be a line")

    @Test fun storyAnswerNotInOptions() =
        assertStoryError(story((1..10).map(::line) + choose.copy(answer = "아니요") + quiz), "answer must be one of 3 distinct options")

    @Test fun storyDuplicateOptions() =
        assertStoryError(
            story((1..10).map(::line) + quiz.copy(options = listOf("Do'kon", "Do'kon", "Uy")) + choose),
            "answer must be one of 3 distinct options",
        )

    @Test fun storyParsesFromJson() {
        val json = """
            {"title_uz": "T", "steps": [
              {"type": "line", "speaker": "minji", "ko": "안녕하세요.", "uz": "Salom."},
              {"type": "choose_reply", "speaker": "aziz", "prompt_uz": "P", "options": ["a", "b", "c"],
               "answer": "a", "uz": "A", "why_uz": "W"},
              {"type": "quiz", "prompt_uz": "Q", "options": ["x", "y", "z"], "answer": "x", "why_uz": "W"}
            ]}
        """.trimIndent()
        val steps = ContentJson.decodeFromString<Story>(json).steps
        assertTrue(steps[0] is StoryLine && steps[1] is ChooseReply && steps[2] is StoryQuiz)
    }
}

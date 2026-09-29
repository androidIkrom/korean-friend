package uz.hangulfriend.content

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import uz.hangulfriend.Fixtures

class LessonValidatorTest {
    private val lesson = Fixtures.validLesson()

    private fun errorsFor(vararg lessons: Lesson) = LessonValidator.validate(lessons.toList())

    private fun assertErrorMentions(errors: List<String>, id: String) =
        assertTrue("expected error for $id in $errors", errors.any { it.startsWith("$id:") })

    private fun withExercise(id: String, change: Exercise.() -> Exercise) =
        lesson.copy(exercises = lesson.exercises.map { if (it.id == id) it.change() else it })

    @Test fun validate_validLesson_noErrors() = assertEquals(emptyList<String>(), errorsFor(lesson))

    @Test fun validate_duplicateIds() {
        val dup = lesson.copy(words = lesson.words + lesson.words.first())
        assertErrorMentions(errorsFor(dup), "u02_l1_w001")
    }

    @Test fun validate_duplicateIdsAcrossLessons() =
        assertErrorMentions(errorsFor(lesson, lesson.copy(id = "u02_l2")), "u02_l1_w001")

    @Test fun validate_danglingTarget() =
        assertErrorMentions(errorsFor(withExercise("u02_l1_e002") { copy(targets = listOf("nope")) }), "u02_l1_e002")

    @Test fun validate_danglingTestRef() =
        assertErrorMentions(errorsFor(lesson.copy(test = listOf("missing_e"))), "u02_l1")

    @Test fun validate_emptyAnswers() =
        assertErrorMentions(errorsFor(withExercise("u02_l1_e002") { copy(answers = emptyList()) }), "u02_l1_e002")

    @Test fun validate_conjugateNeedsBaseAndForm() =
        assertErrorMentions(errorsFor(withExercise("u02_l1_e002") { copy(form = null) }), "u02_l1_e002")

    @Test fun validate_fillBlankWithoutBlank() =
        assertErrorMentions(errorsFor(withExercise("u02_l1_e003") { copy(sentence = "이 옷을 입어 보세요") }), "u02_l1_e003")

    @Test fun validate_choiceAnswerNotInOptions() =
        assertErrorMentions(errorsFor(withExercise("u02_l1_e001") { copy(options = listOf("바지", "모자")) }), "u02_l1_e001")

    @Test fun validate_buildSentenceTokenMismatch() =
        assertErrorMentions(errorsFor(withExercise("u02_l1_e004") { copy(tokens = listOf("입어", "보세요")) }), "u02_l1_e004")

    @Test fun validate_findErrorNeedsSentence() =
        assertErrorMentions(errorsFor(withExercise("u02_l1_e005") { copy(sentence = null) }), "u02_l1_e005")

    @Test fun validate_translateNeedsSource() =
        assertErrorMentions(errorsFor(withExercise("u02_l1_e006") { copy(sourceUz = null) }), "u02_l1_e006")

    @Test fun validate_grammarWithoutPracticeExercise() {
        val onlyTest = lesson.copy(exercises = lesson.exercises.filter { it.id in setOf("u02_l1_e001", "u02_l1_e007") })
        assertErrorMentions(errorsFor(onlyTest), "u02_l1_g1")
    }
}

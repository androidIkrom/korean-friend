package uz.hangulfriend

import uz.hangulfriend.content.Dialogue
import uz.hangulfriend.content.Example
import uz.hangulfriend.content.Exercise
import uz.hangulfriend.content.ExerciseType
import uz.hangulfriend.content.Formation
import uz.hangulfriend.content.Grammar
import uz.hangulfriend.content.Lesson
import uz.hangulfriend.content.Line
import uz.hangulfriend.content.Mistake
import uz.hangulfriend.content.Word

object Fixtures {
    fun word(id: String, ko: String, uz: String) =
        Word(id = id, ko = ko, uz = uz, pos = "noun", exampleKo = "$ko 있어요.", exampleUz = "$uz bor.")

    fun exercise(
        id: String,
        type: ExerciseType,
        targets: List<String>,
        answers: List<String>,
        block: Exercise.() -> Exercise = { this },
    ) = Exercise(id = id, type = type, targets = targets, promptUz = "Mashq", answers = answers).block()

    /** Two words, one grammar point, one practice exercise of every type plus one test exercise. */
    fun validLesson(id: String = "u02_l1", unit: Int = 2, lesson: Int = 1): Lesson {
        val w1 = "${id}_w001"
        val w2 = "${id}_w002"
        val g1 = "${id}_g1"
        return Lesson(
            id = id,
            unit = unit,
            lesson = lesson,
            titleKo = "한번 입어 보세요",
            titleUz = "Kiyib ko'ring",
            topicUz = "Xarid",
            reviewed = false,
            words = listOf(word(w1, "치마", "yubka"), word(w2, "바지", "shim")),
            grammar = listOf(
                Grammar(
                    id = g1,
                    pattern = "-아/어 보다",
                    meaningUz = "sinab ko'rmoq",
                    explanationMd = "**-아/어 보다** — sinab ko'rish.",
                    formation = listOf(Formation("ㅏ/ㅗ", "-아 보다", "가 보다")),
                    examples = listOf(Example("입어 보세요.", "Kiyib ko'ring.")),
                    mistakes = listOf(Mistake("입 보세요", "입어 보세요", "-어 tushib qolgan")),
                    uzCompare = "-ib ko'rmoq",
                ),
            ),
            dialogue = Dialogue(listOf(Line("aziz", "이 치마 입어 봐도 돼요?", "Bu yubkani kiyib ko'rsam bo'ladimi?"))),
            exercises = listOf(
                exercise("${id}_e001", ExerciseType.SITUATION_CHOICE, listOf(w1), listOf("치마")) {
                    copy(options = listOf("치마", "바지"))
                },
                exercise("${id}_e002", ExerciseType.CONJUGATE, listOf(g1), listOf("입어 보다")) {
                    copy(base = "입다", form = "-아/어 보다")
                },
                exercise("${id}_e003", ExerciseType.FILL_BLANK, listOf(g1), listOf("입어 보세요")) {
                    copy(sentence = "이 옷을 ___ (입다)")
                },
                exercise("${id}_e004", ExerciseType.BUILD_SENTENCE, listOf(g1), listOf("치마를 입어 보세요")) {
                    copy(tokens = listOf("입어", "치마를", "보세요"))
                },
                exercise("${id}_e005", ExerciseType.FIND_ERROR, listOf(g1), listOf("입어 보세요")) {
                    copy(sentence = "입 보세요")
                },
                exercise("${id}_e006", ExerciseType.TRANSLATE, listOf(g1), listOf("입어 보세요")) {
                    copy(sourceUz = "Kiyib ko'ring")
                },
                exercise("${id}_e007", ExerciseType.CONJUGATE, listOf(g1), listOf("가 보다")) {
                    copy(base = "가다", form = "-아/어 보다")
                },
            ),
            test = listOf("${id}_e007"),
        )
    }
}

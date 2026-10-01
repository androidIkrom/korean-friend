package uz.hangulfriend.content

/** Checks content rules that the JSON schema alone cannot express. Errors are "<id>: <reason>". */
object LessonValidator {
    fun validate(lessons: List<Lesson>): List<String> {
        val errors = mutableListOf<String>()
        val seen = mutableSetOf<String>()
        for (lesson in lessons) {
            val ids = lesson.words.map { it.id } + lesson.grammar.map { it.id } + lesson.exercises.map { it.id }
            for (id in ids) if (!seen.add(id)) errors += "$id: duplicate id"
            validateLesson(lesson, errors)
        }
        return errors
    }

    private fun validateLesson(lesson: Lesson, errors: MutableList<String>) {
        val itemIds = lesson.words.map { it.id }.toSet() + lesson.grammar.map { it.id }
        val exerciseIds = lesson.exercises.map { it.id }.toSet()
        val testIds = lesson.test.toSet()

        for (ref in lesson.test) if (ref !in exerciseIds) errors += "${lesson.id}: test references unknown exercise $ref"

        for (e in lesson.exercises) {
            fun err(reason: String) {
                errors += "${e.id}: $reason"
            }
            for (t in e.targets) if (t !in itemIds) err("target $t is not a word or grammar of ${lesson.id}")
            if (e.answers.isEmpty()) err("answers is empty")
            when (e.type) {
                ExerciseType.CONJUGATE -> if (e.base == null || e.form == null) err("conjugate needs base and form")
                ExerciseType.FILL_BLANK -> if (e.sentence?.contains("___") != true) err("fill_blank sentence needs ___")
                ExerciseType.SITUATION_CHOICE -> {
                    val options = e.options.orEmpty()
                    if (options.size < 2) err("situation_choice needs at least 2 options")
                    if (!options.containsAll(e.answers)) err("every answer must be one of the options")
                }
                ExerciseType.BUILD_SENTENCE -> {
                    val expected = e.answers.firstOrNull()?.split(" ")?.sorted()
                    if (e.tokens?.sorted() != expected) err("tokens must be the words of answers[0]")
                }
                ExerciseType.FIND_ERROR -> if (e.sentence == null) err("find_error needs sentence")
                ExerciseType.TRANSLATE -> if (e.sourceUz == null) err("translate needs source_uz")
                ExerciseType.LISTEN_QUESTION -> {
                    if (e.audioText == null) err("listen_question needs audio_text")
                    val options = e.options.orEmpty()
                    if (options.size < 2) err("listen_question needs at least 2 options")
                    if (!options.containsAll(e.answers)) err("every answer must be one of the options")
                }
            }
        }

        val practised = lesson.exercises.filter { it.id !in testIds }.flatMap { it.targets }.toSet()
        for (g in lesson.grammar) if (g.id !in practised) errors += "${g.id}: no practice exercise targets this grammar"

        lesson.story?.let { validateStory(lesson.id, it, errors) }
    }

    private fun validateStory(lessonId: String, story: Story, errors: MutableList<String>) {
        fun err(reason: String) {
            errors += "$lessonId: story: $reason"
        }
        val lines = story.steps.count { it is StoryLine }
        val interactive = story.steps.size - lines
        if (lines !in 10..16) err("10–16 line steps (has $lines)")
        if (interactive !in 2..3) err("2–3 interactive steps (has $interactive)")
        if (story.steps.none { it is ChooseReply }) err("needs a choose_reply")
        if (story.steps.firstOrNull() !is StoryLine) err("first step must be a line")
        story.steps.forEachIndexed { i, step ->
            val (options, answer) = when (step) {
                is ChooseReply -> step.options to step.answer
                is StoryQuiz -> step.options to step.answer
                is StoryLine -> return@forEachIndexed
            }
            if (options.size != 3 || options.toSet().size != 3 || answer !in options) {
                err("step ${i + 1}: answer must be one of 3 distinct options")
            }
        }
    }
}

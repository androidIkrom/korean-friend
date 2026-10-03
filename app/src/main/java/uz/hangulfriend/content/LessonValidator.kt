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
            for (t in e.targets) if (t !in itemIds) errors += "${e.id}: target $t is not a word or grammar of ${lesson.id}"
            errors += exerciseErrors(e)
        }

        val practised = lesson.exercises.filter { it.id !in testIds }.flatMap { it.targets }.toSet()
        for (g in lesson.grammar) if (g.id !in practised) errors += "${g.id}: no practice exercise targets this grammar"

        lesson.story?.let { validateStory(lesson.id, it, errors) }
    }

    /** Shape rules of one exercise, independent of its lesson (also used for the final test). */
    fun exerciseErrors(e: Exercise): List<String> {
        val errors = mutableListOf<String>()
        fun err(reason: String) {
            errors += "${e.id}: $reason"
        }
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
                if (e.audioText == null && e.audioDialogue.isNullOrEmpty()) err("listen_question needs audio_text or audio_dialogue")
                val options = e.options.orEmpty()
                if (options.size < 2) err("listen_question needs at least 2 options")
                if (!options.containsAll(e.answers)) err("every answer must be one of the options")
            }
            ExerciseType.READ_CHOICE -> {
                if (e.sentence.isNullOrBlank()) err("read_choice needs a passage in sentence")
                val options = e.options.orEmpty()
                if (options.size != 4 || options.toSet().size != 4) err("read_choice needs 4 distinct options")
                if (!options.containsAll(e.answers)) err("every answer must be one of the options")
            }
        }
        return errors
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
        // The learner always replies as Aziz, the story's hero.
        if (story.steps.any { it is ChooseReply && it.speaker != "aziz" }) err("choose_reply speaker must be aziz")
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

    // --- English (stage 8): every Uzbek text needs its `_en` sibling. Errors are "<id>: <field>". ---

    fun englishErrors(lesson: Lesson): List<String> {
        val errors = mutableListOf<String>()
        val need = EnglishCheck(errors)
        need(lesson.id, "title_en", lesson.titleUz, lesson.titleEn)
        need(lesson.id, "topic_en", lesson.topicUz, lesson.topicEn)
        for (w in lesson.words) {
            need(w.id, "en", w.uz, w.en)
            need(w.id, "example_en", w.exampleUz, w.exampleEn)
        }
        for (g in lesson.grammar) {
            need(g.id, "meaning_en", g.meaningUz, g.meaningEn)
            need(g.id, "explanation_md_en", g.explanationMd, g.explanationMdEn)
            need(g.id, "compare_en", g.uzCompare, g.compareEn)
            g.formation.forEachIndexed { i, f -> need(g.id, "formation[$i].condition_en", f.conditionUz, f.conditionEn) }
            g.examples.forEachIndexed { i, e -> need(g.id, "examples[$i].en", e.uz, e.en) }
            g.mistakes.forEachIndexed { i, m -> need(g.id, "mistakes[$i].why_en", m.whyUz, m.whyEn) }
        }
        lesson.dialogue.lines.forEachIndexed { i, l -> need(lesson.id, "dialogue[$i].en", l.uz, l.en) }
        lesson.exercises.forEach { errors += exerciseEnglishErrors(it) }
        lesson.story?.let { s ->
            need(lesson.id, "story.title_en", s.titleUz, s.titleEn)
            s.steps.forEachIndexed { i, step ->
                val at = "${lesson.id}: story[$i]"
                when (step) {
                    is StoryLine -> need(at, "en", step.uz, step.en)
                    is ChooseReply -> {
                        need(at, "prompt_en", step.promptUz, step.promptEn)
                        need(at, "en", step.uz, step.en)
                        need(at, "why_en", step.whyUz, step.whyEn)
                    }
                    is StoryQuiz -> {
                        need(at, "prompt_en", step.promptUz, step.promptEn)
                        need(at, "why_en", step.whyUz, step.whyEn)
                        need.options(at, step.options, step.optionsEn)
                    }
                }
            }
        }
        return errors
    }

    fun exerciseEnglishErrors(e: Exercise): List<String> {
        val errors = mutableListOf<String>()
        val need = EnglishCheck(errors)
        need(e.id, "prompt_en", e.promptUz, e.promptEn)
        need(e.id, "source_en", e.sourceUz, e.sourceEn)
        need(e.id, "hint_en", e.hintUz, e.hintEn)
        need(e.id, "why_en", e.whyUz, e.whyEn)
        e.options?.let { need.options(e.id, it, e.optionsEn) }
        return errors
    }

    fun englishErrors(test: FinalTest): List<String> = (test.listening + test.reading).flatMap { exerciseEnglishErrors(it) }

    fun englishErrorsCatalog(entries: List<CatalogEntry>): List<String> {
        val errors = mutableListOf<String>()
        val need = EnglishCheck(errors)
        for (e in entries) {
            need(e.id, "title_en", e.titleUz, e.titleEn)
            need(e.id, "topic_en", e.topicUz, e.topicEn)
        }
        return errors
    }

    fun englishErrorsCharacters(characters: List<Character>): List<String> {
        val errors = mutableListOf<String>()
        val need = EnglishCheck(errors)
        for (c in characters) need(c.id, "name_en", c.nameUz, c.nameEn)
        return errors
    }

    private class EnglishCheck(private val errors: MutableList<String>) {
        /** [at] is an id ("u01_l1_w001") or an already prefixed location ("u01_l1: story[2]"). */
        private fun report(at: String, field: String) {
            errors += if (at.contains(": ")) "$at.$field" else "$at: $field"
        }

        operator fun invoke(at: String, field: String, uz: String?, en: String?) {
            if (!uz.isNullOrBlank() && en.isNullOrBlank()) report(at, field)
        }

        /** Options written without Hangul are Uzbek and need an English list of the same size. */
        fun options(at: String, options: List<String>, english: List<String>?) {
            if (options.isEmpty() || options.any { it.hasHangul() }) return
            if (english == null || english.size != options.size || english.any { it.isBlank() }) report(at, "options_en")
        }
    }

    private fun String.hasHangul() = any { it in '가'..'힣' || it in 'ㄱ'..'ㆎ' }
}

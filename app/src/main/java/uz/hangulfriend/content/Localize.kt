package uz.hangulfriend.content

import uz.hangulfriend.i18n.AppLanguage

/*
 * Content is written in Uzbek with English in `*_en` siblings (stage 8). In English mode the English
 * text is copied over the Uzbek fields, so screens read `uz`, `whyUz`, … without knowing the
 * language. A missing English value keeps the Uzbek text instead of showing a blank.
 */

private fun String.or(en: String?): String = en ?: this

fun Lesson.localized(lang: AppLanguage): Lesson = if (lang == AppLanguage.UZ) this else copy(
    titleUz = titleUz.or(titleEn),
    topicUz = topicUz.or(topicEn),
    words = words.map { it.copy(uz = it.uz.or(it.en), exampleUz = it.exampleUz.or(it.exampleEn), pos = posLabelEn(it.pos)) },
    grammar = grammar.map { it.localizedEn() },
    dialogue = Dialogue(dialogue.lines.map { it.copy(uz = it.uz.or(it.en)) }),
    exercises = exercises.map { it.localized(lang) },
    story = story?.let { s -> s.copy(titleUz = s.titleUz.or(s.titleEn), steps = s.steps.map { it.localizedEn() }) },
)

private fun Grammar.localizedEn() = copy(
    meaningUz = meaningUz.or(meaningEn),
    explanationMd = explanationMd.or(explanationMdEn),
    uzCompare = uzCompare.or(compareEn),
    formation = formation.map { it.copy(conditionUz = it.conditionUz.or(it.conditionEn)) },
    examples = examples.map { it.copy(uz = it.uz.or(it.en)) },
    mistakes = mistakes.map { it.copy(whyUz = it.whyUz.or(it.whyEn)) },
)

private fun StoryStep.localizedEn(): StoryStep = when (this) {
    is StoryLine -> copy(uz = uz.or(en))
    is ChooseReply -> copy(promptUz = promptUz.or(promptEn), uz = uz.or(en), whyUz = whyUz.or(whyEn))
    is StoryQuiz -> {
        val swap = optionSwap(options, optionsEn)
        copy(promptUz = promptUz.or(promptEn), options = options.map(swap), answer = swap(answer), whyUz = whyUz.or(whyEn))
    }
}

fun Exercise.localized(lang: AppLanguage): Exercise {
    if (lang == AppLanguage.UZ) return this
    val swap = optionSwap(options.orEmpty(), optionsEn)
    return copy(
        promptUz = promptUz.or(promptEn),
        sourceUz = sourceUz?.or(sourceEn),
        hintUz = hintUz?.or(hintEn),
        whyUz = whyUz?.or(whyEn),
        options = options?.map(swap),
        answers = answers.map(swap),
    )
}

/**
 * Maps each Uzbek option to the English one at the same index; answers go through the same map so
 * grading still matches the picked option. Anything else (Korean answers) passes through.
 */
private fun optionSwap(options: List<String>, english: List<String>?): (String) -> String {
    if (english == null || english.size != options.size) return { it }
    val map = options.zip(english).toMap()
    return { map[it] ?: it }
}

fun FinalTest.localized(lang: AppLanguage): FinalTest =
    if (lang == AppLanguage.UZ) this else FinalTest(listening.map { it.localized(lang) }, reading.map { it.localized(lang) })

fun CatalogEntry.localized(lang: AppLanguage): CatalogEntry =
    if (lang == AppLanguage.UZ) this else copy(titleUz = titleUz.or(titleEn), topicUz = topicUz.or(topicEn))

fun Character.localized(lang: AppLanguage): Character = if (lang == AppLanguage.UZ) this else copy(nameUz = nameUz.or(nameEn))

private val POS_EN = mapOf(
    "ot" to "noun",
    "fe'l" to "verb",
    "sifat" to "adjective",
    "ravish" to "adverb",
    "ibora" to "phrase",
    "olmosh" to "pronoun",
    "aniqlovchi" to "determiner",
)

/** English name of an Uzbek part-of-speech label; unknown labels stay as they are. */
fun posLabelEn(pos: String): String = POS_EN[pos] ?: pos

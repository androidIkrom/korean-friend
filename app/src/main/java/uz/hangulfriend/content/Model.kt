package uz.hangulfriend.content

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonClassDiscriminator

/** Strict JSON for content assets: an unknown key is a typo, not something to skip. */
val ContentJson = Json {
    ignoreUnknownKeys = false
    prettyPrint = true
}

/**
 * One lesson file. English lives in nullable `*_en` siblings; [localized] copies it into the
 * `uz`-named fields, so after loading those fields hold the learner's chosen language.
 */
@Serializable
data class Lesson(
    val id: String,
    val unit: Int,
    val lesson: Int,
    @SerialName("title_ko") val titleKo: String,
    @SerialName("title_uz") val titleUz: String,
    @SerialName("topic_uz") val topicUz: String,
    val reviewed: Boolean,
    val words: List<Word>,
    val grammar: List<Grammar>,
    val dialogue: Dialogue,
    val exercises: List<Exercise>,
    val test: List<String>,
    val story: Story? = null,
    @SerialName("title_en") val titleEn: String? = null,
    @SerialName("topic_en") val topicEn: String? = null,
)

@Serializable
data class Word(
    val id: String,
    val ko: String,
    val uz: String,
    val pos: String,
    @SerialName("example_ko") val exampleKo: String,
    @SerialName("example_uz") val exampleUz: String,
    val audio: String? = null,
    @SerialName("example_audio") val exampleAudio: String? = null,
    val en: String? = null,
    @SerialName("example_en") val exampleEn: String? = null,
)

@Serializable
data class Grammar(
    val id: String,
    val pattern: String,
    @SerialName("meaning_uz") val meaningUz: String,
    @SerialName("explanation_md") val explanationMd: String,
    val formation: List<Formation>,
    val examples: List<Example>,
    val mistakes: List<Mistake>,
    @SerialName("uz_compare") val uzCompare: String,
    @SerialName("meaning_en") val meaningEn: String? = null,
    @SerialName("explanation_md_en") val explanationMdEn: String? = null,
    /** Compares the pattern with English grammar; not a translation of [uzCompare]. */
    @SerialName("compare_en") val compareEn: String? = null,
)

@Serializable
data class Formation(
    @SerialName("condition_uz") val conditionUz: String,
    val rule: String,
    val example: String,
    @SerialName("condition_en") val conditionEn: String? = null,
)

@Serializable
data class Example(val ko: String, val uz: String, val audio: String? = null, val en: String? = null)

@Serializable
data class Mistake(
    val wrong: String,
    val right: String,
    @SerialName("why_uz") val whyUz: String,
    @SerialName("why_en") val whyEn: String? = null,
)

@Serializable
data class Dialogue(val lines: List<Line>)

@Serializable
data class Line(val speaker: String, val ko: String, val uz: String, val audio: String? = null, val en: String? = null)

@Serializable
data class Story(
    @SerialName("title_uz") val titleUz: String,
    val steps: List<StoryStep>,
    @SerialName("title_en") val titleEn: String? = null,
)

/** One beat of a story episode; the JSON `type` field picks the subclass. */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("type")
sealed interface StoryStep

@Serializable
@SerialName("line")
data class StoryLine(
    val speaker: String,
    val ko: String,
    val uz: String,
    val audio: String? = null,
    val en: String? = null,
) : StoryStep

/** The learner picks what [speaker] (Aziz) says; once solved it is shown and played like a line of [answer]. */
@Serializable
@SerialName("choose_reply")
data class ChooseReply(
    val speaker: String,
    @SerialName("prompt_uz") val promptUz: String,
    val options: List<String>,
    val answer: String,
    val uz: String,
    @SerialName("why_uz") val whyUz: String,
    val audio: String? = null,
    @SerialName("prompt_en") val promptEn: String? = null,
    val en: String? = null,
    @SerialName("why_en") val whyEn: String? = null,
) : StoryStep

/** Comprehension check; options are Uzbek, [optionsEn] the same options in English. */
@Serializable
@SerialName("quiz")
data class StoryQuiz(
    @SerialName("prompt_uz") val promptUz: String,
    val options: List<String>,
    val answer: String,
    @SerialName("why_uz") val whyUz: String,
    @SerialName("prompt_en") val promptEn: String? = null,
    @SerialName("options_en") val optionsEn: List<String>? = null,
    @SerialName("why_en") val whyEn: String? = null,
) : StoryStep

@Serializable
enum class ExerciseType {
    @SerialName("situation_choice") SITUATION_CHOICE,
    @SerialName("conjugate") CONJUGATE,
    @SerialName("fill_blank") FILL_BLANK,
    @SerialName("build_sentence") BUILD_SENTENCE,
    @SerialName("find_error") FIND_ERROR,
    @SerialName("translate") TRANSLATE,
    @SerialName("listen_question") LISTEN_QUESTION,

    /** Read a Korean passage ([Exercise.sentence]) and pick the answer (final test reading part). */
    @SerialName("read_choice") READ_CHOICE,
}

@Serializable
data class Exercise(
    val id: String,
    val type: ExerciseType,
    val targets: List<String>,
    @SerialName("prompt_uz") val promptUz: String,
    val answers: List<String>,
    val sentence: String? = null,
    val base: String? = null,
    val form: String? = null,
    val options: List<String>? = null,
    val tokens: List<String>? = null,
    @SerialName("source_uz") val sourceUz: String? = null,
    @SerialName("hint_uz") val hintUz: String? = null,
    @SerialName("why_uz") val whyUz: String? = null,
    @SerialName("audio_text") val audioText: String? = null,
    /** Two-voice dialogue lines (female, male, …) for listening questions; [audioText] keeps the full transcript. */
    @SerialName("audio_dialogue") val audioDialogue: List<String>? = null,
    val audio: String? = null,
    @SerialName("prompt_en") val promptEn: String? = null,
    /** English versions of Uzbek [options] (listening and reading questions), same order. */
    @SerialName("options_en") val optionsEn: List<String>? = null,
    @SerialName("source_en") val sourceEn: String? = null,
    @SerialName("hint_en") val hintEn: String? = null,
    @SerialName("why_en") val whyEn: String? = null,
)

@Serializable
data class CatalogEntry(
    val id: String,
    val unit: Int,
    val lesson: Int,
    @SerialName("title_ko") val titleKo: String,
    @SerialName("title_uz") val titleUz: String,
    @SerialName("topic_uz") val topicUz: String,
    @SerialName("title_en") val titleEn: String? = null,
    @SerialName("topic_en") val topicEn: String? = null,
)

@Serializable
data class Character(
    val id: String,
    @SerialName("name_uz") val nameUz: String,
    @SerialName("name_ko") val nameKo: String,
    val voice: String,
    @SerialName("name_en") val nameEn: String? = null,
)

@Serializable
internal data class Catalog(val lessons: List<CatalogEntry>)

@Serializable
internal data class Characters(val characters: List<Character>)

/** The book's closing TOPIK I-style test (stage 7c): listening first, then reading. */
@Serializable
data class FinalTest(val listening: List<Exercise>, val reading: List<Exercise>)

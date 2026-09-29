package uz.hangulfriend.content

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Strict JSON for content assets: an unknown key is a typo, not something to skip. */
val ContentJson = Json {
    ignoreUnknownKeys = false
    prettyPrint = true
}

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
)

@Serializable
data class Formation(
    @SerialName("condition_uz") val conditionUz: String,
    val rule: String,
    val example: String,
)

@Serializable
data class Example(val ko: String, val uz: String, val audio: String? = null)

@Serializable
data class Mistake(val wrong: String, val right: String, @SerialName("why_uz") val whyUz: String)

@Serializable
data class Dialogue(val lines: List<Line>)

@Serializable
data class Line(val speaker: String, val ko: String, val uz: String, val audio: String? = null)

@Serializable
data class Story(@SerialName("title_uz") val titleUz: String, val lines: List<Line>)

@Serializable
enum class ExerciseType {
    @SerialName("situation_choice") SITUATION_CHOICE,
    @SerialName("conjugate") CONJUGATE,
    @SerialName("fill_blank") FILL_BLANK,
    @SerialName("build_sentence") BUILD_SENTENCE,
    @SerialName("find_error") FIND_ERROR,
    @SerialName("translate") TRANSLATE,
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
)

@Serializable
data class CatalogEntry(
    val id: String,
    val unit: Int,
    val lesson: Int,
    @SerialName("title_ko") val titleKo: String,
    @SerialName("title_uz") val titleUz: String,
    @SerialName("topic_uz") val topicUz: String,
)

@Serializable
data class Character(
    val id: String,
    @SerialName("name_uz") val nameUz: String,
    @SerialName("name_ko") val nameKo: String,
    val voice: String,
)

@Serializable
internal data class Catalog(val lessons: List<CatalogEntry>)

@Serializable
internal data class Characters(val characters: List<Character>)

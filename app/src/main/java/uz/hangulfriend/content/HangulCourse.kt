package uz.hangulfriend.content

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import uz.hangulfriend.i18n.AppLanguage
import uz.hangulfriend.study.LessonId

/** The Hangul course of book 1, unit 1 (`assets/hangul.json`): every letter, and the letters of each lesson. */
@Serializable
data class HangulCourse(val letters: List<Letter>, val lessons: List<HangulLesson>) {
    private val byId by lazy { letters.associateBy { it.id } }

    fun letter(id: String): Letter? = byId[id]

    /** Letters of [lessonId], in course order; empty for an unknown lesson. */
    fun lettersOf(lessonId: String): List<Letter> =
        lessons.find { it.id == lessonId }?.letters.orEmpty().mapNotNull { byId[it] }

    /** Letters of [lessonId] and of every lesson before it. */
    fun knownBy(lessonId: String): List<Letter> {
        val upTo = lessons.indexOfFirst { it.id == lessonId }
        if (upTo < 0) return emptyList()
        return lessons.take(upTo + 1).flatMap { it.letters }.mapNotNull { byId[it] }
    }
}

@Serializable
data class HangulLesson(val id: String, val letters: List<String>)

object LetterKind {
    const val VOWEL = "vowel"
    const val CONSONANT = "consonant"
    const val FINAL = "final"
}

/**
 * One letter. [say] is the syllable its audio speaks (ㄱ as 가, final ㄱ as 악), since a bare jamo
 * would be read out by its name. [roman] is the Revised Romanization.
 */
@Serializable
data class Letter(
    val id: String,
    val jamo: String,
    val kind: String,
    val roman: String,
    val say: String,
    @SerialName("name_ko") val nameKo: String,
    @SerialName("tip_uz") val tipUz: String,
    @SerialName("tip_en") val tipEn: String? = null,
    val example: LetterExample,
    val audio: String? = null,
) {
    /** FSRS item id of the letter's card. */
    val cardItemId: String get() = HANGUL_ITEM_PREFIX + id
}

@Serializable
data class LetterExample(
    val ko: String,
    val roman: String,
    val uz: String,
    val en: String? = null,
    val audio: String? = null,
)

/** The letter with its name and sound, for flags: "ㄱ (기역) — g/k". */
fun Letter.label(): String = "$jamo ($nameKo) — $roman"

const val HANGUL_ITEM_PREFIX = "h_"

/** Hangul lessons are book 1, unit 1. */
fun isHangulLesson(id: String?): Boolean = id?.let { LessonId.parse(it) }?.let { it.book == 1 && it.unit == 1 } == true

val Lesson.isHangul: Boolean get() = isHangulLesson(id)

/** The letter as a word, so cards, reviews and loot treat it like any other word. */
fun Letter.asWord(): Word = Word(
    id = cardItemId,
    ko = jamo,
    uz = roman,
    pos = kind,
    exampleKo = example.ko,
    exampleUz = example.uz,
    audio = audio,
    exampleAudio = example.audio,
)

fun HangulCourse.localized(lang: AppLanguage): HangulCourse = if (lang == AppLanguage.UZ) this else copy(
    letters = letters.map { l ->
        l.copy(tipUz = l.tipEn ?: l.tipUz, example = l.example.copy(uz = l.example.en ?: l.example.uz))
    },
)

/** A Hangul lesson as an ordinary lesson whose words are its letters; null when the course does not list it. */
fun HangulCourse.lesson(entry: CatalogEntry): Lesson? {
    val letters = lettersOf(entry.id).ifEmpty { return null }
    return Lesson(
        id = entry.id,
        unit = entry.unit,
        lesson = entry.lesson,
        titleKo = entry.titleKo,
        titleUz = entry.titleUz,
        topicUz = entry.topicUz,
        reviewed = false,
        words = letters.map { it.asWord() },
        grammar = emptyList(),
        dialogue = Dialogue(emptyList()),
        exercises = emptyList(),
        test = emptyList(),
    )
}

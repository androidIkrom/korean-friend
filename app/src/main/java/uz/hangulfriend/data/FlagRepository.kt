package uz.hangulfriend.data

import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import uz.hangulfriend.content.Word
import uz.hangulfriend.study.ExerciseItem

/** Why a learner flagged an exercise ("Xato bor", stage 7b spec §2). */
enum class FlagReason { TRANSLATION, AUDIO, ANSWER_REJECTED, TYPO, OTHER }

private const val MAX_COMMENT = 300

/** What the flag points at: the exercise id, the word id(s), or the spoken sentence. */
fun flagRef(item: ExerciseItem): String = when (item) {
    is ExerciseItem.Flashcard -> item.word.id
    is ExerciseItem.WordTyping -> item.word.id
    is ExerciseItem.ListenChoose -> item.word.id
    is ExerciseItem.Dictation -> item.word.id
    is ExerciseItem.Speak -> item.ko
    is ExerciseItem.Match -> item.words.joinToString(",") { it.id }
    is ExerciseItem.Authored -> item.exercise.id
}

private fun Word.pair() = "$ko — $uz"

/** The text the learner saw, so a report stays readable even if the content later changes. */
fun flagSnapshot(item: ExerciseItem): String = when (item) {
    is ExerciseItem.Flashcard -> item.word.pair()
    is ExerciseItem.WordTyping -> item.word.pair()
    is ExerciseItem.ListenChoose -> item.word.pair()
    is ExerciseItem.Dictation -> item.word.pair()
    is ExerciseItem.Speak -> "${item.ko} — ${item.uz}"
    is ExerciseItem.Match -> item.words.joinToString("; ") { it.pair() }
    is ExerciseItem.Authored -> {
        val e = item.exercise
        val body = e.sentence ?: e.sourceUz ?: if (e.base != null) "${e.base} · ${e.form.orEmpty()}" else null
        listOfNotNull(e.promptUz, body, "Javob: " + e.answers.joinToString(" / ")).joinToString(" | ")
    }
}

/** `u02_l1_e07` → `u02_l1`; own words → `user`; anything else → "". */
fun lessonOfRef(ref: String): String = when {
    ref.startsWith("user_") -> USER_LESSON_ID
    else -> Regex("^u\\d{2}_l\\d").find(ref)?.value.orEmpty()
}

/**
 * The readable report shared from Settings: grouped by lesson in book order, then own words, then the rest;
 * numbered per group, oldest first.
 */
fun exportText(flags: List<ContentFlagEntity>, appVersion: String, today: LocalDate, reasonLabel: (FlagReason) -> String): String {
    fun groupKey(lessonId: String) = when (lessonId) {
        "" -> "2"
        USER_LESSON_ID -> "1"
        else -> "0$lessonId"
    }
    fun header(lessonId: String) = when (lessonId) {
        "" -> "[?] Boshqa"
        USER_LESSON_ID -> "[user] O'z so'zlarim"
        else -> {
            val unit = lessonId.substring(1, 3).toIntOrNull()
            val lesson = lessonId.substringAfter("_l").toIntOrNull()
            "[$lessonId] $unit-bo'lim, $lesson-dars"
        }
    }
    return buildString {
        appendLine("Hangul Hunt — kontent xatolari")
        appendLine("Sana: $today · Ilova: $appVersion · Jami: ${flags.size}")
        flags.groupBy { it.lessonId }.toSortedMap(compareBy { groupKey(it) }).forEach { (lessonId, group) ->
            appendLine()
            appendLine(header(lessonId))
            group.sortedWith(compareBy({ it.createdMs }, { it.id })).forEachIndexed { i, f ->
                val reason = FlagReason.entries.firstOrNull { it.name == f.reason } ?: FlagReason.OTHER
                appendLine("${i + 1}) ${f.ref} · ${f.type} · ${reasonLabel(reason)}")
                appendLine("   Savol: ${f.snapshot}")
                f.comment?.let { appendLine("   Izoh: $it") }
            }
        }
    }.trimEnd() + "\n"
}

class FlagRepository(private val db: AppDatabase, private val clock: Clock) {
    private val dao = db.flags()

    /** Stores a report; the same exercise and reason again only replaces the comment. */
    suspend fun flag(item: ExerciseItem, reason: FlagReason, comment: String?) {
        val note = comment?.trim()?.take(MAX_COMMENT)?.takeIf { it.isNotEmpty() }
        val ref = flagRef(item)
        val existing = dao.find(ref, reason.name)
        if (existing != null) {
            dao.update(existing.copy(comment = note))
        } else {
            dao.insert(
                ContentFlagEntity(
                    ref = ref, lessonId = lessonOfRef(ref), type = item.typeKey, snapshot = flagSnapshot(item),
                    reason = reason.name, comment = note, createdMs = clock.millis(),
                ),
            )
        }
    }

    fun observeCount(): Flow<Int> = dao.observeCount()

    suspend fun all(): List<ContentFlagEntity> = dao.all()

    suspend fun clear() = dao.deleteAll()
}

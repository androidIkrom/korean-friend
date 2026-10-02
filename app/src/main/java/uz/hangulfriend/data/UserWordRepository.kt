package uz.hangulfriend.data

import androidx.room.withTransaction
import java.time.Clock
import kotlinx.coroutines.flow.Flow
import uz.hangulfriend.content.Dialogue
import uz.hangulfriend.content.Lesson
import uz.hangulfriend.content.Word

/** Pseudo-lesson id of the learner's own words; their cards carry it as `lessonId`. */
const val USER_LESSON_ID = "user"

fun userWordId(rowId: Long) = "user_w$rowId"

/** Korean comparison key: no spaces, lower case (so "사 과" and "사과" are the same word). */
fun normalizeKo(s: String): String = s.filterNot { it.isWhitespace() }.lowercase()

private const val MAX_KO = 40
private const val MAX_UZ = 80
private const val MAX_NOTE = 120

/** The learner's own words (stage 7a spec §2): validation, review cards, and a pseudo-lesson for sessions. */
class UserWordRepository(private val db: AppDatabase, private val study: StudyRepository, private val clock: Clock) {
    sealed interface AddResult {
        data class Added(val id: Long) : AddResult
        data object Empty : AddResult
        data object TooLong : AddResult
        data object Duplicate : AddResult
    }

    private val dao = db.userWords()

    fun observe(): Flow<List<UserWordEntity>> = dao.observeAll()

    /** [bookKo] holds the normalised Korean of every book word, so a book word cannot be added again. */
    suspend fun add(ko: String, uz: String, note: String?, bookKo: Set<String>): AddResult {
        val k = ko.trim()
        val u = uz.trim()
        val n = note?.trim()?.takeIf { it.isNotEmpty() }
        if (k.isEmpty() || u.isEmpty()) return AddResult.Empty
        if (k.length > MAX_KO || u.length > MAX_UZ || (n?.length ?: 0) > MAX_NOTE) return AddResult.TooLong
        val key = normalizeKo(k)
        if (key in bookKo.map(::normalizeKo) || dao.all().any { normalizeKo(it.ko) == key }) return AddResult.Duplicate
        return db.withTransaction {
            val id = dao.insert(UserWordEntity(ko = k, uz = u, note = n, createdMs = clock.millis()))
            study.ensureCards(lessonOf(listOf(UserWordEntity(id, k, u, n, 0))), lessonOrder = -1, origin = CardOrigin.LESSON)
            AddResult.Added(id)
        }
    }

    /** Removes the word with its review cards and their history. */
    suspend fun delete(id: Long) {
        val itemId = userWordId(id)
        db.withTransaction {
            db.logs().deleteForCards(CardKind.entries.map { CardIds.of(itemId, it) })
            db.cards().deleteByItem(itemId)
            dao.delete(id)
        }
    }

    suspend fun asLesson(): Lesson = lessonOf(dao.all())

    private fun lessonOf(words: List<UserWordEntity>) = Lesson(
        id = USER_LESSON_ID,
        unit = 0,
        lesson = 0,
        titleKo = "",
        titleUz = "",
        topicUz = "",
        reviewed = true,
        words = words.map {
            Word(id = userWordId(it.id), ko = it.ko, uz = it.uz, pos = "", exampleKo = it.note.orEmpty(), exampleUz = "")
        },
        grammar = emptyList(),
        dialogue = Dialogue(emptyList()),
        exercises = emptyList(),
        test = emptyList(),
    )
}

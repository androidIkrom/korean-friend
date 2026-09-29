package uz.hangulfriend.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import uz.hangulfriend.srs.CardState

enum class CardKind(val suffix: String) { RECOGNIZE("R"), PRODUCE("P"), GRAMMAR("G") }

object CardIds {
    fun of(itemId: String, kind: CardKind): String = "$itemId#${kind.suffix}"
}

/**
 * LESSON: the lesson the learner opened; BACKLOG: lessons marked passed at onboarding.
 * Never-reviewed cards of both enter reviews under the daily new limit, LESSON first.
 */
enum class CardOrigin { LESSON, BACKLOG }

enum class LessonStatus { NOT_STARTED, PASSED, IN_PROGRESS, COMPLETED, VERIFIED }

@Entity(tableName = "cards", indices = [Index("dueMs"), Index("lessonId")])
data class CardEntity(
    @PrimaryKey val id: String,
    val itemId: String,
    val kind: CardKind,
    val lessonId: String,
    val origin: CardOrigin,
    val state: CardState,
    val step: Int?,
    val stability: Double?,
    val difficulty: Double?,
    val dueMs: Long,
    val lastReviewMs: Long?,
    val firstReviewedMs: Long?,
    val reps: Int,
    val lapses: Int,
    val lessonOrder: Int,
)

@Entity(tableName = "review_logs", indices = [Index("cardId")])
data class ReviewLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cardId: String,
    val rating: Int,
    val reviewedMs: Long,
    val elapsedMs: Long,
)

@Entity(tableName = "lesson_progress")
data class LessonProgressEntity(
    @PrimaryKey val lessonId: String,
    val status: LessonStatus,
    val stage: Int,
    val bestTestScore: Int?,
)

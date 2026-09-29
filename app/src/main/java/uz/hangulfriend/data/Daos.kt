package uz.hangulfriend.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface CardDao {
    @Query("SELECT * FROM cards")
    suspend fun all(): List<CardEntity>

    @Query("SELECT * FROM cards WHERE id = :id")
    suspend fun get(id: String): CardEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(cards: List<CardEntity>)

    @Update
    suspend fun update(card: CardEntity)

    @Query("UPDATE cards SET origin = 'LESSON' WHERE lessonId = :lessonId AND origin = 'BACKLOG' AND firstReviewedMs IS NULL")
    suspend fun promoteUnseenBacklog(lessonId: String)

    @Query("SELECT * FROM cards WHERE firstReviewedMs IS NOT NULL AND dueMs <= :nowMs ORDER BY dueMs, id")
    suspend fun dueReviewed(nowMs: Long): List<CardEntity>

    /** Never-reviewed cards: the current lesson's (LESSON) before the onboarding backlog. */
    @Query(
        "SELECT * FROM cards WHERE firstReviewedMs IS NULL " +
            "ORDER BY CASE origin WHEN 'LESSON' THEN 0 ELSE 1 END, lessonOrder, id LIMIT :limit",
    )
    suspend fun unseen(limit: Int): List<CardEntity>

    @Query("SELECT COUNT(*) FROM cards WHERE firstReviewedMs >= :fromMs AND firstReviewedMs < :toMs")
    suspend fun firstReviewedBetween(fromMs: Long, toMs: Long): Int

    /** Emits whenever the cards table changes; callers recompute the queue. */
    @Query("SELECT COUNT(*) FROM cards")
    fun observeCount(): Flow<Int>

    @Query("SELECT MAX(lastReviewMs) FROM cards")
    fun observeLastReview(): Flow<Long?>
}

@Dao
interface ReviewLogDao {
    @Insert
    suspend fun insert(log: ReviewLogEntity)

    @Query("SELECT * FROM review_logs WHERE cardId = :cardId ORDER BY reviewedMs")
    suspend fun forCard(cardId: String): List<ReviewLogEntity>
}

@Dao
interface ProgressDao {
    @Query("SELECT * FROM lesson_progress")
    fun observeAll(): Flow<List<LessonProgressEntity>>

    @Query("SELECT * FROM lesson_progress WHERE lessonId = :lessonId")
    suspend fun get(lessonId: String): LessonProgressEntity?

    @Upsert
    suspend fun upsert(progress: LessonProgressEntity)
}

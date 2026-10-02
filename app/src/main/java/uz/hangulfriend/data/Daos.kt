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

    @Query("DELETE FROM cards")
    suspend fun deleteAll()

    @Query("DELETE FROM cards WHERE itemId = :itemId")
    suspend fun deleteByItem(itemId: String)

    @Query("SELECT * FROM cards WHERE kind = 'RECOGNIZE'")
    fun observeRecognize(): Flow<List<CardEntity>>

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

    /** Words whose recognition card has graduated to review at least once. */
    @Query("SELECT COUNT(*) FROM cards WHERE kind = 'RECOGNIZE' AND state IN ('REVIEW', 'RELEARNING')")
    suspend fun countLearnedWords(): Int
}

data class AgainCount(val cardId: String, val count: Int)

@Dao
interface GameDao {
    @Insert
    suspend fun insertXp(event: XpEventEntity)

    @Insert
    suspend fun insertAllXp(events: List<XpEventEntity>)

    @Query("SELECT * FROM xp_events")
    suspend fun allXp(): List<XpEventEntity>

    @Query("DELETE FROM xp_events")
    suspend fun deleteAllXp()

    @Insert
    suspend fun insertAllAchievements(items: List<AchievementEntity>)

    @Query("SELECT * FROM achievements")
    suspend fun allAchievements(): List<AchievementEntity>

    @Query("DELETE FROM achievements")
    suspend fun deleteAllAchievements()

    @Insert
    suspend fun insertAllBest(items: List<BestScoreEntity>)

    @Query("SELECT * FROM best_scores")
    suspend fun allBest(): List<BestScoreEntity>

    @Query("DELETE FROM best_scores")
    suspend fun deleteAllBest()

    @Query("SELECT COALESCE(SUM(amount), 0) FROM xp_events")
    fun observeTotalXp(): Flow<Int>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM xp_events WHERE atMs >= :fromMs AND atMs < :toMs")
    suspend fun xpBetween(fromMs: Long, toMs: Long): Int

    @Query("SELECT COUNT(*) FROM xp_events WHERE reason = :reason AND atMs >= :fromMs AND atMs < :toMs")
    suspend fun countReasonBetween(reason: String, fromMs: Long, toMs: Long): Int

    @Query("SELECT COUNT(*) FROM xp_events WHERE reason = :reason")
    suspend fun countReason(reason: String): Int

    @Query("SELECT * FROM xp_events WHERE atMs >= :fromMs")
    suspend fun xpSince(fromMs: Long): List<XpEventEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAchievement(a: AchievementEntity): Long

    @Query("SELECT * FROM achievements ORDER BY unlockedMs")
    fun observeAchievements(): Flow<List<AchievementEntity>>

    @Query("SELECT score FROM best_scores WHERE gameId = :gameId")
    suspend fun best(gameId: String): Int?

    @Upsert
    suspend fun upsertBest(score: BestScoreEntity)

    @Query(
        "SELECT cardId, COUNT(*) AS count FROM review_logs WHERE rating = 1 AND reviewedMs >= :fromMs " +
            "GROUP BY cardId ORDER BY count DESC, cardId LIMIT :limit",
    )
    suspend fun againCounts(fromMs: Long, limit: Int): List<AgainCount>
}

@Dao
interface ReviewLogDao {
    @Insert
    suspend fun insert(log: ReviewLogEntity)

    @Insert
    suspend fun insertAll(logs: List<ReviewLogEntity>)

    @Query("SELECT * FROM review_logs")
    suspend fun all(): List<ReviewLogEntity>

    @Query("DELETE FROM review_logs")
    suspend fun deleteAll()

    @Query("DELETE FROM review_logs WHERE cardId IN (:cardIds)")
    suspend fun deleteForCards(cardIds: List<String>)

    @Query("SELECT * FROM review_logs WHERE cardId = :cardId ORDER BY reviewedMs")
    suspend fun forCard(cardId: String): List<ReviewLogEntity>
}

@Dao
interface ProgressDao {
    @Query("SELECT * FROM lesson_progress")
    fun observeAll(): Flow<List<LessonProgressEntity>>

    @Query("SELECT * FROM lesson_progress")
    suspend fun all(): List<LessonProgressEntity>

    @Query("SELECT * FROM lesson_progress WHERE lessonId = :lessonId")
    suspend fun get(lessonId: String): LessonProgressEntity?

    @Upsert
    suspend fun upsert(progress: LessonProgressEntity)

    @Insert
    suspend fun insertAll(items: List<LessonProgressEntity>)

    @Query("DELETE FROM lesson_progress")
    suspend fun deleteAll()
}

@Dao
interface StoryDao {
    /** Returns -1 when the episode was already finished. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(progress: StoryProgressEntity): Long

    @Query("SELECT * FROM story_progress")
    fun observeAll(): Flow<List<StoryProgressEntity>>

    @Query("SELECT * FROM story_progress")
    suspend fun all(): List<StoryProgressEntity>

    @Insert
    suspend fun insertAll(items: List<StoryProgressEntity>)

    @Query("DELETE FROM story_progress")
    suspend fun deleteAll()
}

@Dao
interface UserWordDao {
    @Insert
    suspend fun insert(word: UserWordEntity): Long

    @Query("DELETE FROM user_words WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM user_words ORDER BY createdMs DESC, id DESC")
    fun observeAll(): Flow<List<UserWordEntity>>

    @Query("SELECT * FROM user_words ORDER BY createdMs DESC, id DESC")
    suspend fun all(): List<UserWordEntity>

    @Insert
    suspend fun insertAll(words: List<UserWordEntity>)

    @Query("DELETE FROM user_words")
    suspend fun deleteAll()
}

@Dao
interface FlagDao {
    @Query("SELECT * FROM content_flags WHERE ref = :ref AND reason = :reason")
    suspend fun find(ref: String, reason: String): ContentFlagEntity?

    @Insert
    suspend fun insert(flag: ContentFlagEntity): Long

    @Update
    suspend fun update(flag: ContentFlagEntity)

    @Query("SELECT COUNT(*) FROM content_flags")
    fun observeCount(): Flow<Int>

    @Query("SELECT * FROM content_flags ORDER BY createdMs, id")
    suspend fun all(): List<ContentFlagEntity>

    @Insert
    suspend fun insertAll(flags: List<ContentFlagEntity>)

    @Query("DELETE FROM content_flags")
    suspend fun deleteAll()
}

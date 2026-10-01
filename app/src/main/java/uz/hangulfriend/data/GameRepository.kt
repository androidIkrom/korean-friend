package uz.hangulfriend.data

import androidx.room.withTransaction
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import uz.hangulfriend.study.GameRules

/** XP, achievements, best scores and the mistakes notebook (spec §3.5). Days are local to [clock]'s zone. */
class GameRepository(private val db: AppDatabase, private val clock: Clock) {
    private val dao = db.game()

    /**
     * Records [amount] XP. If this pushes today's XP to [goal] for the first time today, also records the
     * daily-goal bonus. Returns all XP added.
     */
    suspend fun award(amount: Int, reason: String, goal: Int): Int {
        if (amount <= 0) return 0
        return db.withTransaction {
            val now = clock.instant().toEpochMilli()
            val (from, to) = dayBounds(LocalDate.now(clock))
            val before = dao.xpBetween(from, to)
            dao.insertXp(XpEventEntity(amount = amount, reason = reason, atMs = now))
            val crossed = before < goal && before + amount >= goal && dao.countReasonBetween(REASON_GOAL, from, to) == 0
            if (crossed) {
                dao.insertXp(XpEventEntity(amount = GameRules.XP_GOAL, reason = REASON_GOAL, atMs = now))
                amount + GameRules.XP_GOAL
            } else {
                amount
            }
        }
    }

    fun observeTotalXp(): Flow<Int> = dao.observeTotalXp()

    suspend fun todayXp(): Int {
        val (from, to) = dayBounds(LocalDate.now(clock))
        return dao.xpBetween(from, to)
    }

    suspend fun dailyXp(days: Int = 60): Map<LocalDate, Int> {
        val since = clock.instant().minus(Duration.ofDays(days.toLong())).toEpochMilli()
        return dao.xpSince(since)
            .groupBy { Instant.ofEpochMilli(it.atMs).atZone(clock.zone).toLocalDate() }
            .mapValues { (_, events) -> events.sumOf { it.amount } }
    }

    /** Unlocks [ids]; returns only those that were not unlocked before, in [ids]' order. */
    suspend fun unlockNew(ids: Set<String>): List<String> {
        val now = clock.instant().toEpochMilli()
        return ids.filter { dao.insertAchievement(AchievementEntity(it, now)) != -1L }
    }

    fun observeAchievements(): Flow<List<AchievementEntity>> = dao.observeAchievements()

    suspend fun best(gameId: String): Int = dao.best(gameId) ?: 0

    /** Returns true when [score] is a new record. */
    suspend fun submitScore(gameId: String, score: Int): Boolean {
        if (score <= best(gameId)) return false
        dao.upsertBest(BestScoreEntity(gameId, score))
        return true
    }

    suspend fun stats(goal: Int): GameRules.Stats {
        val completed = db.progress().all()
            .filter { it.status == LessonStatus.COMPLETED || it.status == LessonStatus.VERIFIED }
            .map { it.lessonId }
            .toSet()
        val units = completed.groupBy { it.substringBefore("_l") }.count { (_, lessons) -> lessons.size >= 2 }
        return GameRules.Stats(
            completedLessons = completed,
            unitsCompleted = units,
            wordsLearned = db.cards().countLearnedWords(),
            streak = GameRules.streak(dailyXp(), LocalDate.now(clock), goal),
            bossWins = dao.countReason(REASON_BOSS),
        )
    }

    /** Cards graded AGAIN most often in the last 30 days. */
    suspend fun mistakes(limit: Int = 20): List<CardEntity> {
        val since = clock.instant().minus(Duration.ofDays(30)).toEpochMilli()
        return dao.againCounts(since, limit).mapNotNull { db.cards().get(it.cardId) }
    }

    private fun dayBounds(day: LocalDate): Pair<Long, Long> =
        day.atStartOfDay(clock.zone).toInstant().toEpochMilli() to
            day.plusDays(1).atStartOfDay(clock.zone).toInstant().toEpochMilli()

    companion object {
        const val REASON_ANSWER = "answer"
        const val REASON_LESSON = "lesson"
        const val REASON_BOSS = "boss"
        const val REASON_GOAL = "goal"
        const val REASON_GAME = "game"
    }
}

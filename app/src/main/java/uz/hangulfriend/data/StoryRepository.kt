package uz.hangulfriend.data

import java.time.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import uz.hangulfriend.study.GameRules

/** Which story episodes are finished; the first finish of an episode awards [GameRules.XP_STORY]. */
class StoryRepository(private val db: AppDatabase, private val game: GameRepository, private val clock: Clock) {
    fun observeDone(): Flow<Set<String>> = db.story().observeAll().map { rows -> rows.map { it.lessonId }.toSet() }

    /** Returns the XP added: story XP plus any daily-goal bonus on the first finish, 0 on replays. */
    suspend fun complete(lessonId: String, goal: Int): Int {
        val inserted = db.story().insertIgnore(StoryProgressEntity(lessonId, clock.instant().toEpochMilli())) != -1L
        return if (inserted) game.award(GameRules.XP_STORY, GameRepository.REASON_STORY, goal) else 0
    }
}

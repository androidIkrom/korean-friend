package uz.hangulfriend.share

import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import uz.hangulfriend.content.ContentRepository
import uz.hangulfriend.data.AppDatabase
import uz.hangulfriend.data.GameRepository
import uz.hangulfriend.data.LessonStatus
import uz.hangulfriend.data.SettingsRepository
import uz.hangulfriend.study.GameRules

/** Everything the share card shows; [statuses] follows the book order (18 lessons). */
data class ShareSnapshot(
    val level: Int,
    val totalXp: Int,
    val streak: Int,
    val learnedWords: Int,
    val completedLessons: Int,
    val statuses: List<LessonStatus>,
    val currentLessonTitle: String?,
    val date: LocalDate,
    /** The player's name; empty when unset. */
    val playerName: String = "",
)

class ProgressStats(
    private val content: ContentRepository,
    private val db: AppDatabase,
    private val game: GameRepository,
    private val settings: SettingsRepository,
    private val clock: Clock,
) {
    suspend fun snapshot(): ShareSnapshot {
        val s = settings.settings.first()
        val catalog = content.catalog()
        val progress = db.progress().all().associate { it.lessonId to it.status }
        val statuses = catalog.map { progress[it.id] ?: LessonStatus.NOT_STARTED }
        val totalXp = game.observeTotalXp().first()
        val today = LocalDate.now(clock)
        return ShareSnapshot(
            level = GameRules.level(totalXp).level,
            totalXp = totalXp,
            streak = GameRules.streak(game.dailyXp(), today, s.dailyGoalXp),
            learnedWords = db.cards().countLearnedWords(),
            completedLessons = statuses.count { it == LessonStatus.COMPLETED || it == LessonStatus.VERIFIED },
            statuses = statuses,
            currentLessonTitle = catalog.find { it.id == s.currentLessonId }?.let { "${it.unit}-${it.lesson} ${it.titleKo}" },
            date = today,
            playerName = s.playerName,
        )
    }
}

package uz.hangulfriend.data

import androidx.room.withTransaction
import java.time.Clock
import kotlinx.coroutines.flow.first
import uz.hangulfriend.study.GameRules
import uz.hangulfriend.study.RankRules

/** Reads all progress into a [BackupFile] and replaces all progress with one. */
class BackupService(private val db: AppDatabase, private val settings: SettingsRepository, private val clock: Clock) {
    suspend fun export(): String {
        val file = db.withTransaction {
            BackupFile(
                format = BackupCodec.FORMAT,
                version = BackupCodec.VERSION,
                exportedAtMs = clock.instant().toEpochMilli(),
                dbVersion = DB_VERSION,
                settings = settings.settings.first().toBackup(),
                cards = db.cards().all().map { it.toDto() },
                reviewLogs = db.logs().all().map { it.toDto() },
                lessonProgress = db.progress().all().map { it.toDto() },
                xpEvents = db.game().allXp().map { it.toDto() },
                achievements = db.game().allAchievements().map { it.toDto() },
                bestScores = db.game().allBest().map { it.toDto() },
                storyProgress = db.story().all().map { it.toDto() },
                userWords = db.userWords().all().map { it.toDto() },
            )
        }
        return BackupCodec.encode(file)
    }

    /** All tables are cleared and refilled in one transaction; settings are written only after it commits. */
    suspend fun import(file: BackupFile) {
        db.withTransaction {
            db.cards().deleteAll()
            db.logs().deleteAll()
            db.progress().deleteAll()
            db.game().deleteAllXp()
            db.game().deleteAllAchievements()
            db.game().deleteAllBest()
            db.story().deleteAll()
            db.userWords().deleteAll()
            db.cards().insertIgnore(file.cards.map { it.toEntity() })
            db.logs().insertAll(file.reviewLogs.map { it.toEntity() })
            db.progress().insertAll(file.lessonProgress.map { it.toEntity() })
            db.game().insertAllXp(file.xpEvents.map { it.toEntity() })
            db.game().insertAllAchievements(file.achievements.map { it.toEntity() })
            db.game().insertAllBest(file.bestScores.map { it.toEntity() })
            db.story().insertAll(file.storyProgress.map { it.toEntity() })
            db.userWords().insertAll(file.userWords.map { it.toEntity() })
        }
        settings.replaceAll(file.settings.toSettings())
        // Restored progress is not a new achievement: remember its rank so no rank-up dialog appears.
        settings.setLastSeenRank(RankRules.rankFor(GameRules.level(file.xpEvents.sumOf { it.amount }).level))
    }

    private companion object {
        const val DB_VERSION = 4
    }
}

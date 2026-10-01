package uz.hangulfriend.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/** v2: XP events, achievements and best scores for gamification (stage 4). SQL mirrors schemas/2.json. */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(SQL_XP_EVENTS)
        connection.execSQL(SQL_ACHIEVEMENTS)
        connection.execSQL(SQL_BEST_SCORES)
    }
}

private const val SQL_XP_EVENTS =
    "CREATE TABLE IF NOT EXISTS `xp_events` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
        "`amount` INTEGER NOT NULL, `reason` TEXT NOT NULL, `atMs` INTEGER NOT NULL)"
private const val SQL_ACHIEVEMENTS =
    "CREATE TABLE IF NOT EXISTS `achievements` (`id` TEXT NOT NULL, `unlockedMs` INTEGER NOT NULL, PRIMARY KEY(`id`))"
private const val SQL_BEST_SCORES =
    "CREATE TABLE IF NOT EXISTS `best_scores` (`gameId` TEXT NOT NULL, `score` INTEGER NOT NULL, PRIMARY KEY(`gameId`))"

@Database(
    entities = [
        CardEntity::class,
        ReviewLogEntity::class,
        LessonProgressEntity::class,
        XpEventEntity::class,
        AchievementEntity::class,
        BestScoreEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cards(): CardDao

    abstract fun logs(): ReviewLogDao

    abstract fun progress(): ProgressDao

    abstract fun game(): GameDao

    companion object {
        /** No destructive fallback: losing FSRS history is worse than a crash we can fix with a migration. */
        fun open(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "hangul-friend.db")
                .addMigrations(MIGRATION_1_2)
                .build()
    }
}

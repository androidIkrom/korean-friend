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

/** v3: finished story episodes (stage 5). SQL mirrors schemas/3.json. */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(SQL_STORY_PROGRESS)
    }
}

/** v4: words the learner added (stage 7a). SQL mirrors schemas/4.json. */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(SQL_USER_WORDS)
    }
}

private const val SQL_USER_WORDS =
    "CREATE TABLE IF NOT EXISTS `user_words` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `ko` TEXT NOT NULL, " +
        "`uz` TEXT NOT NULL, `note` TEXT, `createdMs` INTEGER NOT NULL)"
private const val SQL_STORY_PROGRESS =
    "CREATE TABLE IF NOT EXISTS `story_progress` (`lessonId` TEXT NOT NULL, `completedAtMs` INTEGER NOT NULL, " +
        "PRIMARY KEY(`lessonId`))"
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
        StoryProgressEntity::class,
        UserWordEntity::class,
    ],
    version = 4,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cards(): CardDao

    abstract fun logs(): ReviewLogDao

    abstract fun progress(): ProgressDao

    abstract fun game(): GameDao

    abstract fun story(): StoryDao

    abstract fun userWords(): UserWordDao

    companion object {
        /** No destructive fallback: losing FSRS history is worse than a crash we can fix with a migration. */
        fun open(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "hangul-friend.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .build()
    }
}

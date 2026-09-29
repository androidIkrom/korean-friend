package uz.hangulfriend.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [CardEntity::class, ReviewLogEntity::class, LessonProgressEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cards(): CardDao

    abstract fun logs(): ReviewLogDao

    abstract fun progress(): ProgressDao

    companion object {
        /** No destructive fallback: losing FSRS history is worse than a crash we can fix with a migration. */
        fun open(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "hangul-friend.db").build()
    }
}

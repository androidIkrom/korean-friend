package uz.hangulfriend.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import uz.hangulfriend.study.GameRules

data class Settings(
    val onboarded: Boolean = false,
    val currentLessonId: String? = null,
    val dailyNewLimit: Int = DEFAULT_DAILY_NEW_LIMIT,
    val dailyGoalXp: Int = GameRules.DEFAULT_GOAL,
)

const val DEFAULT_DAILY_NEW_LIMIT = 20

class SettingsRepository(private val dataStore: DataStore<Preferences>) {
    val settings: Flow<Settings> = dataStore.data.map { p ->
        Settings(
            onboarded = p[ONBOARDED] ?: false,
            currentLessonId = p[CURRENT_LESSON],
            dailyNewLimit = p[DAILY_NEW_LIMIT] ?: DEFAULT_DAILY_NEW_LIMIT,
            dailyGoalXp = p[DAILY_GOAL_XP] ?: GameRules.DEFAULT_GOAL,
        )
    }

    suspend fun setCurrentLesson(id: String) {
        dataStore.edit { it[CURRENT_LESSON] = id }
    }

    suspend fun setDailyNewLimit(n: Int) {
        dataStore.edit { it[DAILY_NEW_LIMIT] = n }
    }

    suspend fun setDailyGoalXp(n: Int) {
        dataStore.edit { it[DAILY_GOAL_XP] = n }
    }

    suspend fun setOnboarded() {
        dataStore.edit { it[ONBOARDED] = true }
    }

    private companion object {
        val ONBOARDED = booleanPreferencesKey("onboarded")
        val CURRENT_LESSON = stringPreferencesKey("current_lesson")
        val DAILY_NEW_LIMIT = intPreferencesKey("daily_new_limit")
        val DAILY_GOAL_XP = intPreferencesKey("daily_goal_xp")
    }
}

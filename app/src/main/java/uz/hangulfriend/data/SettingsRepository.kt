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
import uz.hangulfriend.study.Rank

data class Settings(
    val onboarded: Boolean = false,
    val currentLessonId: String? = null,
    val dailyNewLimit: Int = DEFAULT_DAILY_NEW_LIMIT,
    val dailyGoalXp: Int = GameRules.DEFAULT_GOAL,
    val reminderEnabled: Boolean = false,
    /** Minutes after local midnight; 1200 = 20:00. */
    val reminderMinutes: Int = DEFAULT_REMINDER_MINUTES,
    val theme: GameThemeId = GameThemeId.SYSTEM,
    val hero: HeroGender = HeroGender.BOY,
)

/** The player's avatar; [key] is what DataStore and backups store. */
enum class HeroGender(val key: String) {
    BOY("boy"),
    GIRL("girl"),
    ;

    companion object {
        fun from(key: String?): HeroGender = entries.firstOrNull { it.key == key } ?: BOY
    }
}

/** Interface style; [key] is what DataStore and backups store. */
enum class GameThemeId(val key: String) {
    SYSTEM("system"),
    NEON("neon"),
    ;

    companion object {
        fun from(key: String?): GameThemeId = entries.firstOrNull { it.key == key } ?: SYSTEM
    }
}

const val DEFAULT_DAILY_NEW_LIMIT = 20
const val DEFAULT_REMINDER_MINUTES = 20 * 60

class SettingsRepository(private val dataStore: DataStore<Preferences>) {
    val settings: Flow<Settings> = dataStore.data.map { p ->
        Settings(
            onboarded = p[ONBOARDED] ?: false,
            currentLessonId = p[CURRENT_LESSON],
            dailyNewLimit = p[DAILY_NEW_LIMIT] ?: DEFAULT_DAILY_NEW_LIMIT,
            dailyGoalXp = p[DAILY_GOAL_XP] ?: GameRules.DEFAULT_GOAL,
            reminderEnabled = p[REMINDER_ENABLED] ?: false,
            reminderMinutes = p[REMINDER_MINUTES] ?: DEFAULT_REMINDER_MINUTES,
            theme = GameThemeId.from(p[UI_THEME]),
            hero = HeroGender.from(p[HERO]),
        )
    }

    /** The rank the user last acknowledged; null until first stored (spec §4.2). */
    val lastSeenRank: Flow<Rank?> = dataStore.data.map { p -> Rank.entries.firstOrNull { it.name == p[LAST_SEEN_RANK] } }

    suspend fun setTheme(id: GameThemeId) {
        dataStore.edit { it[UI_THEME] = id.key }
    }

    suspend fun setHero(hero: HeroGender) {
        dataStore.edit { it[HERO] = hero.key }
    }

    suspend fun setLastSeenRank(rank: Rank) {
        dataStore.edit { it[LAST_SEEN_RANK] = rank.name }
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

    suspend fun setReminder(enabled: Boolean, minutes: Int) {
        dataStore.edit {
            it[REMINDER_ENABLED] = enabled
            it[REMINDER_MINUTES] = minutes
        }
    }

    /** Writes every field at once (import); a null current lesson clears the key. */
    suspend fun replaceAll(s: Settings) {
        dataStore.edit {
            it[ONBOARDED] = s.onboarded
            if (s.currentLessonId == null) it.remove(CURRENT_LESSON) else it[CURRENT_LESSON] = s.currentLessonId
            it[DAILY_NEW_LIMIT] = s.dailyNewLimit
            it[DAILY_GOAL_XP] = s.dailyGoalXp
            it[REMINDER_ENABLED] = s.reminderEnabled
            it[REMINDER_MINUTES] = s.reminderMinutes
            it[UI_THEME] = s.theme.key
            it[HERO] = s.hero.key
        }
    }

    suspend fun setOnboarded() {
        dataStore.edit { it[ONBOARDED] = true }
    }

    private companion object {
        val ONBOARDED = booleanPreferencesKey("onboarded")
        val CURRENT_LESSON = stringPreferencesKey("current_lesson")
        val DAILY_NEW_LIMIT = intPreferencesKey("daily_new_limit")
        val DAILY_GOAL_XP = intPreferencesKey("daily_goal_xp")
        val REMINDER_ENABLED = booleanPreferencesKey("reminder_enabled")
        val REMINDER_MINUTES = intPreferencesKey("reminder_minutes")
        val UI_THEME = stringPreferencesKey("ui_theme")
        val LAST_SEEN_RANK = stringPreferencesKey("last_seen_rank")
        val HERO = stringPreferencesKey("hero")
    }
}

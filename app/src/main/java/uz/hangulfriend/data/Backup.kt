package uz.hangulfriend.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import uz.hangulfriend.srs.CardState

/** Progress export (spec §4). DTOs mirror the Room entities so the file format does not move with the schema. */
@Serializable
data class BackupFile(
    val format: String,
    val version: Int,
    @SerialName("exported_at_ms") val exportedAtMs: Long,
    @SerialName("db_version") val dbVersion: Int,
    val settings: BackupSettings,
    val cards: List<CardDto>,
    @SerialName("review_logs") val reviewLogs: List<ReviewLogDto>,
    @SerialName("lesson_progress") val lessonProgress: List<LessonProgressDto>,
    @SerialName("xp_events") val xpEvents: List<XpEventDto>,
    val achievements: List<AchievementDto>,
    @SerialName("best_scores") val bestScores: List<BestScoreDto>,
    @SerialName("story_progress") val storyProgress: List<StoryProgressDto>,
    /** Missing in files exported before stage 7a. */
    @SerialName("user_words") val userWords: List<UserWordDto> = emptyList(),
    /** Missing in files exported before stage 7b. */
    @SerialName("content_flags") val contentFlags: List<ContentFlagDto> = emptyList(),
)

@Serializable
data class BackupSettings(
    val onboarded: Boolean,
    @SerialName("current_lesson_id") val currentLessonId: String?,
    @SerialName("daily_new_limit") val dailyNewLimit: Int,
    @SerialName("daily_goal_xp") val dailyGoalXp: Int,
    @SerialName("reminder_enabled") val reminderEnabled: Boolean,
    @SerialName("reminder_minutes") val reminderMinutes: Int,
    /** Missing in files exported before stage 6. */
    val theme: String = GameThemeId.SYSTEM.key,
    /** Missing in files exported before stage 6b. */
    val hero: String = HeroGender.BOY.key,
    /** Missing in files exported before stage 9. */
    val sound: Boolean = true,
    /** Missing in files exported before stage 9. */
    val haptics: Boolean = true,
)

@Serializable
data class CardDto(
    val id: String,
    val itemId: String,
    val kind: String,
    val lessonId: String,
    val origin: String,
    val state: String,
    val step: Int?,
    val stability: Double?,
    val difficulty: Double?,
    val dueMs: Long,
    val lastReviewMs: Long?,
    val firstReviewedMs: Long?,
    val reps: Int,
    val lapses: Int,
    val lessonOrder: Int,
)

@Serializable
data class ReviewLogDto(val id: Long, val cardId: String, val rating: Int, val reviewedMs: Long, val elapsedMs: Long)

@Serializable
data class LessonProgressDto(val lessonId: String, val status: String, val stage: Int, val bestTestScore: Int?)

@Serializable
data class XpEventDto(val id: Long, val amount: Int, val reason: String, val atMs: Long)

@Serializable
data class AchievementDto(val id: String, val unlockedMs: Long)

@Serializable
data class BestScoreDto(val gameId: String, val score: Int)

@Serializable
data class StoryProgressDto(val lessonId: String, val completedAtMs: Long)

fun Settings.toBackup() =
    BackupSettings(onboarded, currentLessonId, dailyNewLimit, dailyGoalXp, reminderEnabled, reminderMinutes, theme.key, hero.key, soundOn, hapticsOn)

fun BackupSettings.toSettings() = Settings(
    onboarded, currentLessonId, dailyNewLimit, dailyGoalXp, reminderEnabled, reminderMinutes, GameThemeId.from(theme),
    HeroGender.from(hero), sound, haptics,
)

@Serializable
data class UserWordDto(val id: Long, val ko: String, val uz: String, val note: String?, @SerialName("created_ms") val createdMs: Long)

@Serializable
data class ContentFlagDto(
    val id: Long,
    val ref: String,
    @SerialName("lesson_id") val lessonId: String,
    val type: String,
    val snapshot: String,
    val reason: String,
    val comment: String?,
    @SerialName("created_ms") val createdMs: Long,
)

fun ContentFlagEntity.toDto() = ContentFlagDto(id, ref, lessonId, type, snapshot, reason, comment, createdMs)

fun ContentFlagDto.toEntity() = ContentFlagEntity(id, ref, lessonId, type, snapshot, reason, comment, createdMs)

fun UserWordEntity.toDto() = UserWordDto(id, ko, uz, note, createdMs)

fun UserWordDto.toEntity() = UserWordEntity(id, ko, uz, note, createdMs)

fun CardEntity.toDto() = CardDto(
    id, itemId, kind.name, lessonId, origin.name, state.name, step, stability, difficulty,
    dueMs, lastReviewMs, firstReviewedMs, reps, lapses, lessonOrder,
)

fun CardDto.toEntity() = CardEntity(
    id, itemId, CardKind.valueOf(kind), lessonId, CardOrigin.valueOf(origin), CardState.valueOf(state), step, stability,
    difficulty, dueMs, lastReviewMs, firstReviewedMs, reps, lapses, lessonOrder,
)

fun ReviewLogEntity.toDto() = ReviewLogDto(id, cardId, rating, reviewedMs, elapsedMs)

fun ReviewLogDto.toEntity() = ReviewLogEntity(id, cardId, rating, reviewedMs, elapsedMs)

fun LessonProgressEntity.toDto() = LessonProgressDto(lessonId, status.name, stage, bestTestScore)

fun LessonProgressDto.toEntity() = LessonProgressEntity(lessonId, LessonStatus.valueOf(status), stage, bestTestScore)

fun XpEventEntity.toDto() = XpEventDto(id, amount, reason, atMs)

fun XpEventDto.toEntity() = XpEventEntity(id, amount, reason, atMs)

fun AchievementEntity.toDto() = AchievementDto(id, unlockedMs)

fun AchievementDto.toEntity() = AchievementEntity(id, unlockedMs)

fun BestScoreEntity.toDto() = BestScoreDto(gameId, score)

fun BestScoreDto.toEntity() = BestScoreEntity(gameId, score)

fun StoryProgressEntity.toDto() = StoryProgressDto(lessonId, completedAtMs)

fun StoryProgressDto.toEntity() = StoryProgressEntity(lessonId, completedAtMs)

sealed interface BackupResult {
    data class Ok(val file: BackupFile) : BackupResult

    /** Not JSON, not our format, or a value that does not parse. */
    data object NotBackup : BackupResult

    /** Written by a newer app version than this one understands. */
    data object TooNew : BackupResult
}

object BackupCodec {
    const val FORMAT = "hangul-friend-backup"
    const val VERSION = 1

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    fun encode(file: BackupFile): String = json.encodeToString(BackupFile.serializer(), file)

    fun decode(text: String): BackupResult {
        val file = try {
            json.decodeFromString(BackupFile.serializer(), text)
        } catch (e: SerializationException) {
            return BackupResult.NotBackup
        } catch (e: IllegalArgumentException) {
            return BackupResult.NotBackup
        }
        return when {
            file.format != FORMAT -> BackupResult.NotBackup
            file.version > VERSION -> BackupResult.TooNew
            !file.enumsValid() -> BackupResult.NotBackup
            else -> BackupResult.Ok(file)
        }
    }

    /** Enum names are checked here so a hand-edited file fails before the import transaction starts. */
    private fun BackupFile.enumsValid(): Boolean = runCatching {
        cards.forEach { it.toEntity() }
        lessonProgress.forEach { it.toEntity() }
    }.isSuccess
}

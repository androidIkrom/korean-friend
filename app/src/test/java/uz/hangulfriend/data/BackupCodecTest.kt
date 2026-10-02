package uz.hangulfriend.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import uz.hangulfriend.srs.CardState

class BackupCodecTest {
    private val card = CardEntity(
        id = "u02_l1_w001#R", itemId = "u02_l1_w001", kind = CardKind.RECOGNIZE, lessonId = "u02_l1",
        origin = CardOrigin.LESSON, state = CardState.REVIEW, step = null, stability = 3.5, difficulty = 5.1,
        dueMs = 1000, lastReviewMs = 900, firstReviewedMs = 800, reps = 2, lapses = 1, lessonOrder = 2,
    )
    private val log = ReviewLogEntity(id = 7, cardId = card.id, rating = 3, reviewedMs = 900, elapsedMs = 4000)
    private val progress = LessonProgressEntity("u02_l1", LessonStatus.COMPLETED, stage = 4, bestTestScore = 90)
    private val xp = XpEventEntity(id = 3, amount = 10, reason = "answer", atMs = 900)
    private val achievement = AchievementEntity("first_lesson", 950)
    private val best = BestScoreEntity("speed", 12)
    private val story = StoryProgressEntity("u02_l1", 990)

    private val file = BackupFile(
        format = BackupCodec.FORMAT, version = BackupCodec.VERSION, exportedAtMs = 1234, dbVersion = 3,
        settings = BackupSettings(
            onboarded = true, currentLessonId = "u02_l1", dailyNewLimit = 25, dailyGoalXp = 80,
            reminderEnabled = true, reminderMinutes = 1230,
        ),
        cards = listOf(card.toDto()), reviewLogs = listOf(log.toDto()), lessonProgress = listOf(progress.toDto()),
        xpEvents = listOf(xp.toDto()), achievements = listOf(achievement.toDto()), bestScores = listOf(best.toDto()),
        storyProgress = listOf(story.toDto()),
    )

    @Test fun roundTrip() = assertEquals(BackupResult.Ok(file), BackupCodec.decode(BackupCodec.encode(file)))

    @Test fun rejectsNewerVersion() =
        assertEquals(BackupResult.TooNew, BackupCodec.decode(BackupCodec.encode(file.copy(version = BackupCodec.VERSION + 1))))

    @Test fun rejectsOtherFormat() =
        assertEquals(BackupResult.NotBackup, BackupCodec.decode(BackupCodec.encode(file.copy(format = "other-app"))))

    @Test fun rejectsMalformedJson() = assertEquals(BackupResult.NotBackup, BackupCodec.decode("{"))

    @Test fun rejectsEmptyText() = assertEquals(BackupResult.NotBackup, BackupCodec.decode(""))

    @Test fun acceptsUnknownKeys() {
        val text = BackupCodec.encode(file).replaceFirst("{", "{\"extra\": 1,")
        assertTrue(BackupCodec.decode(text) is BackupResult.Ok)
    }

    @Test fun decodesOldFileWithoutTheme() {
        val text = BackupCodec.encode(file).replace(Regex(""",?\s*"theme":\s*"[a-z]+""""), "")
        assertTrue(!text.contains("\"theme\""))
        assertEquals("system", (BackupCodec.decode(text) as BackupResult.Ok).file.settings.theme)
    }

    @Test fun themeRoundTrip() {
        val neon = file.copy(settings = file.settings.copy(theme = "neon"))
        assertEquals("neon", (BackupCodec.decode(BackupCodec.encode(neon)) as BackupResult.Ok).file.settings.theme)
    }

    @Test fun decodesOldFileWithoutHero() {
        val text = BackupCodec.encode(file.copy(settings = file.settings.copy(hero = "girl")))
            .replace(Regex(""",?\s*"hero":\s*"[a-z]+""""), "")
        assertTrue(!text.contains("\"hero\""))
        assertEquals("boy", (BackupCodec.decode(text) as BackupResult.Ok).file.settings.hero)
    }

    @Test fun heroRoundTrip() {
        val girl = file.copy(settings = file.settings.copy(hero = "girl"))
        assertEquals("girl", (BackupCodec.decode(BackupCodec.encode(girl)) as BackupResult.Ok).file.settings.hero)
    }

    @Test fun decodesOldFileWithoutUserWords() {
        val text = BackupCodec.encode(file).replace(Regex(""",?\s*"user_words":\s*\[[^\]]*\]"""), "")
        assertTrue(!text.contains("user_words"))
        assertEquals(emptyList<UserWordDto>(), (BackupCodec.decode(text) as BackupResult.Ok).file.userWords)
    }

    @Test fun userWordsRoundTrip() {
        val withWords = file.copy(userWords = listOf(UserWordEntity(3, "사과", "olma", "meva", 77).toDto()))
        assertEquals(BackupResult.Ok(withWords), BackupCodec.decode(BackupCodec.encode(withWords)))
        assertEquals(UserWordEntity(3, "사과", "olma", "meva", 77), withWords.userWords.single().toEntity())
    }

    @Test fun decodesOldFileWithoutFlags() {
        val text = BackupCodec.encode(file).replace(Regex(""",?\s*"content_flags":\s*\[[^\]]*\]"""), "")
        assertTrue(!text.contains("content_flags"))
        assertEquals(emptyList<ContentFlagDto>(), (BackupCodec.decode(text) as BackupResult.Ok).file.contentFlags)
    }

    @Test fun flagsRoundTrip() {
        val flag = ContentFlagEntity(4, "u02_l1_e07", "u02_l1", "fill_blank", "s", "TYPO", "izoh", 9)
        val withFlags = file.copy(contentFlags = listOf(flag.toDto()))
        assertEquals(BackupResult.Ok(withFlags), BackupCodec.decode(BackupCodec.encode(withFlags)))
        assertEquals(flag, withFlags.contentFlags.single().toEntity())
    }

    @Test fun entityDtoRoundTrip() {
        assertEquals(card, card.toDto().toEntity())
        assertEquals(log, log.toDto().toEntity())
        assertEquals(progress, progress.toDto().toEntity())
        assertEquals(xp, xp.toDto().toEntity())
        assertEquals(achievement, achievement.toDto().toEntity())
        assertEquals(best, best.toDto().toEntity())
        assertEquals(story, story.toDto().toEntity())
    }
}

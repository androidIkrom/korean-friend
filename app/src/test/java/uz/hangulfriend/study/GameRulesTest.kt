package uz.hangulfriend.study

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameRulesTest {
    private val today = LocalDate.of(2026, 10, 1)

    @Test fun xpForAnswer_comboFromThird() {
        assertEquals(10, GameRules.xpForAnswer(correct = true, comboAfter = 1))
        assertEquals(10, GameRules.xpForAnswer(correct = true, comboAfter = 2))
        assertEquals(15, GameRules.xpForAnswer(correct = true, comboAfter = 3))
        assertEquals(0, GameRules.xpForAnswer(correct = false, comboAfter = 0))
    }

    @Test fun topikLevel() {
        assertEquals(0, GameRules.topikLevel(39))
        assertEquals(1, GameRules.topikLevel(40))
        assertEquals(1, GameRules.topikLevel(69))
        assertEquals(2, GameRules.topikLevel(70))
        assertEquals(2, GameRules.topikLevel(100))
    }

    @Test fun level_thresholds() {
        assertEquals(GameRules.LevelInfo(1, 0, 100), GameRules.level(0))
        assertEquals(GameRules.LevelInfo(1, 99, 100), GameRules.level(99))
        assertEquals(GameRules.LevelInfo(2, 0, 200), GameRules.level(100))
        assertEquals(2, GameRules.level(299).level)
        assertEquals(GameRules.LevelInfo(3, 0, 300), GameRules.level(300))
    }

    @Test fun streak_countsBackFromTodayOrYesterday() {
        val days = mapOf(today to 60, today.minusDays(1) to 50, today.minusDays(2) to 70)
        assertEquals(3, GameRules.streak(days, today, goal = 50))
        val notYetToday = mapOf(today to 10, today.minusDays(1) to 50, today.minusDays(2) to 50)
        assertEquals(2, GameRules.streak(notYetToday, today, goal = 50))
    }

    @Test fun streak_brokenByMissedDay() {
        val days = mapOf(today to 50, today.minusDays(2) to 50)
        assertEquals(1, GameRules.streak(days, today, goal = 50))
        assertEquals(0, GameRules.streak(mapOf(today.minusDays(2) to 80), today, goal = 50))
    }

    @Test fun streak_localDays() {
        // Days are already local dates: yesterday reached, today not yet → streak continues from yesterday.
        assertEquals(1, GameRules.streak(mapOf(today.minusDays(1) to 50), today, goal = 50))
    }

    private fun stats(
        lessons: Set<String> = emptySet(),
        units: Int = 0,
        words: Int = 0,
        streak: Int = 0,
        boss: Int = 0,
    ) = GameRules.Stats(lessons, units, words, streak, boss)

    @Test fun achievements_thresholds() {
        assertTrue(GameRules.achievements(stats()).isEmpty())
        assertFalse("words_100" in GameRules.achievements(stats(words = 99)))
        assertTrue("words_100" in GameRules.achievements(stats(words = 100)))
        assertTrue("words_500" in GameRules.achievements(stats(words = 500)))
        assertTrue("streak_7" in GameRules.achievements(stats(streak = 7)))
        assertFalse("streak_30" in GameRules.achievements(stats(streak = 29)))
        assertTrue("first_boss" in GameRules.achievements(stats(boss = 1)))
        assertTrue("first_lesson" in GameRules.achievements(stats(lessons = setOf("u02_l1"))))
        assertTrue("first_unit" in GameRules.achievements(stats(units = 1)))
    }

    @Test fun gradeBands() {
        assertEquals("S", GameRules.clearGrade(100, failed = false))
        assertEquals("S", GameRules.clearGrade(95, failed = false))
        assertEquals("A", GameRules.clearGrade(94, failed = false))
        assertEquals("A", GameRules.clearGrade(85, failed = false))
        assertEquals("B", GameRules.clearGrade(70, failed = false))
        assertEquals("C", GameRules.clearGrade(55, failed = false))
        assertEquals("D", GameRules.clearGrade(40, failed = false))
        assertEquals("E", GameRules.clearGrade(39, failed = false))
    }

    @Test fun starBands() {
        assertEquals(3, GameRules.clearStars(90, failed = false))
        assertEquals(2, GameRules.clearStars(89, failed = false))
        assertEquals(2, GameRules.clearStars(70, failed = false))
        assertEquals(1, GameRules.clearStars(40, failed = false))
        assertEquals(0, GameRules.clearStars(39, failed = false))
    }

    @Test fun failedIsE() {
        assertEquals("E", GameRules.clearGrade(100, failed = true))
        assertEquals(0, GameRules.clearStars(100, failed = true))
    }

    @Test fun allAchievementIdsListed() =
        assertEquals(setOf("first_lesson", "first_unit", "words_100", "words_500", "streak_7", "streak_30", "first_boss"), GameRules.ACHIEVEMENTS.toSet())
}

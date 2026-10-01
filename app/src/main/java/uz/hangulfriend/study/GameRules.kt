package uz.hangulfriend.study

import java.time.LocalDate

/** XP, level, streak and achievement rules (spec §3.5). Pure functions only. */
object GameRules {
    const val XP_CORRECT = 10
    const val XP_COMBO = 5
    const val COMBO_FROM = 3
    const val XP_LESSON = 100
    const val XP_BOSS = 200
    const val XP_GOAL = 50
    const val XP_GAME = 5
    const val DEFAULT_GOAL = 50

    val ACHIEVEMENTS = listOf("first_lesson", "first_unit", "words_100", "words_500", "streak_7", "streak_30", "first_boss")

    /** XP for one answer; [comboAfter] is the run of correct answers including this one. */
    fun xpForAnswer(correct: Boolean, comboAfter: Int): Int = when {
        !correct -> 0
        comboAfter >= COMBO_FROM -> XP_CORRECT + XP_COMBO
        else -> XP_CORRECT
    }

    data class LevelInfo(val level: Int, val xpIntoLevel: Int, val xpForNext: Int)

    /** Level L starts at 50·L·(L−1) total XP: 1 → 0, 2 → 100, 3 → 300, 4 → 600 … */
    fun level(totalXp: Int): LevelInfo {
        var level = 1
        while (threshold(level + 1) <= totalXp) level++
        return LevelInfo(level, totalXp - threshold(level), threshold(level + 1) - threshold(level))
    }

    private fun threshold(level: Int) = 50 * level * (level - 1)

    /** Consecutive days with XP ≥ [goal], counted back from today, or from yesterday while today is not done yet. */
    fun streak(dailyXp: Map<LocalDate, Int>, today: LocalDate, goal: Int): Int {
        var day = if ((dailyXp[today] ?: 0) >= goal) today else today.minusDays(1)
        var count = 0
        while ((dailyXp[day] ?: 0) >= goal) {
            count++
            day = day.minusDays(1)
        }
        return count
    }

    data class Stats(
        val completedLessons: Set<String>,
        val unitsCompleted: Int,
        val wordsLearned: Int,
        val streak: Int,
        val bossWins: Int,
    )

    fun achievements(s: Stats): Set<String> = buildSet {
        if (s.completedLessons.isNotEmpty()) add("first_lesson")
        if (s.unitsCompleted >= 1) add("first_unit")
        if (s.wordsLearned >= 100) add("words_100")
        if (s.wordsLearned >= 500) add("words_500")
        if (s.streak >= 7) add("streak_7")
        if (s.streak >= 30) add("streak_30")
        if (s.bossWins >= 1) add("first_boss")
    }
}

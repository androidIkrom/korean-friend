package uz.hangulfriend.ui.game

import uz.hangulfriend.R

fun achievementTitle(id: String): Int = when (id) {
    "first_lesson" -> R.string.ach_first_lesson
    "first_unit" -> R.string.ach_first_unit
    "words_100" -> R.string.ach_words_100
    "words_500" -> R.string.ach_words_500
    "streak_7" -> R.string.ach_streak_7
    "streak_30" -> R.string.ach_streak_30
    "first_boss" -> R.string.ach_first_boss
    else -> 0
}

fun achievementDesc(id: String): Int = when (id) {
    "first_lesson" -> R.string.ach_first_lesson_desc
    "first_unit" -> R.string.ach_first_unit_desc
    "words_100" -> R.string.ach_words_100_desc
    "words_500" -> R.string.ach_words_500_desc
    "streak_7" -> R.string.ach_streak_7_desc
    "streak_30" -> R.string.ach_streak_30_desc
    "first_boss" -> R.string.ach_first_boss_desc
    else -> 0
}

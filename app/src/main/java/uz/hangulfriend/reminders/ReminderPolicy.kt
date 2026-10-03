package uz.hangulfriend.reminders

import java.time.Duration
import java.time.ZonedDateTime

/** What a reminder says; [ReminderText] turns it into words in the learner's language. */
sealed interface Reminder {
    val streak: Int

    /** Cards are waiting for review. */
    data class Due(val count: Int, override val streak: Int) : Reminder

    /** Nothing is due, but today's XP goal is not met yet. */
    data class Goal(override val streak: Int) : Reminder
}

/** Whether and what to remind (spec §3). Pure functions; the worker supplies today's numbers. */
object ReminderPolicy {
    /** Null when today's goal is already met: no reminder on a day the learner has done the work. */
    fun message(todayXp: Int, goal: Int, dueCount: Int, streak: Int): Reminder? {
        if (todayXp >= goal) return null
        return if (dueCount > 0) Reminder.Due(dueCount, streak) else Reminder.Goal(streak)
    }

    /** Time from [now] to the next [minutes]-after-midnight; a time that is now or past means tomorrow. */
    /** A reminder that is on but may no longer post notifications should be switched off. */
    fun shouldDisable(enabled: Boolean, permitted: Boolean): Boolean = enabled && !permitted

    fun delayUntilNext(now: ZonedDateTime, minutes: Int): Duration {
        val today = now.toLocalDate().atStartOfDay(now.zone).plusMinutes(minutes.toLong())
        val next = if (today.isAfter(now)) today else now.toLocalDate().plusDays(1).atStartOfDay(now.zone).plusMinutes(minutes.toLong())
        return Duration.between(now, next)
    }
}

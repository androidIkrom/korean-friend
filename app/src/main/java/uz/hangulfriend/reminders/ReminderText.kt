package uz.hangulfriend.reminders

import android.content.Context
import uz.hangulfriend.R

object ReminderText {
    /** [context] must already be in the learner's language (see Localized.wrap). */
    fun of(context: Context, r: Reminder): String {
        val res = context.resources
        val base = when (r) {
            is Reminder.Due -> res.getQuantityString(R.plurals.reminder_due, r.count, r.count)
            is Reminder.Goal -> res.getString(R.string.reminder_goal)
        }
        return if (r.streak > 0) "$base ${res.getQuantityString(R.plurals.reminder_streak, r.streak, r.streak)}" else base
    }
}

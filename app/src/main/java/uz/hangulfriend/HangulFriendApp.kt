package uz.hangulfriend

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.launch
import uz.hangulfriend.reminders.ReminderScheduler

class HangulFriendApp : Application() {
    val container: AppContainer by lazy { AppContainer(this) }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        container.reminders.createChannel()
        // Ensures a reminder is booked at start (KEEP), and re-books when its switch or time changes (also after an import).
        scope.launch {
            var first = true
            container.settings.settings
                .distinctUntilChangedBy { Triple(it.reminderEnabled, it.reminderMinutes, it.onboarded) }
                .collect {
                    val trigger = if (first) ReminderScheduler.Trigger.APP_START else ReminderScheduler.Trigger.SETTINGS_CHANGED
                    first = false
                    container.reminders.apply(it, trigger)
                }
        }
    }
}

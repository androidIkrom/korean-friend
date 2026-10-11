package uz.hangulfriend

import android.app.Application
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
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
        // Content is parsed once per process. Doing it here, a moment after launch, leaves the first screen
        // the CPU and spares the stories, games and vocabulary lists the wait for fifty lesson files.
        scope.launch {
            delay(PRELOAD_DELAY_MS)
            runCatching { container.content.preload() }.onFailure { Log.w("HangulFriendApp", "Content preload failed", it) }
        }
    }

    private companion object {
        const val PRELOAD_DELAY_MS = 1_000L
    }
}

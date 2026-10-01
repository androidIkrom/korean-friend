package uz.hangulfriend

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.launch

class HangulFriendApp : Application() {
    val container: AppContainer by lazy { AppContainer(this) }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        container.reminders.createChannel()
        // Re-books the reminder on every start and whenever its switch or time changes (also after an import).
        scope.launch {
            container.settings.settings
                .distinctUntilChangedBy { Triple(it.reminderEnabled, it.reminderMinutes, it.onboarded) }
                .collect { container.reminders.apply(it) }
        }
    }
}

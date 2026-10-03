package uz.hangulfriend.reminders

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit
import uz.hangulfriend.R
import uz.hangulfriend.i18n.LanguageStore
import uz.hangulfriend.i18n.Localized
import uz.hangulfriend.data.Settings

/** Keeps exactly one pending reminder run, at the next reminder time; WorkManager restores it after reboot. */
class ReminderScheduler(private val context: Context) {
    fun apply(s: Settings, trigger: Trigger) {
        val wm = WorkManager.getInstance(context)
        if (!s.reminderEnabled) {
            wm.cancelUniqueWork(WORK_NAME)
            return
        }
        val delay = ReminderPolicy.delayUntilNext(ZonedDateTime.now(), s.reminderMinutes)
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delay.toMillis(), TimeUnit.MILLISECONDS)
            .build()
        wm.enqueueUniqueWork(WORK_NAME, policyFor(trigger), request)
    }

    fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(CHANNEL_ID, Localized.wrap(context, LanguageStore(context).get()).getString(R.string.reminder_channel), NotificationManager.IMPORTANCE_DEFAULT)
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    enum class Trigger { APP_START, SETTINGS_CHANGED, WORKER }

    companion object {
        const val WORK_NAME = "daily_reminder"

        /**
         * App start must KEEP: WorkManager cold-starts the process for the reminder itself, and replacing then
         * would cancel the run that is about to notify. The worker appends tomorrow's run after itself.
         */
        fun policyFor(trigger: Trigger): ExistingWorkPolicy = when (trigger) {
            Trigger.APP_START -> ExistingWorkPolicy.KEEP
            Trigger.SETTINGS_CHANGED -> ExistingWorkPolicy.REPLACE
            Trigger.WORKER -> ExistingWorkPolicy.APPEND_OR_REPLACE
        }
        const val CHANNEL_ID = "reminders"
    }
}

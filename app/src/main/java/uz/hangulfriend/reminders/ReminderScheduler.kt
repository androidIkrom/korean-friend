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
import uz.hangulfriend.data.Settings

/** Keeps exactly one pending reminder run, at the next reminder time; WorkManager restores it after reboot. */
class ReminderScheduler(private val context: Context) {
    fun apply(s: Settings) {
        val wm = WorkManager.getInstance(context)
        if (!s.reminderEnabled) {
            wm.cancelUniqueWork(WORK_NAME)
            return
        }
        val delay = ReminderPolicy.delayUntilNext(ZonedDateTime.now(), s.reminderMinutes)
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delay.toMillis(), TimeUnit.MILLISECONDS)
            .build()
        wm.enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }

    fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(CHANNEL_ID, context.getString(R.string.reminder_channel), NotificationManager.IMPORTANCE_DEFAULT)
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        const val WORK_NAME = "daily_reminder"
        const val CHANNEL_ID = "reminders"
    }
}

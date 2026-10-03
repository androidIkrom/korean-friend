package uz.hangulfriend.reminders

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import uz.hangulfriend.HangulFriendApp
import uz.hangulfriend.MainActivity
import uz.hangulfriend.R
import uz.hangulfriend.i18n.LanguageStore
import uz.hangulfriend.i18n.Localized
import uz.hangulfriend.study.GameRules

/** Posts today's reminder unless the daily goal is met, then books tomorrow's run. */
class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val container = (applicationContext as HangulFriendApp).container
        val s = container.settings.settings.first()
        if (s.onboarded && s.reminderEnabled) {
            val goal = s.dailyGoalXp
            val reminder = ReminderPolicy.message(
                todayXp = container.game.todayXp(),
                goal = goal,
                dueCount = container.study.dueQueue(s.dailyNewLimit).size,
                streak = GameRules.streak(container.game.dailyXp(), LocalDate.now(container.clock), goal),
            )
            if (reminder != null) notify(ReminderText.of(Localized.wrap(applicationContext, LanguageStore(applicationContext).get()), reminder))
        }
        container.reminders.apply(s, ReminderScheduler.Trigger.WORKER)
        return Result.success()
    }

    private fun notify(text: String) {
        val ctx = applicationContext
        val granted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (!granted) return
        val open = Intent(ctx, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_OPEN, MainActivity.OPEN_REVIEW)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        val pending = PendingIntent.getActivity(ctx, 0, open, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val notification = NotificationCompat.Builder(ctx, ReminderScheduler.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(ctx.getString(R.string.app_name))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(ctx).notify(NOTIFICATION_ID, notification)
    }

    private companion object {
        const val NOTIFICATION_ID = 1
    }
}

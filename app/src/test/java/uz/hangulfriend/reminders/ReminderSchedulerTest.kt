package uz.hangulfriend.reminders

import androidx.work.ExistingWorkPolicy
import org.junit.Assert.assertEquals
import org.junit.Test

/** A cold start by WorkManager runs the app's onCreate too; it must not cancel the reminder that started it. */
class ReminderSchedulerTest {
    @Test fun appStartKeepsPendingOrRunningReminder() =
        assertEquals(ExistingWorkPolicy.KEEP, ReminderScheduler.policyFor(ReminderScheduler.Trigger.APP_START))

    @Test fun settingsChangeReplacesSchedule() =
        assertEquals(ExistingWorkPolicy.REPLACE, ReminderScheduler.policyFor(ReminderScheduler.Trigger.SETTINGS_CHANGED))

    @Test fun workerAppendsNextRunInsteadOfCancellingItself() =
        assertEquals(ExistingWorkPolicy.APPEND_OR_REPLACE, ReminderScheduler.policyFor(ReminderScheduler.Trigger.WORKER))
}

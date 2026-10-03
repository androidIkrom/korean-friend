package uz.hangulfriend.reminders

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import uz.hangulfriend.i18n.AppLanguage
import uz.hangulfriend.i18n.Localized

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ReminderTextTest {
    private val app: Context = ApplicationProvider.getApplicationContext()
    private val uz = Localized.wrap(app, AppLanguage.UZ)
    private val en = Localized.wrap(app, AppLanguage.EN)

    @Test fun uzbekWordingIsUnchanged() {
        assertEquals("Bugun 23 ta karta kutyapti. Streak: 12 kun 🔥", ReminderText.of(uz, Reminder.Due(23, 12)))
        assertEquals("Bugun 23 ta karta kutyapti.", ReminderText.of(uz, Reminder.Due(23, 0)))
        assertEquals(
            "Bugungi maqsadga hali yetmadingiz. 5 daqiqa mashq qilamizmi? Streak: 4 kun 🔥",
            ReminderText.of(uz, Reminder.Goal(4)),
        )
    }

    @Test fun englishUsesPlurals() {
        assertEquals("1 card is waiting today.", ReminderText.of(en, Reminder.Due(1, 0)))
        assertEquals("3 cards are waiting today. 2-day streak 🔥", ReminderText.of(en, Reminder.Due(3, 2)))
        assertEquals("You haven't reached today's goal yet. Practice for 5 minutes?", ReminderText.of(en, Reminder.Goal(0)))
    }
}

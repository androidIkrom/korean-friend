package uz.hangulfriend.reminders

import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReminderPolicyTest {
    private fun at(time: String): ZonedDateTime = LocalDateTime.parse("2026-10-01T$time").atZone(ZoneId.of("Asia/Tashkent"))

    @Test fun noMessageWhenGoalMet() = assertNull(ReminderPolicy.message(todayXp = 60, goal = 50, dueCount = 10, streak = 3))

    @Test fun messageWithDueAndStreak() = assertEquals(
        "Bugun 23 ta karta kutyapti. Streak: 12 kun 🔥",
        ReminderPolicy.message(todayXp = 0, goal = 50, dueCount = 23, streak = 12),
    )

    @Test fun messageWithDueNoStreak() =
        assertEquals("Bugun 23 ta karta kutyapti.", ReminderPolicy.message(todayXp = 0, goal = 50, dueCount = 23, streak = 0))

    @Test fun messageWithoutDue() = assertEquals(
        "Bugungi maqsadga hali yetmadingiz. 5 daqiqa mashq qilamizmi?",
        ReminderPolicy.message(todayXp = 10, goal = 50, dueCount = 0, streak = 0),
    )

    @Test fun messageWithoutDueKeepsStreak() = assertEquals(
        "Bugungi maqsadga hali yetmadingiz. 5 daqiqa mashq qilamizmi? Streak: 4 kun 🔥",
        ReminderPolicy.message(todayXp = 10, goal = 50, dueCount = 0, streak = 4),
    )

    @Test fun delayLaterToday() = assertEquals(Duration.ofMinutes(90), ReminderPolicy.delayUntilNext(at("18:30"), 1200))

    @Test fun delayWhenTimePassedIsTomorrow() = assertEquals(Duration.ofHours(23), ReminderPolicy.delayUntilNext(at("21:00"), 1200))

    @Test fun delayWhenExactlyNowIsTomorrow() = assertEquals(Duration.ofHours(24), ReminderPolicy.delayUntilNext(at("20:00"), 1200))
}

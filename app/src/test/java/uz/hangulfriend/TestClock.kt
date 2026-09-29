package uz.hangulfriend

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/** A clock that tests move by hand, in Tashkent time. */
class MutableClock(var now: Instant = Instant.parse("2026-10-01T06:00:00Z")) : Clock() {
    private val zoneId: ZoneId = ZoneId.of("Asia/Tashkent")

    override fun getZone(): ZoneId = zoneId

    override fun withZone(zone: ZoneId): Clock = throw UnsupportedOperationException()

    override fun instant(): Instant = now

    fun advance(d: Duration) {
        now = now.plus(d)
    }

    fun setLocal(dateTime: String) {
        now = LocalDateTime.parse(dateTime).atZone(zoneId).toInstant()
    }
}

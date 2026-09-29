package uz.hangulfriend.srs

import org.junit.Assert.assertEquals
import org.junit.Test

class AutoRatingTest {
    @Test fun autoRating_wrong() = assertEquals(Rating.AGAIN, AutoRating.from(false, false, 1000))

    @Test fun autoRating_wrongWithHint() = assertEquals(Rating.AGAIN, AutoRating.from(false, true, 1000))

    @Test fun autoRating_hint() = assertEquals(Rating.HARD, AutoRating.from(true, true, 1000))

    @Test fun autoRating_slow() = assertEquals(Rating.HARD, AutoRating.from(true, false, 15_001))

    @Test fun autoRating_boundary() = assertEquals(Rating.GOOD, AutoRating.from(true, false, 15_000))
}

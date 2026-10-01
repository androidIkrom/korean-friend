package uz.hangulfriend.study

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RankRulesTest {
    @Test fun boundaries() {
        mapOf(
            0 to Rank.E, 1 to Rank.E, 4 to Rank.E, 5 to Rank.D, 9 to Rank.D, 10 to Rank.C, 14 to Rank.C,
            15 to Rank.B, 19 to Rank.B, 20 to Rank.A, 29 to Rank.A, 30 to Rank.S, 99 to Rank.S,
        ).forEach { (lv, r) -> assertEquals("level $lv", r, RankRules.rankFor(lv)) }
    }

    @Test fun noDialogWithoutLastSeen() = assertNull(RankRules.rankUpToShow(Rank.C, null))

    @Test fun dialogWhenHigher() = assertEquals(Rank.C, RankRules.rankUpToShow(Rank.C, Rank.D))

    @Test fun noDialogWhenSameOrLower() {
        assertNull(RankRules.rankUpToShow(Rank.C, Rank.C))
        assertNull(RankRules.rankUpToShow(Rank.D, Rank.C))
    }
}

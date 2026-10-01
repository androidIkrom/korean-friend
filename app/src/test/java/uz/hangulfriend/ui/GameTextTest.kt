package uz.hangulfriend.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import uz.hangulfriend.study.GameRules
import uz.hangulfriend.ui.game.achievementDesc
import uz.hangulfriend.ui.game.achievementTitle

class GameTextTest {
    @Test fun achievementTitle_coversAll() {
        val titles = GameRules.ACHIEVEMENTS.map { achievementTitle(it) }
        val descs = GameRules.ACHIEVEMENTS.map { achievementDesc(it) }
        assertTrue((titles + descs).all { it != 0 })
        assertEquals(GameRules.ACHIEVEMENTS.size, titles.toSet().size)
        assertEquals(GameRules.ACHIEVEMENTS.size, descs.toSet().size)
    }
}

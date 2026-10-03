package uz.hangulfriend.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import uz.hangulfriend.ui.story.GATE_CLEAR_FROM
import uz.hangulfriend.ui.story.GATE_CLEAR_TO
import uz.hangulfriend.ui.story.gateLayout

class GateLayoutTest {
    @Test fun sameSeedSameScene() = assertEquals(gateLayout(42), gateLayout(42))

    @Test fun seedsDiffer() = assertNotEquals(gateLayout(1).rocks, gateLayout(2).rocks)

    @Test fun rocksAvoidTheGate() {
        repeat(50) { seed ->
            gateLayout(seed).rocks.forEach { assertTrue("seed $seed x ${it.x}", it.x !in GATE_CLEAR_FROM..GATE_CLEAR_TO) }
        }
    }

    @Test fun towersStayAtTheSides() {
        repeat(50) { seed ->
            val towers = gateLayout(seed).towers
            assertTrue(towers.isNotEmpty())
            towers.forEach { assertTrue("seed $seed $it", it.x + it.w <= 0.38f || it.x >= 0.62f) }
        }
    }

    @Test fun twoRuneColumns() = assertEquals(listOf(7, 7), gateLayout(3).runes.map { it.size })
}

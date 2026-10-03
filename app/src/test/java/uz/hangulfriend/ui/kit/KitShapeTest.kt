package uz.hangulfriend.ui.kit

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Test

class KitShapeTest {
    @Test fun cutCornersAreCut() {
        assertEquals(
            listOf(Offset(10f, 0f), Offset(100f, 0f), Offset(100f, 40f), Offset(90f, 50f), Offset(0f, 50f), Offset(0f, 10f)),
            cutPolygon(100f, 50f, 10f),
        )
    }

    @Test fun cutIsClampedToHalfTheShortSide() {
        val p = cutPolygon(100f, 20f, 50f)
        assertEquals(Offset(10f, 0f), p[0])
        assertEquals(Offset(90f, 20f), p[3])
    }
}

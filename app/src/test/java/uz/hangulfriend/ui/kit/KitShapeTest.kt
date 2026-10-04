package uz.hangulfriend.ui.kit

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test
import uz.hangulfriend.data.GameThemeId
import uz.hangulfriend.ui.theme.tokensFor

class KitShapeTest {
    @Test fun cutCornersAreCut() {
        assertEquals(
            listOf(Offset(10f, 0f), Offset(100f, 0f), Offset(100f, 40f), Offset(90f, 50f), Offset(0f, 50f), Offset(0f, 10f)),
            cutPolygon(100f, 50f, 10f),
        )
    }

    @Test fun themeShapesAreRounded() = GameThemeId.entries.forEach { id ->
        val t = tokensFor(id)
        assertEquals(RoundedCornerShape(24.dp), t.shape())
        assertEquals(RoundedCornerShape(24.dp), t.shape(12.dp))
        assertEquals(RoundedCornerShape(16.dp), t.shape(6.dp))
    }

    @Test fun cutIsClampedToHalfTheShortSide() {
        val p = cutPolygon(100f, 20f, 50f)
        assertEquals(Offset(10f, 0f), p[0])
        assertEquals(Offset(90f, 20f), p[3])
    }
}

package uz.hangulfriend.ui.kit

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test
import uz.hangulfriend.data.GameThemeId
import uz.hangulfriend.ui.theme.tokensFor

class KitShapeTest {
    @Test fun themeShapesAreRounded() = GameThemeId.entries.forEach { id ->
        val t = tokensFor(id)
        assertEquals(RoundedCornerShape(24.dp), t.shape())
        assertEquals(RoundedCornerShape(24.dp), t.shape(12.dp))
        assertEquals(RoundedCornerShape(16.dp), t.shape(6.dp))
    }
}

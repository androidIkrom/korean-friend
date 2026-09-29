package uz.hangulfriend.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import uz.hangulfriend.content.Line
import uz.hangulfriend.ui.lesson.roleplaySteps
import uz.hangulfriend.ui.lesson.speakers

class RoleplayTest {
    private val lines = listOf(
        Line("seller", "어서 오세요.", "Xush kelibsiz."),
        Line("aziz", "코트를 찾아요.", "Palto qidiryapman."),
        Line("seller", "이건 어떠세요?", "Bu qanday?"),
        Line("minji", "예뻐요!", "Chiroyli!"),
    )

    @Test fun roleplaySteps_marksMyLines() {
        val steps = roleplaySteps(lines, myRole = "aziz")
        assertEquals(listOf(false, true, false, false), steps.map { it.mine })
        assertEquals(listOf(0, 1, 2, 3), steps.map { it.index })
    }

    @Test fun speakers_inFirstAppearanceOrder() = assertEquals(listOf("seller", "aziz", "minji"), speakers(lines))
}

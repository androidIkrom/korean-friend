package uz.hangulfriend.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DockTest {
    @Test fun tabsMapToTheirRoutes() {
        assertEquals(DockTab.LOBBY, dockTabFor(Routes.HOME))
        assertEquals(DockTab.GATES, dockTabFor(Routes.MAP))
        assertEquals(DockTab.STORY, dockTabFor(Routes.STORIES))
        assertEquals(DockTab.SYSTEM, dockTabFor(Routes.SETTINGS))
        assertNull(dockTabFor(Routes.LESSON))
        assertNull(dockTabFor(null))
    }

    @Test fun huntTarget() {
        assertEquals("lesson/u02_l1", huntTarget("u02_l1"))
        assertEquals(Routes.MAP, huntTarget(null))
    }
}

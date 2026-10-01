package uz.hangulfriend.ui

import androidx.compose.ui.graphics.vector.PathParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import uz.hangulfriend.data.GameThemeId
import uz.hangulfriend.study.Rank
import uz.hangulfriend.ui.avatar.AvatarAssets
import uz.hangulfriend.ui.avatar.AvatarLayer
import uz.hangulfriend.ui.avatar.AvatarPaths
import uz.hangulfriend.ui.avatar.layersFor
import uz.hangulfriend.ui.avatar.newLayersAt

class AvatarLayersTest {
    @Test fun eHasHoodOthersNot() {
        assertTrue(AvatarLayer.HOOD in layersFor(Rank.E))
        Rank.entries.drop(1).forEach { assertFalse("$it", AvatarLayer.HOOD in layersFor(it)) }
    }

    @Test fun cumulative() {
        Rank.entries.zipWithNext().drop(1).forEach { (a, b) -> assertTrue("$a ⊂ $b", layersFor(b).containsAll(layersFor(a))) }
    }

    @Test fun sHasCompanions() = assertTrue(
        layersFor(Rank.S).containsAll(setOf(AvatarLayer.MONARCH_AURA, AvatarLayer.EYE_TRAIL, AvatarLayer.COMPANIONS)),
    )

    @Test fun eachRankAfterEAnnouncesSomething() = Rank.entries.drop(1).forEach { assertTrue("$it", newLayersAt(it).isNotEmpty()) }
}

class AvatarPathsTest {
    @Test fun allPathsParse() = AvatarPaths.all.forEach { (name, d) ->
        assertTrue(name, PathParser().parsePathString(d).toNodes().isNotEmpty())
    }
}

class AvatarAssetsTest {
    private val a = AvatarAssets(setOf("system_c.webp", "README.md", "neon_S.webp"))

    @Test fun findsSlot() = assertEquals("avatar/system_c.webp", a.slotFor(GameThemeId.SYSTEM, Rank.C))

    @Test fun missingSlot() = assertNull(a.slotFor(GameThemeId.SYSTEM, Rank.B))

    @Test fun caseMustMatch() = assertNull(a.slotFor(GameThemeId.NEON, Rank.S))
}

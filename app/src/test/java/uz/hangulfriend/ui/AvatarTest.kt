package uz.hangulfriend.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.PathParser
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import uz.hangulfriend.data.GameThemeId
import uz.hangulfriend.data.HeroGender
import uz.hangulfriend.study.Rank
import uz.hangulfriend.ui.avatar.AvatarAssets
import uz.hangulfriend.ui.avatar.AvatarLayer
import uz.hangulfriend.ui.avatar.AvatarPaths
import uz.hangulfriend.ui.avatar.catmullRom
import uz.hangulfriend.ui.avatar.eyeStyleFor
import uz.hangulfriend.ui.avatar.flameOutline
import uz.hangulfriend.ui.avatar.layersFor
import uz.hangulfriend.ui.avatar.newLayersAt

class AvatarLayersTest {
    private val heroes = HeroGender.entries

    @Test fun cumulativeForBoth() = heroes.forEach { h ->
        Rank.entries.zipWithNext().drop(1).forEach { (a, b) ->
            val lower = layersFor(a, h) - AvatarLayer.LONG_HAIR
            assertTrue("$h $a ⊂ $b", layersFor(b, h).containsAll(lower))
        }
    }

    @Test fun hoodOnlyAtE() = heroes.forEach { h ->
        assertTrue(AvatarLayer.HOOD in layersFor(Rank.E, h))
        Rank.entries.drop(1).forEach { assertFalse("$h $it", AvatarLayer.HOOD in layersFor(it, h)) }
    }

    @Test fun girlHairTiesAtC() = Rank.entries.forEach { r ->
        val l = layersFor(r, HeroGender.GIRL)
        assertEquals("$r", r < Rank.C, AvatarLayer.LONG_HAIR in l)
        assertEquals("$r", r >= Rank.C, AvatarLayer.PONYTAIL in l)
    }

    @Test fun girlNeverGetsBoyParts() = Rank.entries.forEach { r ->
        assertFalse(AvatarLayer.TWIN_BLADES in layersFor(r, HeroGender.GIRL))
        val boy = layersFor(r, HeroGender.BOY)
        listOf(AvatarLayer.SWORD, AvatarLayer.TIARA, AvatarLayer.PONYTAIL, AvatarLayer.LONG_HAIR).forEach { assertFalse("$r $it", it in boy) }
    }

    @Test fun weaponsAtA() {
        assertTrue(AvatarLayer.TWIN_BLADES in layersFor(Rank.A, HeroGender.BOY))
        assertTrue(AvatarLayer.SWORD in layersFor(Rank.A, HeroGender.GIRL))
        assertTrue(AvatarLayer.TIARA in layersFor(Rank.S, HeroGender.GIRL))
    }

    @Test fun eachRankAfterEAnnouncesSomething() = heroes.forEach { h ->
        Rank.entries.drop(1).forEach { assertTrue("$h $it", newLayersAt(it, h).isNotEmpty()) }
    }
}

class EyeStyleTest {
    @Test fun noGlowBelowC() {
        assertFalse(eyeStyleFor(Rank.E).glowing)
        assertFalse(eyeStyleFor(Rank.D).glowing)
        assertTrue(eyeStyleFor(Rank.C).glowing)
    }

    @Test fun bloomGrows() {
        val r = listOf(Rank.C, Rank.B, Rank.A, Rank.S).map { eyeStyleFor(it).bloomRadius }
        assertEquals(r.sorted(), r)
        assertEquals(r.distinct().size, r.size)
    }

    @Test fun streakOnlyAtA() = Rank.entries.forEach { assertEquals("$it", it == Rank.A, eyeStyleFor(it).streak) }

    @Test fun trailOnlyAtS() = Rank.entries.forEach { assertEquals("$it", it == Rank.S, eyeStyleFor(it).flameTrail) }

    @Test fun monarchOnlyAtS() = Rank.entries.forEach { assertEquals("$it", it == Rank.S, eyeStyleFor(it).monarch) }

    @Test fun eyePathsParse() = Rank.entries.forEach { r ->
        val e = eyeStyleFor(r)
        listOf(e.lid, e.brow, e.mouth).forEach { assertTrue("$r $it", PathParser().parsePathString(it).toNodes().isNotEmpty()) }
    }
}

class FlameShapeTest {
    @Test fun pointCount() = listOf(0.3f, 0.55f, 0.8f, 1f).forEach { k ->
        val lobes = (6 + 4 * k).toInt()
        assertEquals("k=$k", 4 * lobes + 3, flameOutline(k, 1f, 0).size)
    }

    /** Each lobe bulges outward: its crest lies farther from the body (normalised to the ellipse) than its shoulders. */
    @Test fun crestsBulgeOutward() = listOf(0.3f, 0.55f, 0.8f, 1f).forEach { k ->
        val p = flameOutline(k, 1f, 0)
        val lobes = (6 + 4 * k).toInt()
        fun reach(o: Offset) = Offset((o.x - 100f) / 88f, (o.y - 150f) / 118f).getDistance()
        repeat(lobes) { j ->
            val base = 1 + 4 * j
            val crest = reach(p[base + 2])
            assertTrue("k=$k lobe $j", crest > reach(p[base + 1]) && crest > reach(p[base + 3]))
        }
    }

    @Test fun jointsAreSmooth() {
        val segs = catmullRom(flameOutline(1f, 1f, 0))
        segs.indices.forEach { i ->
            val cur = segs[i]
            val next = segs[(i + 1) % segs.size]
            val inTangent = cur.end - cur.c2
            val outTangent = next.c1 - cur.end
            val cross = inTangent.x * outTangent.y - inTangent.y * outTangent.x
            val scale = inTangent.getDistance() * outTangent.getDistance()
            assertTrue("joint $i", abs(cross) <= 1e-3f * scale + 1e-4f)
        }
    }

    @Test fun deterministic() = assertEquals(flameOutline(0.8f, 0.86f, 3), flameOutline(0.8f, 0.86f, 3))

    private operator fun Offset.minus(o: Offset) = Offset(x - o.x, y - o.y)
}

class AvatarPathsTest {
    @Test fun allPathsParse() = AvatarPaths.all.forEach { (name, d) ->
        assertTrue(name, PathParser().parsePathString(d).toNodes().isNotEmpty())
    }
}

class AvatarAssetsTest {
    private val a = AvatarAssets(setOf("system_girl_c.webp", "README.md", "neon_boy_S.webp"))

    @Test fun findsSlot() = assertEquals("avatar/system_girl_c.webp", a.slotFor(GameThemeId.SYSTEM, HeroGender.GIRL, Rank.C))

    @Test fun otherHeroMissing() = assertNull(a.slotFor(GameThemeId.SYSTEM, HeroGender.BOY, Rank.C))

    @Test fun caseMustMatch() = assertNull(a.slotFor(GameThemeId.NEON, HeroGender.BOY, Rank.S))
}

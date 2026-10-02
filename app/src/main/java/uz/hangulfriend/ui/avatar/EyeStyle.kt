package uz.hangulfriend.ui.avatar

import uz.hangulfriend.study.Rank

/**
 * The left eye, brow and mouth at one rank (right eye mirrors around x = 100). Confidence and power grow with rank:
 * a hesitant round eye at E, a narrowed glowing eye at C–A, the violet monarch gaze with a flame trail at S.
 */
data class EyeStyle(
    val lid: String,
    val brow: String,
    val browWidth: Float,
    val mouth: String,
    val bloomRadius: Float,
    val bloomAlpha: Float,
    /** False: a plain eye (sclera, iris, highlight); true: the lid itself glows. */
    val glowing: Boolean,
    val hotCore: Boolean,
    val streak: Boolean,
    val flameTrail: Boolean,
    val monarch: Boolean,
)

const val EYE_STREAK = "M73 103 L54 98"
const val EYE_TRAIL_FILL = "M73 103 C60 99 48 94 34 80 C46 90 58 96 73 100 Z"
const val EYE_TRAIL_LINE = "M73 103 C62 100 52 96 40 86"
const val GIRL_LASH_GLOW = "M72 102 L95 98.5"

private const val SHARP_LID = "M73 103 L96 99 L94 104 C88 107 80 107 73 103 Z"
private const val SHARP_BROW = "M70 86 L97 96"
private const val SMIRK = "M94 132 Q101 133.5 107 130"

fun eyeStyleFor(rank: Rank): EyeStyle = when (rank) {
    Rank.E -> EyeStyle(
        lid = "M74 104 C78 98 90 97 95 102 C90 108 79 109 74 104 Z", brow = "M72 93 Q84 91 95 85", browWidth = 2.4f,
        mouth = "M93 134 Q100 131 107 134", bloomRadius = 0f, bloomAlpha = 0f,
        glowing = false, hotCore = false, streak = false, flameTrail = false, monarch = false,
    )
    Rank.D -> EyeStyle(
        lid = "M74 104 C78 99 90 99 95 103 C90 107 79 108 74 104 Z", brow = "M72 91 Q84 88 95 90", browWidth = 2.4f,
        mouth = "M94 133 L106 133", bloomRadius = 0f, bloomAlpha = 0f,
        glowing = false, hotCore = false, streak = false, flameTrail = false, monarch = false,
    )
    Rank.C -> EyeStyle(
        lid = "M74 103 L95 100 C91 106 80 107 74 103 Z", brow = "M72 89 Q84 89 96 95", browWidth = 2.6f,
        mouth = SMIRK, bloomRadius = 9f, bloomAlpha = 0.35f,
        glowing = true, hotCore = false, streak = false, flameTrail = false, monarch = false,
    )
    Rank.B -> EyeStyle(
        lid = SHARP_LID, brow = SHARP_BROW, browWidth = 3.2f, mouth = SMIRK, bloomRadius = 12f, bloomAlpha = 0.5f,
        glowing = true, hotCore = false, streak = false, flameTrail = false, monarch = false,
    )
    Rank.A -> EyeStyle(
        lid = SHARP_LID, brow = SHARP_BROW, browWidth = 3.2f, mouth = SMIRK, bloomRadius = 15f, bloomAlpha = 0.6f,
        glowing = true, hotCore = true, streak = true, flameTrail = false, monarch = false,
    )
    Rank.S -> EyeStyle(
        lid = SHARP_LID, brow = SHARP_BROW, browWidth = 3.2f, mouth = "M94 132.5 Q100 134 106 132",
        bloomRadius = 19f, bloomAlpha = 0.75f,
        glowing = true, hotCore = true, streak = false, flameTrail = true, monarch = true,
    )
}

/** The upper-lid curve of a plain (E/D) lid, used for the girl's lash line. */
fun upperLid(lid: String): String = lid.substringBefore(" C90")

package uz.hangulfriend.ui.avatar

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import uz.hangulfriend.ui.theme.LocalGameTokens

/** Silhouette of a story character other than Aziz: Minji's shape for female voices, a plain male bust otherwise. */
@Composable
fun SpeakerArt(voice: String, modifier: Modifier = Modifier) {
    val t = LocalGameTokens.current
    val p = paletteFor(t.id)
    val line = t.accent2
    val sw = 1.6f
    Canvas(modifier) {
        inViewBox {
            if (voice == "female") {
                part("MINJI_BACK", p.hair, line, sw)
                part("MINJI_BODY", p.coat, line, sw)
                part("MINJI_FACE", p.skin, line, sw)
                part("MINJI_LOCK_L", p.hair, line, sw)
                part("MINJI_LOCK_R", p.hair, line, sw)
                part("MINJI_BANGS", p.hair, line, sw)
                part("MINJI_EYE_L", p.eyeGlow, null, 0f)
                part("MINJI_EYE_R", p.eyeGlow, null, 0f)
            } else {
                part("BODY", p.coat, line, sw)
                part("NECK", p.shade, null, 0f)
                part("SHIRT", p.shirt, line, sw)
                part("FACE", p.skin, line, sw)
                part("HAIR", p.hair, line, sw)
                part("EYE_L", p.eyeGlow, null, 0f)
                part("EYE_R", p.eyeGlow, null, 0f)
            }
            part("MOUTH", null, Color(0xFF34507A), 1.8f, StrokeCap.Round)
        }
    }
}

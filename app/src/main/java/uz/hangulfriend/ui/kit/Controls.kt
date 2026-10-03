package uz.hangulfriend.ui.kit

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.hangulfriend.ui.theme.LocalGameTokens

/** A square System switch: the knob slides right and lights up when on. */
@Composable
fun HuntToggle(checked: Boolean, onCheckedChange: (Boolean) -> Unit, label: String, modifier: Modifier = Modifier) {
    val t = LocalGameTokens.current
    val feedback = LocalGameFeedback.current
    val knob by animateDpAsState(if (checked) 24.dp else 2.dp, tween(GameMotion.FAST), label = "toggleKnob")
    val rim by animateColorAsState(if (checked) t.accent else t.muted.copy(alpha = 0.5f), tween(GameMotion.FAST), label = "toggleRim")
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .toggleable(value = checked, role = Role.Switch) {
                feedback.play(Sfx.TAP)
                onCheckedChange(it)
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = t.text, modifier = Modifier.weight(1f))
        Box(
            Modifier
                .size(48.dp, 24.dp)
                .background(if (checked) t.accent.copy(alpha = 0.22f) else t.background)
                .border(1.dp, rim),
        ) {
            Box(
                Modifier
                    .offset(x = knob, y = 3.dp)
                    .size(20.dp, 16.dp)
                    .then(if (checked) Modifier.shapeGlow(t.accent.copy(alpha = 0.7f), RectangleShape, 6.dp) else Modifier)
                    .background(rim),
            )
        }
    }
}

/**
 * A number picked on a row of segments ([min]..[max] in [step]s): tap or drag; segments up to the
 * value light up. The label above shows [title] with the current value.
 */
@Composable
fun SegmentSlider(title: @Composable (Int) -> String, value: Int, min: Int, max: Int, step: Int, onChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    val t = LocalGameTokens.current
    val feedback = LocalGameFeedback.current
    val count = (max - min) / step + 1
    var shown by remember(value) { mutableIntStateOf(value) }
    fun valueAt(x: Float, width: Int): Int {
        val i = ((x / width) * count).toInt().coerceIn(0, count - 1)
        return min + i * step
    }
    fun update(v: Int) {
        if (v != shown) {
            shown = v
            feedback.play(Sfx.TAP)
        }
    }
    val label = title(shown)
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, color = t.text, fontFamily = t.display, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        Row(
            Modifier
                .fillMaxWidth()
                .height(28.dp)
                .semantics {
                    contentDescription = label
                    stateDescription = shown.toString()
                }
                .pointerInput(count) {
                    detectTapGestures { pos ->
                        val v = valueAt(pos.x, size.width)
                        update(v)
                        onChange(v)
                    }
                }
                .pointerInput(count) {
                    detectHorizontalDragGestures(
                        onDragEnd = { onChange(shown) },
                    ) { change, _ -> update(valueAt(change.position.x, size.width)) }
                },
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val lit = (shown - min) / step
            repeat(count) { i ->
                val on = i <= lit
                Box(
                    Modifier
                        .weight(1f)
                        .height(if (i == lit) 18.dp else 12.dp)
                        .then(if (i == lit) Modifier.shapeGlow(t.accent.copy(alpha = 0.8f), RectangleShape, 6.dp) else Modifier)
                        .background(if (on) t.accent else t.accent.copy(alpha = 0.14f)),
                )
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(min.toString(), color = t.muted, fontSize = 11.sp)
            Text(max.toString(), color = t.muted, fontSize = 11.sp)
        }
    }
}

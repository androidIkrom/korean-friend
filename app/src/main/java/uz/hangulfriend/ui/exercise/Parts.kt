package uz.hangulfriend.ui.exercise

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import uz.hangulfriend.R
import androidx.compose.material3.OutlinedButton
import uz.hangulfriend.ai.TutorPrompts
import uz.hangulfriend.ui.theme.CorrectGreen
import uz.hangulfriend.ui.tutor.LocalTutor
import uz.hangulfriend.ui.tutor.TutorSheet
import uz.hangulfriend.ui.theme.WrongRed

/** What the learner sees after checking an answer. */
data class FeedbackInfo(
    val correct: Boolean,
    val message: String,
    val correctAnswer: String?,
    val why: String?,
    val samples: List<String> = emptyList(),
    /** Question for the AI tutor about this mistake; the "AI'dan so'rash" button shows when set and wrong. */
    val askAi: String? = null,
)

@Composable
fun FeedbackPanel(info: FeedbackInfo, onNext: () -> Unit) {
    val color: Color = if (info.correct) CorrectGreen else WrongRed
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.15f)),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(info.message, color = color, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (!info.correct && info.correctAnswer != null) {
                Text(stringResource(R.string.ex_correct_answer, info.correctAnswer), style = MaterialTheme.typography.bodyLarge)
            }
            if (info.samples.isNotEmpty()) {
                Text(stringResource(R.string.ex_sample_answers), style = MaterialTheme.typography.labelLarge)
                info.samples.forEach { Text("• $it") }
            }
            info.why?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            val question = info.askAi
            if (!info.correct && question != null && LocalTutor.current != null) {
                var open by remember { mutableStateOf(false) }
                OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.ai_ask_mistake))
                }
                if (open) TutorSheet(TutorPrompts.ROLE, question) { open = false }
            }
            Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.ex_next)) }
        }
    }
}

/** A "Yordam" button that reveals [hint] and reports its first use. */
@Composable
fun HintButton(hint: String?, enabled: Boolean, onUsed: () -> Unit) {
    if (hint == null) return
    var shown by remember { mutableStateOf(false) }
    if (shown) {
        Text("💡 $hint", style = MaterialTheme.typography.bodyMedium)
    } else {
        TextButton(onClick = { shown = true; onUsed() }, enabled = enabled) { Text(stringResource(R.string.ex_hint)) }
    }
}

@Composable
fun PromptText(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium)
}

@Composable
fun KoreanText(text: String) {
    Text(text, style = MaterialTheme.typography.headlineMedium)
}

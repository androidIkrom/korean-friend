package uz.hangulfriend.ui.tutor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mikepenz.markdown.m3.Markdown
import kotlinx.coroutines.launch
import uz.hangulfriend.R
import uz.hangulfriend.ai.AiResult
import uz.hangulfriend.ai.ChatMessage
import uz.hangulfriend.ai.TutorService
import uz.hangulfriend.ai.cleanAiText
import com.mikepenz.markdown.m3.markdownTypography
import uz.hangulfriend.ui.kit.IconAction
import uz.hangulfriend.ui.kit.PanelTitle
import uz.hangulfriend.ui.kit.shape
import uz.hangulfriend.ui.theme.LocalGameTokens
import androidx.compose.ui.text.font.FontWeight
import uz.hangulfriend.ui.theme.WrongRed

val LocalTutor = staticCompositionLocalOf<TutorService?> { null }

fun aiStatusText(r: AiResult): Int? = when (r) {
    is AiResult.Success -> null
    AiResult.QuotaExhausted -> R.string.ai_quota
    AiResult.Offline -> R.string.ai_offline
    AiResult.NotConfigured -> R.string.ai_not_configured
    is AiResult.Failed -> R.string.ai_failed
}

/** Chat with the AI tutor. [firstQuestion] is sent as soon as the sheet opens. History lives only while it is open. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TutorSheet(system: String, firstQuestion: String, onDismiss: () -> Unit) {
    val tutor = LocalTutor.current ?: return
    val scope = rememberCoroutineScope()
    val messages = remember { mutableStateListOf<ChatMessage>() }
    var loading by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<Int?>(null) }
    var input by remember { mutableStateOf("") }

    fun send(text: String) {
        if (text.isBlank() || loading) return
        messages += ChatMessage(fromUser = true, text = text.trim())
        loading = true
        status = null
        scope.launch {
            val result = tutor.ask(system, messages.toList())
            if (result is AiResult.Success) messages += ChatMessage(fromUser = false, text = result.text)
            status = aiStatusText(result)
            loading = false
        }
    }

    LaunchedEffect(Unit) { send(firstQuestion) }

    val t = LocalGameTokens.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = t.panel.compositeOver(t.background),
    ) {
        Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // The `[ SYSTEM · AI ]` window title with a scan line.
            Box(Modifier.fillMaxWidth().clip(t.shape(6.dp)).padding(vertical = 6.dp)) {
                PanelTitle(stringResource(R.string.ai_title))
            }
            Column(
                Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                messages.forEachIndexed { i, m ->
                    // The first user message is the long generated context question; show it short.
                    val text = if (i == 0 && m.fromUser) stringResource(R.string.ai_first_question_label) else m.text
                    Column(
                        Modifier.fillMaxWidth(),
                        horizontalAlignment = if (m.fromUser) Alignment.End else Alignment.Start,
                    ) {
                        val bubble = t.shape(10.dp)
                        val rim = if (m.fromUser) t.accent else t.accent2
                        Column(
                            Modifier
                                .widthIn(max = 320.dp)
                                .background(if (m.fromUser) t.accent.copy(alpha = 0.16f) else t.background.copy(alpha = 0.6f), bubble)
                                .padding(12.dp),
                        ) {
                            if (m.fromUser) Text(text, color = t.text) else Markdown(content = cleanAiText(text), typography = compactMarkdownTypography())
                        }
                    }
                }
                if (loading) CircularProgressIndicator(Modifier.padding(8.dp), color = t.accent)
                status?.let { Text(stringResource(it), color = WrongRed) }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    placeholder = { Text(stringResource(R.string.ai_input_hint)) },
                    modifier = Modifier.weight(1f),
                )
                IconAction(
                    Icons.AutoMirrored.Filled.Send,
                    stringResource(R.string.ai_send),
                    onClick = {
                        send(input)
                        input = ""
                    },
                    enabled = input.isNotBlank() && !loading,
                )
            }
        }
    }
}

/** Headings close to body size instead of the default display sizes: chat bubbles and grammar notes. */
@Composable
fun compactMarkdownTypography() = with(MaterialTheme.typography) {
    val accent = LocalGameTokens.current.accent
    val big = titleLarge.copy(fontWeight = FontWeight.Bold, color = accent)
    val small = titleMedium.copy(fontWeight = FontWeight.Bold, color = accent)
    markdownTypography(h1 = big, h2 = big, h3 = small, h4 = small, h5 = small, h6 = small)
}

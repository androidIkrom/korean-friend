package uz.hangulfriend.ui.lesson

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikepenz.markdown.m3.Markdown
import uz.hangulfriend.R
import uz.hangulfriend.ai.TutorPrompts
import uz.hangulfriend.ui.currentLanguage
import uz.hangulfriend.content.Character
import uz.hangulfriend.content.Grammar
import uz.hangulfriend.content.Lesson
import uz.hangulfriend.content.Word
import uz.hangulfriend.study.ExerciseItem
import uz.hangulfriend.ui.AudioButton
import uz.hangulfriend.ui.exercise.ExerciseView
import uz.hangulfriend.ui.exercise.MatchView
import uz.hangulfriend.ui.theme.CorrectGreen
import uz.hangulfriend.ui.kit.GlowBar
import uz.hangulfriend.ui.kit.HuntButton
import uz.hangulfriend.ui.kit.HuntPanel
import uz.hangulfriend.ui.kit.HuntStyle
import uz.hangulfriend.ui.kit.HuntToggle
import uz.hangulfriend.ui.kit.PanelTitle
import uz.hangulfriend.ui.kit.shape
import uz.hangulfriend.ui.theme.GameCard
import uz.hangulfriend.ui.theme.LocalGameTokens
import uz.hangulfriend.ui.theme.WrongRed
import uz.hangulfriend.ui.tutor.LocalTutor
import uz.hangulfriend.ui.tutor.TutorSheet
import uz.hangulfriend.ui.tutor.compactMarkdownTypography
import uz.hangulfriend.ui.settings.LocalPlayerName

/** Pages through the stage; the last page's "next" moves to the following stage. */
@Composable
private fun Pager(pageCount: Int, onFinished: () -> Unit, page: @Composable (Int, () -> Unit) -> Unit) {
    if (pageCount == 0) {
        // Nothing to show (e.g. a lesson without words): move straight on instead of indexing an empty list.
        LaunchedEffect(Unit) { onFinished() }
        return
    }
    var index by rememberSaveable { mutableIntStateOf(0) }
    val next: () -> Unit = {
        if (index + 1 < pageCount) index++ else onFinished()
    }
    val t = LocalGameTokens.current
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("${index + 1} / $pageCount", color = t.muted, fontFamily = t.ui, fontSize = 12.sp, letterSpacing = 1.sp)
            GlowBar((index + 1f) / pageCount, Modifier.weight(1f), height = 4)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            key(index) { page(index, next) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 8.dp)) {
            HuntButton(
                stringResource(R.string.pager_prev),
                onClick = { index-- },
                enabled = index > 0,
                modifier = Modifier.weight(1f),
                style = HuntStyle.SECONDARY,
                fontSize = 13,
                horizontalPadding = 8.dp,
            )
            HuntButton(
                stringResource(if (index + 1 < pageCount) R.string.pager_next else R.string.pager_next_stage),
                onClick = next,
                modifier = Modifier.weight(1.4f),
                fontSize = 13,
                horizontalPadding = 8.dp,
            )
        }
    }
}

private sealed interface VocabPage {
    data class Card(val word: Word) : VocabPage
    data class Quiz(val words: List<Word>) : VocabPage
}

@Composable
fun VocabStage(lesson: Lesson, vm: LessonViewModel, onFinished: () -> Unit) {
    val pages = remember(lesson) {
        vm.vocabChunks(lesson).flatMap { chunk ->
            chunk.map { VocabPage.Card(it) } + if (chunk.size >= 2) listOf(VocabPage.Quiz(chunk)) else emptyList()
        }
    }
    Pager(pages.size, onFinished) { i, next ->
        when (val p = pages[i]) {
            is VocabPage.Card -> WordCard(p.word)
            is VocabPage.Quiz -> {
                val item = ExerciseItem.Match(p.words)
                MatchView(p.words, onResult = { vm.grade(item, it) }, onNext = next, showNext = false)
            }
        }
    }
}

@Composable
private fun WordCard(word: Word) {
    val t = LocalGameTokens.current
    HuntPanel(accent = t.accent, padding = 20.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(word.ko, color = t.text, style = MaterialTheme.typography.displaySmall, modifier = Modifier.weight(1f))
            AudioButton(word.audio)
        }
        Text(word.uz, color = t.text, style = MaterialTheme.typography.titleLarge)
        Text(word.pos, color = t.accent2, fontFamily = t.ui, fontSize = 11.sp)
        HorizontalDivider(color = t.panelBorder.copy(alpha = 0.5f))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(word.exampleKo, color = t.text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            AudioButton(word.exampleAudio)
        }
        Text(word.exampleUz, color = t.muted, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun GrammarStage(lesson: Lesson, vm: LessonViewModel, onFinished: () -> Unit) {
    Pager(lesson.grammar.size, onFinished) { i, next ->
        val g = lesson.grammar[i]
        GrammarPage(g)
        if (LocalTutor.current != null) {
            var open by remember(g.id) { mutableStateOf(false) }
            HuntButton(
                stringResource(R.string.ai_explain_grammar),
                onClick = { open = true },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                style = HuntStyle.SECONDARY,
                icon = Icons.Outlined.AutoAwesome,
                minHeight = 44.dp,
                fontSize = 13,
            )
            if (open) {
                TutorSheet(TutorPrompts.grammarContext(g, lesson, currentLanguage(), LocalPlayerName.current), stringResource(R.string.ai_explain_grammar_question)) {
                    open = false
                }
            }
        }
        val check = remember(g.id) { vm.grammarCheck(lesson, g.id) }
        if (check != null) {
            Spacer(Modifier.padding(8.dp))
            PanelTitle(stringResource(R.string.grammar_check))
            Spacer(Modifier.padding(4.dp))
            ExerciseView(check, onResult = { vm.grade(check, it) }, onNext = next)
        }
    }
}

@Composable
private fun GrammarPage(g: Grammar) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val t = LocalGameTokens.current
        Text(g.pattern, color = t.accent, style = MaterialTheme.typography.headlineMedium)
        Text(g.meaningUz, color = t.text, style = MaterialTheme.typography.titleMedium)
        GameCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                g.formation.forEach { f ->
                    Row {
                        Text(f.conditionUz, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        Text(f.rule, Modifier.weight(0.6f), fontWeight = FontWeight.Bold)
                        Text(f.example, Modifier.weight(1f))
                    }
                }
            }
        }
        Markdown(content = g.explanationMd, typography = compactMarkdownTypography())
        SectionTitle(R.string.grammar_examples)
        g.examples.forEach { e ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(e.ko, style = MaterialTheme.typography.bodyLarge)
                    Text(e.uz, style = MaterialTheme.typography.bodyMedium)
                }
                AudioButton(e.audio)
            }
        }
        SectionTitle(R.string.grammar_mistakes)
        g.mistakes.forEach { m ->
            Column {
                Text(m.wrong, color = WrongRed, textDecoration = TextDecoration.LineThrough)
                Text(m.right, color = CorrectGreen, fontWeight = FontWeight.Bold)
                Text(m.whyUz, style = MaterialTheme.typography.bodySmall)
            }
        }
        SectionTitle(R.string.grammar_uz_compare)
        Text(g.uzCompare)
    }
}

@Composable
private fun SectionTitle(res: Int) {
    Box(Modifier.padding(top = 8.dp)) { PanelTitle(stringResource(res)) }
}

@Composable
fun DialogueStage(lesson: Lesson, characters: Map<String, Character>, onFinished: () -> Unit) {
    val t = LocalGameTokens.current
    var showTranslation by rememberSaveable { mutableStateOf(false) }
    var roleplay by rememberSaveable { mutableStateOf(false) }
    val first = lesson.dialogue.lines.firstOrNull()?.speaker
    Column(Modifier.fillMaxSize()) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HuntButton(
                stringResource(R.string.dialogue_mode_listen),
                onClick = { roleplay = false },
                modifier = Modifier.weight(1f),
                style = if (roleplay) HuntStyle.SECONDARY else HuntStyle.PRIMARY,
                icon = Icons.AutoMirrored.Filled.VolumeUp,
                minHeight = 40.dp,
                fontSize = 13,
                horizontalPadding = 8.dp,
            )
            HuntButton(
                stringResource(R.string.dialogue_mode_roleplay),
                onClick = { roleplay = true },
                modifier = Modifier.weight(1f),
                style = if (roleplay) HuntStyle.PRIMARY else HuntStyle.SECONDARY,
                icon = Icons.Filled.Mic,
                minHeight = 40.dp,
                fontSize = 13,
                horizontalPadding = 8.dp,
            )
        }
        if (roleplay) {
            Column(Modifier.weight(1f)) { RoleplayView(lesson, characters) }
        } else {
            HuntToggle(showTranslation, { showTranslation = it }, stringResource(R.string.dialogue_translation))
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                lesson.dialogue.lines.forEach { line ->
                    val mine = line.speaker == first
                    Column(
                        Modifier.fillMaxWidth(),
                        horizontalAlignment = if (mine) Alignment.Start else Alignment.End,
                    ) {
                        val tint = if (mine) t.accent else t.accent2
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                (characters[line.speaker]?.nameUz ?: line.speaker),
                                color = tint,
                                fontFamily = t.ui,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                            )
                            AudioButton(line.audio)
                        }
                        val bubble = t.shape(10.dp)
                        Column(
                            Modifier
                                .widthIn(max = 300.dp)
                                .background(tint.copy(alpha = 0.14f), bubble)
                                .padding(12.dp),
                        ) {
                            Text(line.ko, color = t.text, style = MaterialTheme.typography.bodyLarge)
                            if (showTranslation) Text(line.uz, color = t.muted, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
        HuntButton(
            stringResource(R.string.pager_next_stage),
            onClick = onFinished,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
    }
}

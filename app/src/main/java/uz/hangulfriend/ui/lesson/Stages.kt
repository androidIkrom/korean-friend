package uz.hangulfriend.ui.lesson

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.mikepenz.markdown.m3.Markdown
import uz.hangulfriend.R
import uz.hangulfriend.content.Character
import uz.hangulfriend.content.Grammar
import uz.hangulfriend.content.Lesson
import uz.hangulfriend.content.Word
import uz.hangulfriend.study.ExerciseItem
import uz.hangulfriend.ui.exercise.ExerciseView
import uz.hangulfriend.ui.exercise.MatchView
import uz.hangulfriend.ui.theme.CorrectGreen
import uz.hangulfriend.ui.theme.WrongRed

/** Pages through the stage; the last page's "next" moves to the following stage. */
@Composable
private fun Pager(pageCount: Int, onFinished: () -> Unit, page: @Composable (Int, () -> Unit) -> Unit) {
    var index by rememberSaveable { mutableIntStateOf(0) }
    val next: () -> Unit = {
        if (index + 1 < pageCount) index++ else onFinished()
    }
    Column(Modifier.fillMaxSize()) {
        Text(
            "${index + 1} / $pageCount",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            key(index) { page(index, next) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
            OutlinedButton(onClick = { index-- }, enabled = index > 0, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.pager_prev))
            }
            Button(onClick = next, modifier = Modifier.weight(1f)) {
                Text(stringResource(if (index + 1 < pageCount) R.string.pager_next else R.string.pager_next_stage))
            }
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
                MatchView(p.words, onResult = { vm.grade(item, it) }, onNext = next)
            }
        }
    }
}

@Composable
private fun WordCard(word: Word) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(word.ko, style = MaterialTheme.typography.displaySmall)
            Text(word.uz, style = MaterialTheme.typography.titleLarge)
            Text(word.pos, style = MaterialTheme.typography.labelMedium)
            HorizontalDivider()
            Text(word.exampleKo, style = MaterialTheme.typography.bodyLarge)
            Text(word.exampleUz, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun GrammarStage(lesson: Lesson, vm: LessonViewModel, onFinished: () -> Unit) {
    Pager(lesson.grammar.size, onFinished) { i, next ->
        val g = lesson.grammar[i]
        GrammarPage(g)
        val check = remember(g.id) { vm.grammarCheck(lesson, g.id) }
        if (check != null) {
            Spacer(Modifier.padding(8.dp))
            Text(stringResource(R.string.grammar_check), style = MaterialTheme.typography.titleMedium)
            ExerciseView(check, onResult = { vm.grade(check, it) }, onNext = next)
        }
    }
}

@Composable
private fun GrammarPage(g: Grammar) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(g.pattern, style = MaterialTheme.typography.headlineMedium)
        Text(g.meaningUz, style = MaterialTheme.typography.titleMedium)
        Card(Modifier.fillMaxWidth()) {
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
        Markdown(content = g.explanationMd)
        SectionTitle(R.string.grammar_examples)
        g.examples.forEach { e ->
            Column {
                Text(e.ko, style = MaterialTheme.typography.bodyLarge)
                Text(e.uz, style = MaterialTheme.typography.bodyMedium)
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
    Text(stringResource(res), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
}

@Composable
fun DialogueStage(lesson: Lesson, characters: Map<String, Character>, onFinished: () -> Unit) {
    var showTranslation by rememberSaveable { mutableStateOf(false) }
    val first = lesson.dialogue.lines.firstOrNull()?.speaker
    Column(Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.dialogue_translation), Modifier.weight(1f))
            Switch(checked = showTranslation, onCheckedChange = { showTranslation = it })
        }
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
                    Text(characters[line.speaker]?.nameUz ?: line.speaker, style = MaterialTheme.typography.labelMedium)
                    Column(
                        Modifier
                            .widthIn(max = 300.dp)
                            .background(
                                if (mine) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                                RoundedCornerShape(12.dp),
                            )
                            .padding(12.dp),
                    ) {
                        Text(line.ko, style = MaterialTheme.typography.bodyLarge)
                        if (showTranslation) Text(line.uz, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        Button(onClick = onFinished, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Text(stringResource(R.string.pager_next_stage))
        }
    }
}

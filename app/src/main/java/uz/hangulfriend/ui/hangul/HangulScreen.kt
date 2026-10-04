package uz.hangulfriend.ui.hangul

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import uz.hangulfriend.R
import uz.hangulfriend.content.ContentRepository
import uz.hangulfriend.content.Lesson
import uz.hangulfriend.content.Letter
import uz.hangulfriend.content.LetterKind
import uz.hangulfriend.data.CardOrigin
import uz.hangulfriend.data.StudyRepository
import uz.hangulfriend.study.LessonId
import uz.hangulfriend.ui.AudioButton
import uz.hangulfriend.ui.LocalAudioPlayer
import uz.hangulfriend.ui.exercise.BigLetter
import uz.hangulfriend.ui.kit.HuntButton
import uz.hangulfriend.ui.kit.HuntPanel
import uz.hangulfriend.ui.kit.glass
import uz.hangulfriend.ui.theme.LocalGameTokens

data class HangulState(val loading: Boolean = true, val lesson: Lesson? = null, val letters: List<Letter> = emptyList())

/** A Hangul lesson: its letters to explore, and practice. Opening it makes the letter cards active. */
class HangulViewModel(
    private val lessonId: String,
    private val content: ContentRepository,
    private val study: StudyRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(HangulState())
    val state: StateFlow<HangulState> = _state

    init {
        viewModelScope.launch {
            val lesson = content.lesson(lessonId)
            if (lesson != null) {
                val order = content.catalog().indexOfFirst { it.id == lessonId }.coerceAtLeast(0)
                study.ensureCards(lesson, order, CardOrigin.LESSON)
            }
            _state.value = HangulState(false, lesson, content.hangul()?.lettersOf(lessonId).orEmpty())
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HangulScreen(vm: HangulViewModel, onBack: () -> Unit, onPractice: (String) -> Unit) {
    val s by vm.state.collectAsStateWithLifecycle()
    val t = LocalGameTokens.current
    val player = LocalAudioPlayer.current
    var open by rememberSaveable { mutableStateOf<String?>(null) }
    val lesson = s.lesson
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = t.muted) }
            Column(Modifier.weight(1f)) {
                lesson?.let {
                    Text(stringResource(R.string.gate_code, LessonId.parse(it.id)?.code.orEmpty()), color = t.accent, fontFamily = t.ui, fontSize = 11.sp)
                }
                Text(lesson?.titleKo.orEmpty(), color = t.text, fontFamily = t.ui, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text(lesson?.titleUz.orEmpty(), color = t.muted, fontSize = 13.sp)
            }
        }
        when {
            s.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            lesson == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.coming_soon), color = t.text)
            }
            else -> {
                Text(
                    stringResource(R.string.hangul_intro),
                    color = t.muted,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(76.dp),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(s.letters, key = { it.id }) { letter ->
                        LetterTile(letter) {
                            letter.audio?.let { player?.play(it) }
                            open = letter.id
                        }
                    }
                }
                HuntButton(
                    stringResource(R.string.practice_start),
                    onClick = { onPractice(lesson.id) },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                )
            }
        }
    }
    s.letters.find { it.id == open }?.let { letter ->
        ModalBottomSheet(
            onDismissRequest = { open = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = t.panel.compositeOver(t.background),
        ) { LetterSheet(letter) }
    }
}

@Composable
private fun LetterTile(letter: Letter, onClick: () -> Unit) {
    val t = LocalGameTokens.current
    Column(
        Modifier
            .aspectRatio(1f)
            .glass(RoundedCornerShape(20.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(letter.jamo, color = t.text, fontSize = 32.sp, fontWeight = FontWeight.Medium)
        Text(letter.roman, color = t.accent, fontFamily = t.ui, fontSize = 12.sp)
    }
}

@Composable
private fun LetterSheet(letter: Letter) {
    val t = LocalGameTokens.current
    Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BigLetter(letter.jamo, final = letter.kind == LetterKind.FINAL)
            Column(Modifier.weight(1f)) {
                Text(letter.roman, color = t.accent, fontFamily = t.numbers, fontSize = 22.sp)
                Text(stringResource(R.string.hangul_letter_name, letter.nameKo, letter.say), color = t.muted, fontSize = 13.sp)
            }
            AudioButton(letter.audio)
        }
        Text(letter.tipUz, color = t.text, fontSize = 15.sp, lineHeight = 22.sp)
        HuntPanel(title = stringResource(R.string.hangul_example)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(letter.example.ko, color = t.text, fontSize = 26.sp)
                    Text("${letter.example.roman} · ${letter.example.uz}", color = t.muted, fontSize = 14.sp)
                }
                AudioButton(letter.example.audio)
            }
        }
    }
}

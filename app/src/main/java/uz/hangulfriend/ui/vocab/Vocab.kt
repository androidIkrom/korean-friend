package uz.hangulfriend.ui.vocab

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uz.hangulfriend.R
import uz.hangulfriend.content.ContentRepository
import uz.hangulfriend.content.Word
import uz.hangulfriend.data.AppDatabase
import uz.hangulfriend.data.USER_LESSON_ID
import uz.hangulfriend.data.UserWordRepository
import uz.hangulfriend.data.UserWordRepository.AddResult
import uz.hangulfriend.data.userWordId
import uz.hangulfriend.ui.AudioButton
import uz.hangulfriend.ui.kit.HuntButton
import uz.hangulfriend.ui.kit.HuntChip
import uz.hangulfriend.ui.kit.HuntStyle
import uz.hangulfriend.ui.kit.IconAction
import uz.hangulfriend.ui.kit.ScreenHeader
import uz.hangulfriend.ui.kit.Sfx
import uz.hangulfriend.ui.kit.SystemDialog
import uz.hangulfriend.ui.theme.GameBackground
import uz.hangulfriend.ui.theme.LocalGameTokens

/** A lesson filter chip: its id and its "unit-lesson" label. */
data class LessonChip(val id: String, val tag: String)

class VocabViewModel(
    private val content: ContentRepository,
    private val userWords: UserWordRepository,
    db: AppDatabase,
) : ViewModel() {
    private val book = MutableStateFlow<List<VocabEntry>>(emptyList())
    private val bookLoaded = CompletableDeferred<Unit>()

    private val _chips = MutableStateFlow<List<LessonChip>>(emptyList())
    val chips: StateFlow<List<LessonChip>> = _chips

    val query = MutableStateFlow("")
    val filter = MutableStateFlow<VocabFilter>(VocabFilter.All)

    private val own = userWords.observe().map { list ->
        list.map {
            val id = userWordId(it.id)
            VocabEntry(id, it.ko, it.uz, USER_LESSON_ID, "★", own = true, ownId = it.id, word = Word(id, it.ko, it.uz, "", it.note.orEmpty(), ""), note = it.note)
        }
    }

    private val statuses = db.cards().observeRecognize().map { cards -> cards.associate { it.itemId to statusOf(it) } }

    val total: StateFlow<Int> = combine(book, own) { b, o -> b.size + o.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val rows: StateFlow<List<Pair<VocabEntry, WordStatus>>> =
        combine(book, own, statuses, query, filter) { b, o, st, q, f ->
            filterVocab(o + b, q, f).map { it to (st[it.id] ?: WordStatus.NEW) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            val loaded = withContext(Dispatchers.IO) {
                content.catalog().filter { content.isAvailable(it.id) }.mapNotNull { e ->
                    content.lesson(e.id)?.let { lesson -> LessonChip(e.id, "${e.unit}-${e.lesson}") to lesson.words }
                }
            }
            _chips.value = loaded.map { it.first }
            book.value = loaded.flatMap { (chip, words) ->
                words.map { VocabEntry(it.id, it.ko, it.uz, chip.id, chip.tag, own = false, ownId = null, word = it, note = null) }
            }
            bookLoaded.complete(Unit)
        }
    }

    /** Waits for the book words so a book word cannot slip in as a duplicate while they are still loading. */
    suspend fun add(ko: String, uz: String, note: String): AddResult {
        bookLoaded.await()
        return userWords.add(ko, uz, note, book.value.map { it.ko }.toSet())
    }

    fun delete(id: Long) {
        viewModelScope.launch { userWords.delete(id) }
    }
}

@Composable
fun VocabScreen(vm: VocabViewModel, onBack: () -> Unit) {
    val t = LocalGameTokens.current
    val rows by vm.rows.collectAsStateWithLifecycle()
    val total by vm.total.collectAsStateWithLifecycle()
    val chips by vm.chips.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val filter by vm.filter.collectAsStateWithLifecycle()
    var opened by remember { mutableStateOf<VocabEntry?>(null) }
    var adding by rememberSaveable { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val addedText = stringResource(R.string.vocab_added)

    GameBackground {
        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ScreenHeader(stringResource(R.string.vocab_title), onBack) {
                Text(pluralStringResource(R.plurals.vocab_count, total, total), color = t.muted, fontSize = 13.sp, modifier = Modifier.padding(end = 4.dp))
            }
            OutlinedTextField(
                value = query,
                onValueChange = { vm.query.value = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text(stringResource(R.string.vocab_search)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { vm.query.value = "" }) { Icon(Icons.Filled.Clear, contentDescription = null) }
                    }
                },
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val all = listOf(
                    VocabFilter.All to R.string.vocab_all,
                    VocabFilter.Own to R.string.vocab_own,
                )
                items(all) { (f, label) -> Chip(stringResource(label), filter == f) { vm.filter.value = f } }
                items(chips) { c ->
                    val f = VocabFilter.Lesson(c.id)
                    Chip(c.tag, filter == f) { vm.filter.value = f }
                }
            }
            if (rows.isEmpty()) {
                Text(
                    stringResource(if (filter == VocabFilter.Own && query.isBlank()) R.string.vocab_own_empty else R.string.vocab_none),
                    color = t.muted,
                    modifier = Modifier.padding(top = 24.dp),
                )
            }
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 88.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                items(rows, key = { it.first.id }) { (entry, status) -> WordRow(entry, status) { opened = entry } }
            }
        }
        IconAction(
            Icons.Filled.Add,
            stringResource(R.string.vocab_add),
            onClick = { adding = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
            sfx = Sfx.OPEN,
        )
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = 88.dp))
    }

    opened?.let { entry ->
        val status = rows.firstOrNull { it.first.id == entry.id }?.second ?: WordStatus.NEW
        WordSheet(entry, status, onDismiss = { opened = null }, onDelete = { id ->
            vm.delete(id)
            opened = null
        })
    }
    if (adding) {
        AddWordDialog(
            onDismiss = { adding = false },
            onAdd = { ko, uz, note -> vm.add(ko, uz, note) },
            onAdded = {
                adding = false
                scope.launch { snackbar.showSnackbar(addedText) }
            },
        )
    }
}

@Composable
private fun Chip(label: String, selected: Boolean, onClick: () -> Unit) = HuntChip(label, selected, onClick)

@Composable
private fun statusColor(status: WordStatus): Color {
    val t = LocalGameTokens.current
    return when (status) {
        WordStatus.NEW -> t.muted.copy(alpha = 0.6f)
        WordStatus.LEARNING -> t.accent2
        WordStatus.LEARNED -> t.accent
    }
}

@Composable
private fun statusLabel(status: WordStatus): String = stringResource(
    when (status) {
        WordStatus.NEW -> R.string.vocab_status_new
        WordStatus.LEARNING -> R.string.vocab_status_learning
        WordStatus.LEARNED -> R.string.vocab_status_learned
    },
)

/** A small diamond in the word's status colour. */
@Composable
private fun StatusMark(status: WordStatus) {
    Box(Modifier.size(14.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.size(9.dp).rotate(45f).background(statusColor(status)))
    }
}

@Composable
private fun WordRow(entry: VocabEntry, status: WordStatus, onClick: () -> Unit) {
    val t = LocalGameTokens.current
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        StatusMark(status)
        Column(Modifier.weight(1f)) {
            Text(entry.ko, color = t.text, fontSize = 18.sp)
            Text(entry.uz, color = t.muted, fontSize = 13.sp)
        }
        Text(entry.lessonTag, color = if (entry.own) t.top else t.muted, fontSize = 12.sp)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WordSheet(entry: VocabEntry, status: WordStatus, onDismiss: () -> Unit, onDelete: (Long) -> Unit) {
    val t = LocalGameTokens.current
    var confirm by remember { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = t.panel.copy(alpha = 1f)) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(entry.ko, color = t.text, fontSize = 30.sp, modifier = Modifier.weight(1f))
                AudioButton(entry.word.audio)
            }
            Text(entry.uz, color = t.text, fontSize = 18.sp)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusMark(status)
                Text(statusLabel(status), color = t.muted, fontSize = 13.sp)
                Text(if (entry.own) stringResource(R.string.vocab_own) else entry.lessonTag, color = t.muted, fontSize = 13.sp)
            }
            if (!entry.own && entry.word.exampleKo.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(entry.word.exampleKo, color = t.text)
                        Text(entry.word.exampleUz, color = t.muted, fontSize = 13.sp)
                    }
                    AudioButton(entry.word.exampleAudio)
                }
            }
            entry.note?.let { Text(it, color = t.muted) }
            entry.ownId?.let { id ->
                HuntButton(
                    stringResource(R.string.vocab_delete),
                    onClick = { confirm = true },
                    style = HuntStyle.DANGER,
                    minHeight = 40.dp,
                    fontSize = 13,
                )
                if (confirm) {
                    SystemDialog(
                        title = stringResource(R.string.vocab_delete),
                        onDismiss = { confirm = false },
                        actions = {
                            HuntButton(stringResource(R.string.vocab_cancel), onClick = { confirm = false }, modifier = Modifier.weight(1f), style = HuntStyle.SECONDARY, fontSize = 13)
                            HuntButton(stringResource(R.string.vocab_delete), onClick = { onDelete(id) }, modifier = Modifier.weight(1f), style = HuntStyle.DANGER, fontSize = 13)
                        },
                    ) { Text(stringResource(R.string.vocab_delete_q), color = t.text) }
                }
            }
        }
    }
}

@Composable
private fun AddWordDialog(onDismiss: () -> Unit, onAdd: suspend (String, String, String) -> AddResult, onAdded: () -> Unit) {
    var ko by rememberSaveable { mutableStateOf("") }
    var uz by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<Int?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    SystemDialog(
        title = stringResource(R.string.vocab_add),
        onDismiss = onDismiss,
        actions = {
            HuntButton(stringResource(R.string.vocab_cancel), onClick = onDismiss, modifier = Modifier.weight(1f), style = HuntStyle.SECONDARY, fontSize = 13)
            HuntButton(
                stringResource(R.string.vocab_add_ok).uppercase(),
                modifier = Modifier.weight(1f),
                fontSize = 13,
                enabled = ko.isNotBlank() && uz.isNotBlank() && !busy,
                onClick = {
                    busy = true
                    scope.launch {
                        when (onAdd(ko, uz, note)) {
                            is AddResult.Added -> onAdded()
                            AddResult.Empty -> error = R.string.vocab_err_empty
                            AddResult.TooLong -> error = R.string.vocab_err_long
                            AddResult.Duplicate -> error = R.string.vocab_err_dup
                        }
                        busy = false
                    }
                },
            )
        },
    ) {
        OutlinedTextField(ko, { ko = it; error = null }, label = { Text(stringResource(R.string.vocab_ko)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(uz, { uz = it; error = null }, label = { Text(stringResource(R.string.vocab_uz)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(note, { note = it; error = null }, label = { Text(stringResource(R.string.vocab_note)) }, modifier = Modifier.fillMaxWidth())
        error?.let { Text(stringResource(it), color = LocalGameTokens.current.danger) }
    }
}

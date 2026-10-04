package uz.hangulfriend.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import uz.hangulfriend.R
import uz.hangulfriend.content.CatalogEntry
import uz.hangulfriend.content.ContentRepository
import uz.hangulfriend.data.HeroGender
import uz.hangulfriend.ui.kit.FloorBar
import uz.hangulfriend.ui.kit.GateRays
import uz.hangulfriend.ui.kit.HuntButton
import uz.hangulfriend.ui.kit.HuntPanel
import uz.hangulfriend.ui.kit.HuntStyle
import uz.hangulfriend.ui.kit.PanelTitle
import uz.hangulfriend.ui.kit.SelectRow
import uz.hangulfriend.ui.kit.Sfx
import uz.hangulfriend.ui.kit.breathing
import uz.hangulfriend.ui.settings.HeroRow
import uz.hangulfriend.ui.theme.LocalGameTokens
import uz.hangulfriend.ui.settings.LanguageRow
import uz.hangulfriend.study.OnboardingService
import uz.hangulfriend.ui.settings.NameField
import androidx.compose.material3.MaterialTheme

class OnboardingViewModel(content: ContentRepository, private val onboarding: OnboardingService) : ViewModel() {
    val catalog: List<CatalogEntry> = content.catalog()

    private val _selected = MutableStateFlow<String?>(null)
    val selected: StateFlow<String?> = _selected

    private val _hero = MutableStateFlow(HeroGender.BOY)
    val hero: StateFlow<HeroGender> = _hero

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy

    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name

    fun setName(name: String) {
        _name.value = name
    }

    fun pickHero(hero: HeroGender) {
        _hero.value = hero
    }

    fun select(id: String) {
        _selected.value = id
    }

    fun complete(onDone: () -> Unit) {
        val id = _selected.value ?: return
        _busy.value = true
        viewModelScope.launch {
            onboarding.complete(id, _hero.value, _name.value)
            _busy.value = false
            onDone()
        }
    }
}

/** Awakening (language), hero, then the lesson to start from. */
const val ONBOARDING_STEPS = 3

/** The step after [step], or null on the last one (finish). */
/** The first Hangul lesson, where a complete beginner starts. */
const val HANGUL_START = "b1_u01_l1"

fun nextStep(step: Int): Int? = if (step + 1 < ONBOARDING_STEPS) step + 1 else null

/** The flow can finish only on the last step, once a lesson is picked. */
fun canFinish(step: Int, selected: String?): Boolean = step == ONBOARDING_STEPS - 1 && selected != null

/** Lists the book's lessons grouped by unit and lets the learner pick one. Also used by settings. */
@Composable
fun LessonPicker(
    catalog: List<CatalogEntry>,
    selected: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier) { lessonPickerItems(catalog, selected, onSelect) }
}

/** The picker's rows, for a screen that already scrolls in its own LazyColumn. */
fun LazyListScope.lessonPickerItems(catalog: List<CatalogEntry>, selected: String?, onSelect: (String) -> Unit) {
    val books = catalog.map { it.book }.distinct()
    catalog.groupBy { it.book to it.unit }.forEach { (key, entries) ->
        val (book, unit) = key
        if (books.size > 1 && unit == entries.first().let { e -> catalog.first { it.book == e.book }.unit }) {
            item(key = "b$book") {
                Text(
                    stringResource(R.string.book_label, book),
                    color = LocalGameTokens.current.accent,
                    fontFamily = LocalGameTokens.current.ui,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(top = 18.dp),
                )
            }
        }
        item(key = "b${book}u$unit") {
            Box(Modifier.padding(top = 14.dp, bottom = 4.dp)) {
                PanelTitle(stringResource(R.string.unit_title, unit, entries.first().topicUz))
            }
        }
        items(entries, key = { it.id }) { e ->
            SelectRow(e.titleKo, e.titleUz, e.id == selected) { onSelect(e.id) }
        }
    }
}

@Composable
fun OnboardingScreen(vm: OnboardingViewModel, onDone: () -> Unit) {
    val selected by vm.selected.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val hero by vm.hero.collectAsStateWithLifecycle()
    val name by vm.name.collectAsStateWithLifecycle()
    var step by rememberSaveable { mutableIntStateOf(0) }
    BackHandler(enabled = step > 0) { step-- }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        FloorBar(step, ONBOARDING_STEPS)
        Box(Modifier.weight(1f)) {
            when (step) {
                0 -> AwakeningStep()
                1 -> Column(Modifier.verticalScroll(rememberScrollState())) {
                    HuntPanel {
                        Text(stringResource(R.string.name_title), style = MaterialTheme.typography.titleMedium)
                        NameField(name, vm::setName, Modifier.fillMaxWidth())
                        HeroRow(hero, vm::pickHero)
                    }
                }
                else -> Column {
                    Text(
                        stringResource(R.string.onboarding_question),
                        color = LocalGameTokens.current.text,
                        fontFamily = LocalGameTokens.current.ui,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                    )
                    // A shortcut for someone who cannot read Hangul yet: the first letter lesson.
                    vm.catalog.find { it.id == HANGUL_START }?.let { start ->
                        Box(Modifier.padding(vertical = 8.dp)) {
                            SelectRow(
                                stringResource(R.string.onboarding_beginner),
                                stringResource(R.string.onboarding_beginner_sub),
                                selected == start.id,
                            ) { vm.select(start.id) }
                        }
                    }
                    LessonPicker(vm.catalog, selected, vm::select, Modifier.weight(1f))
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (step > 0) {
                HuntButton(
                    stringResource(R.string.onboarding_back),
                    onClick = { step-- },
                    modifier = Modifier.weight(1f),
                    style = HuntStyle.SECONDARY,
                )
            }
            val next = nextStep(step)
            if (next != null) {
                HuntButton(
                    stringResource(R.string.onboarding_next),
                    onClick = { step = next },
                    modifier = Modifier.weight(1.6f),
                )
            } else {
                HuntButton(
                    stringResource(R.string.awaken_arise),
                    onClick = { vm.complete(onDone) },
                    modifier = Modifier.weight(1.6f),
                    style = HuntStyle.GOLD,
                    enabled = canFinish(step, selected) && !busy,
                    icon = Icons.Filled.Bolt,
                    sfx = Sfx.CLEAR,
                )
            }
        }
    }
}

/** Step 1: the System's call over turning rays, the app's purpose, and the language. */
@Composable
private fun AwakeningStep() {
    val t = LocalGameTokens.current
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.size(240.dp), contentAlignment = Alignment.Center) {
            GateRays(t.accent.copy(alpha = 0.35f), Modifier.fillMaxSize())
            Column(Modifier.breathing(0.03f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("SYSTEM", color = t.accent, fontFamily = t.ui, fontWeight = FontWeight.SemiBold, letterSpacing = 1.3.sp, fontSize = 11.sp)
                Text(
                    stringResource(R.string.awaken_title),
                    color = t.text,
                    fontFamily = t.ui,
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp,
                )
            }
        }
        HuntPanel(scan = true) {
            Text(stringResource(R.string.awaken_system), color = t.accent, fontFamily = t.ui, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Text(stringResource(R.string.onboarding_intro), color = t.text, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
        LanguageRow()
    }
}

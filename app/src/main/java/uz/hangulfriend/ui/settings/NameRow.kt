package uz.hangulfriend.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import uz.hangulfriend.R
import uz.hangulfriend.data.NAME_MAX
import uz.hangulfriend.data.cleanName
import uz.hangulfriend.ui.kit.IconAction

/** The player's name as a single-line field, capped at [NAME_MAX] characters. */
@Composable
fun NameField(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier, onDone: () -> Unit = {}) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it.take(NAME_MAX)) },
        modifier = modifier,
        singleLine = true,
        placeholder = { Text(stringResource(R.string.name_hint)) },
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
    )
}

/** Settings row: edit the name and save it with the check button (or the keyboard's Done). */
@Composable
fun NameRow(current: String, onSave: (String) -> Unit) {
    var draft by rememberSaveable(current) { mutableStateOf(current) }
    val changed = cleanName(draft) != current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.name_title), style = MaterialTheme.typography.titleMedium)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NameField(draft, { draft = it }, Modifier.weight(1f), onDone = { if (changed) onSave(draft) })
            IconAction(Icons.Filled.Check, stringResource(R.string.name_save), onClick = { onSave(draft) }, enabled = changed)
        }
    }
}

/** The player's cleaned name ("" when unset), provided once by the activity. */
val LocalPlayerName = staticCompositionLocalOf { "" }

package uz.hangulfriend.ui.exercise

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import uz.hangulfriend.R
import uz.hangulfriend.data.FlagReason
import uz.hangulfriend.study.ExerciseItem

/** Stores a "Xato bor" report; provided by the activity, absent in tests and previews (then no button shows). */
fun interface FlagReporter {
    suspend fun flag(item: ExerciseItem, reason: FlagReason, comment: String?)
}

val LocalFlagReporter = staticCompositionLocalOf<FlagReporter?> { null }

fun reasonLabel(reason: FlagReason): Int = when (reason) {
    FlagReason.TRANSLATION -> R.string.flag_reason_translation
    FlagReason.AUDIO -> R.string.flag_reason_audio
    FlagReason.ANSWER_REJECTED -> R.string.flag_reason_answer
    FlagReason.TYPO -> R.string.flag_reason_typo
    FlagReason.OTHER -> R.string.flag_reason_other
}

/** A small flag above the exercise; it opens a dialog to pick a reason and leave a comment. */
@Composable
fun FlagButton(item: ExerciseItem, modifier: Modifier = Modifier) {
    val reporter = LocalFlagReporter.current ?: return
    var open by rememberSaveable { mutableStateOf(false) }
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        IconButton(onClick = { open = true }) {
            Icon(
                Icons.Outlined.Flag,
                contentDescription = stringResource(R.string.flag_button),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    if (open) FlagDialog(item, reporter) { open = false }
}

@Composable
private fun FlagDialog(item: ExerciseItem, reporter: FlagReporter, onClose: () -> Unit) {
    var reason by rememberSaveable { mutableStateOf(FlagReason.TRANSLATION) }
    var comment by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val thanks = stringResource(R.string.flag_saved)
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(stringResource(R.string.flag_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Column(Modifier.selectableGroup()) {
                    FlagReason.entries.forEach { r ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .heightIn(min = 44.dp)
                                .selectable(selected = r == reason, role = Role.RadioButton, onClick = { reason = r }),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = r == reason, onClick = null)
                            Text(stringResource(reasonLabel(r)))
                        }
                    }
                }
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it.take(300) },
                    label = { Text(stringResource(R.string.flag_comment)) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = !busy,
                onClick = {
                    busy = true
                    scope.launch {
                        reporter.flag(item, reason, comment)
                        Toast.makeText(context, thanks, Toast.LENGTH_SHORT).show()
                        onClose()
                    }
                },
            ) { Text(stringResource(R.string.flag_save)) }
        },
        dismissButton = { TextButton(onClick = onClose) { Text(stringResource(R.string.vocab_cancel)) } },
    )
}

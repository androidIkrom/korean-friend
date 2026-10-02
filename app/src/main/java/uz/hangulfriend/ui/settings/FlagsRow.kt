package uz.hangulfriend.ui.settings

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import kotlinx.coroutines.launch
import uz.hangulfriend.BuildConfig
import uz.hangulfriend.R
import uz.hangulfriend.data.FlagRepository
import uz.hangulfriend.data.exportText
import uz.hangulfriend.ui.exercise.reasonLabel

/** "Xato belgilari": how many reports are stored, share them as text, or clear them. */
@Composable
fun FlagsRow(flags: FlagRepository) {
    val count by remember(flags) { flags.observeCount() }.collectAsState(initial = 0)
    var confirm by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val subject = stringResource(R.string.flags_subject, count)
    val chooser = stringResource(R.string.flags_send)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(R.string.flags_title, count), style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                enabled = count > 0,
                onClick = {
                    scope.launch {
                        val text = exportText(flags.all(), BuildConfig.VERSION_NAME, LocalDate.now()) { context.getString(reasonLabel(it)) }
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, subject)
                            putExtra(Intent.EXTRA_TEXT, text)
                        }
                        context.startActivity(Intent.createChooser(send, chooser))
                    }
                },
            ) { Text(chooser) }
            OutlinedButton(enabled = count > 0, onClick = { confirm = true }) { Text(stringResource(R.string.flags_clear)) }
        }
    }
    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            text = { Text(stringResource(R.string.flags_clear_q)) },
            confirmButton = {
                TextButton(onClick = {
                    confirm = false
                    scope.launch { flags.clear() }
                }) { Text(stringResource(R.string.flags_clear)) }
            },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text(stringResource(R.string.vocab_cancel)) } },
        )
    }
}

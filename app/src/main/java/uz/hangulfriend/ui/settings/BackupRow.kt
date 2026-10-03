package uz.hangulfriend.ui.settings

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uz.hangulfriend.R
import uz.hangulfriend.data.BackupCodec
import uz.hangulfriend.ui.kit.HuntButton
import uz.hangulfriend.ui.kit.HuntStyle
import uz.hangulfriend.data.BackupFile
import uz.hangulfriend.data.BackupResult
import uz.hangulfriend.data.BackupService

/** "Save to file" / "Restore from file" (spec §4). Restoring asks for confirmation because it replaces everything. */
@Composable
fun BackupRow(backup: BackupService) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pending by remember { mutableStateOf<BackupFile?>(null) }

    val save = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            scope.launch {
                val ok = runCatching { withContext(Dispatchers.IO) { write(context, uri, backup.export()) } }.isSuccess
                toast(context, if (ok) R.string.backup_saved else R.string.backup_failed)
            }
        }
    }
    val open = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                val text = runCatching { withContext(Dispatchers.IO) { read(context, uri) } }.getOrDefault("")
                when (val result = BackupCodec.decode(text)) {
                    is BackupResult.Ok -> pending = result.file
                    BackupResult.NotBackup -> toast(context, R.string.backup_not_backup)
                    BackupResult.TooNew -> toast(context, R.string.backup_too_new)
                }
            }
        }
    }

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        HuntButton(
            stringResource(R.string.backup_save),
            { save.launch("hangul-friend-${LocalDate.now()}.json") },
            Modifier.weight(1f),
            style = HuntStyle.SECONDARY,
            minHeight = 44.dp,
            fontSize = 13,
        )
        HuntButton(
            stringResource(R.string.backup_restore),
            { open.launch(arrayOf("application/json", "*/*")) },
            Modifier.weight(1f),
            style = HuntStyle.SECONDARY,
            minHeight = 44.dp,
            fontSize = 13,
        )
    }

    pending?.let { file ->
        AlertDialog(
            onDismissRequest = { pending = null },
            title = { Text(stringResource(R.string.backup_confirm_title)) },
            text = { Text(stringResource(R.string.backup_confirm_text)) },
            confirmButton = {
                TextButton(onClick = {
                    pending = null
                    scope.launch {
                        val ok = runCatching { backup.import(file) }.isSuccess
                        toast(context, if (ok) R.string.backup_restored else R.string.backup_failed)
                    }
                }) { Text(stringResource(R.string.backup_confirm_yes)) }
            },
            dismissButton = { TextButton(onClick = { pending = null }) { Text(stringResource(R.string.backup_confirm_no)) } },
        )
    }
}

private fun write(context: Context, uri: Uri, text: String) {
    context.contentResolver.openOutputStream(uri, "wt")!!.use { it.write(text.toByteArray()) }
}

private fun read(context: Context, uri: Uri): String =
    context.contentResolver.openInputStream(uri)!!.use { it.readBytes().decodeToString() }

private fun toast(context: Context, text: Int) = Toast.makeText(context, text, Toast.LENGTH_LONG).show()

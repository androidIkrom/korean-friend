package uz.hangulfriend.ui.settings

import android.Manifest
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings as AndroidSettings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import uz.hangulfriend.R
import uz.hangulfriend.data.Settings

/** Daily reminder switch and time; turning it on asks for the notification permission on Android 13+. */
@Composable
fun ReminderRow(settings: Settings, onChange: (enabled: Boolean, minutes: Int) -> Unit) {
    val context = LocalContext.current
    var denied by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        denied = !granted
        if (granted) onChange(true, settings.reminderMinutes)
    }
    fun enable() {
        val needs = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        if (needs) launcher.launch(Manifest.permission.POST_NOTIFICATIONS) else onChange(true, settings.reminderMinutes)
    }
    Column {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.settings_reminder), style = MaterialTheme.typography.titleMedium)
            Switch(checked = settings.reminderEnabled, onCheckedChange = { on -> if (on) enable() else onChange(false, settings.reminderMinutes) })
        }
        if (settings.reminderEnabled) {
            val h = settings.reminderMinutes / 60
            val m = settings.reminderMinutes % 60
            TextButton(onClick = {
                TimePickerDialog(context, { _, hh, mm -> onChange(true, hh * 60 + mm) }, h, m, true).show()
            }) { Text(stringResource(R.string.settings_reminder_time, "%02d:%02d".format(h, m))) }
        }
        if (denied) {
            Text(stringResource(R.string.settings_reminder_denied), color = MaterialTheme.colorScheme.error)
            TextButton(onClick = {
                context.startActivity(
                    Intent(AndroidSettings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(AndroidSettings.EXTRA_APP_PACKAGE, context.packageName),
                )
            }) { Text(stringResource(R.string.settings_reminder_open)) }
        }
    }
}

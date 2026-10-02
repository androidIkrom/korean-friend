package uz.hangulfriend.ui.settings

import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import uz.hangulfriend.MainActivity
import uz.hangulfriend.R
import uz.hangulfriend.i18n.AppLanguage
import uz.hangulfriend.i18n.LanguageStore
import uz.hangulfriend.ui.currentLanguage
import uz.hangulfriend.ui.theme.LocalGameTokens

/** Each language is named in itself, so it can be found whatever language the app is in now. */
private fun AppLanguage.ownName() = when (this) {
    AppLanguage.UZ -> "O'zbekcha"
    AppLanguage.EN -> "English"
}

/** Interface language picker (settings and onboarding); a new choice restarts the app in that language. */
@Composable
fun LanguageRow() {
    val t = LocalGameTokens.current
    val context = LocalContext.current
    val current = currentLanguage()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.settings_language), style = MaterialTheme.typography.titleMedium)
        Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AppLanguage.entries.forEach { lang ->
                val selected = lang == current
                val shape = RoundedCornerShape(t.panelCorner)
                Box(
                    Modifier
                        .weight(1f)
                        .selectable(selected = selected, role = Role.RadioButton, onClick = {
                            if (!selected) {
                                LanguageStore(context).set(lang)
                                context.findActivity()?.restartForLanguage()
                            }
                        })
                        .background(t.panel, shape)
                        .border(if (selected) 2.dp else 1.dp, if (selected) t.accent else t.panelBorder, shape)
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(lang.ownName(), color = if (selected) t.accent else t.text, fontFamily = t.display, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private tailrec fun Context.findActivity(): MainActivity? = when (this) {
    is MainActivity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

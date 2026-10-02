package uz.hangulfriend.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import uz.hangulfriend.R
import uz.hangulfriend.i18n.AppLanguage

/** The language the screen's resources are in, which is always the learner's chosen language. */
@Composable
fun currentLanguage(): AppLanguage = AppLanguage.fromCode(stringResource(R.string.lang_code))

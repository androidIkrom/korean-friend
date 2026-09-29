package uz.hangulfriend.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import uz.hangulfriend.R
import uz.hangulfriend.audio.AudioPlayer

val LocalAudioPlayer = staticCompositionLocalOf<AudioPlayer?> { null }

/** Disabled when the clip has no audio file yet (audio not generated) or no player is provided. */
@Composable
fun AudioButton(file: String?, modifier: Modifier = Modifier) {
    val player = LocalAudioPlayer.current
    IconButton(
        onClick = { if (file != null) player?.play(file) },
        enabled = file != null && player != null,
        modifier = modifier,
    ) {
        Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = stringResource(R.string.audio_play))
    }
}

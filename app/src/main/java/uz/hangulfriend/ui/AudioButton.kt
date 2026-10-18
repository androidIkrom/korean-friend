package uz.hangulfriend.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import uz.hangulfriend.R
import uz.hangulfriend.audio.AudioPlayer
import uz.hangulfriend.audio.Voice

val LocalAudioPlayer = staticCompositionLocalOf<AudioPlayer?> { null }

/** Says Korean text: a recorded clip when there is one, else TTS. Null (tests, previews) keeps the app mute. */
val LocalVoice = staticCompositionLocalOf<Voice?> { null }

/**
 * Whether a speaker for [text] (with its clip [file], if any) would sound. Subscribes to the TTS state, so
 * the caller recomposes once the phone's Korean voice is ready.
 */
@Composable
fun canSay(text: String?, file: String? = null): Boolean {
    val voice = LocalVoice.current ?: return file != null && LocalAudioPlayer.current != null
    val ttsReady by voice.ttsReady.collectAsState()
    return voice.canSay(text, file, ttsReady)
}

/**
 * Plays [file] or, when the item has no clip yet, says [text] (its bundled clip found by text, else Korean
 * TTS). Disabled when nothing can sound: no clip, no voice, or no Korean TTS on the phone.
 */
@Composable
fun AudioButton(file: String?, modifier: Modifier = Modifier, text: String? = null) {
    val voice = LocalVoice.current
    val player = LocalAudioPlayer.current
    val enabled = canSay(text, file)
    IconButton(
        onClick = {
            if (voice != null) voice.say(text, file) else if (file != null) player?.play(file)
        },
        enabled = enabled,
        modifier = modifier,
    ) {
        Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = stringResource(R.string.audio_play))
    }
}

/**
 * Says [text] (or plays [file]) once when the exercise appears, like Duolingo reading a new card aloud.
 * Waits for the Korean TTS when the text has no clip; survives rotation without repeating.
 */
@Composable
fun AutoSay(text: String?, file: String? = null) {
    val voice = LocalVoice.current
    val player = LocalAudioPlayer.current
    val ttsReady = if (voice != null) voice.ttsReady.collectAsState().value else false
    var said by rememberSaveable(text, file) { mutableStateOf(false) }
    LaunchedEffect(ttsReady) {
        if (said) return@LaunchedEffect
        val sounded = when {
            voice != null -> voice.say(text, file)
            file != null && player != null -> {
                player.play(file)
                true
            }
            else -> false
        }
        if (sounded) said = true
    }
}

package uz.hangulfriend.audio

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Plays bundled clips. */
interface ClipPlayer {
    fun play(file: String)

    fun stop()
}

/** Says Korean text aloud. [ready] turns true once a Korean voice is available. */
interface TextSpeaker {
    val ready: StateFlow<Boolean>

    fun speak(text: String)

    fun stop()

    fun release()
}

/**
 * Says Korean aloud, Duolingo-style: a recorded clip when the text has one ([audio] field or found by text in
 * [clips]), otherwise the phone's Korean TTS. One voice at a time: a new [say] silences whatever was playing.
 */
class Voice(
    private val player: ClipPlayer,
    private val tts: TextSpeaker?,
    private val clips: ClipIndex,
) {
    /** True once TTS can speak Korean; texts without a clip are mute until then. */
    val ttsReady: StateFlow<Boolean> = tts?.ready ?: MutableStateFlow(false)

    /**
     * Whether [say] would sound for [text] (with its recorded [audio], if any). [ttsReady] can be passed
     * from an observed state so a screen re-evaluates once the Korean voice arrives.
     */
    fun canSay(text: String?, audio: String? = null, ttsReady: Boolean = this.ttsReady.value): Boolean =
        source(text, audio, ttsReady) != null

    /** Plays [audio] or the clip of [text], else speaks [text]; false when nothing could sound. */
    fun say(text: String?, audio: String? = null): Boolean {
        when (val source = source(text, audio, ttsReady.value)) {
            is Source.Clip -> {
                tts?.stop()
                player.play(source.file)
            }
            is Source.Spoken -> {
                player.stop()
                tts?.speak(source.text)
            }
            null -> return false
        }
        return true
    }

    fun stop() {
        tts?.stop()
        player.stop()
    }

    fun release() {
        tts?.release()
    }

    private sealed interface Source {
        data class Clip(val file: String) : Source
        data class Spoken(val text: String) : Source
    }

    private fun source(text: String?, audio: String?, ttsReady: Boolean): Source? {
        if (audio != null) return Source.Clip(audio)
        val spoken = text?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        clips.find(spoken)?.let { return Source.Clip(it) }
        if (tts == null || !ttsReady || !isKorean(spoken)) return null
        return Source.Spoken(spoken)
    }

    companion object {
        /** True when [text] has any Hangul (a syllable or a jamo), so TTS has something Korean to say. */
        fun isKorean(text: String): Boolean = text.any { c ->
            c.code in 0xAC00..0xD7A3 || c.code in 0x3131..0x318E || c.code in 0x1100..0x11FF
        }
    }
}

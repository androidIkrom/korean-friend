package uz.hangulfriend.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Korean text-to-speech through the phone's TTS engine, for words and sentences that have no recorded
 * clip (exercise options, typed answers, the learner's own words). Stays silent, with [ready] false,
 * when the engine or its Korean voice is missing.
 */
class KoreanTts(context: Context) : TextSpeaker {
    private val _ready = MutableStateFlow(false)
    override val ready: StateFlow<Boolean> = _ready

    private var engine: TextToSpeech? = null
    private var counter = 0

    init {
        // The callback may run synchronously (no engine installed): then engine is still null and ready stays false.
        engine = TextToSpeech(context.applicationContext) { status -> onInit(status) }
    }

    private fun onInit(status: Int) {
        val e = engine ?: return
        if (status != TextToSpeech.SUCCESS) return
        val result = e.setLanguage(Locale.KOREAN)
        val supported = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
        if (supported) e.setSpeechRate(SPEECH_RATE)
        _ready.value = supported
    }

    override fun speak(text: String) {
        if (!_ready.value) return
        engine?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "say-${counter++}")
    }

    override fun stop() {
        engine?.stop()
    }

    override fun release() {
        _ready.value = false
        engine?.stop()
        engine?.shutdown()
        engine = null
    }

    private companion object {
        /** A little slower than the engine's default, so learners can follow each syllable. */
        const val SPEECH_RATE = 0.9f
    }
}

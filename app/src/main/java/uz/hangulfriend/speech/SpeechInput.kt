package uz.hangulfriend.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

sealed interface SpeechEvent {
    data class Result(val candidates: List<String>) : SpeechEvent

    data class Error(val reason: SpeechError) : SpeechEvent
}

enum class SpeechError { NO_MATCH, LANGUAGE_UNAVAILABLE, NO_PERMISSION, NETWORK, OTHER }

fun mapRecognizerError(code: Int): SpeechError = when (code) {
    SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> SpeechError.NO_MATCH
    SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED, SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> SpeechError.LANGUAGE_UNAVAILABLE
    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> SpeechError.NO_PERMISSION
    SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> SpeechError.NETWORK
    else -> SpeechError.OTHER
}

/** Korean speech recognition via the system recognizer. Call every method on the main thread. */
class SpeechInput(private val context: Context) {
    private var recognizer: SpeechRecognizer? = null

    fun isAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    fun listen(onEvent: (SpeechEvent) -> Unit) {
        cancel()
        val r = SpeechRecognizer.createSpeechRecognizer(context).also { recognizer = it }
        r.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                val list = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
                onEvent(if (list.isEmpty()) SpeechEvent.Error(SpeechError.NO_MATCH) else SpeechEvent.Result(list))
            }

            override fun onError(error: Int) = onEvent(SpeechEvent.Error(mapRecognizerError(error)))

            override fun onReadyForSpeech(params: Bundle?) = Unit
            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() = Unit
            override fun onPartialResults(partialResults: Bundle?) = Unit
            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        })
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR")
            .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
        r.startListening(intent)
    }

    fun cancel() {
        recognizer?.cancel()
        recognizer?.destroy()
        recognizer = null
    }

    fun destroy() = cancel()
}

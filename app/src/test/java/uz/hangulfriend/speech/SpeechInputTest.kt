package uz.hangulfriend.speech

import android.speech.SpeechRecognizer
import org.junit.Assert.assertEquals
import org.junit.Test

class SpeechInputTest {
    @Test fun mapRecognizerError_mapsKnownCodes() {
        assertEquals(SpeechError.NO_MATCH, mapRecognizerError(SpeechRecognizer.ERROR_NO_MATCH))
        assertEquals(SpeechError.NO_MATCH, mapRecognizerError(SpeechRecognizer.ERROR_SPEECH_TIMEOUT))
        assertEquals(SpeechError.LANGUAGE_UNAVAILABLE, mapRecognizerError(SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED))
        assertEquals(SpeechError.LANGUAGE_UNAVAILABLE, mapRecognizerError(SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE))
        assertEquals(SpeechError.NO_PERMISSION, mapRecognizerError(SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS))
        assertEquals(SpeechError.NETWORK, mapRecognizerError(SpeechRecognizer.ERROR_NETWORK))
        assertEquals(SpeechError.NETWORK, mapRecognizerError(SpeechRecognizer.ERROR_NETWORK_TIMEOUT))
        assertEquals(SpeechError.OTHER, mapRecognizerError(999))
    }
}

package uz.hangulfriend.audio

import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceTest {
    private class FakePlayer : ClipPlayer {
        val played = mutableListOf<String>()
        var stops = 0

        override fun play(file: String) {
            played += file
        }

        override fun stop() {
            stops++
        }
    }

    private class FakeTts(ready: Boolean) : TextSpeaker {
        override val ready = MutableStateFlow(ready)
        val spoken = mutableListOf<String>()
        var stops = 0
        var released = false

        override fun speak(text: String) {
            spoken += text
        }

        override fun stop() {
            stops++
        }

        override fun release() {
            released = true
        }
    }

    private val player = FakePlayer()
    private val tts = FakeTts(ready = true)
    private val index = ClipIndex.of(setOf(bundledAudioName("가족")))
    private val voice = Voice(player, tts, index)

    @Test fun prefersTheGivenClip() {
        assertTrue(voice.say("가족", "given.mp3"))
        assertEquals(listOf("given.mp3"), player.played)
        assertTrue(tts.spoken.isEmpty())
    }

    @Test fun findsTheClipByText() {
        assertTrue(voice.say("가족"))
        assertEquals(listOf(bundledAudioName("가족")), player.played)
        assertTrue(tts.spoken.isEmpty())
    }

    @Test fun speaksKoreanWithoutClip() {
        assertTrue(voice.say(" 먹어요 "))
        assertEquals(listOf("먹어요"), tts.spoken)
        assertTrue(player.played.isEmpty())
    }

    @Test fun oneSourceSilencesTheOther() {
        voice.say("가족")
        assertEquals("clip stops speech", 1, tts.stops)
        voice.say("먹어요")
        assertEquals("speech stops clip", 1, player.stops)
    }

    @Test fun silentUntilKoreanTtsIsReady() {
        val mute = Voice(player, FakeTts(ready = false), index)
        assertFalse(mute.canSay("먹어요"))
        assertFalse(mute.say("먹어요"))
        assertTrue("clips still play", mute.say("가족"))
        assertTrue("given clips still play", mute.canSay("먹어요", "x.mp3"))
    }

    @Test fun canSayTakesAnObservedTtsState() {
        val mute = Voice(player, FakeTts(ready = false), index)
        assertTrue("a screen that saw the voice arrive", mute.canSay("먹어요", ttsReady = true))
        assertFalse(mute.canSay("먹어요", ttsReady = false))
        assertFalse("but say() trusts the real state", mute.say("먹어요"))
    }

    @Test fun silentWithoutTts() {
        val mute = Voice(player, null, index)
        assertFalse(mute.ttsReady.value)
        assertFalse(mute.say("먹어요"))
        assertTrue(mute.say("가족"))
    }

    @Test fun neverSpeaksNonKoreanOrBlank() {
        assertFalse(voice.say("salom"))
        assertFalse(voice.say("   "))
        assertFalse(voice.say(null))
        assertFalse(voice.canSay("salom"))
        assertTrue(tts.spoken.isEmpty())
    }

    @Test fun stopAndReleaseReachBoth() {
        voice.stop()
        assertEquals(1, tts.stops)
        assertEquals(1, player.stops)
        voice.release()
        assertTrue(tts.released)
    }

    @Test fun isKorean() {
        assertTrue(Voice.isKorean("학교"))
        assertTrue(Voice.isKorean("ㄱ"))
        assertTrue(Voice.isKorean("A: 네, 맞아요."))
        assertFalse(Voice.isKorean("maktab"))
        assertFalse(Voice.isKorean("123 ___"))
        assertFalse(Voice.isKorean(""))
    }
}

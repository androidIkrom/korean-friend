package uz.hangulfriend.audio

import org.junit.Assert.assertEquals
import org.junit.Test

class AudioPlayerTest {
    @Test fun audioAssetUri_format() = assertEquals("asset:///audio/ab12.ogg", audioAssetUri("ab12.ogg"))
}

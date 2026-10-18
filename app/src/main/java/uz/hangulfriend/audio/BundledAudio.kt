package uz.hangulfriend.audio

import android.content.res.AssetManager
import java.security.MessageDigest

/** The voice `tools/audio_gen.py` records every single-voice clip with. */
const val BUNDLED_VOICE = "ko-KR-SunHiNeural"

/**
 * The file name `tools/audio_gen.py` gives the clip of [text]: the first 12 hex digits of
 * `sha1("<voice>|<text>")` plus `.mp3`. Lets any screen find the recorded clip of a word or sentence
 * from its text alone, without an `audio` field.
 */
fun bundledAudioName(text: String, voice: String = BUNDLED_VOICE): String {
    val digest = MessageDigest.getInstance("SHA-1").digest("$voice|$text".toByteArray(Charsets.UTF_8))
    return digest.joinToString("") { "%02x".format(it) }.take(12) + ".mp3"
}

/** Finds the bundled clip of a text, if one was recorded. */
fun interface ClipIndex {
    /** The clip file of [text], or null when none is bundled. */
    fun find(text: String): String?

    companion object {
        /** An index over [names], the files of the `audio` asset folder. */
        fun of(names: Set<String>): ClipIndex = ClipIndex { text -> bundledAudioName(text).takeIf { it in names } }
    }
}

/** The clips shipped in `assets/audio`, listed once on first use (call [warm] off the main thread). */
class BundledAudio(private val assets: AssetManager) : ClipIndex {
    private val names: Set<String> by lazy { assets.list("audio")?.toSet().orEmpty() }

    fun warm() {
        names
    }

    override fun find(text: String): String? = bundledAudioName(text).takeIf { it in names }
}

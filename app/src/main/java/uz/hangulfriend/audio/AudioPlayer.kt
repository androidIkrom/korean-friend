package uz.hangulfriend.audio

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer

fun audioAssetUri(file: String): String = "asset:///audio/$file"

/** Plays bundled lesson audio. One clip at a time; a new [play] stops the previous one. */
class AudioPlayer(private val context: Context) {
    private var player: ExoPlayer? = null

    fun play(file: String) {
        val p = player ?: ExoPlayer.Builder(context).build().also { player = it }
        p.stop()
        p.setMediaItem(MediaItem.fromUri(audioAssetUri(file)))
        p.prepare()
        p.play()
    }

    fun release() {
        player?.release()
        player = null
    }
}

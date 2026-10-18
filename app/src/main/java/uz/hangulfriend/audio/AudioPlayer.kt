package uz.hangulfriend.audio

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer

fun audioAssetUri(file: String): String = "asset:///audio/$file"

/** Plays bundled lesson audio. One clip at a time; a new [play] stops the previous one. */
class AudioPlayer(private val context: Context) : ClipPlayer {
    private var player: ExoPlayer? = null

    override fun play(file: String) {
        val p = player ?: ExoPlayer.Builder(context).build().also { player = it }
        p.stop()
        p.setMediaItem(MediaItem.fromUri(audioAssetUri(file)))
        p.prepare()
        p.play()
    }

    override fun stop() {
        player?.stop()
    }

    fun release() {
        player?.release()
        player = null
    }
}

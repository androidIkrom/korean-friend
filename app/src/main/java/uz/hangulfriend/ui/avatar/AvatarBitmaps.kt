package uz.hangulfriend.ui.avatar

import android.content.res.AssetManager
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** The newest [capacity] values by key; reading one keeps it. Meant for a handful of hero images, not a large cache. */
internal class LruMap<V : Any>(private val capacity: Int) {
    private val map = object : LinkedHashMap<String, V>(capacity, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, V>?): Boolean = size > capacity
    }

    @Synchronized
    operator fun get(key: String): V? = map[key]

    @Synchronized
    operator fun set(key: String, value: V) {
        map[key] = value
    }
}

/**
 * Decoded hero images by asset path. A hero shown again (Home after another tab, the rank-up dialog,
 * settings) is drawn from here at once instead of being decoded from the asset again.
 */
object AvatarBitmaps {
    // Each image is 480×576 (about 1.1 MB decoded); six cover the heroes a session shows.
    private val images = LruMap<ImageBitmap>(6)

    fun cached(slot: String): ImageBitmap? = images[slot]

    /** Decodes [slot] off the main thread and keeps it; null when the asset cannot be decoded. */
    suspend fun load(assets: AssetManager, slot: String): ImageBitmap? {
        images[slot]?.let { return it }
        val bitmap = withContext(Dispatchers.IO) {
            runCatching { assets.open(slot).use { s -> BitmapFactory.decodeStream(s)?.asImageBitmap() } }.getOrNull()
        }
        if (bitmap != null) images[slot] = bitmap
        return bitmap
    }
}

package uz.hangulfriend.content

import android.util.Log
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException

class ContentException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

/**
 * Read-only access to content assets. With [strict] (debug builds) a broken file throws
 * [ContentException]; otherwise it is reported as missing so the rest of the app keeps working.
 */
class ContentRepository(private val source: AssetSource, private val strict: Boolean) {
    private val lessonCache = mutableMapOf<String, Lesson?>()

    fun catalog(): List<CatalogEntry> = parse("book.json", Catalog.serializer())?.lessons.orEmpty()

    fun isAvailable(id: String): Boolean = "$id.json" in source.list("lessons")

    fun lesson(id: String): Lesson? = synchronized(lessonCache) {
        lessonCache.getOrPut(id) { parse("lessons/$id.json", Lesson.serializer()) }
    }

    fun characters(): List<Character> = parse("characters.json", Characters.serializer())?.characters.orEmpty()

    private fun <T> parse(path: String, serializer: KSerializer<T>): T? {
        val text = source.read(path) ?: return null
        return try {
            ContentJson.decodeFromString(serializer, text)
        } catch (e: SerializationException) {
            fail(path, e)
        } catch (e: IllegalArgumentException) {
            fail(path, e)
        }
    }

    private fun fail(path: String, e: Exception): Nothing? {
        if (strict) throw ContentException("Broken content file $path: ${e.message}", e)
        runCatching { Log.e("ContentRepository", "Broken content file $path", e) }
        return null
    }
}

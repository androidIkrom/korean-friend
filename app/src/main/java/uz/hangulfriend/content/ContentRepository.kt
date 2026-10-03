package uz.hangulfriend.content

import android.util.Log
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import uz.hangulfriend.i18n.AppLanguage

class ContentException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

/**
 * Read-only access to content assets. With [strict] (debug builds) a broken file throws
 * [ContentException]; otherwise it is reported as missing so the rest of the app keeps working.
 * Everything comes back localized to [language] (see Localize.kt).
 */
class ContentRepository(
    private val source: AssetSource,
    private val strict: Boolean,
    private val language: () -> AppLanguage = { AppLanguage.UZ },
) {
    private val lessonCache = mutableMapOf<String, Lesson?>()

    fun catalog(): List<CatalogEntry> {
        val lang = language()
        return parse("book.json", Catalog.serializer())?.lessons.orEmpty().map { it.localized(lang) }
    }

    fun isAvailable(id: String): Boolean = "$id.json" in source.list("lessons")

    fun lesson(id: String): Lesson? {
        val lang = language()
        return synchronized(lessonCache) {
            lessonCache.getOrPut("${lang.code}/$id") { parse("lessons/$id.json", Lesson.serializer())?.localized(lang) }
        }
    }

    fun finalTest(): FinalTest? = parse("final_test.json", FinalTest.serializer())?.localized(language())

    fun characters(): List<Character> {
        val lang = language()
        return parse("characters.json", Characters.serializer())?.characters.orEmpty().map { it.localized(lang) }
    }

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

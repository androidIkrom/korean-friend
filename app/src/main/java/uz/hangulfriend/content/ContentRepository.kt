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

    /** Every book's lessons in series order; a book without a catalog file yet has none. */
    fun catalog(): List<CatalogEntry> {
        val lang = language()
        return BOOK_FILES.flatMap { (book, file) ->
            parse(file, Catalog.serializer())?.lessons.orEmpty().map { it.copy(book = book).localized(lang) }
        }
    }

    fun isAvailable(id: String): Boolean = "$id.json" in source.list("lessons")

    fun lesson(id: String): Lesson? {
        val lang = language()
        return synchronized(lessonCache) {
            lessonCache.getOrPut("${lang.code}/$id") { parse("lessons/$id.json", Lesson.serializer())?.localized(lang) }
        }
    }

    /** The final test of [book]; null while that book has none. */
    fun finalTest(book: Int = 2): FinalTest? {
        val file = if (book == 2) "final_test.json" else "final_test_b$book.json"
        return parse(file, FinalTest.serializer())?.localized(language())
    }

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

/** Catalog file of each book; book 2 keeps the original name. */
private val BOOK_FILES = listOf(1 to "book1.json", 2 to "book.json", 3 to "book3.json")

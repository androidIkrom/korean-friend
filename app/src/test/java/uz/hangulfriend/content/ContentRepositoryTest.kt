package uz.hangulfriend.content

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import uz.hangulfriend.Fixtures
import uz.hangulfriend.i18n.AppLanguage

class ContentRepositoryTest {
    @get:Rule val tmp = TemporaryFolder()

    private fun repo(strict: Boolean) = ContentRepository(DirAssetSource(tmp.root), strict)

    private fun write(path: String, text: String) =
        File(tmp.root, path).apply { parentFile.mkdirs() }.writeText(text)

    private fun writeLesson(text: String = ContentJson.encodeToString(Lesson.serializer(), Fixtures.validLesson())) =
        write("lessons/u02_l1.json", text)

    private fun entry(id: String, unit: Int) =
        """{"id":"$id","unit":$unit,"lesson":1,"title_ko":"가","title_uz":"a","topic_uz":"t"}"""

    @Test fun catalog_ordersBooksAndStampsThem() {
        write("book.json", """{"lessons":[${entry("u01_l1", 1)}]}""")
        write("book1.json", """{"lessons":[${entry("b1_u02_l1", 2)}]}""")
        write("book3.json", """{"lessons":[${entry("b3_u01_l1", 1)}]}""")
        val c = repo(strict = true).catalog()
        assertEquals(listOf("b1_u02_l1", "u01_l1", "b3_u01_l1"), c.map { it.id })
        assertEquals(listOf(1, 2, 3), c.map { it.book })
    }

    @Test fun catalog_withoutOtherBooksIsBookTwo() {
        write("book.json", """{"lessons":[${entry("u01_l1", 1)}]}""")
        assertEquals(listOf(2), repo(strict = true).catalog().map { it.book })
    }

    @Test fun finalTest_perBook() {
        assertNull(repo(strict = true).finalTest(1))
    }

    @Test fun lesson_roundTripsAndIsAvailable() {
        writeLesson()
        val r = repo(strict = true)
        assertTrue(r.isAvailable("u02_l1"))
        assertEquals(Fixtures.validLesson(), r.lesson("u02_l1"))
    }

    @Test fun lesson_cachesPerLanguage() {
        writeLesson(ContentJson.encodeToString(Lesson.serializer(), Fixtures.validLesson().copy(titleEn = "Try it on")))
        var lang = AppLanguage.UZ
        val r = ContentRepository(DirAssetSource(tmp.root), strict = true) { lang }
        assertEquals("Kiyib ko'ring", r.lesson("u02_l1")?.titleUz)
        lang = AppLanguage.EN
        assertEquals("Try it on", r.lesson("u02_l1")?.titleUz)
        lang = AppLanguage.UZ
        assertEquals("Kiyib ko'ring", r.lesson("u02_l1")?.titleUz)
    }

    @Test fun lesson_usesSnakeCaseKeys() {
        writeLesson()
        val text = File(tmp.root, "lessons/u02_l1.json").readText()
        assertTrue(text.contains("\"title_ko\""))
        assertTrue(text.contains("\"fill_blank\""))
    }

    @Test fun lesson_missingFile_returnsNull() {
        assertFalse(repo(strict = true).isAvailable("u09_l2"))
        assertNull(repo(strict = true).lesson("u09_l2"))
    }

    @Test(expected = ContentException::class)
    fun lesson_malformed_strictThrows() {
        writeLesson("{ not json")
        repo(strict = true).lesson("u02_l1")
    }

    @Test fun lesson_malformed_lenientNull() {
        writeLesson("{ not json")
        assertNull(repo(strict = false).lesson("u02_l1"))
    }

    @Test(expected = ContentException::class)
    fun lesson_unknownKey_strictThrows() {
        val valid = ContentJson.encodeToString(Lesson.serializer(), Fixtures.validLesson())
        writeLesson(valid.replaceFirst("{", "{\"titel_uz\": \"x\","))
        repo(strict = true).lesson("u02_l1")
    }

    @Test fun catalog_keepsOrder() {
        write(
            "book.json",
            """{"lessons":[
              {"id":"u01_l1","unit":1,"lesson":1,"title_ko":"가","title_uz":"a","topic_uz":"Oila"},
              {"id":"u01_l2","unit":1,"lesson":2,"title_ko":"나","title_uz":"b","topic_uz":"Oila"},
              {"id":"u02_l1","unit":2,"lesson":1,"title_ko":"다","title_uz":"c","topic_uz":"Xarid"}]}""",
        )
        assertEquals(listOf("u01_l1", "u01_l2", "u02_l1"), repo(strict = true).catalog().map { it.id })
    }

    @Test fun characters_load() {
        write("characters.json", """{"characters":[{"id":"aziz","name_uz":"Aziz","name_ko":"아지즈","voice":"male"}]}""")
        assertEquals("아지즈", repo(strict = true).characters().single().nameKo)
    }

    /** Counts file reads and folder listings, so a test can show that a repeated call never touches the files again. */
    private class CountingSource(private val inner: AssetSource) : AssetSource {
        var reads = 0
        var lists = 0

        override fun read(path: String): String? {
            reads++
            return inner.read(path)
        }

        override fun list(dir: String): List<String> {
            lists++
            return inner.list(dir)
        }
    }

    private fun writeCharacters() =
        write("characters.json", """{"characters":[{"id":"aziz","name_uz":"Aziz","name_ko":"아지즈","voice":"male"}]}""")

    @Test fun catalog_readOncePerLanguage() {
        write("book.json", """{"lessons":[${entry("u01_l1", 1)}]}""")
        val src = CountingSource(DirAssetSource(tmp.root))
        val r = ContentRepository(src, strict = true)
        val first = r.catalog()
        val reads = src.reads
        assertEquals(first, r.catalog())
        assertEquals(reads, src.reads)
    }

    @Test fun characters_readOnce() {
        writeCharacters()
        val src = CountingSource(DirAssetSource(tmp.root))
        val r = ContentRepository(src, strict = true)
        r.characters()
        val reads = src.reads
        r.characters()
        assertEquals(reads, src.reads)
    }

    @Test fun isAvailable_listsTheLessonFolderOnce() {
        writeLesson()
        val src = CountingSource(DirAssetSource(tmp.root))
        val r = ContentRepository(src, strict = true)
        assertTrue(r.isAvailable("u02_l1"))
        assertFalse(r.isAvailable("u09_l2"))
        assertTrue(r.isAvailable("u02_l1"))
        assertEquals(1, src.lists)
    }

    @Test fun lesson_missingFileIsNotReadAgain() {
        val src = CountingSource(DirAssetSource(tmp.root))
        val r = ContentRepository(src, strict = true)
        assertNull(r.lesson("u09_l2"))
        val reads = src.reads
        assertNull(r.lesson("u09_l2"))
        assertEquals(reads, src.reads)
    }

    @Test fun preload_fillsEveryCache() {
        write("book.json", """{"lessons":[${entry("u02_l1", 2)}]}""")
        writeLesson()
        writeCharacters()
        val src = CountingSource(DirAssetSource(tmp.root))
        val r = ContentRepository(src, strict = true)
        r.preload()
        val reads = src.reads
        val lists = src.lists
        assertEquals(listOf("u02_l1"), r.catalog().map { it.id })
        assertEquals(Fixtures.validLesson(), r.lesson("u02_l1"))
        assertEquals("아지즈", r.characters().single().nameKo)
        assertTrue(r.isAvailable("u02_l1"))
        assertNull(r.hangul())
        assertEquals(reads, src.reads)
        assertEquals(lists, src.lists)
    }
}

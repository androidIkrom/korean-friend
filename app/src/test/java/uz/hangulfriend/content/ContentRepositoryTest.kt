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
}

package uz.hangulfriend.i18n

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element

/** values-en must say everything values says, with the same placeholders (stage 8). */
class StringsParityTest {
    private class Res(val strings: Map<String, String>, val plurals: Map<String, Map<String, String>>)

    private fun load(dir: String): Res {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(File("src/main/res/$dir/strings.xml"))
        val strings = mutableMapOf<String, String>()
        val plurals = mutableMapOf<String, Map<String, String>>()
        val nodes = doc.documentElement.childNodes
        for (i in 0 until nodes.length) {
            val e = nodes.item(i) as? Element ?: continue
            when (e.tagName) {
                "string" -> strings[e.getAttribute("name")] = e.textContent
                "plurals" -> {
                    val items = e.getElementsByTagName("item")
                    plurals[e.getAttribute("name")] = (0 until items.length).associate {
                        val item = items.item(it) as Element
                        item.getAttribute("quantity") to item.textContent
                    }
                }
            }
        }
        return Res(strings, plurals)
    }

    private val uz = load("values")
    private val en = load("values-en")

    private fun args(s: String) = Regex("%\\d+\\$[sd]").findAll(s).map { it.value }.sorted().toList()

    @Test fun sameKeys() {
        assertEquals(uz.strings.keys.sorted(), en.strings.keys.sorted())
        assertEquals(uz.plurals.keys.sorted(), en.plurals.keys.sorted())
    }

    @Test fun sameFormatArgs() {
        for ((key, value) in uz.strings) assertEquals(key, args(value), args(en.strings[key].orEmpty()))
        for ((key, items) in uz.plurals) {
            val expected = args(items.getValue("other"))
            en.plurals[key].orEmpty().forEach { (q, text) -> assertEquals("$key/$q", expected, args(text)) }
        }
    }

    @Test fun englishPluralsHaveOne() {
        for ((key, items) in en.plurals) assertTrue(key, items.keys.containsAll(listOf("one", "other")))
    }

    @Test fun noUzbekLeftInEnglish() {
        val uzbek = Regex("[a-z](['ʻ’]|\\\\')[a-z]", RegexOption.IGNORE_CASE)
        val allowed = Regex("(n't|'s|'re|'ve|'ll|'d)\\b")
        val texts = en.strings + en.plurals.flatMap { (k, v) -> v.map { (q, t) -> "$k/$q" to t } }
        for ((key, text) in texts) {
            val cleaned = text.replace("\\'", "'").replace(allowed, "")
            assertTrue("$key: $text", !uzbek.containsMatchIn(cleaned) && !Regex("[gGoO]'").containsMatchIn(cleaned))
        }
    }
}

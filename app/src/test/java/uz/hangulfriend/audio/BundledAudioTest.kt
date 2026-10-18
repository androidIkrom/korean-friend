package uz.hangulfriend.audio

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import uz.hangulfriend.content.ContentRepository
import uz.hangulfriend.content.DirAssetSource
import uz.hangulfriend.content.isHangulLesson

class BundledAudioTest {
    /** `tools/audio_gen.py`: sha1("ko-KR-SunHiNeural|가족")[:12] + ".mp3", the clip u01_l1 lists for 가족. */
    @Test fun nameMatchesGenerator() = assertEquals("506af9637fb5.mp3", bundledAudioName("가족"))

    @Test fun nameDependsOnVoice() {
        assertEquals("506af9637fb5.mp3", bundledAudioName("가족", BUNDLED_VOICE))
        assertTrue(bundledAudioName("가족", "ko-KR-InJoonNeural") != bundledAudioName("가족"))
    }

    @Test fun indexFindsOnlyBundledClips() {
        val index = ClipIndex.of(setOf("506af9637fb5.mp3"))
        assertEquals("506af9637fb5.mp3", index.find("가족"))
        assertNull(index.find("없는 말"))
    }

    /** Every recorded word and example clip in the shipped lessons is found again from its text alone. */
    @Test fun everyWordClipIsFoundByText() {
        val repo = ContentRepository(DirAssetSource(File("src/main/assets")), strict = true)
        val lessons = repo.catalog().filter { repo.isAvailable(it.id) && !isHangulLesson(it.id) }.map { repo.lesson(it.id)!! }
        assertTrue(lessons.isNotEmpty())
        val names = File("src/main/assets/audio").list()!!.toSet()
        val index = ClipIndex.of(names)
        for (lesson in lessons) {
            for (w in lesson.words) {
                w.audio?.let { assertEquals("${lesson.id}/${w.id}", it, index.find(w.ko)) }
                w.exampleAudio?.let { assertEquals("${lesson.id}/${w.id} example", it, index.find(w.exampleKo)) }
            }
            for (ex in lesson.grammar.flatMap { it.examples }) {
                ex.audio?.let { assertEquals("${lesson.id}/${ex.ko}", it, index.find(ex.ko)) }
            }
        }
    }
}

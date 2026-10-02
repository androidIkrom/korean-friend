package uz.hangulfriend.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import uz.hangulfriend.Fixtures
import uz.hangulfriend.MutableClock
import uz.hangulfriend.content.ExerciseType
import uz.hangulfriend.study.ExerciseItem

class FlagModelTest {
    private val w = Fixtures.word("u02_l1_w001", "치마", "yubka")
    private val w2 = Fixtures.word("u02_l1_w002", "바지", "shim")
    private val ex = Fixtures.exercise("u02_l1_e07", ExerciseType.FILL_BLANK, listOf("u02_l1_g1"), listOf("입어 보세요")) {
        copy(sentence = "이 치마 ___ .")
    }

    @Test fun refAndSnapshotForEveryType() {
        val items = listOf(
            ExerciseItem.Flashcard(w), ExerciseItem.WordTyping(w), ExerciseItem.ListenChoose(w, listOf(w, w2)),
            ExerciseItem.Dictation(w), ExerciseItem.Speak("치마예요", "Bu yubka", null), ExerciseItem.Match(listOf(w, w2)),
            ExerciseItem.Authored(ex, emptyList()),
        )
        items.forEach {
            assertTrue("$it ref", flagRef(it).isNotBlank())
            assertTrue("$it snapshot", flagSnapshot(it).isNotBlank())
        }
        assertEquals("u02_l1_w001", flagRef(ExerciseItem.Flashcard(w)))
        assertEquals("u02_l1_w001,u02_l1_w002", flagRef(ExerciseItem.Match(listOf(w, w2))))
        assertEquals("u02_l1_e07", flagRef(ExerciseItem.Authored(ex, emptyList())))
        assertEquals("치마 — yubka", flagSnapshot(ExerciseItem.Flashcard(w)))
        val authored = flagSnapshot(ExerciseItem.Authored(ex, emptyList()))
        assertTrue(authored.contains("Mashq") && authored.contains("이 치마 ___ .") && authored.contains("입어 보세요"))
    }

    @Test fun lessonOfRefPrefix() {
        assertEquals("u02_l1", lessonOfRef("u02_l1_e07"))
        assertEquals("u09_l2", lessonOfRef("u09_l2_w001,u09_l2_w002"))
        assertEquals("user", lessonOfRef("user_w3"))
        assertEquals("", lessonOfRef("xyz"))
        assertEquals("final", lessonOfRef("final_l01"))
    }

    private fun flag(id: Long, ref: String, reason: FlagReason, comment: String?) = ContentFlagEntity(
        id = id, ref = ref, lessonId = lessonOfRef(ref), type = "flashcard", snapshot = "s$id", reason = reason.name,
        comment = comment, createdMs = id,
    )

    private val labels: (FlagReason) -> String = { it.name.lowercase() }

    @Test fun exportGroupsAndOrders() {
        val text = exportText(
            listOf(
                flag(1, "user_w1", FlagReason.OTHER, "x"),
                flag(2, "u09_l2_e01", FlagReason.TYPO, "a"),
                flag(3, "u02_l1_w001", FlagReason.TRANSLATION, "b"),
                flag(4, "zzz", FlagReason.AUDIO, "c"),
                flag(5, "u02_l1_e03", FlagReason.AUDIO, "d"),
            ),
            appVersion = "1.0",
            today = LocalDate.of(2026, 10, 2),
            reasonLabel = labels,
        )
        assertTrue(text.contains("Sana: 2026-10-02 · Ilova: 1.0 · Jami: 5"))
        val order = listOf("[u02_l1]", "[u09_l2]", "[user]", "[?]").map { text.indexOf(it) }
        assertTrue(order.toString(), order.all { it >= 0 } && order == order.sorted())
        assertTrue(text.contains("1) u02_l1_w001 · flashcard · translation"))
        assertTrue(text.contains("2) u02_l1_e03 · flashcard · audio"))
        assertTrue(text.contains("1) u09_l2_e01"))
    }

    @Test fun exportWithoutComment() {
        val text = exportText(listOf(flag(1, "u02_l1_w001", FlagReason.TYPO, null)), "1.0", LocalDate.of(2026, 10, 2), labels)
        assertFalse(text.contains("Izoh:"))
        assertTrue(text.contains("Savol: s1"))
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class FlagRepositoryTest {
    private val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    private val repo = FlagRepository(db, MutableClock())
    private val item = ExerciseItem.Flashcard(Fixtures.word("u02_l1_w001", "치마", "yubka"))

    @After fun close() = db.close()

    @Test fun flagStores() = runTest {
        repo.flag(item, FlagReason.TRANSLATION, "noto'g'ri")
        val f = repo.all().single()
        assertEquals("u02_l1_w001", f.ref)
        assertEquals("u02_l1", f.lessonId)
        assertEquals("flashcard", f.type)
        assertEquals("TRANSLATION", f.reason)
        assertEquals(1, repo.observeCount().first())
    }

    @Test fun sameRefAndReasonUpdates() = runTest {
        repo.flag(item, FlagReason.TRANSLATION, "a")
        repo.flag(item, FlagReason.TRANSLATION, "b")
        assertEquals(listOf("b"), repo.all().map { it.comment })
    }

    @Test fun otherReasonAddsRow() = runTest {
        repo.flag(item, FlagReason.TRANSLATION, null)
        repo.flag(item, FlagReason.AUDIO, null)
        assertEquals(2, repo.all().size)
    }

    @Test fun clearEmpties() = runTest {
        repo.flag(item, FlagReason.TYPO, null)
        repo.clear()
        assertTrue(repo.all().isEmpty())
    }

    @Test fun commentTrimmedAndCapped() = runTest {
        repo.flag(item, FlagReason.OTHER, "   ")
        assertNull(repo.all().single().comment)
        repo.flag(item, FlagReason.TYPO, "  " + "x".repeat(400))
        assertEquals(300, repo.all().first { it.reason == "TYPO" }.comment?.length)
    }
}

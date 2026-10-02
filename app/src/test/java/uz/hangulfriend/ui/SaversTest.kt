package uz.hangulfriend.ui

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import uz.hangulfriend.ui.exercise.FeedbackInfo
import uz.hangulfriend.ui.exercise.FeedbackInfoSaver
import uz.hangulfriend.ui.exercise.IntListSaver
import uz.hangulfriend.ui.exercise.StringListSaver

class SaversTest {
    private val scope = SaverScope { true }

    private fun <T : Any?> roundTrip(saver: Saver<T, Any>, value: T): T? {
        val saved = with(saver) { scope.save(value) } ?: return null
        return saver.restore(saved)
    }

    @Test fun feedbackInfoRoundTrip() {
        val info = FeedbackInfo(false, "Xato", "가요", "Sabab", listOf("가요", "가세요"), askAi = "Nega?")
        assertEquals(info, roundTrip(FeedbackInfoSaver, info))
        val bare = FeedbackInfo(true, "To'g'ri", null, null)
        assertEquals(bare, roundTrip(FeedbackInfoSaver, bare))
    }

    @Test fun nullFeedbackStaysNull() = assertNull(roundTrip(FeedbackInfoSaver, null))

    @Test fun intListRoundTrip() {
        val list = mutableStateListOf(3, 1, 2)
        assertEquals(listOf(3, 1, 2), roundTrip(IntListSaver, list)?.toList())
    }

    @Test fun stringListRoundTrip() {
        val list = mutableStateListOf("a", "b")
        assertEquals(listOf("a", "b"), roundTrip(StringListSaver, list)?.toList())
    }
}

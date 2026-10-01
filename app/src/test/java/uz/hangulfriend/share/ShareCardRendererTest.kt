package uz.hangulfriend.share

import java.io.File
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import uz.hangulfriend.data.LessonStatus

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ShareCardRendererTest {
    private val labels = ShareLabels("kun ketma-ket", "XP", "so'z", "Darslar", "Hozir", "Tugatilgan", "Jarayonda", "O'tilgan", "Boshlanmagan", "footer")

    private fun snapshot(statuses: List<LessonStatus>) = ShareSnapshot(
        level = 3, totalXp = 420, streak = 12, learnedWords = 87, completedLessons = 2,
        statuses = statuses, currentLessonTitle = "2-1 한번 입어 보세요", date = LocalDate.of(2026, 10, 1),
    )

    @Test fun rendersCardSize() {
        val bitmap = ShareCardRenderer.render(snapshot(List(18) { LessonStatus.NOT_STARTED }), labels)
        assertEquals(ShareCardRenderer.WIDTH, bitmap.width)
        assertEquals(ShareCardRenderer.HEIGHT, bitmap.height)
    }

    /** The first grid cell (lesson 1-1) is painted in its status colour; also writes a preview PNG under build/. */
    @Test fun firstLessonCellHasStatusColour() {
        val statuses = List(18) { i -> if (i < 2) LessonStatus.COMPLETED else if (i == 2) LessonStatus.IN_PROGRESS else LessonStatus.NOT_STARTED }
        val bitmap = ShareCardRenderer.render(snapshot(statuses), labels)
        File("build/share-card-preview.png").outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        assertEquals(ShareCardRenderer.color(LessonStatus.COMPLETED), bitmap.getPixel(100, 700))
    }
}

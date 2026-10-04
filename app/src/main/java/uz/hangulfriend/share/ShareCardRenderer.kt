package uz.hangulfriend.share

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import uz.hangulfriend.data.LessonStatus

/** Localized copy for the card; built from string resources by the caller. */
data class ShareLabels(
    val streakDays: String,
    val xp: String,
    val words: String,
    val lessons: String,
    val now: String,
    val completed: String,
    val inProgress: String,
    val passed: String,
    val notStarted: String,
    val footer: String,
)

/** Draws the 1080×1350 progress card (spec §5) with plain Canvas calls, so it needs no view on screen. */
object ShareCardRenderer {
    const val WIDTH = 1080
    const val HEIGHT = 1350

    private const val MARGIN = 60f
    private const val WHITE = 0xFFFFFFFF.toInt()
    private const val MUTED = 0xB3FFFFFF.toInt()
    private const val PANEL = 0x26FFFFFF

    fun color(status: LessonStatus): Int = when (status) {
        // Verified counts as completed on the card, so it shares the legend's colour.
        LessonStatus.VERIFIED, LessonStatus.COMPLETED -> 0xFF22C55E.toInt()
        LessonStatus.IN_PROGRESS -> 0xFFF59E0B.toInt()
        LessonStatus.PASSED -> 0xFF60A5FA.toInt()
        LessonStatus.NOT_STARTED -> 0x40FFFFFF
    }

    fun render(s: ShareSnapshot, labels: ShareLabels): Bitmap {
        val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val c = Canvas(bitmap)
        val bg = Paint().apply {
            shader = LinearGradient(0f, 0f, 0f, HEIGHT.toFloat(), 0xFF1E1B4B.toInt(), 0xFF4338CA.toInt(), Shader.TileMode.CLAMP)
        }
        c.drawRect(0f, 0f, WIDTH.toFloat(), HEIGHT.toFloat(), bg)

        c.drawText("Hangul Hunt 한글", MARGIN, 150f, text(76f, WHITE, bold = true))
        c.drawText(if (s.playerName.isBlank()) s.date.toString() else "${s.playerName} · ${s.date}", MARGIN, 210f, text(36f, MUTED))

        val boxW = (WIDTH - 2 * MARGIN - 2 * 30f) / 3
        val boxes = listOf(
            "🔥 ${s.streak}" to labels.streakDays,
            "Lv ${s.level}" to "${s.totalXp} ${labels.xp}",
            "${s.learnedWords}" to labels.words,
        )
        boxes.forEachIndexed { i, (big, small) ->
            val left = MARGIN + i * (boxW + 30f)
            c.drawRoundRect(RectF(left, 260f, left + boxW, 520f), 36f, 36f, fill(PANEL))
            c.drawText(big, left + boxW / 2, 410f, text(80f, WHITE, bold = true, center = true))
            c.drawText(small, left + boxW / 2, 475f, text(34f, MUTED, center = true))
        }

        c.drawText("${labels.lessons}: ${s.completedLessons} / ${s.statuses.size}", MARGIN, 610f, text(44f, WHITE, bold = true))
        val gap = 16f
        val cell = (WIDTH - 2 * MARGIN - 8 * gap) / 9
        s.statuses.forEachIndexed { i, status ->
            val unit = i / 2
            val row = i % 2
            val left = MARGIN + unit * (cell + gap)
            val top = 650f + row * (cell + gap)
            c.drawRoundRect(RectF(left, top, left + cell, top + cell), 20f, 20f, fill(color(status)))
        }
        val numbers = text(30f, MUTED, center = true)
        for (unit in 0 until 9) c.drawText("${unit + 1}", MARGIN + unit * (cell + gap) + cell / 2, 650f + 2 * cell + gap + 45f, numbers)

        val legend = listOf(
            LessonStatus.COMPLETED to labels.completed,
            LessonStatus.IN_PROGRESS to labels.inProgress,
            LessonStatus.PASSED to labels.passed,
            LessonStatus.NOT_STARTED to labels.notStarted,
        )
        var x = MARGIN
        val legendText = text(30f, MUTED)
        legend.forEach { (status, label) ->
            c.drawCircle(x + 14f, 1010f, 14f, fill(color(status)))
            c.drawText(label, x + 38f, 1021f, legendText)
            x += 38f + legendText.measureText(label) + 34f
        }

        s.currentLessonTitle?.let { c.drawText("${labels.now}: $it", MARGIN, 1130f, text(42f, WHITE)) }
        c.drawText(labels.footer, MARGIN, 1290f, text(30f, MUTED))
        return bitmap
    }

    private fun fill(color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }

    private fun text(size: Float, color: Int, bold: Boolean = false, center: Boolean = false) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = size
        this.color = color
        typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        if (center) textAlign = Paint.Align.CENTER
    }
}

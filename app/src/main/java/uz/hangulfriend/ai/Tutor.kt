package uz.hangulfriend.ai

import kotlinx.serialization.SerialName
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import uz.hangulfriend.content.Exercise
import uz.hangulfriend.content.Grammar
import uz.hangulfriend.content.Lesson

@Serializable
data class TranslationVerdict(
    val correct: Boolean,
    @SerialName("corrected_ko") val correctedKo: String,
    @SerialName("explanation_uz") val explanationUz: String,
)

fun parseVerdict(json: String): TranslationVerdict? = try {
    GeminiJson.decodeFromString(TranslationVerdict.serializer(), json.trim())
} catch (_: SerializationException) {
    null
} catch (_: IllegalArgumentException) {
    null
}

object TutorPrompts {
    const val ROLE =
        "Sen o'zbek tilida so'zlashuvchi, TOPIK I (2-daraja) darajasidagi talabaga koreys tilini o'rgatuvchi " +
            "sabrli o'qituvchisan. Faqat o'zbek tilida (lotin yozuvida) javob ber. Qisqa va sodda yoz. " +
            "Har bir koreyscha misoldan keyin qavs ichida o'zbekcha tarjimasini ber. Kerak bo'lsa o'zbek " +
            "grammatikasi bilan taqqosla. \"사랑해요 한국어 2\" darsligi darajasidan murakkab grammatika ishlatma."

    fun grammarContext(g: Grammar, lesson: Lesson): String = buildString {
        appendLine(ROLE)
        appendLine()
        appendLine("Hozirgi dars: ${lesson.titleKo} (${lesson.titleUz}).")
        appendLine("Grammatika: ${g.pattern} — ${g.meaningUz}")
        g.formation.forEach { appendLine("- ${it.conditionUz}: ${it.rule} (${it.example})") }
        appendLine("Misollar:")
        g.examples.forEach { appendLine("- ${it.ko} (${it.uz})") }
        appendLine("Dars so'zlari:")
        lesson.words.forEach { appendLine("- ${it.ko} — ${it.uz}") }
    }

    fun mistakeQuestion(e: Exercise, userAnswer: String): String = buildString {
        appendLine("Men mashqda xato qildim. Nega xato ekanini tushuntirib bering.")
        appendLine("Topshiriq: ${e.promptUz}")
        e.sourceUz?.let { appendLine("O'zbekcha gap: $it") }
        e.sentence?.let { appendLine("Gap: $it") }
        if (e.base != null && e.form != null) appendLine("So'z va shakl: ${e.base} + ${e.form}")
        appendLine("Mening javobim: $userAnswer")
        appendLine("To'g'ri javob: ${e.answers.first()}")
    }

    fun translationCheck(e: Exercise, userAnswer: String): String = buildString {
        appendLine("O'quvchi o'zbekcha gapni koreyschaga tarjima qildi. Tarjima grammatik jihatdan to'g'ri va ma'nosi mosmi?")
        appendLine("O'zbekcha gap: ${e.sourceUz}")
        appendLine("Namunaviy javoblar: ${e.answers.joinToString(" / ")}")
        appendLine("O'quvchi javobi: $userAnswer")
        appendLine(
            "Faqat JSON qaytar: {\"correct\": true yoki false, \"corrected_ko\": \"to'g'ri koreyscha variant\", " +
                "\"explanation_uz\": \"qisqa o'zbekcha izoh\"}. Imlo va bo'sh joydagi mayda farqlarni xato hisoblama.",
        )
    }
}

class TutorService(private val client: GeminiClient) {
    suspend fun ask(system: String, history: List<ChatMessage>): AiResult = client.generate(system, history)

    /** null when the AI is unavailable or answers with anything but the expected JSON. */
    suspend fun checkTranslation(e: Exercise, userAnswer: String): TranslationVerdict? {
        val result = client.generate(
            TutorPrompts.ROLE,
            listOf(ChatMessage(fromUser = true, text = TutorPrompts.translationCheck(e, userAnswer))),
            json = true,
        )
        return (result as? AiResult.Success)?.let { parseVerdict(it.text) }
    }
}

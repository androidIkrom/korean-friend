package uz.hangulfriend.ai

import kotlinx.serialization.SerialName
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import uz.hangulfriend.content.Exercise
import uz.hangulfriend.content.Grammar
import uz.hangulfriend.content.Lesson
import uz.hangulfriend.i18n.AppLanguage

@Serializable
data class TranslationVerdict(
    val correct: Boolean,
    @SerialName("corrected_ko") val correctedKo: String,
    /** In the learner's language; the key name predates English. */
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
            "grammatikasi bilan taqqosla. \"사랑해요 한국어 2\" darsligi darajasidan murakkab grammatika ishlatma. " +
            "LaTeX yoki $...$ belgilarini ishlatma, strelka uchun faqat → yoz. Sarlavha (#) o'rniga qalin (**...**) matn ishlat. " +
            "Dars izohiga zid misol yoki qoida keltirma."

    const val ROLE_EN =
        "You are a patient Korean teacher for an English-speaking learner at TOPIK I (level 2). " +
            "Answer only in English. Keep it short and simple. After every Korean example give its English " +
            "translation in brackets. Compare with English grammar when it helps. Do not use grammar beyond " +
            "the level of the \"사랑해요 한국어 2\" textbook. " +
            "Do not use LaTeX or $...$; write arrows only as →. Use **bold** text instead of headings (#). " +
            "Never give an example or rule that contradicts the lesson explanation."

    fun role(lang: AppLanguage, name: String = ""): String {
        val base = if (lang == AppLanguage.EN) ROLE_EN else ROLE
        if (name.isBlank()) return base
        return base + if (lang == AppLanguage.EN) {
            " The learner's name is $name; address them by name now and then."
        } else {
            " O'quvchining ismi $name; vaqti-vaqti bilan unga ismi bilan murojaat qil."
        }
    }

    /** [lesson] is already localized, so its translations are in [lang]. */
    fun grammarContext(g: Grammar, lesson: Lesson, lang: AppLanguage = AppLanguage.UZ, name: String = ""): String = buildString {
        val en = lang == AppLanguage.EN
        appendLine(role(lang, name))
        appendLine()
        appendLine((if (en) "Current lesson: " else "Hozirgi dars: ") + "${lesson.titleKo} (${lesson.titleUz}).")
        appendLine((if (en) "Grammar: " else "Grammatika: ") + "${g.pattern} — ${g.meaningUz}")
        g.formation.forEach { appendLine("- ${it.conditionUz}: ${it.rule} (${it.example})") }
        appendLine(if (en) "Lesson explanation (follow it):" else "Dars izohi (shunga amal qil):")
        appendLine(g.explanationMd)
        appendLine(if (en) "Examples:" else "Misollar:")
        g.examples.forEach { appendLine("- ${it.ko} (${it.uz})") }
        appendLine(if (en) "Lesson words:" else "Dars so'zlari:")
        lesson.words.forEach { appendLine("- ${it.ko} — ${it.uz}") }
    }

    fun mistakeQuestion(e: Exercise, userAnswer: String, lang: AppLanguage = AppLanguage.UZ): String = buildString {
        if (lang == AppLanguage.EN) {
            appendLine("I made a mistake in an exercise. Please explain why it is wrong.")
            appendLine("Task: ${e.promptUz}")
            e.sourceUz?.let { appendLine("English sentence: $it") }
            e.sentence?.let { appendLine("Sentence: $it") }
            if (e.base != null && e.form != null) appendLine("Word and form: ${e.base} + ${e.form}")
            appendLine("My answer: $userAnswer")
            appendLine("Correct answer: ${e.answers.first()}")
        } else {
            appendLine("Men mashqda xato qildim. Nega xato ekanini tushuntirib bering.")
            appendLine("Topshiriq: ${e.promptUz}")
            e.sourceUz?.let { appendLine("O'zbekcha gap: $it") }
            e.sentence?.let { appendLine("Gap: $it") }
            if (e.base != null && e.form != null) appendLine("So'z va shakl: ${e.base} + ${e.form}")
            appendLine("Mening javobim: $userAnswer")
            appendLine("To'g'ri javob: ${e.answers.first()}")
        }
    }

    fun translationCheck(e: Exercise, userAnswer: String, lang: AppLanguage = AppLanguage.UZ): String = buildString {
        if (lang == AppLanguage.EN) {
            appendLine("The learner translated an English sentence into Korean. Is the translation grammatically correct and does it mean the same?")
            appendLine("English sentence: ${e.sourceUz}")
            appendLine("Model answers: ${e.answers.joinToString(" / ")}")
            appendLine("Learner's answer: $userAnswer")
            appendLine(
                "Return only JSON: {\"correct\": true or false, \"corrected_ko\": \"the correct Korean version\", " +
                    "\"explanation_uz\": \"a short explanation in English\"}. Do not count small spelling or spacing differences as mistakes.",
            )
        } else {
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
}

class TutorService(private val client: GeminiClient) {
    suspend fun ask(system: String, history: List<ChatMessage>): AiResult = client.generate(system, history)

    /** null when the AI is unavailable or answers with anything but the expected JSON. */
    suspend fun checkTranslation(e: Exercise, userAnswer: String, lang: AppLanguage = AppLanguage.UZ): TranslationVerdict? {
        val result = client.generate(
            TutorPrompts.role(lang),
            listOf(ChatMessage(fromUser = true, text = TutorPrompts.translationCheck(e, userAnswer, lang))),
            json = true,
        )
        return (result as? AiResult.Success)?.let { parseVerdict(it.text) }
    }
}

private val LATEX_ARROW = Regex("""\$?\\(?:(?:long)?(?:right|Right)arrow|to\b)\$?""")
private val LATEX_DOLLARS = Regex("""\$([^$\n]{1,40})\$""")

/** Cleans what the model sends despite the prompt: LaTeX arrows become →, other `$…$` lose the dollars. */
fun cleanAiText(text: String): String = text.replace(LATEX_ARROW, "→").replace(LATEX_DOLLARS, "$1")

package uz.hangulfriend.study.games

import kotlin.random.Random
import uz.hangulfriend.content.Word

data class SpeedQuestion(val word: Word, val options: List<String>)

/** As many meanings as possible in [DURATION_MS]; the screen owns the timer. */
class SpeedRound(private val words: List<Word>, private val random: Random) {
    private var current: SpeedQuestion? = null
    var score = 0
        private set

    fun next(): SpeedQuestion {
        val word = words.random(random)
        val wrong = words.filter { it.uz != word.uz }.map { it.uz }.distinct().shuffled(random).take(OPTIONS - 1)
        return SpeedQuestion(word, (wrong + word.uz).shuffled(random)).also { current = it }
    }

    fun answer(choice: String): Boolean {
        val correct = choice == current?.word?.uz
        if (correct) score++
        return correct
    }

    companion object {
        const val DURATION_MS = 60_000L
        const val OPTIONS = 4
    }
}

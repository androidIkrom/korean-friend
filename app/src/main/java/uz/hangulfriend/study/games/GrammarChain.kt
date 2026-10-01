package uz.hangulfriend.study.games

import kotlin.random.Random
import uz.hangulfriend.content.Exercise
import uz.hangulfriend.content.ExerciseType
import uz.hangulfriend.hangul.AnswerChecker

/** Conjugation exercises one after another; the first mistake ends the chain. */
class GrammarChain(exercises: List<Exercise>, random: Random) {
    private val queue = exercises.filter { it.type == ExerciseType.CONJUGATE }.shuffled(random)
    private var index = 0
    val total: Int get() = queue.size
    var length = 0
        private set
    var over = queue.isEmpty()
        private set

    fun current(): Exercise? = if (over) null else queue[index]

    fun submit(input: String): Boolean {
        val e = current() ?: return false
        val correct = AnswerChecker.check(input, e.answers).correct
        if (correct) {
            length++
            index++
            if (index == queue.size) over = true
        } else {
            over = true
        }
        return correct
    }
}

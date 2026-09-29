package uz.hangulfriend.srs

import java.time.Duration
import java.time.Instant
import kotlin.math.E
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.round

enum class Rating(val value: Int) { AGAIN(1), HARD(2), GOOD(3), EASY(4) }

enum class CardState(val value: Int) { LEARNING(1), REVIEW(2), RELEARNING(3) }

data class SrsCard(
    val state: CardState,
    val step: Int?,
    val stability: Double?,
    val difficulty: Double?,
    val due: Instant,
    val lastReview: Instant?,
)

/**
 * FSRS-6 scheduler, ported from py-fsrs 6.3.2 (`fsrs/scheduler.py`) without interval fuzzing.
 * `core/srs/src/test/resources/fsrs_vectors.json` pins its output to py-fsrs.
 */
class FsrsScheduler(
    private val parameters: DoubleArray = DEFAULT_PARAMETERS,
    private val desiredRetention: Double = 0.9,
    private val learningSteps: List<Duration> = listOf(Duration.ofMinutes(1), Duration.ofMinutes(10)),
    private val relearningSteps: List<Duration> = listOf(Duration.ofMinutes(10)),
    private val maximumInterval: Int = 36500,
) {
    private val decay = -parameters[20]
    private val factor = 0.9.pow(1 / decay) - 1

    fun newCard(now: Instant): SrsCard =
        SrsCard(CardState.LEARNING, step = 0, stability = null, difficulty = null, due = now, lastReview = null)

    fun retrievability(card: SrsCard, now: Instant): Double {
        val last = card.lastReview ?: return 0.0
        val stability = card.stability ?: return 0.0
        val elapsedDays = max(0L, Duration.between(last, now).toDays())
        return (1 + factor * elapsedDays / stability).pow(decay)
    }

    fun review(card: SrsCard, rating: Rating, now: Instant): SrsCard {
        val daysSinceLastReview = card.lastReview?.let { Duration.between(it, now).toDays() }
        val sameDay = daysSinceLastReview != null && daysSinceLastReview < 1
        var stability: Double
        var difficulty: Double
        var state = card.state
        var step = card.step
        val interval: Duration

        when (card.state) {
            CardState.LEARNING, CardState.RELEARNING -> {
                if (card.stability == null || card.difficulty == null) {
                    stability = initialStability(rating)
                    difficulty = clampDifficulty(initialDifficulty(rating))
                } else if (sameDay) {
                    stability = shortTermStability(card.stability, rating)
                    difficulty = nextDifficulty(card.difficulty, rating)
                } else {
                    stability = nextStability(card.difficulty, card.stability, retrievability(card, now), rating)
                    difficulty = nextDifficulty(card.difficulty, rating)
                }
                val steps = if (card.state == CardState.LEARNING) learningSteps else relearningSteps
                val current = step!!
                if (steps.isEmpty() || (current >= steps.size && rating != Rating.AGAIN)) {
                    state = CardState.REVIEW
                    step = null
                    interval = Duration.ofDays(nextInterval(stability).toLong())
                } else {
                    when (rating) {
                        Rating.AGAIN -> {
                            step = 0
                            interval = steps[0]
                        }
                        Rating.HARD -> interval = when {
                            current == 0 && steps.size == 1 -> steps[0].multipliedBy(3).dividedBy(2)
                            current == 0 -> steps[0].plus(steps[1]).dividedBy(2)
                            else -> steps[current]
                        }
                        Rating.GOOD -> if (current + 1 == steps.size) {
                            state = CardState.REVIEW
                            step = null
                            interval = Duration.ofDays(nextInterval(stability).toLong())
                        } else {
                            step = current + 1
                            interval = steps[current + 1]
                        }
                        Rating.EASY -> {
                            state = CardState.REVIEW
                            step = null
                            interval = Duration.ofDays(nextInterval(stability).toLong())
                        }
                    }
                }
            }
            CardState.REVIEW -> {
                val s = card.stability!!
                val d = card.difficulty!!
                stability = if (sameDay) {
                    shortTermStability(s, rating)
                } else {
                    nextStability(d, s, retrievability(card, now), rating)
                }
                difficulty = nextDifficulty(d, rating)
                if (rating == Rating.AGAIN && relearningSteps.isNotEmpty()) {
                    state = CardState.RELEARNING
                    step = 0
                    interval = relearningSteps[0]
                } else {
                    interval = Duration.ofDays(nextInterval(stability).toLong())
                }
            }
        }
        return SrsCard(state, step, stability, difficulty, due = now.plus(interval), lastReview = now)
    }

    private fun clampDifficulty(d: Double) = d.coerceIn(MIN_DIFFICULTY, MAX_DIFFICULTY)

    private fun clampStability(s: Double) = max(s, STABILITY_MIN)

    private fun initialStability(rating: Rating) = clampStability(parameters[rating.value - 1])

    private fun initialDifficulty(rating: Rating) =
        parameters[4] - E.pow(parameters[5] * (rating.value - 1)) + 1

    /** Python's round() is round-half-to-even, as is Kotlin's [round]. */
    private fun nextInterval(stability: Double): Int {
        val days = (stability / factor) * (desiredRetention.pow(1 / decay) - 1)
        return round(days).toInt().coerceIn(1, maximumInterval)
    }

    private fun shortTermStability(stability: Double, rating: Rating): Double {
        var increase = E.pow(parameters[17] * (rating.value - 3 + parameters[18])) * stability.pow(-parameters[19])
        if (rating != Rating.AGAIN) increase = max(increase, 1.0)
        return clampStability(stability * increase)
    }

    private fun nextDifficulty(difficulty: Double, rating: Rating): Double {
        val delta = -(parameters[6] * (rating.value - 3))
        val damped = difficulty + (10.0 - difficulty) * delta / 9.0
        val reverted = parameters[7] * initialDifficulty(Rating.EASY) + (1 - parameters[7]) * damped
        return clampDifficulty(reverted)
    }

    private fun nextStability(difficulty: Double, stability: Double, retrievability: Double, rating: Rating): Double {
        val next = if (rating == Rating.AGAIN) {
            val longTerm = parameters[11] * difficulty.pow(-parameters[12]) *
                ((stability + 1).pow(parameters[13]) - 1) * E.pow((1 - retrievability) * parameters[14])
            val shortTerm = stability / E.pow(parameters[17] * parameters[18])
            min(longTerm, shortTerm)
        } else {
            val hardPenalty = if (rating == Rating.HARD) parameters[15] else 1.0
            val easyBonus = if (rating == Rating.EASY) parameters[16] else 1.0
            stability * (
                1 + E.pow(parameters[8]) * (11 - difficulty) * stability.pow(-parameters[9]) *
                    (E.pow((1 - retrievability) * parameters[10]) - 1) * hardPenalty * easyBonus
                )
        }
        return clampStability(next)
    }

    companion object {
        val DEFAULT_PARAMETERS = doubleArrayOf(
            0.212, 1.2931, 2.3065, 8.2956, 6.4133, 0.8334, 3.0194, 0.001, 1.8722, 0.1666, 0.796,
            1.4835, 0.0614, 0.2629, 1.6483, 0.6014, 1.8729, 0.5425, 0.0912, 0.0658, 0.1542,
        )
        private const val STABILITY_MIN = 0.001
        private const val MIN_DIFFICULTY = 1.0
        private const val MAX_DIFFICULTY = 10.0
    }
}

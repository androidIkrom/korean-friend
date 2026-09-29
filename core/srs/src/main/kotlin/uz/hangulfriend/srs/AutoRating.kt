package uz.hangulfriend.srs

/** Derives an FSRS rating from an automatically checked answer (every exercise except flashcards). */
object AutoRating {
    private const val SLOW_MS = 15_000L

    fun from(correct: Boolean, usedHint: Boolean, elapsedMs: Long): Rating = when {
        !correct -> Rating.AGAIN
        usedHint || elapsedMs > SLOW_MS -> Rating.HARD
        else -> Rating.GOOD
    }
}

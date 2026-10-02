package uz.hangulfriend.data

import androidx.room.withTransaction
import java.time.Clock
import java.time.Instant
import uz.hangulfriend.content.Lesson
import uz.hangulfriend.srs.CardState
import uz.hangulfriend.srs.FsrsScheduler
import uz.hangulfriend.srs.Rating
import uz.hangulfriend.srs.SrsCard

class StudyRepository(
    private val db: AppDatabase,
    private val scheduler: FsrsScheduler,
    private val clock: Clock,
) {
    private val cards = db.cards()

    suspend fun ensureCards(lesson: Lesson, lessonOrder: Int, origin: CardOrigin) {
        val srs = scheduler.newCard(clock.instant())
        val items = lesson.words.flatMap { listOf(it.id to CardKind.RECOGNIZE, it.id to CardKind.PRODUCE) } +
            lesson.grammar.map { it.id to CardKind.GRAMMAR }
        val fresh = items.map { (itemId, kind) ->
            CardEntity(
                id = CardIds.of(itemId, kind),
                itemId = itemId,
                kind = kind,
                lessonId = lesson.id,
                origin = origin,
                state = srs.state,
                step = srs.step,
                stability = null,
                difficulty = null,
                dueMs = srs.due.toEpochMilli(),
                lastReviewMs = null,
                firstReviewedMs = null,
                reps = 0,
                lapses = 0,
                lessonOrder = lessonOrder,
            )
        }
        db.withTransaction {
            cards.insertIgnore(fresh)
            if (origin == CardOrigin.LESSON) cards.promoteUnseenBacklog(lesson.id)
        }
    }

    suspend fun grade(cardId: String, rating: Rating, elapsedMs: Long) {
        val now = clock.instant()
        val nowMs = now.toEpochMilli()
        db.withTransaction {
            val card = cards.get(cardId) ?: return@withTransaction
            val next = scheduler.review(card.toSrs(), rating, now)
            cards.update(
                card.copy(
                    state = next.state,
                    step = next.step,
                    stability = next.stability,
                    difficulty = next.difficulty,
                    dueMs = next.due.toEpochMilli(),
                    lastReviewMs = nowMs,
                    firstReviewedMs = card.firstReviewedMs ?: nowMs,
                    reps = card.reps + 1,
                    lapses = card.lapses + if (card.state == CardState.REVIEW && rating == Rating.AGAIN) 1 else 0,
                ),
            )
            db.logs().insert(ReviewLogEntity(cardId = cardId, rating = rating.value, reviewedMs = nowMs, elapsedMs = elapsedMs))
        }
    }

    /**
     * Due cards that have been reviewed before (by due time), then never-reviewed cards up to what is
     * left of today's new-card limit. Every card reviewed for the first time today counts toward it.
     */
    suspend fun dueQueue(dailyNewLimit: Int): List<CardEntity> {
        val now = clock.instant()
        val today = now.atZone(clock.zone).toLocalDate()
        val dayStart = today.atStartOfDay(clock.zone).toInstant().toEpochMilli()
        val dayEnd = today.plusDays(1).atStartOfDay(clock.zone).toInstant().toEpochMilli()
        val left = (dailyNewLimit - cards.firstReviewedBetween(dayStart, dayEnd)).coerceAtLeast(0)
        return cards.dueReviewed(now.toEpochMilli()) + cards.unseen(left)
    }

    private fun CardEntity.toSrs() = SrsCard(
        state = state,
        step = step,
        stability = stability,
        difficulty = difficulty,
        due = Instant.ofEpochMilli(dueMs),
        lastReview = lastReviewMs?.let(Instant::ofEpochMilli),
    )
}

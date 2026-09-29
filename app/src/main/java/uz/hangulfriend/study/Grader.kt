package uz.hangulfriend.study

import uz.hangulfriend.data.CardIds
import uz.hangulfriend.data.CardKind
import uz.hangulfriend.data.StudyRepository
import uz.hangulfriend.srs.AutoRating
import uz.hangulfriend.srs.Rating

class Grader(private val study: StudyRepository) {
    suspend fun gradeAuto(item: ExerciseItem, correct: Boolean, usedHint: Boolean, elapsedMs: Long) {
        val rating = AutoRating.from(correct, usedHint, elapsedMs)
        item.cardIds.forEach { study.grade(it, rating, elapsedMs) }
    }

    suspend fun gradeFlashcard(item: ExerciseItem.Flashcard, rating: Rating) {
        item.cardIds.forEach { study.grade(it, rating, 0) }
    }

    /** [firstTryCorrect] holds the word ids matched on the first attempt. */
    suspend fun gradeMatch(item: ExerciseItem.Match, firstTryCorrect: Set<String>) {
        for (word in item.words) {
            val rating = if (word.id in firstTryCorrect) Rating.GOOD else Rating.AGAIN
            study.grade(CardIds.of(word.id, CardKind.RECOGNIZE), rating, 0)
        }
    }
}

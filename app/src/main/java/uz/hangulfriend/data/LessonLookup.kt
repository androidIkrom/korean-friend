package uz.hangulfriend.data

import uz.hangulfriend.content.ContentRepository
import uz.hangulfriend.content.Lesson

/** Finds the lesson a card belongs to: a book lesson, or the pseudo-lesson of the learner's own words. */
class LessonLookup(private val content: ContentRepository, private val userWords: UserWordRepository) {
    suspend fun lesson(id: String): Lesson? = if (id == USER_LESSON_ID) userWords.asLesson() else content.lesson(id)
}

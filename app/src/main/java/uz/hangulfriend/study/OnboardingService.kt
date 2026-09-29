package uz.hangulfriend.study

import uz.hangulfriend.content.ContentRepository
import uz.hangulfriend.data.CardOrigin
import uz.hangulfriend.data.ProgressRepository
import uz.hangulfriend.data.SettingsRepository
import uz.hangulfriend.data.StudyRepository

/** Sets the lesson the learner is on; everything before it counts as passed and feeds the review backlog. */
class OnboardingService(
    private val content: ContentRepository,
    private val study: StudyRepository,
    private val progress: ProgressRepository,
    private val settings: SettingsRepository,
) {
    suspend fun complete(currentLessonId: String) {
        val catalog = content.catalog()
        val currentIndex = catalog.indexOfFirst { it.id == currentLessonId }
        require(currentIndex >= 0) { "Unknown lesson $currentLessonId" }
        val earlier = catalog.subList(0, currentIndex)
        progress.markPassed(earlier.map { it.id })
        earlier.forEachIndexed { order, entry ->
            content.lesson(entry.id)?.let { study.ensureCards(it, order, CardOrigin.BACKLOG) }
        }
        settings.setCurrentLesson(currentLessonId)
        settings.setOnboarded()
    }
}

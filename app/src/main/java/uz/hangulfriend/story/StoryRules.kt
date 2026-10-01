package uz.hangulfriend.story

import uz.hangulfriend.data.LessonStatus

enum class EpisodeState { LOCKED, COMING_SOON, NEW, DONE }

object StoryRules {
    private val UNLOCKING = setOf(LessonStatus.PASSED, LessonStatus.COMPLETED, LessonStatus.VERIFIED)

    /** An episode opens once its lesson is passed, completed or verified; lessons without a story are "coming soon". */
    fun state(status: LessonStatus?, hasStory: Boolean, done: Boolean): EpisodeState = when {
        !hasStory -> EpisodeState.COMING_SOON
        status !in UNLOCKING -> EpisodeState.LOCKED
        done -> EpisodeState.DONE
        else -> EpisodeState.NEW
    }
}

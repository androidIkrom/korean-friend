package uz.hangulfriend.story

import org.junit.Assert.assertEquals
import org.junit.Test
import uz.hangulfriend.data.LessonStatus

class StoryRulesTest {
    @Test fun lockedUntilLessonPassed() {
        for (status in listOf(null, LessonStatus.NOT_STARTED, LessonStatus.IN_PROGRESS)) {
            assertEquals(EpisodeState.LOCKED, StoryRules.state(status, hasStory = true, done = false))
        }
    }

    @Test fun newOncePassedCompletedOrVerified() {
        for (status in listOf(LessonStatus.PASSED, LessonStatus.COMPLETED, LessonStatus.VERIFIED)) {
            assertEquals(EpisodeState.NEW, StoryRules.state(status, hasStory = true, done = false))
        }
    }

    @Test fun doneWhenFinished() =
        assertEquals(EpisodeState.DONE, StoryRules.state(LessonStatus.COMPLETED, hasStory = true, done = true))

    @Test fun comingSoonWithoutStory() {
        for (status in LessonStatus.entries) {
            assertEquals(EpisodeState.COMING_SOON, StoryRules.state(status, hasStory = false, done = false))
        }
    }
}

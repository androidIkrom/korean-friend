package uz.hangulfriend.story

import uz.hangulfriend.content.ChooseReply
import uz.hangulfriend.content.Story
import uz.hangulfriend.content.StoryLine
import uz.hangulfriend.content.StoryQuiz
import uz.hangulfriend.content.StoryStep

/**
 * Immutable playback state of one episode. [index] is the step waiting for "next" or an answer;
 * it equals the step count once the episode is finished.
 */
class StoryPlayer private constructor(
    private val story: Story,
    private val index: Int,
    val eliminated: Set<String>,
    val lastWrongWhy: String?,
) {
    constructor(story: Story) : this(story, 0, emptySet(), null)

    val finished: Boolean get() = index >= story.steps.size

    val current: StoryStep? get() = story.steps.getOrNull(index)

    /** Steps revealed so far, including the current one; a solved choice stays as Aziz's line. */
    val shown: List<StoryStep> get() = story.steps.take(minOf(index + 1, story.steps.size))

    val progress: Float get() = if (story.steps.isEmpty()) 1f else shown.size.toFloat() / story.steps.size

    fun next(): StoryPlayer {
        check(current is StoryLine) { "next() is only for lines" }
        return advance()
    }

    fun choose(option: String): StoryPlayer {
        val (answer, why) = when (val step = current) {
            is ChooseReply -> step.answer to step.whyUz
            is StoryQuiz -> step.answer to step.whyUz
            else -> error("choose() needs an interactive step")
        }
        return if (option == answer) advance() else StoryPlayer(story, index, eliminated + option, why)
    }

    private fun advance() = StoryPlayer(story, index + 1, emptySet(), null)
}

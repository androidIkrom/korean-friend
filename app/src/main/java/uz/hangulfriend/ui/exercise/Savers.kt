package uz.hangulfriend.ui.exercise

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList

/** Keeps the answer panel across rotation; a null panel (not answered yet) is simply not saved. */
val FeedbackInfoSaver: Saver<FeedbackInfo?, Any> = Saver(
    save = { info ->
        info?.let { arrayListOf<Any?>(it.correct, it.message, it.correctAnswer, it.why, ArrayList(it.samples), it.askAi) }
    },
    restore = { saved ->
        val v = saved as List<*>
        FeedbackInfo(
            correct = v[0] as Boolean,
            message = v[1] as String,
            correctAnswer = v[2] as String?,
            why = v[3] as String?,
            samples = (v[4] as List<*>).map { it as String },
            askAi = v[5] as String?,
        )
    },
)

val IntListSaver: Saver<SnapshotStateList<Int>, Any> = Saver(
    save = { ArrayList(it) },
    restore = { saved -> (saved as List<*>).map { it as Int }.toMutableStateList() },
)

val StringListSaver: Saver<SnapshotStateList<String>, Any> = Saver(
    save = { ArrayList(it) },
    restore = { saved -> (saved as List<*>).map { it as String }.toMutableStateList() },
)

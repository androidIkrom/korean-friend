package uz.hangulfriend.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import uz.hangulfriend.R
import uz.hangulfriend.data.LessonStatus

@Composable
fun statusLabel(status: LessonStatus): String = stringResource(
    when (status) {
        LessonStatus.NOT_STARTED -> R.string.status_not_started
        LessonStatus.PASSED -> R.string.status_passed
        LessonStatus.IN_PROGRESS -> R.string.status_in_progress
        LessonStatus.COMPLETED -> R.string.status_completed
        LessonStatus.VERIFIED -> R.string.status_verified
    },
)

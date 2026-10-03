package uz.hangulfriend.ui.home

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uz.hangulfriend.R
import uz.hangulfriend.share.ProgressStats
import uz.hangulfriend.share.ShareCardRenderer
import uz.hangulfriend.share.ShareLabels
import uz.hangulfriend.share.ShareSender

/**
 * Renders the progress card, previews it, then hands the PNG to the share sheet. [trigger] draws the
 * control that starts it (a share icon by default).
 */
@Composable
fun ShareCardButton(
    stats: ProgressStats,
    trigger: @Composable (onClick: () -> Unit) -> Unit = { onClick ->
        IconButton(onClick = onClick) { Icon(Icons.Filled.Share, contentDescription = stringResource(R.string.share_title)) }
    },
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var preview by remember { mutableStateOf<Bitmap?>(null) }
    trigger {
        scope.launch {
            val labels = labels(context)
            preview = withContext(Dispatchers.Default) { ShareCardRenderer.render(stats.snapshot(), labels) }
        }
    }

    preview?.let { bitmap ->
        AlertDialog(
            onDismissRequest = { preview = null },
            title = { Text(stringResource(R.string.share_title)) },
            text = { Image(bitmap.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxWidth()) },
            confirmButton = {
                TextButton(onClick = {
                    preview = null
                    scope.launch {
                        val file = withContext(Dispatchers.IO) { ShareSender.writePng(context, bitmap) }
                        ShareSender.share(context, file)
                    }
                }) { Text(stringResource(R.string.share_send)) }
            },
            dismissButton = { TextButton(onClick = { preview = null }) { Text(stringResource(R.string.share_close)) } },
        )
    }
}

private fun labels(c: Context) = ShareLabels(
    streakDays = c.getString(R.string.share_streak),
    xp = "XP",
    words = c.getString(R.string.share_words),
    lessons = c.getString(R.string.share_lessons),
    now = c.getString(R.string.share_now),
    completed = c.getString(R.string.status_completed),
    inProgress = c.getString(R.string.status_in_progress),
    passed = c.getString(R.string.status_passed),
    notStarted = c.getString(R.string.status_not_started),
    footer = c.getString(R.string.share_footer),
)

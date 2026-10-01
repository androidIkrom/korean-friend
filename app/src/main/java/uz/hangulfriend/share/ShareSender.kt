package uz.hangulfriend.share

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import uz.hangulfriend.R

/** Writes the card to the cache and opens the system share sheet. Call [writePng] off the main thread. */
object ShareSender {
    private const val AUTHORITY = "uz.hangulfriend.fileprovider"

    fun writePng(context: Context, bitmap: Bitmap): File {
        val dir = File(context.cacheDir, "share").apply { mkdirs() }
        return File(dir, "progress.png").also { f -> f.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) } }
    }

    fun share(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, AUTHORITY, file)
        val send = Intent(Intent.ACTION_SEND)
            .setType("image/png")
            .putExtra(Intent.EXTRA_STREAM, uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        try {
            context.startActivity(Intent.createChooser(send, null))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, R.string.share_no_app, Toast.LENGTH_LONG).show()
        }
    }
}

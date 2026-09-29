package uz.hangulfriend.content

import android.content.res.AssetManager
import java.io.File
import java.io.FileNotFoundException

interface AssetSource {
    /** File contents, or null when the file does not exist. */
    fun read(path: String): String?

    fun list(dir: String): List<String>
}

class AndroidAssetSource(private val assets: AssetManager) : AssetSource {
    override fun read(path: String): String? =
        try {
            assets.open(path).bufferedReader().use { it.readText() }
        } catch (_: FileNotFoundException) {
            null
        }

    override fun list(dir: String): List<String> = assets.list(dir)?.toList().orEmpty()
}

class DirAssetSource(private val root: File) : AssetSource {
    override fun read(path: String): String? = File(root, path).takeIf { it.isFile }?.readText()

    override fun list(dir: String): List<String> = File(root, dir).list()?.sorted().orEmpty()
}

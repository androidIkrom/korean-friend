package uz.hangulfriend.ui.avatar

import android.content.res.AssetManager
import androidx.compose.runtime.staticCompositionLocalOf
import uz.hangulfriend.data.GameThemeId
import uz.hangulfriend.study.Rank

/** Optional drawn avatars in `assets/avatar/<theme>_<rank>.webp`; a present file replaces the vector art. */
class AvatarAssets(private val files: Set<String>) {
    fun slotFor(theme: GameThemeId, rank: Rank): String? {
        val name = "${theme.key}_${rank.name.lowercase()}.webp"
        return if (name in files) "avatar/$name" else null
    }

    companion object {
        fun load(assets: AssetManager): AvatarAssets =
            AvatarAssets(runCatching { assets.list("avatar")?.toSet() }.getOrNull().orEmpty())
    }
}

val LocalAvatarAssets = staticCompositionLocalOf { AvatarAssets(emptySet()) }

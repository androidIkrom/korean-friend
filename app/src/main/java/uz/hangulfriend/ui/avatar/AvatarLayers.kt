package uz.hangulfriend.ui.avatar

import androidx.annotation.StringRes
import uz.hangulfriend.R
import uz.hangulfriend.data.HeroGender
import uz.hangulfriend.study.Rank

/** Parts of the hero's look; higher ranks add parts (stage 6b spec §3.1). */
enum class AvatarLayer {
    HOOD, LONG_HAIR, BASE, JACKET, RIM, LONG_COAT, PONYTAIL, FLAME, PAULDRONS, EMBERS,
    CAPE, TWIN_BLADES, SWORD, HAIR_LIGHT, MONARCH, TIARA, COMPANIONS,
}

private fun addedAt(rank: Rank, hero: HeroGender): Set<AvatarLayer> {
    val girl = hero == HeroGender.GIRL
    return when (rank) {
        Rank.E -> if (girl) setOf(AvatarLayer.HOOD, AvatarLayer.LONG_HAIR, AvatarLayer.BASE) else setOf(AvatarLayer.HOOD, AvatarLayer.BASE)
        Rank.D -> setOf(AvatarLayer.JACKET, AvatarLayer.RIM)
        Rank.C -> setOfNotNull(AvatarLayer.LONG_COAT, AvatarLayer.FLAME, AvatarLayer.PONYTAIL.takeIf { girl })
        Rank.B -> setOf(AvatarLayer.PAULDRONS, AvatarLayer.EMBERS)
        Rank.A -> setOf(AvatarLayer.CAPE, AvatarLayer.HAIR_LIGHT, if (girl) AvatarLayer.SWORD else AvatarLayer.TWIN_BLADES)
        Rank.S -> setOfNotNull(AvatarLayer.MONARCH, AvatarLayer.COMPANIONS, AvatarLayer.TIARA.takeIf { girl })
    }
}

/** Every layer worn at [rank]: the hood gives way to a jacket at D, the girl's loose hair is tied up at C. */
fun layersFor(rank: Rank, hero: HeroGender): Set<AvatarLayer> {
    val all = Rank.entries.filter { it <= rank }.flatMap { addedAt(it, hero) }.toMutableSet()
    if (rank > Rank.E) all -= AvatarLayer.HOOD
    if (AvatarLayer.PONYTAIL in all) all -= AvatarLayer.LONG_HAIR
    return all
}

/** What the rank-up dialog lists as new at [rank]. */
fun newLayersAt(rank: Rank, hero: HeroGender): List<Int> {
    val girl = hero == HeroGender.GIRL
    return when (rank) {
        Rank.E -> emptyList()
        Rank.D -> listOf(R.string.avatar_new_jacket)
        Rank.C -> listOfNotNull(
            R.string.avatar_new_long_coat, R.string.avatar_new_gaze, R.string.avatar_new_flame,
            R.string.avatar_new_ponytail.takeIf { girl },
        )
        Rank.B -> listOf(R.string.avatar_new_pauldrons, R.string.avatar_new_embers)
        Rank.A -> listOf(R.string.avatar_new_cape, if (girl) R.string.avatar_new_sword else R.string.avatar_new_blades, R.string.avatar_new_hair_light)
        Rank.S -> listOfNotNull(
            R.string.avatar_new_monarch_flame, R.string.avatar_new_eye_trail, R.string.avatar_new_companions,
            R.string.avatar_new_tiara.takeIf { girl },
        )
    }
}

@StringRes
fun Rank.titleRes(): Int = when (this) {
    Rank.E -> R.string.rank_title_e
    Rank.D -> R.string.rank_title_d
    Rank.C -> R.string.rank_title_c
    Rank.B -> R.string.rank_title_b
    Rank.A -> R.string.rank_title_a
    Rank.S -> R.string.rank_title_s
}

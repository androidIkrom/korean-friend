package uz.hangulfriend.ui.avatar

import androidx.annotation.StringRes
import uz.hangulfriend.R
import uz.hangulfriend.study.Rank

/** Parts of Aziz's outfit and effects; higher ranks add parts (stage 6 spec §4.3). */
enum class AvatarLayer {
    HOOD, BASE, JACKET, RIM, LONG_COAT, EYE_GLOW, AURA_1, PAULDRONS, AURA_2, PARTICLES,
    CAPE, BLADES, HAIR_LIGHT, AURA_3, MONARCH_AURA, EYE_TRAIL, COMPANIONS,
}

private val added: Map<Rank, Set<AvatarLayer>> = mapOf(
    Rank.E to setOf(AvatarLayer.HOOD, AvatarLayer.BASE),
    Rank.D to setOf(AvatarLayer.JACKET, AvatarLayer.RIM),
    Rank.C to setOf(AvatarLayer.LONG_COAT, AvatarLayer.EYE_GLOW, AvatarLayer.AURA_1),
    Rank.B to setOf(AvatarLayer.PAULDRONS, AvatarLayer.AURA_2, AvatarLayer.PARTICLES),
    Rank.A to setOf(AvatarLayer.CAPE, AvatarLayer.BLADES, AvatarLayer.HAIR_LIGHT, AvatarLayer.AURA_3),
    Rank.S to setOf(AvatarLayer.MONARCH_AURA, AvatarLayer.EYE_TRAIL, AvatarLayer.COMPANIONS),
)

/** Every layer worn at [rank]; the hood is the one part a jacket replaces. */
fun layersFor(rank: Rank): Set<AvatarLayer> {
    val all = Rank.entries.filter { it <= rank }.flatMap { added.getValue(it) }.toSet()
    return if (rank > Rank.E) all - AvatarLayer.HOOD else all
}

/** What the rank-up dialog lists as new at [rank]. */
fun newLayersAt(rank: Rank): List<Int> = when (rank) {
    Rank.E -> emptyList()
    Rank.D -> listOf(R.string.avatar_new_jacket)
    Rank.C -> listOf(R.string.avatar_new_long_coat, R.string.avatar_new_eye_glow, R.string.avatar_new_aura)
    Rank.B -> listOf(R.string.avatar_new_pauldrons, R.string.avatar_new_aura_strong)
    Rank.A -> listOf(R.string.avatar_new_cape, R.string.avatar_new_blades, R.string.avatar_new_hair_light)
    Rank.S -> listOf(R.string.avatar_new_monarch_aura, R.string.avatar_new_eye_trail, R.string.avatar_new_companions)
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

package uz.hangulfriend.study

/** Hunter-style ranks shown by the avatar; each starts at [minLevel]. */
enum class Rank(val minLevel: Int) { E(1), D(5), C(10), B(15), A(20), S(30) }

/** Rank rules (stage 6 spec §4.1–4.2). Pure functions only. */
object RankRules {
    fun rankFor(level: Int): Rank = Rank.entries.lastOrNull { it.minLevel <= level } ?: Rank.E

    /**
     * The rank to announce, or null. Without a remembered rank (fresh install, update, import) nothing is
     * announced; the caller stores the current rank instead.
     */
    fun rankUpToShow(current: Rank, lastSeen: Rank?): Rank? =
        if (lastSeen != null && current > lastSeen) current else null
}

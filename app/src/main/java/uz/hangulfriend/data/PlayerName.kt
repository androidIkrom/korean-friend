package uz.hangulfriend.data

/** Longest player name kept; it must fit over the rank letter on Home. */
const val NAME_MAX = 20

/** A typed name as stored: inner whitespace collapsed to single spaces, trimmed, at most [NAME_MAX] characters. */
fun cleanName(raw: String): String = raw.trim().replace(Regex("\\s+"), " ").take(NAME_MAX).trim()

/** The name to show: the player's own, or [fallback] ("Ovchi" / "Hunter") when none is set. */
fun displayName(name: String, fallback: String): String = name.ifBlank { fallback }

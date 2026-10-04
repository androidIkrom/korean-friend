package uz.hangulfriend.data

/** Longest player name kept; it must fit over the rank letter on Home. */
const val NAME_MAX = 20

/** A typed name as stored: inner whitespace collapsed to single spaces, trimmed, at most [NAME_MAX] characters. */
fun cleanName(raw: String): String = raw.trim().replace(Regex("\\s+"), " ").take(NAME_MAX).trim()

/** The name to show: the player's own, or [fallback] ("Ovchi" / "Hunter") when none is set. */
fun displayName(name: String, fallback: String): String = name.ifBlank { fallback }

/** "Aziz" alone or with an Uzbek case/person suffix (Azizning, Azizga, Azizman); not other names (Azizbek). */
private val HERO_NAME = Regex("""\bAziz(?=(?:ning|ga|ni|da|dan|man)?\b)""")

/** Story narration about the hero ("Aziz … demoqchi") with the player's [name]; unchanged when none is set. */
fun personalize(text: String, name: String): String = if (name.isBlank()) text else HERO_NAME.replace(text, name)

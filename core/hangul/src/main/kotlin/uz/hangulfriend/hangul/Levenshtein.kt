package uz.hangulfriend.hangul

internal fun <T> levenshtein(a: List<T>, b: List<T>): Int {
    var prev = IntArray(b.size + 1) { it }
    for (i in 1..a.size) {
        val cur = IntArray(b.size + 1)
        cur[0] = i
        for (j in 1..b.size) {
            val cost = if (a[i - 1] == b[j - 1]) 0 else 1
            cur[j] = minOf(prev[j] + 1, cur[j - 1] + 1, prev[j - 1] + cost)
        }
        prev = cur
    }
    return prev[b.size]
}

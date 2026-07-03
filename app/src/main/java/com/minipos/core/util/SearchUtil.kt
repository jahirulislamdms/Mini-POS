package com.minipos.core.util

/**
 * Common search implementation used by every search box in the app (Phase 22).
 *
 * Matching is case-insensitive and **space-insensitive**: both the query and the searched text
 * are lowercased and stripped of whitespace, and the query is split into words — every word must
 * appear somewhere in one of the item's fields. So for "Samsung Note 14" / "Samsung Note14 5G",
 * the queries "Samsung", "Note", "Note14", "Note 14", "14", "5G", "sams" and "note1" all match
 * as expected, regardless of spacing or case.
 *
 * Results are ranked by relevance: matches that occur earlier in the text (e.g. name prefixes)
 * come first; ties keep their original order. Pure in-memory string work — fast on device scale.
 */
object SearchUtil {

    /** Lowercase and drop all whitespace, so "Note 14" and "note14" compare equal. */
    private fun norm(s: String): String = buildString(s.length) {
        for (c in s) if (!c.isWhitespace()) append(c.lowercaseChar())
    }

    private fun tokens(query: String): List<String> =
        query.trim().split(WHITESPACE).map(::norm).filter { it.isNotEmpty() }

    /** True when every word of [query] appears in at least one of the [fields]. */
    fun matches(query: String, vararg fields: String?): Boolean {
        val toks = tokens(query)
        if (toks.isEmpty()) return true
        val hays = fields.mapNotNull { it?.let(::norm) }
        if (hays.isEmpty()) return false
        return toks.all { t -> hays.any { it.contains(t) } }
    }

    /** Lower = more relevant: earliest position of the first query word across the fields. */
    private fun relevance(query: String, fields: List<String?>): Int {
        val first = tokens(query).firstOrNull() ?: return 0
        var best = Int.MAX_VALUE
        for (f in fields) {
            if (f == null) continue
            val idx = norm(f).indexOf(first)
            if (idx in 0 until best) best = idx
        }
        return best
    }

    /**
     * Filter [items] to those matching [query] (against the strings from [fields]) and rank the
     * most relevant first. A blank query returns the list unchanged.
     */
    fun <T> filter(items: List<T>, query: String, fields: (T) -> List<String?>): List<T> {
        if (query.isBlank()) return items
        return items
            .mapNotNull { item ->
                val f = fields(item)
                if (matches(query, *f.toTypedArray())) item to relevance(query, f) else null
            }
            .sortedBy { it.second } // stable: ties keep original order
            .map { it.first }
    }

    private val WHITESPACE = Regex("\\s+")
}

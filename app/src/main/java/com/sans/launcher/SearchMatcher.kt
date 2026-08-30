package com.sans.launcher

object SearchMatcher {
    /**
     * Checks if [app] matches the given [query] and returns a non-negative match score
     * (lower score = higher relevance / better match), or null if no match.
     */
    fun matchScore(app: AppModel, query: String): Int? {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return 0

        val displayName = app.displayName.lowercase()
        val q = trimmed.lowercase()

        // 1. Exact Match (Score: 0)
        if (displayName == q) return 0

        // 2. Starts With Display Name (Score: 10 + length difference)
        if (displayName.startsWith(q)) return 10 + (displayName.length - q.length)

        // 3. Word Prefix Match (e.g. "Studio" in "YouTube Studio" matches "stu") (Score: 30)
        val displayWords = displayName.split(Regex("[\\s\\-_]+"))
        if (displayWords.any { it.startsWith(q) }) {
            return 30
        }

        // 4. Acronym / Initials Match (e.g. "YouTube Studio" -> "ys" or "yt") (Score: 50)
        val displayAcronym = displayWords.mapNotNull { it.firstOrNull() }.joinToString("")
        if (displayAcronym.startsWith(q)) return 50

        // 5. Contains Substring (Score: 70)
        val subIndex = displayName.indexOf(q)
        if (subIndex >= 0) return 70 + subIndex

        return null
    }
}

package com.sans.launcher

object SearchMatcher {
    /**
     * Checks if [app] matches the given pre-normalized [query] (trimmed + lowercased)
     * and returns a non-negative match score (lower score = higher relevance / better match),
     * or null if no match.
     */
    fun matchScore(app: AppModel, query: String): Int? {
        if (query.isEmpty()) return 0

        val displayName = app.normalizedDisplayName

        // 1. Exact Match (Score: 0)
        if (displayName == query) return 0

        // 2. Starts With Display Name (Score: 10 + length difference)
        if (displayName.startsWith(query)) return 10 + (displayName.length - query.length)

        // 3. Word Prefix Match (e.g. "Studio" in "YouTube Studio" matches "stu") (Score: 30)
        val words = app.words
        for (i in 0 until words.size) {
            if (words[i].startsWith(query)) {
                return 30
            }
        }

        // 4. Acronym / Initials Match (e.g. "YouTube Studio" -> "ys" or "yt") (Score: 50)
        if (app.acronym.startsWith(query)) {
            return 50
        }

        // 5. Contains Substring (Score: 70)
        val subIndex = displayName.indexOf(query)
        if (subIndex >= 0) return 70 + subIndex

        return null
    }

    private val SETTINGS_KEYWORDS = listOf("settings", "preferences", "preference", "pref", "config", "launcher", "sans")

    /**
     * Returns true if [query] matches launcher preferences/settings keywords
     * (e.g. "settings", "preferences", "pref", "config", "launcher", "sans").
     */
    fun isSettingsQuery(query: String): Boolean {
        val trimmed = query.trim().lowercase()
        if (trimmed.length < 2) return false
        for (i in 0 until SETTINGS_KEYWORDS.size) {
            val keyword = SETTINGS_KEYWORDS[i]
            if (keyword.startsWith(trimmed) || (trimmed.length >= 3 && keyword.contains(trimmed))) {
                return true
            }
        }
        return false
    }
}

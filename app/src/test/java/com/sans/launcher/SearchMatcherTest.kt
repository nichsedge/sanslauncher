package com.sans.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchMatcherTest {

    private val youtube = AppModel(packageName = "com.google.android.youtube", label = "YouTube")
    private val youtubeStudio = AppModel(packageName = "com.google.android.apps.youtube.creator", label = "YouTube Studio")
    private val chrome = AppModel(packageName = "com.android.chrome", label = "Google Chrome")
    private val calculator = AppModel(packageName = "com.google.android.calculator", label = "Calculator")
    private val renamedInstagram = AppModel(
        packageName = "com.instagram.android",
        label = "Instagram",
        customLabel = "Dopamine Pit"
    )

    @Test
    fun exactMatchGivesBestScore() {
        val score = SearchMatcher.matchScore(youtube, "youtube")
        assertNotNull(score)
        assertTrue(score == 0)
    }

    @Test
    fun prefixMatchWorks() {
        val score = SearchMatcher.matchScore(calculator, "calc")
        assertNotNull(score)
    }

    @Test
    fun acronymMatchWorks() {
        // "ys" matches YouTube Studio
        val ysScore = SearchMatcher.matchScore(youtubeStudio, "ys")
        assertNotNull(ysScore)

        // "gc" matches Google Chrome
        val gcScore = SearchMatcher.matchScore(chrome, "gc")
        assertNotNull(gcScore)
    }

    @Test
    fun customLabelSearchWorks() {
        // Searching for custom name "dopamine" matches
        val customScore = SearchMatcher.matchScore(renamedInstagram, "dopamine")
        assertNotNull(customScore)

        // Searching for original name "instagram" should NOT match because the user renamed it
        val origScore = SearchMatcher.matchScore(renamedInstagram, "insta")
        assertNull(origScore)
    }

    @Test
    fun nonMatchingQueryReturnsNull() {
        val score = SearchMatcher.matchScore(calculator, "spotify")
        assertNull(score)
    }

    @Test
    fun teleMatchesTelegramButNotIntellect() {
        val telegram = AppModel(packageName = "org.telegram.messenger", label = "Telegram")
        val intellect = AppModel(packageName = "com.intellect.app", label = "Intellect")

        assertNotNull(SearchMatcher.matchScore(telegram, "tele"))
        assertNull(SearchMatcher.matchScore(intellect, "tele"))
    }

    @Test
    fun emptyQueryReturnsZero() {
        val score = SearchMatcher.matchScore(youtube, "")
        assertEquals(0, score)

        val whitespaceScore = SearchMatcher.matchScore(youtube, "   ")
        assertEquals(0, whitespaceScore)
    }

    @Test
    fun hyphenatedAndUnderscoreWordsMatchPrefixes() {
        val googleDrive = AppModel(packageName = "com.google.android.apps.docs", label = "Google-Drive")
        val myFiles = AppModel(packageName = "com.sec.android.app.myfiles", label = "My_Files")

        assertNotNull(SearchMatcher.matchScore(googleDrive, "drive"))
        assertNotNull(SearchMatcher.matchScore(myFiles, "files"))
    }
}

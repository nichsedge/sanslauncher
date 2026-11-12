package com.sans.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsRepositoryTest {

    @Test
    fun testEmptyLabelsReturnsEmptyMap() {
        val result = SettingsRepository.decodeLabels("")
        assertTrue(result.isEmpty())

        val blankResult = SettingsRepository.decodeLabels("   ")
        assertTrue(blankResult.isEmpty())
    }

    @Test
    fun testJsonEncodeAndDecodeRoundTrip() {
        val map = mapOf(
            "com.instagram.android" to "Dopamine Pit",
            "com.google.android.youtube" to "Watch Later (Never)",
            "org.telegram.messenger" to "Messages & Distractions"
        )
        val encoded = SettingsRepository.encodeLabels(map)
        val decoded = SettingsRepository.decodeLabels(encoded)

        assertEquals(map, decoded)
    }

    @Test
    fun testSpecialCharactersPreservedInJson() {
        val map = mapOf(
            "com.test.app" to "App with ::: and ;;; delimiters & quotes \"hello\" and emoji 🚀"
        )
        val encoded = SettingsRepository.encodeLabels(map)
        val decoded = SettingsRepository.decodeLabels(encoded)

        assertEquals(map, decoded)
        assertEquals("App with ::: and ;;; delimiters & quotes \"hello\" and emoji 🚀", decoded["com.test.app"])
    }

    @Test
    fun testLegacyDelimiterFormatFallback() {
        val legacyRaw = "com.instagram.android:::Dopamine Pit;;;com.twitter.android:::Time Sink"
        val decoded = SettingsRepository.decodeLabels(legacyRaw)

        assertEquals(2, decoded.size)
        assertEquals("Dopamine Pit", decoded["com.instagram.android"])
        assertEquals("Time Sink", decoded["com.twitter.android"])
    }
}

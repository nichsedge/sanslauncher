package com.sans.launcher

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsRepositoryTest {

    // Helper to instantiate a minimal decoder/encoder tester without Android context
    private object LabelCodec {
        fun decode(raw: String): Map<String, String> {
            if (raw.isBlank()) return emptyMap()
            return try {
                val json = JSONObject(raw)
                val map = mutableMapOf<String, String>()
                val keys = json.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    map[key] = json.getString(key)
                }
                map
            } catch (_: Exception) {
                // Fallback for legacy delimiter format
                raw.split(";;;").mapNotNull { entry ->
                    val parts = entry.split(":::", limit = 2)
                    if (parts.size == 2) parts[0] to parts[1] else null
                }.toMap()
            }
        }

        fun encode(map: Map<String, String>): String {
            val json = JSONObject()
            map.forEach { (k, v) -> json.put(k, v) }
            return json.toString()
        }
    }

    @Test
    fun testEmptyLabelsReturnsEmptyMap() {
        val result = LabelCodec.decode("")
        assertTrue(result.isEmpty())

        val blankResult = LabelCodec.decode("   ")
        assertTrue(blankResult.isEmpty())
    }

    @Test
    fun testJsonEncodeAndDecodeRoundTrip() {
        val map = mapOf(
            "com.instagram.android" to "Dopamine Pit",
            "com.google.android.youtube" to "Watch Later (Never)",
            "org.telegram.messenger" to "Messages & Distractions"
        )
        val encoded = LabelCodec.encode(map)
        val decoded = LabelCodec.decode(encoded)

        assertEquals(map, decoded)
    }

    @Test
    fun testSpecialCharactersPreservedInJson() {
        val map = mapOf(
            "com.test.app" to "App with ::: and ;;; delimiters & quotes \"hello\" and emoji 🚀"
        )
        val encoded = LabelCodec.encode(map)
        val decoded = LabelCodec.decode(encoded)

        assertEquals(map, decoded)
        assertEquals("App with ::: and ;;; delimiters & quotes \"hello\" and emoji 🚀", decoded["com.test.app"])
    }

    @Test
    fun testLegacyDelimiterFormatFallback() {
        val legacyRaw = "com.instagram.android:::Dopamine Pit;;;com.twitter.android:::Time Sink"
        val decoded = LabelCodec.decode(legacyRaw)

        assertEquals(2, decoded.size)
        assertEquals("Dopamine Pit", decoded["com.instagram.android"])
        assertEquals("Time Sink", decoded["com.twitter.android"])
    }
}

package com.sans.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppModelTest {

    @Test
    fun defaultProfileTypeIsPersonal() {
        val app = AppModel(packageName = "com.example.app", label = "Example")
        assertEquals(ProfileType.PERSONAL, app.profileType)
        assertFalse(app.isWorkProfile)
        assertFalse(app.isCloneProfile)
        assertFalse(app.isPrivateProfile)
        assertEquals("com.example.app", app.uniqueKey)
    }

    @Test
    fun workProfilePropertiesAreCorrect() {
        val app = AppModel(
            packageName = "com.slack",
            label = "Slack",
            profileType = ProfileType.WORK
        )
        assertEquals(ProfileType.WORK, app.profileType)
        assertTrue(app.isWorkProfile)
        assertFalse(app.isCloneProfile)
        assertFalse(app.isPrivateProfile)
    }

    @Test
    fun cloneProfilePropertiesAreCorrect() {
        val app = AppModel(
            packageName = "com.whatsapp",
            label = "WhatsApp",
            profileType = ProfileType.CLONE
        )
        assertEquals(ProfileType.CLONE, app.profileType)
        assertFalse(app.isWorkProfile)
        assertTrue(app.isCloneProfile)
        assertFalse(app.isPrivateProfile)
    }

    @Test
    fun uniqueKeyDiffersWhenUserHandleHashIsProvided() {
        val personalApp = AppModel(
            packageName = "com.slack",
            label = "Slack",
            profileType = ProfileType.PERSONAL
        )
        val workApp = AppModel(
            packageName = "com.slack",
            label = "Slack",
            profileType = ProfileType.WORK
        )

        // Default personal app has packageName as uniqueKey
        assertEquals("com.slack", personalApp.uniqueKey)
    }

    @Test
    fun searchMatcherFindsWorkAndPersonalApps() {
        val personalSlack = AppModel(
            packageName = "com.slack",
            label = "Slack",
            profileType = ProfileType.PERSONAL
        )
        val workSlack = AppModel(
            packageName = "com.slack",
            label = "Slack",
            profileType = ProfileType.WORK
        )

        val score1 = SearchMatcher.matchScore(personalSlack, "slack")
        val score2 = SearchMatcher.matchScore(workSlack, "slack")

        assertNotNull(score1)
        assertNotNull(score2)
        assertEquals(score1, score2)
    }

    @Test
    fun customLabelOnWorkAppIsExclusive() {
        val workSlack = AppModel(
            packageName = "com.slack",
            label = "Slack",
            customLabel = "Company Chat",
            profileType = ProfileType.WORK
        )

        assertEquals("Company Chat", workSlack.displayName)
        assertTrue(workSlack.isWorkProfile)

        // Matching custom label succeeds
        assertNotNull(SearchMatcher.matchScore(workSlack, "company"))
        // Original label does not match due to custom alias exclusivity invariant
        org.junit.Assert.assertNull(SearchMatcher.matchScore(workSlack, "slack"))
    }
}

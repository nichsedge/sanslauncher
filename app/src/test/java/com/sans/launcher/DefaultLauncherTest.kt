package com.sans.launcher

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultLauncherTest {

    @Test
    fun testHomeUiStateDefaultLauncherFlag() {
        val defaultState = HomeUiState()
        assertFalse(defaultState.isDefaultLauncher)

        val activeState = defaultState.copy(isDefaultLauncher = true)
        assertTrue(activeState.isDefaultLauncher)
    }
}

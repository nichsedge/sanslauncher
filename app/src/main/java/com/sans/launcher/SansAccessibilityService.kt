package com.sans.launcher

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent

class SansAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // No event listening required - service only used for global actions
    }

    override fun onInterrupt() { }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
        }
    }

    companion object {
        private var instance: SansAccessibilityService? = null

        fun isServiceRunning(): Boolean = instance != null

        fun lockScreen(): Boolean {
            val service = instance ?: return false
            return service.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
        }

        fun expandNotificationShade(): Boolean {
            val service = instance ?: return false
            return service.performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
        }

        fun openAccessibilitySettings(context: Context) {
            try {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (_: Exception) { }
        }
    }
}

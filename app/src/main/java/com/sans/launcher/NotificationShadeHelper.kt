package com.sans.launcher

import android.content.Context

object NotificationShadeHelper {
    fun expandNotificationShade(context: Context) {
        try {
            val statusBarService = context.getSystemService(Context.STATUS_BAR_SERVICE)
            val statusBarManager = Class.forName("android.app.StatusBarManager")
            val expandMethod = statusBarManager.getMethod("expandNotificationsPanel")
            expandMethod.invoke(statusBarService)
        } catch (_: Exception) {
            try {
                // Alternative fallback
                val statusBarService = context.getSystemService("statusbar")
                val statusBarManager = Class.forName("android.app.StatusBarManager")
                val expandMethod = statusBarManager.getMethod("expand")
                expandMethod.invoke(statusBarService)
            } catch (_: Exception) { }
        }
    }
}

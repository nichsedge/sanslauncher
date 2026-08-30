package com.sans.launcher

import android.annotation.SuppressLint
import android.content.Context

object NotificationShadeHelper {
    @SuppressLint("WrongConstant")
    fun expandNotificationShade(context: Context) {
        try {
            val statusBarService = context.getSystemService("statusbar")
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

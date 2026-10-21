package com.sans.launcher

import android.content.Context

object NotificationShadeHelper {
    private val expandMethod by lazy {
        try {
            val statusBarManager = Class.forName("android.app.StatusBarManager")
            try {
                statusBarManager.getMethod("expandNotificationsPanel")
            } catch (_: Exception) {
                statusBarManager.getMethod("expand")
            }
        } catch (_: Exception) {
            null
        }
    }

    private val collapseMethod by lazy {
        try {
            val statusBarManager = Class.forName("android.app.StatusBarManager")
            try {
                statusBarManager.getMethod("collapsePanels")
            } catch (_: Exception) {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    fun expandNotificationShade(context: Context) {
        try {
            val statusBarService = context.getSystemService(Context.STATUS_BAR_SERVICE)
                ?: context.getSystemService("statusbar")
            expandMethod?.invoke(statusBarService)
        } catch (_: Exception) { }
    }

    fun collapseNotificationShade(context: Context) {
        try {
            val statusBarService = context.getSystemService(Context.STATUS_BAR_SERVICE)
                ?: context.getSystemService("statusbar")
            collapseMethod?.invoke(statusBarService)
        } catch (_: Exception) { }
    }
}

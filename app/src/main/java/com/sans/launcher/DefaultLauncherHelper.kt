package com.sans.launcher

import android.app.Activity
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings

object DefaultLauncherHelper {

    fun isDefaultLauncher(context: Context): Boolean {
        val roleManager = context.getSystemService(RoleManager::class.java)
        if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
            return roleManager.isRoleHeld(RoleManager.ROLE_HOME)
        }

        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
        }
        val resolveInfo = context.packageManager.resolveActivity(
            homeIntent,
            PackageManager.MATCH_DEFAULT_ONLY
        )
        return resolveInfo?.activityInfo?.packageName == context.packageName
    }

    fun createSetDefaultLauncherIntent(context: Context): Intent {
        val roleManager = context.getSystemService(RoleManager::class.java)
        if (roleManager != null &&
            roleManager.isRoleAvailable(RoleManager.ROLE_HOME) &&
            !roleManager.isRoleHeld(RoleManager.ROLE_HOME)
        ) {
            return roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
        }

        val manageDefaultApps = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
        if (manageDefaultApps.resolveActivity(context.packageManager) != null) {
            return manageDefaultApps
        }

        val homeSettings = Intent(Settings.ACTION_HOME_SETTINGS)
        if (homeSettings.resolveActivity(context.packageManager) != null) {
            return homeSettings
        }

        return Intent(Settings.ACTION_SETTINGS)
    }

    fun launchSetDefaultLauncher(context: Context) {
        val intent = createSetDefaultLauncherIntent(context).apply {
            if (context !is Activity) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }

        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                val fallbackIntent = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS).apply {
                    if (context !is Activity) {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                }
                context.startActivity(fallbackIntent)
            } catch (_: Exception) {
                val generalSettings = Intent(Settings.ACTION_SETTINGS).apply {
                    if (context !is Activity) {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                }
                context.startActivity(generalSettings)
            }
        }
    }
}

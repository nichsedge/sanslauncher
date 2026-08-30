package com.sans.launcher

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import android.provider.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn

class AppRepository(private val context: Context) {

    private val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps
    private val userManager = context.getSystemService(Context.USER_SERVICE) as? UserManager

    fun getInstalledApps(): Flow<List<AppModel>> = callbackFlow {
        fun queryApps(): List<AppModel> {
            val apps = mutableListOf<AppModel>()
            val profiles = userManager?.userProfiles ?: listOf(Process.myUserHandle())

            if (launcherApps != null) {
                for (profile in profiles) {
                    try {
                        val activityList: List<LauncherActivityInfo> = launcherApps.getActivityList(null, profile)
                        for (info in activityList) {
                            val pkg = info.applicationInfo.packageName
                            val label = info.label?.toString() ?: pkg
                            if (pkg.isNotBlank() && pkg != context.packageName) {
                                apps.add(
                                    AppModel(
                                        packageName = pkg,
                                        label = label,
                                        userHandle = profile,
                                        activityName = info.componentName.className
                                    )
                                )
                            }
                        }
                    } catch (_: Exception) { }
                }
            } else {
                // Fallback to PackageManager
                val pm = context.packageManager
                val intent = Intent(Intent.ACTION_MAIN, null).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                }
                val resolveInfos = pm.queryIntentActivities(
                    intent,
                    PackageManager.ResolveInfoFlags.of(0)
                )
                for (resolveInfo in resolveInfos) {
                    val pkg = resolveInfo.activityInfo.packageName
                    val label = resolveInfo.loadLabel(pm).toString()
                    if (pkg.isNotBlank() && pkg != context.packageName) {
                        apps.add(AppModel(packageName = pkg, label = label))
                    }
                }
            }

            return apps
                .distinctBy { it.uniqueKey }
                .sortedBy { it.displayName.lowercase() }
        }

        trySend(queryApps())

        val callback = object : LauncherApps.Callback() {
            override fun onPackageRemoved(packageName: String?, user: UserHandle?) {
                trySend(queryApps())
            }

            override fun onPackageAdded(packageName: String?, user: UserHandle?) {
                trySend(queryApps())
            }

            override fun onPackageChanged(packageName: String?, user: UserHandle?) {
                trySend(queryApps())
            }

            override fun onPackagesAvailable(packageNames: Array<out String>?, user: UserHandle?, replacing: Boolean) {
                trySend(queryApps())
            }

            override fun onPackagesUnavailable(packageNames: Array<out String>?, user: UserHandle?, replacing: Boolean) {
                trySend(queryApps())
            }

            override fun onPackagesSuspended(packageNames: Array<out String>?, user: UserHandle?) {
                trySend(queryApps())
            }

            override fun onPackagesUnsuspended(packageNames: Array<out String>?, user: UserHandle?) {
                trySend(queryApps())
            }
        }

        val handler = Handler(Looper.getMainLooper())
        launcherApps?.registerCallback(callback, handler)

        awaitClose {
            try {
                launcherApps?.unregisterCallback(callback)
            } catch (_: Exception) { }
        }
    }.flowOn(Dispatchers.IO)

    fun launchApp(
        packageName: String,
        userHandle: UserHandle? = null,
        activityName: String? = null
    ): Boolean {
        return try {
            if (launcherApps != null && userHandle != null && activityName != null) {
                val component = ComponentName(packageName, activityName)
                launcherApps.startMainActivity(component, userHandle, null, null)
                true
            } else {
                val intent = context.packageManager.getLaunchIntentForPackage(packageName)?.apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                if (intent != null) {
                    context.startActivity(intent)
                    true
                } else {
                    false
                }
            }
        } catch (_: Exception) {
            false
        }
    }

    fun launchApp(app: AppModel): Boolean {
        return launchApp(app.packageName, app.userHandle, app.activityName)
    }

    fun openAppInfo(packageName: String, userHandle: UserHandle? = null, activityName: String? = null) {
        try {
            if (launcherApps != null && userHandle != null && activityName != null) {
                val component = ComponentName(packageName, activityName)
                launcherApps.startAppDetailsActivity(component, userHandle, null, null)
            } else {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:$packageName")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
        } catch (_: Exception) { }
    }

    fun uninstallApp(packageName: String) {
        try {
            val intent = Intent(Intent.ACTION_DELETE).apply {
                data = Uri.parse("package:$packageName")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) { }
    }
}

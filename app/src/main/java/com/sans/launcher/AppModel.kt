package com.sans.launcher

import android.os.UserHandle

data class AppModel(
    val packageName: String,
    val label: String,
    val customLabel: String? = null,
    val isHidden: Boolean = false,
    val isFavorite: Boolean = false,
    val userHandle: UserHandle? = null,
    val activityName: String? = null
) {
    val displayName: String
        get() = customLabel?.takeIf { it.isNotBlank() } ?: label

    val uniqueKey: String
        get() = if (userHandle != null && userHandle.hashCode() != 0) {
            "${packageName}_${userHandle.hashCode()}"
        } else {
            packageName
        }
}

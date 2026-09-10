package com.sans.launcher

import android.os.UserHandle
import androidx.compose.runtime.Immutable

enum class ProfileType {
    PERSONAL,
    WORK,
    CLONE,
    PRIVATE
}

@Immutable
data class AppModel(
    val packageName: String,
    val label: String,
    val customLabel: String? = null,
    val isHidden: Boolean = false,
    val isFavorite: Boolean = false,
    val userHandle: UserHandle? = null,
    val activityName: String? = null,
    val profileType: ProfileType = ProfileType.PERSONAL
) {
    val isWorkProfile: Boolean get() = profileType == ProfileType.WORK
    val isCloneProfile: Boolean get() = profileType == ProfileType.CLONE
    val isPrivateProfile: Boolean get() = profileType == ProfileType.PRIVATE

    val displayName: String = customLabel?.takeIf { it.isNotBlank() } ?: label

    val normalizedDisplayName: String by lazy { displayName.lowercase() }

    val words: List<String> by lazy { displayName.split(WORD_SPLIT_REGEX).map { it.lowercase() } }

    val acronym: String by lazy { words.mapNotNull { it.firstOrNull() }.joinToString("") }

    val uniqueKey: String = if (userHandle != null && userHandle.hashCode() != 0) {
        "${packageName}_${userHandle.hashCode()}"
    } else {
        packageName
    }

    companion object {
        private val WORD_SPLIT_REGEX = Regex("[\\s\\-_]+")
    }
}

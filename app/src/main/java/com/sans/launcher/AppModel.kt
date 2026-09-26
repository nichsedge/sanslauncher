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

    val normalizedDisplayName: String = displayName.lowercase()

    val words: List<String> = computeWords(normalizedDisplayName)

    val acronym: String = computeAcronym(words)

    val uniqueKey: String = if (userHandle != null && userHandle.hashCode() != 0) {
        "${packageName}_${userHandle.hashCode()}"
    } else {
        packageName
    }

    companion object {
        fun computeWords(normalizedDisplayName: String): List<String> {
            val list = mutableListOf<String>()
            val sb = StringBuilder()
            for (ch in normalizedDisplayName) {
                if (ch.isWhitespace() || ch == '-' || ch == '_') {
                    if (sb.isNotEmpty()) {
                        list.add(sb.toString())
                        sb.setLength(0)
                    }
                } else {
                    sb.append(ch)
                }
            }
            if (sb.isNotEmpty()) {
                list.add(sb.toString())
            }
            return list
        }

        fun computeAcronym(words: List<String>): String {
            val sb = StringBuilder(words.size)
            for (w in words) {
                if (w.isNotEmpty()) {
                    sb.append(w[0])
                }
            }
            return sb.toString()
        }
    }
}

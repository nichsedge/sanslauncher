package com.sans.launcher

data class AppModel(
    val packageName: String,
    val label: String,
    val customLabel: String? = null,
    val isHidden: Boolean = false,
    val isFavorite: Boolean = false
) {
    val displayName: String
        get() = customLabel?.takeIf { it.isNotBlank() } ?: label
}

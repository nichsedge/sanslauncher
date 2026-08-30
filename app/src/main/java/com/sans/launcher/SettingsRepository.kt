package com.sans.launcher

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

import org.json.JSONObject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private val favoritesKey = stringSetPreferencesKey("favorites")
    private val hiddenKey = stringSetPreferencesKey("hidden")
    private val customLabelsKey = stringPreferencesKey("custom_labels_v1")
    private val showTimeAnchorKey = booleanPreferencesKey("show_time_anchor")
    private val showDayProgressKey = booleanPreferencesKey("show_day_progress")
    private val themeModeKey = stringPreferencesKey("theme_mode")
    private val fontStyleKey = stringPreferencesKey("font_style")
    private val enableHapticsKey = booleanPreferencesKey("enable_haptics")
    private val autoOpenKeyboardKey = booleanPreferencesKey("auto_open_keyboard")
    private val showNextEventKey = booleanPreferencesKey("show_next_event")
    private val doubleTapToLockKey = booleanPreferencesKey("double_tap_to_lock")
    private val scratchpadTextKey = stringPreferencesKey("scratchpad_text")

    val favorites: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        preferences[favoritesKey] ?: emptySet()
    }

    val hiddenApps: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        preferences[hiddenKey] ?: emptySet()
    }

    val customLabels: Flow<Map<String, String>> = context.dataStore.data.map { preferences ->
        val raw = preferences[customLabelsKey] ?: ""
        decodeLabels(raw)
    }

    val showTimeAnchor: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[showTimeAnchorKey] ?: true
    }

    val showDayProgress: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[showDayProgressKey] ?: true
    }

    val themeMode: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[themeModeKey] ?: "amoled"
    }

    val fontStyle: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[fontStyleKey] ?: "sans"
    }

    val enableHaptics: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[enableHapticsKey] ?: true
    }

    val autoOpenKeyboard: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[autoOpenKeyboardKey] ?: false
    }

    val showNextEvent: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[showNextEventKey] ?: true
    }

    val doubleTapToLock: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[doubleTapToLockKey] ?: true
    }

    val scratchpadText: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[scratchpadTextKey] ?: ""
    }

    suspend fun setCustomLabel(packageName: String, customLabel: String?) {
        context.dataStore.edit { preferences ->
            val currentMap = decodeLabels(preferences[customLabelsKey] ?: "").toMutableMap()
            if (customLabel.isNullOrBlank()) {
                currentMap.remove(packageName)
            } else {
                currentMap[packageName] = customLabel.trim()
            }
            preferences[customLabelsKey] = encodeLabels(currentMap)
        }
    }

    suspend fun toggleFavorite(packageName: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[favoritesKey] ?: emptySet()
            if (packageName in current) {
                preferences[favoritesKey] = current - packageName
            } else {
                preferences[favoritesKey] = current + packageName
            }
        }
    }

    suspend fun toggleHidden(packageName: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[hiddenKey] ?: emptySet()
            if (packageName in current) {
                preferences[hiddenKey] = current - packageName
            } else {
                preferences[hiddenKey] = current + packageName
            }
        }
    }

    suspend fun setShowTimeAnchor(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[showTimeAnchorKey] = enabled
        }
    }

    suspend fun setShowDayProgress(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[showDayProgressKey] = enabled
        }
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[themeModeKey] = mode
        }
    }

    suspend fun setFontStyle(style: String) {
        context.dataStore.edit { preferences ->
            preferences[fontStyleKey] = style
        }
    }

    suspend fun setEnableHaptics(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[enableHapticsKey] = enabled
        }
    }

    suspend fun setAutoOpenKeyboard(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[autoOpenKeyboardKey] = enabled
        }
    }

    suspend fun setShowNextEvent(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[showNextEventKey] = enabled
        }
    }

    suspend fun setDoubleTapToLock(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[doubleTapToLockKey] = enabled
        }
    }

    suspend fun saveScratchpadText(text: String) {
        context.dataStore.edit { preferences ->
            preferences[scratchpadTextKey] = text
        }
    }

    internal fun decodeLabels(raw: String): Map<String, String> {
        if (raw.isBlank()) return emptyMap()
        return try {
            val json = JSONObject(raw)
            val map = mutableMapOf<String, String>()
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                map[key] = json.getString(key)
            }
            map
        } catch (_: Exception) {
            // Fallback for legacy delimiter format
            raw.split(";;;").mapNotNull { entry ->
                val parts = entry.split(":::", limit = 2)
                if (parts.size == 2) parts[0] to parts[1] else null
            }.toMap()
        }
    }

    internal fun encodeLabels(map: Map<String, String>): String {
        val json = JSONObject()
        map.forEach { (k, v) -> json.put(k, v) }
        return json.toString()
    }
}

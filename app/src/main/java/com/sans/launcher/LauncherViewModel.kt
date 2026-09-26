package com.sans.launcher

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import androidx.compose.runtime.Immutable
import kotlinx.coroutines.flow.map

@Immutable
data class HomeUiState(
    val searchQuery: String = "",
    val favoriteApps: List<AppModel> = emptyList(),
    val otherApps: List<AppModel> = emptyList(),
    val allApps: List<AppModel> = emptyList(),
    val selectedAppForMenu: AppModel? = null,
    val appToRename: AppModel? = null,
    val nextEvent: CalendarEventModel? = null,
    val showTimeAnchor: Boolean = true,
    val showNextEvent: Boolean = true,
    val autoOpenKeyboard: Boolean = false,
    val showExpenseShortcut: Boolean = true,
    val themeMode: String = "dark",
    val fontStyle: String = "sans",
    val enableHaptics: Boolean = true,
    val isLoading: Boolean = true
)

@Immutable
data class PartitionedApps(
    val allApps: List<AppModel> = emptyList(),
    val favorites: List<AppModel> = emptyList(),
    val others: List<AppModel> = emptyList(),
    val visibleApps: List<AppModel> = emptyList()
)

@OptIn(FlowPreview::class)
class LauncherViewModel(
    private val appRepository: AppRepository,
    private val settingsRepository: SettingsRepository,
    private val calendarRepository: CalendarRepository,
    context: Context
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _selectedAppForMenu = MutableStateFlow<AppModel?>(null)
    private val _appToRename = MutableStateFlow<AppModel?>(null)

    val timeState: StateFlow<TimeState> = TimeAnchorHelper.timeStateFlow(context)
        .stateIn(viewModelScope, SharingStarted.Lazily, TimeAnchorHelper.calculateTimeState())

    val partitionedAppsFlow: StateFlow<PartitionedApps> = combine(
        appRepository.getInstalledApps(),
        settingsRepository.favorites,
        settingsRepository.hiddenApps,
        settingsRepository.customLabels
    ) { apps, favorites, hidden, labels ->
        val updatedApps = apps.map { app ->
            app.copy(
                isFavorite = app.packageName in favorites || app.uniqueKey in favorites,
                isHidden = app.packageName in hidden || app.uniqueKey in hidden,
                customLabel = labels[app.uniqueKey] ?: labels[app.packageName]
            )
        }
        val visible = updatedApps.filter { !it.isHidden }
        val favs = visible.filter { it.isFavorite }.sortedBy { it.normalizedDisplayName }
        val others = visible.filter { !it.isFavorite }.sortedBy { it.normalizedDisplayName }
        PartitionedApps(
            allApps = updatedApps,
            favorites = favs,
            others = others,
            visibleApps = visible
        )
    }.stateIn(viewModelScope, SharingStarted.Lazily, PartitionedApps())

    val allAppsFlow: StateFlow<List<AppModel>> = partitionedAppsFlow
        .map { it.allApps }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    /** Lightweight flow for theme/font only — used by MainActivity to avoid collecting full uiState. */
    val themeState: StateFlow<Pair<String, String>> = combine(
        settingsRepository.themeMode,
        settingsRepository.fontStyle
    ) { theme, font -> theme to font }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "dark" to "sans")

    private val preferencesFlow: Flow<PreferencesBundle> = combine(
        settingsRepository.showTimeAnchor,
        settingsRepository.themeMode,
        settingsRepository.fontStyle,
        settingsRepository.enableHaptics
    ) { timeAnchor, theme, font, haptics ->
        PreferencesBundle(timeAnchor, theme, font, haptics)
    }

    private val extendedPrefsFlow = combine(
        settingsRepository.autoOpenKeyboard,
        settingsRepository.showNextEvent,
        settingsRepository.showExpenseShortcut,
        calendarRepository.getNextUpcomingEventFlow()
    ) { autoKeyboard, showNextEvent, showExpenseShortcut, nextEvent ->
        ExtendedPrefs(autoKeyboard, showNextEvent, showExpenseShortcut, nextEvent)
    }

    private val dialogStatesFlow = combine(_selectedAppForMenu, _appToRename) { menu, rename ->
        menu to rename
    }

    data class SearchResult(
        val query: String = "",
        val favoriteApps: List<AppModel> = emptyList(),
        val otherApps: List<AppModel> = emptyList()
    )

    private val searchResultFlow: StateFlow<SearchResult> = combine(
        partitionedAppsFlow,
        _searchQuery
    ) { partitioned, query ->
        if (query.isEmpty()) {
            SearchResult(
                query = "",
                favoriteApps = partitioned.favorites,
                otherApps = partitioned.others
            )
        } else {
            val normalizedQuery = query.trim().lowercase()
            val scored = ArrayList<Pair<AppModel, Int>>()
            for (app in partitioned.visibleApps) {
                val score = SearchMatcher.matchScore(app, normalizedQuery)
                if (score != null) {
                    scored.add(app to score)
                }
            }
            scored.sortBy { it.second }
            val matched = ArrayList<AppModel>(scored.size)
            for (i in 0 until scored.size) {
                matched.add(scored[i].first)
            }

            // Instant Auto-Launch: exactly 1 match → launch immediately
            if (matched.size == 1 && !SearchMatcher.isSettingsQuery(query.trim())) {
                viewModelScope.launch { launchApp(matched.first()) }
            }

            SearchResult(
                query = query,
                favoriteApps = emptyList(),
                otherApps = matched
            )
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, SearchResult())

    val uiState: StateFlow<HomeUiState> = combine(
        partitionedAppsFlow,
        searchResultFlow,
        dialogStatesFlow,
        combine(preferencesFlow, extendedPrefsFlow) { p, ep -> p to ep }
    ) { partitioned, searchResult, (selectedMenuApp, renamingApp), (prefs, ext) ->
        val nextEv = if (ext.showNextEvent) ext.nextEvent else null
        val apps = partitioned.allApps

        HomeUiState(
            searchQuery = searchResult.query,
            favoriteApps = searchResult.favoriteApps,
            otherApps = searchResult.otherApps,
            allApps = apps,
            selectedAppForMenu = selectedMenuApp,
            appToRename = renamingApp,
            nextEvent = nextEv,
            showTimeAnchor = prefs.showTimeAnchor,
            showNextEvent = ext.showNextEvent,
            autoOpenKeyboard = ext.autoOpenKeyboard,
            showExpenseShortcut = ext.showExpenseShortcut,
            themeMode = prefs.themeMode,
            fontStyle = prefs.fontStyle,
            enableHaptics = prefs.enableHaptics,
            isLoading = apps.isEmpty()
        )
    }.stateIn(viewModelScope, SharingStarted.Lazily, HomeUiState())

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun launchApp(app: AppModel) {
        appRepository.launchApp(app)
        _searchQuery.value = ""
    }

    fun openAppMenu(app: AppModel) {
        _selectedAppForMenu.value = app
    }

    fun dismissAppMenu() {
        _selectedAppForMenu.value = null
    }

    fun startRenamingApp(app: AppModel) {
        _selectedAppForMenu.value = null
        _appToRename.value = app
    }

    fun dismissRenameDialog() {
        _appToRename.value = null
    }

    fun saveCustomLabel(packageName: String, newLabel: String?) {
        viewModelScope.launch {
            settingsRepository.setCustomLabel(packageName, newLabel)
            _appToRename.value = null
        }
    }

    fun toggleFavorite(appKey: String, legacyKey: String? = null) {
        viewModelScope.launch {
            settingsRepository.toggleFavorite(appKey, legacyKey)
            _selectedAppForMenu.value = null
        }
    }

    fun toggleHidden(appKey: String, legacyKey: String? = null) {
        viewModelScope.launch {
            settingsRepository.toggleHidden(appKey, legacyKey)
            _selectedAppForMenu.value = null
        }
    }

    fun openAppInfo(app: AppModel) {
        appRepository.openAppInfo(app.packageName, app.userHandle, app.activityName)
        _selectedAppForMenu.value = null
    }

    fun uninstallApp(packageName: String) {
        appRepository.uninstallApp(packageName)
        _selectedAppForMenu.value = null
    }

    fun setShowTimeAnchor(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setShowTimeAnchor(enabled) }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    fun setFontStyle(style: String) {
        viewModelScope.launch { settingsRepository.setFontStyle(style) }
    }

    fun setEnableHaptics(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setEnableHaptics(enabled) }
    }

    fun setAutoOpenKeyboard(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setAutoOpenKeyboard(enabled) }
    }

    fun setShowNextEvent(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setShowNextEvent(enabled) }
    }

    fun setShowExpenseShortcut(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setShowExpenseShortcut(enabled) }
    }

    fun recordExpense(query: String? = null) {
        appRepository.launchRecordExpense(query)
        _searchQuery.value = ""
    }

    fun refreshCalendarEvents() {
        calendarRepository.refresh()
    }

    fun openCalendarApp(startTimeMillis: Long? = null) {
        calendarRepository.openCalendarApp(startTimeMillis)
    }

    private data class PreferencesBundle(
        val showTimeAnchor: Boolean,
        val themeMode: String,
        val fontStyle: String,
        val enableHaptics: Boolean
    )

    private data class ExtendedPrefs(
        val autoOpenKeyboard: Boolean,
        val showNextEvent: Boolean,
        val showExpenseShortcut: Boolean,
        val nextEvent: CalendarEventModel?
    )
}


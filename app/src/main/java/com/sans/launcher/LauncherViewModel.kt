package com.sans.launcher

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Immutable
data class HomeUiState(
    val searchQuery: String = "",
    val favoriteApps: List<AppModel> = emptyList(),
    val otherApps: List<AppModel> = emptyList(),
    val selectedAppForMenu: AppModel? = null,
    val appToRename: AppModel? = null,
    val showTimeAnchor: Boolean = true,
    val showNextEvent: Boolean = true,
    val autoOpenKeyboard: Boolean = false,
    val showExpenseShortcut: Boolean = true,
    val showCircleToSearchShortcut: Boolean = true,
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

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class LauncherViewModel(
    private val appRepository: AppRepository,
    private val settingsRepository: SettingsRepository,
    private val calendarRepository: CalendarRepository,
    context: Context
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _selectedAppForMenu = MutableStateFlow<AppModel?>(null)
    private val _appToRename = MutableStateFlow<AppModel?>(null)

    val timeState: StateFlow<TimeState> = settingsRepository.showTimeAnchor
        .flatMapLatest { show ->
            if (show) {
                TimeAnchorHelper.timeStateFlow(context)
            } else {
                flowOf(TimeAnchorHelper.calculateTimeState())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TimeAnchorHelper.calculateTimeState())

    val nextEventState: StateFlow<CalendarEventModel?> = settingsRepository.showNextEvent
        .flatMapLatest { show ->
            if (show) {
                calendarRepository.getNextUpcomingEventFlow()
            } else {
                flowOf(null)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

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
        settingsRepository.showCircleToSearchShortcut
    ) { autoKeyboard, showNextEvent, showExpenseShortcut, showCtsShortcut ->
        ExtendedPrefs(autoKeyboard, showNextEvent, showExpenseShortcut, showCtsShortcut)
    }

    private val dialogStatesFlow = combine(_selectedAppForMenu, _appToRename) { menu, rename ->
        menu to rename
    }

    data class SearchResult(
        val query: String = "",
        val favoriteApps: List<AppModel> = emptyList(),
        val otherApps: List<AppModel> = emptyList()
    )

    private class ScoredApp(
        val app: AppModel,
        val score: Int
    ) : Comparable<ScoredApp> {
        override fun compareTo(other: ScoredApp): Int = score.compareTo(other.score)
    }

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
            val visibleApps = partitioned.visibleApps
            val scored = ArrayList<ScoredApp>(visibleApps.size)
            for (i in 0 until visibleApps.size) {
                val app = visibleApps[i]
                val score = SearchMatcher.matchScore(app, normalizedQuery)
                if (score != null) {
                    scored.add(ScoredApp(app, score))
                }
            }
            scored.sort()
            val matched = ArrayList<AppModel>(scored.size)
            for (i in 0 until scored.size) {
                matched.add(scored[i].app)
            }

            SearchResult(
                query = query,
                favoriteApps = emptyList(),
                otherApps = matched
            )
        }
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Lazily, SearchResult())

    init {
        viewModelScope.launch {
            searchResultFlow.collect { result ->
                if (result.query.isNotEmpty() &&
                    result.otherApps.size == 1 &&
                    !SearchMatcher.isSettingsQuery(result.query.trim())
                ) {
                    launchApp(result.otherApps.first())
                }
            }
        }
    }

    val uiState: StateFlow<HomeUiState> = combine(
        partitionedAppsFlow,
        searchResultFlow,
        dialogStatesFlow,
        combine(preferencesFlow, extendedPrefsFlow) { p, ep -> p to ep }
    ) { partitioned, searchResult, (selectedMenuApp, renamingApp), (prefs, ext) ->
        val apps = partitioned.allApps

        HomeUiState(
            searchQuery = searchResult.query,
            favoriteApps = searchResult.favoriteApps,
            otherApps = searchResult.otherApps,
            selectedAppForMenu = selectedMenuApp,
            appToRename = renamingApp,
            showTimeAnchor = prefs.showTimeAnchor,
            showNextEvent = ext.showNextEvent,
            autoOpenKeyboard = ext.autoOpenKeyboard,
            showExpenseShortcut = ext.showExpenseShortcut,
            showCircleToSearchShortcut = ext.showCircleToSearchShortcut,
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

    fun uninstallApp(app: AppModel) {
        appRepository.uninstallApp(app.packageName, app.userHandle)
        _selectedAppForMenu.value = null
    }

    fun uninstallApp(packageName: String, userHandle: android.os.UserHandle? = null) {
        appRepository.uninstallApp(packageName, userHandle)
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

    fun setShowCircleToSearchShortcut(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setShowCircleToSearchShortcut(enabled) }
    }

    fun triggerCircleToSearch(context: Context) {
        CircleToSearchHelper.launch(context)
    }

    fun requestAddQuickSettingsTile(context: Context) {
        val statusBarManager = context.getSystemService(android.app.StatusBarManager::class.java)
        statusBarManager?.requestAddTileService(
            android.content.ComponentName(context, CircleToSearchTileService::class.java),
            context.getString(R.string.circle_to_search_title),
            android.graphics.drawable.Icon.createWithResource(context, R.drawable.ic_search),
            context.mainExecutor
        ) { _ -> }
    }

    fun recordExpense(query: String? = null) {
        appRepository.launchRecordExpense(query)
        _searchQuery.value = ""
    }

    fun refreshCalendarEvents(force: Boolean = false) {
        if (uiState.value.showNextEvent) {
            calendarRepository.refresh(force)
        }
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
        val showCircleToSearchShortcut: Boolean
    )
}


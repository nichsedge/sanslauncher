package com.sans.launcher

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val searchQuery: String = "",
    val favoriteApps: List<AppModel> = emptyList(),
    val otherApps: List<AppModel> = emptyList(),
    val allApps: List<AppModel> = emptyList(),
    val selectedAppForMenu: AppModel? = null,
    val appToRename: AppModel? = null,
    val timeState: TimeState = TimeAnchorHelper.calculateTimeState(),
    val nextEvent: CalendarEventModel? = null,
    val scratchpadText: String = "",
    val showTimeAnchor: Boolean = true,
    val showDayProgress: Boolean = true,
    val showNextEvent: Boolean = true,
    val autoOpenKeyboard: Boolean = false,
    val doubleTapToLock: Boolean = true,
    val themeMode: String = "amoled",
    val fontStyle: String = "sans",
    val enableHaptics: Boolean = true,
    val isLoading: Boolean = true
)

@OptIn(FlowPreview::class)
class LauncherViewModel(
    private val appRepository: AppRepository,
    private val settingsRepository: SettingsRepository,
    private val calendarRepository: CalendarRepository,
    private val context: Context
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _selectedAppForMenu = MutableStateFlow<AppModel?>(null)
    private val _appToRename = MutableStateFlow<AppModel?>(null)
    private val _scratchpadInput = MutableSharedFlow<String>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    init {
        viewModelScope.launch {
            _scratchpadInput
                .debounce(400L)
                .distinctUntilChanged()
                .collect { text ->
                    settingsRepository.saveScratchpadText(text)
                }
        }
    }

    val allAppsFlow: StateFlow<List<AppModel>> = combine(
        appRepository.getInstalledApps(),
        settingsRepository.favorites,
        settingsRepository.hiddenApps,
        settingsRepository.customLabels
    ) { apps, favorites, hidden, labels ->
        apps.map { app ->
            app.copy(
                isFavorite = app.packageName in favorites || app.uniqueKey in favorites,
                isHidden = app.packageName in hidden || app.uniqueKey in hidden,
                customLabel = labels[app.uniqueKey] ?: labels[app.packageName]
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val preferencesFlow: Flow<PreferencesBundle> = combine(
        settingsRepository.showTimeAnchor,
        settingsRepository.showDayProgress,
        settingsRepository.themeMode,
        settingsRepository.fontStyle,
        settingsRepository.enableHaptics
    ) { timeAnchor, dayProgress, theme, font, haptics ->
        PreferencesBundle(timeAnchor, dayProgress, theme, font, haptics)
    }

    private val extendedPrefsFlow = combine(
        settingsRepository.autoOpenKeyboard,
        settingsRepository.showNextEvent,
        settingsRepository.doubleTapToLock,
        settingsRepository.scratchpadText,
        calendarRepository.getNextUpcomingEventFlow()
    ) { autoKeyboard, showNextEvent, doubleTapLock, scratchText, nextEvent ->
        ExtendedPrefs(autoKeyboard, showNextEvent, doubleTapLock, scratchText, nextEvent)
    }

    private val appsAndSearchFlow = combine(allAppsFlow, _searchQuery) { apps, query ->
        apps to query
    }

    private val dialogStatesFlow = combine(_selectedAppForMenu, _appToRename) { menu, rename ->
        menu to rename
    }

    val uiState: StateFlow<HomeUiState> = combine(
        appsAndSearchFlow,
        dialogStatesFlow,
        TimeAnchorHelper.timeStateFlow(context),
        combine(preferencesFlow, extendedPrefsFlow) { p, ep -> p to ep }
    ) { (apps, query), (selectedMenuApp, renamingApp), timeState, (prefs, ext) ->
        val nextEv = if (ext.showNextEvent) ext.nextEvent else null

        if (apps.isEmpty()) {
            HomeUiState(
                searchQuery = query,
                selectedAppForMenu = selectedMenuApp,
                appToRename = renamingApp,
                timeState = timeState,
                nextEvent = nextEv,
                scratchpadText = ext.scratchpadText,
                showTimeAnchor = prefs.showTimeAnchor,
                showDayProgress = prefs.showDayProgress,
                showNextEvent = ext.showNextEvent,
                autoOpenKeyboard = ext.autoOpenKeyboard,
                doubleTapToLock = ext.doubleTapLock,
                themeMode = prefs.themeMode,
                fontStyle = prefs.fontStyle,
                enableHaptics = prefs.enableHaptics,
                isLoading = true
            )
        } else if (query.isEmpty()) {
            val favorites = apps.filter { it.isFavorite && !it.isHidden }
                .sortedBy { it.displayName.lowercase() }
            val others = apps.filter { !it.isFavorite && !it.isHidden }
                .sortedBy { it.displayName.lowercase() }

            HomeUiState(
                searchQuery = query,
                favoriteApps = favorites,
                otherApps = others,
                allApps = apps,
                selectedAppForMenu = selectedMenuApp,
                appToRename = renamingApp,
                timeState = timeState,
                nextEvent = nextEv,
                scratchpadText = ext.scratchpadText,
                showTimeAnchor = prefs.showTimeAnchor,
                showDayProgress = prefs.showDayProgress,
                showNextEvent = ext.showNextEvent,
                autoOpenKeyboard = ext.autoOpenKeyboard,
                doubleTapToLock = ext.doubleTapLock,
                themeMode = prefs.themeMode,
                fontStyle = prefs.fontStyle,
                enableHaptics = prefs.enableHaptics,
                isLoading = false
            )
        } else {
            val visibleApps = apps.filter { !it.isHidden }
            val scored = visibleApps.mapNotNull { app ->
                val score = SearchMatcher.matchScore(app, query)
                if (score != null) app to score else null
            }.sortedBy { it.second }
                .map { it.first }

            HomeUiState(
                searchQuery = query,
                favoriteApps = emptyList(),
                otherApps = scored,
                allApps = apps,
                selectedAppForMenu = selectedMenuApp,
                appToRename = renamingApp,
                timeState = timeState,
                nextEvent = nextEv,
                scratchpadText = ext.scratchpadText,
                showTimeAnchor = prefs.showTimeAnchor,
                showDayProgress = prefs.showDayProgress,
                showNextEvent = ext.showNextEvent,
                autoOpenKeyboard = ext.autoOpenKeyboard,
                doubleTapToLock = ext.doubleTapLock,
                themeMode = prefs.themeMode,
                fontStyle = prefs.fontStyle,
                enableHaptics = prefs.enableHaptics,
                isLoading = false
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query

        // Instant Auto-Launch: When typing reduces visible matching apps to exactly 1 candidate,
        // immediately launch the app without side-effects in state derivation
        val trimmedQuery = query.trim()
        val isSettingsQuery = "settings".contains(trimmedQuery, ignoreCase = true) && trimmedQuery.length >= 3
        if (trimmedQuery.isNotEmpty() && !isSettingsQuery) {
            val visibleApps = allAppsFlow.value.filter { !it.isHidden }
            val scored = visibleApps.mapNotNull { app ->
                val score = SearchMatcher.matchScore(app, trimmedQuery)
                if (score != null) app to score else null
            }.sortedBy { it.second }.map { it.first }

            if (scored.size == 1) {
                launchApp(scored[0])
            }
        }
    }

    fun launchApp(app: AppModel) {
        appRepository.launchApp(app)
        _searchQuery.value = ""
    }

    fun launchApp(packageName: String) {
        appRepository.launchApp(packageName)
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

    fun toggleFavorite(packageName: String) {
        viewModelScope.launch {
            settingsRepository.toggleFavorite(packageName)
            _selectedAppForMenu.value = null
        }
    }

    fun toggleHidden(packageName: String) {
        viewModelScope.launch {
            settingsRepository.toggleHidden(packageName)
            _selectedAppForMenu.value = null
        }
    }

    fun openAppInfo(app: AppModel) {
        appRepository.openAppInfo(app.packageName, app.userHandle, app.activityName)
        _selectedAppForMenu.value = null
    }

    fun openAppInfo(packageName: String) {
        appRepository.openAppInfo(packageName)
        _selectedAppForMenu.value = null
    }

    fun uninstallApp(packageName: String) {
        appRepository.uninstallApp(packageName)
        _selectedAppForMenu.value = null
    }

    fun setShowTimeAnchor(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setShowTimeAnchor(enabled) }
    }

    fun setShowDayProgress(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setShowDayProgress(enabled) }
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

    fun saveScratchpadText(text: String) {
        _scratchpadInput.tryEmit(text)
    }

    fun flushScratchpadText(text: String) {
        viewModelScope.launch {
            settingsRepository.saveScratchpadText(text)
        }
    }

    fun openCalendarApp() {
        calendarRepository.openCalendarApp()
    }

    fun setDoubleTapToLock(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setDoubleTapToLock(enabled) }
    }

    fun lockScreen(): Boolean {
        return SansAccessibilityService.lockScreen()
    }

    private data class PreferencesBundle(
        val showTimeAnchor: Boolean,
        val showDayProgress: Boolean,
        val themeMode: String,
        val fontStyle: String,
        val enableHaptics: Boolean
    )

    private data class ExtendedPrefs(
        val autoOpenKeyboard: Boolean,
        val showNextEvent: Boolean,
        val doubleTapLock: Boolean,
        val scratchpadText: String,
        val nextEvent: CalendarEventModel?
    )
}

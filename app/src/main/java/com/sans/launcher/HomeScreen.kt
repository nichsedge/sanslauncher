package com.sans.launcher

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: LauncherViewModel,
    onOpenSettings: () -> Unit,
    onRequestCalendarPermission: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val isSearchEmpty by rememberUpdatedState(uiState.searchQuery.isEmpty())
    val canScrollBack by rememberUpdatedState(listState.canScrollBackward)
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }

    var textFieldValue by remember {
        mutableStateOf(TextFieldValue(uiState.searchQuery, TextRange(uiState.searchQuery.length)))
    }

    LaunchedEffect(uiState.searchQuery) {
        if (uiState.searchQuery != textFieldValue.text) {
            textFieldValue = TextFieldValue(
                text = uiState.searchQuery,
                selection = TextRange(uiState.searchQuery.length)
            )
        }
    }

    // BackHandler for granular state cleanup (moved from MainActivity to avoid full uiState collection there)
    BackHandler(enabled = uiState.appToRename != null || uiState.selectedAppForMenu != null || textFieldValue.text.isNotEmpty()) {
        when {
            uiState.appToRename != null -> viewModel.dismissRenameDialog()
            uiState.selectedAppForMenu != null -> viewModel.dismissAppMenu()
            textFieldValue.text.isNotEmpty() -> {
                textFieldValue = TextFieldValue("", TextRange.Zero)
                viewModel.updateSearchQuery("")
            }
        }
    }

    var permissionRequested by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(uiState.showNextEvent) {
        if (uiState.showNextEvent && !permissionRequested) {
            permissionRequested = true
            onRequestCalendarPermission()
        }
    }

    LaunchedEffect(uiState.autoOpenKeyboard, uiState.isLoading, uiState.searchQuery) {
        if (uiState.autoOpenKeyboard && !uiState.isLoading && uiState.searchQuery.isEmpty()) {
            delay(100.milliseconds)
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) {
            focusManager.clearFocus()
            keyboardController?.hide()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = 22.dp, vertical = 12.dp)
            .pointerInput(Unit) {
                val dragThreshold = 35f

                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val up = waitForUpOrCancellation()

                    if (up != null) {
                        val dragY = up.position.y - down.position.y
                        val isDrag = dragY > dragThreshold && isSearchEmpty && !canScrollBack

                        if (isDrag) {
                            NotificationShadeHelper.expandNotificationShade(context)
                        }
                    }
                }
            }
    ) {
        val isSearching by remember { derivedStateOf { uiState.searchQuery.isNotEmpty() } }
        val enableHaptics = uiState.enableHaptics
        val onLaunchApp: (AppModel) -> Unit = remember(viewModel) {
            { app -> viewModel.launchApp(app) }
        }
        val onOpenAppMenu: (AppModel) -> Unit = remember(viewModel) {
            { app -> viewModel.openAppMenu(app) }
        }

        // Time Anchor Section (Long press top area opens settings!)
        // TimeState & NextEventState collected inside TimeAnchorHeader to scope recomposition
        if (!isSearching) {
            TimeAnchorHeader(
                timeStateFlow = viewModel.timeState,
                nextEventFlow = viewModel.nextEventState,
                showTime = uiState.showTimeAnchor,
                showNextEvent = uiState.showNextEvent,
                onEventClick = { startTime -> viewModel.openCalendarApp(startTime) },
                onLongClick = {
                    if (enableHaptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onOpenSettings()
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // App List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            val favorites = uiState.favoriteApps
            val others = uiState.otherApps

            // If searching for settings or preferences, show direct launcher settings entry
            val trimmedSearch = uiState.searchQuery.trim()
            if (isSearching && SearchMatcher.isSettingsQuery(trimmedSearch)) {
                item(key = "launcher_settings_search_item") {
                    Row(
                        modifier = Modifier
                            .animateItem()
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .combinedClickable(
                                onClick = {
                                    if (enableHaptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.updateSearchQuery("")
                                    onOpenSettings()
                                },
                                onClickLabel = "Open launcher settings"
                            )
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                        Text(
                            text = "Sans Launcher Settings",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }

            if (!isSearching && favorites.isNotEmpty()) {
                items(
                    items = favorites,
                    key = { it.uniqueKey },
                    contentType = { "app_item" }
                ) { app ->
                    AppItem(
                        app = app,
                        onLaunchApp = onLaunchApp,
                        onOpenAppMenu = onOpenAppMenu,
                        enableHaptics = enableHaptics,
                        modifier = Modifier.animateItem(),
                        isBold = true
                    )
                }

                if (others.isNotEmpty()) {
                    item(key = "divider") {
                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }

            items(
                items = others,
                key = { it.uniqueKey },
                contentType = { "app_item" }
            ) { app ->
                AppItem(
                    app = app,
                    onLaunchApp = onLaunchApp,
                    onOpenAppMenu = onOpenAppMenu,
                    enableHaptics = enableHaptics,
                    modifier = Modifier.animateItem(),
                    alpha = if (!isSearching) 0.65f else 1f
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f))
        Spacer(modifier = Modifier.height(8.dp))

        // Full-width Minimal Search Bar
        TextField(
            value = textFieldValue,
            onValueChange = { newValue ->
                textFieldValue = newValue
                viewModel.updateSearchQuery(newValue.text)
            },
            placeholder = {
                Text(
                    "type to search...",
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.55f)
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            textStyle = MaterialTheme.typography.bodyLarge,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(
                onGo = {
                    val firstApp = uiState.otherApps.firstOrNull() ?: uiState.favoriteApps.firstOrNull()
                    if (firstApp != null) {
                        if (enableHaptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.launchApp(firstApp)
                        focusManager.clearFocus()
                    }
                }
            ),
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (uiState.showCircleToSearchShortcut) {
                        IconButton(
                            onClick = {
                                if (enableHaptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.triggerCircleToSearch(context)
                                focusManager.clearFocus()
                            }
                        ) {
                            Icon(
                                imageVector = LauncherIcons.Search,
                                contentDescription = "Circle to Search",
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                    if (uiState.showExpenseShortcut) {
                        IconButton(
                            onClick = {
                                if (enableHaptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.recordExpense(textFieldValue.text)
                                focusManager.clearFocus()
                            }
                        ) {
                            Icon(
                                imageVector = LauncherIcons.AttachMoney,
                                contentDescription = "Record expense in Sans Finance",
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                    if (textFieldValue.text.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                textFieldValue = TextFieldValue("", TextRange.Zero)
                                viewModel.updateSearchQuery("")
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear search",
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }
        )
    }

    // App Context BottomSheet
    uiState.selectedAppForMenu?.let { app ->
        AppContextBottomSheet(
            app = app,
            onDismiss = { viewModel.dismissAppMenu() },
            onToggleFavorite = { viewModel.toggleFavorite(app.uniqueKey, app.packageName) },
            onToggleHidden = { viewModel.toggleHidden(app.uniqueKey, app.packageName) },
            onStartRename = { viewModel.startRenamingApp(app) },
            onOpenAppInfo = { viewModel.openAppInfo(app) },
            onUninstall = { viewModel.uninstallApp(app) }
        )
    }

    // Rename App Dialog
    uiState.appToRename?.let { app ->
        RenameAppDialog(
            app = app,
            onDismiss = { viewModel.dismissRenameDialog() },
            onSave = { newLabel -> viewModel.saveCustomLabel(app.uniqueKey, newLabel) }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TimeAnchorHeader(
    timeStateFlow: StateFlow<TimeState>,
    nextEventFlow: StateFlow<CalendarEventModel?>,
    showTime: Boolean,
    showNextEvent: Boolean,
    onEventClick: (Long?) -> Unit,
    onLongClick: () -> Unit
) {
    // Collect timeState & nextEventState here so clock ticks only recompose this header, not the entire HomeScreen
    val timeState by timeStateFlow.collectAsStateWithLifecycle()
    val nextEvent by nextEventFlow.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp)
    ) {
        if (showTime) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = { /* Optional tap */ },
                        onLongClick = onLongClick,
                        onLongClickLabel = "Open settings"
                    )
            ) {
                Text(
                    text = timeState.timeFormatted,
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = timeState.dateFormatted,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }

        // Single Next Upcoming Calendar Event
        if (showNextEvent && nextEvent != null) {
            val event = nextEvent
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onEventClick(event?.startTimeMillis) }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = LauncherIcons.CalendarToday,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = "${event?.timeDisplay} • ${event?.title} (${event?.relativeDisplay})",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (!showTime && (!showNextEvent || nextEvent == null)) {
            // Keep top header touch target active for opening settings on long press when clock is hidden
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .combinedClickable(
                        onClick = { },
                        onLongClick = onLongClick,
                        onLongClickLabel = "Open settings"
                    )
                    .semantics {
                        contentDescription = "Launcher header"
                    }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppItem(
    app: AppModel,
    onLaunchApp: (AppModel) -> Unit,
    onOpenAppMenu: (AppModel) -> Unit,
    enableHaptics: Boolean,
    modifier: Modifier = Modifier,
    alpha: Float = 1f,
    isBold: Boolean = false
) {
    val haptic = LocalHapticFeedback.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .combinedClickable(
                onClick = {
                    if (enableHaptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLaunchApp(app)
                },
                onLongClick = {
                    if (enableHaptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onOpenAppMenu(app)
                },
                onClickLabel = "Launch ${app.displayName}",
                onLongClickLabel = "Open options for ${app.displayName}"
            )
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = app.displayName,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = if (isBold) FontWeight.SemiBold else FontWeight.Normal
            ),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = alpha),
            modifier = Modifier.weight(1f, fill = false)
        )
        if (app.isWorkProfile) {
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = LauncherIcons.Work,
                contentDescription = "Work Profile",
                tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.65f),
                modifier = Modifier.size(15.dp)
            )
        } else if (app.isCloneProfile) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "CLONE",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.65f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppContextBottomSheet(
    app: AppModel,
    onDismiss: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleHidden: () -> Unit,
    onStartRename: () -> Unit,
    onOpenAppInfo: () -> Unit,
    onUninstall: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = app.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (app.isWorkProfile) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = LauncherIcons.Work,
                        contentDescription = "Work Profile",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            if (app.isWorkProfile) {
                Text(
                    text = "Work Profile",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                )
            } else if (app.isCloneProfile) {
                Text(
                    text = "Dual / Cloned App",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                )
            }
            if (app.customLabel != null && app.customLabel != app.label) {
                Text(
                    text = "Original: ${app.label}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f))

            MenuActionItem(
                icon = if (app.isFavorite) Icons.Filled.Star else LauncherIcons.StarBorder,
                title = if (app.isFavorite) "Remove from Pinned Favorites" else "Pin to Favorites",
                onClick = onToggleFavorite
            )

            MenuActionItem(
                icon = Icons.Default.Edit,
                title = "Rename / Custom Alias",
                onClick = onStartRename
            )

            MenuActionItem(
                icon = LauncherIcons.VisibilityOff,
                title = if (app.isHidden) "Unhide App" else "Hide App",
                onClick = onToggleHidden
            )

            MenuActionItem(
                icon = Icons.Default.Info,
                title = "App Info (Settings)",
                onClick = onOpenAppInfo
            )

            MenuActionItem(
                icon = Icons.Default.Delete,
                title = "Uninstall",
                onClick = onUninstall
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun MenuActionItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick)
    )
}

@Composable
fun RenameAppDialog(
    app: AppModel,
    onDismiss: () -> Unit,
    onSave: (String?) -> Unit
) {
    var text by remember { mutableStateOf(app.customLabel ?: app.label) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename App") },
        text = {
            Column {
                Text(
                    text = "Set a custom alias or mnemonic for ${app.label}:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(12.dp))
                TextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(text) }) {
                Text("Save")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!app.customLabel.isNullOrBlank()) {
                    TextButton(onClick = { onSave(null) }) {
                        Text("Reset")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}

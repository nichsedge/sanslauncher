package com.sans.launcher

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: LauncherViewModel,
    onOpenSettings: () -> Unit,
    onRequestCalendarPermission: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }
    var showAccessibilityPrompt by remember { mutableStateOf(false) }

    val pagerState = rememberPagerState(pageCount = { 2 })

    LaunchedEffect(Unit) {
        if (uiState.showNextEvent) {
            onRequestCalendarPermission()
        }
    }

    LaunchedEffect(uiState.autoOpenKeyboard, pagerState.currentPage) {
        if (uiState.autoOpenKeyboard && pagerState.currentPage == 0) {
            focusRequester.requestFocus()
        }
    }

    fun handleDoubleTapToLock() {
        if (uiState.doubleTapToLock) {
            if (uiState.enableHaptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            val locked = viewModel.lockScreen()
            if (!locked) {
                showAccessibilityPrompt = true
            }
        }
    }

    HorizontalPager(
        state = pagerState,
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
    ) { page ->
        if (page == 0) {
            // PAGE 0: MAIN APPS & TIME LAUNCHER
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 22.dp, vertical = 12.dp)
                    .pointerInput(Unit) {
                        detectVerticalDragGestures { _, dragAmount ->
                            if (dragAmount > 35f && uiState.searchQuery.isEmpty()) {
                                NotificationShadeHelper.expandNotificationShade(context)
                            }
                        }
                    }
                    .pointerInput(uiState.doubleTapToLock) {
                        detectTapGestures(
                            onDoubleTap = { handleDoubleTapToLock() }
                        )
                    }
            ) {
                val isSearching = uiState.searchQuery.isNotEmpty()

                // Time Anchor & Day Progress Section (Long press header opens settings!)
                if (!isSearching && (uiState.showTimeAnchor || uiState.showDayProgress || (uiState.showNextEvent && uiState.nextEvent != null))) {
                    TimeAnchorHeader(
                        timeState = uiState.timeState,
                        nextEvent = uiState.nextEvent,
                        showTime = uiState.showTimeAnchor,
                        showProgress = uiState.showDayProgress,
                        showNextEvent = uiState.showNextEvent,
                        onEventClick = { viewModel.openCalendarApp() },
                        onDoubleTap = { handleDoubleTapToLock() },
                        onLongClick = {
                            if (uiState.enableHaptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onOpenSettings()
                        }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // App List
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    val favorites = uiState.favoriteApps
                    val others = uiState.otherApps

                    // If searching for "settings", show direct launcher settings entry
                    if (isSearching && "settings".contains(uiState.searchQuery.trim(), ignoreCase = true)) {
                        item(key = "launcher_settings_search_item") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = {
                                            if (uiState.enableHaptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            viewModel.updateSearchQuery("")
                                            onOpenSettings()
                                        }
                                    )
                                    .padding(vertical = 10.dp),
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
                            key = { "fav_${it.packageName}" }
                        ) { app ->
                            AppItem(
                                app = app,
                                onClick = {
                                    if (uiState.enableHaptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.launchApp(app.packageName)
                                },
                                onLongClick = {
                                    if (uiState.enableHaptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.openAppMenu(app)
                                },
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
                        key = { "other_${it.packageName}" }
                    ) { app ->
                        AppItem(
                            app = app,
                            onClick = {
                                if (uiState.enableHaptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.launchApp(app.packageName)
                            },
                            onLongClick = {
                                if (uiState.enableHaptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.openAppMenu(app)
                            },
                            alpha = if (!isSearching) 0.65f else 1f
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(8.dp))

                // Full-width Minimal Search Bar
                TextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
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
                                if (uiState.enableHaptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.launchApp(firstApp.packageName)
                                focusManager.clearFocus()
                            }
                        }
                    ),
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear search",
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                )
            }
        } else {
            // PAGE 1: BRAIN DUMP SCRATCHPAD
            ScratchpadScreen(
                text = uiState.scratchpadText,
                onTextChange = { viewModel.saveScratchpadText(it) }
            )
        }
    }

    // App Context BottomSheet
    uiState.selectedAppForMenu?.let { app ->
        AppContextBottomSheet(
            app = app,
            onDismiss = { viewModel.dismissAppMenu() },
            onToggleFavorite = { viewModel.toggleFavorite(app.packageName) },
            onToggleHidden = { viewModel.toggleHidden(app.packageName) },
            onStartRename = { viewModel.startRenamingApp(app) },
            onOpenAppInfo = { viewModel.openAppInfo(app.packageName) },
            onUninstall = { viewModel.uninstallApp(app.packageName) }
        )
    }

    // Rename App Dialog
    uiState.appToRename?.let { app ->
        RenameAppDialog(
            app = app,
            onDismiss = { viewModel.dismissRenameDialog() },
            onSave = { newLabel -> viewModel.saveCustomLabel(app.packageName, newLabel) }
        )
    }

    // Accessibility Permission Prompt for Double-Tap Lock
    if (showAccessibilityPrompt) {
        AlertDialog(
            onDismissRequest = { showAccessibilityPrompt = false },
            title = { Text("Enable Double-Tap to Lock") },
            text = {
                Text(
                    text = "To lock your screen on double-tap without breaking fingerprint unlock, Android requires enabling Sans Launcher in Accessibility Settings.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showAccessibilityPrompt = false
                        SansAccessibilityService.openAccessibilitySettings(context)
                    }
                ) {
                    Text("Open Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAccessibilityPrompt = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TimeAnchorHeader(
    timeState: TimeState,
    nextEvent: CalendarEventModel?,
    showTime: Boolean,
    showProgress: Boolean,
    showNextEvent: Boolean,
    onEventClick: () -> Unit,
    onDoubleTap: () -> Unit,
    onLongClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { /* Optional tap */ },
                onDoubleClick = onDoubleTap,
                onLongClick = onLongClick
            )
            .padding(top = 8.dp, bottom = 4.dp)
    ) {
        if (showTime) {
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

        // Single Next Upcoming Calendar Event
        if (showNextEvent && nextEvent != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onEventClick() }
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                    modifier = Modifier.padding(end = 6.dp)
                )
                Text(
                    text = "${nextEvent.timeDisplay} • ${nextEvent.title} (${nextEvent.relativeDisplay})",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1
                )
            }
        }

        if (showProgress) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${timeState.dayProgressPercent}% of today elapsed",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { timeState.dayProgressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                trackColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
                strokeCap = StrokeCap.Round
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppItem(
    app: AppModel,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    alpha: Float = 1f,
    isBold: Boolean = false
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(vertical = 10.dp)
    ) {
        Text(
            text = app.displayName,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = if (isBold) FontWeight.SemiBold else FontWeight.Normal
            ),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = alpha),
            modifier = Modifier.fillMaxWidth()
        )
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
            Text(
                text = app.displayName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
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
                icon = if (app.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                title = if (app.isFavorite) "Remove from Pinned Favorites" else "Pin to Favorites",
                onClick = onToggleFavorite
            )

            MenuActionItem(
                icon = Icons.Default.Edit,
                title = "Rename / Custom Alias",
                onClick = onStartRename
            )

            MenuActionItem(
                icon = Icons.Default.VisibilityOff,
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

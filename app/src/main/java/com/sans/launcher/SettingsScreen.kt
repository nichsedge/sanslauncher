package com.sans.launcher

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController

@Composable
fun SettingsScreen(
    viewModel: LauncherViewModel,
    onBack: () -> Unit,
    onRequestCalendarPermission: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val allApps by viewModel.allAppsFlow.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    var filterQuery by remember { mutableStateOf("") }

    BackHandler(enabled = uiState.appToRename != null || filterQuery.isNotEmpty()) {
        when {
            uiState.appToRename != null -> viewModel.dismissRenameDialog()
            filterQuery.isNotEmpty() -> filterQuery = ""
        }
    }

    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) {
            focusManager.clearFocus()
            keyboardController?.hide()
        }
    }

    val filteredApps = remember(allApps, filterQuery) {
        val trimmed = filterQuery.trim().lowercase()
        if (trimmed.isEmpty()) {
            allApps
        } else {
            allApps.filter {
                it.normalizedDisplayName.contains(trimmed) ||
                it.label.contains(trimmed, ignoreCase = true)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                "Preferences & Apps",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            // --- SECTION 1: ADHD & TIME ANCHOR ---
            item {
                SectionHeader("ADHD & Time Scaffolding")
                SettingToggleRow(
                    title = "Show Clock & Date",
                    subtitle = "Ambient time anchor (long press top area opens settings)",
                    checked = uiState.showTimeAnchor,
                    onCheckedChange = { viewModel.setShowTimeAnchor(it) }
                )
                SettingToggleRow(
                    title = "Next Calendar Event",
                    subtitle = "Single upcoming event preview under the date",
                    checked = uiState.showNextEvent,
                    onCheckedChange = {
                        viewModel.setShowNextEvent(it)
                        if (it) onRequestCalendarPermission()
                    }
                )
                SettingToggleRow(
                    title = "Auto-Open Keyboard",
                    subtitle = "Instantly pop up keyboard on home screen",
                    checked = uiState.autoOpenKeyboard,
                    onCheckedChange = { viewModel.setAutoOpenKeyboard(it) }
                )
                SettingToggleRow(
                    title = "Tactile Haptics",
                    subtitle = "Crisp vibrational feedback on interactions",
                    checked = uiState.enableHaptics,
                    onCheckedChange = { viewModel.setEnableHaptics(it) }
                )
                SettingToggleRow(
                    title = "Sans Finance Quick Expense",
                    subtitle = "Show money icon in search bar to record expenses faster",
                    checked = uiState.showExpenseShortcut,
                    onCheckedChange = { viewModel.setShowExpenseShortcut(it) }
                )
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(16.dp))
            }

            // --- SECTION 2: THEME & TYPOGRAPHY ---
            item {
                SectionHeader("Appearance & Typography")
                
                Text(
                    text = "Theme Palette",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ThemeChip("Dark", uiState.themeMode == "dark" || uiState.themeMode == "amoled") { viewModel.setThemeMode("dark") }
                    ThemeChip("Light", uiState.themeMode == "light") { viewModel.setThemeMode("light") }
                    ThemeChip("System", uiState.themeMode == "system") { viewModel.setThemeMode("system") }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Font Family",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ThemeChip("Sans-Serif", uiState.fontStyle == "sans") { viewModel.setFontStyle("sans") }
                    ThemeChip("Monospace", uiState.fontStyle == "mono") { viewModel.setFontStyle("mono") }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(16.dp))
            }

            // --- SECTION 3: APP MANAGER ---
            item {
                SectionHeader("App Manager (Tap edit to rename / alias)")
                TextField(
                    value = filterQuery,
                    onValueChange = { filterQuery = it },
                    placeholder = {
                        Text(
                            "filter apps...",
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.bodyMedium,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f),
                        unfocusedIndicatorColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
                    ),
                    singleLine = true,
                    trailingIcon = {
                        if (filterQuery.isNotEmpty()) {
                            IconButton(onClick = { filterQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear filter",
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(
                items = filteredApps,
                key = { it.uniqueKey }
            ) { app ->
                Row(
                    modifier = Modifier
                        .animateItem()
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.startRenamingApp(app) }
                            .padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = app.displayName,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (app.isHidden) MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onBackground
                        )
                        if (app.customLabel != null && app.customLabel != app.label) {
                            Text(
                                text = "Orig: ${app.label}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f)
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SettingIconButton(
                            icon = Icons.Default.Edit,
                            active = false,
                            contentDescription = "Rename ${app.displayName}",
                            onClick = { viewModel.startRenamingApp(app) }
                        )

                        SettingIconButton(
                            icon = if (app.isFavorite) Icons.Filled.Star else LauncherIcons.StarBorder,
                            active = app.isFavorite,
                            contentDescription = if (app.isFavorite) "Remove ${app.displayName} from pinned favorites" else "Pin ${app.displayName} to favorites",
                            onClick = { viewModel.toggleFavorite(app.uniqueKey) }
                        )

                        SettingIconButton(
                            icon = if (!app.isHidden) LauncherIcons.Visibility else LauncherIcons.VisibilityOff,
                            active = !app.isHidden,
                            contentDescription = if (app.isHidden) "Unhide ${app.displayName}" else "Hide ${app.displayName}",
                            onClick = { viewModel.toggleHidden(app.uniqueKey) }
                        )
                    }
                }
            }
        }
    }

    // Rename App Dialog in Settings Screen
    uiState.appToRename?.let { app ->
        RenameAppDialog(
            app = app,
            onDismiss = { viewModel.dismissRenameDialog() },
            onSave = { newLabel -> viewModel.saveCustomLabel(app.uniqueKey, newLabel) }
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.background,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.secondary,
                uncheckedTrackColor = MaterialTheme.colorScheme.surface
            )
        )
    }
}

@Composable
private fun ThemeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.bodySmall) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
            containerColor = Color.Transparent,
            labelColor = MaterialTheme.colorScheme.secondary
        )
    )
}

@Composable
private fun SettingIconButton(
    icon: ImageVector,
    active: Boolean,
    contentDescription: String,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)
        )
    }
}

package com.sans.launcher

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory

class MainActivity : ComponentActivity() {

    private var currentScreen by mutableStateOf("home")
    private lateinit var launcherViewModel: LauncherViewModel

    private val requestCalendarPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        // When permission is granted, ViewModel flow will automatically query calendar
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT)
        )
        
        val appRepository = AppRepository(applicationContext)
        val settingsRepository = SettingsRepository(applicationContext)
        val calendarRepository = CalendarRepository(applicationContext)

        val factory = viewModelFactory {
            initializer {
                LauncherViewModel(appRepository, settingsRepository, calendarRepository)
            }
        }

        setContent {
            launcherViewModel = viewModel(factory = factory)
            val uiState by launcherViewModel.uiState.collectAsState()

            LauncherTheme(
                themeMode = uiState.themeMode,
                fontStyle = uiState.fontStyle
            ) {
                // Intercept back gesture everywhere to prevent OS exit swipe animations
                BackHandler(enabled = true) {
                    when {
                        currentScreen != "home" -> {
                            currentScreen = "home"
                        }
                        uiState.appToRename != null -> {
                            launcherViewModel.dismissRenameDialog()
                        }
                        uiState.selectedAppForMenu != null -> {
                            launcherViewModel.dismissAppMenu()
                        }
                        uiState.searchQuery.isNotEmpty() -> {
                            launcherViewModel.updateSearchQuery("")
                        }
                        else -> {
                            // On clean home screen: do nothing and consume back event
                        }
                    }
                }

                if (currentScreen == "home") {
                    HomeScreen(
                        viewModel = launcherViewModel,
                        onOpenSettings = { currentScreen = "settings" },
                        onRequestCalendarPermission = {
                            if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
                                requestCalendarPermissionLauncher.launch(Manifest.permission.READ_CALENDAR)
                            }
                        }
                    )
                } else {
                    SettingsScreen(
                        viewModel = launcherViewModel,
                        onBack = { currentScreen = "home" },
                        onRequestCalendarPermission = {
                            requestCalendarPermissionLauncher.launch(Manifest.permission.READ_CALENDAR)
                        }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        currentScreen = "home"
        if (::launcherViewModel.isInitialized) {
            launcherViewModel.updateSearchQuery("")
            launcherViewModel.dismissAppMenu()
            launcherViewModel.dismissRenameDialog()
        }
    }
}

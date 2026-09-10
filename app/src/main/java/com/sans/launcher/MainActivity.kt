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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory

class MainActivity : ComponentActivity() {

    private var currentScreen by mutableStateOf("home")
    private lateinit var launcherViewModel: LauncherViewModel

    private val requestCalendarPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted && ::launcherViewModel.isInitialized) {
            launcherViewModel.refreshCalendarEvents()
        }
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
                LauncherViewModel(appRepository, settingsRepository, calendarRepository, applicationContext)
            }
        }

        setContent {
            launcherViewModel = viewModel(factory = factory)
            val themeStatePair by launcherViewModel.themeState.collectAsStateWithLifecycle()
            val (themeMode, fontStyle) = themeStatePair

            LauncherTheme(
                themeMode = themeMode,
                fontStyle = fontStyle
            ) {
                // Intercept back gesture everywhere to prevent OS exit swipe animations
                BackHandler(enabled = true) {
                    when {
                        currentScreen != "home" -> {
                            currentScreen = "home"
                        }
                        else -> {
                            // Delegate to HomeScreen/SettingsScreen BackHandler for granular state cleanup.
                            // On clean home screen: do nothing and consume back event.
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

    override fun onResume() {
        super.onResume()
        if (::launcherViewModel.isInitialized) {
            launcherViewModel.refreshCalendarEvents()
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
            launcherViewModel.refreshCalendarEvents()
        }
    }
}

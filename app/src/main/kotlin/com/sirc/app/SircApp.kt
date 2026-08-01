package com.sirc.app

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sirc.feature.history.HistoryScreen
import com.sirc.feature.history.StatsScreen
import com.sirc.feature.settings.SettingsScreen

private enum class Destination(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    HOME("home", "Inicio", Icons.Filled.Home),
    HISTORY("history", "Historial", Icons.Filled.History),
    STATS("stats", "Estadísticas", Icons.Filled.BarChart),
    SETTINGS("settings", "Ajustes", Icons.Filled.Settings),
    DIAGNOSIS("diagnosis", "Diagnóstico", Icons.AutoMirrored.Filled.FactCheck),
    DEBUG("debug", "Debug", Icons.Filled.Build),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SircApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    val debugViewModel: DebugPanelViewModel = hiltViewModel()
    val debugState by debugViewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("SIRC") })
        },
        bottomBar = {
            NavigationBar {
                Destination.entries.forEach { destination ->
                    if (destination == Destination.DEBUG && !debugState.debugPanelEnabled) {
                        return@forEach
                    }
                    val selected =
                        currentDestination?.hierarchy
                            ?.any { it.route == destination.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = destination.icon,
                                contentDescription = destination.label,
                            )
                        },
                        label = { Text(destination.label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Destination.HOME.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Destination.HOME.route) {
                HomeScreen()
            }
            composable(Destination.HISTORY.route) {
                HistoryScreen()
            }
            composable(Destination.STATS.route) {
                StatsScreen()
            }
            composable(Destination.SETTINGS.route) {
                SettingsScreen()
            }
            composable(Destination.DIAGNOSIS.route) {
                DiagnosisScreen()
            }
            composable(Destination.DEBUG.route) {
                DebugPanelScreen()
            }
        }
    }
}

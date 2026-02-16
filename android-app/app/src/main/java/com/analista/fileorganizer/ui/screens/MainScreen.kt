package com.analista.fileorganizer.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.analista.fileorganizer.ui.MainViewModel

sealed class Screen(val route: String, val title: String, val icon: @Composable () -> Unit) {
    data object Agent : Screen("agent", "Agent", { Icon(Icons.Default.SmartToy, contentDescription = "Agent") })
    data object Files : Screen("files", "Files", { Icon(Icons.Default.Folder, contentDescription = "Files") })
    data object Organize : Screen("organize", "Organize", { Icon(Icons.Default.Sort, contentDescription = "Organize") })
    data object Telegram : Screen("telegram", "Telegram", { Icon(Icons.Default.Send, contentDescription = "Telegram") })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStack?.destination?.route ?: Screen.Agent.route

    val uiState by viewModel.uiState.collectAsState()

    val screens = listOf(Screen.Agent, Screen.Files, Screen.Organize, Screen.Telegram)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("File Organizer AI") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            NavigationBar {
                screens.forEach { screen ->
                    NavigationBarItem(
                        icon = screen.icon,
                        label = { Text(screen.title) },
                        selected = currentRoute == screen.route,
                        onClick = {
                            if (currentRoute != screen.route) {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    )
                }
            }
        },
        snackbarHost = {
            uiState.statusMessage?.let { message ->
                Snackbar(
                    action = {
                        TextButton(onClick = { viewModel.clearStatusMessage() }) {
                            Text("Dismiss")
                        }
                    }
                ) {
                    Text(message)
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Agent.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Screen.Agent.route) {
                AgentScreen(viewModel = viewModel)
            }
            composable(Screen.Files.route) {
                FilesScreen(viewModel = viewModel)
            }
            composable(Screen.Organize.route) {
                OrganizeScreen(viewModel = viewModel)
            }
            composable(Screen.Telegram.route) {
                TelegramScreen(viewModel = viewModel)
            }
        }
    }
}

package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.model.DateBucket
import com.example.ui.TaskPulseViewModel
import com.example.ui.components.TemplatePickerDialog
import com.example.ui.navigation.Screen
import com.example.ui.screens.calendar.CalendarScreen
import com.example.ui.screens.dashboard.DashboardScreen
import com.example.ui.screens.editor.TaskEditorScreen
import com.example.ui.screens.insights.InsightsScreen
import com.example.ui.screens.onboarding.OnboardingScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.tasks.TaskListScreen
import com.example.ui.theme.TaskPulseTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = (application as? TaskPulseApplication) ?: TaskPulseApplication.instance

        setContent {
            val viewModel: TaskPulseViewModel = viewModel(
                factory = TaskPulseViewModel.factory(app)
            )
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val systemInDark = androidx.compose.foundation.isSystemInDarkTheme()
            val isDarkTheme = when (uiState.themeMode) {
                "DARK" -> true
                "LIGHT" -> false
                else -> systemInDark
            }

            TaskPulseTheme(
                darkTheme = isDarkTheme,
                dynamicColor = uiState.useDynamicColor
            ) {
                // Request POST_NOTIFICATIONS on Android 13+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val permissionLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestPermission()
                    ) { isGranted ->
                        // Permission result handled gracefully
                    }

                    LaunchedEffect(Unit) {
                        val isGranted = ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED
                        if (!isGranted) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }

                TaskPulseAppRoot(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun TaskPulseAppRoot(viewModel: TaskPulseViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var showTemplatePicker by remember { mutableStateOf(false) }

    // Listen for ViewModel snackbar messages & undo
    LaunchedEffect(uiState.userMessage) {
        val msg = uiState.userMessage
        if (msg != null) {
            val hasUndo = msg.contains("trash", ignoreCase = true)
            val result = snackbarHostState.showSnackbar(
                message = msg,
                actionLabel = if (hasUndo) "Undo" else null,
                duration = if (hasUndo) SnackbarDuration.Short else SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed && hasUndo) {
                viewModel.undoDelete()
            }
            viewModel.clearUserMessage()
        }
    }

    // Determine initial route based on onboarding preference
    val startDestination = remember {
        if (!uiState.onboardingCompleted && false) "onboarding" else Screen.Dashboard.route
    }

    val isTopLevelDestination = Screen.bottomNavScreens.any { it.route == currentRoute }

    BackHandler(enabled = isTopLevelDestination && currentRoute != Screen.Dashboard.route) {
        viewModel.clearFilters()
        navController.navigate(Screen.Dashboard.route) {
            popUpTo(navController.graph.findStartDestination().id) {
                inclusive = false
                saveState = false
            }
            launchSingleTop = true
            restoreState = false
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 600.dp

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                if (!isWideScreen && isTopLevelDestination) {
                    NavigationBar(
                        tonalElevation = 8.dp,
                        modifier = Modifier
                            .navigationBarsPadding()
                            .testTag("bottom_nav_bar")
                    ) {
                        Screen.bottomNavScreens.forEach { screen ->
                            val selected = currentRoute == screen.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    if (screen.route == Screen.Dashboard.route) {
                                        viewModel.clearFilters()
                                        navController.navigate(Screen.Dashboard.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                inclusive = false
                                                saveState = false
                                            }
                                            launchSingleTop = true
                                            restoreState = false
                                        }
                                    } else {
                                        navController.navigate(screen.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) screen.selectedIcon else screen.unselectedIcon,
                                        contentDescription = screen.title,
                                        modifier = Modifier.size(24.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        text = screen.title,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        softWrap = false,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                alwaysShowLabel = true,
                                modifier = Modifier.testTag("nav_${screen.route}")
                            )
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            Row(modifier = Modifier.fillMaxSize()) {
                if (isWideScreen && isTopLevelDestination) {
                    NavigationRail(
                        modifier = Modifier.testTag("navigation_rail")
                    ) {
                        Screen.bottomNavScreens.forEach { screen ->
                            val selected = currentRoute == screen.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = {
                                    if (screen.route == Screen.Dashboard.route) {
                                        viewModel.clearFilters()
                                        navController.navigate(Screen.Dashboard.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                inclusive = false
                                                saveState = false
                                            }
                                            launchSingleTop = true
                                            restoreState = false
                                        }
                                    } else {
                                        navController.navigate(screen.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) screen.selectedIcon else screen.unselectedIcon,
                                        contentDescription = screen.title,
                                        modifier = Modifier.size(24.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        text = screen.title,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        softWrap = false,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = startDestination,
                    modifier = Modifier
                        .weight(1f)
                        .padding(
                            bottom = if (!isWideScreen && isTopLevelDestination) innerPadding.calculateBottomPadding() else 0.dp
                        )
                ) {
                    composable(Screen.Dashboard.route) {
                        DashboardScreen(
                            uiState = uiState,
                            viewModel = viewModel,
                            onNavigateToTasks = { bucket: DateBucket? ->
                                viewModel.setFilterBucket(bucket)
                                navController.navigate(Screen.Tasks.route)
                            },
                            onNavigateToEditor = { taskId: Long? ->
                                val route = if (taskId != null) "task_editor?taskId=$taskId" else "task_editor"
                                navController.navigate(route)
                            },
                            onOpenTemplates = {
                                showTemplatePicker = true
                            }
                        )
                    }

                    composable(Screen.Tasks.route) {
                        TaskListScreen(
                            uiState = uiState,
                            viewModel = viewModel,
                            onNavigateToEditor = { taskId: Long? ->
                                val route = if (taskId != null) "task_editor?taskId=$taskId" else "task_editor"
                                navController.navigate(route)
                            }
                        )
                    }

                    composable(Screen.Calendar.route) {
                        CalendarScreen(
                            uiState = uiState,
                            viewModel = viewModel,
                            onNavigateToEditor = { taskId: Long ->
                                navController.navigate("task_editor?taskId=$taskId")
                            }
                        )
                    }

                    composable(Screen.Insights.route) {
                        InsightsScreen(uiState = uiState)
                    }

                    composable(Screen.Settings.route) {
                        SettingsScreen(
                            uiState = uiState,
                            viewModel = viewModel,
                            onOpenOnboarding = {
                                navController.navigate("onboarding")
                            }
                        )
                    }

                    composable("task_editor") {
                        TaskEditorScreen(
                            taskId = null,
                            uiState = uiState,
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }

                    composable(
                        route = "task_editor?taskId={taskId}",
                        arguments = listOf(
                            navArgument("taskId") {
                                type = NavType.LongType
                                defaultValue = -1L
                            }
                        )
                    ) { backStackEntry ->
                        val taskIdArg = backStackEntry.arguments?.getLong("taskId") ?: -1L
                        val taskId = if (taskIdArg > 0) taskIdArg else null
                        TaskEditorScreen(
                            taskId = taskId,
                            uiState = uiState,
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }

                    composable("onboarding") {
                        OnboardingScreen(
                            onFinish = {
                                viewModel.setOnboardingCompleted(true)
                                navController.navigate(Screen.Dashboard.route) {
                                    popUpTo("onboarding") { inclusive = true }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showTemplatePicker) {
        TemplatePickerDialog(
            onDismiss = { showTemplatePicker = false },
            onSelectTemplate = { prefilledTask ->
                viewModel.createTask(prefilledTask)
                showTemplatePicker = false
            }
        )
    }
}

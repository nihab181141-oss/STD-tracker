package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModelProvider
import com.example.data.local.AppDatabase
import com.example.data.repository.AppRepository
import com.example.ui.screens.AcademicScreen
import com.example.ui.screens.AiCoachScreen
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.GoalsScreen
import com.example.ui.screens.GroupsScreen
import com.example.ui.screens.HabitsScreen
import com.example.ui.screens.NotificationsScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.StudyTimerScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.TrackerViewModel
import com.example.ui.viewmodel.TrackerViewModelFactory

sealed class Screen(val title: String, val icon: ImageVector) {
    object Dashboard : Screen("Dashboard", Icons.Default.Home)
    object Timer : Screen("Focus", Icons.Default.Timer)
    object Habits : Screen("Habits", Icons.Default.TaskAlt)
    object Academic : Screen("Academic", Icons.Default.School)
    object Goals : Screen("Goals", Icons.Default.Flag)
    object Analytics : Screen("Analytics", Icons.Default.BarChart)
    object Coach : Screen("AI Coach", Icons.Default.AutoAwesome)
    object Profile : Screen("Profile", Icons.Default.Person)
    object Groups : Screen("Groups", Icons.Default.Group)
    object Notifications : Screen("Notifications", Icons.Default.Notifications)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Database and Repository Core Setup
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = AppRepository(database.trackerDao())
        val factory = TrackerViewModelFactory(repository)
        val viewModel = ViewModelProvider(this, factory)[TrackerViewModel::class.java]

        setContent {
            MyApplicationTheme {
                var currentScreen by remember { mutableStateOf<Screen>(Screen.Dashboard) }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        NavigationBar(
                            containerColor = Color(0xFF0A0A0F),
                            tonalElevation = 0.dp,
                            modifier = Modifier.background(Color(0xFF0A0A0F))
                        ) {
                            val navDestinations = listOf(
                                Screen.Dashboard,
                                Screen.Timer,
                                Screen.Habits,
                                Screen.Academic,
                                Screen.Coach
                            )
                            navDestinations.forEach { screen ->
                                val selected = currentScreen == screen || 
                                               (screen == Screen.Dashboard && (currentScreen == Screen.Goals || currentScreen == Screen.Analytics || currentScreen == Screen.Profile || currentScreen == Screen.Groups || currentScreen == Screen.Notifications))
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { currentScreen = screen },
                                    icon = {
                                        Icon(
                                            imageVector = screen.icon,
                                            contentDescription = screen.title,
                                            modifier = Modifier.padding(bottom = 2.dp)
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = screen.title,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.2).sp)
                                        )
                                    },
                                    colors = NavigationBarItemColors()
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                            .padding(innerPadding)
                    ) {
                        AnimatedContent(
                            targetState = currentScreen,
                            transitionSpec = {
                                fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
                            },
                            label = "ScreenTransition"
                        ) { screen ->
                            when (screen) {
                                Screen.Dashboard -> DashboardScreen(
                                    viewModel = viewModel,
                                    onNavigateToTimer = { currentScreen = Screen.Timer },
                                    onNavigateToHabits = { currentScreen = Screen.Habits },
                                    onNavigateToGoals = { currentScreen = Screen.Goals },
                                    onNavigateToAnalytics = { currentScreen = Screen.Analytics },
                                    onNavigateToProfile = { currentScreen = Screen.Profile },
                                    onNavigateToGroups = { currentScreen = Screen.Groups },
                                    onNavigateToNotifications = { currentScreen = Screen.Notifications }
                                )
                                Screen.Timer -> StudyTimerScreen(viewModel = viewModel)
                                Screen.Habits -> HabitsScreen(viewModel = viewModel)
                                Screen.Academic -> AcademicScreen(viewModel = viewModel)
                                Screen.Goals -> GoalsScreen(viewModel = viewModel)
                                Screen.Analytics -> AnalyticsScreen(viewModel = viewModel)
                                Screen.Coach -> AiCoachScreen(viewModel = viewModel)
                                Screen.Profile -> ProfileScreen(viewModel = viewModel)
                                Screen.Groups -> GroupsScreen(viewModel = viewModel, onNavigateBack = { currentScreen = Screen.Dashboard })
                                Screen.Notifications -> NotificationsScreen(viewModel = viewModel, onNavigateBack = { currentScreen = Screen.Dashboard })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NavigationBarItemColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = MaterialTheme.colorScheme.secondary,
    selectedTextColor = MaterialTheme.colorScheme.secondary,
    indicatorColor = Color.Transparent,
    unselectedIconColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
    unselectedTextColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
)

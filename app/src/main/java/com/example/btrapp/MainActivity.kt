package com.example.btrapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.btrapp.blocking.AppLockRoutes
import com.example.btrapp.blocking.appLockGraph
import com.example.btrapp.core.ui.theme.MyApplicationTheme
import com.example.btrapp.habits.HabitsRoutes
import com.example.btrapp.habits.habitsGraph
import com.example.btrapp.home.HubScreen
import com.example.btrapp.ui.welcome.WelcomeScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val prefs = remember { getSharedPreferences(PREFS, MODE_PRIVATE) }
                var showWelcome by rememberSaveable {
                    mutableStateOf(!prefs.getBoolean(KEY_WELCOME_SEEN, false))
                }
                if (showWelcome) {
                    WelcomeScreen(onStart = {
                        prefs.edit { putBoolean(KEY_WELCOME_SEEN, true) }
                        showWelcome = false
                    })
                } else {
                    BtrApp()
                }
            }
        }
    }
}

private const val PREFS = "btr_prefs"
private const val KEY_WELCOME_SEEN = "welcome_seen"

private enum class Tab(val route: String, val label: String, val icon: ImageVector) {
    HOME("home", "Home", Icons.Default.Home),
    HABITS(HabitsRoutes.HABITS, "Habits", Icons.Default.CheckCircle),
    LOCK(AppLockRoutes.LOCK, "Lock", Icons.Default.Lock),
}

private val fullScreenRoutes = AppLockRoutes.fullScreen + HabitsRoutes.fullScreen

@Composable
fun BtrApp() {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    Scaffold(
        bottomBar = {
            if (currentRoute !in fullScreenRoutes) {
                NavigationBar {
                    Tab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = { navController.navigateToTab(tab.route) },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Tab.HOME.route,
            modifier = Modifier.padding(padding),
        ) {
            composable(Tab.HOME.route) {
                HubScreen(
                    onOpenHabits = { navController.navigateToTab(Tab.HABITS.route) },
                    onOpenLock = { navController.navigateToTab(Tab.LOCK.route) },
                )
            }
            habitsGraph(navController)
            appLockGraph(navController)
        }
    }
}

private fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

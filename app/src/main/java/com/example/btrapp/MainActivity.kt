package com.example.btrapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
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
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.btrapp.blocking.ui.apps.AppsScreen
import com.example.btrapp.blocking.ui.history.HistoryScreen
import com.example.btrapp.blocking.ui.home.HomeScreen
import com.example.btrapp.blocking.ui.rules.RuleEditorScreen
import com.example.btrapp.blocking.ui.rules.RuleEditorViewModel
import com.example.btrapp.blocking.ui.rules.RulesScreen
import com.example.btrapp.ui.theme.MyApplicationTheme
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
    APPS("apps", "Apps", Icons.Default.Lock),
    RULES("rules", "Rules", Icons.Default.DateRange),
    HISTORY("history", "History", Icons.AutoMirrored.Filled.List),
}

private const val RULE_EDITOR_ROUTE = "rule/{${RuleEditorViewModel.ARG_RULE_ID}}"

private fun ruleEditorRoute(id: Long) = "rule/$id"

@Composable
fun BtrApp() {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    Scaffold(
        bottomBar = {
            if (currentRoute != RULE_EDITOR_ROUTE) {
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
                HomeScreen(onManageApps = { navController.navigateToTab(Tab.APPS.route) })
            }
            composable(Tab.APPS.route) { AppsScreen() }
            composable(Tab.RULES.route) {
                RulesScreen(
                    onEditRule = { navController.navigate(ruleEditorRoute(it)) },
                    onNewRule = {
                        navController.navigate(ruleEditorRoute(RuleEditorViewModel.NEW_RULE))
                    },
                )
            }
            composable(
                RULE_EDITOR_ROUTE,
                arguments = listOf(
                    navArgument(RuleEditorViewModel.ARG_RULE_ID) { type = NavType.LongType },
                ),
            ) {
                RuleEditorScreen(onDone = { navController.popBackStack() })
            }
            composable(Tab.HISTORY.route) { HistoryScreen() }
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

package com.example.btrapp.blocking

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.btrapp.blocking.ui.apps.AppsScreen
import com.example.btrapp.blocking.ui.lock.LockScreen
import com.example.btrapp.blocking.ui.rules.RuleEditorScreen
import com.example.btrapp.blocking.ui.rules.RuleEditorViewModel
import com.example.btrapp.blocking.ui.rules.SessionsScreen

object AppLockRoutes {
    /** The Lock tab. */
    const val LOCK = "lock"
    const val APPS = "lock/apps"
    const val SESSIONS = "lock/sessions"
    const val RULE_EDITOR = "lock/rule/{${RuleEditorViewModel.ARG_RULE_ID}}"

    fun ruleEditor(id: Long) = "lock/rule/$id"

    /** Pushed screens that cover the whole window, without the host's bottom bar. */
    val fullScreen = setOf(APPS, SESSIONS, RULE_EDITOR)
}

fun NavGraphBuilder.appLockGraph(navController: NavHostController) {
    composable(AppLockRoutes.LOCK) {
        LockScreen(
            onManageApps = { navController.navigate(AppLockRoutes.APPS) },
            onEditRule = { navController.navigate(AppLockRoutes.ruleEditor(it)) },
            onNewRule = { navController.navigate(AppLockRoutes.ruleEditor(RuleEditorViewModel.NEW_RULE)) },
            onOpenSessions = { navController.navigate(AppLockRoutes.SESSIONS) },
        )
    }
    composable(AppLockRoutes.APPS) {
        AppsScreen(onBack = { navController.popBackStack() })
    }
    composable(AppLockRoutes.SESSIONS) {
        SessionsScreen(onBack = { navController.popBackStack() })
    }
    composable(
        AppLockRoutes.RULE_EDITOR,
        arguments = listOf(navArgument(RuleEditorViewModel.ARG_RULE_ID) { type = NavType.LongType }),
    ) {
        RuleEditorScreen(onDone = { navController.popBackStack() })
    }
}

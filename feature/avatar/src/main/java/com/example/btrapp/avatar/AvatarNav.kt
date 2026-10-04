package com.example.btrapp.avatar

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable

object AvatarRoutes {
    const val EDITOR = "avatar/edit"

    /** Pushed screens that cover the whole window, without the host's bottom bar. */
    val fullScreen = setOf(EDITOR)
}

fun NavGraphBuilder.avatarGraph(navController: NavHostController) {
    composable(AvatarRoutes.EDITOR) {
        AvatarEditorScreen(onDone = { navController.popBackStack() })
    }
}

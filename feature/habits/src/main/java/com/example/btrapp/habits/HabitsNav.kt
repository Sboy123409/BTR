package com.example.btrapp.habits

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.btrapp.habits.ui.HabitEditorScreen
import com.example.btrapp.habits.ui.HabitEditorViewModel
import com.example.btrapp.habits.ui.HabitsScreen
import com.example.btrapp.habits.ui.ManageHabitsScreen

object HabitsRoutes {
    /** The Habits tab. */
    const val HABITS = "habits"
    const val MANAGE = "habits/manage"
    const val EDITOR = "habits/habit/{${HabitEditorViewModel.ARG_HABIT_ID}}"

    fun editor(id: Long) = "habits/habit/$id"

    /** Pushed screens that cover the whole window, without the host's bottom bar. */
    val fullScreen = setOf(MANAGE, EDITOR)
}

fun NavGraphBuilder.habitsGraph(navController: NavHostController) {
    val newHabit = { navController.navigate(HabitsRoutes.editor(HabitEditorViewModel.NEW_HABIT)) }
    composable(HabitsRoutes.HABITS) {
        HabitsScreen(
            onNewHabit = newHabit,
            onManageHabits = { navController.navigate(HabitsRoutes.MANAGE) },
        )
    }
    composable(HabitsRoutes.MANAGE) {
        ManageHabitsScreen(
            onBack = { navController.popBackStack() },
            onEditHabit = { navController.navigate(HabitsRoutes.editor(it)) },
            onNewHabit = newHabit,
        )
    }
    composable(
        HabitsRoutes.EDITOR,
        arguments = listOf(navArgument(HabitEditorViewModel.ARG_HABIT_ID) { type = NavType.LongType }),
    ) {
        HabitEditorScreen(onDone = { navController.popBackStack() })
    }
}

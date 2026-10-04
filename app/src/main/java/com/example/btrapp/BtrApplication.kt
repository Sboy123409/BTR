package com.example.btrapp

import android.app.Application
import com.example.btrapp.blocking.AppLockGraph
import com.example.btrapp.blocking.AppLockGraphProvider
import com.example.btrapp.habits.HabitsGraph
import com.example.btrapp.habits.HabitsGraphProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Creates each feature module's dependency graph once, for the life of the process. */
class BtrApplication : Application(), AppLockGraphProvider, HabitsGraphProvider {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override lateinit var appLockGraph: AppLockGraph
        private set

    override val habitsGraph: HabitsGraph by lazy { HabitsGraph(this) }

    override fun onCreate() {
        super.onCreate()
        appLockGraph = AppLockGraph(this, appScope)
    }
}

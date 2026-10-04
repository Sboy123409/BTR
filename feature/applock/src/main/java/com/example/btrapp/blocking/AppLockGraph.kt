package com.example.btrapp.blocking

import android.content.Context
import androidx.room.Room
import com.example.btrapp.blocking.data.BlockingRepository
import com.example.btrapp.blocking.data.BtrDatabase
import com.example.btrapp.blocking.domain.InstalledAppsProvider
import kotlinx.coroutines.CoroutineScope

/** The App Lock module's dependencies. The host app creates one and exposes it via [AppLockGraphProvider]. */
class AppLockGraph(context: Context, scope: CoroutineScope) {
    // Kept as "btr.db" from before App Lock became its own module, so existing data carries over.
    private val database = Room.databaseBuilder(context, BtrDatabase::class.java, "btr.db").build()

    val blockingRepository = BlockingRepository(database.blockingDao(), scope)

    val installedApps = InstalledAppsProvider(context)
}

/** Implemented by the [android.app.Application] so the module's screens and service can reach [AppLockGraph]. */
interface AppLockGraphProvider {
    val appLockGraph: AppLockGraph
}

val Context.appLockGraph: AppLockGraph
    get() = (applicationContext as AppLockGraphProvider).appLockGraph

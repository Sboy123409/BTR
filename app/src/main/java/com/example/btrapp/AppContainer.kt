package com.example.btrapp

import android.content.Context
import androidx.room.Room
import com.example.btrapp.blocking.data.BlockingRepository
import com.example.btrapp.blocking.data.BtrDatabase
import com.example.btrapp.blocking.domain.InstalledAppsProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Manual dependency container; one instance lives on [BtrApplication]. */
class AppContainer(context: Context) {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val database = Room.databaseBuilder(context, BtrDatabase::class.java, "btr.db").build()

    val blockingRepository = BlockingRepository(database.blockingDao(), appScope)

    val installedApps = InstalledAppsProvider(context)
}

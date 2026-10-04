package com.example.btrapp.blocking.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [BlockedApp::class, LockRule::class, LockRuleApp::class, EmergencyUnlock::class],
    version = 1,
)
abstract class BtrDatabase : RoomDatabase() {
    abstract fun blockingDao(): BlockingDao
}

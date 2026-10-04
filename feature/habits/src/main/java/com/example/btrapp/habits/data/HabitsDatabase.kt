package com.example.btrapp.habits.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Habit::class, HabitCheck::class, DayTask::class], version = 1)
abstract class HabitsDatabase : RoomDatabase() {
    abstract fun habitsDao(): HabitsDao
}

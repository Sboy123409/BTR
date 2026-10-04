package com.example.btrapp.habits

import android.content.Context
import androidx.room.Room
import com.example.btrapp.habits.data.HabitsDatabase
import com.example.btrapp.habits.data.HabitsRepository

/** The Habits module's dependencies. The host app creates one and exposes it via [HabitsGraphProvider]. */
class HabitsGraph(context: Context) {
    private val database =
        Room.databaseBuilder(context, HabitsDatabase::class.java, "habits.db").build()

    val repository = HabitsRepository(database.habitsDao())
}

/** Implemented by the [android.app.Application] so the module's screens can reach [HabitsGraph]. */
interface HabitsGraphProvider {
    val habitsGraph: HabitsGraph
}

val Context.habitsGraph: HabitsGraph
    get() = (applicationContext as HabitsGraphProvider).habitsGraph

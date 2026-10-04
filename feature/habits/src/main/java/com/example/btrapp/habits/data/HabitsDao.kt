package com.example.btrapp.habits.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitsDao {

    @Query("SELECT * FROM habits ORDER BY id")
    fun observeHabits(): Flow<List<Habit>>

    @Query("SELECT * FROM habits WHERE id = :id")
    suspend fun getHabit(id: Long): Habit?

    @Insert
    suspend fun insertHabit(habit: Habit): Long

    @Update
    suspend fun updateHabit(habit: Habit)

    @Query("DELETE FROM habits WHERE id = :id")
    suspend fun deleteHabit(id: Long)

    @Query("SELECT * FROM habit_checks")
    fun observeChecks(): Flow<List<HabitCheck>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCheck(check: HabitCheck)

    @Query("DELETE FROM habit_checks WHERE habitId = :habitId AND epochDay = :epochDay")
    suspend fun deleteCheck(habitId: Long, epochDay: Long)

    @Query("SELECT * FROM tasks WHERE epochDay = :epochDay ORDER BY createdAt")
    fun observeTasks(epochDay: Long): Flow<List<DayTask>>

    @Insert
    suspend fun insertTask(task: DayTask): Long

    @Query("UPDATE tasks SET done = :done WHERE id = :id")
    suspend fun setTaskDone(id: Long, done: Boolean)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTask(id: Long)
}

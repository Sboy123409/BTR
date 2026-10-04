package com.example.btrapp.habits.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.btrapp.core.DaysMask

/**
 * A recurring habit. It's due on the days in [daysMask] (see [DaysMask]) from [startDay] on.
 * Days are `LocalDate.toEpochDay()` values throughout this module.
 */
@Entity(tableName = "habits")
data class Habit(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val emoji: String? = null,
    val daysMask: Int = DaysMask.EVERY_DAY,
    val startDay: Long,
    val createdAt: Long,
)

/** One habit ticked off on one day. */
@Entity(
    tableName = "habit_checks",
    primaryKeys = ["habitId", "epochDay"],
    foreignKeys = [
        ForeignKey(
            entity = Habit::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class HabitCheck(
    val habitId: Long,
    val epochDay: Long,
    val checkedAt: Long,
)

/** A one-off to-do planned for a single day. */
@Entity(tableName = "tasks", indices = [Index("epochDay")])
data class DayTask(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val epochDay: Long,
    val done: Boolean = false,
    val createdAt: Long,
)

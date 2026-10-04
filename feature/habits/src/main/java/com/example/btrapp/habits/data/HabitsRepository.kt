package com.example.btrapp.habits.data

import com.example.btrapp.habits.domain.HabitSchedule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.ZoneId

data class HabitItem(val habit: Habit, val checked: Boolean, val streak: Int)

/** Everything on one day's checklist: the habits due that day and that day's to-dos. */
data class DayChecklist(
    val epochDay: Long,
    val habits: List<HabitItem>,
    val tasks: List<DayTask>,
) {
    val total: Int get() = habits.size + tasks.size
    val done: Int get() = habits.count { it.checked } + tasks.count { it.done }
}

data class HabitSummary(val habit: Habit, val currentStreak: Int, val bestStreak: Int)

class HabitsRepository(
    private val dao: HabitsDao,
    private val clock: () -> Long = System::currentTimeMillis,
    private val zone: () -> ZoneId = ZoneId::systemDefault,
) {
    fun today(): Long = Instant.ofEpochMilli(clock()).atZone(zone()).toLocalDate().toEpochDay()

    private val checksByHabit: Flow<Map<Long, Set<Long>>> = dao.observeChecks().map { checks ->
        checks.groupBy({ it.habitId }, { it.epochDay }).mapValues { it.value.toSet() }
    }

    fun checklistFor(epochDay: Long): Flow<DayChecklist> =
        combine(dao.observeHabits(), checksByHabit, dao.observeTasks(epochDay)) { habits, checks, tasks ->
            val today = today()
            DayChecklist(
                epochDay = epochDay,
                habits = habits
                    .filter { HabitSchedule.isDue(it.daysMask, it.startDay, epochDay) }
                    .map { habit ->
                        val days = checks[habit.id].orEmpty()
                        HabitItem(
                            habit = habit,
                            checked = epochDay in days,
                            streak = HabitSchedule.currentStreak(
                                habit.daysMask, habit.startDay, days, minOf(epochDay, today),
                            ),
                        )
                    },
                tasks = tasks,
            )
        }

    fun todayChecklist(): Flow<DayChecklist> = checklistFor(today())

    val summaries: Flow<List<HabitSummary>> =
        combine(dao.observeHabits(), checksByHabit) { habits, checks ->
            val today = today()
            habits.map { habit ->
                val days = checks[habit.id].orEmpty()
                HabitSummary(
                    habit = habit,
                    currentStreak = HabitSchedule.currentStreak(habit.daysMask, habit.startDay, days, today),
                    bestStreak = HabitSchedule.bestStreak(habit.daysMask, habit.startDay, days, today),
                )
            }
        }

    /** Future days can't be ticked off. */
    suspend fun setHabitChecked(habitId: Long, epochDay: Long, checked: Boolean) {
        if (checked) {
            if (epochDay > today()) return
            dao.insertCheck(HabitCheck(habitId, epochDay, clock()))
        } else {
            dao.deleteCheck(habitId, epochDay)
        }
    }

    suspend fun getHabit(id: Long): Habit? = dao.getHabit(id)

    suspend fun saveHabit(id: Long, title: String, emoji: String?, daysMask: Int) {
        val existing = if (id == 0L) null else dao.getHabit(id)
        if (existing == null) {
            dao.insertHabit(
                Habit(title = title, emoji = emoji, daysMask = daysMask, startDay = today(), createdAt = clock())
            )
        } else {
            dao.updateHabit(existing.copy(title = title, emoji = emoji, daysMask = daysMask))
        }
    }

    suspend fun deleteHabit(id: Long) = dao.deleteHabit(id)

    suspend fun addTask(title: String, epochDay: Long) {
        dao.insertTask(DayTask(title = title, epochDay = epochDay, createdAt = clock()))
    }

    suspend fun setTaskDone(id: Long, done: Boolean) = dao.setTaskDone(id, done)

    suspend fun deleteTask(id: Long) = dao.deleteTask(id)
}

package com.example.btrapp.habits.domain

import com.example.btrapp.core.DaysMask
import java.time.LocalDate

/** Due days and streaks. Days are `LocalDate.toEpochDay()` values. */
object HabitSchedule {

    fun isDue(daysMask: Int, startDay: Long, epochDay: Long): Boolean =
        epochDay >= startDay &&
            DaysMask.isDayOn(daysMask, LocalDate.ofEpochDay(epochDay).dayOfWeek.value)

    /**
     * Consecutive due days ticked off, counting back from [today]. Today not being ticked yet
     * doesn't break the streak (the day isn't over); days the habit isn't due are skipped.
     */
    fun currentStreak(daysMask: Int, startDay: Long, checked: Set<Long>, today: Long): Int {
        if (daysMask == 0) return 0
        var day = if (today in checked) today else today - 1
        var streak = 0
        while (day >= startDay) {
            if (isDue(daysMask, startDay, day)) {
                if (day !in checked) break
                streak++
            }
            day--
        }
        return streak
    }

    /** Longest run of consecutive due days ticked off, up to [today]. */
    fun bestStreak(daysMask: Int, startDay: Long, checked: Set<Long>, today: Long): Int {
        var best = 0
        var run = 0
        for (day in startDay..today) {
            if (!isDue(daysMask, startDay, day)) continue
            if (day in checked) {
                run++
                best = maxOf(best, run)
            } else if (day != today) {
                run = 0
            }
        }
        return best
    }
}

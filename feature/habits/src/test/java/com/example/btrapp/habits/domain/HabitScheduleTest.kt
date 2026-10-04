package com.example.btrapp.habits.domain

import com.example.btrapp.core.DaysMask
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class HabitScheduleTest {

    // Mon 2026-09-28 … Sun 2026-10-04
    private val mon = LocalDate.of(2026, 9, 28).toEpochDay()
    private fun day(offset: Int) = mon + offset

    @Test
    fun weekdayHabitIsNotDueAtWeekend() {
        assertTrue(HabitSchedule.isDue(DaysMask.WEEKDAYS, mon, day(4)))
        assertFalse(HabitSchedule.isDue(DaysMask.WEEKDAYS, mon, day(5)))
        assertFalse(HabitSchedule.isDue(DaysMask.WEEKDAYS, mon, day(6)))
    }

    @Test
    fun notDueBeforeItStarted() {
        assertFalse(HabitSchedule.isDue(DaysMask.EVERY_DAY, day(2), day(1)))
        assertTrue(HabitSchedule.isDue(DaysMask.EVERY_DAY, day(2), day(2)))
    }

    @Test
    fun uncheckedTodayDoesNotBreakStreak() {
        val checked = setOf(day(0), day(1), day(2))
        assertEquals(3, HabitSchedule.currentStreak(DaysMask.EVERY_DAY, mon, checked, today = day(3)))
        assertEquals(4, HabitSchedule.currentStreak(DaysMask.EVERY_DAY, mon, checked + day(3), today = day(3)))
    }

    @Test
    fun missedDayBreaksStreak() {
        val checked = setOf(day(0), day(2), day(3))
        assertEquals(2, HabitSchedule.currentStreak(DaysMask.EVERY_DAY, mon, checked, today = day(3)))
        assertEquals(2, HabitSchedule.bestStreak(DaysMask.EVERY_DAY, mon, checked, today = day(3)))
    }

    @Test
    fun weekendIsSkippedForWeekdayHabit() {
        // Thu, Fri, then next Mon: the weekend in between doesn't count against it.
        val checked = setOf(day(3), day(4), day(7))
        assertEquals(3, HabitSchedule.currentStreak(DaysMask.WEEKDAYS, mon, checked, today = day(7)))
    }

    @Test
    fun habitStartedMidWeekOnlyCountsFromStart() {
        val start = day(3)
        val checked = setOf(day(3), day(4), day(5))
        assertEquals(3, HabitSchedule.currentStreak(DaysMask.EVERY_DAY, start, checked, today = day(6)))
        assertEquals(3, HabitSchedule.bestStreak(DaysMask.EVERY_DAY, start, checked, today = day(6)))
    }

    @Test
    fun bestStreakSurvivesLaterGap() {
        val checked = setOf(day(0), day(1), day(2), day(4))
        assertEquals(3, HabitSchedule.bestStreak(DaysMask.EVERY_DAY, mon, checked, today = day(5)))
        assertEquals(0, HabitSchedule.currentStreak(DaysMask.EVERY_DAY, mon, checked, today = day(6)))
    }
}

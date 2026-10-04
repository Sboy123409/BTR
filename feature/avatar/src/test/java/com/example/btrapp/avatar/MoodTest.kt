package com.example.btrapp.avatar

import org.junit.Assert.assertEquals
import org.junit.Test

class MoodTest {
    @Test
    fun allDoneIsEcstatic() = assertEquals(Mood.Ecstatic, Mood.from(done = 3, total = 3, hourOfDay = 23))

    @Test
    fun someDoneIsHappy() = assertEquals(Mood.Happy, Mood.from(done = 1, total = 3, hourOfDay = 23))

    @Test
    fun nothingDoneLateIsSleepy() = assertEquals(Mood.Sleepy, Mood.from(done = 0, total = 3, hourOfDay = 22))

    @Test
    fun nothingDoneDaytimeIsNeutral() = assertEquals(Mood.Neutral, Mood.from(done = 0, total = 3, hourOfDay = 14))

    @Test
    fun emptyDayIsNotEcstatic() = assertEquals(Mood.Neutral, Mood.from(done = 0, total = 0, hourOfDay = 14))
}

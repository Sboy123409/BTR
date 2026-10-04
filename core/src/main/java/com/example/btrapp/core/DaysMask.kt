package com.example.btrapp.core

/**
 * Days of the week packed into an Int: bit 0 = Monday … bit 6 = Sunday (ISO day - 1).
 * Shared by lock schedules and habits.
 */
object DaysMask {
    const val EVERY_DAY = 0b1111111
    const val WEEKDAYS = 0b0011111
    const val WEEKENDS = 0b1100000

    fun isDayOn(mask: Int, isoDay: Int): Boolean = mask and (1 shl (isoDay - 1)) != 0

    fun toggle(mask: Int, isoDay: Int): Int = mask xor (1 shl (isoDay - 1))
}

package com.example.btrapp.blocking.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ExerciseCatalogTest {

    @Test
    fun `amount scales with unlock duration`() {
        assertEquals(10, ExerciseCatalog.amount(Exercise.PUSH_UPS, 5, 0))
        assertEquals(20, ExerciseCatalog.amount(Exercise.PUSH_UPS, 10, 0))
        assertEquals(30, ExerciseCatalog.amount(Exercise.PUSH_UPS, 15, 0))
    }

    @Test
    fun `amount escalates with earlier unlocks today`() {
        assertEquals(15, ExerciseCatalog.amount(Exercise.PUSH_UPS, 5, 1))
        assertEquals(20, ExerciseCatalog.amount(Exercise.PUSH_UPS, 5, 2))
        // Burpees: 5 × 1.5 = 7.5, rounded up.
        assertEquals(8, ExerciseCatalog.amount(Exercise.BURPEES, 5, 1))
    }

    @Test
    fun `minimum seconds follows per-rep pace`() {
        assertEquals(20, ExerciseCatalog.minimumSeconds(Exercise.PUSH_UPS, 10))
        assertEquals(30, ExerciseCatalog.minimumSeconds(Exercise.SQUATS, 20))
        assertEquals(30, ExerciseCatalog.minimumSeconds(Exercise.PLANK, 30))
    }
}

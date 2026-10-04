package com.example.btrapp.blocking.domain

import kotlin.math.ceil

enum class ExerciseUnit { REPS, SECONDS }

enum class Exercise(
    val displayName: String,
    val baseAmount: Int,
    val unit: ExerciseUnit,
    /** Minimum time a single rep (or second) is allowed to take; drives the countdown. */
    val secondsPerUnit: Double,
    val instructions: String,
) {
    PUSH_UPS("Push-ups", 10, ExerciseUnit.REPS, 2.0, "Chest to the floor, full lockout at the top."),
    SIT_UPS("Sit-ups", 15, ExerciseUnit.REPS, 2.0, "Knees bent, shoulders off the floor each rep."),
    SQUATS("Squats", 20, ExerciseUnit.REPS, 1.5, "Hips below knees, stand all the way up."),
    JUMPING_JACKS("Jumping jacks", 30, ExerciseUnit.REPS, 1.0, "Arms overhead, feet wide, every rep."),
    PLANK("Plank", 30, ExerciseUnit.SECONDS, 1.0, "Forearms down, body straight. Hold it."),
    BURPEES("Burpees", 5, ExerciseUnit.REPS, 4.0, "Chest to the floor, jump at the top."),
}

object ExerciseCatalog {

    /** Unlock lengths offered to the user, in minutes. */
    val unlockDurations = listOf(5, 10, 15)

    /** Each earlier emergency unlock today adds this fraction of the base amount. */
    const val ESCALATION_PER_UNLOCK = 0.5

    /** ×1 / ×2 / ×3 for 5 / 10 / 15 minute unlocks. */
    fun durationMultiplier(durationMin: Int): Int = (durationMin / 5).coerceIn(1, 3)

    fun amount(exercise: Exercise, durationMin: Int, priorUnlocksToday: Int): Int {
        val escalation = 1.0 + ESCALATION_PER_UNLOCK * priorUnlocksToday.coerceAtLeast(0)
        return ceil(exercise.baseAmount * durationMultiplier(durationMin) * escalation).toInt()
    }

    fun minimumSeconds(exercise: Exercise, amount: Int): Int =
        ceil(amount * exercise.secondsPerUnit).toInt()

    fun describe(exercise: Exercise, amount: Int): String = when (exercise.unit) {
        ExerciseUnit.REPS -> "$amount ${exercise.displayName.lowercase()}"
        ExerciseUnit.SECONDS -> "${amount}s ${exercise.displayName.lowercase()}"
    }
}

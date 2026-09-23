package com.workout.tracker.data

import java.time.LocalDate

/** A single day's assignment in the rotation. */
sealed interface DayPlan {
    val date: LocalDate
    data class Rest(override val date: LocalDate) : DayPlan
    data class Train(override val date: LocalDate, val workout: Workout, val rotationIndex: Int) : DayPlan
    data class Before(override val date: LocalDate) : DayPlan   // before the schedule starts
}

object Scheduler {
    /** Assign one date, given the ordered workout list (position order). */
    fun planFor(date: LocalDate, config: ScheduleConfig, workouts: List<Workout>): DayPlan {
        if (workouts.isEmpty()) return DayPlan.Before(date)
        val d = date.toEpochDay() - config.startEpochDay
        if (d < 0) return DayPlan.Before(date)
        val cycle = (config.trainOn + config.restLen).coerceAtLeast(1)
        val posInCycle = (d % cycle).toInt()
        if (posInCycle >= config.trainOn) return DayPlan.Rest(date)
        val fullCycles = d / cycle
        val trainingNumber = (fullCycles * config.trainOn + posInCycle).toInt() // 0-based
        val idx = trainingNumber % workouts.size
        return DayPlan.Train(date, workouts[idx], idx)
    }

    /** A contiguous range of days for the agenda view. */
    fun plan(from: LocalDate, days: Int, config: ScheduleConfig, workouts: List<Workout>): List<DayPlan> =
        (0 until days).map { planFor(from.plusDays(it.toLong()), config, workouts) }
}

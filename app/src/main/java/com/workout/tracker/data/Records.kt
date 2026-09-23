package com.workout.tracker.data

/** Personal bests derived from a flat list of an exercise's logged (finished) sets. */
data class PersonalRecords(
    val topSetKg: Double? = null,
    val topSetDate: Long? = null,
    val topVolumeKg: Double? = null,
    val topVolumeDate: Long? = null,
) {
    val hasAny get() = topSetKg != null || topVolumeKg != null
}

fun computeRecords(history: List<SetLogWithSession>): PersonalRecords {
    val done = history.filter { it.done && it.weightKg > 0.0 }
    if (done.isEmpty()) return PersonalRecords()

    // Heaviest single set.
    val topSet = done.maxByOrNull { it.weightKg }

    // Biggest single-session volume (sum of weight*reps within one session).
    val volumeBySession = done.groupBy { it.sessionId }
        .map { (_, sets) ->
            val vol = sets.sumOf { it.weightKg * it.reps }
            vol to sets.first().sessionStartedAt
        }
    val topVolume = volumeBySession.maxByOrNull { it.first }

    return PersonalRecords(
        topSetKg = topSet?.weightKg,
        topSetDate = topSet?.sessionStartedAt,
        topVolumeKg = topVolume?.first,
        topVolumeDate = topVolume?.second,
    )
}

/** One past session's worth of an exercise's sets, for the History tab. */
data class HistorySession(
    val sessionId: Long,
    val workoutName: String,
    val date: Long,
    val sets: List<SetLogWithSession>,
)

fun groupHistory(history: List<SetLogWithSession>): List<HistorySession> =
    history.groupBy { it.sessionId }
        .map { (id, sets) ->
            val first = sets.first()
            HistorySession(id, first.sessionWorkoutName, first.sessionStartedAt, sets.sortedBy { it.setIndex })
        }
        .sortedByDescending { it.date }

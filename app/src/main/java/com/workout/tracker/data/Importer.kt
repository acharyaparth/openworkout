package com.workout.tracker.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class WorkoutBundle(val program: String, val workouts: List<BundleWorkout>)

@Serializable
data class BundleWorkout(
    val num: Int,
    val name: String,
    val title: String = "",
    val sections: List<BundleSection> = emptyList(),
)

@Serializable
data class BundleSection(val name: String, val groups: List<BundleGroup> = emptyList())

@Serializable
data class BundleGroup(
    val label: String = "",
    val scheme: String = "",
    val note: String = "",
    val exercises: List<BundleExercise> = emptyList(),
)

@Serializable
data class BundleExercise(
    val label: String = "",
    val name: String,
    val prescription: String = "",
    val targetSets: Int = 3,
    val timeBased: Boolean = false,
    val tracksWeight: Boolean = true,
    val tracked: Boolean = true,
)

private val json = Json { ignoreUnknownKeys = true }

/** Parse the bundled asset and write the whole program into the database, in order. */
suspend fun importBundle(repo: Repository, bundleJson: String) {
    val bundle = json.decodeFromString<WorkoutBundle>(bundleJson)
    val programId = repo.addProgram(bundle.program)
    for (wk in bundle.workouts.sortedBy { it.num }) {
        val workoutId = repo.addWorkoutBare(programId, wk.name, wk.num)
        for (sec in wk.sections) {
            val sectionId = repo.addSection(workoutId, sec.name)   // appends in order
            for (g in sec.groups) {
                val groupId = repo.addGroup(sectionId, g.label, g.scheme, g.note)
                for (e in g.exercises) {
                    repo.addExercise(
                        groupId = groupId,
                        label = e.label,
                        name = e.name,
                        prescription = e.prescription,
                        youtubeUrl = "",
                        targetSets = e.targetSets,
                        timeBased = e.timeBased,
                        tracksWeight = e.tracksWeight,
                        tracked = e.tracked,
                    )
                }
            }
        }
    }
}

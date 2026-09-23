package com.workout.tracker.data

import androidx.room.withTransaction
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** A complete snapshot of the local database — programs, workouts, structure and logged sessions. */
@Serializable
data class BackupData(
    val version: Int = 1,
    val app: String = "OpenWorkout",
    val exportedAt: Long = System.currentTimeMillis(),
    val programs: List<Program> = emptyList(),
    val workouts: List<Workout> = emptyList(),
    val sections: List<Section> = emptyList(),
    val groups: List<ExerciseGroup> = emptyList(),
    val exercises: List<Exercise> = emptyList(),
    val sessions: List<WorkoutSession> = emptyList(),
    val setLogs: List<SetLog> = emptyList(),
)

private val backupJson = Json { prettyPrint = true; ignoreUnknownKeys = true; encodeDefaults = true }

class BackupManager(private val db: AppDatabase) {
    private val dao = db.backupDao()

    suspend fun export(): String {
        val data = BackupData(
            programs = dao.allPrograms(),
            workouts = dao.allWorkouts(),
            sections = dao.allSections(),
            groups = dao.allGroups(),
            exercises = dao.allExercises(),
            sessions = dao.allSessions(),
            setLogs = dao.allSetLogs(),
        )
        return backupJson.encodeToString(data)
    }

    /** Replaces all current data with the backup's contents (ids preserved so links stay intact). */
    suspend fun import(json: String) {
        val data = backupJson.decodeFromString<BackupData>(json)
        db.withTransaction {
            // wipe children-first
            dao.clearSetLogs(); dao.clearSessions(); dao.clearExercises()
            dao.clearGroups(); dao.clearSections(); dao.clearWorkouts(); dao.clearPrograms()
            // insert parents-first
            dao.insertPrograms(data.programs)
            dao.insertWorkouts(data.workouts)
            dao.insertSections(data.sections)
            dao.insertGroups(data.groups)
            dao.insertExercises(data.exercises)
            dao.insertSessions(data.sessions)
            dao.insertSetLogs(data.setLogs)
        }
    }
}

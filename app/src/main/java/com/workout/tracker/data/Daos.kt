package com.workout.tracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgramDao {
    @Query("SELECT * FROM programs ORDER BY active DESC, position ASC, createdAt DESC")
    fun observeAll(): Flow<List<Program>>

    @Query("SELECT * FROM programs WHERE id = :id")
    suspend fun get(id: Long): Program?

    @Insert
    suspend fun insert(program: Program): Long

    @Update
    suspend fun update(program: Program)

    @Delete
    suspend fun delete(program: Program)

    @Query("SELECT COUNT(*) FROM exercises e " +
        "JOIN groups g ON e.groupId = g.id " +
        "JOIN sections s ON g.sectionId = s.id " +
        "JOIN workouts w ON s.workoutId = w.id " +
        "WHERE w.programId = :programId")
    fun observeExerciseCount(programId: Long): Flow<Int>
}

@Dao
interface WorkoutDao {
    @Query("SELECT * FROM workouts WHERE programId = :programId ORDER BY position ASC, createdAt ASC")
    fun observeForProgram(programId: Long): Flow<List<Workout>>

    @Query("SELECT * FROM workouts WHERE id = :id")
    suspend fun get(id: Long): Workout?

    @Transaction
    @Query("SELECT * FROM workouts WHERE id = :id")
    fun observeContent(id: Long): Flow<WorkoutWithContent?>

    @Transaction
    @Query("SELECT * FROM workouts WHERE id = :id")
    suspend fun getContent(id: Long): WorkoutWithContent?

    @Insert
    suspend fun insert(workout: Workout): Long

    @Update
    suspend fun update(workout: Workout)

    @Delete
    suspend fun delete(workout: Workout)

    @Query("SELECT COUNT(*) FROM exercises e " +
        "JOIN groups g ON e.groupId = g.id " +
        "JOIN sections s ON g.sectionId = s.id " +
        "WHERE s.workoutId = :workoutId")
    suspend fun exerciseCount(workoutId: Long): Int
}

@Dao
interface StructureDao {
    @Insert suspend fun insertSection(section: Section): Long
    @Update suspend fun updateSection(section: Section)
    @Delete suspend fun deleteSection(section: Section)
    @Query("SELECT * FROM sections WHERE workoutId = :workoutId ORDER BY position ASC")
    suspend fun sectionsFor(workoutId: Long): List<Section>

    @Insert suspend fun insertGroup(group: ExerciseGroup): Long
    @Update suspend fun updateGroup(group: ExerciseGroup)
    @Delete suspend fun deleteGroup(group: ExerciseGroup)
    @Query("SELECT * FROM groups WHERE sectionId = :sectionId ORDER BY position ASC")
    suspend fun groupsFor(sectionId: Long): List<ExerciseGroup>

    @Insert suspend fun insertExercise(exercise: Exercise): Long
    @Update suspend fun updateExercise(exercise: Exercise)
    @Delete suspend fun deleteExercise(exercise: Exercise)
    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun getExercise(id: Long): Exercise?
    @Query("SELECT * FROM exercises WHERE groupId = :groupId ORDER BY position ASC")
    suspend fun exercisesFor(groupId: Long): List<Exercise>
}

@Dao
interface SessionDao {
    @Insert suspend fun insertSession(session: WorkoutSession): Long
    @Update suspend fun updateSession(session: WorkoutSession)
    @Query("UPDATE sessions SET finishedAt = :finishedAt WHERE id = :id")
    suspend fun markFinished(id: Long, finishedAt: Long)
    @Query("DELETE FROM sessions WHERE id = :sessionId") suspend fun deleteSession(sessionId: Long)

    @Query("SELECT COALESCE(SUM(weightKg * reps), 0) FROM set_logs WHERE done = 1")
    fun observeTotalVolumeKg(): Flow<Double>

    @Query("SELECT * FROM sessions WHERE workoutId = :workoutId ORDER BY startedAt DESC")
    fun observeSessionsForWorkout(workoutId: Long): Flow<List<WorkoutSession>>

    @Query("SELECT * FROM sessions WHERE finishedAt IS NOT NULL ORDER BY finishedAt DESC")
    fun observeFinishedSessions(): Flow<List<WorkoutSession>>

    /** Per finished session: date, total volume (kg), and duration — for the Progress charts. */
    @Query("SELECT s.id AS sessionId, s.finishedAt AS finishedAt, " +
        "COALESCE(SUM(CASE WHEN l.done = 1 THEN l.weightKg * l.reps ELSE 0 END), 0) AS volumeKg, " +
        "(s.finishedAt - s.startedAt) AS durationMs " +
        "FROM sessions s LEFT JOIN set_logs l ON l.sessionId = s.id " +
        "WHERE s.finishedAt IS NOT NULL GROUP BY s.id ORDER BY s.finishedAt ASC")
    fun observeSessionStats(): Flow<List<SessionStat>>

    @Insert suspend fun insertSetLog(log: SetLog): Long
    @Update suspend fun updateSetLog(log: SetLog)
    @Delete suspend fun deleteSetLog(log: SetLog)

    @Query("SELECT * FROM set_logs WHERE sessionId = :sessionId ORDER BY exerciseId, setIndex")
    suspend fun setLogsForSession(sessionId: Long): List<SetLog>

    @Query("SELECT * FROM set_logs WHERE sessionId = :sessionId ORDER BY exerciseId, setIndex")
    fun observeSetLogsForSession(sessionId: Long): Flow<List<SetLog>>

    // History for one exercise, joined with its session for date + workout label.
    @Query("SELECT sl.*, s.startedAt AS sessionStartedAt, s.workoutName AS sessionWorkoutName " +
        "FROM set_logs sl JOIN sessions s ON sl.sessionId = s.id " +
        "WHERE sl.exerciseId = :exerciseId AND s.finishedAt IS NOT NULL " +
        "ORDER BY s.startedAt DESC, sl.setIndex ASC")
    fun observeHistoryForExercise(exerciseId: Long): Flow<List<SetLogWithSession>>
}

@Dao
interface BackupDao {
    @Query("SELECT * FROM programs") suspend fun allPrograms(): List<Program>
    @Query("SELECT * FROM workouts") suspend fun allWorkouts(): List<Workout>
    @Query("SELECT * FROM sections") suspend fun allSections(): List<Section>
    @Query("SELECT * FROM groups") suspend fun allGroups(): List<ExerciseGroup>
    @Query("SELECT * FROM exercises") suspend fun allExercises(): List<Exercise>
    @Query("SELECT * FROM sessions") suspend fun allSessions(): List<WorkoutSession>
    @Query("SELECT * FROM set_logs") suspend fun allSetLogs(): List<SetLog>
    @Query("SELECT * FROM measurements") suspend fun allMeasurements(): List<Measurement>

    @Insert suspend fun insertPrograms(rows: List<Program>)
    @Insert suspend fun insertWorkouts(rows: List<Workout>)
    @Insert suspend fun insertSections(rows: List<Section>)
    @Insert suspend fun insertGroups(rows: List<ExerciseGroup>)
    @Insert suspend fun insertExercises(rows: List<Exercise>)
    @Insert suspend fun insertSessions(rows: List<WorkoutSession>)
    @Insert suspend fun insertSetLogs(rows: List<SetLog>)
    @Insert suspend fun insertMeasurements(rows: List<Measurement>)

    // children first to respect foreign keys
    @Query("DELETE FROM measurements") suspend fun clearMeasurements()
    @Query("DELETE FROM set_logs") suspend fun clearSetLogs()
    @Query("DELETE FROM sessions") suspend fun clearSessions()
    @Query("DELETE FROM exercises") suspend fun clearExercises()
    @Query("DELETE FROM groups") suspend fun clearGroups()
    @Query("DELETE FROM sections") suspend fun clearSections()
    @Query("DELETE FROM workouts") suspend fun clearWorkouts()
    @Query("DELETE FROM programs") suspend fun clearPrograms()
}

data class SessionStat(
    val sessionId: Long,
    val finishedAt: Long,
    val volumeKg: Double,
    val durationMs: Long,
)

@Dao
interface MeasurementDao {
    @Query("SELECT * FROM measurements ORDER BY recordedAt ASC")
    fun observeAll(): Flow<List<Measurement>>
    @Insert suspend fun insert(m: Measurement): Long
    @Query("DELETE FROM measurements WHERE id = :id") suspend fun delete(id: Long)
}

data class SetLogWithSession(
    val id: Long,
    val sessionId: Long,
    val exerciseId: Long,
    val exerciseName: String,
    val setIndex: Int,
    val weightKg: Double,
    val reps: Int,
    val done: Boolean,
    val sessionStartedAt: Long,
    val sessionWorkoutName: String,
)

package com.workout.tracker.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private fun Flow<List<Program>>.mapToFirst(): Flow<Program?> = map { it.firstOrNull() }

class Repository(
    private val programDao: ProgramDao,
    private val workoutDao: WorkoutDao,
    private val structureDao: StructureDao,
    private val sessionDao: SessionDao,
    private val measurementDao: MeasurementDao,
) {
    // ---- Progress: body measurements + training stats ----
    fun measurements(): Flow<List<Measurement>> = measurementDao.observeAll()
    suspend fun addMeasurement(metric: String, unit: String, value: Double) =
        measurementDao.insert(Measurement(metric = metric, unit = unit, value = value))
    suspend fun deleteMeasurement(id: Long) = measurementDao.delete(id)
    fun sessionStats(): Flow<List<SessionStat>> = sessionDao.observeSessionStats()

    // ---- Programs ----
    fun programs(): Flow<List<Program>> = programDao.observeAll()
    /** The program the schedule follows: the top (active, then most recent) one. */
    fun scheduledProgram(): Flow<Program?> =
        programDao.observeAll().mapToFirst()
    fun programExerciseCount(programId: Long): Flow<Int> = programDao.observeExerciseCount(programId)
    suspend fun getProgram(id: Long) = programDao.get(id)

    suspend fun addProgram(name: String): Long =
        programDao.insert(Program(name = name.trim().ifBlank { "New Program" }))

    suspend fun updateProgram(program: Program) = programDao.update(program)
    suspend fun deleteProgram(program: Program) = programDao.delete(program)

    // ---- Workouts ----
    fun workouts(programId: Long): Flow<List<Workout>> = workoutDao.observeForProgram(programId)
    fun workoutContent(workoutId: Long): Flow<WorkoutWithContent?> = workoutDao.observeContent(workoutId)
    suspend fun workoutContentOnce(workoutId: Long): WorkoutWithContent? = workoutDao.getContent(workoutId)
    suspend fun getWorkout(id: Long) = workoutDao.get(id)
    suspend fun exerciseCount(workoutId: Long) = workoutDao.exerciseCount(workoutId)

    /** New workout seeded with the three standard sections. */
    suspend fun addWorkout(programId: Long, name: String): Long {
        val position = System.currentTimeMillis().toInt()
        val id = workoutDao.insert(
            Workout(programId = programId, name = name.trim().ifBlank { "New Workout" }, position = position)
        )
        listOf("Warm-up", "Main Exercise", "Cooldown").forEachIndexed { i, s ->
            structureDao.insertSection(Section(workoutId = id, name = s, position = i))
        }
        return id
    }

    /** Insert a workout with no auto-created sections (used by the bundle importer). */
    suspend fun addWorkoutBare(programId: Long, name: String, position: Int): Long =
        workoutDao.insert(Workout(programId = programId, name = name, position = position))

    suspend fun updateWorkout(workout: Workout) = workoutDao.update(workout)
    suspend fun deleteWorkout(workout: Workout) = workoutDao.delete(workout)

    // ---- Structure editing ----
    suspend fun sectionsFor(workoutId: Long) = structureDao.sectionsFor(workoutId)
    suspend fun addSection(workoutId: Long, name: String): Long {
        val pos = structureDao.sectionsFor(workoutId).size
        return structureDao.insertSection(Section(workoutId = workoutId, name = name, position = pos))
    }
    suspend fun updateSection(section: Section) = structureDao.updateSection(section)
    suspend fun deleteSection(section: Section) = structureDao.deleteSection(section)

    suspend fun addGroup(sectionId: Long, label: String, scheme: String, note: String): Long {
        val pos = structureDao.groupsFor(sectionId).size
        return structureDao.insertGroup(
            ExerciseGroup(sectionId = sectionId, label = label, scheme = scheme, note = note, position = pos)
        )
    }
    suspend fun updateGroup(group: ExerciseGroup) = structureDao.updateGroup(group)
    suspend fun deleteGroup(group: ExerciseGroup) = structureDao.deleteGroup(group)
    suspend fun groupsFor(sectionId: Long) = structureDao.groupsFor(sectionId)

    suspend fun getExercise(id: Long) = structureDao.getExercise(id)
    suspend fun addExercise(
        groupId: Long, label: String, name: String, prescription: String,
        youtubeUrl: String, targetSets: Int, timeBased: Boolean, tracksWeight: Boolean = true,
        tracked: Boolean = true,
    ): Long {
        val pos = structureDao.exercisesFor(groupId).size
        return structureDao.insertExercise(
            Exercise(
                groupId = groupId, label = label, name = name, prescription = prescription,
                youtubeUrl = youtubeUrl, targetSets = targetSets.coerceIn(1, 12),
                timeBased = timeBased, tracksWeight = tracksWeight, tracked = tracked, position = pos
            )
        )
    }
    suspend fun updateExercise(exercise: Exercise) = structureDao.updateExercise(exercise)
    suspend fun deleteExercise(exercise: Exercise) = structureDao.deleteExercise(exercise)

    // ---- Sessions & logging ----
    suspend fun startSession(workout: Workout): Long =
        sessionDao.insertSession(WorkoutSession(workoutId = workout.id, workoutName = workout.name))

    suspend fun setLogsForSession(sessionId: Long) = sessionDao.setLogsForSession(sessionId)
    fun observeSetLogsForSession(sessionId: Long) = sessionDao.observeSetLogsForSession(sessionId)

    suspend fun upsertSetLog(log: SetLog): Long =
        if (log.id == 0L) sessionDao.insertSetLog(log) else { sessionDao.updateSetLog(log); log.id }

    suspend fun deleteSetLog(log: SetLog) = sessionDao.deleteSetLog(log)

    /** Stamp finishedAt without touching startedAt (so the recorded duration is correct). */
    suspend fun finishSession(sessionId: Long, finishedAt: Long) =
        sessionDao.markFinished(sessionId, finishedAt)

    suspend fun deleteSession(sessionId: Long) = sessionDao.deleteSession(sessionId)

    fun totalVolumeKg(): Flow<Double> = sessionDao.observeTotalVolumeKg()

    suspend fun getSessionRaw(sessionId: Long): List<SetLog> = sessionDao.setLogsForSession(sessionId)

    fun historyForExercise(exerciseId: Long): Flow<List<SetLogWithSession>> =
        sessionDao.observeHistoryForExercise(exerciseId)

    fun sessionsForWorkout(workoutId: Long): Flow<List<WorkoutSession>> =
        sessionDao.observeSessionsForWorkout(workoutId)

    fun finishedSessions(): Flow<List<WorkoutSession>> = sessionDao.observeFinishedSessions()
}

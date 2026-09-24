package com.workout.tracker.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import kotlinx.serialization.Serializable

/**
 * The program/workout structure mirrors a coach-style model:
 *   Program (a "Block") -> Workout -> Section -> Group (a lettered block) -> Exercise
 * Logged data lives separately so history survives edits to the template:
 *   WorkoutSession -> SetLog
 * All weights are stored canonically in kilograms; the UI converts to the
 * user's chosen display unit so the lbs/kg toggle is lossless.
 */

@Serializable
@Entity(tableName = "programs")
data class Program(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val active: Boolean = true,
    val position: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
)

@Serializable
@Entity(
    tableName = "workouts",
    foreignKeys = [ForeignKey(
        entity = Program::class,
        parentColumns = ["id"],
        childColumns = ["programId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("programId")]
)
data class Workout(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val programId: Long,
    val name: String,
    val position: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
)

@Serializable
@Entity(
    tableName = "sections",
    foreignKeys = [ForeignKey(
        entity = Workout::class,
        parentColumns = ["id"],
        childColumns = ["workoutId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("workoutId")]
)
data class Section(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workoutId: Long,
    val name: String,
    val position: Int = 0,
)

@Serializable
@Entity(
    tableName = "groups",
    foreignKeys = [ForeignKey(
        entity = Section::class,
        parentColumns = ["id"],
        childColumns = ["sectionId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("sectionId")]
)
data class ExerciseGroup(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sectionId: Long,
    val label: String = "",       // "A", "B" ...
    val scheme: String = "",      // "5 Rounds", "4 Giant Sets", "40-30-20-10"
    val note: String = "",        // free-text coaching note
    val position: Int = 0,
)

@Serializable
@Entity(
    tableName = "exercises",
    foreignKeys = [ForeignKey(
        entity = ExerciseGroup::class,
        parentColumns = ["id"],
        childColumns = ["groupId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("groupId")]
)
data class Exercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val groupId: Long,
    val label: String = "",       // "A1", "A2" ...
    val name: String,
    val prescription: String = "", // "10 each side, dbs on shoulders"
    val youtubeUrl: String = "",
    val targetSets: Int = 3,
    val timeBased: Boolean = false, // true -> the numeric field is seconds, not reps
    val tracksWeight: Boolean = true, // false -> bodyweight/mobility: just mark done, no weight
    val tracked: Boolean = true, // false -> a "just for fun" note: no logging, no done-gating
    val position: Int = 0,
)

@Serializable
@Entity(
    tableName = "sessions",
    foreignKeys = [ForeignKey(
        entity = Workout::class,
        parentColumns = ["id"],
        childColumns = ["workoutId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("workoutId")]
)
data class WorkoutSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workoutId: Long,
    val workoutName: String,      // snapshot for stable history labels
    val startedAt: Long = System.currentTimeMillis(),
    val finishedAt: Long? = null,
)

@Serializable
@Entity(
    tableName = "set_logs",
    foreignKeys = [ForeignKey(
        entity = WorkoutSession::class,
        parentColumns = ["id"],
        childColumns = ["sessionId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("sessionId"), Index("exerciseId")]
)
data class SetLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val exerciseId: Long,
    val exerciseName: String,     // snapshot for stable history labels
    val setIndex: Int,            // 1-based
    val weightKg: Double = 0.0,   // canonical kilograms
    val reps: Int = 0,            // reps, or seconds when the exercise is time-based
    val done: Boolean = false,
)

@Serializable
@Entity(tableName = "measurements")
data class Measurement(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val metric: String,          // "Weight", "Waist", "Body Fat", ...
    val unit: String,            // "kg", "lbs", "in", "%"
    val value: Double,
    val recordedAt: Long = System.currentTimeMillis(),
)

// ---- Read-only relation graphs ----

data class GroupWithExercises(
    @Embedded val group: ExerciseGroup,
    @Relation(parentColumn = "id", entityColumn = "groupId")
    val exercises: List<Exercise>,
)

data class SectionWithContent(
    @Embedded val section: Section,
    @Relation(entity = ExerciseGroup::class, parentColumn = "id", entityColumn = "sectionId")
    val groups: List<GroupWithExercises>,
)

data class WorkoutWithContent(
    @Embedded val workout: Workout,
    @Relation(entity = Section::class, parentColumn = "id", entityColumn = "workoutId")
    val sections: List<SectionWithContent>,
)

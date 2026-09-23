package com.workout.tracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        Program::class,
        Workout::class,
        Section::class,
        ExerciseGroup::class,
        Exercise::class,
        WorkoutSession::class,
        SetLog::class,
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun programDao(): ProgramDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun structureDao(): StructureDao
    abstract fun sessionDao(): SessionDao
    abstract fun backupDao(): BackupDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        /** v1 -> v2: add exercises.tracksWeight, defaulting bodyweight/mobility work to no-weight. */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE exercises ADD COLUMN tracksWeight INTEGER NOT NULL DEFAULT 1")
                db.execSQL("UPDATE exercises SET tracksWeight = 0 WHERE timeBased = 1")
                db.execSQL(
                    "UPDATE exercises SET tracksWeight = 0 WHERE groupId IN (" +
                        "SELECT g.id FROM groups g JOIN sections s ON g.sectionId = s.id " +
                        "WHERE s.name IN ('Warm-up','Cooldown') " +
                        "OR lower(g.scheme) LIKE '%core%' OR lower(g.scheme) LIKE '%cardio%')"
                )
            }
        }

        /** v2 -> v3: add exercises.tracked; mark "just for fun" items (e.g. Insta Story) untracked. */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE exercises ADD COLUMN tracked INTEGER NOT NULL DEFAULT 1")
                db.execSQL(
                    "UPDATE exercises SET tracked = 0, tracksWeight = 0 WHERE " +
                        "lower(name) LIKE '%insta%' OR lower(name) LIKE '%story%' " +
                        "OR lower(name) LIKE '%selfie%' OR lower(name) LIKE '%photo%'"
                )
            }
        }

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "workout.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build().also { instance = it }
            }
    }
}

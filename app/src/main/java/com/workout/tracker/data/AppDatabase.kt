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
        Measurement::class,
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun programDao(): ProgramDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun structureDao(): StructureDao
    abstract fun sessionDao(): SessionDao
    abstract fun backupDao(): BackupDao
    abstract fun measurementDao(): MeasurementDao

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

        /** v3 -> v4: add the body-measurements table for the Progress tab. */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `measurements` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`metric` TEXT NOT NULL, `unit` TEXT NOT NULL, " +
                        "`value` REAL NOT NULL, `recordedAt` INTEGER NOT NULL)"
                )
            }
        }

        /** Classify every exercise's logType from its name/section/scheme. This is a fallback for
         *  scraped or hand-entered data; the authoritative source is the generator/agent, which
         *  should set logType per exercise with judgment (see WORKOUTS_SCHEMA.md). */
        private fun reclassifyLogType(db: SupportSQLiteDatabase) {
            // mark-done: warm-up/cooldown/core/cardio, cardio + ab/core names, "just for fun" items
            db.execSQL(
                "UPDATE exercises SET logType='none' WHERE tracked = 0 " +
                    "OR lower(name) LIKE '%jump rope%' OR lower(name) LIKE '%skip%' " +
                    "OR lower(name) LIKE '%crunch%' OR lower(name) LIKE '%russian twist%' " +
                    "OR lower(name) LIKE '%sit up%' OR lower(name) LIKE '%situp%' " +
                    "OR lower(name) LIKE '%leg raise%' OR lower(name) LIKE '%flutter%' " +
                    "OR lower(name) LIKE '%mountain climber%' OR lower(name) LIKE '%dead bug%' " +
                    "OR lower(name) LIKE '%bird dog%' OR lower(name) LIKE '%hollow%' " +
                    "OR groupId IN (SELECT g.id FROM groups g JOIN sections s ON g.sectionId = s.id " +
                    "WHERE s.name IN ('Warm-up','Cooldown') " +
                    "OR lower(g.scheme) LIKE '%core%' OR lower(g.scheme) LIKE '%cardio%')"
            )
            // timed holds / stretches (avoid '%hold%' — it false-matches "press with hold on top")
            db.execSQL(
                "UPDATE exercises SET logType='time' WHERE logType != 'none' AND (" +
                    "lower(name) LIKE '%stretch%' OR lower(name) LIKE '%plank%' " +
                    "OR lower(name) LIKE '%wall sit%' OR lower(name) LIKE '%dead hang%')"
            )
            // bodyweight strength -> reps (only over the default weighted, and not externally loaded)
            db.execSQL(
                "UPDATE exercises SET logType='reps' WHERE logType='weight_reps' AND (" +
                    "lower(name) LIKE '%push up%' OR lower(name) LIKE '%pushup%' OR lower(name) LIKE '%push-up%' " +
                    "OR lower(name) LIKE '%pull up%' OR lower(name) LIKE '%pullup%' OR lower(name) LIKE '%chin up%' " +
                    "OR lower(name) LIKE '%dip%' OR lower(name) LIKE '%air squat%' OR lower(name) LIKE '%burpee%' " +
                    "OR lower(name) LIKE '%pistol squat%' OR lower(name) LIKE '%box jump%') " +
                    "AND NOT (lower(name) LIKE '%dumbbell%' OR lower(name) LIKE '%barbell%' " +
                    "OR lower(name) LIKE '%kettlebell%' OR lower(name) LIKE '%cable%' OR lower(name) LIKE '%machine%')"
            )
        }

        /** v4 -> v5: add exercises.logType (weight_reps|reps|time|none) and classify existing rows. */
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE exercises ADD COLUMN logType TEXT NOT NULL DEFAULT 'weight_reps'")
                reclassifyLogType(db)
            }
        }

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "workout.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5).build().also { instance = it }
            }
    }
}

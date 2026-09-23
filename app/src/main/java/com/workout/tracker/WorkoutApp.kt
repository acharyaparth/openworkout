package com.workout.tracker

import android.app.Application
import android.content.Context
import com.workout.tracker.data.AppDatabase
import com.workout.tracker.data.Repository
import com.workout.tracker.data.SettingsStore
import com.workout.tracker.data.seedIfEmpty
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Tiny service locator — no DI framework needed for an app this size. */
class AppContainer(context: Context) {
    private val db = AppDatabase.get(context)
    val repository = Repository(
        db.programDao(), db.workoutDao(), db.structureDao(), db.sessionDao()
    )
    val settings = SettingsStore(context)
    val backup = com.workout.tracker.data.BackupManager(db)
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
}

class WorkoutApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        val bundleJson = runCatching {
            assets.open("workouts.json").bufferedReader().use { it.readText() }
        }.getOrNull()
        if (bundleJson != null) {
            container.applicationScope.launch { seedIfEmpty(container.repository, bundleJson) }
        }
    }
}

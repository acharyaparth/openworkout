package com.workout.tracker.data

import kotlinx.coroutines.flow.first

/**
 * On a fresh install, import the program bundle in assets/workouts.json (generated
 * for you during setup) so the app opens populated. No-op if data already exists.
 */
suspend fun seedIfEmpty(repo: Repository, bundleJson: String) {
    if (repo.programs().first().isNotEmpty()) return
    importBundle(repo, bundleJson)
}

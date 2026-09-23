package com.workout.tracker.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.workout.tracker.ui.edit.EditExerciseScreen
import com.workout.tracker.ui.edit.EditProgramScreen
import com.workout.tracker.ui.edit.EditWorkoutScreen
import com.workout.tracker.ui.exercise.ExerciseDetailScreen
import com.workout.tracker.ui.overview.WorkoutOverviewScreen
import com.workout.tracker.ui.player.PlayerScreen
import com.workout.tracker.ui.programs.ProgramsScreen
import com.workout.tracker.ui.workouts.WorkoutListScreen

object Routes {
    const val HOME = "home"
    const val PROGRAMS = "programs"
    const val SETTINGS = "settings"
    fun workouts(programId: Long) = "workouts/$programId"
    fun overview(workoutId: Long, editable: Boolean = false) = "overview/$workoutId?editable=$editable"
    fun player(workoutId: Long) = "player/$workoutId"
    fun exercise(exerciseId: Long) = "exercise/$exerciseId"
    fun editProgram(programId: Long?) = "editProgram?programId=${programId ?: -1L}"
    fun editWorkout(programId: Long, workoutId: Long?) =
        "editWorkout/$programId?workoutId=${workoutId ?: -1L}"
    fun editExercise(groupId: Long, exerciseId: Long?) =
        "editExercise/$groupId?exerciseId=${exerciseId ?: -1L}"
}

@Composable
fun AppNav(nav: NavHostController) {
    NavHost(navController = nav, startDestination = Routes.HOME) {

        composable(Routes.HOME) {
            HomeScreen(nav)
        }

        composable(Routes.SETTINGS) {
            com.workout.tracker.ui.settings.SettingsScreen(onBack = { nav.popBackStack() })
        }

        composable(
            "workouts/{programId}",
            arguments = listOf(navArgument("programId") { type = NavType.LongType })
        ) { entry ->
            val programId = entry.arguments!!.getLong("programId")
            WorkoutListScreen(
                programId = programId,
                onBack = { nav.popBackStack() },
                onOpenWorkout = { nav.navigate(Routes.overview(it, editable = true)) },
                onEditWorkout = { wid -> nav.navigate(Routes.editWorkout(programId, wid)) },
                onEditProgram = { nav.navigate(Routes.editProgram(programId)) },
            )
        }

        composable(
            "overview/{workoutId}?editable={editable}",
            arguments = listOf(
                navArgument("workoutId") { type = NavType.LongType },
                navArgument("editable") { type = NavType.BoolType; defaultValue = false },
            )
        ) { entry ->
            val workoutId = entry.arguments!!.getLong("workoutId")
            val editable = entry.arguments!!.getBoolean("editable")
            WorkoutOverviewScreen(
                workoutId = workoutId,
                editable = editable,
                onBack = { nav.popBackStack() },
                onStart = { nav.navigate(Routes.player(workoutId)) },
                onOpenExercise = { nav.navigate(Routes.exercise(it)) },
                onEditWorkout = { nav.navigate(Routes.editWorkout(0, workoutId)) },
                onEditExercise = { groupId, exId -> nav.navigate(Routes.editExercise(groupId, exId)) },
            )
        }

        composable(
            "player/{workoutId}",
            arguments = listOf(navArgument("workoutId") { type = NavType.LongType })
        ) { entry ->
            val workoutId = entry.arguments!!.getLong("workoutId")
            PlayerScreen(
                workoutId = workoutId,
                onClose = { nav.popBackStack() },
                onOpenExercise = { nav.navigate(Routes.exercise(it)) },
            )
        }

        composable(
            "exercise/{exerciseId}",
            arguments = listOf(navArgument("exerciseId") { type = NavType.LongType })
        ) { entry ->
            val exerciseId = entry.arguments!!.getLong("exerciseId")
            ExerciseDetailScreen(exerciseId = exerciseId, onBack = { nav.popBackStack() })
        }

        composable(
            "editProgram?programId={programId}",
            arguments = listOf(navArgument("programId") { type = NavType.LongType; defaultValue = -1L })
        ) { entry ->
            val pid = entry.arguments!!.getLong("programId").takeIf { it >= 0 }
            EditProgramScreen(programId = pid, onDone = { nav.popBackStack() })
        }

        composable(
            "editWorkout/{programId}?workoutId={workoutId}",
            arguments = listOf(
                navArgument("programId") { type = NavType.LongType },
                navArgument("workoutId") { type = NavType.LongType; defaultValue = -1L },
            )
        ) { entry ->
            val pid = entry.arguments!!.getLong("programId")
            val wid = entry.arguments!!.getLong("workoutId").takeIf { it >= 0 }
            EditWorkoutScreen(programId = pid, workoutId = wid, onDone = { nav.popBackStack() })
        }

        composable(
            "editExercise/{groupId}?exerciseId={exerciseId}",
            arguments = listOf(
                navArgument("groupId") { type = NavType.LongType },
                navArgument("exerciseId") { type = NavType.LongType; defaultValue = -1L },
            )
        ) { entry ->
            val gid = entry.arguments!!.getLong("groupId")
            val exId = entry.arguments!!.getLong("exerciseId").takeIf { it >= 0 }
            EditExerciseScreen(groupId = gid, exerciseId = exId, onDone = { nav.popBackStack() })
        }
    }
}

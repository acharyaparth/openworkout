package com.workout.tracker.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.workout.tracker.AppContainer
import com.workout.tracker.WorkoutApp

@Composable
fun appContainer(): AppContainer =
    (LocalContext.current.applicationContext as WorkoutApp).container

/** Build a screen-scoped ViewModel with access to the app container. */
@Composable
inline fun <reified VM : ViewModel> appViewModel(
    key: String? = null,
    crossinline create: (AppContainer) -> VM,
): VM {
    val container = appContainer()
    return viewModel(
        key = key,
        factory = viewModelFactory { initializer { create(container) } }
    )
}

// Date formatting shared across screens.
fun formatDate(millis: Long): String {
    val fmt = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
    return fmt.format(java.util.Date(millis))
}

fun formatDuration(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return "%02d:%02d:%02d".format(h, m, s)
}

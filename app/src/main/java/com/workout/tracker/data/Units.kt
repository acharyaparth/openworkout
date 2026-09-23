package com.workout.tracker.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.math.roundToInt

enum class WeightUnit(val label: String, val perKg: Double) {
    LBS("lbs", 2.2046226218),
    KG("kg", 1.0);

    /** Canonical kg -> this unit. */
    fun fromKg(kg: Double): Double = kg * perKg

    /** A value in this unit -> canonical kg. */
    fun toKg(value: Double): Double = value / perKg
}

/** Trim trailing ".0" so "25.0" shows as "25" but "22.5" stays "22.5". */
fun Double.trimWeight(): String {
    val rounded = (this * 10).roundToInt() / 10.0
    return if (rounded % 1.0 == 0.0) rounded.toInt().toString() else rounded.toString()
}

private val Context.dataStore by preferencesDataStore(name = "settings")
private val UNIT_KEY = stringPreferencesKey("weight_unit")
private val SCHED_START = androidx.datastore.preferences.core.longPreferencesKey("sched_start_epochday")
private val SCHED_TRAIN = androidx.datastore.preferences.core.intPreferencesKey("sched_train_on")
private val SCHED_REST = androidx.datastore.preferences.core.intPreferencesKey("sched_rest_len")

/** Sequential-rotation schedule config: [trainOn] training days then [restLen] rest days,
 *  starting on [startEpochDay]; workouts cycle in order across training days. */
data class ScheduleConfig(
    val startEpochDay: Long,
    val trainOn: Int,
    val restLen: Int,
)

class SettingsStore(private val context: Context) {
    val unit: Flow<WeightUnit> = context.dataStore.data.map { prefs ->
        when (prefs[UNIT_KEY]) {
            "kg" -> WeightUnit.KG
            else -> WeightUnit.LBS
        }
    }

    suspend fun setUnit(unit: WeightUnit) {
        context.dataStore.edit { it[UNIT_KEY] = unit.label }
    }

    val schedule: Flow<ScheduleConfig> = context.dataStore.data.map { prefs ->
        ScheduleConfig(
            startEpochDay = prefs[SCHED_START] ?: java.time.LocalDate.now().toEpochDay(),
            trainOn = prefs[SCHED_TRAIN] ?: 6,
            restLen = prefs[SCHED_REST] ?: 1,
        )
    }

    suspend fun setSchedule(config: ScheduleConfig) {
        context.dataStore.edit {
            it[SCHED_START] = config.startEpochDay
            it[SCHED_TRAIN] = config.trainOn.coerceIn(1, 14)
            it[SCHED_REST] = config.restLen.coerceIn(1, 7)
        }
    }
}

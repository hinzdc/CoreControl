package com.sargaverse.corecontrol.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sargaverse.corecontrol.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "core_control_settings")

class SettingsRepositoryImpl(
    private val context: Context
) : SettingsRepository {

    companion object {
        val KEY_REFRESH_INTERVAL_MS = longPreferencesKey("refresh_interval_ms")
        val KEY_CONFIRM_BULK_ACTIONS = booleanPreferencesKey("confirm_bulk_actions")
        val KEY_CONFIRM_INDIVIDUAL_ACTION = booleanPreferencesKey("confirm_individual_action")

        const val DEFAULT_REFRESH_INTERVAL_MS = 2000L
    }

    override val refreshIntervalMs: Flow<Long> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[KEY_REFRESH_INTERVAL_MS] ?: DEFAULT_REFRESH_INTERVAL_MS
        }

    override val confirmBulkActions: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[KEY_CONFIRM_BULK_ACTIONS] ?: true
        }

    override val confirmIndividualAction: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[KEY_CONFIRM_INDIVIDUAL_ACTION] ?: false
        }

    override suspend fun setRefreshIntervalMs(interval: Long) {
        context.dataStore.edit { preferences ->
            preferences[KEY_REFRESH_INTERVAL_MS] = interval
        }
    }

    override suspend fun setConfirmBulkActions(confirm: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_CONFIRM_BULK_ACTIONS] = confirm
        }
    }

    override suspend fun setConfirmIndividualAction(confirm: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_CONFIRM_INDIVIDUAL_ACTION] = confirm
        }
    }
}

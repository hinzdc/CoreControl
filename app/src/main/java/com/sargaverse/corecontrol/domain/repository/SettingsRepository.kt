package com.sargaverse.corecontrol.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Repository for managing application preferences and safety settings via DataStore.
 */
interface SettingsRepository {
    val refreshIntervalMs: Flow<Long>
    val confirmBulkActions: Flow<Boolean>
    val confirmIndividualAction: Flow<Boolean>

    suspend fun setRefreshIntervalMs(interval: Long)
    suspend fun setConfirmBulkActions(confirm: Boolean)
    suspend fun setConfirmIndividualAction(confirm: Boolean)
}

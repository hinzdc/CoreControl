package com.sargaverse.corecontrol.domain.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Historical record of a CPU control operation executed by the user.
 */
data class OperationLog(
    val id: String = java.util.UUID.randomUUID().toString(),
    val timestampMs: Long = System.currentTimeMillis(),
    val cpuId: Int,
    val action: OperationAction,
    val result: OperationResult,
    val durationMs: Long
) {
    val formattedTime: String
        get() = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date(timestampMs))

    val isSuccess: Boolean
        get() = result is OperationResult.Success
}

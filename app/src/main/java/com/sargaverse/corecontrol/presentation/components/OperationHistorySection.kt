package com.sargaverse.corecontrol.presentation.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sargaverse.corecontrol.domain.model.OperationAction
import com.sargaverse.corecontrol.domain.model.OperationLog
import com.sargaverse.corecontrol.domain.model.OperationResult
import com.sargaverse.corecontrol.presentation.theme.CoreErrorRed
import com.sargaverse.corecontrol.presentation.theme.CoreOnlineGreen
import com.sargaverse.corecontrol.presentation.theme.CyberPrimary

@Composable
fun OperationHistorySection(
    logs: List<OperationLog>,
    modifier: Modifier = Modifier
) {
    if (logs.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text(
            text = "OPERATION AUDIT LOG",
            style = MaterialTheme.typography.labelSmall.copy(
                letterSpacing = 0.8.sp,
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(4.dp))

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp)
                )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                logs.take(8).forEach { log ->
                    LogItemRow(log = log)
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
    }
}

@Composable
private fun LogItemRow(log: OperationLog) {
    val actionText = when (log.action) {
        OperationAction.TOGGLE_ON -> "CPU ${log.cpuId} → ONLINE"
        OperationAction.TOGGLE_OFF -> "CPU ${log.cpuId} → OFFLINE"
        OperationAction.BULK_ALL_ON -> "BULK ALL ON (CPU ${log.cpuId})"
        OperationAction.BULK_ALL_OFF -> "BULK ALL OFF (CPU ${log.cpuId})"
        OperationAction.BOOT_RESTORE -> "BOOT RESTORE (CPU ${log.cpuId})"
        OperationAction.REFRESH -> "REFRESH"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = if (log.isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                contentDescription = if (log.isSuccess) "Success" else "Failed",
                tint = if (log.isSuccess) CoreOnlineGreen else CoreErrorRed,
                modifier = Modifier.size(16.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column {
                Text(
                    text = actionText,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                val detailMsg = when (val res = log.result) {
                    is OperationResult.Success -> res.message
                    is OperationResult.Failure -> res.errorMessage
                }
                Text(
                    text = detailMsg,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = log.formattedTime,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${log.durationMs}ms",
                style = MaterialTheme.typography.labelSmall,
                color = CyberPrimary
            )
        }
    }
}

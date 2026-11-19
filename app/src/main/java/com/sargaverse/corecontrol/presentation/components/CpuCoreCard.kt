package com.sargaverse.corecontrol.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sargaverse.corecontrol.domain.model.CpuCapabilityState
import com.sargaverse.corecontrol.domain.model.CpuCore
import com.sargaverse.corecontrol.presentation.theme.CoreErrorRed
import com.sargaverse.corecontrol.presentation.theme.CoreOfflineSlate
import com.sargaverse.corecontrol.presentation.theme.CoreOnlineGreen
import com.sargaverse.corecontrol.presentation.theme.CorePrimePurple
import com.sargaverse.corecontrol.presentation.theme.CoreProtectedAmber
import com.sargaverse.corecontrol.presentation.theme.CyberPrimary

@Composable
fun CpuCoreCard(
    core: CpuCore,
    isExecuting: Boolean,
    onToggle: (CpuCore) -> Unit,
    modifier: Modifier = Modifier
) {
    val statusColor by animateColorAsState(
        targetValue = when (core.state) {
            CpuCapabilityState.ONLINE -> CoreOnlineGreen
            CpuCapabilityState.OFFLINE -> CoreOfflineSlate
            CpuCapabilityState.PROTECTED -> CoreProtectedAmber
            CpuCapabilityState.UNSUPPORTED -> Color.Gray
            CpuCapabilityState.ERROR -> CoreErrorRed
            CpuCapabilityState.UNKNOWN -> Color.Gray
        },
        animationSpec = tween(durationMillis = 300),
        label = "statusColor"
    )

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (core.isOnline) statusColor.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Column: CPU ID & Status indicator
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Status dot indicator
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "CPU ${core.cpuId}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        // Capability or Cluster tag
                        val tagText = when {
                            core.isProtected -> "PROTECTED"
                            !core.hotplugSupported -> "FIXED"
                            core.clusterId == 2 -> "PRIME"
                            core.clusterId == 1 -> "PERF"
                            core.clusterId == 0 -> "EFF"
                            else -> "CORE"
                        }
                        val tagColor = when {
                            core.isProtected -> CoreProtectedAmber
                            core.clusterId == 2 -> CorePrimePurple
                            core.clusterId == 1 -> CyberPrimary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(tagColor.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = tagText,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = tagColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    // Frequency & Governor info
                    val freqDisplay = if (core.isOnline) {
                        core.formattedFrequency() + (core.governor?.let { " • $it" } ?: "")
                    } else {
                        "Offline • Low Power State"
                    }

                    Text(
                        text = freqDisplay,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (core.isOnline) MaterialTheme.colorScheme.onSurfaceVariant else CoreOfflineSlate
                    )
                }
            }

            // Right side: Controls or Protected Lock Icon
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(48.dp)
            ) {
                if (isExecuting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.5.dp,
                        color = CyberPrimary
                    )
                } else if (core.isProtected) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Core Protected",
                        tint = CoreProtectedAmber,
                        modifier = Modifier.size(22.dp)
                    )
                } else if (!core.hotplugSupported) {
                    Text(
                        text = "N/A",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Switch(
                        checked = core.isOnline,
                        onCheckedChange = { onToggle(core) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CoreOnlineGreen,
                            checkedTrackColor = CoreOnlineGreen.copy(alpha = 0.3f),
                            uncheckedThumbColor = CoreOfflineSlate,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }
            }
        }
    }
}

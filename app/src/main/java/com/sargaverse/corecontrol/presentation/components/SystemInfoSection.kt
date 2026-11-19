package com.sargaverse.corecontrol.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sargaverse.corecontrol.domain.model.SystemInfo
import com.sargaverse.corecontrol.presentation.theme.CyberPrimary

@Composable
fun SystemInfoSection(
    systemInfo: SystemInfo?,
    modifier: Modifier = Modifier
) {
    if (systemInfo == null) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text(
            text = "HARDWARE & TOPOLOGY",
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
            Column(modifier = Modifier.padding(16.dp)) {
                InfoRow(label = "Platform / SoC", value = systemInfo.socModel)
                InfoRow(label = "Architecture", value = systemInfo.architecture)
                InfoRow(label = "Kernel Release", value = systemInfo.kernelRelease)
                InfoRow(label = "OS", value = systemInfo.androidVersion)
                systemInfo.hyperOsVersion?.let {
                    InfoRow(label = "HyperOS Build", value = it)
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Clusters
                Text(
                    text = "Clusters Detected (${systemInfo.clusters.size})",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = CyberPrimary
                )

                Spacer(modifier = Modifier.height(4.dp))

                systemInfo.clusters.forEach { cluster ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = cluster.name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "CPUs [${cluster.coreIds.joinToString(", ")}]",
                            style = MaterialTheme.typography.labelMedium,
                            color = CyberPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Medium
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

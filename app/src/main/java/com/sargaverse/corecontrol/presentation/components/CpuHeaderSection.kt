package com.sargaverse.corecontrol.presentation.components

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sargaverse.corecontrol.domain.model.RootStatus
import com.sargaverse.corecontrol.presentation.theme.CoreOnlineGreen
import com.sargaverse.corecontrol.presentation.theme.CoreProtectedAmber
import com.sargaverse.corecontrol.presentation.theme.CyberPrimary

@Composable
fun CpuHeaderSection(
    rootStatus: RootStatus,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onOpenTopology: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "CORE CONTROL",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        letterSpacing = 0.8.sp
                    ),
                    color = CyberPrimary
                )
                Text(
                    text = "SARGAVERSE SYSTEM UTILITY",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.2.sp,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onOpenTopology,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "System Info",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = onRefresh,
                    enabled = !isRefreshing,
                    modifier = Modifier.size(36.dp)
                ) {
                    if (isRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = CyberPrimary
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Root authorization status badge
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = when (rootStatus) {
                is RootStatus.Granted -> CoreOnlineGreen.copy(alpha = 0.12f)
                is RootStatus.Denied -> MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
                is RootStatus.Checking -> CoreProtectedAmber.copy(alpha = 0.12f)
                RootStatus.NotAvailable -> MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            when (rootStatus) {
                                is RootStatus.Granted -> CoreOnlineGreen
                                is RootStatus.Denied -> MaterialTheme.colorScheme.error
                                is RootStatus.Checking -> CoreProtectedAmber
                                RootStatus.NotAvailable -> Color.Gray
                            }
                        )
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = when (rootStatus) {
                        is RootStatus.Granted -> "Root Access: Granted (KernelSU / SU)"
                        is RootStatus.Denied -> "Root Access: Denied (${rootStatus.reason})"
                        is RootStatus.Checking -> "Root Access: Checking authorization..."
                        RootStatus.NotAvailable -> "Root Access: SU Binary Not Found"
                    },
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp
                    ),
                    color = when (rootStatus) {
                        is RootStatus.Granted -> CoreOnlineGreen
                        is RootStatus.Denied -> MaterialTheme.colorScheme.error
                        is RootStatus.Checking -> CoreProtectedAmber
                        RootStatus.NotAvailable -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}

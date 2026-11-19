package com.sargaverse.corecontrol.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sargaverse.corecontrol.domain.model.CpuSummary
import com.sargaverse.corecontrol.presentation.theme.CoreOfflineSlate
import com.sargaverse.corecontrol.presentation.theme.CoreOnlineGreen
import com.sargaverse.corecontrol.presentation.theme.CoreProtectedAmber
import com.sargaverse.corecontrol.presentation.theme.CyberPrimary

@Composable
fun CpuSummarySection(
    summary: CpuSummary,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        SummaryMetricCard(
            label = "TOTAL",
            value = summary.totalCores.toString(),
            color = CyberPrimary,
            modifier = Modifier.weight(1f)
        )
        SummaryMetricCard(
            label = "ONLINE",
            value = summary.onlineCores.toString(),
            color = CoreOnlineGreen,
            modifier = Modifier.weight(1f)
        )
        SummaryMetricCard(
            label = "OFFLINE",
            value = summary.offlineCores.toString(),
            color = CoreOfflineSlate,
            modifier = Modifier.weight(1f)
        )
        SummaryMetricCard(
            label = "PROTECTED",
            value = summary.protectedCores.toString(),
            color = CoreProtectedAmber,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SummaryMetricCard(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .border(1.dp, color.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
            .padding(vertical = 5.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                ),
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 8.5.sp,
                    letterSpacing = 0.5.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

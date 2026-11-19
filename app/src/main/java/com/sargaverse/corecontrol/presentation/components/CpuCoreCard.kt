package com.sargaverse.corecontrol.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sargaverse.corecontrol.domain.model.CpuCapabilityState
import com.sargaverse.corecontrol.domain.model.CpuCore
import com.sargaverse.corecontrol.presentation.theme.CardNeutralBgDark
import com.sargaverse.corecontrol.presentation.theme.CardNeutralBgLight
import com.sargaverse.corecontrol.presentation.theme.CardOfflineBgDark
import com.sargaverse.corecontrol.presentation.theme.CardOfflineBgLight
import com.sargaverse.corecontrol.presentation.theme.CardOnlineBgDark
import com.sargaverse.corecontrol.presentation.theme.CardOnlineBgLight
import com.sargaverse.corecontrol.presentation.theme.CoreErrorRed
import com.sargaverse.corecontrol.presentation.theme.CoreProtectedAmber

/**
 * Clickable rectangular CPU core card.
 *
 * Designed according to SARGAVERSE precision specifications:
 * - Top: CPU Processor Icon
 * - Center: High-contrast CPU ID & Frequency
 * - Bottom: Current Status Label (ONLINE / OFFLINE / PROTECTED / UNSUPPORTED)
 * - The entire card is clickable to toggle states (NO toggle switches).
 */
@Composable
fun CpuCoreCard(
    core: CpuCore,
    isExecuting: Boolean,
    onToggle: (CpuCore) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()

    // Determine state visual parameters
    val targetBgColor = when (core.state) {
        CpuCapabilityState.ONLINE -> if (isDark) CardOnlineBgDark else CardOnlineBgLight
        CpuCapabilityState.OFFLINE -> if (isDark) CardOfflineBgDark else CardOfflineBgLight
        CpuCapabilityState.PROTECTED -> if (isDark) CardNeutralBgDark else CardNeutralBgLight
        CpuCapabilityState.UNSUPPORTED,
        CpuCapabilityState.UNKNOWN,
        CpuCapabilityState.ERROR -> if (isDark) CardNeutralBgDark else CardNeutralBgLight
    }

    val targetAccentColor = when (core.state) {
        CpuCapabilityState.ONLINE -> Color(0xFF22C55E) // Bright Green
        CpuCapabilityState.OFFLINE -> Color(0xFFEF4444) // Bright Red
        CpuCapabilityState.PROTECTED -> CoreProtectedAmber // Amber
        CpuCapabilityState.UNSUPPORTED -> Color(0xFF64748B) // Slate Gray
        CpuCapabilityState.ERROR -> CoreErrorRed
        CpuCapabilityState.UNKNOWN -> Color(0xFF64748B)
    }

    val targetTextColor = when {
        core.state == CpuCapabilityState.UNSUPPORTED -> Color(0xFF94A3B8)
        isDark -> Color.White
        else -> if (core.state == CpuCapabilityState.PROTECTED) Color(0xFF0F172A) else Color.White
    }

    val animatedBgColor by animateColorAsState(
        targetValue = targetBgColor,
        animationSpec = tween(durationMillis = 250),
        label = "cardBgColor"
    )

    val animatedAccentColor by animateColorAsState(
        targetValue = targetAccentColor,
        animationSpec = tween(durationMillis = 250),
        label = "cardAccentColor"
    )

    val isClickable = core.isControllable && !isExecuting

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(130.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(animatedBgColor)
            .border(
                width = 1.dp,
                color = animatedAccentColor.copy(alpha = if (isDark) 0.35f else 0.5f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(
                enabled = isClickable,
                onClick = { onToggle(core) }
            )
            .padding(14.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // TOP: CPU Icon (or Spinner if executing)
            Box(
                modifier = Modifier.size(28.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isExecuting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.5.dp,
                        color = animatedAccentColor
                    )
                } else if (core.isProtected) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Core Protected",
                        tint = animatedAccentColor,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Memory,
                        contentDescription = "CPU Core ${core.cpuId}",
                        tint = animatedAccentColor,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // CENTER: CPU ID and optional live frequency
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "CPU ${core.cpuId}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 17.sp,
                        letterSpacing = 0.5.sp
                    ),
                    color = targetTextColor
                )

                val freqText = if (core.isOnline) {
                    core.formattedFrequency()
                } else {
                    "Offline"
                }

                Text(
                    text = freqText,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp
                    ),
                    color = if (core.isOnline) animatedAccentColor.copy(alpha = 0.9f) else Color(0xFF94A3B8)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // BOTTOM: Status Label
            val statusLabel = when (core.state) {
                CpuCapabilityState.ONLINE -> "ONLINE"
                CpuCapabilityState.OFFLINE -> "OFFLINE"
                CpuCapabilityState.PROTECTED -> "PROTECTED"
                CpuCapabilityState.UNSUPPORTED -> "UNSUPPORTED"
                CpuCapabilityState.ERROR -> "ERROR"
                CpuCapabilityState.UNKNOWN -> "UNKNOWN"
            }

            Text(
                text = statusLabel,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp
                ),
                color = animatedAccentColor
            )
        }
    }
}

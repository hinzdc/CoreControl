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
import androidx.compose.foundation.layout.fillMaxSize
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
import com.sargaverse.corecontrol.presentation.theme.CardNeutralBorderDark
import com.sargaverse.corecontrol.presentation.theme.CardNeutralBorderLight
import com.sargaverse.corecontrol.presentation.theme.CardOfflineBgDark
import com.sargaverse.corecontrol.presentation.theme.CardOfflineBgLight
import com.sargaverse.corecontrol.presentation.theme.CardOfflineBorderDark
import com.sargaverse.corecontrol.presentation.theme.CardOfflineBorderLight
import com.sargaverse.corecontrol.presentation.theme.CardOnlineBgDark
import com.sargaverse.corecontrol.presentation.theme.CardOnlineBgLight
import com.sargaverse.corecontrol.presentation.theme.CardOnlineBorderDark
import com.sargaverse.corecontrol.presentation.theme.CardOnlineBorderLight
import com.sargaverse.corecontrol.presentation.theme.CoreErrorRed
import com.sargaverse.corecontrol.presentation.theme.CoreProtectedAmber

/**
 * Compact rectangular CPU core card.
 *
 * SARGAVERSE Compact Grid Precision Specification:
 * - Exactly 4 cards per row, 2 rows for 8 cores.
 * - Top: Compact CPU Processor Icon / Lock / Spinner
 * - Center: High-contrast CPU ID & Frequency
 * - Bottom: Status Label (ONLINE / OFFLINE / PROTECTED / UNSUPPORTED)
 * - Restrained, non-glow technical color treatment.
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

    // Determine restrained surface & border colors
    val targetBgColor = when (core.state) {
        CpuCapabilityState.ONLINE -> if (isDark) CardOnlineBgDark else CardOnlineBgLight
        CpuCapabilityState.OFFLINE -> if (isDark) CardOfflineBgDark else CardOfflineBgLight
        CpuCapabilityState.PROTECTED -> if (isDark) CardNeutralBgDark else CardNeutralBgLight
        CpuCapabilityState.UNSUPPORTED,
        CpuCapabilityState.UNKNOWN,
        CpuCapabilityState.ERROR -> if (isDark) CardNeutralBgDark else CardNeutralBgLight
    }

    val targetBorderColor = when (core.state) {
        CpuCapabilityState.ONLINE -> if (isDark) CardOnlineBorderDark else CardOnlineBorderLight
        CpuCapabilityState.OFFLINE -> if (isDark) CardOfflineBorderDark else CardOfflineBorderLight
        CpuCapabilityState.PROTECTED -> if (isDark) Color(0xFF423218) else Color(0xFFFCD34D)
        CpuCapabilityState.UNSUPPORTED,
        CpuCapabilityState.UNKNOWN,
        CpuCapabilityState.ERROR -> if (isDark) CardNeutralBorderDark else CardNeutralBorderLight
    }

    // Restrained, sophisticated technical accent colors (no garish neon)
    val targetAccentColor = when (core.state) {
        CpuCapabilityState.ONLINE -> if (isDark) Color(0xFF34D399) else Color(0xFF16A34A) // Refined emerald
        CpuCapabilityState.OFFLINE -> if (isDark) Color(0xFFF87171) else Color(0xFFDC2626) // Refined crimson
        CpuCapabilityState.PROTECTED -> CoreProtectedAmber
        CpuCapabilityState.UNSUPPORTED -> Color(0xFF94A3B8)
        CpuCapabilityState.ERROR -> CoreErrorRed
        CpuCapabilityState.UNKNOWN -> Color(0xFF94A3B8)
    }

    val targetTextColor = when {
        core.state == CpuCapabilityState.UNSUPPORTED -> Color(0xFF94A3B8)
        isDark -> Color(0xFFF1F5F9)
        else -> if (core.state == CpuCapabilityState.PROTECTED) Color(0xFF0F172A) else Color(0xFF1E293B)
    }

    val animatedBgColor by animateColorAsState(
        targetValue = targetBgColor,
        animationSpec = tween(durationMillis = 200),
        label = "cardBgColor"
    )

    val animatedBorderColor by animateColorAsState(
        targetValue = targetBorderColor,
        animationSpec = tween(durationMillis = 200),
        label = "cardBorderColor"
    )

    val animatedAccentColor by animateColorAsState(
        targetValue = targetAccentColor,
        animationSpec = tween(durationMillis = 200),
        label = "cardAccentColor"
    )

    val isClickable = core.isControllable && !isExecuting

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(84.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(animatedBgColor)
            .border(
                width = 1.dp,
                color = animatedBorderColor,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(
                enabled = isClickable,
                onClick = { onToggle(core) }
            )
            .padding(horizontal = 4.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // TOP: CPU Icon (or Spinner if executing)
            Box(
                modifier = Modifier.size(18.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isExecuting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = animatedAccentColor
                    )
                } else if (core.isProtected) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Core Protected",
                        tint = animatedAccentColor,
                        modifier = Modifier.size(15.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Memory,
                        contentDescription = "CPU Core ${core.cpuId}",
                        tint = animatedAccentColor,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }

            // CENTER: CPU ID and live frequency
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "CPU ${core.cpuId}",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        letterSpacing = 0.2.sp
                    ),
                    color = targetTextColor,
                    maxLines = 1
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
                        fontSize = 9.sp
                    ),
                    color = if (core.isOnline) animatedAccentColor.copy(alpha = 0.9f) else Color(0xFF94A3B8),
                    maxLines = 1
                )
            }

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
                    fontSize = 9.5.sp,
                    letterSpacing = 0.5.sp
                ),
                color = animatedAccentColor,
                maxLines = 1
            )
        }
    }
}

package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class LayoutBreakpoint {
    COMPACT,   // < 600.dp (Smartphone)
    MEDIUM,    // 600.dp .. 899.dp (Tablet / Dobrável)
    EXPANDED   // >= 900.dp (Desktop / ChromeOS / Tablet Paisagem)
}

fun layoutBreakpointFor(availableWidth: Dp): LayoutBreakpoint {
    return when {
        availableWidth >= 900.dp -> LayoutBreakpoint.EXPANDED
        availableWidth >= 600.dp -> LayoutBreakpoint.MEDIUM
        else -> LayoutBreakpoint.COMPACT
    }
}

@Composable
fun rememberDesktopHoverBorder(
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
): Pair<MutableInteractionSource, BorderStroke?> {
    val isHovered by interactionSource.collectIsHoveredAsState()
    val border = if (isHovered) {
        BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.65f))
    } else {
        null
    }
    return interactionSource to border
}

fun Modifier.desktopHoverable(interactionSource: MutableInteractionSource): Modifier = composed {
    this.hoverable(interactionSource = interactionSource)
}

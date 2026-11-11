package com.auramusic.offline.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun GlassmorphicCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f

    val bgBrush = if (isDark) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0x33FFFFFF),
                Color(0x1AFFFFFF)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xF2FFFFFF),
                Color(0xE6FFFFFF)
            )
        )
    }

    val borderColor = if (isDark) Color(0x26FFFFFF) else Color(0x1F000000)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(bgBrush)
            .border(1.dp, borderColor, RoundedCornerShape(cornerRadius)),
        content = content
    )
}

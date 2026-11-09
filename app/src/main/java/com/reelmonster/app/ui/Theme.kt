package com.reelmonster.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = Color(0xFF6C4DF6), secondary = Color(0xFFE8457F), tertiary = Color(0xFF12A07A),
    background = Color(0xFFFBF8FF), surface = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE5DEFF), onPrimaryContainer = Color(0xFF21005D),
    secondaryContainer = Color(0xFFFFDCE7), onSecondaryContainer = Color(0xFF3F0020),
    tertiaryContainer = Color(0xFFCFF5E7), onTertiaryContainer = Color(0xFF00382A),
    surfaceVariant = Color(0xFFF0EBFA)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFB9A8FF), secondary = Color(0xFFFF8FB5), tertiary = Color(0xFF5FE0B7),
    background = Color(0xFF14111D), surface = Color(0xFF1D1930),
    primaryContainer = Color(0xFF3A2F7A), onPrimaryContainer = Color(0xFFE5DEFF),
    secondaryContainer = Color(0xFF6A2A45), onSecondaryContainer = Color(0xFFFFDCE7),
    tertiaryContainer = Color(0xFF14533F), onTertiaryContainer = Color(0xFFCFF5E7),
    surfaceVariant = Color(0xFF2A2540)
)

@Composable
fun ReelMonsterTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = Typography(),
        shapes = Shapes(
            small = RoundedCornerShape(12.dp),
            medium = RoundedCornerShape(20.dp),
            large = RoundedCornerShape(28.dp)
        ),
        content = content
    )
}

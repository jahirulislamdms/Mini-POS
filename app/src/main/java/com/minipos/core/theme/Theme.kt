package com.minipos.core.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// Light-only scheme. Primary brand colour is yellow (dark text/icons on it). No dynamic/dark theme.
private val LightColors = lightColorScheme(
    primary = BrandYellow,
    onPrimary = OnYellow,
    secondary = BrandYellow,
    onSecondary = OnYellow,
    background = AppBackground,
    onBackground = OnSurface,
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = AppBackground,
    onSurfaceVariant = TextMuted,
    error = ExpenseRed,
    onError = OnBlue,
    outline = Divider,
)

@Composable
fun MiniPosTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = Typography,
        shapes = Shapes,
        content = content,
    )
}

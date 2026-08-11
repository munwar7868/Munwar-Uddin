package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val MunwarColorScheme = darkColorScheme(
    primary = ImmersiveBluePrimary,
    onPrimary = Color.White,
    primaryContainer = ImmersiveBlueBright,
    onPrimaryContainer = Color.White,
    secondary = ImmersiveCyanAccent,
    onSecondary = Color.Black,
    background = ImmersiveDarkBg,
    onBackground = ImmersiveTextWhite,
    surface = ImmersiveSurface,
    onSurface = ImmersiveTextWhite,
    surfaceVariant = ImmersiveCardBg,
    onSurfaceVariant = ImmersiveTextMuted,
    error = ImmersiveErrorRed,
    onError = Color.White
)

@Composable
fun MunwarAiTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = MunwarColorScheme,
        typography = Typography,
        content = content
    )
}


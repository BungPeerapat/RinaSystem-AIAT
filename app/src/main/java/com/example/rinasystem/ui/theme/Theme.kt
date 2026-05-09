package com.example.rinasystem.ui.theme

import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView

private val AriaColorScheme = darkColorScheme(
    primary = AriaCyan,
    onPrimary = AriaBgDark,
    primaryContainer = AriaCyanDark,
    onPrimaryContainer = AriaCyanLight,
    secondary = AriaBlue,
    onSecondary = AriaBgDark,
    tertiary = AriaPurple,
    onTertiary = AriaBgDark,
    background = AriaBgDark,
    onBackground = AriaTextPrimary,
    surface = AriaBgSurface,
    onSurface = AriaTextPrimary,
    surfaceVariant = AriaBgElevated,
    onSurfaceVariant = AriaTextSecondary,
    outline = AriaBorder,
    outlineVariant = AriaDivider,
    error = AriaRed,
    onError = AriaBgDark,
)

@Composable
fun RinaSystemTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as ComponentActivity
            activity.enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.dark(AriaBgDark.toArgb()),
                navigationBarStyle = SystemBarStyle.dark(AriaBgDark.toArgb())
            )
        }
    }

    MaterialTheme(
        colorScheme = AriaColorScheme,
        typography = Typography,
        content = content
    )
}

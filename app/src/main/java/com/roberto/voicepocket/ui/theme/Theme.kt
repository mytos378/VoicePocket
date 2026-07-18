package com.roberto.voicepocket.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = DeepOceanBlue,
    onPrimary = PureWhite,
    primaryContainer = LightBlueContainer,
    onPrimaryContainer = Charcoal,

    secondary = SlateGray,
    onSecondary = PureWhite,
    secondaryContainer = LightSurfaceVariant,
    onSecondaryContainer = Charcoal,

    tertiary = PocketTurquoise,
    onTertiary = Charcoal,
    tertiaryContainer = LightTurquoiseContainer,
    onTertiaryContainer = Charcoal,

    error = VoicePocketError,
    onError = PureWhite,

    background = SoftWhite,
    onBackground = Charcoal,

    surface = PureWhite,
    onSurface = Charcoal,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = MediumGray,

    outline = SlateGray
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkPrimaryContainer,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = LightText,

    secondary = DarkSecondary,
    onSecondary = Charcoal,
    secondaryContainer = DarkSurfaceVariant,
    onSecondaryContainer = LightText,

    tertiary = DarkTertiary,
    onTertiary = DarkTertiaryContainer,
    tertiaryContainer = DarkTertiaryContainer,
    onTertiaryContainer = LightText,

    error = VoicePocketDarkError,
    onError = Charcoal,

    background = DarkBackground,
    onBackground = LightText,

    surface = DarkSurface,
    onSurface = LightText,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkSecondary,

    outline = DarkSecondary
)

@Composable
fun VoicePocketTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        DarkColorScheme
    } else {
        LightColorScheme
    }

    val view = LocalView.current

    if (!view.isInEditMode) {
        val window = (view.context as Activity).window

        @Suppress("DEPRECATION")
        window.statusBarColor = colorScheme.background.toArgb()

        @Suppress("DEPRECATION")
        window.navigationBarColor = colorScheme.background.toArgb()

        WindowCompat.getInsetsController(
            window,
            view
        ).apply {
            isAppearanceLightStatusBars = !darkTheme
            isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = VoicePocketTypography,
        shapes = VoicePocketShapes,
        content = content
    )
}
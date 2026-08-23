package com.whip.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = WhipPurple,
    secondary = WhipGreen,
    background = Color(0xFFFFFBFF),
    surface = Color(0xFFFFFBFF),
)

private val DarkColors = darkColorScheme(
    primary = WhipPurpleLight,
    secondary = WhipGreenLight,
    background = Color(0xFF1C1B1F),
    surface = Color(0xFF1C1B1F),
)

@Composable
fun WhipTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val systemColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && darkTheme -> {
            dynamicDarkColorScheme(context)
        }
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    // Selected and active controls use one accent family. Material's defaults
    // otherwise mix primary, secondary, and tertiary highlights across buttons,
    // chips, segmented controls, and banners even when they mean the same thing.
    val colorScheme = systemColorScheme.withUnifiedHighlights()

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = WhipShapes,
        typography = WhipTypography,
        content = content,
    )
}

/** Keeps equivalent selected/active states from changing accent by component. */
internal fun ColorScheme.withUnifiedHighlights(): ColorScheme = copy(
    secondary = primary,
    onSecondary = onPrimary,
    secondaryContainer = primaryContainer,
    onSecondaryContainer = onPrimaryContainer,
    tertiary = primary,
    onTertiary = onPrimary,
    tertiaryContainer = primaryContainer,
    onTertiaryContainer = onPrimaryContainer,
)

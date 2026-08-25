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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = WhipPurple,
    secondary = WhipGreen,
    tertiary = WhipAmber,
    error = WhipRed,
    background = Color(0xFFFFFBFF),
    surface = Color(0xFFFFFBFF),
)

private val DarkColors = darkColorScheme(
    primary = WhipPurpleLight,
    secondary = WhipGreenLight,
    tertiary = WhipAmberLight,
    error = WhipRedLight,
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
    val semanticColors = systemColorScheme.toWhipSemanticColors()

    CompositionLocalProvider(LocalWhipSemanticColors provides semanticColors) {
        MaterialTheme(
            colorScheme = systemColorScheme,
            shapes = WhipShapes,
            typography = WhipTypography,
            content = content,
        )
    }
}

/**
 * Semantic roles prevent unrelated meanings from inheriting the same accent.
 * Dynamic color remains supported because each role is derived from the active
 * Material scheme rather than from hard-coded screen colors.
 */
@Immutable
data class WhipSemanticColors(
    val action: Color,
    val onAction: Color,
    val selection: Color,
    val onSelection: Color,
    val success: Color,
    val onSuccess: Color,
    val warning: Color,
    val onWarning: Color,
    val destructive: Color,
    val onDestructive: Color,
    val metadata: Color,
)

private val LocalWhipSemanticColors = staticCompositionLocalOf {
    WhipSemanticColors(
        action = WhipPurple,
        onAction = Color.White,
        selection = Color(0xFFE9DDFF),
        onSelection = Color(0xFF211047),
        success = WhipGreen,
        onSuccess = Color.White,
        warning = WhipAmber,
        onWarning = Color.White,
        destructive = WhipRed,
        onDestructive = Color.White,
        metadata = Color(0xFF5F6368),
    )
}

val MaterialTheme.whipColors: WhipSemanticColors
    @Composable get() = LocalWhipSemanticColors.current

internal fun ColorScheme.toWhipSemanticColors() = WhipSemanticColors(
    action = primary,
    onAction = onPrimary,
    selection = primaryContainer,
    onSelection = onPrimaryContainer,
    success = secondary,
    onSuccess = onSecondary,
    warning = tertiary,
    onWarning = onTertiary,
    destructive = error,
    onDestructive = onError,
    metadata = onSurfaceVariant,
)

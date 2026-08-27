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
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE9E3EE),
    onPrimaryContainer = Color(0xFF201B25),
    secondary = WhipGreen,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFB8EBD0),
    onSecondaryContainer = Color(0xFF092116),
    tertiary = WhipAmber,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDF91),
    onTertiaryContainer = Color(0xFF261A00),
    error = WhipRed,
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = WhipWarmWhite,
    onBackground = Color(0xFF1D1B18),
    surface = Color(0xFFFBF9F4),
    onSurface = Color(0xFF1D1B18),
    surfaceVariant = Color(0xFFE6E1D8),
    onSurfaceVariant = Color(0xFF4A4741),
    outline = Color(0xFF7B776F),
    outlineVariant = Color(0xFFCCC6BC),
    inverseSurface = Color(0xFF32302C),
    inverseOnSurface = Color(0xFFF5F0E8),
    inversePrimary = Color(0xFFF2EEE6),
    surfaceTint = WhipInk,
    scrim = Color.Black,
    surfaceBright = Color(0xFFFBF9F4),
    surfaceDim = Color(0xFFDDD9D2),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF5F2EC),
    surfaceContainer = Color(0xFFEFECE6),
    surfaceContainerHigh = Color(0xFFE9E6E0),
    surfaceContainerHighest = Color(0xFFE3E0DA),
)

private val DarkColors = darkColorScheme(
    primary = WhipPurpleLight,
    onPrimary = Color(0xFF242228),
    primaryContainer = Color(0xFF48424F),
    onPrimaryContainer = Color(0xFFF3EAF7),
    secondary = WhipGreenLight,
    onSecondary = Color(0xFF123724),
    secondaryContainer = Color(0xFF214E3A),
    onSecondaryContainer = Color(0xFFC4F1D6),
    tertiary = WhipAmberLight,
    onTertiary = Color(0xFF402D00),
    tertiaryContainer = Color(0xFF5C4300),
    onTertiaryContainer = Color(0xFFFFE29B),
    error = WhipRedLight,
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF121210),
    onBackground = Color(0xFFE7E2DA),
    surface = Color(0xFF171715),
    onSurface = Color(0xFFE7E2DA),
    surfaceVariant = Color(0xFF494640),
    onSurfaceVariant = Color(0xFFCEC7BD),
    outline = Color(0xFF99938A),
    outlineVariant = Color(0xFF494640),
    inverseSurface = Color(0xFFE7E2DA),
    inverseOnSurface = Color(0xFF302E2A),
    inversePrimary = WhipInk,
    surfaceTint = WhipPurpleLight,
    scrim = Color.Black,
    surfaceBright = Color(0xFF3A3935),
    surfaceDim = Color(0xFF121210),
    surfaceContainerLowest = Color(0xFF0D0D0B),
    surfaceContainerLow = Color(0xFF1B1B18),
    surfaceContainer = Color(0xFF201F1C),
    surfaceContainerHigh = Color(0xFF2A2925),
    surfaceContainerHighest = Color(0xFF35332F),
)

@Composable
fun WhipTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
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

package com.familyos.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.familyos.core.domain.model.ThemeMode

/** Brand primary blue. */
val FamilyOsPrimary = FamilyPrimary

/** Brand secondary indigo. */
val FamilyOsSecondary = FamilySecondary

/** Success green. */
val FamilyOsSuccess = FamilySuccess

/** Warning amber. */
val FamilyOsWarning = FamilyWarning

/** Danger red. */
val FamilyOsDanger = FamilyDanger

private val LightColors = lightColorScheme(
    primary = FamilyPrimary,
    onPrimary = Color.White,
    secondary = FamilySecondary,
    onSecondary = Color.White,
    tertiary = FamilySuccess,
    error = FamilyDanger,
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surface = FamilyBackgroundLight,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF475569),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF60A5FA),
    onPrimary = Color(0xFF0F172A),
    secondary = Color(0xFF818CF8),
    onSecondary = Color(0xFF0F172A),
    tertiary = FamilySuccess,
    error = FamilyDanger,
    background = FamilyBackgroundDark,
    onBackground = Color(0xFFF1F5F9),
    surface = Color(0xFF111827),
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFF1F2937),
    onSurfaceVariant = Color(0xFFCBD5E1),
)

/** Extra semantic colors not covered by Material ColorScheme. */
data class FamilyOsExtendedColors(
    val success: Color,
    val warning: Color,
    val danger: Color,
)

/** CompositionLocal for [FamilyOsExtendedColors]. */
val LocalFamilyOsColors = staticCompositionLocalOf {
    FamilyOsExtendedColors(
        success = FamilySuccess,
        warning = FamilyWarning,
        danger = FamilyDanger,
    )
}

/**
 * FamilyOS Material 3 theme supporting Light / Dark / System modes.
 */
@Composable
fun FamilyOsTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    darkTheme: Boolean? = null,
    content: @Composable () -> Unit,
) {
    val dark = darkTheme ?: when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    CompositionLocalProvider(
        LocalFamilyOsColors provides FamilyOsExtendedColors(
            success = FamilySuccess,
            warning = FamilyWarning,
            danger = FamilyDanger,
        ),
    ) {
        MaterialTheme(
            colorScheme = if (dark) DarkColors else LightColors,
            typography = FamilyOsTypography,
            content = content,
        )
    }
}

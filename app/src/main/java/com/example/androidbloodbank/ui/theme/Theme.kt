package com.example.androidbloodbank.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ------- Soothing rose palette (light) -------
private val LightColors = lightColorScheme(
    primary = Color(0xFFE56B6F),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFFF3A0A7),
    onSecondary = Color(0xFF3F2326),
    tertiary = Color(0xFF7AB6B1),
    onTertiary = Color(0xFF103C39),
    background = Color(0xFFFFFBFB),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF2E7E9),
    onSurface = Color(0xFF1E1E1F),
    onSurfaceVariant = Color(0xFF514A4C),
)

// ------- Cozy dark (no pure black) -------
private val DarkColors = darkColorScheme(
    primary = Color(0xFFFF8C95),
    onPrimary = Color(0xFF2C0A0E),
    secondary = Color(0xFFF2B8BD),
    onSecondary = Color(0xFF2D1316),
    tertiary = Color(0xFFAEDDD8),
    onTertiary = Color(0xFF062927),
    background = Color(0xFF0F1113),
    surface = Color(0xFF131517),
    surfaceVariant = Color(0xFF22282B),
    onSurface = Color(0xFFE3E6E8),
    onSurfaceVariant = Color(0xFFB2B8BC),
)

/** App theme entry point — THIS is what MainActivity should import. */
@Composable
fun AndroidBloodBankTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // keep false so brand colors are stable
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        content = content
    )
}

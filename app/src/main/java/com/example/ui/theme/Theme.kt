package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val MyDarkColorScheme = darkColorScheme(
    primary = CrimsonPrimary,
    onPrimary = LightText,
    secondary = NeonTeal,
    onSecondary = BlackBackground,
    tertiary = WarningAmber,
    onTertiary = BlackBackground,
    background = BlackBackground,
    onBackground = LightText,
    surface = ObsidianSurface,
    onSurface = LightText
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force dark theme for atmospheric horror gaming vibe
    dynamicColor: Boolean = false, // Disable system dynamic color to preserve original dark-red art style
    content: @Composable () -> Unit,
) {
    val colorScheme = MyDarkColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

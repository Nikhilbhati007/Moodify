package com.bhati.moodify.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = ElectricPurple,
    secondary = DeepSeaLight,
    tertiary = DarkAccent,
    background = DeepSeaBlue,
    surface = DeepSeaLight,
    onPrimary = TextWhite,
    onSecondary = TextWhite,
    onTertiary = TextWhite,
    onBackground = TextWhite,
    onSurface = TextWhite,
)

private val LightColorScheme = lightColorScheme(
    primary = ElectricPurpleLight,
    secondary = ElectricPurple,
    tertiary = LightGray,
    background = LightBackground, 
    surface = LightSurface,
    onPrimary = TextWhite,
    onSecondary = TextWhite,
    onTertiary = DarkText,
    onBackground = DarkText,
    onSurface = DarkText,
)


@Composable
fun MoodifyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Disabled dynamic color to enforce Moodify custom colors globally
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
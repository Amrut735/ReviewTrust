package com.reviewtrust.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = Blue700,
    onPrimary = White,
    primaryContainer = Blue200,
    secondary = BlueGrey800,
    onSecondary = White,
    background = LightBackground,
    onBackground = DarkBackground,
    surface = White,
    onSurface = DarkBackground
)

@Composable
fun ReviewTrustTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        content = content
    )
}

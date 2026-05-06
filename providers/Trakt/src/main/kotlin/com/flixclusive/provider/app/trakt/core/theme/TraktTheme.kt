package com.flixclusive.provider.app.trakt.core.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun TraktTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(
            primary = Color(0xFF9f42c6),
            onPrimary = Color.White,
            primaryContainer = Color(0xFF9f42c6).copy(alpha = 0.1f),
            onPrimaryContainer = Color(0xFF9f42c6).copy(alpha = 0.9f),
            tertiary = Color(0xFFFF0000),
            onTertiary = Color.White,
            tertiaryContainer = Color(0xFFFF0000).copy(alpha = 0.1f),
            onTertiaryContainer = Color(0xFFFF0000).copy(alpha = 0.9f),
        ),
        content = content
    )
}
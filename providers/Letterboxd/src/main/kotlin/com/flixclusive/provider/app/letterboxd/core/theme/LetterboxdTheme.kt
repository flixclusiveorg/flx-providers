package com.flixclusive.provider.app.letterboxd.core.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

internal val LetterboxdGreen = Color(0xFF00E054)
internal val LetterboxdOrange = Color(0xFFFF8000)
internal val LetterboxdBlue = Color(0xFF40BCF4)

/**
 * Copies the host's scheme so the screen keeps the app's light/dark treatment, and
 * only swaps in the Letterboxd accents.
 */
@Composable
internal fun LetterboxdTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme =
            MaterialTheme.colorScheme.copy(
                primary = LetterboxdGreen,
                onPrimary = Color.Black,
                primaryContainer = LetterboxdGreen.copy(alpha = 0.12f),
                onPrimaryContainer = LetterboxdGreen,
                secondary = LetterboxdBlue,
                onSecondary = Color.Black,
                tertiary = LetterboxdOrange,
                onTertiary = Color.Black,
            ),
        content = content,
    )
}

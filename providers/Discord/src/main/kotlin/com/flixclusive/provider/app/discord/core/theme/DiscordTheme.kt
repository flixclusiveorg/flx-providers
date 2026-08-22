package com.flixclusive.provider.app.discord.core.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Blurple = Color(0xFF5865F2)
private val Surface = Color(0xFF313338)
private val SurfaceVariant = Color(0xFF2B2D31)
private val OnSurface = Color(0xFFF2F3F5)
private val OnSurfaceMuted = Color(0xFFB5BAC1)

private val DiscordColors =
    darkColorScheme(
        primary = Blurple,
        onPrimary = Color.White,
        surface = Surface,
        onSurface = OnSurface,
        surfaceVariant = SurfaceVariant,
        onSurfaceVariant = OnSurfaceMuted,
        background = Surface,
        onBackground = OnSurface,
    )

@Composable
internal fun DiscordTheme(content: @Composable () -> Unit) = MaterialTheme(colorScheme = DiscordColors, content = content)

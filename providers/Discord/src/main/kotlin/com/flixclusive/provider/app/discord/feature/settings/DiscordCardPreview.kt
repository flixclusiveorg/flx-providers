package com.flixclusive.provider.app.discord.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.flixclusive.provider.app.discord.core.model.DiscordActivity
import java.util.concurrent.TimeUnit

private val CardBackground = Color(0xFF232428)
private val ArtPlaceholder = Color(0xFF1E1F22)
private val Muted = Color(0xFFB5BAC1)
private val ButtonBorder = Color(0xFF4E5058)

@Composable
internal fun DiscordCardPreview(
    activity: DiscordActivity,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(CardBackground)
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "${activity.sectionHeader()} ${activity.name}",
            color = Muted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier =
                    Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(ArtPlaceholder),
            ) {
                activity.largeImage?.let { image ->
                    AsyncImage(
                        model = image,
                        contentDescription = activity.largeText,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(60.dp),
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                activity.details?.let { PreviewLine(text = it) }
                activity.state?.let { PreviewLine(text = it) }
                activity.elapsedLabel()?.let { PreviewLine(text = it) }
            }
        }

        activity.buttons.take(DiscordActivity.MAX_BUTTONS).forEach { button ->
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(32.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .border(1.dp, ButtonBorder, RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = button.label, color = Color.White, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun PreviewLine(
    text: String,
    bold: Boolean = false,
) {
    Text(
        text = text,
        color = if (bold) Color.White else Muted,
        fontSize = if (bold) 14.sp else 13.sp,
        fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

private fun DiscordActivity.sectionHeader(): String =
    when (type) {
        DiscordActivity.TYPE_WATCHING -> "Watching"
        DiscordActivity.TYPE_LISTENING -> "Listening to"
        else -> "Playing a game"
    }

private fun DiscordActivity.elapsedLabel(): String? {
    val start = startTimestamp ?: return null
    val elapsed = (System.currentTimeMillis() - start).coerceAtLeast(0L)

    val minutes = TimeUnit.MILLISECONDS.toMinutes(elapsed)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(elapsed) % 60

    return "%02d:%02d elapsed".format(minutes, seconds)
}

package com.flixclusive.provider.app.tmdb.settings.components

import android.net.Uri
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import com.flixclusive.provider.app.tmdb.core.model.UserCatalog

private val TmdbBlue = Color(0xFF01B4E4)

@Composable
internal fun CatalogCardItem(
    catalog: UserCatalog,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val parsedUri = remember(catalog.url) { runCatching { Uri.parse(catalog.url) }.getOrNull() }

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = catalog.name,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(4.dp))
                    CategoryChip(label = catalog.category)
                }

                Switch(
                    checked = catalog.enabled,
                    onCheckedChange = onToggle,
                )

                IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                    Icon(
                        painter = painterResource(android.R.drawable.ic_menu_edit),
                        contentDescription = "Edit",
                        modifier = Modifier.size(18.dp),
                    )
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(
                        painter = painterResource(android.R.drawable.ic_menu_delete),
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            if (expanded && parsedUri != null) {
                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(Modifier.height(10.dp))
                UrlInspector(uri = parsedUri)
            }
        }
    }
}

@Composable
private fun CategoryChip(label: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(TmdbBlue.copy(alpha = 0.15f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text = label.uppercase(),
            color = TmdbBlue,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
        )
    }
}

@Composable
private fun UrlInspector(uri: Uri) {
    val baseUrl = buildString {
        append(uri.scheme ?: "https")
        append("://")
        append(uri.host ?: "")
        if (uri.path?.isNotBlank() == true) append(uri.path)
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        UrlRow(key = "URL", value = baseUrl, isBase = true)

        uri.queryParameterNames.sortedBy { it }.fastForEach { key ->
            val value = uri.getQueryParameter(key) ?: ""
            UrlRow(key = key, value = value)
        }
    }
}

@Composable
private fun UrlRow(key: String, value: String, isBase: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(
                if (isBase) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                else MaterialTheme.colorScheme.surface
            )
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = key,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium,
                color = TmdbBlue,
            ),
        )
        Box(modifier = Modifier.weight(1f)) {
            val scrollState = rememberScrollState()
            Text(
                text = value,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                ),
                modifier = Modifier.horizontalScroll(scrollState),
                softWrap = false,
            )
        }
    }
}

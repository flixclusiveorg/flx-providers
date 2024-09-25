package com.flxProviders.stremio.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.provider.util.res.painterResource
import com.flixclusive.provider.util.res.stringResource
import com.flxProviders.stremio.BuildConfig
import com.flxProviders.stremio.api.model.Addon
import java.net.URL

@Composable
internal fun AddonCard(
    addon: Addon,
    onUpdateManifest: () -> Unit,
    onEdit: () -> Unit,
    onRemove: () -> Unit,
) {
    val clipboardManager = LocalClipboardManager.current

    OutlinedCard(
        onClick = {
            val stremioUrl = "${addon.baseUrl}/manifest.json"
                .replace("https://", "stremio://")
            clipboardManager.setText(
                AnnotatedString(text = stremioUrl)
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .height(60.dp)
                .fillMaxWidth()
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(weight = 1F, fill = true)
                    .padding(3.dp)
            ) {
                ImageWithSmallPlaceholder(
                    modifier = Modifier.size(60.dp),
                    placeholderModifier = Modifier.size(30.dp),
                    urlImage = addon.logo,
                    placeholder = painterResource(
                        name = "provider_logo",
                        packageName = BuildConfig.LIBRARY_PACKAGE_NAME
                    ),
                    contentDesc = stringResource(
                        name = "addon_icon_content_desc",
                        packageName = BuildConfig.LIBRARY_PACKAGE_NAME
                    ),
                    shape = MaterialTheme.shapes.small
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val host = remember { URL(addon.baseUrl).host }

                    Text(
                        text = addon.name,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )

                    Text(
                        text = host ?: "",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Normal,
                            color = LocalContentColor.current.copy(0.6F)
                        )
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 3.dp)
            ) {
                IconButton(onClick = onUpdateManifest) {
                    Icon(
                        painter = painterResource(
                            name = "refresh",
                            packageName = BuildConfig.LIBRARY_PACKAGE_NAME
                        ),
                        contentDescription = "Update addon"
                    )
                }

                IconButton(onClick = onEdit) {
                    Icon(
                        painter = painterResource(
                            name = "modify",
                            packageName = BuildConfig.LIBRARY_PACKAGE_NAME
                        ),
                        contentDescription = "Edit addon"
                    )
                }

                IconButton(onClick = onRemove) {
                    Icon(
                        painter = painterResource(
                            name = "delete",
                            packageName = BuildConfig.LIBRARY_PACKAGE_NAME
                        ),
                        contentDescription = "Delete addon"
                    )
                }
            }
        }
    }
}
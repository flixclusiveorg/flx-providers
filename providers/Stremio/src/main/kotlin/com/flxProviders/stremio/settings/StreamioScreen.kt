@file:Suppress("SpellCheckingInspection")

package com.flxProviders.stremio.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.flixclusive.provider.settings.ProviderSettingsManager
import com.flxProviders.stremio.settings.AddonUtil.getAddons
import com.flxProviders.stremio.settings.AddonUtil.removeAddon
import okhttp3.OkHttpClient
import com.flixclusive.core.ui.common.R as UiCommonR
import com.flixclusive.core.util.R as UtilR

@Composable
internal fun StreamioScreen(
    settings: ProviderSettingsManager,
    client: OkHttpClient
) {
    var reactiveInt by remember { mutableIntStateOf(0) } // Wtf is this?
    val addons = remember(settings.getAddons(), reactiveInt) {
        settings.getAddons()
    }

    var showAddDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        AnimatedContent(
            targetState = addons.isEmpty(),
            label = ""
        ) { state ->
            when(state) {
                true -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No addons found ಠ⌣ಠ",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier
                                .fillMaxSize()
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        item {
                            Text(
                                text = "Addons ヾ(⌐■_■)ノ♪",
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp)
                            )
                        }
                        items(addons) {
                            AddonCard(
                                addon = it,
                                onRemove = {
                                    val success = settings.removeAddon(it)

                                    if (success) {
                                        reactiveInt++
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .fillMaxWidth()
                .height(70.dp)
        ) {
            ElevatedButton(
                onClick = { showAddDialog = true },
                shape = MaterialTheme.shapes.small,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = UiCommonR.drawable.round_add_24),
                        contentDescription = stringResource(UtilR.string.add_provider)
                    )

                    Text(stringResource(UtilR.string.add_provider))
                }
            }
        }
    }

    if (showAddDialog) {
        AddDialog(
            client = client,
            settings = settings,
            onDismiss = { showAddDialog = false }
        )
    }
}
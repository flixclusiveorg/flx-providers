@file:Suppress("SpellCheckingInspection")

package com.flixclusive.provider.app.stremio.settings

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
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.flixclusive.core.util.android.showToast
import com.flixclusive.core.util.coroutines.FlxDispatchers
import com.flixclusive.core.util.network.json.fromJson
import com.flixclusive.provider.app.stremio.BuildConfig
import com.flixclusive.provider.app.stremio.core.model.Addon
import com.flixclusive.provider.app.stremio.core.util.AddonUtil
import com.flixclusive.provider.app.stremio.core.util.AddonUtil.downloadAddon
import com.flixclusive.provider.app.stremio.core.util.AddonUtil.removeAddon
import com.flixclusive.provider.app.stremio.core.util.AddonUtil.updateAddon
import com.flixclusive.provider.app.stremio.core.util.Failed
import com.flixclusive.provider.app.stremio.core.util.Success
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.flixclusive.provider.extensions.getStringAsFlow
import com.flixclusive.provider.util.res.painterResource
import com.flixclusive.provider.util.res.stringResource
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient

@Composable
internal fun StreamioScreen(
    prefs: DataStore<Preferences>,
    client: OkHttpClient
) {
    val context = LocalContext.current

    val addons = remember { mutableStateSetOf<Addon>() }

    LaunchedEffect(prefs) {
        prefs.getStringAsFlow(AddonUtil.STREAMIO_ADDONS_KEY)
            .filterNotNull()
            .distinctUntilChanged()
            .collectLatest { raw ->
                val list = fromJson<List<Addon>>(raw)

                addons.addAll(list)
            }
    }

    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var editAddon by remember { mutableStateOf<Addon?>(null) }

    val scope = rememberCoroutineScope()
    var updateJob by remember { mutableStateOf<Job?>(null) }

    fun updateAddon(addon: Addon) {
        if (updateJob?.isActive == true)
            return

        updateJob = scope.launch {
            val url = addon.baseUrl ?: return@launch
            val updatedAddon = client.downloadAddon(url = url)

            if (updatedAddon == null) {
                context.showToast("Failed to parse addon url [${addon.name}]")
                return@launch
            }

            val message = when (val response = prefs.updateAddon(addon = updatedAddon)) {
                is Failed -> "Failed to add addon [${updatedAddon.name}]: ${response.error}"
                Success -> "Addon [${updatedAddon.name}] has been updated!"
                else -> null
            }

            if (message != null) {
                context.showToast(message)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        AnimatedContent(
            targetState = addons.isEmpty(),
            label = ""
        ) { state ->
            when (state) {
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
                        item(
                            key = null,
                            contentType = null
                        ) {
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

                        items(
                            count = addons.size,
                            key = { addons.elementAt(it).id },
                            contentType = { null }
                        ) { index ->
                            val addon = addons.elementAt(index)

                            AddonCard(
                                addon = addon,
                                onEdit = { editAddon = addon },
                                onUpdateManifest = { updateAddon(addon) },
                                onRemove = {
                                    FlxDispatchers.launchOnIO {
                                        val success = prefs.removeAddon(addon)

                                        FlxDispatchers.withMainContext {
                                            if (success) {
                                                context.showToast("Addon [${addon.name}] has been removed")
                                            } else {
                                                context.showToast("Failed to remove addon [${addon.name}]")
                                            }
                                        }
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
                    horizontalArrangement = Arrangement.spacedBy(
                        8.dp,
                        Alignment.CenterHorizontally
                    ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource("add", BuildConfig.LIBRARY_PACKAGE_NAME),
                        contentDescription = stringResource(
                            name = "add_addon_icon_content_desc",
                            packageName = BuildConfig.LIBRARY_PACKAGE_NAME
                        )
                    )

                    Text("Add stremio addon")
                }
            }
        }
    }

    if (showAddDialog || editAddon != null) {
        AddDialog(
            client = client,
            settings = prefs,
            addonToEdit = editAddon,
            onDismiss = {
                showAddDialog = false
                editAddon = null
            }
        )
    }
}
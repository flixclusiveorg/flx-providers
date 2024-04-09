package com.flxProviders.flixhq.settings

import android.content.res.Resources
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.ui.common.util.showToast
import com.flixclusive.core.util.exception.safeCall
import com.flixclusive.core.util.network.ignoreAllSSLErrors
import com.flxProviders.flixhq.api.FlixHQApi
import com.flxProviders.flixhq.extractors.vidcloud.dto.VidCloudKey
import com.flxProviders.flixhq.settings.util.getString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient

@Composable
internal fun MainSettingsPanel(
    key: VidCloudKey,
    setKey: (VidCloudKey) -> Unit,
    resources: Resources
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isKeySetAlready by remember(key) { mutableStateOf(key.e4Key.isNotEmpty()) }
    var isKeyWorking by remember(key) { mutableStateOf(isKeySetAlready) }
    var isGettingKey by remember { mutableStateOf(false) }

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val labelToUse = when(isKeySetAlready) {
                true -> resources.getString("key_initialized")
                false -> resources.getString("key_uninitialized")
            }

            Text(
                text = labelToUse,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 18.sp
                ),
                modifier = Modifier
                    .weight(1F)
            )

            ElevatedButton(
                onClick = { isGettingKey = true },
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = resources.getString("set_key"),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }


        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val labelToUse = when(isKeyWorking) {
                true -> resources.getString("key_working")
                false -> resources.getString("key_not_working")
            }

            Text(
                text = labelToUse,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 18.sp
                ),
                modifier = Modifier
                    .weight(1F)
            )

            ElevatedButton(
                onClick = {
                    scope.launch(Dispatchers.IO) {
                        isKeyWorking = testKey(key)

                        val message = if (isKeyWorking) {
                            resources.getString("key_working_toast_message")
                        } else resources.getString("key_not_working_toast_message")

                        launch(Dispatchers.Main) {
                            context.showToast(message)
                        }
                    }
                },
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = resources.getString("test_key"),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }

    if (isGettingKey) {
        WebViewKeyGetter(
            resources = resources,
            setKey = {
                isKeySetAlready = true
                setKey(it)
            },
            onDismiss = { isGettingKey = false },
        )
    }
}

private suspend fun testKey(key: VidCloudKey): Boolean {
    return safeCall {
        val api = FlixHQApi(
            client = OkHttpClient.Builder()
                .ignoreAllSSLErrors()
                .build(),
            key = key
        )

        api.getSourceLinks(
            filmId = SHAWSHANK_REDEMPTION_WATCH_ID,
            onLinkLoaded = {},
            onSubtitleLoaded = {},
        )

        return@safeCall true
    } ?: false
}
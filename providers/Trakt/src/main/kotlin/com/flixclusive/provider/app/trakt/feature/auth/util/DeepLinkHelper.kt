package com.flixclusive.provider.app.trakt.feature.auth.util

import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.core.util.Consumer

@Composable
internal fun ObserveOauthDeepLinkUri(onDeepLink: (Uri) -> Unit) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val callback by rememberUpdatedState(onDeepLink)

    LaunchedEffect(Unit) {
        activity?.intent?.data?.let { uri ->
            callback(uri)
            activity.intent.data = null // consume it
        }
    }

    DisposableEffect(activity) {
        val listener = Consumer<Intent> { newIntent ->
            newIntent.data?.let { uri ->
                callback(uri)
                newIntent.data = null // consume it
            }
        }
        activity?.addOnNewIntentListener(listener)
        onDispose {
            activity?.removeOnNewIntentListener(listener)
        }
    }
}
package com.flixclusive.provider.app.trakt.feature.auth.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.flixclusive.provider.app.trakt.TraktPlugin
import com.flixclusive.provider.app.trakt.core.config.TraktApiConfig
import com.flixclusive.provider.app.trakt.core.ui.LoadingScreen
import com.flixclusive.provider.app.trakt.feature.auth.AuthState

private const val TRAKT_ICON_URL = "https://i.imgur.com/cwmhW7c.png"

@Composable
internal fun TraktPlugin.AuthGuardScreen(
    state: AuthState,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    AnimatedContent(
        targetState = state,
        transitionSpec = {
            slideInHorizontally { -it / 6 } + fadeIn() togetherWith
                    fadeOut() + slideOutHorizontally { -it / 6 }
        },
        modifier = modifier.fillMaxSize()
    ) { state ->
        when (state) {
            is AuthState.Loading, is AuthState.Expired -> LoadingScreen(message = "Checking authentication status...")
            is AuthState.Authenticated -> content()
            else -> UnauthenticatedScreen()
        }
    }
}

@Composable
private fun TraktPlugin.UnauthenticatedScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier.fillMaxSize()
    ) {
        AsyncImage(
            model = remember {
                ImageRequest.Builder(context)
                    .data(TRAKT_ICON_URL)
                    .crossfade(true)
                    .build()
            },
            contentDescription = "Trakt Logo",
            modifier = Modifier
                .fillMaxWidth(0.3f)
                .aspectRatio(1f)
                .padding(bottom = 16.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Text(
                "Please authenticate with Trakt to use this provider.",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier
                    .padding(top = 16.dp)
                    .align(Alignment.CenterHorizontally)
            )

            Button(
                onClick = {
                    uriHandler.openUri(TraktApiConfig.getAuthorizeAppUri(id))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .align(Alignment.CenterHorizontally)
            ) {
                Text("Authenticate with Trakt")
            }
        }
    }
}
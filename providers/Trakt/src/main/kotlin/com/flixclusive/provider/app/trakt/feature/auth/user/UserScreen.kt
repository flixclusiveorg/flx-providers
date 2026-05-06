package com.flixclusive.provider.app.trakt.feature.auth.user

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.flixclusive.provider.app.trakt.core.model.TraktUser
import com.flixclusive.provider.app.trakt.core.ui.LoadingScreen

@Composable
internal fun UserScreen(
    userState: UserState,
    toggles: () -> List<SettingsToggleItem>,
    onLogout: () -> Unit,
    onRetry: () -> Unit,
    onToggle: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedContent(
        targetState = userState,
        transitionSpec = {
            slideInHorizontally { -it / 6 } + fadeIn() togetherWith
                    fadeOut() + slideOutHorizontally { -it / 6 }
        },
        modifier = modifier.fillMaxSize()
    ) { state ->
        when (state) {
            is UserState.LoggingOut -> LoadingScreen(message = "Logging out...")
            is UserState.Loading -> LoadingScreen()
            is UserState.Error -> RetryErrorScreen(onRetry = onRetry, modifier = modifier)
            is UserState.Success -> UserScreenContent(
                user = state.user,
                onLogout = onLogout,
                toggles = toggles,
                onToggle = onToggle,
                modifier = modifier
            )
        }
    }
}

@Composable
private fun RetryErrorScreen(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Failed to load user data.")
        Spacer(modifier = Modifier.size(16.dp))
        OutlinedButton(onClick = onRetry) {
            Text("Retry")
        }
    }
}

@Composable
private fun UserScreenContent(
    user: TraktUser,
    toggles: () -> List<SettingsToggleItem>,
    onToggle: (String, Boolean) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(16.dp),
    ) {
        item {
            AsyncImage(
                model = remember {
                    ImageRequest.Builder(context)
                        .data(user.avatar)
                        .crossfade(true)
                        .build()
                },
                contentDescription = user.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(160.dp)
                    .aspectRatio(1f)
                    .clip(CircleShape)
            )
        }

        item {
            Text(
                text = user.name,
                style = MaterialTheme.typography.headlineMedium
            )
        }

        item {
            Text(
                text = "@${user.username}",
                style = MaterialTheme.typography.labelMedium,
                color = LocalContentColor.current.copy(alpha = 0.7f)
            )
        }

        item {
            OutlinedButton(
                onClick = onLogout,
                modifier = Modifier
                    .fillMaxWidth(0.7f)
            ) {
                Text("Logout")
            }
        }

        items(
            toggles(),
            key = { it.key }
        ) { toggle ->
            SettingToggle(
                item = toggle,
                onCheckedChange = { onToggle(toggle.key, it) }
            )
        }
    }
}
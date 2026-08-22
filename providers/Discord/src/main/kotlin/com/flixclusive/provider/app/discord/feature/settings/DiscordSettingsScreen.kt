package com.flixclusive.provider.app.discord.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flixclusive.core.util.log.errorLog
import com.flixclusive.provider.app.discord.core.config.DiscordConfig
import com.flixclusive.provider.app.discord.core.config.PrefsKey
import com.flixclusive.provider.app.discord.core.model.DiscordAuthToken
import com.flixclusive.provider.app.discord.core.network.DiscordAuthService
import com.flixclusive.provider.app.discord.core.theme.DiscordTheme
import com.flixclusive.provider.app.discord.feature.auth.AuthState
import com.flixclusive.provider.app.discord.feature.auth.toAuthState
import com.flixclusive.provider.app.discord.feature.auth.util.ObserveOauthDeepLinkUri
import com.flixclusive.provider.extensions.getBoolAsFlow
import com.flixclusive.provider.extensions.getObjectAsFlow
import com.flixclusive.provider.extensions.getString
import com.flixclusive.provider.extensions.remove
import com.flixclusive.provider.extensions.setBool
import com.flixclusive.provider.extensions.setString
import kotlinx.coroutines.launch

private val Connected = Color(0xFF23A55A)
private val Expired = Color(0xFFF0B232)
private val Offline = Color(0xFF80848E)

@Composable
internal fun DiscordSettingsScreen(
    settings: DataStore<Preferences>,
    providerId: String,
    authService: DiscordAuthService,
    onCodeReceived: suspend (code: String, verifier: String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    var previewKind by remember { mutableStateOf(PreviewKind.MOVIE) }

    val token by settings
        .getObjectAsFlow<DiscordAuthToken>(PrefsKey.AUTH)
        .collectAsStateWithLifecycle(initialValue = null)

    val enabled by settings
        .getBoolAsFlow(PrefsKey.ENABLED, defValue = true)
        .collectAsStateWithLifecycle(initialValue = true)

    val authState = token.toAuthState()

    ObserveOauthDeepLinkUri { uri ->
        val code = uri.getQueryParameter("code") ?: return@ObserveOauthDeepLinkUri
        val returnedState = uri.getQueryParameter("state")
        val expectedState = settings.getString(PrefsKey.OAUTH_STATE, null)

        if (returnedState == null || returnedState != expectedState) {
            errorLog("Discord: OAuth state mismatch, ignoring redirect.")
            return@ObserveOauthDeepLinkUri
        }

        val verifier = settings.getString(PrefsKey.PKCE_VERIFIER, null)
        if (verifier == null) {
            errorLog("Discord: no PKCE verifier stored, cannot complete sign-in.")
            return@ObserveOauthDeepLinkUri
        }

        runCatching { onCodeReceived(code, verifier) }
            .onFailure { errorLog("Discord: token exchange failed — ${it.message}") }

        settings.remove(PrefsKey.OAUTH_STATE)
        settings.remove(PrefsKey.PKCE_VERIFIER)
    }

    DiscordTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                Header()

                SectionCard {
                    StatusRow(authState)

                    if (authState is AuthState.Authenticated) {
                        OutlinedButton(
                            onClick = { scope.launch { settings.remove(PrefsKey.AUTH) } },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(text = "Disconnect")
                        }
                    } else {
                        Button(
                            onClick = {
                                scope.launch {
                                    val pkce = authService.generatePkcePair()
                                    val state = authService.generateState()

                                    settings.setString(PrefsKey.PKCE_VERIFIER, pkce.verifier)
                                    settings.setString(PrefsKey.OAUTH_STATE, state)

                                    uriHandler.openUri(
                                        DiscordConfig.authorizeUrl(providerId, pkce.challenge, state),
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(text = "Connect Discord")
                        }
                    }
                }

                Section(title = "Activity") {
                    SectionCard {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "Show what I'm watching", fontWeight = FontWeight.Medium)
                                Caption(text = "Updates your Discord status while something is playing.")
                            }

                            Switch(
                                checked = enabled,
                                onCheckedChange = { scope.launch { settings.setBool(PrefsKey.ENABLED, it) } },
                            )
                        }
                    }
                }

                Section(title = "Preview") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PreviewKind.entries.forEach { kind ->
                            FilterChip(
                                selected = previewKind == kind,
                                onClick = { previewKind = kind },
                                label = { Text(text = kind.label) },
                            )
                        }
                    }

                    DiscordCardPreview(activity = sampleActivity(previewKind))
                }
            }
        }
    }
}

@Composable
private fun Header() {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "Discord Rich Presence",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
        )
        Caption(text = "Share what you're watching on your Discord profile.")
    }
}

@Composable
private fun Section(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = title.uppercase(),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        content()
    }
}

@Composable
private fun SectionCard(content: @Composable () -> Unit) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        content()
    }
}

@Composable
private fun StatusRow(authState: AuthState) {
    val (color, label, caption) =
        when (authState) {
            is AuthState.Authenticated -> Triple(Connected, "Connected", "Your activity is being shared.")
            is AuthState.Expired -> Triple(Expired, "Session expired", "Reconnect to keep sharing your activity.")
            AuthState.Unauthenticated -> Triple(Offline, "Not connected", "Connect an account to get started.")
            AuthState.Loading -> Triple(Offline, "Checking…", "Reading your saved session.")
        }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(color),
        )

        Column {
            Text(text = label, fontWeight = FontWeight.SemiBold)
            Caption(text = caption)
        }
    }
}

@Composable
private fun Caption(text: String) =
    Text(
        text = text,
        fontSize = 13.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

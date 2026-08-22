package com.flixclusive.provider.app.letterboxd.feature.settings

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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.flixclusive.provider.app.letterboxd.core.config.LetterboxdConfig
import com.flixclusive.provider.app.letterboxd.core.config.PrefsKey
import com.flixclusive.provider.app.letterboxd.core.model.AuthToken
import com.flixclusive.provider.app.letterboxd.core.model.Member
import com.flixclusive.provider.app.letterboxd.core.theme.LetterboxdGreen
import com.flixclusive.provider.app.letterboxd.core.theme.LetterboxdOrange
import com.flixclusive.provider.app.letterboxd.core.theme.LetterboxdTheme
import com.flixclusive.provider.app.letterboxd.feature.auth.AuthState
import com.flixclusive.provider.app.letterboxd.feature.auth.toAuthState
import com.flixclusive.provider.extensions.getBoolAsFlow
import com.flixclusive.provider.extensions.getObjectAsFlow
import com.flixclusive.provider.extensions.setBool
import kotlinx.coroutines.launch

private val Offline = Color(0xFF80848E)

@Composable
internal fun LetterboxdSettingsScreen(
    settings: DataStore<Preferences>,
    onSignIn: suspend (username: String, password: String) -> Unit,
    onSignOut: suspend () -> Unit,
) {
    val scope = rememberCoroutineScope()

    val token by settings
        .getObjectAsFlow<AuthToken>(PrefsKey.MEMBER_AUTH)
        .collectAsStateWithLifecycle(initialValue = null)

    val member by settings
        .getObjectAsFlow<Member>(PrefsKey.MEMBER)
        .collectAsStateWithLifecycle(initialValue = null)

    val listManagement by settings
        .getBoolAsFlow(PrefsKey.LIST_MANAGEMENT, PrefsKey.DEFAULT_LIST_MANAGEMENT)
        .collectAsStateWithLifecycle(initialValue = PrefsKey.DEFAULT_LIST_MANAGEMENT)

    val markWatched by settings
        .getBoolAsFlow(PrefsKey.MARK_WATCHED, PrefsKey.DEFAULT_MARK_WATCHED)
        .collectAsStateWithLifecycle(initialValue = PrefsKey.DEFAULT_MARK_WATCHED)

    val diaryLogging by settings
        .getBoolAsFlow(PrefsKey.DIARY_LOGGING, PrefsKey.DEFAULT_DIARY_LOGGING)
        .collectAsStateWithLifecycle(initialValue = PrefsKey.DEFAULT_DIARY_LOGGING)

    val authState = token.toAuthState()

    LetterboxdTheme {
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

                if (!LetterboxdConfig.hasCredentials) {
                    NoticeCard(
                        title = "No API credentials",
                        body =
                            "This build has no Letterboxd client id or secret compiled in, " +
                                "so nothing will load. Rebuild with them set in local.properties.",
                    )
                }

                SectionCard {
                    AccountRow(authState = authState, member = member)

                    if (authState is AuthState.Authenticated) {
                        OutlinedButton(
                            onClick = { scope.launch { onSignOut() } },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(text = "Sign out")
                        }
                    } else {
                        SignInForm(
                            enabled = LetterboxdConfig.hasCredentials,
                            onSubmit = onSignIn,
                        )
                    }
                }

                Section(title = "Sync") {
                    SectionCard {
                        ToggleRow(
                            title = "Watchlist and lists",
                            description = "Browse and edit your Letterboxd watchlist from Flixclusive.",
                            checked = listManagement,
                            onCheckedChange = { scope.launch { settings.setBool(PrefsKey.LIST_MANAGEMENT, it) } },
                        )

                        ToggleRow(
                            title = "Mark films as watched",
                            description = "When you finish a film, mark it watched on Letterboxd.",
                            checked = markWatched,
                            onCheckedChange = { scope.launch { settings.setBool(PrefsKey.MARK_WATCHED, it) } },
                        )

                        ToggleRow(
                            title = "Log films to your diary",
                            description = "When you finish a film, add a dated diary entry. This is public on your profile.",
                            checked = diaryLogging,
                            accent = LetterboxdOrange,
                            onCheckedChange = { scope.launch { settings.setBool(PrefsKey.DIARY_LOGGING, it) } },
                        )
                    }
                }

                Caption(
                    text =
                        "Letterboxd covers films only and provides no streams — " +
                            "pair it with a source provider for playback.",
                )
            }
        }
    }
}

/**
 * Credentials are sent straight to Letterboxd's token endpoint and never stored — only
 * the tokens that come back are.
 */
@Composable
private fun SignInForm(
    enabled: Boolean,
    onSubmit: suspend (username: String, password: String) -> Unit,
) {
    val scope = rememberCoroutineScope()

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var revealPassword by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val canSubmit = enabled && !isSubmitting && username.isNotBlank() && password.isNotBlank()

    fun submit() {
        if (!canSubmit) return

        scope.launch {
            isSubmitting = true
            error = null

            runCatching { onSubmit(username.trim(), password) }
                .onSuccess { password = "" }
                .onFailure { error = it.message?.takeIf { m -> m.isNotBlank() } ?: "Sign in failed." }

            isSubmitting = false
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = username,
            onValueChange = {
                username = it
                error = null
            },
            label = { Text(text = "Username") },
            singleLine = true,
            enabled = enabled && !isSubmitting,
            isError = error != null,
            keyboardOptions =
                KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next,
                ),
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                error = null
            },
            label = { Text(text = "Password") },
            singleLine = true,
            enabled = enabled && !isSubmitting,
            isError = error != null,
            visualTransformation =
                if (revealPassword) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions =
                KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
            keyboardActions = KeyboardActions(onDone = { submit() }),
            trailingIcon = {
                TextButton(onClick = { revealPassword = !revealPassword }) {
                    Text(text = if (revealPassword) "Hide" else "Show", fontSize = 12.sp)
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )

        error?.let {
            Text(
                text = it,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.error,
            )
        }

        Button(
            onClick = ::submit,
            enabled = canSubmit,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(18.dp),
                )
            } else {
                Text(text = "Sign in")
            }
        }

        Caption(text = "Your password is sent to Letterboxd to get a token, and is never stored.")
    }
}

@Composable
private fun Header() {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = "Letterboxd", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Caption(text = "Browse film charts and lists, and sync your watchlist and diary.")
    }
}

@Composable
private fun AccountRow(
    authState: AuthState,
    member: Member?,
) {
    val (color, label, caption) =
        when (authState) {
            is AuthState.Authenticated ->
                Triple(
                    LetterboxdGreen,
                    member?.displayName ?: "Signed in",
                    member?.username?.let { "letterboxd.com/$it" } ?: "Your account is linked.",
                )

            is AuthState.Expired ->
                Triple(LetterboxdOrange, "Session expired", "Sign in again to keep syncing.")

            AuthState.Unauthenticated ->
                Triple(Offline, "Not signed in", "Browsing works without an account; syncing needs one.")

            AuthState.Loading ->
                Triple(Offline, "Checking…", "Reading your saved session.")
        }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val avatar = member?.avatarUrl
        if (authState is AuthState.Authenticated && avatar != null) {
            AsyncImage(
                model = avatar,
                contentDescription = null,
                modifier = Modifier.size(40.dp).clip(CircleShape),
            )
        } else {
            Box(
                modifier =
                    Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(color),
            )
        }

        Column {
            Text(text = label, fontWeight = FontWeight.SemiBold)
            Caption(text = caption)
        }
    }
}

@Composable
private fun ToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    accent: Color? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontWeight = FontWeight.Medium,
                color = accent ?: MaterialTheme.colorScheme.onSurface,
            )
            Caption(text = description)
        }

        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun NoticeCard(
    title: String,
    body: String,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(LetterboxdOrange.copy(alpha = 0.12f))
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(text = title, fontWeight = FontWeight.SemiBold, color = LetterboxdOrange)
        Caption(text = body)
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
private fun Caption(text: String) =
    Text(
        text = text,
        fontSize = 13.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

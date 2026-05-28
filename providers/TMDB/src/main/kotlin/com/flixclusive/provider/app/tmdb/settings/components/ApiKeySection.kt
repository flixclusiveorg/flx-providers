package com.flixclusive.provider.app.tmdb.settings.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flixclusive.provider.app.tmdb.BuildConfig
import com.flixclusive.provider.app.tmdb.core.config.KEY_API_KEY
import com.flixclusive.provider.extensions.getStringAsFlow
import com.flixclusive.provider.extensions.setString
import kotlinx.coroutines.launch

@Composable
internal fun ApiKeySection(
    settings: DataStore<Preferences>,
    onSaveSuccess: suspend () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val storedKey by settings.getStringAsFlow(KEY_API_KEY).collectAsStateWithLifecycle(initialValue = null)
    val displayKey = storedKey ?: BuildConfig.TMDB_API_KEY

    var passwordVisible by remember { mutableStateOf(false) }
    var fieldOverride by rememberSaveable { mutableStateOf<String?>(null) }
    val fieldValue = fieldOverride ?: displayKey
    val hasChanges = fieldOverride != null && fieldOverride!!.trim() != displayKey

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "API Key",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = fieldValue,
            onValueChange = { fieldOverride = it },
            modifier = Modifier.fillMaxWidth(),
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            label = { Text("TMDB API Key") },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    AnimatedVisibility(
                        visible = passwordVisible,
                        enter = fadeIn(),
                        exit = fadeOut(),
                    ) {
                        Icon(
                            painter = painterResource(android.R.drawable.ic_menu_view),
                            contentDescription = "Hide API Key",
                        )
                    }
                    AnimatedVisibility(
                        visible = !passwordVisible,
                        enter = fadeIn(),
                        exit = fadeOut(),
                    ) {
                        Icon(
                            painter = painterResource(android.R.drawable.ic_menu_view),
                            contentDescription = "Show API Key",
                        )
                    }
                }
            },
            singleLine = true,
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(Modifier.weight(1f))
            FilledTonalButton(
                onClick = {
                    fieldOverride = null
                    scope.launch { settings.setString(KEY_API_KEY, BuildConfig.TMDB_API_KEY) }
                },
            ) {
                Text("Reset to default")
            }
            AnimatedVisibility(visible = hasChanges) {
                Row {
                    Spacer(Modifier.padding(start = 8.dp))
                    Button(
                        onClick = {
                            val key = fieldValue.trim()
                            fieldOverride = null
                            scope.launch {
                                settings.setString(KEY_API_KEY, key)
                                onSaveSuccess()
                            }
                        },
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

package com.flixclusive.provider.app.tmdb.settings.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.unit.dp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flixclusive.provider.app.tmdb.core.config.DEFAULT_LANGUAGE
import com.flixclusive.provider.app.tmdb.core.config.KEY_ADULT
import com.flixclusive.provider.app.tmdb.core.config.KEY_LANGUAGE
import com.flixclusive.provider.extensions.getStringAsFlow
import com.flixclusive.provider.extensions.setString
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
internal fun PreferencesSection(
    settings: DataStore<Preferences>,
    onSaveSuccess: suspend () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()

    val storedAdult by settings.getStringAsFlow(KEY_ADULT)
        .map { it ?: "false" }
        .collectAsStateWithLifecycle(initialValue = "false")
    val storedLanguage by settings.getStringAsFlow(KEY_LANGUAGE)
        .map { it ?: DEFAULT_LANGUAGE }
        .collectAsStateWithLifecycle(initialValue = DEFAULT_LANGUAGE)

    var adultOverride by rememberSaveable { mutableStateOf<Boolean?>(null) }
    var langOverride by rememberSaveable { mutableStateOf<String?>(null) }
    val adultField = adultOverride ?: (storedAdult == "true")
    val langField = langOverride ?: storedLanguage
    var showLanguagePicker by rememberSaveable { mutableStateOf(false) }

    val hasChanges = (adultOverride != null && adultOverride != (storedAdult == "true")) ||
        (langOverride != null && langOverride!!.trim() != storedLanguage)

    val displayLang = remember(langField) {
        val locale = Locale.forLanguageTag(langField)
        val name = locale.getDisplayName(locale).replaceFirstChar { it.uppercase() }
        if (name.isNotBlank()) name else langField
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Include Adult Content",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "Show adult-rated titles in results",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = adultField,
                onCheckedChange = { adultOverride = if (it == (storedAdult == "true")) null else it },
            )
        }
        Spacer(Modifier.height(12.dp))
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = displayLang,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Language") },
                supportingText = { Text(langField) },
                singleLine = true,
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                    ) { showLanguagePicker = true },
            )
        }
        if (showLanguagePicker) {
            LanguagePickerSheet(
                currentLanguage = langField,
                onDismiss = { showLanguagePicker = false },
                onSelect = { langOverride = if (it.trim() == storedLanguage) null else it },
            )
        }
        AnimatedVisibility(visible = hasChanges) {
            Column {
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        val lang = langField.trim().ifBlank { DEFAULT_LANGUAGE }
                        adultOverride = null
                        langOverride = null
                        scope.launch {
                            settings.setString(KEY_ADULT, adultField.toString())
                            settings.setString(KEY_LANGUAGE, lang)
                            onSaveSuccess()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                ) {
                    Text("Save")
                }
            }
        }
    }
}

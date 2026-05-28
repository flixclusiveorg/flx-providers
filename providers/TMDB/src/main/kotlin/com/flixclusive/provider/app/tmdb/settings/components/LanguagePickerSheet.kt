package com.flixclusive.provider.app.tmdb.settings.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Locale

private data class LanguageOption(val tag: String, val displayName: String)

private val languageTagRegex = Regex("^[a-zA-Z]{2,3}(-[a-zA-Z]{2,3})?$")

private fun buildLanguageList(): List<LanguageOption> =
    Locale.getAvailableLocales()
        .asSequence()
        .filter { it.language.isNotEmpty() && it.script.isEmpty() }
        .map { Locale.forLanguageTag(it.toLanguageTag()) }
        .distinctBy { it.toLanguageTag() }
        .filter { it.toLanguageTag().matches(languageTagRegex) }
        .map { locale ->
            LanguageOption(
                tag = locale.toLanguageTag(),
                displayName = locale.getDisplayName(locale)
                    .replaceFirstChar { c -> c.uppercase() },
            )
        }
        .filter { it.displayName.isNotBlank() }
        .sortedBy { it.displayName }
        .toList()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LanguagePickerSheet(
    currentLanguage: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
) {
    val deviceTag = remember {
        val l = Locale.getDefault()
        if (l.country.isNotEmpty()) "${l.language}-${l.country}" else l.language
    }
    val languages = remember { buildLanguageList() }
    var searchQuery by rememberSaveable { mutableStateOf("") }

    val filtered = remember(searchQuery) {
        val q = searchQuery.trim()
        if (q.isBlank()) languages
        else languages.filter {
            it.displayName.contains(q, ignoreCase = true) || it.tag.contains(q, ignoreCase = true)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                BottomSheetDefaults.DragHandle()
            }
            Text(
                text = "Select Language",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
            )
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp),
            ) {
                items(filtered, key = { it.tag }) { lang ->
                    val isDevice = lang.tag == deviceTag
                    val isSelected = lang.tag == currentLanguage
                    ListItem(
                        headlineContent = {
                            Text(
                                text = buildString {
                                    append(lang.displayName)
                                    if (isDevice) append(" (Default)")
                                },
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            )
                        },
                        supportingContent = { Text(lang.tag) },
                        trailingContent = if (isSelected) ({
                            Text(
                                text = "✓",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                            )
                        }) else null,
                        modifier = Modifier.clickable {
                            onSelect(lang.tag)
                            onDismiss()
                        },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

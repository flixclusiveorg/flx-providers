package com.flixclusive.provider.app.tmdb.settings

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flixclusive.provider.app.tmdb.core.config.CACHE_MAX_AGE
import com.flixclusive.provider.app.tmdb.core.config.CACHE_MAX_STALE
import com.flixclusive.provider.app.tmdb.core.config.KEY_BACKDROP_DETAIL
import com.flixclusive.provider.app.tmdb.core.config.KEY_BACKDROP_PARTIAL
import com.flixclusive.provider.app.tmdb.core.config.KEY_CACHE_MAX_AGE_PREF
import com.flixclusive.provider.app.tmdb.core.config.KEY_CACHE_MAX_STALE_PREF
import com.flixclusive.provider.app.tmdb.core.config.KEY_CATALOGS
import com.flixclusive.provider.app.tmdb.core.config.KEY_POSTER_DETAIL
import com.flixclusive.provider.app.tmdb.core.config.KEY_POSTER_PARTIAL
import com.flixclusive.provider.app.tmdb.core.config.TMDB_API_BASE_URL
import com.flixclusive.provider.app.tmdb.core.model.UserCatalog
import com.flixclusive.provider.app.tmdb.core.model.loadCatalogSeed
import com.flixclusive.provider.app.tmdb.settings.components.ApiKeySection
import com.flixclusive.provider.app.tmdb.settings.components.CatalogCardItem
import com.flixclusive.provider.app.tmdb.settings.components.PreferencesSection
import com.flixclusive.provider.extensions.getObjectAsFlow
import com.flixclusive.provider.extensions.getStringAsFlow
import com.flixclusive.provider.extensions.setObject
import com.flixclusive.provider.extensions.setString
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import okio.FileNotFoundException
import java.io.File

private val TmdbPrimaryBlue = Color(0xFF01B4E4)
private val TmdbNavy = Color(0xFF032541)
private val TmdbGreen = Color(0xFF90CEA1)

private val TmdbDarkColors = darkColorScheme(
    primary = TmdbPrimaryBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF013A52),
    onPrimaryContainer = Color(0xFFB0E8F8),
    secondary = Color(0xFFCCCCCC),
    onSecondary = Color(0xFF1A1A1A),
    secondaryContainer = Color(0xFF2E2E2E),
    onSecondaryContainer = Color(0xFFE0E0E0),
    tertiary = TmdbGreen,
    onTertiary = TmdbNavy,
    background = Color(0xFF0D1117),
    onBackground = Color(0xFFDDE3EC),
    surface = TmdbNavy,
    onSurface = Color(0xFFDDE3EC),
    surfaceVariant = Color(0xFF1E2B38),
    onSurfaceVariant = Color(0xFF8C9BAA),
    outline = Color(0xFF3A4855),
    outlineVariant = Color(0xFF252F3A),
)

@Composable
private fun TMDBTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TmdbDarkColors,
        content = content,
    )
}

private val json by lazy { Json { ignoreUnknownKeys = true } }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TMDBSettingsScreen(
    settings: DataStore<Preferences>,
    onClearCache: () -> Unit = {},
    cacheDir: File? = null,
) {
    TMDBTheme {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val allCatalogs by settings.getObjectAsFlow<List<UserCatalog>>(KEY_CATALOGS)
            .collectAsStateWithLifecycle(initialValue = null)

        val snackbarHostState = remember { SnackbarHostState() }

        LaunchedEffect(allCatalogs) {
            if (allCatalogs == null) {
                val seed = try {
                    loadCatalogSeed(context.applicationContext)
                } catch (_: FileNotFoundException) {
                    snackbarHostState.showSnackbar("Failed to load default catalogs.")
                    return@LaunchedEffect
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("Error loading catalogs: ${e.localizedMessage}")
                    return@LaunchedEffect
                }
                settings.setObject(KEY_CATALOGS, seed)
            }
        }

        val catalogs = allCatalogs ?: emptyList()
        val grouped = remember(catalogs) { catalogs.groupBy { it.category } }
        var showAddSheet by rememberSaveable { mutableStateOf(false) }
        var editTarget by remember { mutableStateOf<UserCatalog?>(null) }
        var showImportSheet by rememberSaveable { mutableStateOf(false) }

        Scaffold(
            contentWindowInsets = WindowInsets(0),
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = { showAddSheet = true },
                    icon = {
                        Icon(
                            painter = painterResource(android.R.drawable.ic_input_add),
                            contentDescription = "Add catalog",
                        )
                    },
                    text = { Text("Add Catalog") },
                )
            },
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = innerPadding,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item(key = "api_key_header") {
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItem(),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Authentication",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Spacer(Modifier.height(12.dp))
                            ApiKeySection(
                                settings = settings,
                                onSaveSuccess = { snackbarHostState.showSnackbar("Reload the app to apply the new TMDB config.") },
                            )
                        }
                    }
                }

                item(key = "global_prefs") {
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItem(),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Preferences",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Spacer(Modifier.height(12.dp))
                            PreferencesSection(
                                settings = settings,
                                onSaveSuccess = { snackbarHostState.showSnackbar("Reload the app to apply the new TMDB config.") },
                            )
                        }
                    }
                }

                item(key = "image_quality") {
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItem(),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Image Quality",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Spacer(Modifier.height(12.dp))
                            ImageQualitySection(settings = settings)
                        }
                    }
                }

                item(key = "cache") {
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItem(),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Cache",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Spacer(Modifier.height(12.dp))
                            CacheSection(
                                settings = settings,
                                onClearCache = onClearCache,
                                cacheDir = cacheDir,
                                onSaveSuccess = { snackbarHostState.showSnackbar("Reload the app to apply the new TMDB config.") },
                            )
                        }
                    }
                }

                item(key = "catalog_actions") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItem(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    try {
                                        val seed = loadCatalogSeed(context.applicationContext)
                                        settings.setObject(KEY_CATALOGS, seed)
                                        snackbarHostState.showSnackbar("Catalogs restored to defaults.")
                                    } catch (e: Exception) {
                                        snackbarHostState.showSnackbar("Failed to restore: ${e.localizedMessage}")
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Restore")
                        }
                        OutlinedButton(
                            onClick = {
                                val text = json.encodeToString(
                                    ListSerializer(UserCatalog.serializer()),
                                    catalogs,
                                )
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/json"
                                    putExtra(Intent.EXTRA_TEXT, text)
                                    putExtra(Intent.EXTRA_SUBJECT, "TMDB Catalogs")
                                }
                                context.startActivity(
                                    Intent.createChooser(
                                        intent,
                                        "Export Catalogs"
                                    )
                                )
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Export")
                        }
                        OutlinedButton(
                            onClick = { showImportSheet = true },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Import")
                        }
                    }
                }

                grouped.entries.sortedBy { it.key }.forEach { (category, items) ->
                    item(key = "header_$category") {
                        SectionHeader(
                            title = categoryDisplayName(category),
                            modifier = Modifier.animateItem(),
                        )
                    }

                    itemsIndexed(
                        items,
                        key = { i, c -> "${c.category}_${c.name}_$i" }) { _, catalog ->
                        CatalogCardItem(
                            catalog = catalog,
                            modifier = Modifier.animateItem(),
                            onToggle = { enabled ->
                                scope.launch {
                                    val updated = catalogs.map {
                                        if (it.name == catalog.name && it.category == catalog.category)
                                            it.copy(enabled = enabled)
                                        else it
                                    }
                                    settings.setObject(KEY_CATALOGS, updated)
                                }
                            },
                            onEdit = { editTarget = catalog },
                            onDelete = {
                                scope.launch {
                                    val updated = catalogs.filter {
                                        !(it.name == catalog.name && it.category == catalog.category)
                                    }
                                    settings.setObject(KEY_CATALOGS, updated)
                                }
                            },
                        )
                    }

                    item(key = "divider_$category") {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                        )
                    }
                }


            }
        }

        if (showAddSheet || editTarget != null) {
            CatalogEditSheet(
                initial = editTarget,
                onDismiss = {
                    showAddSheet = false
                    editTarget = null
                },
                onConfirm = { name, url ->
                    scope.launch {
                        val updated = if (editTarget != null) {
                            catalogs.map {
                                if (it.name == editTarget!!.name && it.category == editTarget!!.category)
                                    it.copy(name = name, url = url)
                                else it
                            }
                        } else {
                            catalogs + UserCatalog(name = name, url = url, category = "custom")
                        }
                        settings.setObject(KEY_CATALOGS, updated)
                    }
                    showAddSheet = false
                    editTarget = null
                },
            )
        }

        if (showImportSheet) {
            ImportCatalogsSheet(
                onDismiss = { showImportSheet = false },
                onApply = { text ->
                    scope.launch {
                        try {
                            val parsed = json.decodeFromString(
                                ListSerializer(UserCatalog.serializer()),
                                text,
                            )
                            settings.setObject(KEY_CATALOGS, parsed)
                            snackbarHostState.showSnackbar("Imported ${parsed.size} catalog(s).")
                        } catch (e: Exception) {
                            snackbarHostState.showSnackbar("Invalid JSON: ${e.localizedMessage}")
                        }
                        showImportSheet = false
                    }
                },
            )
        }
    }
}

@Composable
private fun ImageQualitySection(settings: DataStore<Preferences>) {
    val scope = rememberCoroutineScope()
    val posterPartial by settings.getStringAsFlow(KEY_POSTER_PARTIAL)
        .collectAsStateWithLifecycle(initialValue = "w500")
    val posterDetail by settings.getStringAsFlow(KEY_POSTER_DETAIL)
        .collectAsStateWithLifecycle(initialValue = "w500")
    val backdropPartial by settings.getStringAsFlow(KEY_BACKDROP_PARTIAL)
        .collectAsStateWithLifecycle(initialValue = "w500")
    val backdropDetail by settings.getStringAsFlow(KEY_BACKDROP_DETAIL)
        .collectAsStateWithLifecycle(initialValue = "original")

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        QualityRow(
            label = "Poster — Cards",
            selected = posterPartial ?: "w500",
            options = listOf("w200", "w500"),
            onSelect = { scope.launch { settings.setString(KEY_POSTER_PARTIAL, it) } },
        )
        QualityRow(
            label = "Poster — Detail",
            selected = posterDetail ?: "w500",
            options = listOf("w500", "original"),
            onSelect = { scope.launch { settings.setString(KEY_POSTER_DETAIL, it) } },
        )
        QualityRow(
            label = "Backdrop — Cards",
            selected = backdropPartial ?: "w500",
            options = listOf("w500", "original"),
            onSelect = { scope.launch { settings.setString(KEY_BACKDROP_PARTIAL, it) } },
        )
        QualityRow(
            label = "Backdrop — Detail",
            selected = backdropDetail ?: "original",
            options = listOf("w500", "original"),
            onSelect = { scope.launch { settings.setString(KEY_BACKDROP_DETAIL, it) } },
        )
    }
}

@Composable
private fun QualityRow(
    label: String,
    selected: String,
    options: List<String>,
    onSelect: (String) -> Unit,
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { opt ->
                FilterChip(
                    selected = selected == opt,
                    onClick = { if (selected != opt) onSelect(opt) },
                    label = { Text(opt) },
                )
            }
        }
    }
}

@Composable
private fun CacheSection(
    settings: DataStore<Preferences>,
    onClearCache: () -> Unit,
    cacheDir: File?,
    onSaveSuccess: suspend () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var cacheSize by remember { mutableStateOf<String?>(null) }
    var cacheKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(cacheKey) {
        cacheSize = cacheDir?.let { dir ->
            if (dir.exists()) {
                val bytes = dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
                "%.1f MB".format(bytes / 1_048_576.0)
            } else "0.0 MB"
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text("HTTP Cache", style = MaterialTheme.typography.bodyMedium)
            Text(
                text = cacheSize ?: "–",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        OutlinedButton(onClick = { onClearCache(); cacheKey++ }) {
            Text("Clear")
        }
    }

    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

    val storedMaxAge by settings.getStringAsFlow(KEY_CACHE_MAX_AGE_PREF)
        .collectAsStateWithLifecycle(initialValue = null)
    val storedMaxStale by settings.getStringAsFlow(KEY_CACHE_MAX_STALE_PREF)
        .collectAsStateWithLifecycle(initialValue = null)
    val displayMaxAge = storedMaxAge ?: CACHE_MAX_AGE.toString()
    val displayMaxStale = storedMaxStale ?: CACHE_MAX_STALE.toString()

    var maxAgeOverride by rememberSaveable { mutableStateOf<String?>(null) }
    var maxStaleOverride by rememberSaveable { mutableStateOf<String?>(null) }
    val maxAgeField = maxAgeOverride ?: displayMaxAge
    val maxStaleField = maxStaleOverride ?: displayMaxStale
    val hasChanges = (maxAgeOverride != null && maxAgeOverride!!.trim() != displayMaxAge) ||
            (maxStaleOverride != null && maxStaleOverride!!.trim() != displayMaxStale)

    OutlinedTextField(
        value = maxAgeField,
        onValueChange = { maxAgeOverride = it },
        label = { Text("Online freshness (seconds)") },
        supportingText = { Text("How long to reuse saved data before fetching fresh info. Default: 8 hours (28800 s)") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(4.dp))
    OutlinedTextField(
        value = maxStaleField,
        onValueChange = { maxStaleOverride = it },
        label = { Text("Offline fallback (seconds)") },
        supportingText = { Text("How long old saved data can still be shown when there\u2019s no internet. Default: 7 days (604800 s)") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )

    AnimatedVisibility(visible = hasChanges) {
        Button(
            onClick = {
                val age = maxAgeField.trim().ifBlank { CACHE_MAX_AGE.toString() }
                val stale = maxStaleField.trim().ifBlank { CACHE_MAX_STALE.toString() }
                maxAgeOverride = null
                maxStaleOverride = null
                scope.launch {
                    settings.setString(KEY_CACHE_MAX_AGE_PREF, age)
                    settings.setString(KEY_CACHE_MAX_STALE_PREF, stale)
                    onSaveSuccess()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        ) {
            Text("Save")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImportCatalogsSheet(
    onDismiss: () -> Unit,
    onApply: (String) -> Unit,
) {
    var importText by rememberSaveable { mutableStateOf("") }

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
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                BottomSheetDefaults.DragHandle()
            }
            Text(
                text = "Import Catalogs",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            OutlinedTextField(
                value = importText,
                onValueChange = { importText = it },
                label = { Text("Paste JSON here") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            )
            Button(
                onClick = { onApply(importText) },
                modifier = Modifier.fillMaxWidth(),
                enabled = importText.isNotBlank(),
            ) {
                Text("Apply")
            }
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Cancel")
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(vertical = 6.dp, horizontal = 4.dp),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CatalogEditSheet(
    initial: UserCatalog?,
    onDismiss: () -> Unit,
    onConfirm: (name: String, url: String) -> Unit,
) {
    val parsedUri = remember(initial?.url) {
        initial?.url?.let { runCatching { it.toUri() }.getOrNull() }
    }
    var name by rememberSaveable { mutableStateOf(initial?.name ?: "") }
    var baseUrl by rememberSaveable {
        mutableStateOf(
            parsedUri?.let { "${it.scheme}://${it.host}${it.path}" } ?: (initial?.url
                ?: TMDB_API_BASE_URL),
        )
    }
    var queryParams by remember {
        mutableStateOf(
            parsedUri?.queryParameterNames
                ?.flatMap { key -> parsedUri.getQueryParameters(key).map { key to it } }
                ?: emptyList(),
        )
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
                .padding(bottom = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxWidth(),
            ) {
                BottomSheetDefaults.DragHandle()
            }

            Text(
                text = if (initial == null) "Add Catalog" else "Edit Catalog",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier.fillMaxWidth(),
            )
            HorizontalDivider()
            Text(
                text = "URL Builder",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            OutlinedTextField(
                value = baseUrl,
                onValueChange = { baseUrl = it },
                label = { Text("Base URL") },
                placeholder = { Text("https://api.themoviedb.org/3/\u2026") },
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            if (queryParams.isNotEmpty()) {
                Text(
                    text = "Query Parameters",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            queryParams.forEachIndexed { index, (key, value) ->
                UrlEditRow(
                    key = key,
                    value = value,
                    onKeyChange = { newKey ->
                        queryParams = queryParams.mapIndexed { i, p ->
                            if (i == index) newKey to p.second else p
                        }
                    },
                    onValueChange = { newValue ->
                        queryParams = queryParams.mapIndexed { i, p ->
                            if (i == index) p.first to newValue else p
                        }
                    },
                    onDelete = {
                        queryParams = queryParams.filterIndexed { i, _ -> i != index }
                    },
                )
            }
            OutlinedButton(
                onClick = { queryParams = queryParams + ("" to "") },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("+ Add Parameter")
            }
            Spacer(Modifier.height(8.dp))
            val validParams = queryParams.filter { it.first.isNotBlank() }
            val currentUrl = if (validParams.isEmpty()) baseUrl.trim()
            else baseUrl.trim() + "?" + validParams.joinToString("&") { "${it.first}=${it.second}" }
            val hasChanges = if (initial == null) {
                name.isNotBlank() && baseUrl.isNotBlank()
            } else {
                (name.trim() != initial.name || currentUrl != initial.url) &&
                        name.isNotBlank() && baseUrl.isNotBlank()
            }
            Button(
                onClick = {
                    onConfirm(name.trim(), currentUrl)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = hasChanges,
            ) {
                Text("Save")
            }
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Cancel")
            }
        }
    }
}

@Composable
private fun UrlEditRow(
    key: String,
    value: String,
    onKeyChange: (String) -> Unit,
    onValueChange: (String) -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        BasicTextField(
            value = key,
            onValueChange = onKeyChange,
            textStyle = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium,
                color = TmdbPrimaryBlue,
            ),
            cursorBrush = SolidColor(TmdbPrimaryBlue),
            modifier = Modifier.weight(0.4f),
            singleLine = true,
        )
        Text(
            text = "=",
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                color = TmdbPrimaryBlue,
            ),
        )
        Box(modifier = Modifier.weight(0.6f)) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                singleLine = true,
            )
        }
        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(24.dp),
        ) {
            Icon(
                painter = painterResource(android.R.drawable.ic_menu_delete),
                contentDescription = "Delete parameter",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

private fun categoryDisplayName(category: String): String = when (category) {
    "all" -> "Trending & Mixed"
    "movie" -> "Movies"
    "tv" -> "TV Shows"
    "network" -> "Networks"
    "studio" -> "Studios"
    "genre" -> "Genres"
    "type" -> "Browse by Type"
    "custom" -> "Custom"
    else -> category.replaceFirstChar { it.uppercase() }
}

package com.flxProviders.stremio.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.flixclusive.core.ui.common.util.createTextFieldValue
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.core.ui.common.util.showToast
import com.flixclusive.provider.settings.ProviderSettingsManager
import com.flxProviders.stremio.settings.AddonUtil.addAddon
import com.flxProviders.stremio.settings.AddonUtil.getManifest
import com.flxProviders.stremio.settings.AddonUtil.parseStremioAddonUrl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import com.flixclusive.core.ui.common.R as UiCommonR
import com.flixclusive.core.util.R as UtilR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddDialog(
    settings: ProviderSettingsManager,
    client: OkHttpClient,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val clipboardManager = LocalClipboardManager.current

    val scope = rememberCoroutineScope()


    val buttonMinHeight = 60.dp

    var textFieldValue by remember {
        val text = clipboardManager.getText()?.text
            ?.let {
                when {
                    it.contains("manifest.json") -> parseStremioAddonUrl(it)
                    else -> ""
                }
            } ?: ""

        mutableStateOf(text.createTextFieldValue())
    }

    var isError by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    var addJob by remember { mutableStateOf<Job?>(null) }

    fun onAdd(url: String) {
        if (addJob?.isActive == true) return

        addJob = scope.launch {
            if (url.isBlank()) {
                isError = true
                return@launch
            }

            val addon = withContext(Dispatchers.IO) {
                client.getManifest(addonUrl = url)
            }
            if (addon == null) {
                context.showToast("[Stremio]> Failed to parse addon url [$url]")
                isError = true
                return@launch
            }

            if (addon.isCatalog) {
                context.showToast("[Stremio]> ${addon.name} is a catalog. Flixclusive doesn't support those yet.")
                isError = true
                return@launch
            }

            val success = settings.addAddon(addon = addon)
            if (!success) {
                context.showToast("[Stremio]> Failed to add addon")
            }

            onDismiss()
        }
    }

    BasicAlertDialog(
        onDismissRequest = onDismiss
    ) {
        Surface(
            shape = RoundedCornerShape(10),
        ) {
            Column(
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(10.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .padding(horizontal = 10.dp)
                ) {
                    Text(
                        text = "Enter Stremio Addon URL",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .padding(top = 15.dp)
                    )

                    TextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 15.dp)
                            .focusRequester(focusRequester),
                        value = textFieldValue,
                        isError = isError,
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodySmall,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                        shape = MaterialTheme.shapes.extraSmall,
                        onValueChange = {
                            isError = false
                            textFieldValue = it
                        },
                        keyboardActions = KeyboardActions(
                            onGo = {
                                focusManager.clearFocus(force = false)
                                keyboardController?.hide()

                                if(textFieldValue.text.isEmpty()) {
                                    isError = true
                                    return@KeyboardActions
                                }

                                onAdd(textFieldValue.text)
                            }
                        ),
                        placeholder = {
                            Text(
                                text = "Paste addon url",
                                style = MaterialTheme.typography.bodySmall,
                                color = LocalContentColor.current.onMediumEmphasis(),
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 1
                            )
                        },
                        trailingIcon = {
                            AnimatedVisibility(
                                visible = textFieldValue.text.isNotEmpty(),
                                enter = scaleIn(),
                                exit = scaleOut(),
                            ) {
                                IconButton(
                                    onClick = {
                                        textFieldValue = "".createTextFieldValue()
                                    }
                                ) {
                                    Icon(
                                        painter = painterResource(UiCommonR.drawable.round_close_24),
                                        contentDescription = stringResource(UtilR.string.clear_text_button)
                                    )
                                }
                            }
                        },
                    )
                }

                Row {
                    Button(
                        onClick = {
                            val isDirtyUrl =
                                textFieldValue.text.contains("stremio://", true)
                                || textFieldValue.text.contains("manifest.json", true)

                            if (isDirtyUrl) {
                                textFieldValue = parseStremioAddonUrl(textFieldValue.text).createTextFieldValue()
                            }

                            keyboardController?.hide()
                            focusManager.clearFocus(force = false)

                            onAdd(textFieldValue.text)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary.onMediumEmphasis(),
                            contentColor = Color.White
                        ),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .weight(weight = 1F, fill = true)
                            .heightIn(min = buttonMinHeight)
                            .padding(5.dp)
                    ) {
                        Text(
                            text = "Add",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .padding(end = 2.dp)
                        )
                    }

                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant.onMediumEmphasis()
                        ),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .weight(weight = 1F, fill = true)
                            .heightIn(min = buttonMinHeight)
                            .padding(5.dp)
                    ) {
                        Text(
                            text = stringResource(id = UtilR.string.cancel),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Light
                        )
                    }
                }
            }
        }
    }
}
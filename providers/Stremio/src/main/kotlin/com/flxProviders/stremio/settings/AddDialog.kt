package com.flxProviders.stremio.settings

import android.widget.Toast
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.flixclusive.core.util.android.showToast
import com.flixclusive.provider.settings.ProviderSettings
import com.flixclusive.provider.util.res.painterResource
import com.flixclusive.provider.util.res.stringResource
import com.flxProviders.stremio.BuildConfig
import com.flxProviders.stremio.api.model.Addon
import com.flxProviders.stremio.settings.util.AddonUtil.addAddon
import com.flxProviders.stremio.settings.util.AddonUtil.downloadAddon
import com.flxProviders.stremio.settings.util.AddonUtil.parseStremioAddonUrl
import com.flxProviders.stremio.settings.util.Duplicate
import com.flxProviders.stremio.settings.util.Failed
import com.flxProviders.stremio.settings.util.Success
import com.flxProviders.stremio.settings.util.createTextFieldValue
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddDialog(
    settings: ProviderSettings,
    client: OkHttpClient,
    addonToEdit: Addon?,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val clipboardManager = LocalClipboardManager.current

    val scope = rememberCoroutineScope()
    var addJob by remember { mutableStateOf<Job?>(null) }


    val buttonMinHeight = 60.dp

    var textFieldValue by remember {
        var text = clipboardManager.getText()?.text
            ?.let {
                when {
                    it.contains("manifest.json") || it.contains("stremio://") -> parseStremioAddonUrl(it)
                    else -> ""
                }
            } ?: ""

        if (addonToEdit != null)
            text = addonToEdit.baseUrl ?: text

        mutableStateOf(text.createTextFieldValue())
    }

    var isError by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }



    fun addToAddonList(url: String) {
        if (addJob?.isActive == true) return

        addJob = scope.launch {
            if(url.isBlank()) {
                isError = true
                return@launch
            }

            val addon = client.downloadAddon(url = url)

            if (addon == null) {
                context.showToast("Failed to parse addon url [${textFieldValue.text}]")
                return@launch
            }

            when (val response = settings.addAddon(addon = addon)) {
                Duplicate -> {
                    context.showToast("Addon [${addon.name}] is already installed!")
                }
                is Failed -> {
                    context.showToast("Failed to add addon [${addon.name}]: ${response.error}")
                }
                Success -> {
                    if (addon.needsConfiguration) {
                        context.showToast(
                            message = "${addon.name} seems to need configuration. Make sure you've configured the addon properly.",
                            duration = Toast.LENGTH_LONG
                        )
                    }

                    if (addon.hasCatalog) {
                        context.showToast(
                            message = "${addon.name} has catalogs. In order to load them, you must restart the app.",
                            duration = Toast.LENGTH_LONG
                        )
                    } else context.showToast("Addon [${addon.name}] has been added!")
                }
            }

            onDismiss()
        }
    }

    val onClickAdd = onClickAdd@ {
        var text = textFieldValue.text

        val isDirtyUrl =
            text.contains("stremio://", true)
            || text.contains("manifest.json", true)

        if (isDirtyUrl) {
            text = parseStremioAddonUrl(text)
        }

        if (text.equals(addonToEdit?.baseUrl, true)) {
            onDismiss()
            return@onClickAdd
        }

        keyboardController?.hide()
        focusManager.clearFocus(force = false)

        addToAddonList(text)
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
                            onGo = { onClickAdd() }
                        ),
                        placeholder = {
                            Text(
                                text = "Paste addon url",
                                style = MaterialTheme.typography.bodySmall,
                                color = LocalContentColor.current.copy(alpha = 0.6F),
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
                                        painter = painterResource(
                                            name = "close",
                                            packageName = BuildConfig.LIBRARY_PACKAGE_NAME
                                        ),
                                        contentDescription = stringResource("clear_text_button_content_desc", BuildConfig.LIBRARY_PACKAGE_NAME)
                                    )
                                }
                            }
                        },
                    )
                }

                Row {
                    Button(
                        onClick = onClickAdd,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6F),
                            contentColor = Color.White
                        ),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .weight(weight = 1F, fill = true)
                            .heightIn(min = buttonMinHeight)
                            .padding(5.dp)
                    ) {
                        Text(
                            text = if (addonToEdit != null) "Update" else "Add",
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
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6F)
                        ),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .weight(weight = 1F, fill = true)
                            .heightIn(min = buttonMinHeight)
                            .padding(5.dp)
                    ) {
                        Text(
                            text = stringResource("cancel", BuildConfig.LIBRARY_PACKAGE_NAME),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Light
                        )
                    }
                }
            }
        }
    }
}
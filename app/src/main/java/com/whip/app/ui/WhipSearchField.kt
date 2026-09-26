package com.whip.app.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction

/** Shared query presentation; filtering, query limits and saved state belong to the caller. */
@Composable
internal fun WhipSearchField(
    label: String,
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    hint: String? = null,
    onSubmit: (() -> Unit)? = null,
    enabled: Boolean = true,
    clearLabel: String = "Clear Search",
) {
    val hintContent: (@Composable () -> Unit)? = hint?.let { text -> { Text(text) } }
    WhipInlineTextField(
        value = query,
        onValueChange = onQueryChange,
        label = label,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        placeholder = hintContent,
        keyboardOptions = if (onSubmit == null) KeyboardOptions.Default else KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = if (onSubmit == null) KeyboardActions.Default else KeyboardActions(onSearch = { onSubmit() }),
        trailingIcon = if (query.isNotEmpty()) {{
            IconButton(onClick = { onQueryChange("") }, enabled = enabled) {
                Icon(Icons.Outlined.Close, contentDescription = clearLabel)
            }
        }} else null,
    )
}

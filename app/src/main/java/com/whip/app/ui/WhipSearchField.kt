package com.whip.app.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Shared query presentation; filtering, query limits and saved state belong to the caller. */
@Composable
internal fun WhipSearchField(
    label: String,
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    WhipInlineTextField(
        value = query,
        onValueChange = onQueryChange,
        label = label,
        modifier = modifier.fillMaxWidth(),
        trailingIcon = if (query.isNotEmpty()) {{
            IconButton(onClick = { onQueryChange("") }) {
                Icon(Icons.Outlined.Close, contentDescription = "Clear Search")
            }
        }} else null,
    )
}

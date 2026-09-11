package com.whip.app.ui

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.VisualTransformation

/**
 * Inline capture and search align their visible outline with neighboring content.
 * An idle empty field uses its name as a placeholder; only a floating label needs
 * Material's extra top inset. Authored values, focus actions and saving stay local.
 */
@Composable
internal fun WhipInlineTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    placeholder: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    var focused by remember { mutableStateOf(false) }
    val hasFloatingLabel = focused || value.isNotEmpty()
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = if (hasFloatingLabel) {{ Text(label) }} else null,
        placeholder = if (hasFloatingLabel) placeholder else {{ Text(label) }},
        modifier = modifier.onFocusChanged { focused = it.isFocused }.semantics { contentDescription = label },
        enabled = enabled,
        trailingIcon = trailingIcon,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = true,
    )
}

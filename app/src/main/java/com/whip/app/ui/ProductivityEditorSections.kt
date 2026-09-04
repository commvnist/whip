package com.whip.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * The shared presentation boundary for the identity portion of productivity
 * editors. Callers retain their draft and validation policy in [identityFields].
 */
@Composable
internal fun ProductivityIdentitySection(
    title: String,
    supportingText: String,
    modifier: Modifier = Modifier,
    identityFields: @Composable ColumnScope.() -> Unit,
    emojiPicker: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(WhipSpacing.compact),
    ) {
        EditorSectionHeader(title, supportingText)
        identityFields()
        emojiPicker()
    }
}

/**
 * The shared presentation boundary for Area ownership and caller-owned
 * organization details such as tags. Area selection semantics stay with each
 * editor through [areaPicker].
 */
@Composable
internal fun ProductivityOrganizationSection(
    supportingText: String,
    modifier: Modifier = Modifier,
    areaPicker: @Composable ColumnScope.() -> Unit,
    extras: @Composable ColumnScope.() -> Unit = {},
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(WhipSpacing.compact),
    ) {
        EditorSectionHeader("Organization", supportingText)
        areaPicker()
        extras()
    }
}

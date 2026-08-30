package com.whip.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics

@Composable
fun DomainLoadContent(
    domain: String,
    innerPadding: PaddingValues,
    errorMessage: String? = null,
    onRetry: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(WhipSpacing.screenCompact)
            .semantics { liveRegion = LiveRegionMode.Polite },
        verticalArrangement = Arrangement.spacedBy(WhipSpacing.compact, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (errorMessage == null) {
            WhipStatusCard(
                kind = WhipStatusKind.Loading,
                title = "Loading ${domain.uiTitleCase()}",
                message = "This content will appear when loading is complete.",
            )
        } else {
            WhipStatusCard(
                kind = WhipStatusKind.Error,
                title = "Could Not Load ${domain.uiTitleCase()}",
                message = errorMessage,
                actionLabel = "Try Again",
                onAction = onRetry,
            )
        }
    }
}

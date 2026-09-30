package com.whip.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics

internal fun domainLoadTitle(domain: String, errorMessage: String?): String =
    if (errorMessage == null) "Loading ${domain.uiTitleCase()}" else "Could Not Load ${domain.uiTitleCase()}"

@Composable
fun DomainLoadContent(
    domain: String,
    innerPadding: PaddingValues,
    errorMessage: String? = null,
    onRetry: () -> Unit = {},
) {
    BoxWithConstraints(Modifier.fillMaxSize().padding(innerPadding)) {
        // Full-screen states own scrolling; a LazyColumn item already has a scroll owner.
        val scrollModifier = if (constraints.hasBoundedHeight) {
            Modifier.verticalScroll(rememberScrollState())
        } else Modifier
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(scrollModifier)
                .padding(WhipSpacing.screenCompact)
                .semantics { liveRegion = LiveRegionMode.Polite },
            verticalArrangement = Arrangement.spacedBy(WhipSpacing.compact, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (errorMessage == null) {
                WhipStatusCard(
                    kind = WhipStatusKind.Loading,
                    title = domainLoadTitle(domain, errorMessage),
                    message = "This content will appear when loading is complete.",
                )
            } else {
                WhipStatusCard(
                    kind = WhipStatusKind.Error,
                    title = domainLoadTitle(domain, errorMessage),
                    message = errorMessage,
                    actionLabel = "Try Again",
                    onAction = onRetry,
                )
            }
        }
    }
}

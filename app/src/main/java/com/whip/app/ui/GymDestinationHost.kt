package com.whip.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag

/**
 * Navigation chrome for the Gym workspace.
 *
 * Route state, persistence coordinators, editors, and overlays deliberately stay
 * with [GymAreaContent]; this host owns only the stable destination presentation
 * seam so Library children cannot drift from the primary Gym navigation.
 */
@Composable
internal fun GymDestinationHost(
    destination: GymDestination,
    summary: String = destination.label,
    innerPadding: PaddingValues,
    navigationVisible: Boolean,
    onSelect: (GymDestination) -> Unit,
    onBackToLibrary: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(innerPadding),
    ) {
        if (navigationVisible) {
            DestinationTabBar(
                selected = destination.takeUnless { it in libraryGymDestinations } ?: GymDestination.Library,
                destinations = primaryGymDestinations,
                onSelect = onSelect,
                label = GymDestination::label,
                testTagPrefix = "gym-destination",
                barTestTag = "gym-workspace-navigation",
            )
        }
        if (navigationVisible && destination in libraryGymDestinations) {
            WhipWorkspaceHeader(
                summary = "Library",
                onBack = onBackToLibrary,
                backLabel = "Back to Gym Library",
                backModifier = Modifier.testTag("gym-library-child-${destination.name}"),
            )
        }
        if (navigationVisible && destination !in libraryGymDestinations) WhipWorkspaceHeader(summary)
        content()
    }
}

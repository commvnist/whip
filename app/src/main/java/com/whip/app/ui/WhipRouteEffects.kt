package com.whip.app.ui

import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.whip.app.domain.Area
import com.whip.app.domain.AreaScope
import com.whip.app.domain.ScheduledTask

@Composable
internal fun CompletedTaskRouteEffect(
    completedItemKey: String?,
    completedItem: ScheduledTask?,
    loading: Boolean,
    onClear: () -> Unit,
) {
    LaunchedEffect(
        completedItemKey,
        completedItem?.completedAtMillis,
        completedItem?.occurrenceState,
        loading,
    ) {
        if (
            completedItemKey != null &&
            !loading &&
            (completedItem == null || completedItem.completedAtMillis == null)
        ) {
            onClear()
        }
    }
}

@Composable
internal fun AreaRouteFeedbackEffects(
    areaMoveNotice: String?,
    areaMoveRestoreScope: String?,
    pendingAreaBadgeId: String?,
    areas: List<Area>,
    snackbarHostState: SnackbarHostState,
    onAreaMoveConsumed: () -> Unit,
    onAreaBadgeConsumed: () -> Unit,
    onSelectAreaScope: (AreaScope) -> Unit,
    presentFeedback: (String, Int, Boolean, suspend () -> Unit) -> Unit,
) {
    LaunchedEffect(areaMoveNotice) {
        val message = areaMoveNotice ?: return@LaunchedEffect
        onAreaMoveConsumed()
        presentFeedback("area-move", 2, true) {
            val result = snackbarHostState.showSnackbar(
                message,
                actionLabel = "Restore view",
                withDismissAction = true,
            )
            if (result == SnackbarResult.ActionPerformed) {
                onSelectAreaScope(AreaScope.fromStorageKey(areaMoveRestoreScope))
            }
        }
    }

    LaunchedEffect(pendingAreaBadgeId) {
        val id = pendingAreaBadgeId ?: return@LaunchedEffect
        val area = areas.firstOrNull { it.id == id && !it.archived }
        onAreaBadgeConsumed()
        if (area != null) {
            onSelectAreaScope(AreaScope.One(id))
            presentFeedback("area-badge", 2, false) {
                val result = snackbarHostState.showSnackbar(
                    "Showing ${area.name}",
                    actionLabel = "Show all",
                    withDismissAction = true,
                )
                if (result == SnackbarResult.ActionPerformed) onSelectAreaScope(AreaScope.All)
            }
        }
    }
}

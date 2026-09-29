package com.whip.app.ui

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.whip.app.domain.HabitDayProgress

@Stable
internal class HomeHabitValueEditor(
    private val selectedId: MutableState<Long?>,
    private val additive: MutableState<Boolean>,
) {
    fun setTotal(item: HabitDayProgress) { additive.value = false; selectedId.value = item.habit.id }
    fun addAmount(item: HabitDayProgress) { additive.value = true; selectedId.value = item.habit.id }
    fun close() { selectedId.value = null }

    @Composable fun Content(state: HabitUiState, viewModel: HabitViewModel?) {
        val item = selectedId.value?.let { id -> (state.today + state.all).firstOrNull { it.habit.id == id } }
        HomeHabitValueRoute(item, additive.value, selectedId, viewModel, state.customUnits)
    }
}
@Composable
internal fun rememberHomeHabitValueEditor(): HomeHabitValueEditor {
    val id = rememberSaveable { mutableStateOf<Long?>(null) }
    val additive = rememberSaveable { mutableStateOf(false) }
    return remember(id, additive) { HomeHabitValueEditor(id, additive) }
}

@Composable
private fun HomeHabitValueRoute(
    item: HabitDayProgress?,
    additive: Boolean,
    itemIdState: MutableState<Long?>,
    viewModel: HabitViewModel?,
    customUnits: List<com.whip.app.domain.UnitDefinition> = emptyList(),
) {
    item ?: return
    viewModel ?: return
    val authoredMutationState by viewModel.authoredMutationState.collectAsStateWithLifecycle()
    val authoredMutationCoordinator = rememberPersistenceRequestCoordinator(
        state = authoredMutationState,
        consume = viewModel::consumeAuthoredMutationResult,
        key = "home-habit-${item.habit.id}",
        requestNamespace = "home-habit-quick",
        onPersisted = { itemIdState.value = null },
    )
    HabitValueDialog(
        item = item,
        additive = additive,
        customUnits = customUnits,
        saving = authoredMutationCoordinator.saving,
        persistenceError = authoredMutationCoordinator.errorMessage,
        onDismiss = {
            authoredMutationCoordinator.clear()
            itemIdState.value = null
        },
        onLog = { value, note ->
            val requestId = authoredMutationCoordinator.begin() ?: return@HabitValueDialog
            val accepted = if (additive || item.habit.trackingMode == com.whip.app.domain.HabitTrackingMode.LogOnly) {
                viewModel.log(item.habit.id, value, date = item.date, note = note, requestId = requestId)
            } else {
                viewModel.setPeriodValue(item, requireNotNull(value), note, requestId = requestId)
            }
            if (!accepted) {
                authoredMutationCoordinator.finishFailure(
                    "Another Habit history change is already finishing.",
                )
            }
        },
    )
}

package com.whip.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.whip.app.domain.UnitDefinition
import com.whip.app.domain.UnitDimension
import com.whip.app.core.PersistenceRequestState
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class CreateCustomUnitAction(
    val state: StateFlow<PersistenceRequestState<CustomUnitMutationReceipt>>,
    val consume: (String) -> Unit,
    val submit: (
        requestId: String,
        requestedId: String,
        name: String,
        symbol: String,
        dimension: UnitDimension,
        factor: Double,
    ) -> Boolean,
)

private val unavailableCustomUnitCreationState =
    MutableStateFlow<PersistenceRequestState<CustomUnitMutationReceipt>>(PersistenceRequestState.Idle)

internal val UnavailableCreateCustomUnitAction = CreateCustomUnitAction(
    state = unavailableCustomUnitCreationState,
    consume = {},
    submit = { _, _, _, _, _, _ -> false },
)

internal fun unitDefinitionDisplayLabel(unit: UnitDefinition): String =
    "${unit.name}${unit.symbol.takeIf(String::isNotBlank)?.let { " ($it)" }.orEmpty()}"

/**
 * A unit chooser whose creation action lives with the choice it affects. The new
 * unit is created for the already-selected dimension and selected immediately.
 */
@Composable
internal fun UnitSelectionField(
    units: List<UnitDefinition>,
    selectedUnitId: String,
    dimension: UnitDimension,
    onSelect: (String) -> Unit,
    onCreateUnit: CreateCustomUnitAction,
    modifier: Modifier = Modifier,
    dialogModifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: String = "Unit",
    supportingText: String = "Create or choose the unit used for targets, check-ins, and history.",
    allowAnyDimension: Boolean = false,
    onDimensionSelect: (UnitDimension) -> Unit = {},
) {
    val available = units.filter { (allowAnyDimension || it.dimension == dimension) && (!it.archived || it.id == selectedUnitId) }
    val selected = available.firstOrNull { it.id == selectedUnitId }
    var expanded by rememberSaveable(label) { mutableStateOf(false) }
    LaunchedEffect(enabled) { if (!enabled) expanded = false }
    var creating by rememberSaveable(label) { mutableStateOf(false) }
    var requestedUnitId by rememberSaveable(label) { mutableStateOf(UUID.randomUUID().toString()) }
    var pendingSelectionId by rememberSaveable(label) { mutableStateOf<String?>(null) }
    val creationState by onCreateUnit.state.collectAsStateWithLifecycle()
    val creationCoordinator = rememberPersistenceRequestCoordinator(
        state = creationState,
        consume = onCreateUnit.consume,
        key = requestedUnitId,
        requestNamespace = "inline-custom-unit-$requestedUnitId",
        onPersisted = { receipt ->
            pendingSelectionId = receipt.unitId
            creating = false
            requestedUnitId = UUID.randomUUID().toString()
        },
        orphanedMessage = "The previous custom-unit save was interrupted. Your draft is still here; retrying with the same identity is safe.",
    )
    LaunchedEffect(pendingSelectionId, units) {
        val createdUnit = units.firstOrNull { it.id == pendingSelectionId } ?: return@LaunchedEffect
        onDimensionSelect(createdUnit.dimension)
        onSelect(createdUnit.id)
        pendingSelectionId = null
    }
    fun createUnit() {
        expanded = false
        pendingSelectionId = null
        creationCoordinator.clear()
        requestedUnitId = UUID.randomUUID().toString()
        creating = true
    }
    val selectionLabel = selected?.let {
        if (it.id == "unitless") "No Unit" else unitDefinitionDisplayLabel(it)
    } ?: if (available.isEmpty()) "Create Custom Unit…" else "Choose Unit"

    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Box(Modifier.fillMaxWidth()) {
            WhipOutlinedButton(
                enabled = enabled,
                onClick = { if (available.isEmpty()) createUnit() else expanded = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .semantics {
                        contentDescription = "$label: $selectionLabel"
                        stateDescription = if (expanded && enabled) "Menu open" else "Menu closed"
                    },
            ) {
                Text(
                    selectionLabel,
                    modifier = Modifier.weight(1f),
                )
                Icon(Icons.Outlined.ArrowDropDown, contentDescription = null)
            }
            DropdownMenu(expanded = expanded && enabled, onDismissRequest = { expanded = false }) {
                available.forEach { unit ->
                    val isSelected = unit.id == selected?.id
                    DropdownMenuItem(
                        text = { Text(if (unit.id == "unitless") "No Unit" else unitDefinitionDisplayLabel(unit)) },
                        leadingIcon = if (isSelected) {{ Icon(Icons.Outlined.Check, contentDescription = null) }} else null,
                        onClick = {
                            pendingSelectionId = null
                            onDimensionSelect(unit.dimension)
                            onSelect(unit.id)
                            expanded = false
                        },
                        modifier = Modifier.semantics { this.selected = isSelected },
                    )
                }
                HorizontalDivider()
                DropdownMenuItem(
                    text = {
                        Column {
                            Text("Create Custom Unit…")
                            Text(
                                if (allowAnyDimension) "Choose its measurement type while creating it" else "For ${dimension.uiLabel().lowercase()} values",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    onClick = ::createUnit,
                )
            }
        }
        Text(
            if (pendingSelectionId != null) "The unit was saved. Waiting for the updated unit list…" else supportingText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    if (creating) {
        CustomUnitDialog(
            modifier = dialogModifier,
            mode = CustomUnitEditMode.Create,
            initialDimension = dimension,
            dimensionLocked = !allowAnyDimension,
            saving = creationCoordinator.saving,
            error = creationCoordinator.errorMessage,
            onDismiss = {
                if (!creationCoordinator.saving) {
                    creating = false
                    creationCoordinator.clear()
                }
            },
            onSave = { name, symbol, selectedDimension, factor ->
                val requestId = creationCoordinator.begin() ?: return@CustomUnitDialog
                if (!onCreateUnit.submit(
                        requestId,
                        requestedUnitId,
                        name,
                        symbol,
                        selectedDimension,
                        factor,
                    )
                ) {
                    creationCoordinator.finishFailure(
                        "Another custom-unit change is still finishing. Review it and try again.",
                    )
                }
            },
        )
    }
}

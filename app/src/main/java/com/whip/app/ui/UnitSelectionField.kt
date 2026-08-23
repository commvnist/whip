package com.whip.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.whip.app.domain.UnitDefinition
import com.whip.app.domain.UnitDimension

internal typealias CreateCustomUnitAction = (
    name: String,
    symbol: String,
    dimension: UnitDimension,
    factor: Double,
    onResult: (Result<String>) -> Unit,
) -> Unit

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
) {
    val available = units.filter { it.dimension == dimension && (!it.archived || it.id == selectedUnitId) }
    val selected = available.firstOrNull { it.id == selectedUnitId } ?: available.firstOrNull()
    var expanded by rememberSaveable(label) { mutableStateOf(false) }
    var creating by rememberSaveable(label) { mutableStateOf(false) }
    var saving by rememberSaveable(label) { mutableStateOf(false) }
    var error by rememberSaveable(label) { mutableStateOf<String?>(null) }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Box(Modifier.fillMaxWidth()) {
            WhipOutlinedButton(
                enabled = enabled && selected != null,
                onClick = { expanded = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .semantics {
                        contentDescription = "$label: ${selected?.let(::unitDefinitionDisplayLabel) ?: "No compatible units"}"
                        stateDescription = if (expanded) "Menu open" else "Menu closed"
                    },
            ) {
                Text(
                    selected?.let(::unitDefinitionDisplayLabel) ?: "No compatible units",
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Icon(Icons.Outlined.ArrowDropDown, contentDescription = null)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                available.forEach { unit ->
                    DropdownMenuItem(
                        text = { Text((if (unit.id == selectedUnitId) "✓  " else "") + unitDefinitionDisplayLabel(unit)) },
                        onClick = { onSelect(unit.id); expanded = false },
                    )
                }
                HorizontalDivider()
                DropdownMenuItem(
                    text = {
                        Column {
                            Text("Create Custom Unit…")
                            Text(
                                "For ${dimension.uiLabel().lowercase()} values",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    onClick = { expanded = false; error = null; creating = true },
                )
            }
        }
        Text(
            supportingText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    if (creating) {
        CustomUnitDialog(
            modifier = dialogModifier,
            mode = CustomUnitEditMode.Create,
            initialDimension = dimension,
            dimensionLocked = true,
            saving = saving,
            error = error,
            onDismiss = { if (!saving) creating = false },
            onSave = { name, symbol, selectedDimension, factor ->
                saving = true
                error = null
                onCreateUnit(name, symbol, selectedDimension, factor) { result ->
                    saving = false
                    result.onSuccess { id ->
                        onSelect(id)
                        creating = false
                    }.onFailure { failure ->
                        error = failure.message ?: "Could not create custom unit"
                    }
                }
            },
        )
    }
}

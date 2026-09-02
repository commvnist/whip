package com.whip.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Check
import com.whip.app.domain.Area

internal data class AreaUiContext(
    val areas: List<Area> = emptyList(),
    val onSelectScope: (String) -> Unit = {},
)

internal val LocalAreaUiContext = staticCompositionLocalOf { AreaUiContext() }

@Composable
internal fun AreaBadge(areaId: String?, areaName: String, modifier: Modifier = Modifier) {
    if (areaId == null || areaName.isBlank()) return
    val context = LocalAreaUiContext.current
    val area = context.areas.firstOrNull { it.id == areaId }
    val archived = area?.archived == true
    val label = areaName + if (archived) " · Archived" else ""
    Surface(
        color = area?.colorArgb?.let { Color(it).copy(alpha = 0.18f) } ?: MaterialTheme.colorScheme.secondaryContainer,
        shape = MaterialTheme.shapes.small,
        modifier = modifier.semantics {
            contentDescription = if (archived) "Area $areaName, archived" else "Area $areaName. Show only this area."
        },
        enabled = !archived,
        onClick = { context.onSelectScope(areaId) },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            area?.colorArgb?.let { color -> Box(Modifier.size(7.dp).clip(CircleShape).background(Color(color))) }
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
internal fun AreaPicker(
    areas: List<Area>,
    selectedAreaId: String?,
    selectedAreaName: String,
    onSelect: (String?, String) -> Unit,
    onCreateArea: (String, Long?, (Result<String>) -> Unit) -> Unit,
    modifier: Modifier = Modifier,
    dialogModifier: Modifier = Modifier,
    inheritedFromScope: Boolean = false,
) {
    var creating by rememberSaveable { mutableStateOf(false) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Area", style = MaterialTheme.typography.labelLarge)
        Text(
            "Group this item with related tasks, habits, goals, and tracks.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        AreaSelectionDropdown(
            areas = areas,
            selectedAreaId = selectedAreaId,
            selectedAreaName = selectedAreaName,
            onSelect = onSelect,
            onCreate = { creating = true },
        )
        if (inheritedFromScope && selectedAreaId != null) {
            Text(
                "Defaulted from the current area view.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    if (creating) {
        CreateAreaDialog(
            modifier = dialogModifier,
            existingAreas = areas,
            onDismiss = { creating = false },
            onCreate = onCreateArea,
            onSelected = { id, name -> onSelect(id, name); creating = false },
        )
    }
}

@Composable
internal fun AreaSelectionDropdown(
    areas: List<Area>,
    selectedAreaId: String?,
    onSelect: (String?, String) -> Unit,
    modifier: Modifier = Modifier,
    selectedAreaName: String = "",
    onCreate: (() -> Unit)? = null,
    nullLabel: String = "Main",
    allowNullSelection: Boolean = false,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    val selected = areas.firstOrNull { it.id == selectedAreaId }
    val effectiveSelectedAreaId = selected?.id ?: selectedAreaId
    val label = selected?.name
        ?: selectedAreaName.takeIf(String::isNotBlank)
        ?: if (allowNullSelection) nullLabel else "Choose Area"
    Box(modifier.fillMaxWidth()) {
        WhipOutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Area selection: $label" },
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                selected?.colorArgb?.let { Box(Modifier.size(10.dp).clip(CircleShape).background(Color(it))) }
                Text(label, modifier = Modifier.weight(1f))
                Icon(Icons.Outlined.ArrowDropDown, contentDescription = null)
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (areas.count { !it.archived } > 8) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it.take(40) },
                    label = { Text("Find area") },
                    singleLine = true,
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
            }
            if (allowNullSelection) {
                val isSelected = selectedAreaId == null
                DropdownMenuItem(
                    text = { Text(nullLabel) },
                    leadingIcon = if (isSelected) {{ Icon(Icons.Outlined.Check, contentDescription = null) }} else null,
                    onClick = { onSelect(null, ""); expanded = false },
                    modifier = Modifier.semantics { this.selected = isSelected },
                )
            }
            areas.filter { (!it.archived || it.id == selectedAreaId) && (query.isBlank() || it.name.contains(query, true)) }
                .forEach { area ->
                    val isSelected = effectiveSelectedAreaId == area.id
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(Modifier.size(20.dp), contentAlignment = Alignment.Center) {
                                    if (isSelected) Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                }
                                area.colorArgb?.let { Box(Modifier.size(9.dp).clip(CircleShape).background(Color(it))) }
                                Text(area.name + if (area.archived) " · Archived" else "")
                            }
                        },
                        onClick = { onSelect(area.id, area.name); expanded = false },
                        modifier = Modifier.semantics {
                            contentDescription = "Area ${area.name}"
                            this.selected = isSelected
                        },
                    )
                }
            onCreate?.let {
                HorizontalDivider()
                DropdownMenuItem(text = { Text("Create Area…") }, onClick = { expanded = false; it() })
            }
        }
    }
}

@Composable
internal fun CreateAreaDialog(
    modifier: Modifier = Modifier,
    existingAreas: List<Area>,
    onDismiss: () -> Unit,
    onCreate: (String, Long?, (Result<String>) -> Unit) -> Unit,
    onSelected: (String, String) -> Unit = { _, _ -> },
    controlledSaving: Boolean? = null,
    controlledError: String? = null,
    onCreateRequested: ((String, Long?) -> Unit)? = null,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var color by rememberSaveable { mutableStateOf<Long?>(null) }
    var localSaving by rememberSaveable { mutableStateOf(false) }
    var localError by rememberSaveable { mutableStateOf<String?>(null) }
    val saving = controlledSaving ?: localSaving
    val error = controlledError ?: localError
    val duplicate = existingAreas.firstOrNull { it.name.equals(name.trim(), true) }
    fun submitCreate(displayName: String, requestedColor: Long?) {
        if (onCreateRequested != null) {
            onCreateRequested(displayName, requestedColor)
        } else {
            localSaving = true
            onCreate(displayName, requestedColor) { result ->
                localSaving = false
                result.onSuccess { onSelected(it, displayName) }
                    .onFailure { localError = it.message ?: "Could not create area" }
            }
        }
    }
    PaneAwareAlertDialog(
        modifier = modifier,
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text("Create Area") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(40); localError = null },
                    label = { Text("Area name") },
                    supportingText = { Text("${name.length}/40") },
                    placeholder = { Text("Enter a name") },
                    isError = error != null,
                    singleLine = true,
                    enabled = !saving,
                    modifier = Modifier.fillMaxWidth(),
                )
                WhipColorField(
                    value = color,
                    onValueChange = { color = it },
                    enabled = !saving,
                    dialogModifier = modifier,
                    modifier = Modifier.fillMaxWidth(),
                )
                duplicate?.let {
                    Text(
                        if (it.archived) {
                            "${it.name} is archived. Restore it to use it again; its saved color will be kept."
                        } else {
                            "${it.name} already exists. Select it instead."
                        },
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            if (duplicate?.archived == true) {
                WhipTextButton(
                    enabled = !saving,
                    onClick = { submitCreate(duplicate.name, duplicate.colorArgb) },
                ) { Text(if (saving) "Restoring…" else "Restore Existing Area") }
            } else if (duplicate != null) {
                WhipTextButton(
                    enabled = !saving,
                    onClick = { onSelected(duplicate.id, duplicate.name) },
                ) { Text("Select Existing") }
            } else {
                WhipTextButton(
                    enabled = name.isNotBlank() && !saving,
                    onClick = {
                        val displayName = name.trim()
                        submitCreate(displayName, color)
                    },
                ) { Text(if (saving) "Saving…" else "Create") }
            }
        },
        dismissButton = { WhipTextButton(enabled = !saving, onClick = onDismiss) { Text("Cancel") } },
    )
}

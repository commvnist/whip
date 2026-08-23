package com.whip.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import com.whip.app.domain.Area

internal val AreaColorPalette = listOf<Long?>(
    null, 0xFF6750A4L, 0xFF1565C0L, 0xFF00897BL, 0xFF2E7D32L, 0xFFF57C00L, 0xFFC62828L, 0xFF6D4C41L,
)

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
        shape = CircleShape,
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
            "Keep Personal, Work, Health, and other parts of life separate.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        AreaSelectionDropdown(areas, selectedAreaId, selectedAreaName, onSelect, onCreate = { creating = true })
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
    selectedAreaName: String = "",
    onSelect: (String?, String) -> Unit,
    modifier: Modifier = Modifier,
    onCreate: (() -> Unit)? = null,
    nullLabel: String = "No area",
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    val selected = areas.firstOrNull { it.id == selectedAreaId }
    val label = selected?.name ?: selectedAreaName.takeIf(String::isNotBlank) ?: nullLabel
    Box(modifier.fillMaxWidth()) {
        OutlinedButton(
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
                Text("▾")
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
            DropdownMenuItem(
                text = { Text((if (selectedAreaId == null) "✓  " else "") + nullLabel) },
                onClick = { onSelect(null, ""); expanded = false },
            )
            areas.filter { (!it.archived || it.id == selectedAreaId) && (query.isBlank() || it.name.contains(query, true)) }
                .forEach { area ->
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                area.colorArgb?.let { Box(Modifier.size(9.dp).clip(CircleShape).background(Color(it))) }
                                Text((if (selectedAreaId == area.id) "✓  " else "") + area.name + if (area.archived) " · Archived" else "")
                            }
                        },
                        onClick = { onSelect(area.id, area.name); expanded = false },
                        modifier = Modifier.semantics { contentDescription = "Area ${area.name}" },
                    )
                }
            onCreate?.let {
                HorizontalDivider()
                DropdownMenuItem(text = { Text("Create area…") }, onClick = { expanded = false; it() })
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
) {
    var name by rememberSaveable { mutableStateOf("") }
    var color by rememberSaveable { mutableStateOf<Long?>(null) }
    var saving by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    val duplicate = existingAreas.firstOrNull { it.name.equals(name.trim(), true) }
    PaneAwareAlertDialog(
        modifier = modifier,
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text("Create area") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(40); error = null },
                    label = { Text("Area name") },
                    supportingText = { Text("${name.length}/40") },
                    placeholder = { Text("Personal, Work, Health…") },
                    isError = error != null,
                    singleLine = true,
                    enabled = !saving,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Color", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AreaColorPalette.forEach { choice ->
                        Surface(
                            onClick = { color = choice },
                            enabled = !saving,
                            color = choice?.let(::Color) ?: MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(if (color == choice) 3.dp else 1.dp, if (color == choice) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
                            shape = CircleShape,
                            modifier = Modifier.size(48.dp).semantics {
                                contentDescription = if (choice == null) "No color${if (color == null) ", selected" else ""}" else "Area color${if (color == choice) ", selected" else ""}"
                            },
                        ) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                if (choice == null) Text("—")
                            }
                        }
                    }
                }
                duplicate?.let { Text("${it.name} already exists. Select it instead.", color = MaterialTheme.colorScheme.primary) }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            if (duplicate != null) {
                TextButton(onClick = { onSelected(duplicate.id, duplicate.name) }) { Text("Select existing") }
            } else {
                TextButton(
                    enabled = name.isNotBlank() && !saving,
                    onClick = {
                        saving = true
                        val displayName = name.trim()
                        onCreate(displayName, color) { result ->
                            saving = false
                            result.onSuccess { onSelected(it, displayName) }
                                .onFailure { error = it.message ?: "Could not create area" }
                        }
                    },
                ) { Text(if (saving) "Saving…" else "Create") }
            }
        },
        dismissButton = { TextButton(enabled = !saving, onClick = onDismiss) { Text("Cancel") } },
    )
}

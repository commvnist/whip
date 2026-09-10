package com.whip.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

internal sealed interface WhipSettingControl {
    data class Toggle(val checked: Boolean, val change: (Boolean) -> Unit) : WhipSettingControl
    data class Choice(val value: String, val options: List<WhipSettingOption>) : WhipSettingControl
    data class Edit(val value: String, val open: () -> Unit) : WhipSettingControl
}

internal data class WhipSettingOption(val label: String, val selected: Boolean, val select: () -> Unit)

/** One preference declares its content and one interaction; the renderer owns its layout. */
internal class WhipSettingItemScope internal constructor() {
    internal var explanation: String? = null
    internal var control: WhipSettingControl? = null

    fun description(text: String?) { explanation = text?.takeIf(String::isNotBlank) }
    fun toggle(checked: Boolean, onChange: (Boolean) -> Unit) = setControl(WhipSettingControl.Toggle(checked, onChange))
    fun edit(value: String, onOpen: () -> Unit) = setControl(WhipSettingControl.Edit(value, onOpen))
    fun <T> choice(values: List<T>, selected: T, text: (T) -> String, onChange: (T) -> Unit) = setControl(
        WhipSettingControl.Choice(text(selected), values.map { value ->
            WhipSettingOption(text(value), value == selected) { onChange(value) }
        }),
    )
    private fun setControl(value: WhipSettingControl) {
        check(control == null) { "A setting item must declare exactly one interaction" }
        control = value
    }
}

@Composable
internal fun WhipSettingItem(
    title: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    itemKey: String = title,
    content: WhipSettingItemScope.() -> Unit,
) {
    val item = WhipSettingItemScope().apply(content)
    val control = checkNotNull(item.control) { "A setting item must declare an interaction" }
    var menuOpen by rememberSaveable(itemKey) { mutableStateOf(false) }
    LaunchedEffect(enabled, control is WhipSettingControl.Choice) {
        if (!enabled || control !is WhipSettingControl.Choice) menuOpen = false
    }
    val interaction = when (control) {
        is WhipSettingControl.Toggle -> Modifier.toggleable(
            value = control.checked, enabled = enabled, role = Role.Switch, onValueChange = control.change,
        )
        is WhipSettingControl.Choice -> Modifier
            .clickable(enabled = enabled, role = Role.Button, onClickLabel = "Choose $title") { menuOpen = true }
            .semantics {
                contentDescription = "$title: ${control.value}"
                stateDescription = if (menuOpen && enabled) "Menu open" else "Menu closed"
            }
        is WhipSettingControl.Edit -> Modifier
            .clickable(enabled = enabled, role = Role.Button, onClickLabel = "Edit $title", onClick = control.open)
            .semantics { stateDescription = "Saved value ${control.value}. Activate to edit." }
    }
    Box {
        Column(
            modifier.fillMaxWidth().then(interaction).heightIn(min = 56.dp).padding(vertical = WhipSpacing.sibling),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Row(Modifier.fillMaxWidth().heightIn(min = 32.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(12.dp))
                Box(Modifier.width(52.dp).heightIn(min = 32.dp), contentAlignment = Alignment.Center) {
                    when (control) {
                        is WhipSettingControl.Toggle -> Switch(
                            checked = control.checked, onCheckedChange = null, enabled = enabled,
                            modifier = Modifier.clearAndSetSemantics {},
                        )
                        is WhipSettingControl.Choice -> Icon(Icons.Outlined.ArrowDropDown, contentDescription = null)
                        is WhipSettingControl.Edit -> Icon(Icons.Outlined.Edit, contentDescription = null)
                    }
                }
            }
            val value = when (control) {
                is WhipSettingControl.Toggle -> null
                is WhipSettingControl.Choice -> control.value
                is WhipSettingControl.Edit -> control.value
            }
            value?.let {
                Text(it, Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            }
            item.explanation?.let {
                Text(it, Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (control is WhipSettingControl.Choice) {
            DropdownMenu(expanded = menuOpen && enabled, onDismissRequest = { menuOpen = false }) {
                control.options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.label) },
                        leadingIcon = if (option.selected) {{ Icon(Icons.Outlined.Check, contentDescription = null) }} else null,
                        modifier = Modifier.semantics {
                            contentDescription = "$title option: ${option.label}"
                            selected = option.selected
                        },
                        onClick = { menuOpen = false; option.select() },
                    )
                }
            }
        }
    }
}

package com.whip.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/** Compact, shared entry point for every user-assigned color in Whip. */
@Composable
internal fun WhipColorField(
    value: Long?,
    onValueChange: (Long?) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Color",
    enabled: Boolean = true,
    dialogModifier: Modifier = Modifier,
) {
    var pickerOpen by rememberSaveable(label) { mutableStateOf(false) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        WhipOutlinedButton(
            onClick = { pickerOpen = true },
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .testTag("color-picker-field")
                .semantics { contentDescription = "$label: ${colorDisplayName(value)}. Choose color." },
        ) {
            ColorPreview(value, Modifier.size(24.dp))
            Text(
                colorDisplayName(value),
                modifier = Modifier.weight(1f).padding(horizontal = 10.dp),
                maxLines = 1,
            )
            Text("Choose", style = MaterialTheme.typography.labelLarge)
        }
    }
    if (pickerOpen) {
        WhipColorPickerDialog(
            title = "Choose $label",
            initialColor = value,
            modifier = dialogModifier,
            onDismiss = { pickerOpen = false },
            onConfirm = {
                onValueChange(it)
                pickerOpen = false
            },
        )
    }
}

/** One palette and one custom-color workflow shared by Areas, Habits, Goals, and Gym. */
@Composable
internal fun WhipColorPickerDialog(
    title: String,
    initialColor: Long?,
    onDismiss: () -> Unit,
    onConfirm: (Long?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val initialOpaque = initialColor?.let(::opaqueColor)
    val initialCustomColor = initialOpaque ?: WhipColorPresets.first { it.name == "Blue" }.argb
    val initialHsv = colorArgbToHsv(initialCustomColor)
    var selectedColor by rememberSaveable(initialColor) { mutableStateOf(initialOpaque) }
    var customExpanded by rememberSaveable(initialColor) {
        mutableStateOf(initialOpaque != null && WhipColorPresets.none { it.argb == initialOpaque })
    }
    var hue by rememberSaveable(initialColor) { mutableFloatStateOf(initialHsv.hue) }
    var saturation by rememberSaveable(initialColor) { mutableFloatStateOf(initialHsv.saturation) }
    var brightness by rememberSaveable(initialColor) { mutableFloatStateOf(initialHsv.brightness) }
    var hexText by rememberSaveable(initialColor) { mutableStateOf(colorArgbToRgbHex(initialCustomColor)) }

    fun adoptColor(color: Long) {
        val opaque = opaqueColor(color)
        selectedColor = opaque
        val hsv = colorArgbToHsv(opaque)
        hue = hsv.hue
        saturation = hsv.saturation
        brightness = hsv.brightness
        hexText = colorArgbToRgbHex(opaque)
    }

    fun adoptSliders() {
        val color = hsvToColorArgb(hue, saturation, brightness)
        selectedColor = color
        hexText = colorArgbToRgbHex(color)
    }

    PaneAwareAlertDialog(
        modifier = modifier.testTag("color-picker-dialog"),
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ColorPreview(selectedColor, Modifier.size(42.dp))
                    Column(Modifier.weight(1f)) {
                        Text(colorDisplayName(selectedColor), fontWeight = FontWeight.SemiBold)
                        Text(
                            selectedColor?.let(::colorArgbToRgbHex) ?: "Uses the app default",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Text("Presets", style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ColorSwatch(
                        name = "Default",
                        color = null,
                        selected = selectedColor == null,
                        onClick = { selectedColor = null; customExpanded = false },
                    )
                    WhipColorPresets.forEach { preset ->
                        ColorSwatch(
                            name = preset.name,
                            color = preset.argb,
                            selected = selectedColor == preset.argb,
                            onClick = { adoptColor(preset.argb); customExpanded = false },
                        )
                    }
                }
                DisclosureButton(
                    label = "Custom Color",
                    expanded = customExpanded,
                    onClick = {
                        customExpanded = !customExpanded
                        if (customExpanded) adoptColor(selectedColor ?: initialCustomColor)
                    },
                    modifier = Modifier.fillMaxWidth().testTag("custom-color-toggle"),
                )
                if (customExpanded) {
                    OutlinedTextField(
                        value = hexText,
                        onValueChange = { updated ->
                            hexText = updated.take(9).uppercase()
                            parseColorArgb(hexText)?.let(::adoptColor)
                        },
                        label = { Text("Hex Color") },
                        supportingText = {
                            Text(if (parseColorArgb(hexText) == null) "Use #RRGGBB" else "Exact RGB value")
                        },
                        isError = parseColorArgb(hexText) == null,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("custom-color-hex"),
                    )
                    ColorSlider(
                        label = "Hue",
                        valueText = "${hue.roundToInt()}°",
                        value = hue,
                        range = 0f..359f,
                        testTag = "custom-color-hue",
                        gradient = listOf(
                            Color.Red,
                            Color.Yellow,
                            Color.Green,
                            Color.Cyan,
                            Color.Blue,
                            Color.Magenta,
                            Color.Red,
                        ),
                        thumbColor = Color(hsvToColorArgb(hue, 1f, 1f)),
                    ) { hue = it; adoptSliders() }
                    ColorSlider(
                        label = "Saturation",
                        valueText = "${(saturation * 100).roundToInt()}%",
                        value = saturation,
                        range = 0f..1f,
                        testTag = "custom-color-saturation",
                        gradient = listOf(
                            Color(hsvToColorArgb(hue, 0f, brightness)),
                            Color(hsvToColorArgb(hue, 1f, brightness)),
                        ),
                        thumbColor = Color(hsvToColorArgb(hue, saturation, brightness)),
                    ) { saturation = it; adoptSliders() }
                    ColorSlider(
                        label = "Brightness",
                        valueText = "${(brightness * 100).roundToInt()}%",
                        value = brightness,
                        range = 0f..1f,
                        testTag = "custom-color-brightness",
                        gradient = listOf(
                            Color.Black,
                            Color(hsvToColorArgb(hue, saturation, 1f)),
                        ),
                        thumbColor = Color(hsvToColorArgb(hue, saturation, brightness)),
                    ) { brightness = it; adoptSliders() }
                }
            }
        },
        confirmButton = {
            WhipTextButton(
                enabled = !customExpanded || parseColorArgb(hexText) != null,
                onClick = { onConfirm(selectedColor) },
            ) { Text("Apply") }
        },
        dismissButton = { WhipTextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun ColorSlider(
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    testTag: String,
    gradient: List<Color>,
    thumbColor: Color,
    onValueChange: (Float) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(valueText, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Box(Modifier.fillMaxWidth().height(40.dp), contentAlignment = Alignment.Center) {
            SegmentedColorTrack(gradient)
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = range,
                colors = SliderDefaults.colors(
                    thumbColor = thumbColor,
                    activeTrackColor = Color.Transparent,
                    inactiveTrackColor = Color.Transparent,
                    activeTickColor = Color.Transparent,
                    inactiveTickColor = Color.Transparent,
                ),
                modifier = Modifier.fillMaxWidth().testTag(testTag).semantics {
                    contentDescription = "$label: $valueText"
                },
            )
        }
    }
}

/**
 * Draws the color scale without a runtime shader. A small number of solid
 * segments is visually continuous at this height and avoids GPU-specific
 * shader failures observed on software-rendered Android devices.
 */
@Composable
private fun SegmentedColorTrack(colors: List<Color>) {
    val segmentCount = ((colors.size - 1).coerceAtLeast(1) * 8)
    Row(
        Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(CircleShape),
    ) {
        repeat(segmentCount) { index ->
            val position = if (segmentCount == 1) 0f else index.toFloat() / (segmentCount - 1)
            val scaledPosition = position * colors.lastIndex
            val startIndex = scaledPosition.toInt().coerceAtMost(colors.lastIndex)
            val endIndex = (startIndex + 1).coerceAtMost(colors.lastIndex)
            val segmentColor = lerp(
                colors[startIndex],
                colors[endIndex],
                scaledPosition - startIndex,
            )
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .background(segmentColor),
            )
        }
    }
}

@Composable
private fun ColorSwatch(name: String, color: Long?, selected: Boolean, onClick: () -> Unit) {
    val swatchColor = color?.let(::Color) ?: MaterialTheme.colorScheme.surfaceVariant
    val foreground = color?.let { readableForeground(it) } ?: MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        onClick = onClick,
        color = swatchColor,
        contentColor = foreground,
        border = BorderStroke(
            if (selected) 3.dp else 1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        ),
        shape = MaterialTheme.shapes.small,
        modifier = Modifier
            .size(48.dp)
            .testTag("color-preset-${name.lowercase().replace(' ', '-')}")
            .semantics {
                role = Role.RadioButton
                this.selected = selected
                contentDescription = "$name color${if (selected) ", selected" else ""}"
            },
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (selected) Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(22.dp))
            else if (color == null) Text("—", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun ColorPreview(value: Long?, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .background(value?.let(::Color) ?: MaterialTheme.colorScheme.surfaceVariant)
            .semantics { contentDescription = "Color preview: ${colorDisplayName(value)}" },
        contentAlignment = Alignment.Center,
    ) {
        if (value == null) Text("—", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

internal fun readableForeground(color: Long): Color {
    fun linear(channel: Long): Double {
        val srgb = channel.toDouble() / 255.0
        return if (srgb <= 0.04045) srgb / 12.92 else Math.pow((srgb + 0.055) / 1.055, 2.4)
    }
    val luminance = 0.2126 * linear((color shr 16) and 0xFF) +
        0.7152 * linear((color shr 8) and 0xFF) +
        0.0722 * linear(color and 0xFF)
    val blackContrast = (luminance + 0.05) / 0.05
    val whiteContrast = 1.05 / (luminance + 0.05)
    return if (blackContrast >= whiteContrast) Color.Black else Color.White
}

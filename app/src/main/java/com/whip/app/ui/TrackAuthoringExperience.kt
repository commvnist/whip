package com.whip.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.whip.app.domain.*
import java.time.LocalDate

internal enum class TrackStarter(val label: String, val description: String) {
    Blank("Blank Track", "Start with a name for each entry, then add your own fields."),
    Reading("Reading Log", "Book or article title, pages read, and optional notes."),
    Spending("Spending Log", "What you bought, the amount, and optional notes."),
    Reflection("Daily Reflection", "A title, a 1–5 mood rating, and optional notes."),
}

internal fun TrackStarter.draft(areaId: String?): TrackDraft {
    val identity = TrackFieldDraft(
        when (this) { TrackStarter.Reading -> "Title"; TrackStarter.Spending -> "Item"; else -> "Name" },
        TrackFieldType.ShortText, required = true, primary = true,
    )
    val detail = when (this) {
        TrackStarter.Blank -> null
        TrackStarter.Reading -> TrackFieldDraft("Pages", TrackFieldType.Number, dimension = UnitDimension.Count, unitId = "page", precision = 0, showInList = true)
        TrackStarter.Spending -> TrackFieldDraft("Amount", TrackFieldType.Number, required = true, dimension = UnitDimension.Money, unitId = "currency", precision = 2, showInList = true)
        TrackStarter.Reflection -> TrackFieldDraft("Mood", TrackFieldType.Scale, scaleMin = 1, scaleMax = 5, scaleLowLabel = "Low", scaleHighLabel = "High", showInList = true)
    }
    return TrackDraft(
        name = if (this == TrackStarter.Blank) "" else label,
        icon = when (this) { TrackStarter.Blank -> DEFAULT_TRACK_EMOJI; TrackStarter.Reading -> "📚"; TrackStarter.Spending -> "💰"; TrackStarter.Reflection -> "📝" },
        areaId = areaId,
        fields = listOf(identity) + listOfNotNull(detail) +
            if (this == TrackStarter.Blank) emptyList() else listOf(TrackFieldDraft("Notes", TrackFieldType.LongText)),
    )
}

/** Temporary identities belong only to the preview; neither draft nor repository is changed. */
internal fun TrackDraft.entryPreviewProjection(): TrackProjection {
    val valid = copy(name = name.ifBlank { "Entry Preview" }).validated()
    val fields = valid.fields.mapIndexed { index, field ->
        TrackField(
            id = -(index + 1L), uuid = "preview-field-$index", trackId = -1,
            name = field.name, type = field.type, position = index, required = field.required,
            primary = field.primary, showInList = field.showInList, dimension = field.dimension,
            unitId = field.unitId, precision = field.precision, scaleMin = field.scaleMin,
            scaleMax = field.scaleMax, scaleLowLabel = field.scaleLowLabel, scaleHighLabel = field.scaleHighLabel,
            createdAtMillis = 0, updatedAtMillis = 0, scaleStep = field.scaleStep,
        )
    }
    val options = valid.fields.flatMapIndexed { index, field ->
        field.options.mapIndexed { optionIndex, option ->
            TrackChoiceOption(-(optionIndex + 1L), "preview-option-$index-$optionIndex", fields[index].id,
                option.label, optionIndex, 0, 0)
        }
    }
    return TrackProjection(
        Track(-1, "preview", valid.name, valid.description, valid.icon, valid.areaId.orEmpty(), valid.area,
            valid.tags, false, false, 0, 0, 0),
        fields, options, emptyList(),
    )
}

@Composable
internal fun TrackEntryPreviewDialog(draft: TrackDraft, units: List<UnitDefinition>, onDismiss: () -> Unit) {
    val projection = remember(draft) { draft.entryPreviewProjection() }
    // Preview input is intentionally local and disposable: it never enters the editor checkpoint.
    var values by remember(projection) { mutableStateOf<Map<String, TrackValueDraft>>(emptyMap()) }
    var rawNumbers by remember(projection) { mutableStateOf<Map<String, String>>(emptyMap()) }
    var result by remember(projection) { mutableStateOf<String?>(null) }
    var validResult by remember(projection) { mutableStateOf(false) }
    val listState = rememberLazyListState()
    var previewViewport by remember { mutableStateOf(IntSize.Zero) }
    LaunchedEffect(result) { if (result != null) listState.animateScrollToItem(1) }
    val today = LocalWhipToday.current
    PaneAwareAlertDialog(
        testTag = "track-entry-preview",
        onDismissRequest = onDismiss,
        title = { Text("Entry Preview") },
        text = {
            LazyColumn(state = listState, verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.testTag("track-preview-fields").onSizeChanged { previewViewport = it }) {
                item {
                    Text("Try the form before saving your Track. Sample values are discarded when you close this preview.",
                        style = MaterialTheme.typography.bodyMedium)
                    Text("Entry Date · $today", style = MaterialTheme.typography.bodySmall)
                }
                result?.let { message -> item {
                    Text(message, color = if (validResult) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        modifier = Modifier.testTag("track-preview-validation"))
                } }
                items(projection.fields, key = { it.uuid }) { field ->
                    Column {
                        if (field.primary) Text("Entry identity", style = MaterialTheme.typography.labelSmall)
                        TrackEntryField(
                            field, values[field.uuid] ?: TrackValueDraft(enteredUnitId = field.unitId),
                            projection.options.filter { it.fieldId == field.id }, units, today,
                            showError = false,
                            numberText = rawNumbers[field.uuid].orEmpty(),
                            onNumberText = { raw ->
                                rawNumbers = rawNumbers + (field.uuid to raw)
                                values = values + (field.uuid to TrackValueDraft(enteredNumber = raw.toWhipDoubleOrNull(), enteredUnitId = values[field.uuid]?.enteredUnitId ?: field.unitId))
                                result = null
                            },
                            onValue = { value -> values = values + (field.uuid to value); result = null },
                            viewportSize = previewViewport,
                        )
                    }
                }
            }
        },
        confirmButton = { WhipTextButton(onClick = onDismiss) { Text("Close Preview") } },
        dismissButton = { WhipTextButton(onClick = {
            val validation = runCatching {
                projection.fields.filter { it.type == TrackFieldType.Number }.forEach { field ->
                    val raw = rawNumbers[field.uuid].orEmpty()
                    require(raw.isBlank() || raw.toWhipDoubleOrNull() != null) { "Enter a valid ${field.name}." }
                }
                validateTrackEntryDraft(projection.fields, projection.options, TrackEntryDraft(today, values))
            }
            validResult = validation.isSuccess
            result = validation.exceptionOrNull()?.message ?: "Sample entry is valid. Nothing has been saved."
        }, modifier = Modifier.testTag("track-preview-check")) { Text("Check Sample") } },
    )
}

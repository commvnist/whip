package com.whip.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.whip.app.R
import com.whip.app.domain.TrackEntryDraft
import com.whip.app.domain.TrackEntryFormSnapshot
import com.whip.app.domain.TrackField
import com.whip.app.domain.TrackFieldType
import com.whip.app.domain.TrackValueDraft
import com.whip.app.domain.formatTrackScaleValue
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Only the selected draft is rendered; interpretation comes from the frozen CSV form. */
internal fun LazyListScope.trackCsvEntryPreview(
    form: TrackEntryFormSnapshot,
    draft: TrackEntryDraft,
    index: Int,
    count: Int,
    enabled: Boolean,
    onSelect: (Int) -> Unit,
) {
    item(key = "csv-entry-preview-heading") {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.track_csv_entry_preview), fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { heading() })
            Text(stringResource(R.string.track_csv_entry_position, index + 1, count),
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (count > 1) FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                WhipTextButton(enabled = enabled && index > 0, onClick = { onSelect(index - 1) }) {
                    Text(stringResource(R.string.track_csv_previous_entry))
                }
                WhipTextButton(enabled = enabled && index + 1 < count, onClick = { onSelect(index + 1) }) {
                    Text(stringResource(R.string.track_csv_next_entry))
                }
            }
        }
    }
    item(key = "csv-entry-preview-date") {
        TrackCsvPreviewField(
            label = stringResource(R.string.track_csv_entry_date),
            value = draft.entryDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)),
            tag = "track-csv-preview-date",
        )
    }
    items(form.fields, key = { "csv-entry-preview-field-${it.uuid}" }) { field ->
        TrackCsvPreviewField(
            label = field.name,
            value = trackCsvPreviewValue(form, field, draft.values[field.uuid]),
            tag = "track-csv-preview-value-${field.uuid}",
        )
    }
}

@Composable
private fun TrackCsvPreviewField(label: String, value: String, tag: String) {
    // A CSV can contain megabytes of valid text. Preview excerpts are explicit and never alter drafts.
    val shortened = value.length > 2_000 || value.codePointCount(0, value.length) > 1_000
    val shown = if (shortened) value.substring(0, value.offsetByCodePoints(0, 1_000)) + "…" else value
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(shown.ifBlank { stringResource(R.string.track_csv_value_not_provided) }, modifier = Modifier.testTag(tag))
        if (shortened) Text(stringResource(R.string.track_csv_value_shortened), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun trackCsvPreviewValue(form: TrackEntryFormSnapshot, field: TrackField, value: TrackValueDraft?): String {
    value ?: return ""
    return when (field.type) {
        TrackFieldType.ShortText, TrackFieldType.LongText -> value.textValue.orEmpty()
        TrackFieldType.Number -> value.enteredNumber?.let { number ->
            val symbol = form.units.firstOrNull { it.id == value.enteredUnitId }?.symbol ?: value.enteredUnitId.orEmpty()
            number.formatForField(field.precision) + symbol.takeIf(String::isNotBlank)?.let { " $it" }.orEmpty()
        }.orEmpty()
        TrackFieldType.SingleChoice -> form.options.firstOrNull { it.uuid == value.choiceOptionUuid }?.label.orEmpty()
        TrackFieldType.Scale -> value.scaleValue?.let(::formatTrackScaleValue).orEmpty()
        TrackFieldType.Date -> value.dateValue?.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)).orEmpty()
        TrackFieldType.YesNo -> value.booleanValue?.let {
            stringResource(if (it) R.string.track_csv_value_yes else R.string.track_csv_value_no)
        }.orEmpty()
    }
}

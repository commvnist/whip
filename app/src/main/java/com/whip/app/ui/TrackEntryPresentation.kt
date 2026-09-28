package com.whip.app.ui

import com.whip.app.domain.BuiltInUnits
import com.whip.app.domain.TrackEntryProjection
import com.whip.app.domain.TrackFieldType
import com.whip.app.domain.TrackProjection
import com.whip.app.domain.UnitDefinition
import com.whip.app.domain.formatTrackScaleValue
import com.whip.app.domain.plainNumericValue
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Presentation only: duplicate detection and persisted identities keep their original contract. */
internal fun TrackProjection.entryDisplayTitle(
    entry: TrackEntryProjection,
    units: List<UnitDefinition> = BuiltInUnits.all,
): String = primaryFields.mapNotNull { field ->
    val value = entry.value(field.id) ?: return@mapNotNull null
    when (field.type) {
        TrackFieldType.ShortText, TrackFieldType.LongText -> value.textValue.orEmpty()
        TrackFieldType.Number -> value.enteredNumber?.let { number ->
            val unit = units.firstOrNull { it.id == value.enteredUnitId }
            val label = unit?.symbol?.ifBlank { unit.name } ?: value.enteredUnitId.orEmpty()
            listOf(plainNumericValue(number), label).filter(String::isNotBlank).joinToString(" ")
        }.orEmpty()
        TrackFieldType.SingleChoice -> options.firstOrNull { it.id == value.choiceOptionId }?.label.orEmpty()
        TrackFieldType.Scale -> value.scaleValue?.let(::formatTrackScaleValue).orEmpty()
        TrackFieldType.Date -> value.dateValue?.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)).orEmpty()
        TrackFieldType.YesNo -> value.booleanValue?.let { if (it) "Yes" else "No" }.orEmpty()
    }.takeIf(String::isNotBlank)
}.joinToString(" · ").ifBlank { "Untitled Entry" }

package com.whip.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.text.BreakIterator
import java.time.DayOfWeek
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale

internal enum class WhipWeekdayLabelWidth {
    Full,
    Short,
    Compact,
}

/** Locale-bound weekday copy. DayOfWeek remains the persistence identity. */
internal class WhipWeekdayFormatter(
    val locale: Locale,
) {
    private val fullLabels = DayOfWeek.entries.associateWith { day ->
        day.getDisplayName(JavaTextStyle.FULL, locale)
    }
    private val shortLabels = DayOfWeek.entries.associateWith { day ->
        day.getDisplayName(JavaTextStyle.SHORT, locale)
    }
    private val compactLabels = collisionSafeCompactLabels(shortLabels, fullLabels, locale)

    fun label(day: DayOfWeek, width: WhipWeekdayLabelWidth): String = when (width) {
        WhipWeekdayLabelWidth.Full -> fullLabels.getValue(day)
        WhipWeekdayLabelWidth.Short -> shortLabels.getValue(day)
        WhipWeekdayLabelWidth.Compact -> compactLabels.getValue(day)
    }
}

@Composable
internal fun rememberWhipWeekdayFormatter(): WhipWeekdayFormatter {
    val locale = LocalConfiguration.current.locales[0]
    val localeTag = locale.toLanguageTag()
    return remember(localeTag) { WhipWeekdayFormatter(locale) }
}

internal fun orderedWhipWeekdays(first: DayOfWeek): List<DayOfWeek> =
    (0..6).map { offset -> DayOfWeek.of((first.value - 1 + offset) % 7 + 1) }

@Composable
internal fun WhipCalendarMonthHeader(
    month: YearMonth,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    modifier: Modifier = Modifier,
    monthModifier: Modifier = Modifier,
    onMonthClick: (() -> Unit)? = null,
    monthActionLabel: String? = null,
    contextualAction: (@Composable () -> Unit)? = null,
) {
    val locale = LocalConfiguration.current.locales[0]
    val localeTag = locale.toLanguageTag()
    val monthLabel = remember(month, localeTag) {
        month.atDay(1).format(DateTimeFormatter.ofPattern("MMMM yyyy", locale))
    }
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(WhipSpacing.micro),
    ) {
        IconButton(onClick = onPreviousMonth, modifier = Modifier.size(48.dp)) {
            Icon(
                Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Previous Month",
            )
        }
        if (onMonthClick != null) {
            WhipTextButton(
                onClick = onMonthClick,
                modifier = Modifier
                    .weight(1f)
                    .then(monthModifier)
                    .semantics {
                        contentDescription = listOfNotNull(monthLabel, monthActionLabel)
                            .joinToString(". ")
                    },
            ) {
                CalendarMonthTitle(monthLabel, monthActionLabel)
            }
        } else {
            CalendarMonthTitle(
                monthLabel = monthLabel,
                supportingText = monthActionLabel,
                modifier = Modifier.weight(1f).then(monthModifier),
            )
        }
        contextualAction?.invoke()
        IconButton(onClick = onNextMonth, modifier = Modifier.size(48.dp)) {
            Icon(
                Icons.AutoMirrored.Outlined.ArrowForward,
                contentDescription = "Next Month",
            )
        }
    }
}

@Composable
private fun CalendarMonthTitle(
    monthLabel: String,
    supportingText: String?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(WhipSpacing.micro),
    ) {
        Text(
            monthLabel,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        supportingText?.let {
            Text(
                it,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
            )
        }
    }
}

@Composable
internal fun WhipCalendarWeekdayHeader(
    firstDayOfWeek: DayOfWeek,
    modifier: Modifier = Modifier,
    width: WhipWeekdayLabelWidth = WhipWeekdayLabelWidth.Compact,
    textStyle: TextStyle = MaterialTheme.typography.labelSmall,
) {
    val formatter = rememberWhipWeekdayFormatter()
    Row(modifier.fillMaxWidth()) {
        orderedWhipWeekdays(firstDayOfWeek).forEach { day ->
            Text(
                formatter.label(day, width),
                modifier = Modifier.weight(1f),
                style = textStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun collisionSafeCompactLabels(
    shortLabels: Map<DayOfWeek, String>,
    fullLabels: Map<DayOfWeek, String>,
    locale: Locale,
): Map<DayOfWeek, String> {
    val normalizedShortCounts = shortLabels.values.groupingBy { it.normalized(locale) }.eachCount()
    val bases = DayOfWeek.entries.associateWith { day ->
        shortLabels.getValue(day).takeUnless {
            normalizedShortCounts[it.normalized(locale)] != 1
        } ?: fullLabels.getValue(day)
    }
    val compact = DayOfWeek.entries.associateWith { day ->
        val base = bases.getValue(day)
        base.compactPrefixes(locale).firstOrNull { candidate ->
            bases.none { (otherDay, otherLabel) ->
                otherDay != day && otherLabel.normalized(locale)
                    .startsWith(candidate.normalized(locale))
            }
        } ?: base
    }.toMutableMap()

    compact.entries.groupBy { it.value.normalized(locale) }.values
        .filter { entries -> entries.size > 1 }
        .flatten()
        .forEach { (day, label) -> compact[day] = "$label ${day.value}" }
    return compact
}

private fun String.compactPrefixes(locale: Locale): List<String> {
    if (isEmpty()) return listOf(this)
    val iterator = BreakIterator.getCharacterInstance(locale)
    iterator.setText(this)
    val boundaries = buildList {
        var boundary = iterator.first()
        while (boundary != BreakIterator.DONE) {
            if (boundary > 0) add(boundary)
            boundary = iterator.next()
        }
    }
    val minimumIndex = if (boundaries.size > 1) 1 else 0
    return boundaries.drop(minimumIndex).map { end -> substring(0, end) }
}

private fun String.normalized(locale: Locale): String = trim().lowercase(locale)

package com.whip.app.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.time.temporal.TemporalAdjusters

data class ParsedTaskCapture(
    val title: String,
    val scheduleKind: ScheduleKind,
    val date: LocalDate?,
    val recurrence: RecurrenceRule?,
    val deadline: LocalDate?,
    val recognized: List<String>,
)

object TaskQuickCaptureParser {
    private val intervalPattern = Regex(
        "\\bevery\\s+(?:(\\d+)\\s+)?(day|days|week|weeks|month|months|year|years)\\b",
        RegexOption.IGNORE_CASE,
    )
    private val weekdayPattern = Regex(
        "\\bevery\\s+((?:mon(?:day)?|tue(?:sday)?|wed(?:nesday)?|thu(?:rsday)?|fri(?:day)?|sat(?:urday)?|sun(?:day)?)(?:\\s*(?:,|and)\\s*(?:mon(?:day)?|tue(?:sday)?|wed(?:nesday)?|thu(?:rsday)?|fri(?:day)?|sat(?:urday)?|sun(?:day)?))*)\\b",
        RegexOption.IGNORE_CASE,
    )
    private val isoDatePattern = Regex("\\b(?:on|work)\\s+(\\d{4}-\\d{2}-\\d{2})\\b", RegexOption.IGNORE_CASE)
    private val deadlinePattern = Regex("\\b(?:deadline|due)\\s+(\\d{4}-\\d{2}-\\d{2})\\b", RegexOption.IGNORE_CASE)
    private val nextWeekdayPattern = Regex(
        "\\bnext\\s+(mon(?:day)?|tue(?:sday)?|wed(?:nesday)?|thu(?:rsday)?|fri(?:day)?|sat(?:urday)?|sun(?:day)?)\\b",
        RegexOption.IGNORE_CASE,
    )

    fun parse(input: String, today: LocalDate): ParsedTaskCapture {
        var remaining = input.trim()
        var date: LocalDate? = null
        var deadline: LocalDate? = null
        var recurrence: RecurrenceRule? = null
        val recognized = mutableListOf<String>()

        deadlinePattern.find(remaining)?.let { match ->
            parseDate(match.groupValues[1])?.let { parsed ->
                deadline = parsed
                recognized += "deadline $parsed"
                remaining = remaining.removeMatch(match)
            }
        }
        isoDatePattern.find(remaining)?.let { match ->
            parseDate(match.groupValues[1])?.let { parsed ->
                date = parsed
                recognized += "work date $parsed"
                remaining = remaining.removeMatch(match)
            }
        }
        Regex("\\btomorrow\\b", RegexOption.IGNORE_CASE).find(remaining)?.let { match ->
            date = today.plusDays(1)
            recognized += "tomorrow"
            remaining = remaining.removeMatch(match)
        }
        Regex("\\btoday\\b", RegexOption.IGNORE_CASE).find(remaining)?.let { match ->
            date = today
            recognized += "today"
            remaining = remaining.removeMatch(match)
        }
        nextWeekdayPattern.find(remaining)?.let { match ->
            val day = parseWeekday(match.groupValues[1])
            date = today.with(TemporalAdjusters.next(day))
            recognized += "next ${day.name.lowercase()}"
            remaining = remaining.removeMatch(match)
        }

        weekdayPattern.find(remaining)?.let { match ->
            val days = Regex("mon(?:day)?|tue(?:sday)?|wed(?:nesday)?|thu(?:rsday)?|fri(?:day)?|sat(?:urday)?|sun(?:day)?", RegexOption.IGNORE_CASE)
                .findAll(match.groupValues[1])
                .map { parseWeekday(it.value) }
                .toCollection(linkedSetOf())
            val start = date ?: today
            recurrence = RecurrenceRule(
                unit = RecurrenceUnit.Weeks,
                weekdays = days,
                startDate = start,
            )
            recognized += "every ${days.joinToString { it.name.lowercase() }}"
            remaining = remaining.removeMatch(match)
        }
        if (recurrence == null) intervalPattern.find(remaining)?.let { match ->
            val interval = match.groupValues[1].toIntOrNull() ?: 1
            val unit = when (match.groupValues[2].lowercase().removeSuffix("s")) {
                "day" -> RecurrenceUnit.Days
                "week" -> RecurrenceUnit.Weeks
                "month" -> RecurrenceUnit.Months
                else -> RecurrenceUnit.Years
            }
            val start = date ?: today
            recurrence = RecurrenceRule(unit = unit, interval = interval, startDate = start)
            recognized += "every $interval ${unit.name.lowercase()}"
            remaining = remaining.removeMatch(match)
        }

        val normalizedTitle = remaining.replace(Regex("\\s{2,}"), " ")
            .trim(' ', ',', '-', '·')
            .ifBlank { input.trim() }
        val effectiveDate = date ?: deadline
        val kind = if (recurrence != null) ScheduleKind.Recurring
        else if (effectiveDate != null) ScheduleKind.Once else ScheduleKind.Anytime
        return ParsedTaskCapture(
            title = normalizedTitle,
            scheduleKind = kind,
            date = effectiveDate ?: recurrence?.startDate,
            recurrence = recurrence,
            deadline = deadline,
            recognized = recognized,
        )
    }

    private fun String.removeMatch(match: MatchResult): String = removeRange(match.range)

    private fun parseDate(value: String): LocalDate? = try {
        LocalDate.parse(value)
    } catch (_: DateTimeParseException) {
        null
    }

    private fun parseWeekday(value: String): DayOfWeek = when (value.take(3).lowercase()) {
        "mon" -> DayOfWeek.MONDAY
        "tue" -> DayOfWeek.TUESDAY
        "wed" -> DayOfWeek.WEDNESDAY
        "thu" -> DayOfWeek.THURSDAY
        "fri" -> DayOfWeek.FRIDAY
        "sat" -> DayOfWeek.SATURDAY
        else -> DayOfWeek.SUNDAY
    }
}

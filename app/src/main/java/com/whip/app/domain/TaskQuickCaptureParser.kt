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
    val assumptions: List<TaskCaptureAssumption>,
)

enum class TaskCaptureAssumptionKind {
    Schedule,
    Repeat,
    Deadline,
}

data class TaskCaptureAssumption(
    val kind: TaskCaptureAssumptionKind,
    val sourceText: String,
    val start: Int,
    val endExclusive: Int,
    val interpretation: String,
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
    private val tomorrowPattern = Regex("\\btomorrow\\b(?!['’]s)", RegexOption.IGNORE_CASE)
    private val todayPattern = Regex("\\btoday\\b(?!['’]s)", RegexOption.IGNORE_CASE)

    fun parse(input: String, today: LocalDate): ParsedTaskCapture {
        val deadlineCandidate = deadlinePattern.find(input)?.let { match ->
            parseDate(match.groupValues[1])?.let { date -> DateCandidate(match, date, "Deadline · $date") }
        }
        val dateCandidate = listOfNotNull(
            isoDatePattern.find(input)?.let { match ->
                parseDate(match.groupValues[1])?.let { date -> DateCandidate(match, date, "Schedule · $date") }
            },
            tomorrowPattern.find(input)?.let { match ->
                DateCandidate(match, today.plusDays(1), "Schedule · Tomorrow → ${today.plusDays(1)}")
            },
            todayPattern.find(input)?.let { match ->
                DateCandidate(match, today, "Schedule · Today → $today")
            },
            nextWeekdayPattern.find(input)?.let { match ->
                val day = parseWeekday(match.groupValues[1])
                val date = today.with(TemporalAdjusters.next(day))
                DateCandidate(
                    match,
                    date,
                    "Schedule · Next ${day.name.lowercase().replaceFirstChar(Char::uppercase)} → $date",
                )
            },
        ).minByOrNull { it.match.range.first }

        val weekdayCandidate = weekdayPattern.find(input)?.let { match ->
            val days = Regex("mon(?:day)?|tue(?:sday)?|wed(?:nesday)?|thu(?:rsday)?|fri(?:day)?|sat(?:urday)?|sun(?:day)?", RegexOption.IGNORE_CASE)
                .findAll(match.groupValues[1])
                .map { parseWeekday(it.value) }
                .toCollection(linkedSetOf())
            RecurrenceCandidate(
                match = match,
                rule = RecurrenceRule(
                    unit = RecurrenceUnit.Weeks,
                    weekdays = days,
                    startDate = dateCandidate?.date ?: today,
                ),
                interpretation = "Repeat · ${days.joinToString { day -> day.name.lowercase().replaceFirstChar(Char::uppercase) }}",
            )
        }
        val intervalCandidate = intervalPattern.find(input)?.let { match ->
            val interval = (match.groupValues[1].toIntOrNull() ?: 1).takeIf { it > 0 }
                ?: return@let null
            val unit = when (match.groupValues[2].lowercase().removeSuffix("s")) {
                "day" -> RecurrenceUnit.Days
                "week" -> RecurrenceUnit.Weeks
                "month" -> RecurrenceUnit.Months
                else -> RecurrenceUnit.Years
            }
            RecurrenceCandidate(
                match = match,
                rule = RecurrenceRule(
                    unit = unit,
                    interval = interval,
                    startDate = dateCandidate?.date ?: today,
                ),
                interpretation = "Repeat · every " + if (interval == 1) {
                    unit.name.lowercase().removeSuffix("s")
                } else {
                    "$interval ${unit.name.lowercase()}"
                },
            )
        }
        val recurrenceCandidate = listOfNotNull(weekdayCandidate, intervalCandidate)
            .minByOrNull { it.match.range.first }
        val scheduleStart = dateCandidate?.date ?: recurrenceCandidate?.rule?.startDate
        val applicableDeadlineCandidate = deadlineCandidate?.takeIf { candidate ->
            scheduleStart == null || !candidate.date.isBefore(scheduleStart)
        }

        val assumptions = listOfNotNull(
            dateCandidate?.toAssumption(TaskCaptureAssumptionKind.Schedule),
            recurrenceCandidate?.toAssumption(),
            applicableDeadlineCandidate?.toAssumption(TaskCaptureAssumptionKind.Deadline),
        ).sortedBy(TaskCaptureAssumption::start)
        val remaining = input.toCharArray().also { chars ->
            assumptions.forEach { assumption ->
                (assumption.start until assumption.endExclusive).forEach { index -> chars[index] = ' ' }
            }
        }.concatToString()

        val normalizedTitle = remaining.replace(Regex("\\s{2,}"), " ")
            .trim(' ', ',', '-', '—', '–', '·')
            .ifBlank { input.trim() }
        val recurrence = recurrenceCandidate?.rule
        val deadline = applicableDeadlineCandidate?.date
        val effectiveDate = dateCandidate?.date ?: deadline
        val kind = if (recurrence != null) ScheduleKind.Recurring
        else if (effectiveDate != null) ScheduleKind.Once else ScheduleKind.Anytime
        return ParsedTaskCapture(
            title = normalizedTitle,
            scheduleKind = kind,
            date = effectiveDate ?: recurrence?.startDate,
            recurrence = recurrence,
            deadline = deadline,
            recognized = assumptions.map(TaskCaptureAssumption::interpretation),
            assumptions = assumptions,
        )
    }

    private data class DateCandidate(
        val match: MatchResult,
        val date: LocalDate,
        val interpretation: String,
    ) {
        fun toAssumption(kind: TaskCaptureAssumptionKind) = TaskCaptureAssumption(
            kind = kind,
            sourceText = match.value,
            start = match.range.first,
            endExclusive = match.range.last + 1,
            interpretation = interpretation,
        )
    }

    private data class RecurrenceCandidate(
        val match: MatchResult,
        val rule: RecurrenceRule,
        val interpretation: String,
    ) {
        fun toAssumption() = TaskCaptureAssumption(
            kind = TaskCaptureAssumptionKind.Repeat,
            sourceText = match.value,
            start = match.range.first,
            endExclusive = match.range.last + 1,
            interpretation = interpretation,
        )
    }

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

package com.whip.app.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeParseException
import java.time.temporal.TemporalAdjusters

data class ParsedTaskCapture(
    val title: String,
    val scheduleKind: ScheduleKind,
    val date: LocalDate?,
    val recurrence: RecurrenceRule?,
    val deadline: LocalDate?,
    val timeMinutes: Int?,
    val reminderEnabled: Boolean,
    val reminderOffsetsMinutes: List<Int>,
    val priority: TaskPriority?,
    val durationMinutes: Int?,
    val effort: TaskEffort?,
    val tags: Set<String>,
    val recognized: List<String>,
    val assumptions: List<TaskCaptureAssumption>,
)

enum class TaskCaptureAssumptionKind {
    Schedule,
    Time,
    Repeat,
    RepeatEnd,
    Deadline,
    Reminder,
    Priority,
    Duration,
    Effort,
    Tag,
}

data class TaskCaptureAssumption(
    val kind: TaskCaptureAssumptionKind,
    val sourceText: String,
    val start: Int,
    val endExclusive: Int,
    val interpretation: String,
)

object TaskQuickCaptureParser {
    private const val WEEKDAY_BASE =
        "(?:mon(?:day)?|tue(?:s(?:day)?)?|wed(?:nesday)?|thu(?:r(?:sday)?)?|fri(?:day)?|sat(?:urday)?|sun(?:day)?)"
    private const val WEEKDAY_TOKEN = "${WEEKDAY_BASE}s?"
    private const val PLURAL_WEEKDAY_TOKEN =
        "(?:mon(?:day)?s|tue(?:s(?:day)?)?s|wed(?:nesday)?s|thu(?:r(?:sday)?)?s|fri(?:day)?s|sat(?:urday)?s|sun(?:day)?s)"
    private const val WEEKDAY_SEPARATOR =
        "(?:\\s*,\\s*(?:(?:and|or|&)\\s+)?|\\s+(?:and|or|&)\\s+|\\s*/\\s*|\\s*\\+\\s*)"
    private const val WEEKDAY_LIST = "$WEEKDAY_TOKEN(?:$WEEKDAY_SEPARATOR$WEEKDAY_TOKEN)*"
    private const val PLURAL_WEEKDAY_LIST =
        "$PLURAL_WEEKDAY_TOKEN(?:$WEEKDAY_SEPARATOR$PLURAL_WEEKDAY_TOKEN)*"
    private const val MONTH_TOKEN =
        "(?:jan(?:uary)?|feb(?:ruary)?|mar(?:ch)?|apr(?:il)?|may|jun(?:e)?|jul(?:y)?|aug(?:ust)?|sep(?:t(?:ember)?)?|oct(?:ober)?|nov(?:ember)?|dec(?:ember)?)"
    private const val DATE_UNIT = "(?:day|week|month|year)s?"

    private val intervalWeekdayPattern = Regex(
        "\\b(?:every|each)\\s+(?:(other)\\s+|(\\d+)\\s+)?weeks?\\s+on\\s+($WEEKDAY_LIST)\\b",
        RegexOption.IGNORE_CASE,
    )
    private val weekdayPattern = Regex(
        "\\b(?:(?:every|each)\\s+($WEEKDAY_LIST)|weekly\\s+on\\s+($WEEKDAY_LIST))\\b",
        RegexOption.IGNORE_CASE,
    )
    private val pluralWeekdayPattern = Regex(
        "\\b(?:on\\s+)?($PLURAL_WEEKDAY_LIST)\\b",
        RegexOption.IGNORE_CASE,
    )
    private val namedDayGroupPattern = Regex(
        "\\b(?:every|each)\\s+(weekdays?|weekends?)\\b",
        RegexOption.IGNORE_CASE,
    )
    private val monthlyOrdinalPattern = Regex(
        "\\b(?:(?:every|each)\\s+month|monthly)\\s+on\\s+(?:the\\s+)?(\\d{1,2})(?:st|nd|rd|th)?\\b",
        RegexOption.IGNORE_CASE,
    )
    private val yearlyDatePattern = Regex(
        "\\b(?:(?:every|each)\\s+year|yearly)\\s+on\\s+($MONTH_TOKEN)\\s+(\\d{1,2})(?:st|nd|rd|th)?\\b",
        RegexOption.IGNORE_CASE,
    )
    private val intervalPattern = Regex(
        "\\b(?:every|each)\\s+(?:(other)\\s+|(\\d+)\\s+)?(days?|weeks?|months?|years?)(\\s+after\\s+completion)?\\b",
        RegexOption.IGNORE_CASE,
    )
    private val cadenceWordPattern = Regex(
        "\\b(daily|weekly|monthly|yearly)(\\s+after\\s+completion)?\\b",
        RegexOption.IGNORE_CASE,
    )
    private val weekdayTokenPattern = Regex(WEEKDAY_TOKEN, RegexOption.IGNORE_CASE)

    private val recurrenceCountPattern = Regex(
        "\\bfor\\s+(\\d+)\\s+(?:occurrences?|times?)\\b",
        RegexOption.IGNORE_CASE,
    )
    private val timePattern = Regex(
        "(?:(?<!\\w)@\\s*|\\bat\\s+)(noon|midnight|(?:[01]?\\d|2[0-3]):[0-5]\\d|(?:1[0-2]|0?[1-9])(?::[0-5]\\d)?\\s*(?:a\\.?m\\.?|p\\.?m\\.?))(?!\\w)|" +
            "\\b((?:1[0-2]|0?[1-9])(?::[0-5]\\d)?\\s*(?:a\\.?m\\.?|p\\.?m\\.?))(?!\\w)",
        RegexOption.IGNORE_CASE,
    )
    private val priorityPattern = Regex(
        "\\bpriority\\s*:?\\s*(urgent|high|medium|normal|low|p[1-4])\\b|" +
            "(?<!\\w)!(urgent|high|medium|low|p[1-4])\\b",
        RegexOption.IGNORE_CASE,
    )
    private val effortPattern = Regex(
        "\\beffort\\s*:?\\s*(light|medium|high)\\b|\\b(light|medium|high)\\s+effort\\b",
        RegexOption.IGNORE_CASE,
    )
    private val durationPattern = Regex(
        "\\b(?:for|duration\\s*:?\\s*)\\s*(" +
            "(?:\\d+\\s*(?:hours?|hrs?|h)(?:\\s*(?:and\\s+)?\\d+\\s*(?:minutes?|mins?|m))?)|" +
            "(?:\\d+\\s*(?:minutes?|mins?|m)))\\b",
        RegexOption.IGNORE_CASE,
    )
    private val tagPattern = Regex("(?<![\\p{L}\\p{N}_])#([\\p{L}\\p{N}][\\p{L}\\p{N}_-]{0,31})\\b")
    private val reminderOffsetPattern = Regex(
        "\\bremind(?:\\s+me)?\\s+(\\d+)\\s*(minutes?|mins?|m|hours?|hrs?|h)\\s+before\\b",
        RegexOption.IGNORE_CASE,
    )
    private val reminderPrefixPattern = Regex("^\\s*remind\\s+me\\s+to\\b", RegexOption.IGNORE_CASE)
    private val reminderSimplePattern = Regex(
        "\\b(?:remind\\s+me|with\\s+(?:a\\s+)?reminder)\\b",
        RegexOption.IGNORE_CASE,
    )

    fun parse(input: String, today: LocalDate): ParsedTaskCapture {
        val deadlineCandidate = findQualifiedDate(
            input = input,
            today = today,
            prefix = "(?:due|deadline|by)(?:\\s+on)?",
            interpretationPrefix = "Deadline",
        )
        val recurrenceEndDateCandidate = findQualifiedDate(
            input = input,
            today = today,
            prefix = "until",
            interpretationPrefix = "Repeat ends",
        )
        val excludedDateRanges = listOfNotNull(
            deadlineCandidate?.match?.range,
            recurrenceEndDateCandidate?.match?.range,
        )
        val rawDateCandidate = findScheduleDate(input, today)
            ?.takeUnless { candidate -> excludedDateRanges.any { candidate.match.range.overlaps(it) } }

        val baseRecurrenceCandidate = findRecurrence(input, today, rawDateCandidate?.date)
        val recurrenceEndCandidate = baseRecurrenceCandidate?.let { recurrence ->
            findRecurrenceEnd(input, recurrenceEndDateCandidate, recurrence.rule.startDate)
        }
        val recurrenceCandidate = baseRecurrenceCandidate?.let { candidate ->
            candidate.copy(rule = candidate.rule.withEnd(recurrenceEndCandidate))
        }
        val dateCandidate = rawDateCandidate?.takeUnless { candidate ->
            recurrenceCandidate?.match?.range?.let { range -> candidate.match.range.overlaps(range) } == true
        }

        val scheduleStart = dateCandidate?.date ?: recurrenceCandidate?.rule?.startDate
        val applicableDeadlineCandidate = deadlineCandidate?.takeIf { candidate ->
            scheduleStart == null || !candidate.date.isBefore(scheduleStart)
        }
        val timeCandidate = findTime(input)
        val priorityCandidate = findPriority(input)
        val durationCandidate = findDuration(input)
        val effortCandidate = findEffort(input)
        val tagCandidates = tagPattern.findAll(input).map { match ->
            ValueCandidate(
                match = match,
                value = match.groupValues[1],
                kind = TaskCaptureAssumptionKind.Tag,
                interpretation = "Tag · #${match.groupValues[1]}",
            )
        }.toList()

        val recurrence = recurrenceCandidate?.rule
        val deadline = applicableDeadlineCandidate?.date
        val scheduledDate = dateCandidate?.date ?: deadline ?: today.takeIf { timeCandidate != null }
        val kind = when {
            recurrence != null -> ScheduleKind.Recurring
            scheduledDate != null -> ScheduleKind.Once
            else -> ScheduleKind.Anytime
        }
        val reminderCandidate = timeCandidate?.let { findReminder(input) }

        val assumptions = nonOverlapping(
            listOfNotNull(
                dateCandidate?.toAssumption(TaskCaptureAssumptionKind.Schedule),
                timeCandidate?.toAssumption(),
                recurrenceCandidate?.toAssumption(),
                recurrenceEndCandidate?.toAssumption(),
                applicableDeadlineCandidate?.toAssumption(TaskCaptureAssumptionKind.Deadline),
                reminderCandidate?.toAssumption(),
                priorityCandidate?.toAssumption(),
                durationCandidate?.toAssumption(),
                effortCandidate?.toAssumption(),
            ) + tagCandidates.map(ValueCandidate<String>::toAssumption),
        )
        val remaining = input.toCharArray().also { chars ->
            assumptions.forEach { assumption ->
                (assumption.start until assumption.endExclusive).forEach { index -> chars[index] = ' ' }
            }
        }.concatToString()

        val normalizedTitle = remaining
            .replace(Regex("\\(\\s*\\)"), " ")
            .replace(Regex("\\s{2,}"), " ")
            .trim(' ', ',', ':', ';', '-', '—', '–', '·', '|')
            .ifBlank { input.trim() }
        return ParsedTaskCapture(
            title = normalizedTitle,
            scheduleKind = kind,
            date = scheduledDate ?: recurrence?.startDate,
            recurrence = recurrence,
            deadline = deadline,
            timeMinutes = timeCandidate?.value,
            reminderEnabled = reminderCandidate != null,
            reminderOffsetsMinutes = reminderCandidate?.value?.let(::listOf).orEmpty(),
            priority = priorityCandidate?.value,
            durationMinutes = durationCandidate?.value,
            effort = effortCandidate?.value,
            tags = tagCandidates.mapTo(linkedSetOf()) { it.value },
            recognized = assumptions.map(TaskCaptureAssumption::interpretation),
            assumptions = assumptions,
        )
    }

    private fun findScheduleDate(input: String, today: LocalDate): DateCandidate? {
        val isoPattern = Regex(
            "\\b(?:on|work|start(?:ing)?)\\s+(\\d{4}-\\d{2}-\\d{2})\\b",
            RegexOption.IGNORE_CASE,
        )
        val monthPattern = Regex(
            "\\b(?:on|start(?:ing)?)\\s+($MONTH_TOKEN)\\s+(\\d{1,2})(?:st|nd|rd|th)?(?:,?\\s+(\\d{4}))?\\b",
            RegexOption.IGNORE_CASE,
        )
        val relativePattern = Regex("\\bin\\s+(\\d+)\\s+($DATE_UNIT)\\b", RegexOption.IGNORE_CASE)
        val nextWeekdayPattern = Regex(
            "\\b(?:on\\s+)?next\\s+($WEEKDAY_BASE)\\b",
            RegexOption.IGNORE_CASE,
        )
        val thisWeekdayPattern = Regex(
            "\\b(?:on\\s+)?this\\s+($WEEKDAY_BASE)\\b",
            RegexOption.IGNORE_CASE,
        )
        val onWeekdayPattern = Regex("\\bon\\s+($WEEKDAY_BASE)\\b", RegexOption.IGNORE_CASE)
        val tomorrowPattern = Regex("\\btomorrow\\b(?!['’]s)", RegexOption.IGNORE_CASE)
        val todayPattern = Regex("\\btoday\\b(?!['’]s)", RegexOption.IGNORE_CASE)

        return listOfNotNull(
            isoPattern.find(input)?.let { match ->
                parseDate(match.groupValues[1])?.let { date ->
                    DateCandidate(match, date, "Schedule · $date")
                }
            },
            monthPattern.find(input)?.let { match ->
                parseMonthDate(match.groupValues[1], match.groupValues[2], match.groupValues[3], today)?.let { date ->
                    DateCandidate(match, date, "Schedule · $date")
                }
            },
            relativePattern.find(input)?.let { match ->
                relativeDate(today, match.groupValues[1], match.groupValues[2])?.let { date ->
                    DateCandidate(match, date, "Schedule · ${match.value.replaceFirstChar(Char::uppercase)} → $date")
                }
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
                DateCandidate(match, date, "Schedule · Next ${day.label()} → $date")
            },
            thisWeekdayPattern.find(input)?.let { match ->
                val day = parseWeekday(match.groupValues[1])
                val date = today.with(TemporalAdjusters.nextOrSame(day))
                DateCandidate(match, date, "Schedule · This ${day.label()} → $date")
            },
            onWeekdayPattern.find(input)?.let { match ->
                val day = parseWeekday(match.groupValues[1])
                val date = today.with(TemporalAdjusters.nextOrSame(day))
                DateCandidate(match, date, "Schedule · ${day.label()} → $date")
            },
        ).minByOrNull { it.match.range.first }
    }

    private fun findQualifiedDate(
        input: String,
        today: LocalDate,
        prefix: String,
        interpretationPrefix: String,
    ): DateCandidate? {
        val isoPattern = Regex("\\b$prefix\\s+(\\d{4}-\\d{2}-\\d{2})\\b", RegexOption.IGNORE_CASE)
        val monthPattern = Regex(
            "\\b$prefix\\s+($MONTH_TOKEN)\\s+(\\d{1,2})(?:st|nd|rd|th)?(?:,?\\s+(\\d{4}))?\\b",
            RegexOption.IGNORE_CASE,
        )
        val namedPattern = Regex("\\b$prefix\\s+(today|tomorrow)\\b", RegexOption.IGNORE_CASE)
        val weekdayPattern = Regex(
            "\\b$prefix\\s+(?:(next|this)\\s+)?($WEEKDAY_BASE)\\b",
            RegexOption.IGNORE_CASE,
        )
        val relativePattern = Regex(
            "\\b$prefix\\s+in\\s+(\\d+)\\s+($DATE_UNIT)\\b",
            RegexOption.IGNORE_CASE,
        )
        return listOfNotNull(
            isoPattern.find(input)?.let { match ->
                parseDate(match.groupValues[1])?.let { date ->
                    DateCandidate(match, date, "$interpretationPrefix · $date")
                }
            },
            monthPattern.find(input)?.let { match ->
                parseMonthDate(match.groupValues[1], match.groupValues[2], match.groupValues[3], today)?.let { date ->
                    DateCandidate(match, date, "$interpretationPrefix · $date")
                }
            },
            namedPattern.find(input)?.let { match ->
                val date = if (match.groupValues[1].equals("tomorrow", true)) today.plusDays(1) else today
                DateCandidate(match, date, "$interpretationPrefix · $date")
            },
            weekdayPattern.find(input)?.let { match ->
                val day = parseWeekday(match.groupValues[2])
                val date = if (match.groupValues[1].equals("next", true)) {
                    today.with(TemporalAdjusters.next(day))
                } else {
                    today.with(TemporalAdjusters.nextOrSame(day))
                }
                DateCandidate(match, date, "$interpretationPrefix · $date")
            },
            relativePattern.find(input)?.let { match ->
                relativeDate(today, match.groupValues[1], match.groupValues[2])?.let { date ->
                    DateCandidate(match, date, "$interpretationPrefix · $date")
                }
            },
        ).minByOrNull { it.match.range.first }
    }

    private fun findRecurrence(input: String, today: LocalDate, explicitStart: LocalDate?): RecurrenceCandidate? {
        val startDate = explicitStart ?: today
        val candidates = listOfNotNull(
            monthlyOrdinalPattern.find(input)?.let { match ->
                val day = match.groupValues[1].toIntOrNull() ?: return@let null
                val start = nextDayOfMonth(today, day) ?: return@let null
                RecurrenceCandidate(
                    match,
                    RecurrenceRule(RecurrenceUnit.Months, startDate = start),
                    "Repeat · monthly on day $day",
                )
            },
            yearlyDatePattern.find(input)?.let { match ->
                val start = parseMonthDate(match.groupValues[1], match.groupValues[2], "", today)
                    ?: return@let null
                RecurrenceCandidate(
                    match,
                    RecurrenceRule(RecurrenceUnit.Years, startDate = start),
                    "Repeat · yearly on ${match.groupValues[1].replaceFirstChar(Char::uppercase)} ${match.groupValues[2]}",
                )
            },
            intervalWeekdayPattern.find(input)?.let { match ->
                val interval = intervalValue(match.groupValues[1], match.groupValues[2]) ?: return@let null
                val days = parseWeekdays(match.groupValues[3])
                RecurrenceCandidate(
                    match,
                    RecurrenceRule(RecurrenceUnit.Weeks, interval, days, startDate),
                    weekdayInterpretation(days, interval),
                )
            },
            weekdayPattern.find(input)?.let { match ->
                val days = parseWeekdays(match.groupValues.drop(1).first(String::isNotBlank))
                RecurrenceCandidate(
                    match,
                    RecurrenceRule(RecurrenceUnit.Weeks, weekdays = days, startDate = startDate),
                    weekdayInterpretation(days),
                )
            },
            namedDayGroupPattern.find(input)?.let { match ->
                val weekend = match.groupValues[1].startsWith("weekend", true)
                val days = if (weekend) {
                    linkedSetOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
                } else {
                    linkedSetOf(
                        DayOfWeek.MONDAY,
                        DayOfWeek.TUESDAY,
                        DayOfWeek.WEDNESDAY,
                        DayOfWeek.THURSDAY,
                        DayOfWeek.FRIDAY,
                    )
                }
                RecurrenceCandidate(
                    match,
                    RecurrenceRule(RecurrenceUnit.Weeks, weekdays = days, startDate = startDate),
                    "Repeat · ${if (weekend) "Weekends" else "Weekdays"}",
                )
            },
            pluralWeekdayPattern.find(input)?.let { match ->
                val days = parseWeekdays(match.groupValues[1])
                RecurrenceCandidate(
                    match,
                    RecurrenceRule(RecurrenceUnit.Weeks, weekdays = days, startDate = startDate),
                    weekdayInterpretation(days),
                )
            },
            intervalPattern.find(input)?.let { match ->
                val interval = intervalValue(match.groupValues[1], match.groupValues[2]) ?: return@let null
                val unit = recurrenceUnit(match.groupValues[3])
                val anchor = if (match.groupValues[4].isNotBlank()) {
                    RecurrenceAnchor.Completion
                } else {
                    RecurrenceAnchor.Schedule
                }
                val cadence = if (interval == 1) unit.name.lowercase().removeSuffix("s")
                else "$interval ${unit.name.lowercase()}"
                RecurrenceCandidate(
                    match,
                    RecurrenceRule(unit, interval, startDate = startDate, anchor = anchor),
                    "Repeat · every $cadence${if (anchor == RecurrenceAnchor.Completion) " after completion" else ""}",
                )
            },
            cadenceWordPattern.find(input)
                ?.takeIf { match -> input.substring(0, match.range.first).trim().isNotEmpty() }
                ?.let { match ->
                val unit = when (match.groupValues[1].lowercase()) {
                    "daily" -> RecurrenceUnit.Days
                    "weekly" -> RecurrenceUnit.Weeks
                    "monthly" -> RecurrenceUnit.Months
                    else -> RecurrenceUnit.Years
                }
                val anchor = if (match.groupValues[2].isNotBlank()) {
                    RecurrenceAnchor.Completion
                } else {
                    RecurrenceAnchor.Schedule
                }
                RecurrenceCandidate(
                    match,
                    RecurrenceRule(unit, startDate = startDate, anchor = anchor),
                    "Repeat · ${match.groupValues[1].lowercase()}${if (anchor == RecurrenceAnchor.Completion) " after completion" else ""}",
                )
            },
        )
        return candidates.minByOrNull { it.match.range.first }
    }

    private fun findRecurrenceEnd(
        input: String,
        dateCandidate: DateCandidate?,
        startDate: LocalDate,
    ): RecurrenceEndCandidate? {
        val onDate = dateCandidate?.takeIf { !it.date.isBefore(startDate) }?.let { candidate ->
            RecurrenceEndCandidate(
                match = candidate.match,
                end = RecurrenceEnd.OnDate,
                endDate = candidate.date,
                occurrenceCount = null,
                interpretation = candidate.interpretation,
            )
        }
        val afterCount = recurrenceCountPattern.find(input)?.let { match ->
            val count = match.groupValues[1].toIntOrNull()?.takeIf { it > 0 } ?: return@let null
            RecurrenceEndCandidate(
                match = match,
                end = RecurrenceEnd.AfterCount,
                endDate = null,
                occurrenceCount = count,
                interpretation = "Repeat ends · after $count occurrences",
            )
        }
        return listOfNotNull(onDate, afterCount).minByOrNull { it.match.range.first }
    }

    private fun findTime(input: String): ValueCandidate<Int>? = timePattern.find(input)?.let { match ->
        val source = match.groupValues.drop(1).first(String::isNotBlank)
        val minutes = parseTimeMinutes(source) ?: return@let null
        ValueCandidate(
            match,
            minutes,
            TaskCaptureAssumptionKind.Time,
            "Time · ${formatTime(minutes)}",
        )
    }

    private fun findPriority(input: String): ValueCandidate<TaskPriority>? = priorityPattern.find(input)?.let { match ->
        val source = match.groupValues.drop(1).first(String::isNotBlank).lowercase()
        val priority = when (source) {
            "urgent", "p1" -> TaskPriority.Urgent
            "high", "p2" -> TaskPriority.High
            "medium", "normal", "p3" -> TaskPriority.Medium
            else -> TaskPriority.Low
        }
        ValueCandidate(
            match,
            priority,
            TaskCaptureAssumptionKind.Priority,
            "Priority · ${priority.name}",
        )
    }

    private fun findEffort(input: String): ValueCandidate<TaskEffort>? = effortPattern.find(input)?.let { match ->
        val source = match.groupValues.drop(1).first(String::isNotBlank).lowercase()
        val effort = when (source) {
            "light" -> TaskEffort.Light
            "medium" -> TaskEffort.Medium
            else -> TaskEffort.High
        }
        ValueCandidate(match, effort, TaskCaptureAssumptionKind.Effort, "Effort · ${effort.label}")
    }

    private fun findDuration(input: String): ValueCandidate<Int>? = durationPattern.find(input)?.let { match ->
        val minutes = parseDurationMinutes(match.value)?.takeIf { it in 1..1_440 } ?: return@let null
        ValueCandidate(
            match,
            minutes,
            TaskCaptureAssumptionKind.Duration,
            "Duration · ${formatDuration(minutes)}",
        )
    }

    private fun findReminder(input: String): ValueCandidate<Int>? {
        reminderOffsetPattern.find(input)?.let { match ->
            val amount = match.groupValues[1].toIntOrNull() ?: return@let
            val multiplier = if (match.groupValues[2].lowercase().startsWith("h")) 60 else 1
            val offset = amount * multiplier
            if (offset in 1..10_080) {
                return ValueCandidate(
                    match,
                    offset,
                    TaskCaptureAssumptionKind.Reminder,
                    "Reminder · ${formatDuration(offset)} before",
                )
            }
        }
        val match = reminderPrefixPattern.find(input) ?: reminderSimplePattern.find(input) ?: return null
        return ValueCandidate(
            match,
            0,
            TaskCaptureAssumptionKind.Reminder,
            "Reminder · At scheduled time",
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

    private data class RecurrenceEndCandidate(
        val match: MatchResult,
        val end: RecurrenceEnd,
        val endDate: LocalDate?,
        val occurrenceCount: Int?,
        val interpretation: String,
    ) {
        fun toAssumption() = TaskCaptureAssumption(
            kind = TaskCaptureAssumptionKind.RepeatEnd,
            sourceText = match.value,
            start = match.range.first,
            endExclusive = match.range.last + 1,
            interpretation = interpretation,
        )
    }

    private data class ValueCandidate<T>(
        val match: MatchResult,
        val value: T,
        val kind: TaskCaptureAssumptionKind,
        val interpretation: String,
    ) {
        fun toAssumption() = TaskCaptureAssumption(
            kind = kind,
            sourceText = match.value,
            start = match.range.first,
            endExclusive = match.range.last + 1,
            interpretation = interpretation,
        )
    }

    private fun RecurrenceRule.withEnd(candidate: RecurrenceEndCandidate?): RecurrenceRule = when (candidate?.end) {
        RecurrenceEnd.OnDate -> copy(end = RecurrenceEnd.OnDate, endDate = candidate.endDate, occurrenceCount = null)
        RecurrenceEnd.AfterCount -> copy(
            end = RecurrenceEnd.AfterCount,
            endDate = null,
            occurrenceCount = candidate.occurrenceCount,
        )
        else -> this
    }

    private fun parseDate(value: String): LocalDate? = try {
        LocalDate.parse(value)
    } catch (_: DateTimeParseException) {
        null
    }

    private fun parseMonthDate(monthText: String, dayText: String, yearText: String, today: LocalDate): LocalDate? {
        val month = monthNumber(monthText)
        val day = dayText.toIntOrNull() ?: return null
        val explicitYear = yearText.toIntOrNull()
        val first = runCatching { LocalDate.of(explicitYear ?: today.year, month, day) }.getOrNull() ?: return null
        return if (explicitYear == null && first.isBefore(today)) first.plusYears(1) else first
    }

    private fun relativeDate(today: LocalDate, amountText: String, unitText: String): LocalDate? {
        val amount = amountText.toLongOrNull()?.takeIf { it in 1..3_650 } ?: return null
        return when (unitText.lowercase().removeSuffix("s")) {
            "day" -> today.plusDays(amount)
            "week" -> today.plusWeeks(amount)
            "month" -> today.plusMonths(amount)
            else -> today.plusYears(amount)
        }
    }

    private fun nextDayOfMonth(today: LocalDate, day: Int): LocalDate? {
        if (day !in 1..31) return null
        var month = YearMonth.from(today)
        repeat(24) {
            if (day <= month.lengthOfMonth()) {
                val candidate = month.atDay(day)
                if (!candidate.isBefore(today)) return candidate
            }
            month = month.plusMonths(1)
        }
        return null
    }

    private fun parseTimeMinutes(source: String): Int? {
        val normalized = source.lowercase().replace(".", "").replace(" ", "")
        if (normalized == "noon") return 12 * 60
        if (normalized == "midnight") return 0
        val amPm = normalized.takeLast(2).takeIf { it == "am" || it == "pm" }
        val clock = if (amPm == null) normalized else normalized.dropLast(2)
        val parts = clock.split(':')
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: return null
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
        if (minute !in 0..59) return null
        val resolvedHour = if (amPm == null) {
            hour.takeIf { it in 0..23 } ?: return null
        } else {
            if (hour !in 1..12) return null
            when {
                amPm == "am" && hour == 12 -> 0
                amPm == "pm" && hour < 12 -> hour + 12
                else -> hour
            }
        }
        return resolvedHour * 60 + minute
    }

    private fun parseDurationMinutes(source: String): Int? {
        val hours = Regex("(\\d+)\\s*(?:hours?|hrs?|h)\\b", RegexOption.IGNORE_CASE)
            .find(source)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val minutes = Regex("(\\d+)\\s*(?:minutes?|mins?|m)\\b", RegexOption.IGNORE_CASE)
            .find(source)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        return (hours * 60 + minutes).takeIf { it > 0 }
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

    private fun parseWeekdays(value: String): Set<DayOfWeek> = weekdayTokenPattern
        .findAll(value)
        .map { parseWeekday(it.value) }
        .toCollection(linkedSetOf())

    private fun intervalValue(other: String, number: String): Int? = when {
        other.isNotBlank() -> 2
        number.isBlank() -> 1
        else -> number.toIntOrNull()?.takeIf { it > 0 }
    }

    private fun recurrenceUnit(value: String): RecurrenceUnit = when (value.lowercase().removeSuffix("s")) {
        "day" -> RecurrenceUnit.Days
        "week" -> RecurrenceUnit.Weeks
        "month" -> RecurrenceUnit.Months
        else -> RecurrenceUnit.Years
    }

    private fun monthNumber(value: String): Int = when (value.take(3).lowercase()) {
        "jan" -> 1
        "feb" -> 2
        "mar" -> 3
        "apr" -> 4
        "may" -> 5
        "jun" -> 6
        "jul" -> 7
        "aug" -> 8
        "sep" -> 9
        "oct" -> 10
        "nov" -> 11
        else -> 12
    }

    private fun weekdayInterpretation(days: Set<DayOfWeek>, interval: Int = 1): String = buildString {
        append("Repeat · ")
        if (interval > 1) append("every $interval weeks · ")
        append(days.joinToString { it.label() })
    }

    private fun DayOfWeek.label(): String = name.lowercase().replaceFirstChar(Char::uppercase)

    private fun formatTime(minutes: Int): String {
        val hour24 = minutes / 60
        val minute = minutes % 60
        val suffix = if (hour24 < 12) "AM" else "PM"
        val hour12 = when (val value = hour24 % 12) {
            0 -> 12
            else -> value
        }
        return "$hour12:${minute.toString().padStart(2, '0')} $suffix"
    }

    private fun formatDuration(minutes: Int): String = when {
        minutes < 60 -> "$minutes min"
        minutes % 60 == 0 -> "${minutes / 60} hr"
        else -> "${minutes / 60} hr ${minutes % 60} min"
    }

    private fun nonOverlapping(assumptions: List<TaskCaptureAssumption>): List<TaskCaptureAssumption> =
        assumptions.sortedBy(TaskCaptureAssumption::start).fold(mutableListOf()) { accepted, candidate ->
            if (accepted.none { existing ->
                    candidate.start < existing.endExclusive && existing.start < candidate.endExclusive
                }
            ) {
                accepted += candidate
            }
            accepted
        }

    private fun IntRange.overlaps(other: IntRange): Boolean = first <= other.last && other.first <= last
}

package com.whip.app.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

object RecurrenceEngine {
    fun occurrencesBetween(
        rule: RecurrenceRule,
        from: LocalDate,
        through: LocalDate,
    ): List<LocalDate> {
        if (through.isBefore(from)) return emptyList()
        val lastDate = when (rule.end) {
            RecurrenceEnd.OnDate -> minOf(through, requireNotNull(rule.endDate))
            else -> through
        }
        if (lastDate.isBefore(rule.startDate)) return emptyList()
        val firstDate = maxOf(from, rule.startDate)
        val maximumCount = if (rule.end == RecurrenceEnd.AfterCount) {
            requireNotNull(rule.occurrenceCount)
        } else {
            Int.MAX_VALUE
        }

        return buildList {
            when (rule.unit) {
                RecurrenceUnit.Days -> {
                    val interval = rule.interval.toLong()
                    val elapsed = ChronoUnit.DAYS.between(rule.startDate, firstDate).coerceAtLeast(0)
                    var index = (elapsed + interval - 1) / interval
                    var date = rule.startDate.plusDays(index * interval)
                    while (!date.isAfter(lastDate) && index < maximumCount) {
                        add(date)
                        index++
                        date = rule.startDate.plusDays(index * interval)
                    }
                }
                RecurrenceUnit.Weeks -> {
                    val selectedDays = rule.weekdays.ifEmpty { setOf(rule.startDate.dayOfWeek) }.sortedBy { it.value }
                    val startWeek = rule.startDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                    val firstWeek = firstDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                    val weeksElapsed = ChronoUnit.WEEKS.between(startWeek, firstWeek).coerceAtLeast(0)
                    var periodIndex = weeksElapsed / rule.interval
                    val firstWeekCount = selectedDays.count { day ->
                        !startWeek.plusDays((day.value - 1).toLong()).isBefore(rule.startDate)
                    }
                    fun countBeforePeriod(period: Long): Long = when {
                        period <= 0 -> 0
                        else -> firstWeekCount.toLong() + (period - 1) * selectedDays.size
                    }
                    while (true) {
                        val week = startWeek.plusWeeks(periodIndex * rule.interval)
                        if (week.isAfter(lastDate)) break
                        var occurrenceIndex = countBeforePeriod(periodIndex)
                        selectedDays.forEach { day ->
                            val date = week.plusDays((day.value - 1).toLong())
                            if (date.isBefore(rule.startDate)) return@forEach
                            if (occurrenceIndex >= maximumCount) return@buildList
                            if (!date.isBefore(firstDate) && !date.isAfter(lastDate)) add(date)
                            occurrenceIndex++
                        }
                        periodIndex++
                    }
                }
                RecurrenceUnit.Months -> {
                    val elapsed = ChronoUnit.MONTHS.between(
                        rule.startDate.withDayOfMonth(1),
                        firstDate.withDayOfMonth(1),
                    ).coerceAtLeast(0)
                    var index = elapsed / rule.interval
                    var date = rule.startDate.plusMonths(index * rule.interval)
                    if (date.isBefore(firstDate)) {
                        index++
                        date = rule.startDate.plusMonths(index * rule.interval)
                    }
                    while (!date.isAfter(lastDate) && index < maximumCount) {
                        add(date)
                        index++
                        date = rule.startDate.plusMonths(index * rule.interval)
                    }
                }
                RecurrenceUnit.Years -> {
                    val elapsed = (firstDate.year - rule.startDate.year).coerceAtLeast(0).toLong()
                    var index = elapsed / rule.interval
                    var date = rule.startDate.plusYears(index * rule.interval)
                    if (date.isBefore(firstDate)) {
                        index++
                        date = rule.startDate.plusYears(index * rule.interval)
                    }
                    while (!date.isAfter(lastDate) && index < maximumCount) {
                        add(date)
                        index++
                        date = rule.startDate.plusYears(index * rule.interval)
                    }
                }
            }
        }
    }

    fun nextOccurrence(
        rule: RecurrenceRule,
        onOrAfter: LocalDate,
        searchDays: Long = 3660,
    ): LocalDate? {
        val lastAllowed = when (rule.end) {
            RecurrenceEnd.OnDate -> minOf(onOrAfter.plusDays(searchDays), requireNotNull(rule.endDate))
            else -> onOrAfter.plusDays(searchDays)
        }
        return occurrencesBetween(rule, onOrAfter, lastAllowed).firstOrNull()
    }

    fun previousOccurrence(rule: RecurrenceRule, onOrBefore: LocalDate): LocalDate? {
        val target = when (rule.end) {
            RecurrenceEnd.OnDate -> minOf(onOrBefore, requireNotNull(rule.endDate))
            else -> onOrBefore
        }
        if (target.isBefore(rule.startDate)) return null
        val lowerBound = if (rule.end == RecurrenceEnd.AfterCount) {
            rule.startDate
        } else when (rule.unit) {
            RecurrenceUnit.Days -> target.minusDays(rule.interval.toLong())
            RecurrenceUnit.Weeks -> target.minusWeeks(rule.interval.toLong()).minusDays(7)
            RecurrenceUnit.Months -> target.minusMonths(rule.interval.toLong() + 1)
            RecurrenceUnit.Years -> target.minusYears(rule.interval.toLong() + 1)
        }
        return occurrencesBetween(rule, maxOf(rule.startDate, lowerBound), target).lastOrNull()
    }

    private fun matches(rule: RecurrenceRule, date: LocalDate): Boolean {
        if (date.isBefore(rule.startDate)) return false

        return when (rule.unit) {
            RecurrenceUnit.Days -> {
                ChronoUnit.DAYS.between(rule.startDate, date) % rule.interval == 0L
            }
            RecurrenceUnit.Weeks -> {
                val selectedDays = rule.weekdays.ifEmpty { setOf(rule.startDate.dayOfWeek) }
                val startWeek = rule.startDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                val dateWeek = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                val weeksSinceStart = ChronoUnit.WEEKS.between(startWeek, dateWeek)
                weeksSinceStart % rule.interval == 0L && date.dayOfWeek in selectedDays
            }
            RecurrenceUnit.Months -> {
                val months = ChronoUnit.MONTHS.between(
                    rule.startDate.withDayOfMonth(1),
                    date.withDayOfMonth(1),
                )
                val expectedDay = minOf(rule.startDate.dayOfMonth, date.lengthOfMonth())
                months % rule.interval == 0L && date.dayOfMonth == expectedDay
            }
            RecurrenceUnit.Years -> {
                val years = date.year - rule.startDate.year
                val expectedMonth = rule.startDate.month
                val expectedDay = minOf(
                    rule.startDate.dayOfMonth,
                    YearMonth.of(date.year, expectedMonth).lengthOfMonth(),
                )
                years % rule.interval == 0 && date.month == expectedMonth &&
                    date.dayOfMonth == expectedDay
            }
        }
    }

    fun afterCompletion(rule: RecurrenceRule, completedOn: LocalDate): LocalDate =
        when (rule.unit) {
            RecurrenceUnit.Days -> completedOn.plusDays(rule.interval.toLong())
            RecurrenceUnit.Weeks -> completedOn.plusWeeks(rule.interval.toLong())
            RecurrenceUnit.Months -> completedOn.plusMonths(rule.interval.toLong())
            RecurrenceUnit.Years -> completedOn.plusYears(rule.interval.toLong())
        }

    fun nextCompletionRelative(
        rule: RecurrenceRule,
        lastCompletedOn: LocalDate?,
        completedCount: Int,
    ): LocalDate? {
        if (rule.end == RecurrenceEnd.AfterCount && completedCount >= requireNotNull(rule.occurrenceCount)) {
            return null
        }
        val candidate = lastCompletedOn?.let { afterCompletion(rule, it) } ?: rule.startDate
        return candidate.takeUnless {
            rule.end == RecurrenceEnd.OnDate && it.isAfter(requireNotNull(rule.endDate))
        }
    }
}

fun Set<DayOfWeek>.toWeekdayMask(): Int = fold(0) { mask, day ->
    mask or (1 shl (day.value - 1))
}

fun Int.toWeekdays(): Set<DayOfWeek> = DayOfWeek.entries
    .filterTo(linkedSetOf()) { day -> this and (1 shl (day.value - 1)) != 0 }

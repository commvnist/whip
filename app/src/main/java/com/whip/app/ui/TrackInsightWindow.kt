package com.whip.app.ui

import java.time.LocalDate

/** Recent evidence includes both boundary dates; future Entries remain all-time evidence. */
internal fun Iterable<LocalDate>.trackInsightCount(today: LocalDate, days: Int): Int {
    require(days > 0)
    val first = today.minusDays(days.toLong() - 1)
    return count { it in first..today }
}

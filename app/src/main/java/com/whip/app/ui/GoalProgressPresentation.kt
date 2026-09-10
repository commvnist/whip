package com.whip.app.ui

import java.text.NumberFormat
import java.util.Locale

/** Formats the domain's progress ratio without hiding a beginning or inventing a completed target. */
internal fun formatGoalProgressPercent(progress: Double, locale: Locale = Locale.getDefault()): String {
    val percent = NumberFormat.getPercentInstance(locale).apply {
        maximumFractionDigits = 1
        isGroupingUsed = false
    }
    return when {
        progress == 0.0 -> percent.format(0.0)
        progress > 0.0 && progress < 0.001 -> "<${percent.format(0.001)}"
        progress > 0.999 && progress < 1.0 -> ">${percent.format(0.999)}"
        else -> percent.format(progress)
    }
}

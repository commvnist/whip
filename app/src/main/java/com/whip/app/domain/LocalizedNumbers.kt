package com.whip.app.domain

import java.text.DecimalFormatSymbols
import java.util.Locale

/** Parses direct numeric input without silently accepting grouping separators.
 * Whip accepts the current locale's decimal separator and always accepts a dot,
 * which keeps imported workout notation and hardware-keyboard entry predictable. */
fun String.toWhipDoubleOrNull(locale: Locale = Locale.getDefault()): Double? {
    val source = trim().replace('\u00a0', ' ').replace(" ", "")
    if (source.isBlank()) return null
    val decimalSeparator = DecimalFormatSymbols.getInstance(locale).decimalSeparator
    if (source.count { it == '.' } + source.count { it == ',' } > 1) return null
    val normalized = when {
        ',' in source && decimalSeparator == ',' -> source.replace(',', '.')
        ',' in source -> return null
        else -> source
    }
    return normalized.toDoubleOrNull()?.takeIf(Double::isFinite)
}

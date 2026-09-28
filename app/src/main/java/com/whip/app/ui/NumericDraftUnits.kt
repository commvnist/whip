package com.whip.app.ui

import com.whip.app.domain.UnitDefinition
import com.whip.app.domain.plainNumericValue
import com.whip.app.domain.toWhipDoubleOrNull

/** Convert complete physical values together; the caller keeps an incomplete draft unchanged. */
internal fun convertNumericDraftValues(raw: List<String>, from: UnitDefinition, to: UnitDefinition): List<String>? {
    if (from.id == to.id) return raw
    if (from.dimension != to.dimension) return null
    return raw.map { text ->
        if (text.isBlank()) "" else {
            val value = text.toWhipDoubleOrNull() ?: return null
            val converted = to.fromCanonical(from.toCanonical(value))
            if (!converted.isFinite()) return null
            plainNumericValue(converted)
        }
    }
}

package com.whip.app.domain

/** Keep a duplicate editable under its entity's existing name-length contract. */
internal fun copiedItemName(
    name: String,
    maximumLength: Int = 100,
    codePointLimit: Boolean = false,
    suffix: String = " copy",
): String {
    val remaining = maximumLength - if (codePointLimit) suffix.codePointCount(0, suffix.length) else suffix.length
    require(remaining > 0)
    val end = if (codePointLimit) {
        name.offsetByCodePoints(0, minOf(remaining, name.codePointCount(0, name.length)))
    } else {
        minOf(remaining, name.length).let { index ->
            if (index in 1 until name.length && name[index - 1].isHighSurrogate() && name[index].isLowSurrogate()) index - 1
            else index
        }
    }
    return name.substring(0, end).trimEnd() + suffix
}

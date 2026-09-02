package com.whip.app.core

/**
 * Bounds untrusted text before it can enter Activity saved state or a Task editor.
 * Limits are deliberately code-point and UTF-8 based so surrogate pairs are never
 * split and unusually expensive payloads cannot grow an Android saved-state transaction.
 */
data class BoundedSharedTaskCapture(
    val text: String,
    val wasShortened: Boolean,
)

object SharedTaskCapturePolicy {
    const val MAX_INPUT_CODE_POINTS = 8_192
    const val MAX_INPUT_UTF8_BYTES = 32_768
    const val MAX_TITLE_CODE_POINTS = 200
    const val MAX_SUBTASKS = 50
    const val MAX_SUBTASK_CODE_POINTS = 200

    fun boundTaskTitle(value: String): String = takeCodePoints(value, MAX_TITLE_CODE_POINTS).value

    fun taskTitleCodePointCount(value: String): Int = value.codePointCount(0, value.length)

    fun bound(raw: String?): BoundedSharedTaskCapture? {
        val source = raw?.trim()?.takeIf(String::isNotBlank) ?: return null
        val input = takeCodePointsAndUtf8Bytes(
            value = source.replace("\r\n", "\n").replace('\r', '\n'),
            maxCodePoints = MAX_INPUT_CODE_POINTS,
            maxUtf8Bytes = MAX_INPUT_UTF8_BYTES,
        )
        val lines = input.value.lineSequence().map(String::trim).filter(String::isNotBlank).toList()
        if (lines.isEmpty()) return null

        val title = takeCodePoints(lines.first(), MAX_TITLE_CODE_POINTS)
        val requestedSubtasks = lines.drop(1)
        val subtasks = requestedSubtasks.take(MAX_SUBTASKS).map { line ->
            takeCodePoints(line, MAX_SUBTASK_CODE_POINTS)
        }
        val shortened = input.shortened ||
            title.shortened ||
            requestedSubtasks.size > MAX_SUBTASKS ||
            subtasks.any(BoundedText::shortened)
        val text = buildList {
            add(title.value)
            addAll(subtasks.map(BoundedText::value))
        }.joinToString("\n")
        return BoundedSharedTaskCapture(text = text, wasShortened = shortened)
    }

    private data class BoundedText(
        val value: String,
        val shortened: Boolean,
    )

    private fun takeCodePoints(value: String, maxCodePoints: Int): BoundedText {
        val codePointCount = value.codePointCount(0, value.length)
        if (codePointCount <= maxCodePoints) return BoundedText(value, shortened = false)
        val end = value.offsetByCodePoints(0, maxCodePoints)
        return BoundedText(value.substring(0, end), shortened = true)
    }

    private fun takeCodePointsAndUtf8Bytes(
        value: String,
        maxCodePoints: Int,
        maxUtf8Bytes: Int,
    ): BoundedText {
        var index = 0
        var codePoints = 0
        var utf8Bytes = 0
        while (index < value.length && codePoints < maxCodePoints) {
            val codePoint = value.codePointAt(index)
            val nextBytes = when {
                codePoint <= 0x7f -> 1
                codePoint <= 0x7ff -> 2
                codePoint <= 0xffff -> 3
                else -> 4
            }
            if (utf8Bytes + nextBytes > maxUtf8Bytes) break
            utf8Bytes += nextBytes
            codePoints++
            index += Character.charCount(codePoint)
        }
        return BoundedText(value.substring(0, index), shortened = index < value.length)
    }
}

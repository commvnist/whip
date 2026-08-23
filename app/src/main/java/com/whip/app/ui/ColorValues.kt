package com.whip.app.ui

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

internal data class WhipColorPreset(val name: String, val argb: Long)

/**
 * The single product palette for user-assigned colors. Keep this order stable: saved
 * values are ARGB, while the names and order form the visual language users learn.
 */
internal val WhipColorPresets = listOf(
    WhipColorPreset("Rose", 0xFFC83A5AL),
    WhipColorPreset("Coral", 0xFFD65A31L),
    WhipColorPreset("Amber", 0xFFC47A00L),
    WhipColorPreset("Leaf", 0xFF3F7D44L),
    WhipColorPreset("Teal", 0xFF00796BL),
    WhipColorPreset("Sky", 0xFF0277BDL),
    WhipColorPreset("Blue", 0xFF315CB5L),
    WhipColorPreset("Indigo", 0xFF5145A6L),
    WhipColorPreset("Violet", 0xFF7B3FA1L),
    WhipColorPreset("Pink", 0xFFAD3D7CL),
    WhipColorPreset("Brown", 0xFF765548L),
    WhipColorPreset("Slate", 0xFF59636EL),
)

internal data class HsvColor(val hue: Float, val saturation: Float, val brightness: Float)

fun colorArgbToHex(value: Long?): String = value?.let { "#%08X".format(it and 0xFFFFFFFFL) }.orEmpty()

internal fun colorArgbToRgbHex(value: Long?): String = value?.let { "#%06X".format(it and 0xFFFFFFL) }.orEmpty()

fun parseColorArgb(value: String): Long? {
    val clean = value.trim().removePrefix("#")
    if (clean.isBlank()) return null
    val argb = when (clean.length) {
        6 -> "FF$clean"
        8 -> clean
        else -> return null
    }
    return argb.toLongOrNull(16)
}

internal fun colorDisplayName(value: Long?): String = when (value) {
    null -> "Default"
    else -> WhipColorPresets.firstOrNull { it.argb == opaqueColor(value) }?.name
        ?: "Custom · ${colorArgbToRgbHex(value)}"
}

internal fun opaqueColor(value: Long): Long = 0xFF000000L or (value and 0xFFFFFFL)

internal fun colorArgbToHsv(value: Long): HsvColor {
    val opaque = opaqueColor(value)
    val red = ((opaque shr 16) and 0xFF).toFloat() / 255f
    val green = ((opaque shr 8) and 0xFF).toFloat() / 255f
    val blue = (opaque and 0xFF).toFloat() / 255f
    val maximum = max(red, max(green, blue))
    val minimum = min(red, min(green, blue))
    val delta = maximum - minimum
    val hue = when {
        delta == 0f -> 0f
        maximum == red -> 60f * (((green - blue) / delta) % 6f)
        maximum == green -> 60f * (((blue - red) / delta) + 2f)
        else -> 60f * (((red - green) / delta) + 4f)
    }.let { if (it < 0f) it + 360f else it }
    return HsvColor(
        hue = hue,
        saturation = if (maximum == 0f) 0f else delta / maximum,
        brightness = maximum,
    )
}

internal fun hsvToColorArgb(hue: Float, saturation: Float, brightness: Float): Long {
    val normalizedHue = ((hue % 360f) + 360f) % 360f
    val normalizedSaturation = saturation.coerceIn(0f, 1f)
    val normalizedBrightness = brightness.coerceIn(0f, 1f)
    val chroma = normalizedBrightness * normalizedSaturation
    val section = normalizedHue / 60f
    val intermediate = chroma * (1f - abs(section % 2f - 1f))
    val (redPrime, greenPrime, bluePrime) = when (section.toInt().coerceIn(0, 5)) {
        0 -> Triple(chroma, intermediate, 0f)
        1 -> Triple(intermediate, chroma, 0f)
        2 -> Triple(0f, chroma, intermediate)
        3 -> Triple(0f, intermediate, chroma)
        4 -> Triple(intermediate, 0f, chroma)
        else -> Triple(chroma, 0f, intermediate)
    }
    val match = normalizedBrightness - chroma
    val red = ((redPrime + match) * 255f).roundToInt().coerceIn(0, 255)
    val green = ((greenPrime + match) * 255f).roundToInt().coerceIn(0, 255)
    val blue = ((bluePrime + match) * 255f).roundToInt().coerceIn(0, 255)
    return 0xFF000000L or (red.toLong() shl 16) or (green.toLong() shl 8) or blue.toLong()
}

package com.whip.app.ui

fun colorArgbToHex(value: Long?): String = value?.let { "#%08X".format(it and 0xFFFFFFFFL) }.orEmpty()

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

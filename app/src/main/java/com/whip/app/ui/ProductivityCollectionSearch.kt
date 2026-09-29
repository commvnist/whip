package com.whip.app.ui

import com.whip.app.domain.Goal
import com.whip.app.domain.Habit
import com.whip.app.domain.Track
import java.util.Locale

/** Prepared once per changed collection, not once per row on each keystroke. */
internal fun Habit.collectionSearchText(): String =
    "$name $notes ${tags.joinToString(" ")} $area ${trackingMode.uiLabel()} ${if (archived) "archived" else if (paused) "paused" else "active"}"
        .lowercase(Locale.ROOT)

internal fun Goal.collectionSearchText(): String =
    "$name $description ${tags.joinToString(" ")} $area ${type.name.replace(Regex("([a-z])([A-Z])"), "$1 $2")} ${status.name} ${if (archived) "archived" else ""}"
        .lowercase(Locale.ROOT)

internal fun Track.collectionSearchText(): String =
    "$name $description ${tags.joinToString(" ")} $area ${if (archived) "archived" else "active"}"
        .lowercase(Locale.ROOT)

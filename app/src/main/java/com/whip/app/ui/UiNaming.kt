package com.whip.app.ui

import com.whip.app.domain.GymGraphRange
import com.whip.app.domain.HabitTrackingMode
import com.whip.app.domain.LinkSourceMetric
import com.whip.app.domain.RepeatStepPolicy
import com.whip.app.domain.UnitDimension
import com.whip.app.domain.WorkoutSetClassification

/**
 * Title Case for interface chrome only: page names, tabs, buttons, menus, and
 * disclosures. Callers must not apply this to user-authored names or prose.
 * Existing capitals are preserved so acronyms such as AMRAP and 1RM survive.
 */
private val uiTitleWord = Regex("[\\p{L}\\p{N}]+(?:['’][\\p{L}\\p{N}]+)*")
private val uiTitleMinorWords = setOf(
    "a", "an", "and", "as", "at", "but", "by", "for", "from", "in", "into",
    "nor", "of", "on", "or", "the", "to", "via", "with", "without",
)

internal fun String.uiTitleCase(): String {
    val words = uiTitleWord.findAll(this).toList()
    if (words.isEmpty()) return this
    var wordIndex = 0
    return uiTitleWord.replace(this) { match ->
        val source = match.value
        val lower = source.lowercase()
        val isEdgeWord = wordIndex == 0 || wordIndex == words.lastIndex
        wordIndex += 1
        if (!isEdgeWord && lower in uiTitleMinorWords) {
            lower
        } else {
            source.replaceFirstChar(Char::uppercase)
        }
    }
}

/** Canonical user-facing names for values whose storage identifiers are not UI copy. */
internal fun HabitTrackingMode.uiLabel(): String = when (this) {
    HabitTrackingMode.CheckOff -> "Check Off"
    HabitTrackingMode.Count -> "Count"
    HabitTrackingMode.Decimal -> "Measurement"
    HabitTrackingMode.Duration -> "Timer"
    HabitTrackingMode.Checklist -> "Checklist"
    HabitTrackingMode.Rating -> "Rating"
    HabitTrackingMode.LogOnly -> "Log Only"
}

internal fun RepeatStepPolicy.uiLabel(): String = when (this) {
    RepeatStepPolicy.Reset -> "Reset Subtasks"
    RepeatStepPolicy.CarryUnfinished -> "Carry Unfinished Subtasks"
}

internal fun WorkoutSetClassification.uiLabel(): String = when (this) {
    WorkoutSetClassification.WarmUp -> "Warm-up"
    WorkoutSetClassification.Working -> "Working"
    WorkoutSetClassification.BackOff -> "Back-off"
    WorkoutSetClassification.Drop -> "Drop"
    WorkoutSetClassification.Amrap -> "AMRAP"
    WorkoutSetClassification.Failure -> "Failure"
}

internal fun String.workoutSetClassificationLabel(): String =
    WorkoutSetClassification.entries.firstOrNull { it.name == this }?.uiLabel()
        ?: replace(Regex("([a-z])([A-Z])"), "$1 $2").replaceFirstChar(Char::uppercase)

internal fun GymGraphRange.uiLabel(): String = when (this) {
    GymGraphRange.Month -> "1 Month"
    GymGraphRange.ThreeMonths -> "3 Months"
    GymGraphRange.SixMonths -> "6 Months"
    GymGraphRange.Year -> "1 Year"
    GymGraphRange.All -> "All Time"
    GymGraphRange.Custom -> "Custom"
}

internal fun LinkSourceMetric.uiLabel(): String = when (this) {
    LinkSourceMetric.NumericValue -> "Numeric Value"
    LinkSourceMetric.Success -> "Success"
    LinkSourceMetric.Completion -> "Completion"
    LinkSourceMetric.Count -> "Count"
    LinkSourceMetric.Duration -> "Duration"
    LinkSourceMetric.Volume -> "Volume"
    LinkSourceMetric.EstimatedOneRepMax -> "Estimated 1RM"
    LinkSourceMetric.MaxWeight -> "Maximum Weight"
    LinkSourceMetric.Distance -> "Distance"
    LinkSourceMetric.Repetitions -> "Repetitions"
}

internal fun UnitDimension.uiLabel(): String = when (this) {
    UnitDimension.Count -> "Count"
    UnitDimension.Duration -> "Duration"
    UnitDimension.Distance -> "Distance"
    UnitDimension.Volume -> "Volume"
    UnitDimension.Mass -> "Mass"
    UnitDimension.Length -> "Length"
    UnitDimension.Money -> "Money"
    UnitDimension.Energy -> "Energy"
    UnitDimension.Percentage -> "Percentage"
    UnitDimension.Unitless -> "No Unit"
    UnitDimension.Custom -> "Custom"
}

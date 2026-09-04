package com.whip.app.ui

import com.whip.app.domain.GymGraphRange
import com.whip.app.domain.GymGraphAggregation
import com.whip.app.domain.PersonalRecordType
import com.whip.app.domain.HabitTrackingMode
import com.whip.app.domain.HabitLogStatus
import com.whip.app.domain.LinkSourceMeasurement
import com.whip.app.domain.MeasurementEntryStatus
import com.whip.app.domain.MeasurementSourceType
import com.whip.app.domain.RepeatStepPolicy
import com.whip.app.domain.UnitDimension
import com.whip.app.domain.WorkoutSetClassification

internal enum class SortDirection(val label: String) {
    Ascending("Ascending"),
    Descending("Descending"),
}

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

/** Activity copy must describe what happened, never expose stored enum names. */
internal fun HabitLogStatus.activityLabel(): String = when (this) {
    HabitLogStatus.Recorded -> "Logged"
    HabitLogStatus.Success -> "Completed"
    HabitLogStatus.Failed -> "Below target"
}

internal fun MeasurementEntryStatus.activityLabel(): String = when (this) {
    MeasurementEntryStatus.Recorded -> "Logged"
    MeasurementEntryStatus.Missing -> "No entry"
    MeasurementEntryStatus.Failed -> "Below target"
    MeasurementEntryStatus.Skipped -> "Skipped"
    MeasurementEntryStatus.Excused -> "Excused"
}

internal fun MeasurementSourceType.uiLabel(): String = when (this) {
    MeasurementSourceType.Manual -> "Whip"
    MeasurementSourceType.Habit -> "Habit"
    MeasurementSourceType.Goal -> "Goal"
    MeasurementSourceType.Task -> "Task"
    MeasurementSourceType.Workout -> "Workout"
    MeasurementSourceType.Exercise -> "Exercise"
    MeasurementSourceType.Track -> "Track"
    MeasurementSourceType.Import -> "Import"
    MeasurementSourceType.HealthConnect -> "Health Connect"
}

/** Manual activity needs no attribution; connected activity should explain why it is read-only. */
internal fun MeasurementSourceType.activityAttribution(): String? = when (this) {
    MeasurementSourceType.Manual -> null
    MeasurementSourceType.HealthConnect -> "Synced from Health Connect"
    MeasurementSourceType.Import -> "Imported"
    else -> "Added from ${uiLabel()}"
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
    WorkoutSetClassification.TrainingMaxTest -> "Training Max test"
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

internal fun GymGraphAggregation.uiLabel(): String = when (this) {
    GymGraphAggregation.Workout -> "Each Workout"
    GymGraphAggregation.Week -> "Weekly"
    GymGraphAggregation.Month -> "Monthly"
}

internal fun PersonalRecordType.uiLabel(): String = when (this) {
    PersonalRecordType.MaxWeight -> "Heaviest Weight"
    PersonalRecordType.MaxRepetitions -> "Most Repetitions"
    PersonalRecordType.MaxRepetitionsForWeight -> "Most Repetitions at a Weight"
    PersonalRecordType.BestWeightForRepCount -> "Heaviest Weight for a Rep Count"
    PersonalRecordType.EstimatedOneRepMax -> "Estimated 1RM"
    PersonalRecordType.SetVolume -> "Best Set Volume"
    PersonalRecordType.ExerciseWorkoutVolume -> "Highest Workout Volume"
    PersonalRecordType.MaxDistance -> "Longest Distance"
    PersonalRecordType.MaxDuration -> "Longest Duration"
    PersonalRecordType.MaxSpeed -> "Fastest Speed"
    PersonalRecordType.MinPace -> "Fastest Pace"
    PersonalRecordType.MaxMachineSetting -> "Best Machine Setting"
}

internal fun LinkSourceMeasurement.uiLabel(): String = when (this) {
    LinkSourceMeasurement.NumericValue -> "Numeric Value"
    LinkSourceMeasurement.Success -> "Success"
    LinkSourceMeasurement.Completion -> "Completion"
    LinkSourceMeasurement.Count -> "Count"
    LinkSourceMeasurement.Duration -> "Duration"
    LinkSourceMeasurement.Volume -> "Volume"
    LinkSourceMeasurement.EstimatedOneRepMax -> "Estimated 1RM"
    LinkSourceMeasurement.MaxWeight -> "Maximum Weight"
    LinkSourceMeasurement.Distance -> "Distance"
    LinkSourceMeasurement.Repetitions -> "Repetitions"
    LinkSourceMeasurement.EntryCount -> "Entry Count"
    LinkSourceMeasurement.FieldValue -> "Field Value"
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
    UnitDimension.Temperature -> "Temperature"
    UnitDimension.Speed -> "Speed"
    UnitDimension.Pace -> "Pace"
    UnitDimension.Frequency -> "Frequency"
    UnitDimension.Percentage -> "Percentage"
    UnitDimension.Unitless -> "No Unit"
    UnitDimension.Custom -> "Custom"
}

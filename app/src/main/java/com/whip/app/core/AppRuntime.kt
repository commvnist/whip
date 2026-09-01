package com.whip.app.core

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.UUID

interface WhipClock {
    fun now(): Instant

    fun zoneId(): ZoneId = ZoneId.systemDefault()

    fun today(zoneId: ZoneId = zoneId()): LocalDate =
        now().atZone(zoneId).toLocalDate()
}

object SystemWhipClock : WhipClock {
    override fun now(): Instant = Instant.now()
}

data class ExactLocalTimeOption(
    val instant: Instant,
    val offset: ZoneOffset,
)

data class ExactLocalTimeResolution(
    val localDateTime: LocalDateTime,
    val options: List<ExactLocalTimeOption>,
    val firstValidDateTimeAfterGap: LocalDateTime? = null,
) {
    val isGap: Boolean get() = options.isEmpty()
    val isOverlap: Boolean get() = options.size > 1

    fun selected(preferredOffsetSeconds: Int?): ExactLocalTimeOption? =
        when {
            options.size <= 1 -> options.firstOrNull()
            preferredOffsetSeconds == null -> null
            else -> options.firstOrNull { it.offset.totalSeconds == preferredOffsetSeconds }
        }
}

/** Exact wall-time resolution for user-entered instants; gaps never normalize silently. */
fun resolveExactLocalTime(
    date: LocalDate,
    minutes: Int,
    zoneId: ZoneId,
): ExactLocalTimeResolution {
    require(minutes in 0..1_439)
    val local = LocalDateTime.of(date, LocalTime.of(minutes / 60, minutes % 60))
    val offsets = zoneId.rules.getValidOffsets(local)
    return ExactLocalTimeResolution(
        localDateTime = local,
        options = offsets.map { offset -> ExactLocalTimeOption(local.toInstant(offset), offset) },
        firstValidDateTimeAfterGap = if (offsets.isEmpty()) zoneId.rules.getTransition(local)?.dateTimeAfter else null,
    )
}

/** Keeps persisted instants byte-for-byte stable until the user edits their wall time. */
fun resolveEditedExactInstant(
    initialInstant: Instant,
    wallTimeEdited: Boolean,
    resolution: ExactLocalTimeResolution,
    preferredOffsetSeconds: Int?,
): Instant? = if (wallTimeEdited) resolution.selected(preferredOffsetSeconds)?.instant else initialInstant

fun interface WhipIdGenerator {
    fun nextId(): String
}

object UuidWhipIdGenerator : WhipIdGenerator {
    override fun nextId(): String = UUID.randomUUID().toString()
}

sealed interface OperationStatus {
    data object Idle : OperationStatus
    data class Running(val message: String) : OperationStatus
    data class Succeeded(
        val message: String,
        val feedbackPresentation: OperationFeedbackPresentation = OperationFeedbackPresentation.Inline,
        val recoveryToken: Long? = null,
    ) : OperationStatus
    data class Failed(val message: String, val cause: Throwable? = null) : OperationStatus
}

enum class OperationFeedbackPresentation {
    /** The changed control or content is the acknowledgement; do not add a transient banner. */
    Inline,

    /** Reserve transient feedback for recovery, continuation, or consequential outcomes. */
    Snackbar,
}

sealed interface WhipResult<out T> {
    data class Success<T>(val value: T) : WhipResult<T>
    data class Failure(val message: String, val cause: Throwable? = null) : WhipResult<Nothing>
}

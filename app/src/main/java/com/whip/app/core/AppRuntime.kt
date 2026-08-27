package com.whip.app.core

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
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

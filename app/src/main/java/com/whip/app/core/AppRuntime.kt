package com.whip.app.core

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.UUID
import java.util.concurrent.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow

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

/** Receipt for an authored entity after its authoritative repository write committed. */
data class EntitySaveReceipt(
    val entityId: Long?,
    val areaId: String?,
    val warnings: List<String> = emptyList(),
    val areaVerified: Boolean = true,
)

class CommittedEntitySaveCancellation(
    val receipt: EntitySaveReceipt,
    cause: CancellationException,
) : CancellationException(cause.message) {
    init {
        initCause(cause)
    }
}

/**
 * Makes the repository write an explicit point of no return. After [commit]
 * succeeds, ordinary follow-up failures can only add warnings; they can never
 * produce a retryable failure for an entity that already exists.
 */
suspend fun completeCommittedEntitySave(
    commit: suspend () -> EntitySaveReceipt,
    followUp: suspend (EntitySaveReceipt) -> EntitySaveReceipt,
): EntitySaveReceipt {
    val committed = commit()
    return try {
        followUp(committed)
    } catch (cancelled: CancellationException) {
        throw CommittedEntitySaveCancellation(committed, cancelled)
    } catch (_: Exception) {
        committed.copy(
            warnings = committed.warnings + "Some post-save updates did not finish; the item itself was saved.",
        )
    }
}

/**
 * Runs derived work after an entity commit without turning that committed save
 * into a retryable failure. Structured cancellation is never downgraded to a warning.
 */
suspend fun saveFollowUpWarning(
    warning: String,
    block: suspend () -> Unit,
): String? = try {
    block()
    null
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (_: Exception) {
    warning
}

/**
 * Request-scoped save state survives Activity recreation with its ViewModel.
 * UI must only react to a terminal result whose request id it owns.
 */
sealed interface PersistenceRequestState<out T> {
    data object Idle : PersistenceRequestState<Nothing>
    data class Running(val requestId: String) : PersistenceRequestState<Nothing>
    data class Finished<T>(
        val requestId: String,
        val result: WhipResult<T>,
    ) : PersistenceRequestState<T>
}

/**
 * Atomically admits one authored save request. This prevents two near-simultaneous
 * taps or callers from both passing a separate read-then-write Running check.
 */
fun <T> MutableStateFlow<PersistenceRequestState<T>>.tryStartPersistenceRequest(
    requestId: String,
): Boolean {
    while (true) {
        val current = value
        if (current !is PersistenceRequestState.Idle) return false
        if (compareAndSet(current, PersistenceRequestState.Running(requestId))) return true
    }
}

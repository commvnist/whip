package com.whip.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.staticCompositionLocalOf
import com.whip.app.core.WhipLaunchActions
import com.whip.app.domain.ScheduledTask
import com.whip.app.widget.WhipWidgetProvider
import java.time.LocalDate

/**
 * Keeps platform-entry delivery policy out of the already broad app shell.
 * Besides keeping the effect testable, this prevents Compose/Jacoco from
 * folding another complete navigation state machine into WhipScreen's class.
 */
internal sealed interface LaunchDeliveryCommand {
    data class LoadFailed(val destination: AppDestination) : LaunchDeliveryCommand
    data class Unavailable(val destination: AppDestination, val message: String) : LaunchDeliveryCommand
    data object OpenTaskAgenda : LaunchDeliveryCommand
    data object OpenHabitTracking : LaunchDeliveryCommand
    data class AddTask(val date: LocalDate?) : LaunchDeliveryCommand
    data class CaptureSharedTask(
        val text: String,
        val wasShortened: Boolean,
    ) : LaunchDeliveryCommand
    data object AddHabit : LaunchDeliveryCommand
    data class OpenTask(
        val item: ScheduledTask,
        val completed: Boolean,
        val destination: TaskDestination?,
    ) : LaunchDeliveryCommand
    data class OpenTaskFallback(val message: String) : LaunchDeliveryCommand
    data class OpenHabit(val id: Long) : LaunchDeliveryCommand
    data class OpenGoal(val id: Long) : LaunchDeliveryCommand
    data object OpenGym : LaunchDeliveryCommand
    data class OpenTrack(val id: Long) : LaunchDeliveryCommand
    data class SharedTaskQueueOverflow(val count: Int) : LaunchDeliveryCommand
}

internal val LocalLaunchDeliveryConsumer = staticCompositionLocalOf<(Long) -> Unit> { {} }

@Composable
internal fun LaunchDeliveryEffect(
    launchDeliveryId: Long,
    consumedLaunchDeliveryId: Long?,
    areaSelectionReady: Boolean,
    initialAction: String?,
    initialEntityId: Long?,
    initialOccurrenceEpochDay: Long?,
    initialSharedText: String?,
    initialSharedTextShortened: Boolean,
    commandAdmissionVersion: Long,
    setupCompleted: Boolean,
    taskState: TaskUiState,
    habitState: HabitUiState,
    goalState: GoalUiState,
    trackState: TrackUiState,
    onConsume: (Long) -> Unit,
    onCommand: (LaunchDeliveryCommand) -> Boolean,
) {
    val externalConsumer = LocalLaunchDeliveryConsumer.current
    val consume: (Long) -> Unit = { deliveryId ->
        onConsume(deliveryId)
        externalConsumer(deliveryId)
    }
    LaunchedEffect(
        launchDeliveryId,
        initialAction,
        initialEntityId,
        initialOccurrenceEpochDay,
        initialSharedText,
        initialSharedTextShortened,
        commandAdmissionVersion,
        setupCompleted,
        areaSelectionReady,
        taskState,
        habitState,
        goalState,
        trackState,
    ) {
        if (
            launchDeliveryId == 0L ||
            consumedLaunchDeliveryId == launchDeliveryId ||
            !setupCompleted ||
            !areaSelectionReady
        ) {
            return@LaunchedEffect
        }
        val targetResolution = resolveLaunchTarget(
            action = initialAction,
            entityId = initialEntityId,
            occurrenceEpochDay = initialOccurrenceEpochDay,
            taskState = taskState,
            habitState = habitState,
            goalState = goalState,
            trackState = trackState,
        )
        when (targetResolution) {
            LaunchTargetResolution.Pending -> return@LaunchedEffect
            is LaunchTargetResolution.LoadFailed -> {
                onCommand(LaunchDeliveryCommand.LoadFailed(targetResolution.destination))
                return@LaunchedEffect
            }
            is LaunchTargetResolution.Unavailable -> {
                consume(launchDeliveryId)
                onCommand(
                    LaunchDeliveryCommand.Unavailable(
                        targetResolution.destination,
                        targetResolution.message,
                    ),
                )
                return@LaunchedEffect
            }
            LaunchTargetResolution.NotApplicable,
            is LaunchTargetResolution.Available -> Unit
        }
        var admitted = true
        when (initialAction) {
            WhipWidgetProvider.ACTION_OPEN_TASK_AGENDA -> {
                admitted = onCommand(LaunchDeliveryCommand.OpenTaskAgenda)
            }
            WhipWidgetProvider.ACTION_OPEN_HABIT_TRACKING -> {
                admitted = onCommand(LaunchDeliveryCommand.OpenHabitTracking)
            }
            WhipWidgetProvider.ACTION_ADD_TASK -> {
                admitted = onCommand(
                    LaunchDeliveryCommand.AddTask(initialOccurrenceEpochDay?.let(LocalDate::ofEpochDay)),
                )
            }
            WhipLaunchActions.ACTION_CAPTURE_SHARED_TASK -> {
                admitted = onCommand(
                    LaunchDeliveryCommand.CaptureSharedTask(
                        text = initialSharedText.orEmpty(),
                        wasShortened = initialSharedTextShortened,
                    ),
                )
            }
            WhipWidgetProvider.ACTION_ADD_HABIT -> {
                admitted = onCommand(LaunchDeliveryCommand.AddHabit)
            }
            WhipLaunchActions.ACTION_OPEN_TASK -> {
                val id = initialEntityId ?: return@LaunchedEffect
                val allTasks = taskState.inbox + taskState.today + taskState.upcoming +
                    taskState.planning + taskState.completed + taskState.archived
                val found = allTasks.firstOrNull { item ->
                    item.task.id == id && (
                        initialOccurrenceEpochDay == null ||
                            item.originalDate?.toEpochDay() == initialOccurrenceEpochDay
                        )
                }
                if (found == null) {
                    val detail = (targetResolution as? LaunchTargetResolution.Available)?.unavailableDetail
                    if (detail != null) onCommand(LaunchDeliveryCommand.OpenTaskFallback(detail))
                    consume(launchDeliveryId)
                    return@LaunchedEffect
                }
                val destination = when (found) {
                    in taskState.inbox -> TaskDestination.Inbox
                    in taskState.planning -> TaskDestination.Upcoming
                    else -> null
                }
                admitted = onCommand(
                    LaunchDeliveryCommand.OpenTask(
                        found,
                        found in taskState.completed,
                        destination,
                    ),
                )
            }
            WhipLaunchActions.ACTION_OPEN_HABIT -> initialEntityId?.let {
                admitted = onCommand(LaunchDeliveryCommand.OpenHabit(it))
            }
            WhipLaunchActions.ACTION_OPEN_GOAL -> initialEntityId?.let {
                admitted = onCommand(LaunchDeliveryCommand.OpenGoal(it))
            }
            WhipLaunchActions.ACTION_OPEN_GYM -> {
                admitted = onCommand(LaunchDeliveryCommand.OpenGym)
            }
            WhipLaunchActions.ACTION_OPEN_TRACK -> initialEntityId?.let { trackId ->
                admitted = onCommand(LaunchDeliveryCommand.OpenTrack(trackId))
            }
            WhipLaunchActions.ACTION_SHARED_TASK_QUEUE_OVERFLOW -> {
                admitted = onCommand(
                    LaunchDeliveryCommand.SharedTaskQueueOverflow(
                        initialSharedText?.toIntOrNull()?.coerceAtLeast(1) ?: 1,
                    ),
                )
            }
        }
        if (admitted) consume(launchDeliveryId)
    }
}

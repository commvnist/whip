package com.whip.app.data

import androidx.room.withTransaction

/** Performs an all-or-nothing deletion of an Area and every item assigned to it. */
class AreaDeletionCoordinator(
    private val database: WhipDatabase,
    private val areaRepository: AreaRepository,
    private val taskDeletionCoordinator: TaskDeletionCoordinator,
    private val domainDeletionCoordinator: DomainDeletionCoordinator,
) {
    suspend fun deleteAreaAndItems(areaId: String): AreaDeletionSummary = database.withTransaction {
        requireNotNull(database.measurementDao().getArea(areaId)) { "Area no longer exists" }
        val taskIds = database.taskDao().getAllTasks().filter { it.areaId == areaId }.map { it.id }
        val habitIds = database.habitDao().getAllHabits().filter { it.areaId == areaId }.map { it.id }
        val goalIds = database.goalDao().getAllGoals().filter { it.areaId == areaId }.map { it.id }
        val trackIds = database.trackDao().getAllTracks().filter { it.areaId == areaId }.map { it.id }

        taskIds.forEach { taskDeletionCoordinator.delete(it) }
        habitIds.forEach { domainDeletionCoordinator.deleteHabit(it) }
        goalIds.forEach { domainDeletionCoordinator.deleteGoal(it) }
        trackIds.forEach { domainDeletionCoordinator.deleteTrack(it) }
        areaRepository.deletePermanently(areaId)

        AreaDeletionSummary(taskIds, habitIds, goalIds, trackIds)
    }
}

data class AreaDeletionSummary(
    val taskIds: List<Long>,
    val habitIds: List<Long>,
    val goalIds: List<Long>,
    val trackIds: List<Long>,
) {
    val total: Int get() = taskIds.size + habitIds.size + goalIds.size + trackIds.size
}

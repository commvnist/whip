package com.whip.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals ORDER BY pinned DESC, position, name")
    fun observeGoals(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goal_milestones ORDER BY goalId, position, id")
    fun observeMilestones(): Flow<List<GoalMilestoneEntity>>

    @Query("SELECT * FROM goals WHERE id = :id")
    suspend fun getGoal(id: Long): GoalEntity?

    @Query("SELECT * FROM goals")
    suspend fun getAllGoals(): List<GoalEntity>

    @Query("SELECT * FROM goals WHERE status IN ('Active', 'Paused')")
    suspend fun getOpenGoals(): List<GoalEntity>

    @Query("SELECT * FROM goal_milestones WHERE id = :id")
    suspend fun getMilestone(id: Long): GoalMilestoneEntity?

    @Query("SELECT * FROM goal_milestones WHERE goalId = :goalId ORDER BY position, id")
    suspend fun getMilestones(goalId: Long): List<GoalMilestoneEntity>

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM goals")
    suspend fun nextPosition(): Int

    @Insert suspend fun insertGoal(entity: GoalEntity): Long
    @Insert suspend fun insertMilestone(entity: GoalMilestoneEntity): Long
    @Update suspend fun updateGoal(entity: GoalEntity)
    @Update suspend fun updateMilestone(entity: GoalMilestoneEntity)

    @Query("DELETE FROM goal_milestones WHERE id = :id")
    suspend fun deleteMilestone(id: Long): Int

    @Query("DELETE FROM goals WHERE id = :id")
    suspend fun deleteGoal(id: Long): Int
}

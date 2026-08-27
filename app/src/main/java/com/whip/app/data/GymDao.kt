package com.whip.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface GymDao {
    @Query("SELECT * FROM gym_machines ORDER BY archived, name")
    fun observeMachines(): Flow<List<GymMachineEntity>>

    @Query("SELECT * FROM gym_machine_exercise_joins")
    fun observeMachineExerciseJoins(): Flow<List<GymMachineExerciseJoinEntity>>

    @Query("SELECT * FROM exercises ORDER BY favorite DESC, position, name")
    fun observeExercises(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercise_categories ORDER BY position, name")
    fun observeCategories(): Flow<List<ExerciseCategoryEntity>>

    @Query("SELECT * FROM exercise_category_joins")
    fun observeCategoryJoins(): Flow<List<ExerciseCategoryJoinEntity>>

    @Query("SELECT * FROM workout_sessions ORDER BY startedAtMillis DESC")
    fun observeSessions(): Flow<List<WorkoutSessionEntity>>

    @Query("SELECT * FROM workout_exercises ORDER BY sessionId, position, id")
    fun observeWorkoutExercises(): Flow<List<WorkoutExerciseEntity>>

    @Query("SELECT * FROM workout_sets ORDER BY workoutExerciseId, position, id")
    fun observeWorkoutSets(): Flow<List<WorkoutSetEntity>>

    @Query("SELECT * FROM workout_groups ORDER BY sessionId, position, id")
    fun observeWorkoutGroups(): Flow<List<WorkoutGroupEntity>>

    @Query("SELECT * FROM exercises")
    suspend fun getAllExercises(): List<ExerciseEntity>

    @Query("SELECT * FROM gym_machines")
    suspend fun getAllMachines(): List<GymMachineEntity>

    @Query("SELECT * FROM workout_sessions")
    suspend fun getAllSessions(): List<WorkoutSessionEntity>

    @Query("SELECT * FROM workout_exercises")
    suspend fun getAllWorkoutExercises(): List<WorkoutExerciseEntity>

    @Query("SELECT * FROM workout_sets")
    suspend fun getAllWorkoutSets(): List<WorkoutSetEntity>

    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun getExercise(id: Long): ExerciseEntity?

    @Query("SELECT * FROM gym_machines WHERE id = :id")
    suspend fun getMachine(id: Long): GymMachineEntity?

    @Query("SELECT * FROM gym_machine_exercise_joins WHERE machineId = :machineId")
    suspend fun getMachineExerciseJoins(machineId: Long): List<GymMachineExerciseJoinEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM gym_machine_exercise_joins WHERE machineId = :machineId AND exerciseId = :exerciseId)")
    suspend fun machineSupportsExercise(machineId: Long, exerciseId: Long): Boolean

    @Query("SELECT * FROM exercise_categories WHERE id = :id")
    suspend fun getCategory(id: Long): ExerciseCategoryEntity?

    @Query("SELECT * FROM exercise_category_joins WHERE exerciseId = :exerciseId")
    suspend fun getCategoryJoins(exerciseId: Long): List<ExerciseCategoryJoinEntity>

    @Query("SELECT * FROM workout_sessions WHERE id = :id")
    suspend fun getSession(id: Long): WorkoutSessionEntity?

    @Query("SELECT * FROM workout_sessions WHERE state = 'Active' LIMIT 1")
    suspend fun getActiveSession(): WorkoutSessionEntity?

    @Query("SELECT COUNT(*) FROM workout_sessions WHERE sourceRoutineId = :routineId AND state = 'Finished' AND archived = 0")
    suspend fun countFinishedRoutineSessions(routineId: Long): Int

    @Query("SELECT * FROM workout_exercises WHERE id = :id")
    suspend fun getWorkoutExercise(id: Long): WorkoutExerciseEntity?

    @Query("SELECT * FROM workout_exercises WHERE sessionId = :sessionId ORDER BY position, id")
    suspend fun getWorkoutExercises(sessionId: Long): List<WorkoutExerciseEntity>

    @Query("SELECT * FROM workout_sets WHERE id = :id")
    suspend fun getWorkoutSet(id: Long): WorkoutSetEntity?

    @Query(
        "SELECT * FROM workout_sets WHERE workoutExerciseId = :workoutExerciseId " +
            "ORDER BY position, id",
    )
    suspend fun getWorkoutSets(workoutExerciseId: Long): List<WorkoutSetEntity>

    @Query(
        "SELECT ws.* FROM workout_sets ws " +
            "INNER JOIN workout_exercises we ON we.id = ws.workoutExerciseId " +
            "INNER JOIN workout_sessions session ON session.id = we.sessionId " +
            "WHERE we.exerciseId = :exerciseId AND we.machineProfileUuidSnapshot IS :machineProfileUuidSnapshot " +
            "AND ws.completed = 1 " +
            "AND ws.deletedAtMillis IS NULL AND session.id != :excludingSessionId " +
            "ORDER BY session.startedAtMillis DESC, ws.position DESC LIMIT 1",
    )
    suspend fun getLatestCompletedSet(
        exerciseId: Long,
        excludingSessionId: Long,
        machineProfileUuidSnapshot: String?,
    ): WorkoutSetEntity?

    @Query("SELECT * FROM workout_exercises WHERE machineProfileUuidSnapshot = :scopeUuid")
    suspend fun getWorkoutExercisesForMachineScope(scopeUuid: String): List<WorkoutExerciseEntity>

    @Query(
        "SELECT COUNT(*) FROM workout_exercises we " +
            "INNER JOIN workout_sessions session ON session.id = we.sessionId " +
            "WHERE we.machineId = :machineId AND session.state = 'Active'",
    )
    suspend fun activeWorkoutMachinePlacementCount(machineId: Long): Int

    @Query("SELECT COUNT(*) FROM workout_exercises WHERE exerciseId = :exerciseId")
    suspend fun exerciseHistoryCount(exerciseId: Long): Int

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM exercises")
    suspend fun nextExercisePosition(): Int

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM exercise_categories")
    suspend fun nextCategoryPosition(): Int

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM workout_exercises WHERE sessionId = :sessionId")
    suspend fun nextWorkoutExercisePosition(sessionId: Long): Int

    @Query(
        "SELECT COALESCE(MAX(position), -1) + 1 FROM workout_sets " +
            "WHERE workoutExerciseId = :workoutExerciseId",
    )
    suspend fun nextSetPosition(workoutExerciseId: Long): Int

    @Insert suspend fun insertExercise(entity: ExerciseEntity): Long
    @Insert suspend fun insertMachine(entity: GymMachineEntity): Long
    @Insert suspend fun insertCategory(entity: ExerciseCategoryEntity): Long
    @Insert suspend fun insertSession(entity: WorkoutSessionEntity): Long
    @Insert suspend fun insertWorkoutExercise(entity: WorkoutExerciseEntity): Long
    @Insert suspend fun insertWorkoutSet(entity: WorkoutSetEntity): Long
    @Insert suspend fun insertWorkoutGroup(entity: WorkoutGroupEntity): Long

    @Update suspend fun updateExercise(entity: ExerciseEntity)
    @Update suspend fun updateMachine(entity: GymMachineEntity)
    @Update suspend fun updateCategory(entity: ExerciseCategoryEntity)
    @Update suspend fun updateSession(entity: WorkoutSessionEntity)
    @Update suspend fun updateWorkoutExercise(entity: WorkoutExerciseEntity)
    @Update suspend fun updateWorkoutSet(entity: WorkoutSetEntity)
    @Update suspend fun updateWorkoutGroup(entity: WorkoutGroupEntity)

    @Upsert suspend fun upsertCategoryJoin(entity: ExerciseCategoryJoinEntity)
    @Upsert suspend fun upsertMachineExerciseJoin(entity: GymMachineExerciseJoinEntity)

    @Query("DELETE FROM exercise_category_joins WHERE exerciseId = :exerciseId")
    suspend fun clearExerciseCategories(exerciseId: Long)

    @Query("DELETE FROM gym_machine_exercise_joins WHERE machineId = :machineId")
    suspend fun clearMachineExercises(machineId: Long)

    @Query("DELETE FROM workout_groups WHERE id = :id")
    suspend fun deleteWorkoutGroup(id: Long)

    @Query("DELETE FROM workout_exercises WHERE id = :id")
    suspend fun deleteWorkoutExercise(id: Long)

    @Query("DELETE FROM workout_exercises WHERE exerciseId = :exerciseId")
    suspend fun deleteWorkoutExercisesForExercise(exerciseId: Long): Int

    @Query("UPDATE workout_exercises SET machineId = NULL WHERE machineId = :machineId")
    suspend fun clearWorkoutMachineReferences(machineId: Long): Int

    @Query("DELETE FROM gym_machines WHERE id = :id")
    suspend fun deleteMachine(id: Long): Int

    @Query("DELETE FROM workout_sessions WHERE id = :id")
    suspend fun permanentlyDeleteSession(id: Long): Int

    @Query("UPDATE workout_sessions SET sourceRoutineId = NULL WHERE sourceRoutineId = :routineId")
    suspend fun clearSourceRoutine(routineId: Long): Int

    @Query("DELETE FROM exercises WHERE id = :id")
    suspend fun deleteExercise(id: Long): Int
}

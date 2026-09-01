package com.whip.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutineDao {
    @Query("SELECT * FROM gym_routines ORDER BY position, name")
    fun observeRoutines(): Flow<List<GymRoutineEntity>>

    @Query("SELECT * FROM routine_days ORDER BY routineId, position, id")
    fun observeDays(): Flow<List<RoutineDayEntity>>

    @Query("SELECT * FROM routine_exercises ORDER BY routineDayId, position, id")
    fun observeExercises(): Flow<List<RoutineExerciseEntity>>

    @Query("SELECT * FROM routine_sets ORDER BY routineExerciseId, position, id")
    fun observeSets(): Flow<List<RoutineSetEntity>>

    @Query("SELECT * FROM personal_records ORDER BY achievedAtMillis DESC")
    fun observePersonalRecords(): Flow<List<PersonalRecordEntity>>

    @Query("SELECT * FROM graph_presets ORDER BY createdAtMillis")
    fun observeGraphPresets(): Flow<List<GraphPresetEntity>>

    @Query("SELECT * FROM training_max_decisions ORDER BY createdAtMillis DESC, id DESC")
    fun observeTrainingMaxDecisions(): Flow<List<TrainingMaxDecisionEntity>>

    @Query("SELECT * FROM training_max_decisions ORDER BY createdAtMillis DESC, id DESC")
    suspend fun getAllTrainingMaxDecisions(): List<TrainingMaxDecisionEntity>

    @Query("SELECT * FROM graph_presets")
    suspend fun getGraphPresets(): List<GraphPresetEntity>

    @Query("SELECT * FROM personal_records")
    suspend fun getAllPersonalRecords(): List<PersonalRecordEntity>

    @Query("SELECT * FROM gym_routines WHERE id = :id")
    suspend fun getRoutine(id: Long): GymRoutineEntity?

    @Query("SELECT * FROM gym_routines")
    suspend fun getAllRoutines(): List<GymRoutineEntity>

    @Query("SELECT * FROM routine_days WHERE routineId = :routineId ORDER BY position, id")
    suspend fun getDays(routineId: Long): List<RoutineDayEntity>

    @Query("SELECT * FROM routine_days WHERE id = :dayId")
    suspend fun getDay(dayId: Long): RoutineDayEntity?

    @Query("SELECT * FROM routine_exercises WHERE routineDayId = :dayId ORDER BY position, id")
    suspend fun getExercises(dayId: Long): List<RoutineExerciseEntity>

    @Query("SELECT * FROM routine_exercises WHERE id = :id")
    suspend fun getExercise(id: Long): RoutineExerciseEntity?

    @Query("SELECT * FROM routine_exercises")
    suspend fun getAllExercises(): List<RoutineExerciseEntity>

    @Query("SELECT * FROM routine_sets WHERE routineExerciseId = :routineExerciseId ORDER BY position, id")
    suspend fun getSets(routineExerciseId: Long): List<RoutineSetEntity>

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM gym_routines")
    suspend fun nextRoutinePosition(): Int

    @Query(
        "SELECT ws.* FROM workout_sets ws " +
            "INNER JOIN workout_exercises we ON we.id = ws.workoutExerciseId " +
            "WHERE we.exerciseId = :exerciseId AND ws.completed = 1 " +
            "AND ws.deletedAtMillis IS NULL ORDER BY ws.completedAtMillis, ws.id",
    )
    suspend fun getCompletedSetsForExercise(exerciseId: Long): List<WorkoutSetEntity>

    @Insert suspend fun insertRoutine(entity: GymRoutineEntity): Long
    @Insert suspend fun insertDay(entity: RoutineDayEntity): Long
    @Insert suspend fun insertExercise(entity: RoutineExerciseEntity): Long
    @Insert suspend fun insertSet(entity: RoutineSetEntity): Long
    @Insert suspend fun insertGraphPreset(entity: GraphPresetEntity): Long
    @Insert suspend fun insertTrainingMaxDecision(entity: TrainingMaxDecisionEntity): Long

    @Update suspend fun updateRoutine(entity: GymRoutineEntity)

    @Update suspend fun updateDay(entity: RoutineDayEntity)

    @Update suspend fun updateExercise(entity: RoutineExerciseEntity)

    @Update suspend fun updateGraphPreset(entity: GraphPresetEntity)

    @Upsert suspend fun upsertPersonalRecord(entity: PersonalRecordEntity)

    @Query("DELETE FROM routine_days WHERE routineId = :routineId")
    suspend fun deleteDays(routineId: Long)

    @Query("DELETE FROM personal_records WHERE exerciseId = :exerciseId")
    suspend fun deleteRecords(exerciseId: Long)

    @Query("DELETE FROM routine_exercises WHERE exerciseId = :exerciseId")
    suspend fun deleteExercisesForExercise(exerciseId: Long): Int

    @Query(
        "UPDATE routine_exercises SET machineId = NULL, equipmentBindingState = 'NeedsEquipment', " +
            "updatedAtMillis = :updatedAtMillis WHERE machineId = :machineId",
    )
    suspend fun markMachineBindingsNeedsEquipment(machineId: Long, updatedAtMillis: Long): Int

    @Query("UPDATE personal_records SET machineId = NULL WHERE machineId = :machineId")
    suspend fun clearPersonalRecordMachineReferences(machineId: Long): Int

    @Query("DELETE FROM gym_routines WHERE id = :id")
    suspend fun deleteRoutine(id: Long): Int

    @Query("DELETE FROM graph_presets WHERE id = :id")
    suspend fun deleteGraphPreset(id: Long)
}

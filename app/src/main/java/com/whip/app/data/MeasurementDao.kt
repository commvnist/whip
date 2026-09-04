package com.whip.app.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface MeasurementDao {
    @Query("SELECT * FROM unit_definitions ORDER BY name")
    fun observeCustomUnits(): Flow<List<UnitDefinitionEntity>>

    @Query("SELECT * FROM measurement_definitions ORDER BY createdAtMillis")
    fun observeMeasurements(): Flow<List<MeasurementDefinitionEntity>>

    @Query("SELECT * FROM measurement_entries ORDER BY timestampMillis DESC")
    fun observeEntries(): Flow<List<MeasurementEntryEntity>>

    @Query("SELECT * FROM areas ORDER BY position, name")
    fun observeAreas(): Flow<List<AreaEntity>>

    @Query("SELECT * FROM areas ORDER BY position, name")
    suspend fun observeAreasSnapshot(): List<AreaEntity>

    @Query("SELECT * FROM tags ORDER BY name")
    fun observeTags(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags ORDER BY name")
    suspend fun getTagsSnapshot(): List<TagEntity>

    @Query("SELECT * FROM measurement_definitions WHERE id = :id")
    suspend fun getMeasurement(id: String): MeasurementDefinitionEntity?

    @Query("SELECT * FROM measurement_entries WHERE id = :id")
    suspend fun getEntry(id: String): MeasurementEntryEntity?

    @Query("SELECT * FROM unit_definitions WHERE id = :id")
    suspend fun getUnit(id: String): UnitDefinitionEntity?

    @Query("SELECT * FROM unit_definitions")
    suspend fun getAllUnits(): List<UnitDefinitionEntity>

    @Query("SELECT * FROM areas WHERE id = :id")
    suspend fun getArea(id: String): AreaEntity?

    @Query("SELECT * FROM areas WHERE nameKey = :nameKey LIMIT 1")
    suspend fun getAreaByNameKey(nameKey: String): AreaEntity?

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM areas")
    suspend fun nextAreaPosition(): Int

    @Query("SELECT * FROM tags WHERE id = :id")
    suspend fun getTag(id: String): TagEntity?

    @Query("SELECT * FROM measurement_entries")
    suspend fun getAllEntries(): List<MeasurementEntryEntity>

    @Query("SELECT * FROM measurement_entries WHERE measurementId = :measurementId")
    suspend fun getEntriesForMeasurement(measurementId: String): List<MeasurementEntryEntity>

    @Query("SELECT * FROM measurement_entries WHERE sourceType = :sourceType AND sourceId LIKE :sourcePrefix || '%'")
    suspend fun getEntriesBySourcePrefix(sourceType: String, sourcePrefix: String): List<MeasurementEntryEntity>

    @Query(
        "SELECT * FROM measurement_entries WHERE sourceType = :sourceType " +
            "AND sourceId LIKE :sourcePrefix || '%' " +
            "AND timestampMillis >= :startInclusiveMillis AND timestampMillis < :endExclusiveMillis",
    )
    suspend fun getEntriesBySourceWindow(
        sourceType: String,
        sourcePrefix: String,
        startInclusiveMillis: Long,
        endExclusiveMillis: Long,
    ): List<MeasurementEntryEntity>

    @Query("SELECT COUNT(*) FROM measurement_entries WHERE measurementId = :measurementId")
    suspend fun entryCount(measurementId: String): Int

    @Upsert suspend fun upsertUnit(entity: UnitDefinitionEntity)
    @Upsert suspend fun upsertMeasurement(entity: MeasurementDefinitionEntity)
    @Upsert suspend fun upsertEntry(entity: MeasurementEntryEntity)
    @Upsert suspend fun upsertArea(entity: AreaEntity)
    @Update suspend fun updateArea(entity: AreaEntity)
    @Upsert suspend fun upsertTag(entity: TagEntity)
    @Query("DELETE FROM measurement_entries WHERE id = :id")
    suspend fun deleteEntry(id: String)

    @Query("DELETE FROM measurement_entries WHERE sourceType = :sourceType")
    suspend fun deleteEntriesBySourceType(sourceType: String): Int

    @Query("DELETE FROM measurement_definitions WHERE id = :id")
    suspend fun deleteMeasurement(id: String): Int

    @Query("DELETE FROM areas WHERE id = :id")
    suspend fun deleteArea(id: String): Int

    @Query(
        "SELECT (SELECT COUNT(*) FROM tasks WHERE areaId = :areaId) + " +
            "(SELECT COUNT(*) FROM habits WHERE areaId = :areaId) + " +
            "(SELECT COUNT(*) FROM goals WHERE areaId = :areaId)",
    )
    suspend fun countAreaAssignments(areaId: String): Int

    @Query("UPDATE tasks SET areaId = NULL, area = '', updatedAtMillis = :now WHERE areaId = :areaId")
    suspend fun clearTaskAreaReferences(areaId: String, now: Long): Int

    @Query("UPDATE habits SET areaId = NULL, area = '', updatedAtMillis = :now WHERE areaId = :areaId")
    suspend fun clearHabitAreaReferences(areaId: String, now: Long): Int

    @Query("UPDATE goals SET areaId = NULL, area = '', updatedAtMillis = :now WHERE areaId = :areaId")
    suspend fun clearGoalAreaReferences(areaId: String, now: Long): Int

    @Query("UPDATE tasks SET areaId = :targetId, area = :targetName, updatedAtMillis = :now WHERE areaId = :sourceId")
    suspend fun moveTaskAreaReferences(sourceId: String, targetId: String, targetName: String, now: Long)

    @Query("UPDATE habits SET areaId = :targetId, area = :targetName, updatedAtMillis = :now WHERE areaId = :sourceId")
    suspend fun moveHabitAreaReferences(sourceId: String, targetId: String, targetName: String, now: Long)

    @Query("UPDATE goals SET areaId = :targetId, area = :targetName, updatedAtMillis = :now WHERE areaId = :sourceId")
    suspend fun moveGoalAreaReferences(sourceId: String, targetId: String, targetName: String, now: Long)

    @Query("UPDATE tasks SET areaId = :targetId, area = :targetName, updatedAtMillis = :now WHERE (:sourceId IS NULL AND areaId IS NULL) OR areaId = :sourceId")
    suspend fun reassignAllTaskAreas(sourceId: String?, targetId: String?, targetName: String, now: Long): Int

    @Query("UPDATE habits SET areaId = :targetId, area = :targetName, updatedAtMillis = :now WHERE (:sourceId IS NULL AND areaId IS NULL) OR areaId = :sourceId")
    suspend fun reassignAllHabitAreas(sourceId: String?, targetId: String?, targetName: String, now: Long): Int

    @Query("UPDATE goals SET areaId = :targetId, area = :targetName, updatedAtMillis = :now WHERE (:sourceId IS NULL AND areaId IS NULL) OR areaId = :sourceId")
    suspend fun reassignAllGoalAreas(sourceId: String?, targetId: String?, targetName: String, now: Long): Int

    @Query("UPDATE tasks SET area = :name, updatedAtMillis = :now WHERE areaId = :areaId")
    suspend fun updateTaskAreaNames(areaId: String, name: String, now: Long)

    @Query("UPDATE habits SET area = :name, updatedAtMillis = :now WHERE areaId = :areaId")
    suspend fun updateHabitAreaNames(areaId: String, name: String, now: Long)

    @Query("UPDATE goals SET area = :name, updatedAtMillis = :now WHERE areaId = :areaId")
    suspend fun updateGoalAreaNames(areaId: String, name: String, now: Long)

    @Query("DELETE FROM tags WHERE id = :id")
    suspend fun deleteTag(id: String): Int
}

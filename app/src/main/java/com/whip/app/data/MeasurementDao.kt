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

    @Query("SELECT * FROM metric_definitions ORDER BY createdAtMillis")
    fun observeMetrics(): Flow<List<MetricDefinitionEntity>>

    @Query("SELECT * FROM metric_entries ORDER BY timestampMillis DESC")
    fun observeEntries(): Flow<List<MetricEntryEntity>>

    @Query("SELECT * FROM areas ORDER BY position, name")
    fun observeAreas(): Flow<List<AreaEntity>>

    @Query("SELECT * FROM areas ORDER BY position, name")
    suspend fun observeAreasSnapshot(): List<AreaEntity>

    @Query("SELECT * FROM tags ORDER BY name")
    fun observeTags(): Flow<List<TagEntity>>

    @Query("SELECT * FROM metric_definitions WHERE id = :id")
    suspend fun getMetric(id: String): MetricDefinitionEntity?

    @Query("SELECT * FROM metric_entries WHERE id = :id")
    suspend fun getEntry(id: String): MetricEntryEntity?

    @Query("SELECT * FROM unit_definitions WHERE id = :id")
    suspend fun getUnit(id: String): UnitDefinitionEntity?

    @Query("SELECT * FROM areas WHERE id = :id")
    suspend fun getArea(id: String): AreaEntity?

    @Query("SELECT * FROM areas WHERE nameKey = :nameKey LIMIT 1")
    suspend fun getAreaByNameKey(nameKey: String): AreaEntity?

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM areas")
    suspend fun nextAreaPosition(): Int

    @Query("SELECT * FROM tags WHERE id = :id")
    suspend fun getTag(id: String): TagEntity?

    @Query("SELECT * FROM metric_entries")
    suspend fun getAllEntries(): List<MetricEntryEntity>

    @Query("SELECT * FROM metric_entries WHERE metricId = :metricId")
    suspend fun getEntriesForMetric(metricId: String): List<MetricEntryEntity>

    @Query("SELECT * FROM metric_entries WHERE sourceType = :sourceType AND sourceId LIKE :sourcePrefix || '%'")
    suspend fun getEntriesBySourcePrefix(sourceType: String, sourcePrefix: String): List<MetricEntryEntity>

    @Query("SELECT COUNT(*) FROM metric_entries WHERE metricId = :metricId")
    suspend fun entryCount(metricId: String): Int

    @Upsert suspend fun upsertUnit(entity: UnitDefinitionEntity)
    @Upsert suspend fun upsertMetric(entity: MetricDefinitionEntity)
    @Upsert suspend fun upsertEntry(entity: MetricEntryEntity)
    @Upsert suspend fun upsertArea(entity: AreaEntity)
    @Update suspend fun updateArea(entity: AreaEntity)
    @Upsert suspend fun upsertTag(entity: TagEntity)
    @Query("DELETE FROM metric_entries WHERE id = :id")
    suspend fun deleteEntry(id: String)

    @Query("DELETE FROM metric_definitions WHERE id = :id")
    suspend fun deleteMetric(id: String): Int

    @Query("DELETE FROM areas WHERE id = :id")
    suspend fun deleteArea(id: String): Int

    @Query(
        "SELECT (SELECT COUNT(*) FROM tasks WHERE areaId = :areaId) + " +
            "(SELECT COUNT(*) FROM habits WHERE areaId = :areaId) + " +
            "(SELECT COUNT(*) FROM goals WHERE areaId = :areaId)",
    )
    suspend fun countAreaAssignments(areaId: String): Int

    @Query("UPDATE tasks SET areaId = NULL, area = '' WHERE areaId = :areaId")
    suspend fun clearTaskAreaReferences(areaId: String): Int

    @Query("UPDATE habits SET areaId = NULL, area = '' WHERE areaId = :areaId")
    suspend fun clearHabitAreaReferences(areaId: String): Int

    @Query("UPDATE goals SET areaId = NULL, area = '' WHERE areaId = :areaId")
    suspend fun clearGoalAreaReferences(areaId: String): Int

    @Query("UPDATE tasks SET areaId = :targetId, area = :targetName WHERE areaId = :sourceId")
    suspend fun moveTaskAreaReferences(sourceId: String, targetId: String, targetName: String)

    @Query("UPDATE habits SET areaId = :targetId, area = :targetName WHERE areaId = :sourceId")
    suspend fun moveHabitAreaReferences(sourceId: String, targetId: String, targetName: String)

    @Query("UPDATE goals SET areaId = :targetId, area = :targetName WHERE areaId = :sourceId")
    suspend fun moveGoalAreaReferences(sourceId: String, targetId: String, targetName: String)

    @Query("UPDATE tasks SET areaId = :targetId, area = :targetName WHERE (:sourceId IS NULL AND areaId IS NULL) OR areaId = :sourceId")
    suspend fun reassignAllTaskAreas(sourceId: String?, targetId: String?, targetName: String): Int

    @Query("UPDATE habits SET areaId = :targetId, area = :targetName WHERE (:sourceId IS NULL AND areaId IS NULL) OR areaId = :sourceId")
    suspend fun reassignAllHabitAreas(sourceId: String?, targetId: String?, targetName: String): Int

    @Query("UPDATE goals SET areaId = :targetId, area = :targetName WHERE (:sourceId IS NULL AND areaId IS NULL) OR areaId = :sourceId")
    suspend fun reassignAllGoalAreas(sourceId: String?, targetId: String?, targetName: String): Int

    @Query("UPDATE tasks SET area = :name WHERE areaId = :areaId")
    suspend fun updateTaskAreaNames(areaId: String, name: String)

    @Query("UPDATE habits SET area = :name WHERE areaId = :areaId")
    suspend fun updateHabitAreaNames(areaId: String, name: String)

    @Query("UPDATE goals SET area = :name WHERE areaId = :areaId")
    suspend fun updateGoalAreaNames(areaId: String, name: String)

    @Query("DELETE FROM tags WHERE id = :id")
    suspend fun deleteTag(id: String): Int
}

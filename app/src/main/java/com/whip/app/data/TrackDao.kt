package com.whip.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import androidx.room.OnConflictStrategy
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackDao {
    @Query("SELECT * FROM tracks ORDER BY pinned DESC, position, name")
    fun observeTracks(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM track_fields ORDER BY trackId, position")
    fun observeFields(): Flow<List<TrackFieldEntity>>

    @Query("SELECT * FROM track_choice_options ORDER BY fieldId, position")
    fun observeOptions(): Flow<List<TrackChoiceOptionEntity>>

    @Query("SELECT * FROM track_entries ORDER BY entryEpochDay DESC, createdAtMillis DESC, id DESC")
    fun observeEntries(): Flow<List<TrackEntryEntity>>

    @Query("SELECT * FROM track_values ORDER BY entryId, fieldId")
    fun observeValues(): Flow<List<TrackValueEntity>>

    @Query("SELECT * FROM tracks WHERE id = :id")
    suspend fun getTrack(id: Long): TrackEntity?

    @Query("SELECT * FROM tracks")
    suspend fun getAllTracks(): List<TrackEntity>

    @Query("SELECT * FROM track_fields WHERE id = :id")
    suspend fun getField(id: Long): TrackFieldEntity?

    @Query("SELECT * FROM track_fields WHERE uuid = :uuid")
    suspend fun getFieldByUuid(uuid: String): TrackFieldEntity?

    @Query("SELECT * FROM track_fields WHERE trackId = :trackId ORDER BY position")
    suspend fun getFields(trackId: Long): List<TrackFieldEntity>

    @Query("SELECT * FROM track_choice_options WHERE id = :id")
    suspend fun getOption(id: Long): TrackChoiceOptionEntity?

    @Query("SELECT * FROM track_choice_options WHERE uuid = :uuid")
    suspend fun getOptionByUuid(uuid: String): TrackChoiceOptionEntity?

    @Query("SELECT * FROM track_choice_options WHERE fieldId = :fieldId ORDER BY position")
    suspend fun getOptions(fieldId: Long): List<TrackChoiceOptionEntity>

    @Query("SELECT * FROM track_choice_options WHERE fieldId IN (:fieldIds) ORDER BY fieldId, position")
    suspend fun getOptionsForFields(fieldIds: List<Long>): List<TrackChoiceOptionEntity>

    @Query("SELECT * FROM track_entries WHERE id = :id")
    suspend fun getEntry(id: Long): TrackEntryEntity?

    @Query("SELECT * FROM track_entries WHERE uuid = :uuid")
    suspend fun getEntryByUuid(uuid: String): TrackEntryEntity?

    @Query("SELECT * FROM track_entries WHERE sourceOccurrenceId IN (:occurrenceIds)")
    suspend fun getEntriesForSourceOccurrences(occurrenceIds: List<Long>): List<TrackEntryEntity>

    @Query("SELECT * FROM track_entries WHERE trackId = :trackId ORDER BY entryEpochDay DESC, createdAtMillis DESC, id DESC")
    suspend fun getEntries(trackId: Long): List<TrackEntryEntity>

    @Query("SELECT COUNT(*) FROM track_entries WHERE trackId = :trackId")
    suspend fun countEntries(trackId: Long): Int

    @Query("SELECT * FROM track_entries WHERE trackId = :trackId ORDER BY entryEpochDay DESC, createdAtMillis DESC, id DESC LIMIT :limit OFFSET :offset")
    suspend fun getEntryPage(trackId: Long, offset: Int, limit: Int): List<TrackEntryEntity>

    @Query("SELECT * FROM track_values WHERE entryId = :entryId")
    suspend fun getValues(entryId: Long): List<TrackValueEntity>

    @Query("SELECT * FROM track_values WHERE uuid = :uuid")
    suspend fun getValueByUuid(uuid: String): TrackValueEntity?

    @Query("SELECT * FROM track_values WHERE entryId IN (:entryIds)")
    suspend fun getValuesForEntries(entryIds: List<Long>): List<TrackValueEntity>

    @Query("SELECT * FROM track_values WHERE fieldId = :fieldId ORDER BY id")
    suspend fun getValuesForField(fieldId: Long): List<TrackValueEntity>

    @Query("SELECT * FROM track_values WHERE choiceOptionId IN (:optionIds) ORDER BY id")
    suspend fun getValuesForOptions(optionIds: List<Long>): List<TrackValueEntity>

    @Query("SELECT COUNT(*) FROM track_values WHERE fieldId = :fieldId")
    suspend fun countValuesForField(fieldId: Long): Int

    @Query("SELECT COUNT(*) FROM track_values WHERE choiceOptionId = :optionId")
    suspend fun countValuesForOption(optionId: Long): Int

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM tracks")
    suspend fun nextTrackPosition(): Int

    @Insert suspend fun insertTrack(entity: TrackEntity): Long
    @Update suspend fun updateTrack(entity: TrackEntity)
    @Insert suspend fun insertField(entity: TrackFieldEntity): Long
    @Update suspend fun updateField(entity: TrackFieldEntity)

    @Insert suspend fun insertOption(entity: TrackChoiceOptionEntity): Long
    @Update suspend fun updateOption(entity: TrackChoiceOptionEntity)
    @Insert suspend fun insertEntry(entity: TrackEntryEntity): Long
    @Update suspend fun updateEntry(entity: TrackEntryEntity): Int
    @Upsert suspend fun upsertValue(entity: TrackValueEntity): Long

    @Query("DELETE FROM track_values WHERE id = :id")
    suspend fun deleteValue(id: Long): Int

    @Query("DELETE FROM track_values WHERE entryId = :entryId AND fieldId NOT IN (:retainedFieldIds)")
    suspend fun deleteValuesOutsideFields(entryId: Long, retainedFieldIds: List<Long>): Int

    @Query("DELETE FROM track_values WHERE entryId = :entryId")
    suspend fun deleteValues(entryId: Long): Int

    @Query("DELETE FROM track_values WHERE choiceOptionId = :optionId")
    suspend fun deleteValuesForOption(optionId: Long): Int

    @Query("UPDATE track_values SET choiceOptionId = :replacementId, updatedAtMillis = :now WHERE choiceOptionId = :removedId")
    suspend fun replaceChoiceOptionValues(removedId: Long, replacementId: Long, now: Long): Int

    @Query("DELETE FROM track_choice_options WHERE id = :id")
    suspend fun deleteOption(id: Long): Int

    @Query("DELETE FROM track_fields WHERE id = :id")
    suspend fun deleteField(id: Long): Int

    @Query("DELETE FROM track_entries WHERE id = :id")
    suspend fun deleteEntry(id: Long): Int

    @Query("UPDATE track_entries SET sourceOccurrenceId = NULL WHERE sourceOccurrenceId = :occurrenceId")
    suspend fun clearSourceOccurrence(occurrenceId: Long): Int

    @Query("DELETE FROM tracks WHERE id = :id")
    suspend fun deleteTrack(id: Long): Int

    @Query("UPDATE tracks SET areaId = :targetId, area = :targetName, updatedAtMillis = :now WHERE areaId = :sourceId")
    suspend fun moveAreaReferences(sourceId: String, targetId: String, targetName: String, now: Long): Int

    @Query("UPDATE tracks SET areaId = :targetId, area = :targetName, updatedAtMillis = :now WHERE (:sourceId IS NULL AND areaId IS NULL) OR areaId = :sourceId")
    suspend fun reassignAllAreas(sourceId: String?, targetId: String, targetName: String, now: Long): Int

    @Query("UPDATE tracks SET area = :name, updatedAtMillis = :now WHERE areaId = :areaId")
    suspend fun updateAreaNames(areaId: String, name: String, now: Long): Int

    @Query("SELECT COUNT(*) FROM tracks WHERE areaId = :areaId")
    suspend fun countAreaAssignments(areaId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSearch(entity: TrackEntrySearchEntity)

    @Query("DELETE FROM track_entry_search WHERE rowid = :entryId")
    suspend fun deleteSearch(entryId: Long): Int

    @Query("DELETE FROM track_entry_search WHERE trackId = :trackId")
    suspend fun deleteSearchForTrack(trackId: Long): Int

    @Query("SELECT rowid FROM track_entry_search WHERE trackId = :trackId AND track_entry_search MATCH :query ORDER BY rowid DESC")
    suspend fun searchEntryIds(trackId: Long, query: String): List<Long>

    @Query("SELECT rowid FROM track_entry_search WHERE trackId = :trackId AND content LIKE '%' || :token || '%' ORDER BY rowid DESC")
    suspend fun searchEntryIdsContaining(trackId: Long, token: String): List<Long>
}

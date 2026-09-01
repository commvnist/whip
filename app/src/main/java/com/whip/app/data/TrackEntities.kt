package com.whip.app.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Fts4
import androidx.room.ColumnInfo

@Entity(
    tableName = "tracks",
    foreignKeys = [
        ForeignKey(
            entity = AreaEntity::class,
            parentColumns = ["id"],
            childColumns = ["areaId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index("uuid", unique = true),
        Index("areaId"),
        Index("archived"),
        Index("pinned"),
    ],
)
data class TrackEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uuid: String,
    val name: String,
    val description: String,
    val icon: String,
    val areaId: String,
    val area: String,
    val tagsCsv: String,
    val pinned: Boolean,
    val archived: Boolean,
    val position: Int,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
)

@Entity(
    tableName = "track_fields",
    foreignKeys = [
        ForeignKey(
            entity = TrackEntity::class,
            parentColumns = ["id"],
            childColumns = ["trackId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("uuid", unique = true),
        Index(value = ["trackId", "position"]),
        Index(value = ["trackId", "primaryField"]),
    ],
)
data class TrackFieldEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uuid: String,
    val trackId: Long,
    val name: String,
    val type: String,
    val position: Int,
    val required: Boolean,
    val primaryField: Boolean,
    val showInList: Boolean,
    val dimension: String?,
    val unitId: String?,
    val precision: Int,
    val scaleMin: Int?,
    val scaleMax: Int?,
    val scaleLowLabel: String,
    val scaleHighLabel: String,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val scaleStep: Double = 1.0,
)

@Entity(
    tableName = "track_choice_options",
    foreignKeys = [
        ForeignKey(
            entity = TrackFieldEntity::class,
            parentColumns = ["id"],
            childColumns = ["fieldId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("uuid", unique = true),
        Index(value = ["fieldId", "position"]),
    ],
)
data class TrackChoiceOptionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uuid: String,
    val fieldId: Long,
    val label: String,
    val position: Int,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
)

@Entity(
    tableName = "track_entries",
    foreignKeys = [
        ForeignKey(
            entity = TrackEntity::class,
            parentColumns = ["id"],
            childColumns = ["trackId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("uuid", unique = true),
        Index(value = ["trackId", "entryEpochDay"]),
        Index("sourceOccurrenceId", unique = true),
    ],
)
data class TrackEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uuid: String,
    val trackId: Long,
    val entryEpochDay: Long,
    val sourceOccurrenceId: Long?,
    val sourceExplanation: String,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
)

@Entity(
    tableName = "track_values",
    foreignKeys = [
        ForeignKey(
            entity = TrackEntryEntity::class,
            parentColumns = ["id"],
            childColumns = ["entryId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = TrackFieldEntity::class,
            parentColumns = ["id"],
            childColumns = ["fieldId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = TrackChoiceOptionEntity::class,
            parentColumns = ["id"],
            childColumns = ["choiceOptionId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index("uuid", unique = true),
        Index(value = ["entryId", "fieldId"], unique = true),
        Index("fieldId"),
        Index("choiceOptionId"),
        Index(value = ["fieldId", "canonicalNumber"]),
        Index(value = ["fieldId", "dateEpochDay"]),
    ],
)
data class TrackValueEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uuid: String,
    val entryId: Long,
    val fieldId: Long,
    val textValue: String?,
    val enteredNumber: Double?,
    val canonicalNumber: Double?,
    val enteredUnitId: String?,
    val dateEpochDay: Long?,
    val booleanValue: Boolean?,
    val choiceOptionId: Long?,
    val scaleValue: Double?,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
)

/** Rebuildable full-text projection. Track tables remain the source of truth. */
@Fts4
@Entity(tableName = "track_entry_search")
data class TrackEntrySearchEntity(
    @PrimaryKey @ColumnInfo(name = "rowid") val rowId: Long,
    val trackId: Long,
    val content: String,
)

/**
 * Private commit receipt for process-death-safe CSV import retry. It stores no
 * URI, header, mapping, or imported values; those are represented only by
 * versioned digests.
 */
@Entity(
    tableName = "track_csv_import_receipts",
    foreignKeys = [
        ForeignKey(
            entity = TrackEntity::class,
            parentColumns = ["id"],
            childColumns = ["trackId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("trackId")],
)
data class TrackCsvImportReceiptEntity(
    @PrimaryKey val batchUuid: String,
    val trackId: Long,
    val trackUuid: String,
    val trackCreatedAtMillis: Long,
    val requestFingerprint: String,
    val fingerprintVersion: Int,
    val entryIdentityDigest: String,
    val rowCount: Int,
    val identityVersion: Int,
    val committedAtMillis: Long,
)

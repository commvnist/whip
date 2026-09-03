package com.whip.app.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.ColumnInfo

@Entity(tableName = "unit_definitions", indices = [Index("archived")])
data class UnitDefinitionEntity(
    @androidx.room.PrimaryKey val id: String,
    val name: String,
    val symbol: String,
    val dimension: String,
    val toCanonicalFactor: Double,
    val toCanonicalOffset: Double,
    val custom: Boolean,
    @ColumnInfo(defaultValue = "0") val archived: Boolean = false,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
)

@Entity(tableName = "metric_definitions", indices = [Index("archived")])
data class MetricDefinitionEntity(
    @androidx.room.PrimaryKey val id: String,
    val name: String,
    val valueKind: String,
    val dimension: String,
    val defaultUnitId: String,
    val precision: Int,
    val dimensionLocked: Boolean,
    val archived: Boolean,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
)

@Entity(
    tableName = "metric_entries",
    foreignKeys = [
        ForeignKey(
            entity = MetricDefinitionEntity::class,
            parentColumns = ["id"],
            childColumns = ["metricId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("metricId"),
        Index(value = ["metricId", "timestampMillis"]),
        Index(value = ["sourceType", "sourceId"]),
    ],
)
data class MetricEntryEntity(
    @androidx.room.PrimaryKey val id: String,
    val metricId: String,
    val canonicalValue: Double?,
    val enteredValue: Double?,
    val enteredUnitId: String?,
    val status: String,
    val timestampMillis: Long,
    val localEpochDay: Long,
    val zoneId: String,
    val offsetSeconds: Int,
    val sourceType: String,
    val sourceId: String?,
    val note: String,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
)

@Entity(
    tableName = "areas",
    indices = [Index("nameKey", unique = true), Index("archived")],
)
data class AreaEntity(
    @androidx.room.PrimaryKey val id: String,
    val name: String,
    @ColumnInfo(defaultValue = "''") val nameKey: String,
    val colorArgb: Long?,
    val position: Int,
    val archived: Boolean,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
)

@Entity(tableName = "tags", indices = [Index("name", unique = true), Index("archived")])
data class TagEntity(
    @androidx.room.PrimaryKey val id: String,
    val name: String,
    val archived: Boolean,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
)

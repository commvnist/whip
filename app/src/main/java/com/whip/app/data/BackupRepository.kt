package com.whip.app.data

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.util.Base64
import androidx.room.withTransaction
import com.whip.app.core.AppSettings
import com.whip.app.core.AppThemeMode
import com.whip.app.core.AreaOpeningMode
import com.whip.app.core.HealthDataType
import com.whip.app.core.HomeSection
import com.whip.app.core.ReviewSection
import com.whip.app.core.ReviewPeriod
import com.whip.app.core.SavedTaskFilter
import com.whip.app.core.normalizedNavigation
import com.whip.app.core.PlatePreset
import com.whip.app.core.RepPrescriptionScheme
import com.whip.app.core.TrackedGymRecord
import com.whip.app.core.SettingsRepository
import com.whip.app.core.SystemWhipClock
import com.whip.app.core.UuidWhipIdGenerator
import com.whip.app.core.DEFAULT_REST_TIMER_PRESET_SECONDS
import com.whip.app.core.normalizeRestTimerPresets
import com.whip.app.core.normalized
import com.whip.app.domain.RepeatStepPolicy
import com.whip.app.domain.TaskPriority
import com.whip.app.domain.TaskEffort
import com.whip.app.domain.CustomIdentityEmoji
import com.whip.app.domain.PersonalRecordType
import com.whip.app.domain.BuiltInUnits
import com.whip.app.domain.LoadInterpretation
import com.whip.app.domain.MachineLevelDirection
import com.whip.app.domain.MachineStackMode
import com.whip.app.domain.HabitTrackingMode
import com.whip.app.domain.UnitDimension
import com.whip.app.domain.canonicalResistanceKg
import java.security.MessageDigest
import java.time.DayOfWeek
import java.time.Instant
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject

data class BackupPreview(
    val envelopeVersion: Int,
    val dataModelEpoch: Int = CURRENT_DATA_MODEL_EPOCH,
    val databaseVersion: Int,
    val exportedAt: Instant,
    val tableCounts: Map<String, Int>,
    val totalRecords: Int,
    val duplicateStableIds: Int,
    val checksumValid: Boolean,
    val settingsIncluded: Boolean,
    val restoreCompatible: Boolean = databaseVersion == BACKUP_DATABASE_VERSION,
    val compatibilityMessage: String? = null,
)

data class BackupMergeSummary(
    val importedRecords: Int,
    val skippedExistingRecords: Int,
    val settingsKept: Boolean = true,
)

interface BackupRepository {
    suspend fun exportBackup(): String
    /** App-private rollback snapshot; may include local recovery journals omitted from portable exports. */
    suspend fun exportRecoveryBackup(): String = exportBackup()
    suspend fun previewBackup(json: String): BackupPreview
    suspend fun restoreBackup(json: String)
    suspend fun mergeBackup(json: String): BackupMergeSummary
    suspend fun exportTasksCsv(): String
    suspend fun exportHabitsCsv(): String
    suspend fun exportGoalsCsv(): String
    suspend fun exportGymCsv(): String
    suspend fun exportTracksCsv(): String
    suspend fun deleteAllData()
}

class RoomBackupRepository(
    private val database: WhipDatabase,
    private val settingsRepository: SettingsRepository? = null,
    private val areaRepository: AreaRepository? = null,
) : BackupRepository {
    override suspend fun exportBackup(): String = exportBackup(includeLocalRecoveryState = false)

    override suspend fun exportRecoveryBackup(): String = exportBackup(includeLocalRecoveryState = true)

    private suspend fun exportBackup(includeLocalRecoveryState: Boolean): String = database.withTransaction {
        val db = database.openHelper.readableDatabase
        val tables = JSONObject()
        EXPORT_TABLES.forEach { table ->
            val rows = JSONArray()
            db.query("SELECT * FROM ${safeIdentifier(table)}").use { cursor ->
                while (cursor.moveToNext()) rows.put(cursor.toJsonRow())
            }
            tables.put(table, rows)
        }
        if (!includeLocalRecoveryState) sanitizeHabitTimersForPortableBackup(tables)
        val settings = settingsRepository?.current()?.toJson(includeLocalRecoveryState)
        val payload = checksumPayload(tables, settings)
        JSONObject()
            .put("format", BACKUP_FORMAT)
            .put("envelopeVersion", ENVELOPE_VERSION)
            .put("dataModelEpoch", CURRENT_DATA_MODEL_EPOCH)
            .put("databaseVersion", BACKUP_DATABASE_VERSION)
            .put("exportedAt", Instant.now().toString())
            .put("checksumSha256", sha256(payload))
            .put("tables", tables)
            .apply { settings?.let { put("settings", it) } }
            .toString(2)
    }

    override suspend fun previewBackup(json: String): BackupPreview {
        val root = parseAndValidate(json)
        val tables = root.getJSONObject("tables")
        val settings = root.optJSONObject("settings")
        val checksumValid = root.getString("checksumSha256") == sha256(checksumPayload(tables, settings))
        if (checksumValid) {
            validateBackupUnitDefinitions(tables)
        }
        val counts = EXPORT_TABLES.associateWith { table -> tables.optJSONArray(table)?.length() ?: 0 }
        var duplicates = 0
        val db = database.openHelper.readableDatabase
        EXPORT_TABLES.forEach { table ->
            val array = tables.optJSONArray(table) ?: return@forEach
            for (index in 0 until array.length()) {
                val row = array.getJSONObject(index)
                val key = when {
                    row.has("uuid") && !row.isNull("uuid") -> "uuid" to row.getString("uuid")
                    row.has("id") && row.opt("id") is String -> "id" to row.getString("id")
                    else -> null
                } ?: continue
                db.query("SELECT COUNT(*) FROM ${safeIdentifier(table)} WHERE ${safeIdentifier(key.first)} = ?", arrayOf(key.second)).use { cursor ->
                    if (cursor.moveToFirst() && cursor.getInt(0) > 0) duplicates++
                }
            }
        }
        val envelopeVersion = root.getInt("envelopeVersion")
        val databaseVersion = root.getInt("databaseVersion")
        return BackupPreview(
            envelopeVersion = envelopeVersion,
            dataModelEpoch = root.getInt("dataModelEpoch"),
            databaseVersion = databaseVersion,
            exportedAt = Instant.parse(root.getString("exportedAt")),
            tableCounts = counts,
            totalRecords = counts.values.sum(),
            duplicateStableIds = duplicates,
            checksumValid = checksumValid,
            settingsIncluded = settings != null,
            restoreCompatible = true,
            compatibilityMessage = null,
        )
    }

    override suspend fun restoreBackup(json: String) {
        val root = parseAndValidate(json)
        val tables = root.getJSONObject("tables")
        val settings = root.optJSONObject("settings")
        require(
            root.getString("checksumSha256") == sha256(
                checksumPayload(tables, settings),
            ),
        ) { "Backup checksum does not match" }
        validateBackupUnitDefinitions(tables)
        database.withTransaction {
            val db = database.openHelper.writableDatabase
            EXPORT_TABLES.asReversed().forEach { table -> db.execSQL("DELETE FROM ${safeIdentifier(table)}") }
            EXPORT_TABLES.forEach { table ->
                val rows = tables.getJSONArray(table)
                for (index in 0 until rows.length()) {
                    val values = rows.getJSONObject(index).toContentValues()
                    val result = db.insert(safeIdentifier(table), SQLiteDatabase.CONFLICT_ABORT, values)
                    require(result != -1L) { "Could not restore $table row ${index + 1}" }
                }
            }
        }
        settings?.let { restored ->
            settingsRepository?.let { repository ->
                check(repository.updateAndConfirm { restored.toAppSettings() }) {
                    "Local storage did not confirm the restored settings. Whip will roll back this restore."
                }
            }
        }
        areaRepository?.ensureDefaultArea()
        RoomTrackRepository(database, SystemWhipClock, UuidWhipIdGenerator).rebuildSearchIndex()
    }

    override suspend fun mergeBackup(json: String): BackupMergeSummary {
        val root = parseAndValidate(json)
        val tables = root.getJSONObject("tables")
        val settings = root.optJSONObject("settings")
        require(
            root.getString("checksumSha256") == sha256(
                checksumPayload(tables, settings),
            ),
        ) { "Backup checksum does not match" }
        validateBackupUnitDefinitions(tables)
        sanitizeHabitTimersForMerge(tables)
        val summary = database.withTransaction {
            val db = database.openHelper.writableDatabase
            val idMaps = mutableMapOf<String, MutableMap<Long, Long>>()
            val areaIdMap = mutableMapOf<String, String>()
            val pendingEntryOccurrenceLinks = mutableListOf<Pair<Long, Long>>()
            var imported = 0
            var skipped = 0
            EXPORT_TABLES.forEach { table ->
                val rows = tables.getJSONArray(table)
                val metadata = mergeTableMetadata(db, table)
                for (index in 0 until rows.length()) {
                    val row = rows.getJSONObject(index)
                    if (table == "areas") {
                        val sourceAreaId = row.optString("id")
                        val name = row.optString("name").trim()
                        val key = row.optString("nameKey").ifBlank { name.lowercase() }
                        val existingAreaId = db.query(
                            "SELECT id FROM areas WHERE id = ? OR nameKey = ? LIMIT 1",
                            arrayOf(sourceAreaId, key),
                        ).use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
                        if (existingAreaId != null) {
                            areaIdMap[sourceAreaId] = existingAreaId
                            skipped++
                            continue
                        }
                    }
                    val sourceNumericId = row.optLongOrNull("id")
                    val stable = row.stableMergeKey(metadata)
                    val existing = stable?.let { (column, value) ->
                        db.query(
                            "SELECT ${if (metadata.autoNumericId) "id" else safeIdentifier(column)} FROM ${safeIdentifier(table)} WHERE ${safeIdentifier(column)} = ? LIMIT 1",
                            arrayOf(value),
                        ).use { cursor ->
                            if (!cursor.moveToFirst()) null
                            else ExistingMergeRow(if (metadata.autoNumericId) cursor.getLong(0) else null)
                        }
                    }
                    if (existing != null) {
                        if (table == "unit_definitions") {
                            requireMatchingLiveUnitContract(db, row)
                        }
                        existing.numericId?.let { target -> sourceNumericId?.let { source -> idMaps.getOrPut(table, ::mutableMapOf)[source] = target } }
                        skipped++
                        continue
                    }
                    val values = row.toContentValues()
                    val sourceOccurrenceId = if (table == "track_entries") {
                        values.getAsLong("sourceOccurrenceId")?.also { values.putNull("sourceOccurrenceId") }
                    } else {
                        null
                    }
                    if (table in setOf("tasks", "habits", "goals", "tracks")) {
                        values.getAsString("areaId")?.let { sourceId -> values.put("areaId", areaIdMap[sourceId] ?: sourceId) }
                    }
                    remapForeignKeys(values, metadata, idMaps)
                    remapPolymorphicReferences(table, values, idMaps)
                    if (metadata.autoNumericId) values.remove("id")
                    val result = db.insert(safeIdentifier(table), SQLiteDatabase.CONFLICT_IGNORE, values)
                    if (result == -1L) {
                        if (metadata.autoNumericId && sourceNumericId != null) {
                            existingNaturalChildId(db, table, values)?.let { targetId ->
                                idMaps.getOrPut(table, ::mutableMapOf)[sourceNumericId] = targetId
                            }
                        }
                        skipped++
                    } else {
                        imported++
                        if (table == "areas") row.optString("id").takeIf(String::isNotBlank)?.let { areaIdMap[it] = it }
                        if (metadata.autoNumericId && sourceNumericId != null) {
                            idMaps.getOrPut(table, ::mutableMapOf)[sourceNumericId] = result
                        }
                        if (table == "track_entries" && sourceOccurrenceId != null) {
                            pendingEntryOccurrenceLinks += result to sourceOccurrenceId
                        }
                    }
                }
            }
            val sourceOccurrences = tables.getJSONArray("trigger_occurrences")
            pendingEntryOccurrenceLinks.forEach { (entryId, sourceOccurrenceId) ->
                val targetOccurrenceId = idMaps["trigger_occurrences"]?.get(sourceOccurrenceId)
                    ?: sourceOccurrences.findObjectByNumericId(sourceOccurrenceId)?.let { source ->
                        val sourceRuleId = source.optLongOrNull("triggerRuleId") ?: return@let null
                        val targetRuleId = idMaps["trigger_rules"]?.get(sourceRuleId) ?: sourceRuleId
                        db.query(
                            "SELECT id FROM trigger_occurrences WHERE triggerRuleId = ? AND sourceEventId = ? LIMIT 1",
                            arrayOf(targetRuleId.toString(), source.optString("sourceEventId")),
                        ).use { cursor -> if (cursor.moveToFirst()) cursor.getLong(0) else null }
                    }
                targetOccurrenceId?.let { occurrenceId ->
                    db.execSQL(
                        "UPDATE track_entries SET sourceOccurrenceId = ? WHERE id = ?",
                        arrayOf(occurrenceId, entryId),
                    )
                }
            }
            BackupMergeSummary(imported, skipped)
        }
        areaRepository?.ensureDefaultArea()
        RoomTrackRepository(database, SystemWhipClock, UuidWhipIdGenerator).rebuildSearchIndex()
        return summary
    }

    override suspend fun exportTasksCsv(): String = queryCsv(
        "SELECT t.id, t.title, t.notes, t.scheduleKind, t.dateEpochDay, t.deadlineEpochDay, t.priority, COALESCE(a.name, '') AS area, t.tagsCsv, t.recurrenceUnit, t.recurrenceInterval, t.recurrenceAnchor, t.reminderOffsetsMinutesCsv, t.completedAtMillis, t.archived FROM tasks t LEFT JOIN areas a ON a.id = t.areaId ORDER BY t.id",
    )

    override suspend fun exportHabitsCsv(): String = queryCsv(
        "SELECT h.uuid AS habitUuid, h.name AS habit, COALESCE(a.name, '') AS area, h.trackingMode, l.uuid AS eventUuid, l.localEpochDay, l.value, l.enteredUnitId, l.status, l.note FROM habits h LEFT JOIN areas a ON a.id = h.areaId LEFT JOIN habit_logs l ON l.habitId = h.id UNION ALL SELECT h.uuid, h.name, COALESCE(a.name, ''), h.trackingMode, s.uuid, s.localEpochDay, NULL, NULL, 'Skipped', '' FROM habits h JOIN habit_skips s ON s.habitId = h.id LEFT JOIN areas a ON a.id = h.areaId ORDER BY habitUuid, localEpochDay",
    )

    override suspend fun exportGoalsCsv(): String = queryCsv(
        "SELECT g.uuid AS goalUuid, g.name AS goal, COALESCE(a.name, '') AS area, g.type, g.unitId, g.elapsedStartMillis, g.elapsedDisplayUnit, e.id AS entryId, e.localEpochDay, e.enteredValue, e.enteredUnitId, e.status, e.sourceType, e.note FROM goals g LEFT JOIN areas a ON a.id = g.areaId LEFT JOIN measurement_entries e ON e.measurementId = g.measurementId ORDER BY g.id, e.timestampMillis",
    )

    override suspend fun exportGymCsv(): String = queryCsv(
        "SELECT s.uuid AS workoutUuid, s.localEpochDay, s.name AS workout, e.uuid AS exerciseUuid, e.name AS exercise, e.trackingType, e.archived AS exerciseArchived, we.machineProfileUuidSnapshot AS machineScopeUuid, we.machineNameSnapshot AS machine, we.machineConfigurationGroupSnapshot AS machineConfigurationFamily, we.machineConfigurationVersionSnapshot AS machineConfigurationVersion, we.machineConfigurationSnapshot AS machineConfiguration, we.machineLoadTypeSnapshot AS machineLoadType, we.machineLevelDirectionSnapshot AS machineLevelDirection, ws.uuid AS setUuid, ws.position, ws.classification, ws.machineLoadValue, ws.enteredWeight, ws.enteredWeightUnitId, ws.repetitions, ws.enteredDistance, ws.enteredDistanceUnitId, ws.durationSeconds, ws.rpe, ws.rir, ws.tempo, ws.note FROM exercises e LEFT JOIN workout_exercises we ON we.exerciseId = e.id LEFT JOIN workout_sessions s ON s.id = we.sessionId LEFT JOIN workout_sets ws ON ws.workoutExerciseId = we.id AND ws.deletedAtMillis IS NULL ORDER BY e.position, s.startedAtMillis, we.position, ws.position",
    )

    override suspend fun exportTracksCsv(): String = queryCsv(
        "SELECT t.uuid AS trackUuid, t.name AS track, a.name AS area, e.uuid AS entryUuid, e.entryEpochDay, f.uuid AS fieldUuid, f.name AS field, f.type, v.textValue, v.enteredNumber, v.enteredUnitId, v.canonicalNumber, v.dateEpochDay, v.booleanValue, o.label AS choiceValue, v.scaleValue FROM tracks t JOIN areas a ON a.id = t.areaId LEFT JOIN track_entries e ON e.trackId = t.id LEFT JOIN track_values v ON v.entryId = e.id LEFT JOIN track_fields f ON f.id = v.fieldId LEFT JOIN track_choice_options o ON o.id = v.choiceOptionId ORDER BY t.position, e.entryEpochDay, e.createdAtMillis, f.position",
    )

    override suspend fun deleteAllData() {
        settingsRepository?.let { repository ->
            check(repository.updateAndConfirm { AppSettings() }) {
                "Local storage did not confirm reset settings; no Whip records were deleted."
            }
        }
        database.clearAllTables()
        areaRepository?.ensureDefaultArea()
    }

    private suspend fun queryCsv(sql: String): String = database.withTransaction {
        buildString {
            database.openHelper.readableDatabase.query(sql).use { cursor ->
                appendLine(cursor.columnNames.joinToString(",", transform = ::csvCell))
                while (cursor.moveToNext()) {
                    appendLine(cursor.columnNames.indices.joinToString(",") { index ->
                        val value = if (cursor.isNull(index)) "" else when (cursor.getType(index)) {
                            Cursor.FIELD_TYPE_BLOB -> Base64.encodeToString(cursor.getBlob(index), Base64.NO_WRAP)
                            else -> cursor.getString(index)
                        }
                        csvCell(value)
                    })
                }
            }
        }
    }

    private fun parseAndValidate(json: String): JSONObject {
        val root = runCatching { JSONObject(json) }.getOrElse { error("This is not valid JSON") }
        require(root.optString("format") == BACKUP_FORMAT) { "This is not a Whip backup" }
        validateBackupContract(
            envelopeVersion = root.optInt("envelopeVersion"),
            dataModelEpoch = root.optInt("dataModelEpoch", 0),
            databaseVersion = root.optInt("databaseVersion"),
        )
        val dbVersion = root.getInt("databaseVersion")
        val tables = root.optJSONObject("tables") ?: error("Backup has no table data")
        val tableNames = tables.keys().asSequence().toSet()
        require(tableNames == EXPORT_TABLES.toSet()) {
            "Backup table set does not match this build"
        }
        require(tableNames.all { tables.optJSONArray(it) != null }) { "Backup contains invalid table data" }
        tableNames.forEach { table ->
            val rows = tables.getJSONArray(table)
            val seen = mutableSetOf<String>()
            for (index in 0 until rows.length()) {
                val row = rows.getJSONObject(index)
                val stable = when {
                    row.has("uuid") && !row.isNull("uuid") -> row.optString("uuid").takeIf(String::isNotBlank)
                    row.opt("id") is String -> row.optString("id").takeIf(String::isNotBlank)
                    else -> null
                } ?: continue
                require(seen.add(stable)) { "Backup contains duplicate stable identity '$stable' in $table" }
            }
        }
        return root
    }

    /**
     * A checksum proves that a backup was not changed after export; it does not
     * prove that the contained domain values are safe. Validate custom units
     * before any live table is deleted or merged so malformed conversions can
     * never become latent crashes or corrupt later history.
     */
    private fun validateBackupUnitDefinitions(tables: JSONObject) {
        val rows = tables.getJSONArray("unit_definitions")
        val validDimensions = UnitDimension.entries.mapTo(mutableSetOf(), UnitDimension::name)
        for (index in 0 until rows.length()) {
            val row = rows.getJSONObject(index)
            val id = row.optString("id")
            val name = row.optString("name")
            val dimension = row.optString("dimension")
            val factor = runCatching { row.getDouble("toCanonicalFactor") }
                .getOrElse { error("Backup custom unit ${index + 1} has no valid conversion factor") }
            val offset = runCatching { row.getDouble("toCanonicalOffset") }
                .getOrElse { error("Backup custom unit ${index + 1} has no valid conversion offset") }

            require(id.isNotBlank() && id.length <= 512) { "Backup custom unit ${index + 1} has an invalid identity" }
            require(BuiltInUnits.get(id) == null) { "Backup custom unit '$id' conflicts with a built-in unit" }
            // Existing releases did not cap custom-unit labels. Preserve those
            // historical definitions even though current creation UI applies
            // tighter limits to new names and symbols.
            require(name.isNotBlank()) {
                "Backup custom unit '$id' has an invalid name"
            }
            require(dimension in validDimensions) { "Backup custom unit '$id' has an unknown measurement type" }
            require(factor.isFinite() && factor > 0.0) { "Backup custom unit '$id' has an invalid conversion factor" }
            require(offset.isFinite()) { "Backup custom unit '$id' has an invalid conversion offset" }
            require(row.optInt("custom", 0) == 1 || row.optBoolean("custom", false)) {
                "Backup unit '$id' is not a valid custom unit"
            }
        }
        validateBackupUnitReferencesAndCanonicalValues(tables)
        validateBackupDomainRelationships(tables)
    }

    /**
     * SQLite foreign keys prove that each referenced row exists, but several
     * historical tables deliberately store two owners so a row can still point
     * at a real child belonging to the wrong parent. Reject those valid-looking
     * cross-owner links, unknown enums, and invalid authored primitives before
     * replace or merge touches live data.
     */
    private fun validateBackupDomainRelationships(tables: JSONObject) {
        fun enumNames(vararg values: String) = values.toSet()
        fun JSONObject.requireEnum(key: String, values: Set<String>, label: String) {
            require(optString(key) in values) { "Backup $label has an unknown $key" }
        }

        val areaKeys = mutableSetOf<String>()
        tables.getJSONArray("areas").forEachObject { row ->
            val name = row.optString("name")
            require(name.isNotBlank() && row.optString("nameKey") == areaNameKey(name)) {
                "Backup Area has an invalid normalized name"
            }
            require(areaKeys.add(row.getString("nameKey"))) { "Backup contains duplicate Area names" }
        }
        val tagNames = mutableSetOf<String>()
        tables.getJSONArray("tags").forEachObject { row ->
            val name = row.optString("name").trim()
            require(name.isNotBlank() && ',' !in name) { "Backup Tag has an invalid name" }
            require(tagNames.add(name.lowercase(Locale.ROOT))) { "Backup contains duplicate Tag names" }
        }

        val measurements = tables.getJSONArray("measurement_definitions").objects().associateBy { it.getString("id") }
        tables.getJSONArray("measurement_definitions").forEachObject { row ->
            row.requireEnum(
                "valueKind",
                enumNames("Boolean", "Integer", "Decimal", "Duration", "Percentage", "Rating", "TimeOfDay", "Checklist"),
                "measurement definition",
            )
            row.requireEnum("dimension", UnitDimension.entries.mapTo(mutableSetOf(), UnitDimension::name), "measurement definition")
        }
        val measurementEntries = tables.getJSONArray("measurement_entries").objects().associateBy { it.getString("id") }
        measurementEntries.values.forEach { row ->
            require(measurements.containsKey(row.getString("measurementId"))) { "Backup measurement history references a missing measurement" }
            row.requireEnum("status", enumNames("Recorded", "Missing", "Failed", "Skipped", "Excused"), "measurement history")
            row.requireEnum("sourceType", enumNames("Manual", "Habit", "Goal", "Task", "Workout", "Exercise", "Track", "Import", "HealthConnect"), "measurement history")
        }

        val tasks = tables.getJSONArray("tasks").objects().associateBy { it.getLong("id") }
        tasks.values.forEach { row ->
            require(row.optString("title").isNotBlank()) { "Backup Task has no title" }
            row.requireEnum("scheduleKind", enumNames("Anytime", "Once", "Recurring"), "Task")
            row.requireEnum("progressDisplay", enumNames("Percent", "Fraction", "Both"), "Task")
            row.requireEnum("repeatStepPolicy", enumNames("Reset", "CarryUnfinished"), "Task")
            row.requireEnum("priority", enumNames("None", "Low", "Medium", "High", "Urgent"), "Task")
            row.requireEnum("effort", enumNames("Unspecified", "Light", "Medium", "High"), "Task")
            row.requireEnum("missedOccurrencePolicy", enumNames("KeepOldest", "KeepLatest", "CurrentOnly"), "Task")
            val time = row.optLongOrNull("timeMinutes")
            val duration = row.optLongOrNull("durationMinutes")
            require(time == null || time in 0..1439) { "Backup Task has an invalid time" }
            require(duration == null || duration in 1..1440) { "Backup Task has an invalid duration" }
            when (row.getString("scheduleKind")) {
                "Anytime" -> Unit
                "Once" -> require(row.optLongOrNull("dateEpochDay") != null) { "Backup scheduled Task has no date" }
                "Recurring" -> {
                    require(row.optLongOrNull("dateEpochDay") != null) { "Backup recurring Task has no start date" }
                    row.requireEnum("recurrenceUnit", enumNames("Days", "Weeks", "Months", "Years"), "recurring Task")
                    row.requireEnum("recurrenceEnd", enumNames("Never", "OnDate", "AfterCount"), "recurring Task")
                    row.requireEnum("recurrenceAnchor", enumNames("Schedule", "Completion"), "recurring Task")
                    require(row.optInt("recurrenceInterval", 0) > 0) { "Backup recurring Task has an invalid interval" }
                }
            }
        }
        val taskSteps = tables.getJSONArray("task_steps").objects().associate { it.getLong("id") to it.getLong("taskId") }
        tables.getJSONArray("task_step_states").forEachObject { row ->
            require(taskSteps[row.getLong("stepId")] == row.getLong("taskId")) {
                "Backup Subtask state belongs to a different Task"
            }
            val completed = row.optInt("completed", 0) != 0 || row.optBoolean("completed", false)
            require(completed == (row.optLongOrNull("completedAtMillis") != null)) {
                "Backup Subtask state has inconsistent completion data"
            }
        }
        tables.getJSONArray("task_occurrences").forEachObject { row ->
            require(tasks.containsKey(row.getLong("taskId"))) { "Backup occurrence references a missing Task" }
            row.requireEnum("state", enumNames("Open", "Completed", "Skipped"), "Task occurrence")
            require((row.getString("state") == "Open") == (row.optLongOrNull("completedAtMillis") == null)) {
                "Backup Task occurrence has inconsistent completion data"
            }
        }

        val habits = tables.getJSONArray("habits").objects().associateBy { it.getLong("id") }
        habits.values.forEach { row ->
            require(row.optString("name").isNotBlank()) { "Backup Habit has no name" }
            row.requireEnum("trackingMode", enumNames("CheckOff", "Count", "Decimal", "Duration", "Checklist", "Rating", "LogOnly"), "Habit")
            row.requireEnum("comparison", enumNames("AtLeast", "AtMost", "Exactly", "WithinRange", "None"), "Habit")
            row.requireEnum("targetPeriod", enumNames("Occurrence", "Day", "Week", "Month", "RollingDays"), "Habit")
            row.requireEnum("scheduleType", enumNames("Daily", "SelectedWeekdays", "EveryNDays", "FlexibleTimesPerWeek", "FlexibleTimesPerMonth"), "Habit")
            row.requireEnum("endType", enumNames("Never", "OnDate", "AfterStreak", "AfterCompletions", "AfterTotal"), "Habit")
            row.requireEnum("weekStart", enumNames("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"), "Habit")
            val measurement = measurements[row.getString("measurementId")] ?: error("Backup Habit references a missing measurement")
            require(measurement.getString("dimension") == row.getString("dimension")) { "Backup Habit measurement uses another measurement type" }
            val expectedValueKind = when (row.getString("trackingMode")) {
                "CheckOff" -> "Boolean"
                "Count" -> "Integer"
                "Decimal", "LogOnly" -> "Decimal"
                "Duration" -> "Duration"
                "Checklist" -> "Checklist"
                "Rating" -> "Rating"
                else -> error("Backup Habit has an unknown tracking mode")
            }
            require(measurement.getString("valueKind") == expectedValueKind) { "Backup Habit measurement has incompatible value semantics" }
            row.nonBlankString("sourceMeasurementId")?.let { sourceId ->
                val source = measurements[sourceId] ?: error("Backup Habit references a missing connected data source")
                require(source.getString("dimension") == row.getString("dimension")) {
                    "Backup Habit connected data source uses another measurement type"
                }
                val expectedSourceMode = when (source.getString("valueKind")) {
                    "Integer" -> "Count"
                    "Duration" -> "Duration"
                    else -> "Decimal"
                }
                require(row.getString("trackingMode") == expectedSourceMode) {
                    "Backup Habit tracking does not match its connected data source"
                }
            }
        }
        val checklistItems = tables.getJSONArray("habit_checklist_items").objects()
            .associate { it.getLong("id") to it.getLong("habitId") }
        tables.getJSONArray("habit_checklist_states").forEachObject { row ->
            require(checklistItems[row.getLong("itemId")] == row.getLong("habitId")) {
                "Backup checklist history belongs to a different Habit"
            }
            val completed = row.optInt("completed", 0) != 0 || row.optBoolean("completed", false)
            require(!completed || row.optLongOrNull("completedAtMillis") != null) {
                "Backup checklist history has no completion time"
            }
        }
        tables.getJSONArray("habit_logs").forEachObject { row ->
            val habit = habits[row.getLong("habitId")] ?: error("Backup Habit history references a missing Habit")
            row.requireEnum("status", enumNames("Recorded", "Success", "Failed"), "Habit history")
            row.requireEnum("sourceType", enumNames("Manual", "Habit", "Goal", "Task", "Workout", "Exercise", "Track", "Import", "HealthConnect"), "Habit history")
            row.nonBlankString("measurementEntryId")?.let { entryId ->
                require(measurementEntries[entryId]?.getString("measurementId") == habit.getString("measurementId")) {
                    "Backup Habit history points to another measurement"
                }
            }
        }

        tables.getJSONArray("goals").forEachObject { row ->
            require(row.optString("name").isNotBlank()) { "Backup Goal has no name" }
            row.requireEnum("type", enumNames("ReachValue", "ReduceValue", "AccumulateTotal", "MaintainRange", "MeetAverage", "Consistency", "WeightedMilestones", "OpenEndedTrend", "ElapsedSince"), "Goal")
            row.requireEnum("aggregation", enumNames("Latest", "Sum", "Average", "Minimum", "Maximum", "CompletionCount", "TimeInRange"), "Goal")
            row.requireEnum("aggregationPeriod", enumNames("All", "Day", "Week", "Month", "RollingDays"), "Goal")
            row.requireEnum("direction", enumNames("Increase", "Decrease", "Neutral"), "Goal")
            row.requireEnum("paceType", enumNames("Linear", "None"), "Goal")
            row.requireEnum("consistencyPeriod", enumNames("Day", "Week", "Month"), "Goal")
            row.requireEnum("elapsedDisplayUnit", enumNames("Auto", "Minutes", "Hours", "Days", "Weeks", "Years"), "Goal")
            row.requireEnum("status", enumNames("Active", "Paused", "Completed", "Abandoned", "Archived"), "Goal")
            val measurement = measurements[row.getString("measurementId")] ?: error("Backup Goal references a missing measurement")
            require(measurement.getString("dimension") == row.getString("dimension")) { "Backup Goal measurement uses another measurement type" }
            require(measurement.getString("valueKind") == "Decimal") { "Backup Goal measurement has incompatible value semantics" }
        }
        tables.getJSONArray("goal_completion_snapshots").forEachObject { row ->
            row.requireEnum("status", enumNames("Completed", "Abandoned"), "Goal closure history")
        }

        val trackFields = tables.getJSONArray("track_fields").objects().associateBy { it.getLong("id") }
        val trackEntries = tables.getJSONArray("track_entries").objects().associateBy { it.getLong("id") }
        val trackChoices = tables.getJSONArray("track_choice_options").objects().associateBy { it.getLong("id") }
        trackFields.values.forEach { row ->
            row.requireEnum("type", enumNames("ShortText", "LongText", "Number", "SingleChoice", "Scale", "Date", "YesNo"), "Track Field")
        }
        tables.getJSONArray("track_values").forEachObject { row ->
            val field = trackFields[row.getLong("fieldId")] ?: error("Backup Track value references a missing Field")
            val entry = trackEntries[row.getLong("entryId")] ?: error("Backup Track value references a missing Entry")
            require(field.getLong("trackId") == entry.getLong("trackId")) { "Backup Track value crosses Track definitions" }
            val present = buildSet {
                if (row.nonBlankString("textValue") != null) add("Text")
                if (row.nullableDouble("enteredNumber") != null) add("Number")
                if (row.optLongOrNull("choiceOptionId") != null) add("Choice")
                if (row.nullableDouble("scaleValue") != null) add("Scale")
                if (row.optLongOrNull("dateEpochDay") != null) add("Date")
                if (row.opt("booleanValue").let { it != null && it != JSONObject.NULL }) add("Boolean")
            }
            val expectedShape = when (field.getString("type")) {
                "ShortText", "LongText" -> "Text"
                "Number" -> "Number"
                "SingleChoice" -> "Choice"
                "Scale" -> "Scale"
                "Date" -> "Date"
                "YesNo" -> "Boolean"
                else -> error("Backup Track Field has an unknown type")
            }
            require(present == setOf(expectedShape)) { "Backup Track value does not match its Field type" }
            row.optLongOrNull("choiceOptionId")?.let { choiceId ->
                require(trackChoices[choiceId]?.getLong("fieldId") == field.getLong("id")) {
                    "Backup Track choice belongs to another Field"
                }
            }
        }
    }

    private data class BackupUnitContract(
        val dimension: String,
        val factor: Double,
        val offset: Double,
    ) {
        fun toCanonical(value: Double): Double = (value + offset) * factor
    }

    private fun validateBackupUnitReferencesAndCanonicalValues(tables: JSONObject) {
        val builtInUnits = BuiltInUnits.all.associate { unit ->
            unit.id to BackupUnitContract(unit.dimension.name, unit.toCanonicalFactor, unit.toCanonicalOffset)
        }
        val units = builtInUnits.toMutableMap()
        tables.getJSONArray("unit_definitions").forEachObject { row ->
            units[row.getString("id")] = BackupUnitContract(
                row.getString("dimension"),
                row.getDouble("toCanonicalFactor"),
                row.getDouble("toCanonicalOffset"),
            )
        }

        val measurementDimensions = mutableMapOf<String, String>()
        tables.getJSONArray("measurement_definitions").forEachObject { row ->
            val dimension = row.getString("dimension")
            measurementDimensions[row.getString("id")] = dimension
            requireCompatibleBackupUnit(units, row.getString("defaultUnitId"), dimension, "measurement definition")
        }
        tables.getJSONArray("measurement_entries").forEachObject { row ->
            val enteredValue = row.nullableDouble("enteredValue")
            val enteredUnitId = row.nonBlankString("enteredUnitId")
            require((enteredValue == null) == (enteredUnitId == null)) {
                "Backup measurement history has inconsistent value and unit data"
            }
            if (enteredUnitId == null) {
                require(row.nullableDouble("canonicalValue") == null) {
                    "Backup measurement history has canonical data without an entered value"
                }
                return@forEachObject
            }
            val dimension = measurementDimensions[row.getString("measurementId")]
                ?: error("Backup measurement entry references a missing measurement definition")
            val unit = requireCompatibleBackupUnit(units, enteredUnitId, dimension, "measurement history")
            validateCanonicalPair(row, "enteredValue", "canonicalValue", unit, "measurement history")
        }

        tables.getJSONArray("habits").forEachObject { row ->
            requireCompatibleBackupUnit(units, row.getString("unitId"), row.getString("dimension"), "Habit")
        }
        validateBackupHabitTimers(tables, units)
        val habitDimensions = tables.getJSONArray("habits").objects().associate { row ->
            row.getLong("id") to row.getString("dimension")
        }
        tables.getJSONArray("habit_logs").forEachObject { row ->
            val value = row.nullableDouble("value")
            val unitId = row.nonBlankString("enteredUnitId")
            require((value == null) == (unitId == null)) {
                "Backup Habit history has inconsistent value and unit data"
            }
            if (unitId == null) {
                require(row.nullableDouble("canonicalValue") == null) {
                    "Backup Habit history has canonical data without an entered value"
                }
                return@forEachObject
            }
            val dimension = habitDimensions[row.getLong("habitId")]
                ?: error("Backup Habit history references a missing Habit")
            val unit = requireCompatibleBackupUnit(units, unitId, dimension, "Habit history")
            validateCanonicalPair(row, "value", "canonicalValue", unit, "Habit history")
        }
        tables.getJSONArray("goals").forEachObject { row ->
            requireCompatibleBackupUnit(units, row.getString("unitId"), row.getString("dimension"), "Goal")
        }

        val trackFieldDimensions = mutableMapOf<Long, String>()
        tables.getJSONArray("track_fields").forEachObject { row ->
            val unitId = row.nullableString("unitId") ?: return@forEachObject
            val dimension = row.nullableString("dimension")
                ?: error("Backup Track Field with a unit has no measurement type")
            trackFieldDimensions[row.getLong("id")] = dimension
            requireCompatibleBackupUnit(units, unitId, dimension, "Track Field")
        }
        tables.getJSONArray("track_values").forEachObject { row ->
            val entered = row.nullableDouble("enteredNumber")
            val unitId = row.nonBlankString("enteredUnitId")
            require((entered == null) == (unitId == null)) {
                "Backup Track history has inconsistent value and unit data"
            }
            if (unitId == null) {
                require(row.nullableDouble("canonicalNumber") == null) {
                    "Backup Track history has canonical data without an entered value"
                }
                return@forEachObject
            }
            val dimension = trackFieldDimensions[row.getLong("fieldId")]
                ?: error("Backup Track value references a Field without a compatible unit")
            val unit = requireCompatibleBackupUnit(units, unitId, dimension, "Track history")
            validateCanonicalPair(row, "enteredNumber", "canonicalNumber", unit, "Track history")
        }

        val mass = UnitDimension.Mass.name
        val distance = UnitDimension.Distance.name
        tables.getJSONArray("exercises").forEachObject { row ->
            requireCompatibleBackupUnit(builtInUnits, row.getString("weightUnitId"), mass, "Exercise")
        }
        tables.getJSONArray("gym_machines").forEachObject { row ->
            require(runCatching { MachineLevelDirection.valueOf(row.getString("levelDirection")) }.isSuccess) {
                "Backup Gym Machine has an unsupported numbered-setting direction"
            }
            if (row.optString("loadType") == "Mass") {
                val unitId = row.nonBlankString("unitId")
                    ?: error("Backup Gym Machine has no mass unit")
                requireCompatibleBackupUnit(builtInUnits, unitId, mass, "Gym Machine")
            } else {
                row.requireOptionalBackupUnit(builtInUnits, "unitId", mass, "Gym Machine")
            }
        }
        val routinePlacements = mutableMapOf<Long, JSONObject>()
        tables.getJSONArray("routine_exercises").forEachObject { row ->
            routinePlacements[row.getLong("id")] = row
            row.requireOptionalBackupUnit(builtInUnits, "machineUnitIdSnapshot", mass, "Routine machine snapshot")
            if (row.optString("machineLoadTypeSnapshot") == "Mass") {
                require(row.nonBlankString("machineUnitIdSnapshot") != null) {
                    "Backup Routine mass-machine snapshot has no unit"
                }
            }
            row.requireBackupUnitForValues(
                builtInUnits, "trainingMaxUnitId", mass, "Routine Training Max", "trainingMaxValue",
            )
            row.requireBackupUnitForValues(
                builtInUnits, "trainingMaxBasisUnitId", mass, "Routine Training Max basis", "trainingMaxBasisValue",
            )
            row.nonBlankString("trainingMaxUnitId")?.let { unitId ->
                validateCanonicalPair(
                    row, "trainingMaxValue", "trainingMaxKg",
                    builtInUnits.getValue(unitId), "Routine Training Max",
                )
            }
        }
        tables.getJSONArray("routine_sets").forEachObject { row ->
            row.requireBackupUnitForValues(
                builtInUnits, "enteredWeightUnitId", mass, "Routine weight", "enteredWeight",
            )
            row.requireBackupUnitForValues(
                builtInUnits, "enteredDistanceUnitId", distance, "Routine distance", "enteredDistance",
            )
            validateMachineBoundWeightUnit(
                row,
                routinePlacements[row.getLong("routineExerciseId")]
                    ?: error("Backup Routine Set references a missing Exercise placement"),
                "enteredWeight",
                "enteredWeightUnitId",
                "Routine weight",
            )
        }
        val workoutPlacements = mutableMapOf<Long, JSONObject>()
        tables.getJSONArray("workout_exercises").forEachObject { row ->
            workoutPlacements[row.getLong("id")] = row
            require(
                runCatching {
                    MachineLevelDirection.valueOf(row.getString("machineLevelDirectionSnapshot"))
                }.isSuccess,
            ) { "Backup Workout Exercise has an unsupported numbered-setting direction" }
            row.requireOptionalBackupUnit(builtInUnits, "machineUnitIdSnapshot", mass, "Workout machine snapshot")
            if (row.optString("machineLoadTypeSnapshot") == "Mass") {
                require(row.nonBlankString("machineUnitIdSnapshot") != null) {
                    "Backup Workout mass-machine snapshot has no unit"
                }
            }
            row.requireOptionalBackupUnit(builtInUnits, "exerciseWeightUnitSnapshot", mass, "Workout Exercise snapshot")
            row.requireBackupUnitForValues(
                builtInUnits, "trainingMaxUnitIdSnapshot", mass, "Workout Training Max snapshot",
                "trainingMaxValueSnapshot",
            )
            row.nonBlankString("trainingMaxUnitIdSnapshot")?.let { unitId ->
                validateCanonicalPair(
                    row, "trainingMaxValueSnapshot", "trainingMaxKgSnapshot",
                    builtInUnits.getValue(unitId), "Workout Training Max snapshot",
                )
            }
        }
        tables.getJSONArray("workout_sets").forEachObject { row ->
            row.requireBackupUnitForValues(
                builtInUnits, "enteredWeightUnitId", mass, "Workout weight history", "enteredWeight",
            )
            row.requireBackupUnitForValues(
                builtInUnits, "prescribedWeightUnitId", mass, "Workout prescribed weight", "prescribedEnteredWeight",
            )
            row.requireBackupUnitForValues(
                builtInUnits, "enteredDistanceUnitId", distance, "Workout distance history", "enteredDistance",
            )
            row.nonBlankString("enteredDistanceUnitId")?.let { unitId ->
                validateCanonicalPair(
                    row, "enteredDistance", "canonicalDistanceMetres",
                    builtInUnits.getValue(unitId), "Workout distance history",
                )
            }
            val placement = workoutPlacements[row.getLong("workoutExerciseId")]
                ?: error("Backup Workout Set references a missing Exercise placement")
            validateMachineBoundWeightUnit(
                row, placement, "enteredWeight", "enteredWeightUnitId", "Workout weight history",
            )
            validateMachineBoundWeightUnit(
                row, placement, "prescribedEnteredWeight", "prescribedWeightUnitId", "Workout prescribed weight",
            )
            validateWorkoutResistancePair(
                row = row,
                placement = placement,
                enteredKey = "enteredWeight",
                unitKey = "enteredWeightUnitId",
                canonicalKey = "canonicalWeightKg",
                machineSettingKey = "machineLoadValue",
                label = "Workout weight history",
            )
            validateWorkoutResistancePair(
                row = row,
                placement = placement,
                enteredKey = "prescribedEnteredWeight",
                unitKey = "prescribedWeightUnitId",
                canonicalKey = "prescribedCanonicalWeightKg",
                machineSettingKey = "prescribedMachineLoadValue",
                label = "Workout prescribed weight",
            )
        }
        tables.getJSONArray("training_max_decisions").forEachObject { row ->
            requireCompatibleBackupUnit(builtInUnits, row.getString("unitId"), mass, "Training Max decision")
        }
        tables.getJSONArray("personal_records").forEachObject { row ->
            val expectedUnitId = when (row.getString("type")) {
                "MaxWeight", "BestWeightForRepCount", "EstimatedOneRepMax",
                "SetVolume", "ExerciseWorkoutVolume" -> "kilogram"
                "MaxRepetitions", "MaxRepetitionsForWeight" -> "count"
                "MaxDistance" -> "distance_m"
                "MaxDuration" -> "second"
                "MaxSpeed" -> "distance_m/second"
                "MinPace" -> "second/kilometre"
                // Machine setting units are immutable user-authored labels,
                // not measurement conversion identities.
                "MaxMachineSetting" -> null
                else -> error("Backup Personal Record has an unknown type")
            }
            if (expectedUnitId != null) {
                require(row.getString("unitId") == expectedUnitId) {
                    "Backup Personal Record does not use its canonical unit"
                }
            }
        }

        tables.getJSONArray("trigger_field_mappings").forEachObject { row ->
            val unitId = row.nonBlankString("constantUnitId") ?: return@forEachObject
            val dimension = trackFieldDimensions[row.getLong("targetFieldId")]
                ?: error("Backup automation constant references a Field without a compatible unit")
            requireCompatibleBackupUnit(units, unitId, dimension, "automation constant")
        }
    }

    private fun JSONObject.requireOptionalBackupUnit(
        units: Map<String, BackupUnitContract>,
        key: String,
        dimension: String,
        label: String,
    ) {
        val unitId = nonBlankString(key) ?: return
        requireCompatibleBackupUnit(units, unitId, dimension, label)
    }

    private fun JSONObject.requireBackupUnitForValues(
        units: Map<String, BackupUnitContract>,
        key: String,
        dimension: String,
        label: String,
        vararg valueKeys: String,
    ) {
        val hasValue = valueKeys.any { valueKey ->
            has(valueKey) && !isNull(valueKey) && when (val value = opt(valueKey)) {
                is String -> value.isNotBlank()
                null -> false
                else -> true
            }
        }
        val unitId = nonBlankString(key)
        require(!hasValue || unitId != null) { "Backup $label has a value without a unit" }
        unitId?.let { requireCompatibleBackupUnit(units, it, dimension, label) }
    }

    private fun validateWorkoutResistancePair(
        row: JSONObject,
        placement: JSONObject,
        enteredKey: String,
        unitKey: String,
        canonicalKey: String,
        machineSettingKey: String,
        label: String,
    ) {
        val entered = row.nullableDouble(enteredKey)
        val unitId = row.nonBlankString(unitKey)
        val machineType = placement.optString("machineLoadTypeSnapshot")
        val machineSetting = row.nullableDouble(machineSettingKey).takeIf { machineType == "Level" }
        if (entered == null && machineSetting == null && row.nullableDouble(canonicalKey) == null) return
        val expected = canonicalResistanceKg(
            enteredValue = entered,
            enteredUnitId = unitId,
            machineSetting = machineSetting,
            interpretation = runCatching {
                LoadInterpretation.valueOf(placement.optString("loadInterpretationSnapshot", "Total"))
            }.getOrDefault(LoadInterpretation.Total),
            baseLoadKg = placement.nullableDouble("baseLoadKgSnapshot"),
            addOnPlateKg = placement.nullableDouble("machineAddOnPlateKgSnapshot"),
            massMappingKg = placement.optString("machineMassMappingCsvSnapshot").parseStableMappingCsv(),
            stackMode = runCatching {
                MachineStackMode.valueOf(placement.optString("machineStackModeSnapshot", "Single"))
            }.getOrDefault(MachineStackMode.Single),
            pulleyRatio = placement.optDouble("machinePulleyRatioSnapshot", 1.0),
            unilateral = row.optInt("unilateral", 0) != 0,
        )
        val stored = row.nullableDouble(canonicalKey)
        require(stored == null || stored.isFinite()) { "Backup $label contains a non-finite canonical value" }
        require(expected == null || expected.isFinite()) { "Backup $label overflows its conversion" }
        require((expected == null) == (stored == null)) { "Backup $label has inconsistent canonical data" }
        if (expected != null && stored != null) requireApproximatelyEqual(expected, stored, label)
    }

    private fun validateMachineBoundWeightUnit(
        row: JSONObject,
        placement: JSONObject,
        enteredKey: String,
        unitKey: String,
        label: String,
    ) {
        val entered = row.nullableDouble(enteredKey)
        val unitId = row.nonBlankString(unitKey)
        when (placement.optString("machineLoadTypeSnapshot")) {
            "Mass" -> if (entered != null) {
                require(unitId == placement.nonBlankString("machineUnitIdSnapshot")) {
                    "Backup $label does not match its mass-machine unit snapshot"
                }
            }
            "Level" -> require(entered == null && unitId == null) {
                "Backup $label stores weight for an ordinal machine"
            }
        }
    }

    private fun requireCompatibleBackupUnit(
        units: Map<String, BackupUnitContract>,
        unitId: String,
        dimension: String,
        label: String,
    ): BackupUnitContract {
        val unit = units[unitId] ?: error("Backup $label references unknown unit '$unitId'")
        require(unit.dimension == dimension) { "Backup $label uses an incompatible unit '$unitId'" }
        return unit
    }

    private fun validateCanonicalPair(
        row: JSONObject,
        enteredKey: String,
        canonicalKey: String,
        unit: BackupUnitContract,
        label: String,
    ) {
        val entered = row.nullableDouble(enteredKey)
        val canonical = row.nullableDouble(canonicalKey)
        require((entered == null) == (canonical == null)) {
            "Backup $label has incomplete canonical data"
        }
        if (entered == null || canonical == null) return
        require(entered.isFinite() && canonical.isFinite()) { "Backup $label contains a non-finite value" }
        val expected = unit.toCanonical(entered)
        require(expected.isFinite()) { "Backup $label overflows its saved unit conversion" }
        requireApproximatelyEqual(expected, canonical, label)
    }

    private fun requireApproximatelyEqual(expected: Double, actual: Double, label: String) {
        require(actual.isFinite()) { "Backup $label contains a non-finite canonical value" }
        val tolerance = maxOf(1.0, kotlin.math.abs(expected), kotlin.math.abs(actual)) * 1e-9
        require(kotlin.math.abs(expected - actual) <= tolerance) {
            "Backup $label contradicts its saved unit conversion"
        }
    }

    private fun requireMatchingLiveUnitContract(db: androidx.sqlite.db.SupportSQLiteDatabase, row: JSONObject) {
        db.query(
            "SELECT dimension, toCanonicalFactor, toCanonicalOffset, custom FROM unit_definitions WHERE id = ? LIMIT 1",
            arrayOf(row.getString("id")),
        ).use { cursor ->
            require(cursor.moveToFirst()) { "Live custom unit disappeared during merge" }
            require(cursor.getString(0) == row.getString("dimension")) {
                "Backup custom unit identity conflicts with the live measurement type"
            }
            require(cursor.getDouble(1) == row.getDouble("toCanonicalFactor") &&
                cursor.getDouble(2) == row.getDouble("toCanonicalOffset")) {
                "Backup custom unit identity conflicts with the live conversion contract"
            }
            require(cursor.getInt(3) != 0) { "Live unit identity is not a custom unit" }
        }
    }

    private fun sanitizeHabitTimersForPortableBackup(tables: JSONObject) {
        val habits = tables.getJSONArray("habits").objects().associateBy { it.getLong("id") }
        val portable = JSONArray()
        tables.getJSONArray("habit_timer_sessions").forEachObject { session ->
            if (session.optString("state") !in setOf("Running", "ReviewRequired")) return@forEachObject
            session.put("state", "ReviewRequired")
            session.put("anchorElapsedRealtimeMillis", JSONObject.NULL)
            session.put("anchorBootId", JSONObject.NULL)
            portable.put(session)
            habits[session.getLong("habitId")]?.apply {
                put("timerNeedsReview", true)
                put("timerSessionId", session.getString("sessionId"))
                put("timerAccumulatedSeconds", session.optDouble("accumulatedCanonicalSeconds", 0.0))
                put("timerAnchorElapsedRealtimeMillis", JSONObject.NULL)
            }
        }
        tables.put("habit_timer_sessions", portable)
    }

    private fun sanitizeHabitTimersForMerge(tables: JSONObject) {
        tables.getJSONArray("habits").forEachObject { habit ->
            habit.put("timerStartedAtMillis", JSONObject.NULL)
            habit.put("timerSessionId", JSONObject.NULL)
            habit.put("timerNeedsReview", false)
            habit.put("timerAccumulatedSeconds", 0.0)
            habit.put("timerAnchorElapsedRealtimeMillis", JSONObject.NULL)
        }
        // Active timers belong to the installation that started them. Completed timer facts
        // already live in Habit/Measurement history and merge through those tables normally.
        tables.put("habit_timer_sessions", JSONArray())
    }

    private fun validateBackupHabitTimers(
        tables: JSONObject,
        units: Map<String, BackupUnitContract>,
    ) {
        val habits = tables.getJSONArray("habits").objects().associateBy { it.getLong("id") }
        val seenSessions = mutableSetOf<String>()
        val activeByHabit = mutableMapOf<Long, JSONObject>()
        tables.getJSONArray("habit_timer_sessions").forEachObject { session ->
            val sessionId = session.optString("sessionId")
            require(sessionId.isNotBlank() && seenSessions.add(sessionId)) {
                "Backup Habit timer has an invalid or duplicate session identity"
            }
            val habitId = session.getLong("habitId")
            require(habits.containsKey(habitId)) { "Backup Habit timer references a missing Habit" }
            val state = session.optString("state")
            require(state in setOf("Running", "ReviewRequired", "Completed", "Discarded")) {
                "Backup Habit timer has an unknown state"
            }
            val activeHabitId = session.optLongOrNull("activeHabitId")
            val unresolved = state == "Running" || state == "ReviewRequired"
            require(unresolved == (activeHabitId != null)) { "Backup Habit timer state is inconsistent" }
            if (unresolved) {
                require(activeHabitId == habitId && activeByHabit.put(habitId, session) == null) {
                    "Backup contains more than one active timer for a Habit"
                }
                val habit = requireNotNull(habits[habitId])
                require(
                    habit.optString("trackingMode") == HabitTrackingMode.Duration.name &&
                        habit.optString("dimension") == UnitDimension.Duration.name &&
                        habit.nullableString("sourceMeasurementId") == null
                ) { "Backup active timer belongs to an incompatible Habit" }
                require(session.optLongOrNull("anchorWallMillis") != null) { "Backup active timer has no wall anchor" }
                val timerUnitId = session.optString("unitId")
                require(
                    timerUnitId == habit.optString("unitId") &&
                        units[timerUnitId]?.dimension == UnitDimension.Duration.name
                ) { "Backup active timer has an incompatible duration unit" }
                val accumulated = session.optDouble("accumulatedCanonicalSeconds", Double.NaN)
                require(accumulated.isFinite() && accumulated >= 0.0) { "Backup active timer has invalid elapsed time" }
                require(session.optLongOrNull("resolvedAtMillis") == null) {
                    "Backup active timer is already marked resolved"
                }
                if (state == "Running") {
                    require(
                        session.optLongOrNull("anchorElapsedRealtimeMillis") != null &&
                            session.optString("anchorBootId").isNotBlank()
                    ) { "Backup running timer has no monotonic clock identity" }
                }
            } else {
                require(
                    activeHabitId == null &&
                        session.optLongOrNull("anchorWallMillis") == null &&
                        session.optLongOrNull("anchorElapsedRealtimeMillis") == null &&
                        session.nullableString("anchorBootId") == null &&
                        session.nullableDouble("accumulatedCanonicalSeconds") == null &&
                        session.nullableString("unitId") == null &&
                        session.optLongOrNull("resolvedAtMillis") != null
                ) { "Backup resolved timer retains active timing state" }
            }
        }
        habits.forEach { (habitId, habit) ->
            val active = activeByHabit[habitId]
            val mirroredSession = habit.nonBlankString("timerSessionId")
            if (active == null) {
                require(
                    mirroredSession == null &&
                        habit.optLongOrNull("timerStartedAtMillis") == null &&
                        habit.optLongOrNull("timerAnchorElapsedRealtimeMillis") == null
                ) {
                    "Backup Habit has timer state without an active session"
                }
            } else {
                require(mirroredSession == active.getString("sessionId")) { "Backup Habit timer mirror is stale" }
                require(habit.optLongOrNull("timerStartedAtMillis") == active.optLongOrNull("anchorWallMillis")) {
                    "Backup Habit timer start does not match its session"
                }
                val needsReview = habit.optInt("timerNeedsReview", 0) != 0 || habit.optBoolean("timerNeedsReview", false)
                require(needsReview == (active.optString("state") == "ReviewRequired")) {
                    "Backup Habit timer review state is inconsistent"
                }
                val accumulated = active.getDouble("accumulatedCanonicalSeconds")
                require(habit.optDouble("timerAccumulatedSeconds", Double.NaN) == accumulated) {
                    "Backup Habit timer elapsed-time mirror is stale"
                }
                val expectedElapsedAnchor = active.optLongOrNull("anchorElapsedRealtimeMillis")
                    .takeIf { active.optString("state") == "Running" }
                require(habit.optLongOrNull("timerAnchorElapsedRealtimeMillis") == expectedElapsedAnchor) {
                    "Backup Habit timer monotonic-clock mirror is stale"
                }
            }
        }
    }


}

private inline fun JSONArray.forEachObject(block: (JSONObject) -> Unit) {
    for (index in 0 until length()) block(getJSONObject(index))
}

private data class MergeForeignKey(
    val childColumn: String,
    val parentTable: String,
)

private data class MergeTableMetadata(
    val columns: Set<String>,
    val autoNumericId: Boolean,
    val foreignKeys: List<MergeForeignKey>,
)

private data class ExistingMergeRow(val numericId: Long?)

private fun mergeTableMetadata(
    db: androidx.sqlite.db.SupportSQLiteDatabase,
    table: String,
): MergeTableMetadata {
    val columns = linkedSetOf<String>()
    var autoNumericId = false
    db.query("PRAGMA table_info(${safeIdentifier(table)})").use { cursor ->
        val nameIndex = cursor.getColumnIndexOrThrow("name")
        val typeIndex = cursor.getColumnIndexOrThrow("type")
        val pkIndex = cursor.getColumnIndexOrThrow("pk")
        while (cursor.moveToNext()) {
            val name = cursor.getString(nameIndex)
            columns += name
            if (name == "id" && cursor.getInt(pkIndex) > 0 && cursor.getString(typeIndex).contains("INT", true)) {
                autoNumericId = true
            }
        }
    }
    val foreignKeys = buildList {
        db.query("PRAGMA foreign_key_list(${safeIdentifier(table)})").use { cursor ->
            val parentIndex = cursor.getColumnIndexOrThrow("table")
            val fromIndex = cursor.getColumnIndexOrThrow("from")
            val toIndex = cursor.getColumnIndexOrThrow("to")
            while (cursor.moveToNext()) {
                if (cursor.getString(toIndex) == "id") {
                    add(MergeForeignKey(cursor.getString(fromIndex), cursor.getString(parentIndex)))
                }
            }
        }
    }
    return MergeTableMetadata(columns, autoNumericId, foreignKeys)
}

private fun JSONObject.stableMergeKey(metadata: MergeTableMetadata): Pair<String, String>? = when {
    "uuid" in metadata.columns && has("uuid") && !isNull("uuid") && optString("uuid").isNotBlank() ->
        "uuid" to optString("uuid")
    !metadata.autoNumericId && "id" in metadata.columns && has("id") && opt("id") is String && optString("id").isNotBlank() ->
        "id" to optString("id")
    else -> null
}

private fun JSONObject.optLongOrNull(key: String): Long? =
    if (!has(key) || isNull(key) || opt(key) !is Number) null else optLong(key)

private fun JSONArray.findObjectByNumericId(id: Long): JSONObject? {
    for (index in 0 until length()) {
        val candidate = optJSONObject(index) ?: continue
        if (candidate.optLongOrNull("id") == id) return candidate
    }
    return null
}

private fun remapForeignKeys(
    values: ContentValues,
    metadata: MergeTableMetadata,
    idMaps: Map<String, Map<Long, Long>>,
) {
    metadata.foreignKeys.forEach { key ->
        val old = values.getAsLong(key.childColumn) ?: return@forEach
        idMaps[key.parentTable]?.get(old)?.let { values.put(key.childColumn, it) }
    }
}

private fun existingNaturalChildId(
    db: androidx.sqlite.db.SupportSQLiteDatabase,
    table: String,
    values: ContentValues,
): Long? {
    val (parentColumn, positionColumn) = when (table) {
        "link_rule_conditions" -> "linkRuleId" to "position"
        "trigger_rule_conditions" -> "triggerRuleId" to "position"
        "trigger_field_mappings" -> "triggerRuleId" to "targetFieldId"
        else -> return null
    }
    val parent = values.getAsLong(parentColumn) ?: return null
    val position = values.getAsLong(positionColumn) ?: return null
    return db.query(
        "SELECT id FROM ${safeIdentifier(table)} WHERE ${safeIdentifier(parentColumn)} = ? AND ${safeIdentifier(positionColumn)} = ? LIMIT 1",
        arrayOf(parent.toString(), position.toString()),
    ).use { cursor -> if (cursor.moveToFirst()) cursor.getLong(0) else null }
}

private fun remapPolymorphicReferences(
    table: String,
    values: ContentValues,
    idMaps: Map<String, Map<Long, Long>>,
) {
    fun remap(column: String, parent: String) {
        val old = values.getAsLong(column) ?: return
        idMaps[parent]?.get(old)?.let { values.put(column, it) }
    }
    fun sourceParent(type: String?): String? = when (type) {
        "Task", "Subtask" -> "tasks"
        "Habit" -> "habits"
        "Workout" -> "workout_sessions"
        "Exercise" -> "exercises"
        "Track" -> "tracks"
        else -> null
    }
    fun remapCsv(column: String, parent: String) {
        val original = values.getAsString(column) ?: return
        values.put(
            column,
            original.split(',').mapNotNull(String::trim).mapNotNull(String::toLongOrNull)
                .map { idMaps[parent]?.get(it) ?: it }.distinct().joinToString(","),
        )
    }
    when (table) {
        "task_step_snapshots" -> remap("stepId", "task_steps")
        "workout_sessions" -> {
            remap("sourceRoutineId", "gym_routines")
            remap("sourceRoutineDayId", "routine_days")
            remapCsv("invalidatedMainExerciseIdsCsv", "exercises")
        }
        "workout_exercises" -> {
            remap("machineId", "gym_machines")
            remapCsv("alternativeExerciseIdsCsvSnapshot", "exercises")
        }
        "routine_exercises" -> {
            remap("machineId", "gym_machines")
            remapCsv("alternativeExerciseIdsCsv", "exercises")
        }
        "personal_records" -> remap("machineId", "gym_machines")
        "gym_machine_exercise_joins" -> {
            remap("machineId", "gym_machines")
            remap("exerciseId", "exercises")
        }
        "graph_presets" -> remapCsv("exerciseIdsCsv", "exercises")
        "link_rules" -> {
            sourceParent(values.getAsString("sourceType"))?.let { remap("sourceEntityId", it) }
            if (values.getAsString("sourceType") == "Subtask") remap("sourceItemId", "task_steps")
        }
        "contributions" -> sourceParent(values.getAsString("sourceType"))?.let { remap("sourceEntityId", it) }
        "trigger_rules" -> {
            sourceParent(values.getAsString("sourceType"))?.let { remap("sourceEntityId", it) }
            if (values.getAsString("sourceType") == "Subtask") remap("sourceItemId", "task_steps")
            when (values.getAsString("targetType")) {
                "Habit" -> remap("targetEntityId", "habits")
                "Task" -> remap("targetEntityId", "tasks")
                "Track" -> remap("targetEntityId", "tracks")
            }
        }
    }
}

private fun Cursor.toJsonRow(): JSONObject = JSONObject().also { row ->
    columnNames.indices.forEach { index ->
        val value: Any = if (isNull(index)) JSONObject.NULL else when (getType(index)) {
            Cursor.FIELD_TYPE_INTEGER -> getLong(index)
            Cursor.FIELD_TYPE_FLOAT -> getDouble(index)
            Cursor.FIELD_TYPE_BLOB -> JSONObject().put("blobBase64", Base64.encodeToString(getBlob(index), Base64.NO_WRAP))
            else -> getString(index)
        }
        row.put(columnNames[index], value)
    }
}

private fun JSONObject.toContentValues(): ContentValues = ContentValues().also { values ->
    keys().forEach { key ->
        val value = get(key)
        when (value) {
            JSONObject.NULL -> values.putNull(key)
            is Int -> values.put(key, value)
            is Long -> values.put(key, value)
            is Double -> values.put(key, value)
            is Boolean -> values.put(key, value)
            is String -> values.put(key, value)
            is JSONObject -> if (value.has("blobBase64")) values.put(key, Base64.decode(value.getString("blobBase64"), Base64.DEFAULT)) else error("Unsupported object in $key")
            else -> error("Unsupported backup value in $key")
        }
    }
}

private fun safeIdentifier(value: String): String {
    require(value.matches(Regex("[A-Za-z_][A-Za-z0-9_]*"))) { "Unsafe database identifier" }
    return value
}

private fun csvCell(value: String): String = "\"${value.replace("\"", "\"\"")}\""
private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
    .digest(value.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }

private const val BACKUP_FORMAT = "whip-backup"
internal const val ENVELOPE_VERSION = 3
internal const val CURRENT_DATA_MODEL_EPOCH = 5
internal const val BACKUP_DATABASE_VERSION = 22

internal fun validateBackupContract(
    envelopeVersion: Int,
    dataModelEpoch: Int,
    databaseVersion: Int,
) {
    require(dataModelEpoch == CURRENT_DATA_MODEL_EPOCH) {
        if (dataModelEpoch < CURRENT_DATA_MODEL_EPOCH) {
            "This backup is from an older Whip data epoch and cannot be restored after the fresh-start update"
        } else {
            "This backup is from a newer Whip data epoch and requires a newer version of Whip"
        }
    }
    require(envelopeVersion == ENVELOPE_VERSION) {
        "This backup uses unsupported envelope version $envelopeVersion; this build requires version $ENVELOPE_VERSION"
    }
    require(databaseVersion == BACKUP_DATABASE_VERSION) {
        if (databaseVersion < BACKUP_DATABASE_VERSION) {
            "This backup uses old data version $databaseVersion; only current version $BACKUP_DATABASE_VERSION can be restored"
        } else {
            "This backup uses future data version $databaseVersion; this build requires version $BACKUP_DATABASE_VERSION"
        }
    }
}

private val EXPORT_TABLES = listOf(
    "areas",
    "tasks", "task_occurrences", "task_steps", "task_step_states", "task_step_snapshots",
    "unit_definitions", "measurement_definitions", "measurement_entries", "tags",
    "exercises", "exercise_categories", "exercise_category_joins", "gym_machines", "gym_machine_exercise_joins",
    "gym_routines", "routine_days", "routine_exercises", "routine_sets", "training_max_decisions",
    "workout_sessions", "workout_groups", "workout_exercises", "workout_sets",
    "personal_records", "graph_presets", "habits", "habit_timer_sessions", "habit_checklist_items", "habit_logs",
    "habit_checklist_states", "habit_pauses", "habit_skips", "goals", "goal_milestones",
    "goal_completion_snapshots", "goal_elapsed_reset_events",
    "tracks", "track_fields", "track_choice_options", "track_entries", "track_values",
    "link_rules", "link_rule_conditions", "link_condition_choices", "contributions", "trigger_rules", "trigger_rule_conditions", "trigger_condition_choices", "trigger_field_mappings", "trigger_occurrences",
)

private fun checksumPayload(tables: JSONObject, settings: JSONObject?): String =
    tables.toString() + "\n" + settings?.toString().orEmpty()

private fun AppSettings.toJson(includeLocalRecoveryState: Boolean = false): JSONObject = JSONObject()
    .put("setupCompleted", setupCompleted)
    .put("powerMode", powerMode)
    .put("lowPressureMode", lowPressureMode)
    .put("notificationPermissionRequested", notificationPermissionRequested)
    .put("activeAreaScope", activeAreaScope)
    .put("areaOpeningMode", areaOpeningMode.name)
    .put("chosenOpeningAreaScope", chosenOpeningAreaScope)
    .put("hardSetClassifications", JSONArray(hardSetClassifications.toList()))
    .put("categoryAllocationMode", categoryAllocationMode)
    .put("adjustE1rmForEffort", adjustE1rmForEffort)
    .put("includeAssistedInPersonalRecords", includeAssistedInPersonalRecords)
    .put("themeMode", themeMode.name)
    .put("dynamicColor", dynamicColor)
    .put("compactItemLayout", compactItemLayout)
    .put("firstDayOfWeek", firstDayOfWeek.name)
    .put("timeZoneId", timeZoneId ?: JSONObject.NULL)
    .put("dayCutoffMinutes", dayCutoffMinutes)
    .put("massUnitId", massUnitId)
    .put("distanceUnitId", distanceUnitId)
    .put("volumeUnitId", volumeUnitId)
    .put("gymWeightUnitId", gymWeightUnitId)
    .put("numberPrecision", numberPrecision)
    .put("oneRepMaxFormula", oneRepMaxFormula)
    .put("oneRepMaxRepCutoff", oneRepMaxRepCutoff)
    .put("defaultRestSeconds", defaultRestSeconds)
    .put("restTimerPresetSeconds", JSONArray(restTimerPresetSeconds))
    .put("timerSound", timerSound)
    .put("timerVibration", timerVibration)
    .put("keepScreenAwake", keepScreenAwake)
    .put("restTimerAutoStart", restTimerAutoStart)
    .put("showGymRpe", showGymRpe)
    .put("showGymRir", showGymRir)
    .put("showGymTempo", showGymTempo)
    .put("includeWarmupsInGymStats", includeWarmupsInGymStats)
    .putNullable("quietStartMinutes", quietStartMinutes)
    .putNullable("quietEndMinutes", quietEndMinutes)
    .put("homeSections", JSONArray(homeSections.map(HomeSection::name)))
    .put("hiddenHomeSections", JSONArray(hiddenHomeSections.map(HomeSection::name)))
    .put("collapsedHomeSections", JSONArray(collapsedHomeSections.map(HomeSection::name)))
    .put("healthConnectEnabled", healthConnectEnabled)
    .put("healthDataTypes", JSONArray(healthDataTypes.map(HealthDataType::name)))
    .put("healthSyncDays", healthSyncDays)
    .put("reviewPeriod", reviewPeriod.name)
    .put("defaultTaskStepPolicy", defaultTaskStepPolicy.name)
    .put("showAllUpcomingTaskOccurrences", showAllUpcomingTaskOccurrences)
    .put("showHabitsInTaskPlanning", showHabitsInTaskPlanning)
    .put("activeTaskSortMode", activeTaskSortMode)
    .put("defaultHabitWeekStart", defaultHabitWeekStart.name)
    .put("naturalLanguageTaskCapture", naturalLanguageTaskCapture)
    .put("customIdentityEmojis", JSONArray(customIdentityEmojis.map { choice ->
        JSONObject().put("emoji", choice.emoji).put("name", choice.name)
    }))
    .put("savedTaskFilters", JSONArray(savedTaskFilters.map { filter ->
        JSONObject()
            .put("name", filter.name)
            .put("priorities", JSONArray(filter.priorities.map(TaskPriority::name)))
            .put("areaId", filter.areaId ?: JSONObject.NULL)
            .put("pinnedOnly", filter.pinnedOnly)
            .put("tags", JSONArray(filter.tags.toList()))
            .put("requireAllTags", filter.requireAllTags)
            .put("dateMode", filter.dateMode)
            .put("deadlineOnly", filter.deadlineOnly)
            .put("inboxOnly", filter.inboxOnly)
            .put("efforts", JSONArray(filter.efforts.map(TaskEffort::name)))
            .putNullable("maximumDurationMinutes", filter.maximumDurationMinutes)
            .put("textQuery", filter.textQuery)
            .put("destination", filter.destination)
            .put("planningView", filter.planningView)
            .put("sortMode", filter.sortMode)
            .put("sortDescending", filter.sortDescending)
            .put("groupMode", filter.groupMode)
    }))
    .put("homeTaskFilterName", homeTaskFilterName ?: JSONObject.NULL)
    .put("reviewSections", JSONArray(reviewSections.map(ReviewSection::name)))
    .put("gymCompactSetRows", gymCompactSetRows)
    .put("platePresets", JSONArray(platePresets.map { preset ->
        JSONObject().put("name", preset.name).put("unitId", preset.unitId)
            .put("barWeight", preset.barWeight).put("plates", JSONArray(preset.plates))
    }))
    .put("repPrescriptionSchemes", JSONArray(repPrescriptionSchemes.map { scheme ->
        JSONObject()
            .put("id", scheme.id)
            .put("name", scheme.name)
            .put("setCount", scheme.setCount)
            .put("repetitionsMin", scheme.repetitionsMin)
            .put("repetitionsMax", scheme.repetitionsMax)
            .put("classification", scheme.classification.name)
            .putNullable("restSeconds", scheme.restSeconds)
    }))
    .put("trackedGymRecords", JSONArray(trackedGymRecords.map { selection ->
        JSONObject()
            .put("exerciseUuid", selection.exerciseUuid)
            .put("type", selection.type.name)
            .put("secondaryValue", selection.secondaryValue ?: JSONObject.NULL)
            .put("machineProfileUuid", selection.machineProfileUuid ?: JSONObject.NULL)
            .put("position", selection.position)
    }))
    .putNullableLong("focusTimerDeadlineMillis", focusTimerDeadlineMillis)
    .putNullableLong("focusTimerTaskId", focusTimerTaskId)
    .apply {
        if (includeLocalRecoveryState) {
            putNullableLong("healthLastSyncMillis", healthLastSyncMillis)
            put("healthLastSyncCount", healthLastSyncCount.coerceAtLeast(0))
            put("healthConnectDeletionPending", healthConnectDeletionPending)
        }
    }

private fun JSONObject.toAppSettings(): AppSettings = AppSettings(
    setupCompleted = optBoolean("setupCompleted", true),
    powerMode = optBoolean("powerMode", true),
    lowPressureMode = optBoolean("lowPressureMode", false),
    notificationPermissionRequested = optBoolean("notificationPermissionRequested", false),
    activeAreaScope = optString("activeAreaScope", "all"),
    areaOpeningMode = enumValue("areaOpeningMode", AreaOpeningMode.LastUsed),
    chosenOpeningAreaScope = optString("chosenOpeningAreaScope", "all"),
    hardSetClassifications = optJSONArray("hardSetClassifications")?.let { array ->
        (0 until array.length()).mapNotNullTo(mutableSetOf()) { index -> array.optString(index).takeIf(String::isNotBlank) }
    }?.ifEmpty { setOf("Working", "BackOff", "Drop", "Amrap", "Failure") }
        ?: setOf("Working", "BackOff", "Drop", "Amrap", "Failure"),
    categoryAllocationMode = optString("categoryAllocationMode", "Fractional"),
    adjustE1rmForEffort = optBoolean("adjustE1rmForEffort", false),
    includeAssistedInPersonalRecords = optBoolean("includeAssistedInPersonalRecords", false),
    themeMode = enumValue("themeMode", AppThemeMode.System),
    dynamicColor = optBoolean("dynamicColor", true),
    compactItemLayout = optBoolean("compactItemLayout", false),
    firstDayOfWeek = enumValue("firstDayOfWeek", DayOfWeek.MONDAY),
    timeZoneId = optString("timeZoneId").takeUnless { !has("timeZoneId") || isNull("timeZoneId") || runCatching { java.time.ZoneId.of(it) }.isFailure },
    dayCutoffMinutes = optInt("dayCutoffMinutes", 0).coerceIn(0, 1439),
    massUnitId = optString("massUnitId", "kilogram"),
    distanceUnitId = optString("distanceUnitId", "kilometre"),
    volumeUnitId = optString("volumeUnitId", "litre"),
    gymWeightUnitId = optString("gymWeightUnitId", "kilogram"),
    numberPrecision = optInt("numberPrecision", 1).coerceIn(0, 6),
    oneRepMaxFormula = optString("oneRepMaxFormula", "Epley"),
    oneRepMaxRepCutoff = optInt("oneRepMaxRepCutoff", 10).coerceIn(1, 36),
    defaultRestSeconds = optInt("defaultRestSeconds", 120).coerceAtLeast(0),
    restTimerPresetSeconds = normalizeRestTimerPresets(
        optJSONArray("restTimerPresetSeconds")?.let { array ->
            (0 until array.length()).map { index -> array.optInt(index, -1) }
        } ?: DEFAULT_REST_TIMER_PRESET_SECONDS,
    ),
    timerSound = optBoolean("timerSound", true),
    timerVibration = optBoolean("timerVibration", true),
    keepScreenAwake = optBoolean("keepScreenAwake", false),
    restTimerAutoStart = optBoolean("restTimerAutoStart", true),
    showGymRpe = optBoolean("showGymRpe", false),
    showGymRir = optBoolean("showGymRir", false),
    showGymTempo = optBoolean("showGymTempo", true),
    includeWarmupsInGymStats = optBoolean("includeWarmupsInGymStats", false),
    quietStartMinutes = nullableInt("quietStartMinutes"),
    quietEndMinutes = nullableInt("quietEndMinutes"),
    homeSections = enumList("homeSections", HomeSection.entries).takeIf { it.toSet() == HomeSection.entries.toSet() }
        ?: HomeSection.entries,
    hiddenHomeSections = enumSet("hiddenHomeSections", HomeSection.entries),
    collapsedHomeSections = enumSet("collapsedHomeSections", HomeSection.entries),
    healthConnectEnabled = optBoolean("healthConnectEnabled", false),
    healthDataTypes = if (has("healthDataTypes")) {
        enumSet("healthDataTypes", HealthDataType.entries)
    } else if (optBoolean("healthConnectEnabled", false)) {
        // Preserve old enabled backups that predate explicit category scope.
        HealthDataType.entries.toSet()
    } else {
        emptySet()
    },
    healthSyncDays = optInt("healthSyncDays", 30).coerceIn(1, 365),
    healthLastSyncMillis = nullableLong("healthLastSyncMillis"),
    healthLastSyncCount = optInt("healthLastSyncCount", 0).coerceAtLeast(0),
    healthConnectDeletionPending = optBoolean("healthConnectDeletionPending", false),
    reviewPeriod = enumValue("reviewPeriod", ReviewPeriod.Weekly),
    defaultTaskStepPolicy = enumValue("defaultTaskStepPolicy", RepeatStepPolicy.Reset),
    showAllUpcomingTaskOccurrences = optBoolean("showAllUpcomingTaskOccurrences", false),
    showHabitsInTaskPlanning = optBoolean("showHabitsInTaskPlanning", false),
    activeTaskSortMode = optString("activeTaskSortMode", "Smart"),
    defaultHabitWeekStart = enumValue("defaultHabitWeekStart", DayOfWeek.MONDAY),
    naturalLanguageTaskCapture = optBoolean("naturalLanguageTaskCapture", true),
    customIdentityEmojis = optJSONArray("customIdentityEmojis").objects().mapNotNull { value ->
        val emoji = value.optString("emoji").takeIf(String::isNotBlank) ?: return@mapNotNull null
        val name = value.optString("name").takeIf(String::isNotBlank) ?: return@mapNotNull null
        CustomIdentityEmoji(emoji = emoji, name = name)
    },
    savedTaskFilters = optJSONArray("savedTaskFilters").objects().mapNotNull { value ->
        value.optString("name").takeIf(String::isNotBlank)?.let { name ->
            SavedTaskFilter(
                name = name,
                priorities = value.optJSONArray("priorities").enumNames<TaskPriority>(),
                areaId = value.nullableString("areaId"),
                pinnedOnly = value.optBoolean("pinnedOnly", false),
                tags = value.optJSONArray("tags")?.let { array ->
                    (0 until array.length()).mapNotNullTo(linkedSetOf()) { index -> array.optString(index).takeIf(String::isNotBlank) }
                }.orEmpty(),
                requireAllTags = value.optBoolean("requireAllTags", true),
                dateMode = value.optString("dateMode", "Any"),
                deadlineOnly = value.optBoolean("deadlineOnly", false),
                inboxOnly = value.optBoolean("inboxOnly", false),
                efforts = value.optJSONArray("efforts").enumNames<TaskEffort>(),
                maximumDurationMinutes = value.nullableInt("maximumDurationMinutes"),
                textQuery = value.optString("textQuery"),
                destination = value.optString("destination"),
                planningView = value.optString("planningView", "List"),
                sortMode = value.optString("sortMode", "Smart"),
                sortDescending = value.optBoolean("sortDescending", false),
                groupMode = value.optString("groupMode", "None"),
            ).normalizedNavigation()
        }
    },
    homeTaskFilterName = nullableString("homeTaskFilterName"),
    reviewSections = optJSONArray("reviewSections").enumNames<ReviewSection>().ifEmpty { ReviewSection.entries.toSet() },
    gymCompactSetRows = optBoolean("gymCompactSetRows", false),
    platePresets = optJSONArray("platePresets").objects().mapNotNull { value ->
        val name = value.optString("name").takeIf(String::isNotBlank) ?: return@mapNotNull null
        val plates = value.optJSONArray("plates").doubles().filter { it > 0 }
        PlatePreset(name, value.optString("unitId", "kilogram"), value.optDouble("barWeight", 20.0), plates)
            .takeIf { plates.isNotEmpty() }
    },
    repPrescriptionSchemes = optJSONArray("repPrescriptionSchemes").objects().mapNotNull { value ->
        RepPrescriptionScheme(
            id = value.optString("id"),
            name = value.optString("name"),
            setCount = value.optInt("setCount"),
            repetitionsMin = value.optInt("repetitionsMin"),
            repetitionsMax = value.optInt("repetitionsMax"),
            classification = runCatching {
                com.whip.app.domain.WorkoutSetClassification.valueOf(value.optString("classification"))
            }.getOrNull() ?: return@mapNotNull null,
            restSeconds = value.nullableInt("restSeconds"),
        ).takeIf(RepPrescriptionScheme::isValid)
    }.distinctBy(RepPrescriptionScheme::id),
    trackedGymRecords = optJSONArray("trackedGymRecords").objects().mapNotNull { value ->
        TrackedGymRecord(
            exerciseUuid = value.optString("exerciseUuid").takeIf(String::isNotBlank) ?: return@mapNotNull null,
            type = runCatching { PersonalRecordType.valueOf(value.optString("type")) }.getOrNull()
                ?: return@mapNotNull null,
            secondaryValue = value.optDouble("secondaryValue").takeUnless(Double::isNaN),
            machineProfileUuid = value.nullableString("machineProfileUuid"),
            position = value.optInt("position"),
        )
    },
    focusTimerDeadlineMillis = nullableLong("focusTimerDeadlineMillis"),
    focusTimerTaskId = nullableLong("focusTimerTaskId"),
).normalized()

private fun JSONObject.putNullable(key: String, value: Int?): JSONObject = put(key, value ?: JSONObject.NULL)
private fun JSONObject.putNullableLong(key: String, value: Long?): JSONObject = put(key, value ?: JSONObject.NULL)
private fun JSONObject.nullableInt(key: String): Int? = if (!has(key) || isNull(key)) null else optInt(key)
private fun JSONObject.nullableLong(key: String): Long? = if (!has(key) || isNull(key)) null else optLong(key)
private fun JSONObject.nullableDouble(key: String): Double? = if (!has(key) || isNull(key)) null else getDouble(key)
private fun JSONObject.nullableString(key: String): String? = if (!has(key) || isNull(key)) null else optString(key)
private fun JSONObject.nonBlankString(key: String): String? = nullableString(key)?.takeIf(String::isNotBlank)
private fun JSONArray?.objects(): List<JSONObject> = if (this == null) emptyList() else
    (0 until length()).mapNotNull { optJSONObject(it) }
private fun JSONArray?.doubles(): List<Double> = if (this == null) emptyList() else
    (0 until length()).mapNotNull { index -> optDouble(index).takeUnless(Double::isNaN) }
private inline fun <reified T : Enum<T>> JSONArray?.enumNames(): Set<T> = if (this == null) emptySet() else
    (0 until length()).mapNotNullTo(linkedSetOf()) { index -> runCatching { enumValueOf<T>(optString(index)) }.getOrNull() }
private inline fun <reified T : Enum<T>> JSONObject.enumValue(key: String, default: T): T =
    runCatching { enumValueOf<T>(optString(key)) }.getOrDefault(default)
private fun <T : Enum<T>> JSONObject.enumList(key: String, values: List<T>): List<T> {
    val byName = values.associateBy { it.name }
    val array = optJSONArray(key) ?: return emptyList()
    return (0 until array.length()).mapNotNull { byName[array.optString(it)] }
}
private fun <T : Enum<T>> JSONObject.enumSet(key: String, values: List<T>): Set<T> = enumList(key, values).toSet()

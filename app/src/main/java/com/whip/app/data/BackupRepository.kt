package com.whip.app.data

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.util.Base64
import androidx.room.withTransaction
import com.whip.app.core.AppSettings
import com.whip.app.core.AppThemeMode
import com.whip.app.core.HealthDataType
import com.whip.app.core.HomeSection
import com.whip.app.core.ReviewPeriod
import com.whip.app.core.SavedTaskFilter
import com.whip.app.core.normalizedNavigation
import com.whip.app.core.SavedReviewFilter
import com.whip.app.core.PlatePreset
import com.whip.app.core.RepPrescriptionScheme
import com.whip.app.core.SettingsRepository
import com.whip.app.core.DEFAULT_REST_TIMER_PRESET_SECONDS
import com.whip.app.core.normalizeRestTimerPresets
import com.whip.app.domain.RepeatStepPolicy
import com.whip.app.domain.TaskPriority
import com.whip.app.domain.TaskEffort
import java.security.MessageDigest
import java.time.DayOfWeek
import java.time.Instant
import org.json.JSONArray
import org.json.JSONObject

data class BackupPreview(
    val envelopeVersion: Int,
    val databaseVersion: Int,
    val exportedAt: Instant,
    val tableCounts: Map<String, Int>,
    val totalRecords: Int,
    val duplicateStableIds: Int,
    val checksumValid: Boolean,
    val settingsIncluded: Boolean,
    val restoreCompatible: Boolean = databaseVersion == DATABASE_VERSION,
    val compatibilityMessage: String? = null,
)

data class BackupMergeSummary(
    val importedRecords: Int,
    val skippedExistingRecords: Int,
    val settingsKept: Boolean = true,
)

interface BackupRepository {
    suspend fun exportBackup(): String
    suspend fun previewBackup(json: String): BackupPreview
    suspend fun restoreBackup(json: String)
    suspend fun mergeBackup(json: String): BackupMergeSummary
    suspend fun exportTasksCsv(): String
    suspend fun exportHabitsCsv(): String
    suspend fun exportGoalsCsv(): String
    suspend fun exportGymCsv(): String
    suspend fun deleteAllData()
}

class RoomBackupRepository(
    private val database: WhipDatabase,
    private val settingsRepository: SettingsRepository? = null,
    private val areaRepository: AreaRepository? = null,
) : BackupRepository {
    override suspend fun exportBackup(): String = database.withTransaction {
        val db = database.openHelper.readableDatabase
        val tables = JSONObject()
        EXPORT_TABLES.forEach { table ->
            val rows = JSONArray()
            db.query("SELECT * FROM ${safeIdentifier(table)}").use { cursor ->
                while (cursor.moveToNext()) rows.put(cursor.toJsonRow())
            }
            tables.put(table, rows)
        }
        val settings = settingsRepository?.current()?.toJson()
        val payload = checksumPayload(tables, settings)
        JSONObject()
            .put("format", BACKUP_FORMAT)
            .put("envelopeVersion", ENVELOPE_VERSION)
            .put("databaseVersion", DATABASE_VERSION)
            .put("exportedAt", Instant.now().toString())
            .put("checksumSha256", sha256(payload))
            .put("tables", tables)
            .apply { settings?.let { put("settings", it) } }
            .toString(2)
    }

    override suspend fun previewBackup(json: String): BackupPreview {
        val root = parseAndValidate(json)
        val tables = root.getJSONObject("tables")
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
        val settings = root.optJSONObject("settings")
        val envelopeVersion = root.getInt("envelopeVersion")
        val payload = checksumPayload(tables, settings)
        val databaseVersion = root.getInt("databaseVersion")
        return BackupPreview(
            envelopeVersion = envelopeVersion,
            databaseVersion = databaseVersion,
            exportedAt = Instant.parse(root.getString("exportedAt")),
            tableCounts = counts,
            totalRecords = counts.values.sum(),
            duplicateStableIds = duplicates,
            checksumValid = root.getString("checksumSha256") == sha256(payload),
            settingsIncluded = settings != null,
            restoreCompatible = true,
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
        settings?.let { restored -> settingsRepository?.update { restored.toAppSettings() } }
        areaRepository?.ensureDefaultArea()
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
        val summary = database.withTransaction {
            val db = database.openHelper.writableDatabase
            val idMaps = mutableMapOf<String, MutableMap<Long, Long>>()
            val areaIdMap = mutableMapOf<String, String>()
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
                        existing.numericId?.let { target -> sourceNumericId?.let { source -> idMaps.getOrPut(table, ::mutableMapOf)[source] = target } }
                        skipped++
                        continue
                    }
                    val values = row.toContentValues()
                    if (table in setOf("tasks", "habits", "goals")) {
                        values.getAsString("areaId")?.let { sourceId -> values.put("areaId", areaIdMap[sourceId] ?: sourceId) }
                    }
                    remapForeignKeys(values, metadata, idMaps)
                    remapPolymorphicReferences(table, values, idMaps)
                    if (metadata.autoNumericId) values.remove("id")
                    val result = db.insert(safeIdentifier(table), SQLiteDatabase.CONFLICT_IGNORE, values)
                    if (result == -1L) {
                        skipped++
                    } else {
                        imported++
                        if (table == "areas") row.optString("id").takeIf(String::isNotBlank)?.let { areaIdMap[it] = it }
                        if (metadata.autoNumericId && sourceNumericId != null) {
                            idMaps.getOrPut(table, ::mutableMapOf)[sourceNumericId] = result
                        }
                    }
                }
            }
            BackupMergeSummary(imported, skipped)
        }
        areaRepository?.ensureDefaultArea()
        return summary
    }

    override suspend fun exportTasksCsv(): String = queryCsv(
        "SELECT t.id, t.title, t.notes, t.scheduleKind, t.dateEpochDay, t.deadlineEpochDay, t.priority, COALESCE(a.name, '') AS area, t.tagsCsv, t.recurrenceUnit, t.recurrenceInterval, t.recurrenceAnchor, t.reminderOffsetsMinutesCsv, t.completedAtMillis, t.archived FROM tasks t LEFT JOIN areas a ON a.id = t.areaId ORDER BY t.id",
    )

    override suspend fun exportHabitsCsv(): String = queryCsv(
        "SELECT h.uuid AS habitUuid, h.name AS habit, COALESCE(a.name, '') AS area, h.trackingMode, l.uuid AS logUuid, l.localEpochDay, l.value, l.enteredUnitId, l.status, l.note FROM habits h LEFT JOIN areas a ON a.id = h.areaId LEFT JOIN habit_logs l ON l.habitId = h.id ORDER BY h.id, l.timestampMillis",
    )

    override suspend fun exportGoalsCsv(): String = queryCsv(
        "SELECT g.uuid AS goalUuid, g.name AS goal, COALESCE(a.name, '') AS area, g.type, g.unitId, e.id AS entryId, e.localEpochDay, e.enteredValue, e.enteredUnitId, e.status, e.sourceType, e.note FROM goals g LEFT JOIN areas a ON a.id = g.areaId LEFT JOIN metric_entries e ON e.metricId = g.metricId ORDER BY g.id, e.timestampMillis",
    )

    override suspend fun exportGymCsv(): String = queryCsv(
        "SELECT s.uuid AS workoutUuid, s.localEpochDay, s.name AS workout, e.uuid AS exerciseUuid, e.name AS exercise, e.trackingType, e.archived AS exerciseArchived, we.machineProfileUuidSnapshot AS machineScopeUuid, we.machineNameSnapshot AS machine, we.machineConfigurationGroupSnapshot AS machineConfigurationFamily, we.machineConfigurationVersionSnapshot AS machineConfigurationVersion, we.machineConfigurationSnapshot AS machineConfiguration, we.machineLoadTypeSnapshot AS machineLoadType, ws.uuid AS setUuid, ws.position, ws.classification, ws.machineLoadValue, ws.enteredWeight, ws.enteredWeightUnitId, ws.repetitions, ws.enteredDistance, ws.enteredDistanceUnitId, ws.durationSeconds, ws.rpe, ws.rir, ws.tempo, ws.note FROM exercises e LEFT JOIN workout_exercises we ON we.exerciseId = e.id LEFT JOIN workout_sessions s ON s.id = we.sessionId LEFT JOIN workout_sets ws ON ws.workoutExerciseId = we.id AND ws.deletedAtMillis IS NULL ORDER BY e.position, s.startedAtMillis, we.position, ws.position",
    )

    override suspend fun deleteAllData() {
        database.clearAllTables()
        areaRepository?.ensureDefaultArea()
        settingsRepository?.update { AppSettings() }
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
        require(root.optInt("envelopeVersion") == ENVELOPE_VERSION) {
            "This backup uses unsupported envelope version ${root.optInt("envelopeVersion")}; this build requires version $ENVELOPE_VERSION"
        }
        val dbVersion = root.optInt("databaseVersion")
        require(dbVersion == DATABASE_VERSION) {
            "This backup uses unsupported database version $dbVersion; this build requires version $DATABASE_VERSION"
        }
        val tables = root.optJSONObject("tables") ?: error("Backup has no table data")
        val tableNames = tables.keys().asSequence().toSet()
        require(tableNames == EXPORT_TABLES.toSet()) { "Backup table set does not match this build" }
        require(EXPORT_TABLES.all { tables.optJSONArray(it) != null }) { "Backup contains invalid table data" }
        return root
    }
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
        "workout_sessions" -> remap("sourceRoutineId", "gym_routines")
        "workout_exercises" -> {
            remap("machineId", "gym_machines")
            remapCsv("alternativeExerciseIdsCsvSnapshot", "exercises")
        }
        "routine_exercises" -> {
            remap("machineId", "gym_machines")
            remapCsv("alternativeExerciseIdsCsv", "exercises")
        }
        "personal_records" -> remap("machineId", "gym_machines")
        "graph_presets" -> remapCsv("exerciseIdsCsv", "exercises")
        "link_rules" -> {
            sourceParent(values.getAsString("sourceType"))?.let { remap("sourceEntityId", it) }
            if (values.getAsString("sourceType") == "Subtask") remap("sourceItemId", "task_steps")
        }
        "contributions" -> sourceParent(values.getAsString("sourceType"))?.let { remap("sourceEntityId", it) }
        "trigger_rules" -> {
            sourceParent(values.getAsString("sourceType"))?.let { remap("sourceEntityId", it) }
            when (values.getAsString("targetType")) {
                "Habit" -> remap("targetEntityId", "habits")
                "Task" -> remap("targetEntityId", "tasks")
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
private const val ENVELOPE_VERSION = 2
private const val DATABASE_VERSION = 1

private val EXPORT_TABLES = listOf(
    "areas",
    "tasks", "task_occurrences", "task_steps", "task_step_states", "task_step_snapshots",
    "unit_definitions", "metric_definitions", "metric_entries", "tags",
    "exercises", "exercise_categories", "exercise_category_joins", "gym_machines",
    "gym_routines", "routine_days", "routine_exercises", "routine_sets",
    "workout_sessions", "workout_groups", "workout_exercises", "workout_sets",
    "personal_records", "graph_presets", "habits", "habit_checklist_items", "habit_logs",
    "habit_checklist_states", "habit_pauses", "goals", "goal_milestones",
    "link_rules", "contributions", "trigger_rules", "trigger_occurrences",
)

private fun checksumPayload(tables: JSONObject, settings: JSONObject?): String =
    tables.toString() + "\n" + settings?.toString().orEmpty()

private fun AppSettings.toJson(): JSONObject = JSONObject()
    .put("setupCompleted", setupCompleted)
    .put("powerMode", powerMode)
    .put("lowPressureMode", lowPressureMode)
    .put("notificationPermissionRequested", notificationPermissionRequested)
    .put("activeAreaScope", activeAreaScope)
    .put("hardSetClassifications", JSONArray(hardSetClassifications.toList()))
    .put("categoryAllocationMode", categoryAllocationMode)
    .put("adjustE1rmForEffort", adjustE1rmForEffort)
    .put("includeAssistedInPersonalRecords", includeAssistedInPersonalRecords)
    .put("themeMode", themeMode.name)
    .put("dynamicColor", dynamicColor)
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
    .put("defaultHabitWeekStart", defaultHabitWeekStart.name)
    .put("naturalLanguageTaskCapture", naturalLanguageTaskCapture)
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
            .put("groupMode", filter.groupMode)
    }))
    .put("homeTaskFilterName", homeTaskFilterName ?: JSONObject.NULL)
    .put("savedReviewFilters", JSONArray(savedReviewFilters.map { filter ->
        JSONObject().put("name", filter.name)
            .put("sections", JSONArray(filter.sections.map(HomeSection::name)))
    }))
    .put("selectedReviewFilterName", selectedReviewFilterName ?: JSONObject.NULL)
    .put("reviewSections", JSONArray(reviewSections.map(HomeSection::name)))
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
    .putNullableLong("focusTimerDeadlineMillis", focusTimerDeadlineMillis)
    .putNullableLong("focusTimerTaskId", focusTimerTaskId)

private fun JSONObject.toAppSettings(): AppSettings = AppSettings(
    setupCompleted = optBoolean("setupCompleted", true),
    powerMode = optBoolean("powerMode", true),
    lowPressureMode = optBoolean("lowPressureMode", false),
    notificationPermissionRequested = optBoolean("notificationPermissionRequested", false),
    activeAreaScope = optString("activeAreaScope", "all"),
    hardSetClassifications = optJSONArray("hardSetClassifications")?.let { array ->
        (0 until array.length()).mapNotNullTo(mutableSetOf()) { index -> array.optString(index).takeIf(String::isNotBlank) }
    }?.ifEmpty { setOf("Working", "BackOff", "Drop", "Amrap", "Failure") }
        ?: setOf("Working", "BackOff", "Drop", "Amrap", "Failure"),
    categoryAllocationMode = optString("categoryAllocationMode", "Fractional"),
    adjustE1rmForEffort = optBoolean("adjustE1rmForEffort", false),
    includeAssistedInPersonalRecords = optBoolean("includeAssistedInPersonalRecords", false),
    themeMode = enumValue("themeMode", AppThemeMode.System),
    dynamicColor = optBoolean("dynamicColor", true),
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
    healthDataTypes = if (has("healthDataTypes")) enumSet("healthDataTypes", HealthDataType.entries) else HealthDataType.entries.toSet(),
    healthSyncDays = optInt("healthSyncDays", 30).coerceIn(1, 365),
    reviewPeriod = enumValue("reviewPeriod", ReviewPeriod.Weekly),
    defaultTaskStepPolicy = enumValue("defaultTaskStepPolicy", RepeatStepPolicy.Reset),
    showAllUpcomingTaskOccurrences = optBoolean("showAllUpcomingTaskOccurrences", false),
    showHabitsInTaskPlanning = optBoolean("showHabitsInTaskPlanning", false),
    defaultHabitWeekStart = enumValue("defaultHabitWeekStart", DayOfWeek.MONDAY),
    naturalLanguageTaskCapture = optBoolean("naturalLanguageTaskCapture", false),
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
                groupMode = value.optString("groupMode", "None"),
            ).normalizedNavigation()
        }
    },
    homeTaskFilterName = nullableString("homeTaskFilterName"),
    savedReviewFilters = optJSONArray("savedReviewFilters").objects().mapNotNull { value ->
        value.optString("name").takeIf(String::isNotBlank)?.let { name ->
            SavedReviewFilter(name, value.optJSONArray("sections").enumNames<HomeSection>().ifEmpty { HomeSection.entries.toSet() })
        }
    },
    selectedReviewFilterName = nullableString("selectedReviewFilterName"),
    reviewSections = optJSONArray("reviewSections").enumNames<HomeSection>().ifEmpty { HomeSection.entries.toSet() },
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
    focusTimerDeadlineMillis = nullableLong("focusTimerDeadlineMillis"),
    focusTimerTaskId = nullableLong("focusTimerTaskId"),
)

private fun JSONObject.putNullable(key: String, value: Int?): JSONObject = put(key, value ?: JSONObject.NULL)
private fun JSONObject.putNullableLong(key: String, value: Long?): JSONObject = put(key, value ?: JSONObject.NULL)
private fun JSONObject.nullableInt(key: String): Int? = if (!has(key) || isNull(key)) null else optInt(key)
private fun JSONObject.nullableLong(key: String): Long? = if (!has(key) || isNull(key)) null else optLong(key)
private fun JSONObject.nullableString(key: String): String? = if (!has(key) || isNull(key)) null else optString(key)
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

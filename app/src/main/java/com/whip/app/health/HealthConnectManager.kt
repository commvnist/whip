package com.whip.app.health

import android.annotation.SuppressLint
import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.whip.app.core.HealthDataType
import com.whip.app.core.SettingsRepository
import com.whip.app.core.zoneId
import com.whip.app.data.MeasurementRepository
import com.whip.app.domain.MetricEntryStatus
import com.whip.app.domain.MetricSourceType
import com.whip.app.domain.MetricValueKind
import com.whip.app.domain.UnitDimension
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.reflect.KClass

enum class HealthConnectAvailability { Available, InstallOrUpdate, Unsupported }

data class HealthConnectStatus(
    val availability: HealthConnectAvailability = HealthConnectAvailability.Unsupported,
    val grantedPermissions: Set<String> = emptySet(),
    val lastSync: Instant? = null,
    val importedEntries: Int = 0,
    val message: String? = null,
)

data class HealthRecordSnapshot(
    val providerRecordId: String,
    val value: Double,
    val unitId: String,
    val timestamp: Instant,
    val localDate: LocalDate? = null,
    val note: String = "Imported from Health Connect",
)

/** Shared reconciliation seam used by the real client and deterministic fakes.
 * Stable provider IDs make imports, edits, and retries upserts; records absent
 * from the authoritative backfill window are deleted rather than duplicated. */
internal suspend fun reconcileHealthRecords(
    measurements: MeasurementRepository,
    metricId: String,
    sourcePrefix: String,
    records: List<HealthRecordSnapshot>,
    zone: ZoneId,
): Int {
    val retained = linkedSetOf<String>()
    records.forEach { record ->
        val source = "$sourcePrefix${record.providerRecordId}"
        val entryId = "entry-$source"
        retained += entryId
        measurements.record(
            metricId = metricId,
            value = record.value,
            unitId = record.unitId,
            timestamp = record.timestamp,
            localDate = record.localDate,
            zoneId = zone,
            sourceType = MetricSourceType.HealthConnect,
            sourceId = source,
            note = record.note,
            existingEntryId = entryId,
        )
    }
    measurements.deleteSourceEntriesExcept(MetricSourceType.HealthConnect, sourcePrefix, retained)
    return retained.size
}

/**
 * A deliberately narrow Health Connect boundary. Whip requests read-only access to only the
 * record types selected by the user and mirrors those records into the normal metric ledger.
 */
class HealthConnectManager(
    private val context: Context,
    private val measurements: MeasurementRepository,
    private val settingsRepository: SettingsRepository? = null,
) {
    private val providerPackage = "com.google.android.apps.healthdata"

    fun permissionContract() = PermissionController.createRequestPermissionResultContract()

    @SuppressLint("SwitchIntDef")
    fun availability(): HealthConnectAvailability = when (
        HealthConnectClient.getSdkStatus(context, providerPackage)
    ) {
        HealthConnectClient.SDK_AVAILABLE -> HealthConnectAvailability.Available
        HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> HealthConnectAvailability.InstallOrUpdate
        else -> HealthConnectAvailability.Unsupported
    }

    fun requiredPermissions(types: Set<HealthDataType>): Set<String> = buildSet {
        types.forEach { type ->
            add(
                when (type) {
                    HealthDataType.Weight -> HealthPermission.getReadPermission(WeightRecord::class)
                    HealthDataType.Steps -> HealthPermission.getReadPermission(StepsRecord::class)
                    HealthDataType.Distance -> HealthPermission.getReadPermission(DistanceRecord::class)
                    HealthDataType.Hydration -> HealthPermission.getReadPermission(HydrationRecord::class)
                    HealthDataType.Sleep -> HealthPermission.getReadPermission(SleepSessionRecord::class)
                    HealthDataType.Exercise -> HealthPermission.getReadPermission(ExerciseSessionRecord::class)
                },
            )
        }
    }

    suspend fun status(previous: HealthConnectStatus = HealthConnectStatus()): HealthConnectStatus {
        val availability = availability()
        if (availability != HealthConnectAvailability.Available) {
            return previous.copy(availability = availability, grantedPermissions = emptySet())
        }
        return previous.copy(
            availability = availability,
            grantedPermissions = client().permissionController.getGrantedPermissions(),
            message = null,
        )
    }

    suspend fun sync(types: Set<HealthDataType>, days: Int): HealthConnectStatus {
        require(types.isNotEmpty()) { "Choose at least one Health Connect data type" }
        require(availability() == HealthConnectAvailability.Available) { "Health Connect is unavailable" }
        val client = client()
        val granted = client.permissionController.getGrantedPermissions()
        val missing = requiredPermissions(types) - granted
        require(missing.isEmpty()) { "Grant the selected Health Connect permissions first" }

        val zone = settingsRepository?.current()?.zoneId() ?: ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val firstDay = today.minusDays(days.coerceIn(1, 365).toLong() - 1L)
        val start = firstDay.atStartOfDay(zone).toInstant()
        val end = today.plusDays(1).atStartOfDay(zone).toInstant()
        var imported = 0

        if (HealthDataType.Weight in types) imported += syncWeights(client, start, end, zone)
        if (HealthDataType.Hydration in types) imported += syncHydration(client, start, end, zone)
        if (HealthDataType.Sleep in types) imported += syncSleep(client, start, end, zone)
        if (HealthDataType.Exercise in types) imported += syncExercise(client, start, end, zone)
        if (HealthDataType.Steps in types) imported += syncDailySteps(client, firstDay, today, zone)
        if (HealthDataType.Distance in types) imported += syncDailyDistance(client, firstDay, today, zone)

        return HealthConnectStatus(
            availability = HealthConnectAvailability.Available,
            grantedPermissions = granted,
            lastSync = Instant.now(),
            importedEntries = imported,
            message = "Synced $imported Health Connect entries",
        )
    }

    private suspend fun syncWeights(client: HealthConnectClient, start: Instant, end: Instant, zone: ZoneId): Int {
        ensureMetric(WEIGHT_METRIC, "Health weight", MetricValueKind.Decimal, UnitDimension.Mass, "kilogram", 2)
        val records = readAll(client, WeightRecord::class, start, end)
        return reconcileHealthRecords(measurements, WEIGHT_METRIC, WEIGHT_PREFIX, records.map { record ->
            HealthRecordSnapshot(record.metadata.id, record.weight.inKilograms, "kilogram", record.time)
        }, zone)
    }

    private suspend fun syncHydration(client: HealthConnectClient, start: Instant, end: Instant, zone: ZoneId): Int {
        ensureMetric(HYDRATION_METRIC, "Health hydration", MetricValueKind.Decimal, UnitDimension.Volume, "litre", 2)
        val records = readAll(client, HydrationRecord::class, start, end)
        return reconcileHealthRecords(measurements, HYDRATION_METRIC, HYDRATION_PREFIX, records.map { record ->
            HealthRecordSnapshot(record.metadata.id, record.volume.inLiters, "litre", record.startTime)
        }, zone)
    }

    private suspend fun syncSleep(client: HealthConnectClient, start: Instant, end: Instant, zone: ZoneId): Int {
        ensureMetric(SLEEP_METRIC, "Health sleep duration", MetricValueKind.Duration, UnitDimension.Duration, "minute", 0)
        val records = readAll(client, SleepSessionRecord::class, start, end)
        return reconcileHealthRecords(measurements, SLEEP_METRIC, SLEEP_PREFIX, records.map { record ->
            HealthRecordSnapshot(record.metadata.id, Duration.between(record.startTime, record.endTime).toMinutes().toDouble(), "minute", record.endTime)
        }, zone)
    }

    private suspend fun syncExercise(client: HealthConnectClient, start: Instant, end: Instant, zone: ZoneId): Int {
        ensureMetric(EXERCISE_METRIC, "Health exercise duration", MetricValueKind.Duration, UnitDimension.Duration, "minute", 0)
        val records = readAll(client, ExerciseSessionRecord::class, start, end)
        return reconcileHealthRecords(measurements, EXERCISE_METRIC, EXERCISE_PREFIX, records.map { record ->
            HealthRecordSnapshot(
                record.metadata.id,
                Duration.between(record.startTime, record.endTime).toMinutes().toDouble(),
                "minute",
                record.endTime,
                note = record.title?.let { "Health Connect: $it" } ?: "Imported from Health Connect",
            )
        }, zone)
    }

    private suspend fun syncDailySteps(
        client: HealthConnectClient,
        firstDay: LocalDate,
        today: LocalDate,
        zone: ZoneId,
    ): Int {
        ensureMetric(STEPS_METRIC, "Health steps", MetricValueKind.Integer, UnitDimension.Count, "count", 0)
        val retained = mutableSetOf<String>()
        for (date in datesBetweenInclusive(firstDay, today)) {
            val bounds = dayBounds(date, zone)
            val value = client.aggregate(
                AggregateRequest(setOf(StepsRecord.COUNT_TOTAL), TimeRangeFilter.between(bounds.first, bounds.second)),
            )[StepsRecord.COUNT_TOTAL] ?: continue
            val source = "$STEPS_PREFIX$date"
            val entry = stableEntryId(source)
            retained += entry
            measurements.record(
                metricId = STEPS_METRIC, value = value.toDouble(), unitId = "count",
                timestamp = bounds.second.minusMillis(1), localDate = date, zoneId = zone,
                sourceType = MetricSourceType.HealthConnect, sourceId = source,
                note = "Daily Health Connect aggregate", existingEntryId = entry,
            )
        }
        measurements.deleteSourceEntriesExcept(MetricSourceType.HealthConnect, STEPS_PREFIX, retained)
        return retained.size
    }

    private suspend fun syncDailyDistance(
        client: HealthConnectClient,
        firstDay: LocalDate,
        today: LocalDate,
        zone: ZoneId,
    ): Int {
        ensureMetric(DISTANCE_METRIC, "Health distance", MetricValueKind.Decimal, UnitDimension.Distance, "distance_m", 0)
        val retained = mutableSetOf<String>()
        for (date in datesBetweenInclusive(firstDay, today)) {
            val bounds = dayBounds(date, zone)
            val value = client.aggregate(
                AggregateRequest(setOf(DistanceRecord.DISTANCE_TOTAL), TimeRangeFilter.between(bounds.first, bounds.second)),
            )[DistanceRecord.DISTANCE_TOTAL] ?: continue
            val source = "$DISTANCE_PREFIX$date"
            val entry = stableEntryId(source)
            retained += entry
            measurements.record(
                metricId = DISTANCE_METRIC, value = value.inMeters, unitId = "distance_m",
                timestamp = bounds.second.minusMillis(1), localDate = date, zoneId = zone,
                sourceType = MetricSourceType.HealthConnect, sourceId = source,
                note = "Daily Health Connect aggregate", existingEntryId = entry,
            )
        }
        measurements.deleteSourceEntriesExcept(MetricSourceType.HealthConnect, DISTANCE_PREFIX, retained)
        return retained.size
    }

    private suspend fun <T : androidx.health.connect.client.records.Record> readAll(
        client: HealthConnectClient,
        type: KClass<T>,
        start: Instant,
        end: Instant,
    ): List<T> {
        val records = mutableListOf<T>()
        var pageToken: String? = null
        do {
            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = type,
                    timeRangeFilter = TimeRangeFilter.between(start, end),
                    ascendingOrder = true,
                    pageToken = pageToken,
                ),
            )
            records += response.records
            pageToken = response.pageToken
        } while (pageToken != null)
        return records
    }

    private suspend fun ensureMetric(
        id: String,
        name: String,
        kind: MetricValueKind,
        dimension: UnitDimension,
        unit: String,
        precision: Int,
    ) = measurements.ensureMetric(id, name, kind, dimension, unit, precision)

    private fun client() = HealthConnectClient.getOrCreate(context, providerPackage)
    private fun stableEntryId(sourceId: String) = "entry-$sourceId"
    private fun dayBounds(date: LocalDate, zone: ZoneId) =
        date.atStartOfDay(zone).toInstant() to date.plusDays(1).atStartOfDay(zone).toInstant()

    private companion object {
        const val WEIGHT_METRIC = "health-connect-weight"
        const val STEPS_METRIC = "health-connect-steps"
        const val DISTANCE_METRIC = "health-connect-distance"
        const val HYDRATION_METRIC = "health-connect-hydration"
        const val SLEEP_METRIC = "health-connect-sleep"
        const val EXERCISE_METRIC = "health-connect-exercise"
        const val WEIGHT_PREFIX = "health:weight:"
        const val STEPS_PREFIX = "health:steps:"
        const val DISTANCE_PREFIX = "health:distance:"
        const val HYDRATION_PREFIX = "health:hydration:"
        const val SLEEP_PREFIX = "health:sleep:"
        const val EXERCISE_PREFIX = "health:exercise:"
    }
}

private fun datesBetweenInclusive(first: LocalDate, last: LocalDate): Sequence<LocalDate> =
    generateSequence(first) { date -> date.plusDays(1).takeUnless { it.isAfter(last) } }

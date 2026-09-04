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
import androidx.health.connect.client.request.AggregateGroupByPeriodRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.whip.app.core.HealthDataType
import com.whip.app.core.AppSettings
import com.whip.app.core.SettingsRepository
import com.whip.app.core.zoneId
import com.whip.app.data.MeasurementRepository
import com.whip.app.domain.HealthMeasurementContract
import com.whip.app.domain.HealthSourceRecord
import com.whip.app.domain.HealthSourceWindow
import com.whip.app.domain.MeasurementValueKind
import com.whip.app.domain.UnitDimension
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId
import kotlin.reflect.KClass
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class HealthConnectAvailability { Available, InstallOrUpdate, Unsupported }

data class HealthConnectStatus(
    val availability: HealthConnectAvailability = HealthConnectAvailability.Unsupported,
    val grantedPermissions: Set<String> = emptySet(),
    val lastSync: Instant? = null,
    val importedEntries: Int = 0,
    val receiptPersisted: Boolean = true,
    val message: String? = null,
)

data class HealthRecordSnapshot(
    val providerRecordId: String,
    val value: Double,
    val unitId: String,
    val timestamp: Instant,
    val zoneOffsetSeconds: Int? = null,
    val localDate: LocalDate? = null,
    val note: String = "Imported from Health Connect",
)

data class HealthDeletionResult(
    val deletedEntries: Int,
)

data class HealthWindowReadRequest(
    val types: Set<HealthDataType>,
    val start: Instant,
    val end: Instant,
    val firstDay: LocalDate,
    val today: LocalDate,
    val zoneId: ZoneId,
)

/** Deterministic boundary for provider, permission, clock, and race testing. */
interface HealthConnectRuntimeSeam {
    fun availability(): HealthConnectAvailability
    suspend fun grantedPermissions(): Set<String>
    suspend fun readWindows(request: HealthWindowReadRequest): List<HealthSourceWindow>
    fun now(): Instant
}

internal data class HealthRecordPage<T>(val records: List<T>, val nextPageToken: String?)

internal suspend fun <T> collectHealthRecordPages(
    fetch: suspend (pageToken: String?) -> HealthRecordPage<T>,
): List<T> {
    val records = mutableListOf<T>()
    var pageToken: String? = null
    do {
        val page = fetch(pageToken)
        records += page.records
        pageToken = page.nextPageToken
    } while (pageToken != null)
    return records
}

/** Shared reconciliation seam used by the real client and deterministic fakes.
 * Stable provider IDs make imports, edits, and retries upserts; records absent
 * from the authoritative backfill window are deleted rather than duplicated. */
internal suspend fun reconcileHealthRecords(
    measurements: MeasurementRepository,
    window: HealthSourceWindow,
): Int = measurements.reconcileHealthSourceWindows(listOf(window))

/**
 * A deliberately narrow Health Connect boundary. Whip requests read-only access to only the
 * record types selected by the user and mirrors those records into the normal measurement ledger.
 */
class HealthConnectManager(
    private val context: Context,
    private val measurements: MeasurementRepository,
    private val settingsRepository: SettingsRepository? = null,
    private val runtimeSeam: HealthConnectRuntimeSeam? = null,
) {
    private val providerPackage = "com.google.android.apps.healthdata"
    private val mutationMutex = Mutex()

    fun permissionContract() = PermissionController.createRequestPermissionResultContract()

    @SuppressLint("SwitchIntDef")
    fun availability(): HealthConnectAvailability = runtimeSeam?.availability() ?: when (
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

    /** Serializes durable policy changes with provider reads and the Room commit. */
    suspend fun updatePolicyAndConfirm(transform: (AppSettings) -> AppSettings): Boolean =
        withMutationBoundary {
            requireNotNull(settingsRepository) { "Health Connect settings are unavailable" }
                .updateAndConfirm(transform)
        }

    /**
     * Global lock-order root for work that must be atomic against Health reads,
     * reconciliation, deletion, or policy changes. Callers may acquire the
     * reminder state boundary only inside this block; never in the reverse order.
     */
    suspend fun <T> withMutationBoundary(block: suspend () -> T): T =
        mutationMutex.withLock { block() }

    suspend fun status(previous: HealthConnectStatus = HealthConnectStatus()): HealthConnectStatus {
        val availability = availability()
        if (availability != HealthConnectAvailability.Available) {
            return previous.copy(availability = availability, grantedPermissions = emptySet())
        }
        return previous.copy(
            availability = availability,
            grantedPermissions = grantedPermissions(),
            message = null,
        )
    }

    suspend fun sync(types: Set<HealthDataType>, days: Int): HealthConnectStatus = mutationMutex.withLock {
        require(types.isNotEmpty()) { "Choose at least one Health Connect data type" }
        val policyZone = settingsRepository?.current()?.zoneId() ?: ZoneId.systemDefault()
        requirePolicyStillAllows(types, days, policyZone)
        require(availability() == HealthConnectAvailability.Available) { "Health Connect is unavailable" }
        val granted = grantedPermissions()
        val missing = requiredPermissions(types) - granted
        require(missing.isEmpty()) { "Review Android access for the selected Health Connect categories first" }

        val zone = policyZone
        val today = runtimeNow().atZone(zone).toLocalDate()
        val firstDay = today.minusDays(days.coerceIn(1, 365).toLong() - 1L)
        val start = firstDay.atStartOfDay(zone).toInstant()
        val end = today.plusDays(1).atStartOfDay(zone).toInstant()
        val windows = runtimeSeam?.readWindows(
            HealthWindowReadRequest(types, start, end, firstDay, today, zone),
        ) ?: client().let { client ->
            buildList {
                if (HealthDataType.Weight in types) add(prepareWeights(client, start, end, zone))
                if (HealthDataType.Hydration in types) add(prepareHydration(client, start, end, zone))
                if (HealthDataType.Sleep in types) add(prepareSleep(client, start, end, zone))
                if (HealthDataType.Exercise in types) add(prepareExercise(client, start, end, zone))
                if (HealthDataType.Steps in types) add(prepareDailySteps(client, firstDay, today, zone))
                if (HealthDataType.Distance in types) add(prepareDailyDistance(client, firstDay, today, zone))
            }
        }

        requirePolicyStillAllows(types, days, policyZone)
        val currentGranted = grantedPermissions()
        require((requiredPermissions(types) - currentGranted).isEmpty()) {
            "Health Connect access changed before the sync could be saved"
        }
        val synchronized = measurements.reconcileHealthSourceWindows(windows)
        val synchronizedAt = runtimeNow()
        val receiptPersisted = settingsRepository?.updateAndConfirm { current ->
            current.copy(
                healthLastSyncMillis = synchronizedAt.toEpochMilli(),
                healthLastSyncCount = synchronized,
            )
        } ?: true
        HealthConnectStatus(
            availability = HealthConnectAvailability.Available,
            grantedPermissions = currentGranted,
            lastSync = synchronizedAt,
            importedEntries = synchronized,
            receiptPersisted = receiptPersisted,
            message = "Synchronized $synchronized Health Connect records",
        )
    }

    suspend fun deleteImportedData(): HealthDeletionResult = mutationMutex.withLock {
        val settings = requireNotNull(settingsRepository) { "Health Connect settings are unavailable" }
        check(settings.updateAndConfirm { current ->
            current.copy(
                healthConnectEnabled = false,
                healthConnectDeletionPending = true,
                healthLastSyncMillis = null,
                healthLastSyncCount = 0,
            )
        }) { "Local storage could not safely prepare Health Connect deletion" }
        val deleted = measurements.deleteHealthConnectEntries()
        HealthDeletionResult(deleted)
    }

    /** Clears the journal only after callers rebuild every derived local projection. */
    suspend fun completeImportedDataDeletion(): Boolean = mutationMutex.withLock {
        val settings = requireNotNull(settingsRepository) { "Health Connect settings are unavailable" }
        settings.updateAndConfirm { current ->
            current.copy(
                healthConnectEnabled = false,
                healthConnectDeletionPending = false,
                healthLastSyncMillis = null,
                healthLastSyncCount = 0,
            )
        }
    }

    private fun requirePolicyStillAllows(types: Set<HealthDataType>, days: Int, expectedZone: ZoneId) {
        settingsRepository?.current()?.let { settings ->
            require(settings.healthConnectEnabled && !settings.healthConnectDeletionPending) {
                "Health Connect sync is turned off"
            }
            require(settings.healthDataTypes == types && settings.healthSyncDays == days.coerceIn(1, 365)) {
                "Health Connect choices changed; start the sync again"
            }
            require(settings.zoneId() == expectedZone) {
                "The active time zone changed; start the Health Connect sync again"
            }
        }
    }

    private suspend fun grantedPermissions(): Set<String> =
        runtimeSeam?.grantedPermissions() ?: client().permissionController.getGrantedPermissions()

    private fun runtimeNow(): Instant = runtimeSeam?.now() ?: Instant.now()

    private suspend fun prepareWeights(
        client: HealthConnectClient,
        start: Instant,
        end: Instant,
        zone: ZoneId,
    ): HealthSourceWindow {
        val records = readAll(client, WeightRecord::class, start, end)
        return healthWindow(WEIGHT_CONTRACT, WEIGHT_PREFIX, start, end, zone, records.map { record ->
            HealthRecordSnapshot(
                record.metadata.id,
                record.weight.inKilograms,
                "kilogram",
                record.time,
                zoneOffsetSeconds = record.zoneOffset?.totalSeconds,
            )
        })
    }

    private suspend fun prepareHydration(
        client: HealthConnectClient,
        start: Instant,
        end: Instant,
        zone: ZoneId,
    ): HealthSourceWindow {
        val records = readAll(client, HydrationRecord::class, start, end)
        return healthWindow(HYDRATION_CONTRACT, HYDRATION_PREFIX, start, end, zone, records.map { record ->
            HealthRecordSnapshot(
                record.metadata.id,
                record.volume.inLiters,
                "litre",
                record.startTime,
                zoneOffsetSeconds = record.startZoneOffset?.totalSeconds,
            )
        })
    }

    private suspend fun prepareSleep(
        client: HealthConnectClient,
        start: Instant,
        end: Instant,
        zone: ZoneId,
    ): HealthSourceWindow {
        val records = readAll(client, SleepSessionRecord::class, start, end)
        return healthWindow(SLEEP_CONTRACT, SLEEP_PREFIX, start, end, zone, records.map { record ->
            HealthRecordSnapshot(
                record.metadata.id,
                Duration.between(record.startTime, record.endTime).toMinutes().toDouble(),
                "minute",
                record.endTime,
                zoneOffsetSeconds = record.endZoneOffset?.totalSeconds,
            )
        })
    }

    private suspend fun prepareExercise(
        client: HealthConnectClient,
        start: Instant,
        end: Instant,
        zone: ZoneId,
    ): HealthSourceWindow {
        val records = readAll(client, ExerciseSessionRecord::class, start, end)
        return healthWindow(EXERCISE_CONTRACT, EXERCISE_PREFIX, start, end, zone, records.map { record ->
            HealthRecordSnapshot(
                record.metadata.id,
                Duration.between(record.startTime, record.endTime).toMinutes().toDouble(),
                "minute",
                record.endTime,
                zoneOffsetSeconds = record.endZoneOffset?.totalSeconds,
                note = record.title?.let { "Health Connect: $it" } ?: "Imported from Health Connect",
            )
        })
    }

    private suspend fun prepareDailySteps(
        client: HealthConnectClient,
        firstDay: LocalDate,
        today: LocalDate,
        zone: ZoneId,
    ): HealthSourceWindow {
        val groups = client.aggregateGroupByPeriod(
            AggregateGroupByPeriodRequest(
                metrics = setOf(StepsRecord.COUNT_TOTAL),
                timeRangeFilter = TimeRangeFilter.between(
                    firstDay.atStartOfDay(),
                    today.plusDays(1).atStartOfDay(),
                ),
                timeRangeSlicer = Period.ofDays(1),
            ),
        )
        val records = groups.mapNotNull { group ->
            val value = group.result[StepsRecord.COUNT_TOTAL] ?: return@mapNotNull null
            val date = group.startTime.toLocalDate()
            HealthRecordSnapshot(
                providerRecordId = date.toString(), value = value.toDouble(), unitId = "count",
                timestamp = group.endTime.atZone(zone).toInstant().minusMillis(1), localDate = date,
                zoneOffsetSeconds = zone.rules.getOffset(group.endTime.atZone(zone).toInstant().minusMillis(1)).totalSeconds,
                note = "Daily Health Connect aggregate",
            )
        }
        return healthWindow(
            STEPS_CONTRACT,
            STEPS_PREFIX,
            firstDay.atStartOfDay(zone).toInstant(),
            today.plusDays(1).atStartOfDay(zone).toInstant(),
            zone,
            records,
        )
    }

    private suspend fun prepareDailyDistance(
        client: HealthConnectClient,
        firstDay: LocalDate,
        today: LocalDate,
        zone: ZoneId,
    ): HealthSourceWindow {
        val groups = client.aggregateGroupByPeriod(
            AggregateGroupByPeriodRequest(
                metrics = setOf(DistanceRecord.DISTANCE_TOTAL),
                timeRangeFilter = TimeRangeFilter.between(
                    firstDay.atStartOfDay(),
                    today.plusDays(1).atStartOfDay(),
                ),
                timeRangeSlicer = Period.ofDays(1),
            ),
        )
        val records = groups.mapNotNull { group ->
            val value = group.result[DistanceRecord.DISTANCE_TOTAL] ?: return@mapNotNull null
            val date = group.startTime.toLocalDate()
            HealthRecordSnapshot(
                providerRecordId = date.toString(), value = value.inMeters, unitId = "distance_m",
                timestamp = group.endTime.atZone(zone).toInstant().minusMillis(1), localDate = date,
                zoneOffsetSeconds = zone.rules.getOffset(group.endTime.atZone(zone).toInstant().minusMillis(1)).totalSeconds,
                note = "Daily Health Connect aggregate",
            )
        }
        return healthWindow(
            DISTANCE_CONTRACT,
            DISTANCE_PREFIX,
            firstDay.atStartOfDay(zone).toInstant(),
            today.plusDays(1).atStartOfDay(zone).toInstant(),
            zone,
            records,
        )
    }

    private suspend fun <T : androidx.health.connect.client.records.Record> readAll(
        client: HealthConnectClient,
        type: KClass<T>,
        start: Instant,
        end: Instant,
    ): List<T> {
        return collectHealthRecordPages { pageToken ->
            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = type,
                    timeRangeFilter = TimeRangeFilter.between(start, end),
                    ascendingOrder = true,
                    pageToken = pageToken,
                ),
            )
            HealthRecordPage(response.records, response.pageToken)
        }
    }

    private fun client() = HealthConnectClient.getOrCreate(context, providerPackage)
    private fun healthWindow(
        measurement: HealthMeasurementContract,
        sourcePrefix: String,
        startInclusive: Instant,
        endExclusive: Instant,
        zoneId: ZoneId,
        records: List<HealthRecordSnapshot>,
    ) = HealthSourceWindow(
        measurement = measurement,
        sourcePrefix = sourcePrefix,
        startInclusive = startInclusive,
        endExclusive = endExclusive,
        zoneId = zoneId,
        records = records.map { record ->
            HealthSourceRecord(
                providerRecordId = record.providerRecordId,
                value = record.value,
                unitId = record.unitId,
                timestamp = record.timestamp,
                zoneOffsetSeconds = record.zoneOffsetSeconds,
                localDate = record.localDate,
                note = record.note,
            )
        },
    )
    private companion object {
        const val WEIGHT_MEASUREMENT = "health-connect-weight"
        const val STEPS_MEASUREMENT = "health-connect-steps"
        const val DISTANCE_MEASUREMENT = "health-connect-distance"
        const val HYDRATION_MEASUREMENT = "health-connect-hydration"
        const val SLEEP_MEASUREMENT = "health-connect-sleep"
        const val EXERCISE_MEASUREMENT = "health-connect-exercise"
        const val WEIGHT_PREFIX = "health:weight:"
        const val STEPS_PREFIX = "health:steps:"
        const val DISTANCE_PREFIX = "health:distance:"
        const val HYDRATION_PREFIX = "health:hydration:"
        const val SLEEP_PREFIX = "health:sleep:"
        const val EXERCISE_PREFIX = "health:exercise:"
        val WEIGHT_CONTRACT = HealthMeasurementContract(
            WEIGHT_MEASUREMENT, "Health weight", MeasurementValueKind.Decimal, UnitDimension.Mass, "kilogram", 2,
        )
        val STEPS_CONTRACT = HealthMeasurementContract(
            STEPS_MEASUREMENT, "Health steps", MeasurementValueKind.Integer, UnitDimension.Count, "count", 0,
        )
        val DISTANCE_CONTRACT = HealthMeasurementContract(
            DISTANCE_MEASUREMENT, "Health distance", MeasurementValueKind.Decimal, UnitDimension.Distance, "distance_m", 0,
        )
        val HYDRATION_CONTRACT = HealthMeasurementContract(
            HYDRATION_MEASUREMENT, "Health hydration", MeasurementValueKind.Decimal, UnitDimension.Volume, "litre", 2,
        )
        val SLEEP_CONTRACT = HealthMeasurementContract(
            SLEEP_MEASUREMENT, "Health sleep duration", MeasurementValueKind.Duration, UnitDimension.Duration, "minute", 0,
        )
        val EXERCISE_CONTRACT = HealthMeasurementContract(
            EXERCISE_MEASUREMENT,
            "Health exercise duration",
            MeasurementValueKind.Duration,
            UnitDimension.Duration,
            "minute",
            0,
        )
    }
}

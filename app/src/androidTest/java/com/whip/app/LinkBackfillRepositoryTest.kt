package com.whip.app

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.whip.app.core.WhipClock
import com.whip.app.core.WhipIdGenerator
import com.whip.app.data.ContributionEntity
import com.whip.app.data.GoalEntity
import com.whip.app.data.HabitLogEntity
import com.whip.app.data.LinkRuleConditionEntity
import com.whip.app.data.LinkRuleEntity
import com.whip.app.data.MeasurementEntryEntity
import com.whip.app.data.RoomAreaRepository
import com.whip.app.data.RoomGoalRepository
import com.whip.app.data.RoomHabitRepository
import com.whip.app.data.RoomLinkRepository
import com.whip.app.data.RoomMeasurementRepository
import com.whip.app.data.RoomTrackRepository
import com.whip.app.data.TrackEntryEntity
import com.whip.app.data.TrackValueEntity
import com.whip.app.data.WhipDatabase
import com.whip.app.domain.GoalAggregation
import com.whip.app.domain.GoalDraft
import com.whip.app.domain.GoalType
import com.whip.app.domain.HabitDraft
import com.whip.app.domain.HabitLogStatus
import com.whip.app.domain.HabitTrackingMode
import com.whip.app.domain.LinkRuleDraft
import com.whip.app.domain.LinkSourceMeasurement
import com.whip.app.domain.LinkSourceType
import com.whip.app.domain.TrackAggregation
import com.whip.app.domain.TrackCondition
import com.whip.app.domain.TrackConditionOperator
import com.whip.app.domain.TrackDraft
import com.whip.app.domain.TrackEntryDraft
import com.whip.app.domain.TrackFieldDraft
import com.whip.app.domain.TrackFieldType
import com.whip.app.domain.TrackValueDraft
import com.whip.app.domain.UnitDimension
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LinkBackfillRepositoryTest {
    private lateinit var database: WhipDatabase
    private lateinit var measurements: RoomMeasurementRepository
    private lateinit var tracks: RoomTrackRepository
    private lateinit var goals: RoomGoalRepository
    private lateinit var links: RoomLinkRepository
    private lateinit var ids: SequentialIds

    @Before
    fun setUp() = runBlocking {
        // Deliberately omit the integrity-guard callback so one test can model a malformed
        // historical Number row and verify that a read-only preview fails closed around it.
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            WhipDatabase::class.java,
        ).build()
        ids = SequentialIds()
        measurements = RoomMeasurementRepository(database, TestClock, ids)
        RoomAreaRepository(database, TestClock, ids).ensureDefaultArea()
        tracks = RoomTrackRepository(database, TestClock, ids)
        goals = RoomGoalRepository(database, measurements, TestClock, ids)
        links = RoomLinkRepository(database, measurements, TestClock, ids)
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun numberFieldPreviewAccountsForConditionsBlanksMalformedRowsAndCanonicalUnitsWithoutMutation() = runBlocking {
        val goalId = createGoal("Distance target", UnitDimension.Distance, "kilometre")
        val trackId = tracks.create(
            TrackDraft(
                name = "Runs",
                fields = listOf(
                    TrackFieldDraft("Title", TrackFieldType.ShortText, required = true, primary = true),
                    TrackFieldDraft(
                        "Distance",
                        TrackFieldType.Number,
                        dimension = UnitDimension.Distance,
                        unitId = "kilometre",
                        precision = 3,
                    ),
                    TrackFieldDraft("Qualified", TrackFieldType.YesNo),
                ),
            ),
        )
        val projection = requireNotNull(tracks.projection(trackId))
        val title = projection.primaryField
        val distance = projection.fields.single { it.name == "Distance" }
        val qualified = projection.fields.single { it.name == "Qualified" }
        val historyStart = LocalDate.of(2026, 8, 10)

        addEntry(trackId, historyStart.minusDays(1), title.uuid, "Earlier", qualified.uuid, true, distance.uuid, 2.0, "mile")
        addEntry(trackId, historyStart, title.uuid, "Race", qualified.uuid, true, distance.uuid, 1.0, "mile")
        addEntry(trackId, historyStart.plusDays(1), title.uuid, "Unqualified", qualified.uuid, false, distance.uuid, 3.0, "kilometre")
        addEntry(trackId, historyStart.plusDays(2), title.uuid, "Blank", qualified.uuid, true)
        val malformedEntryId = addEntry(
            trackId,
            historyStart.plusDays(3),
            title.uuid,
            "Malformed",
            qualified.uuid,
            true,
            distance.uuid,
            0.25,
            "mile",
        )
        addEntry(trackId, historyStart.plusDays(4), title.uuid, "Training", qualified.uuid, true, distance.uuid, 500.0, "distance_m")

        val malformed = database.trackDao().getValues(malformedEntryId).single { it.fieldId == distance.id }
        database.trackDao().upsertValue(malformed.copy(canonicalNumber = null))
        val before = persistenceSnapshot(trackId)

        val preview = links.previewBackfill(
            LinkRuleDraft(
                name = "Qualified distance",
                sourceType = LinkSourceType.Track,
                sourceEntityId = trackId,
                sourceMeasurement = LinkSourceMeasurement.FieldValue,
                targetGoalId = goalId,
                multiplier = 2.0,
                offset = 10.0,
                retroactiveFrom = historyStart,
                trackAggregation = TrackAggregation.Sum,
                sourceFieldId = distance.id,
                conditions = listOf(TrackCondition(qualified.uuid, TrackConditionOperator.IsYes)),
            ),
        )

        assertEquals(5, preview.scannedEventCount)
        assertEquals(2, preview.eligibleEventCount)
        assertEquals(3, preview.skippedEventCount)
        assertEquals(
            mapOf(
                "Did not match conditions" to 1,
                "Blank source Field" to 2,
            ),
            preview.skippedReasons,
        )
        assertEquals(2, preview.contributionCount)
        assertEquals(4_238.688, preview.totalCanonicalValue, 0.000_001)
        assertEquals(historyStart, preview.firstDate)
        assertEquals(historyStart.plusDays(4), preview.lastDate)
        assertEquals(
            "Source values are converted to kilometre before the multiplier and offset.",
            preview.unitExplanation,
        )
        assertEquals("2 auditable contributions would be created.", preview.targetImpact)
        assertEquals(before, persistenceSnapshot(trackId))
    }

    @Test
    fun scaleFieldPreviewUsesTypedFractionalValuesAndBoundsItsExactImpactWithoutMutation() = runBlocking {
        val goalId = createGoal("Rating target", UnitDimension.Unitless, "unitless")
        val trackId = tracks.create(
            TrackDraft(
                name = "Reviews",
                fields = listOf(
                    TrackFieldDraft("Title", TrackFieldType.ShortText, required = true, primary = true),
                    TrackFieldDraft(
                        "Rating",
                        TrackFieldType.Scale,
                        scaleMin = 1,
                        scaleMax = 5,
                        scaleStep = 0.5,
                    ),
                ),
            ),
        )
        val projection = requireNotNull(tracks.projection(trackId))
        val title = projection.primaryField
        val rating = projection.fields.single { it.name == "Rating" }
        val firstDate = LocalDate.of(2026, 8, 20)
        addScaleEntry(trackId, firstDate, title.uuid, "Good", rating.uuid, 3.5)
        addScaleEntry(trackId, firstDate.plusDays(1), title.uuid, "Unrated")
        addScaleEntry(trackId, firstDate.plusDays(2), title.uuid, "Excellent", rating.uuid, 5.0)
        val before = persistenceSnapshot(trackId)

        val preview = links.previewBackfill(
            LinkRuleDraft(
                name = "Adjusted ratings",
                sourceType = LinkSourceType.Track,
                sourceEntityId = trackId,
                sourceMeasurement = LinkSourceMeasurement.FieldValue,
                targetGoalId = goalId,
                multiplier = 2.0,
                offset = -1.0,
                retroactiveFrom = firstDate,
                trackAggregation = TrackAggregation.Average,
                sourceFieldId = rating.id,
            ),
        )

        assertEquals(3, preview.scannedEventCount)
        assertEquals(2, preview.eligibleEventCount)
        assertEquals(1, preview.skippedEventCount)
        assertEquals(mapOf("Blank source Field" to 1), preview.skippedReasons)
        assertEquals(2, preview.contributionCount)
        assertEquals(15.0, preview.totalCanonicalValue, 0.0)
        assertEquals(firstDate, preview.firstDate)
        assertEquals(firstDate.plusDays(2), preview.lastDate)
        assertEquals("2 auditable contributions would be created.", preview.targetImpact)
        assertEquals(before, persistenceSnapshot(trackId))
    }

    @Test
    fun habitNumericPreviewRespectsHistoryAndRecordedStatusWithoutMutation() = runBlocking {
        val habits = RoomHabitRepository(database, measurements, TestClock, ids)
        val historyStart = LocalDate.of(2026, 8, 15)
        val habitId = habits.create(
            HabitDraft(
                name = "Hydration",
                trackingMode = HabitTrackingMode.Decimal,
                dimension = UnitDimension.Volume,
                unitId = "litre",
                precision = 2,
                targetMin = 2.0,
                startDate = historyStart.minusDays(5),
            ),
        )
        val goalId = createGoal("Hydration target", UnitDimension.Volume, "litre")
        habits.log(habitId, 2.0, date = historyStart.minusDays(1))
        habits.log(habitId, 1.5, date = historyStart)
        habits.log(habitId, 9.0, status = HabitLogStatus.Failed, date = historyStart.plusDays(1))
        habits.log(habitId, 0.5, status = HabitLogStatus.Success, date = historyStart.plusDays(2))
        val before = persistenceSnapshot()

        val preview = links.previewBackfill(
            LinkRuleDraft(
                name = "Recorded hydration",
                sourceType = LinkSourceType.Habit,
                sourceEntityId = habitId,
                sourceMeasurement = LinkSourceMeasurement.NumericValue,
                targetGoalId = goalId,
                multiplier = 0.5,
                retroactiveFrom = historyStart,
            ),
        )

        assertEquals(2, preview.scannedEventCount)
        assertEquals(2, preview.eligibleEventCount)
        assertEquals(0, preview.skippedEventCount)
        assertEquals(emptyMap<String, Int>(), preview.skippedReasons)
        assertEquals(2, preview.contributionCount)
        assertEquals(1_000.0, preview.totalCanonicalValue, 0.0)
        assertEquals(historyStart, preview.firstDate)
        assertEquals(historyStart.plusDays(2), preview.lastDate)
        assertEquals("2 auditable contributions would be created.", preview.targetImpact)
        assertEquals(before, persistenceSnapshot())
    }

    private suspend fun createGoal(name: String, dimension: UnitDimension, unitId: String): Long =
        goals.create(
            GoalDraft(
                name = name,
                type = GoalType.AccumulateTotal,
                dimension = dimension,
                unitId = unitId,
                targetMin = 10_000.0,
                startDate = LocalDate.of(2026, 8, 1),
                aggregation = GoalAggregation.Sum,
            ),
        )

    private suspend fun addEntry(
        trackId: Long,
        date: LocalDate,
        titleFieldUuid: String,
        title: String,
        conditionFieldUuid: String,
        condition: Boolean,
        numberFieldUuid: String? = null,
        number: Double? = null,
        unitId: String? = null,
    ): Long {
        val values = linkedMapOf(
            titleFieldUuid to TrackValueDraft(textValue = title),
            conditionFieldUuid to TrackValueDraft(booleanValue = condition),
        )
        if (numberFieldUuid != null) {
            values[numberFieldUuid] = TrackValueDraft(enteredNumber = number, enteredUnitId = unitId)
        }
        val request = requireNotNull(tracks.prepareEntryCreate(trackId)).request
        return tracks.addEntry(request, TrackEntryDraft(date, values)).entryId
    }

    private suspend fun addScaleEntry(
        trackId: Long,
        date: LocalDate,
        titleFieldUuid: String,
        title: String,
        scaleFieldUuid: String? = null,
        scale: Double? = null,
    ): Long {
        val values = linkedMapOf(titleFieldUuid to TrackValueDraft(textValue = title))
        if (scaleFieldUuid != null) values[scaleFieldUuid] = TrackValueDraft(scaleValue = scale)
        val request = requireNotNull(tracks.prepareEntryCreate(trackId)).request
        return tracks.addEntry(request, TrackEntryDraft(date, values)).entryId
    }

    private suspend fun persistenceSnapshot(trackId: Long? = null): PersistenceSnapshot {
        val entries = trackId?.let { database.trackDao().getEntries(it) }.orEmpty()
        return PersistenceSnapshot(
            rules = database.linkDao().getRules(),
            ruleConditions = database.linkDao().getAllRuleConditions(),
            contributions = database.linkDao().observeContributionsSnapshot(),
            goals = database.goalDao().getAllGoals(),
            measurementEntries = database.measurementDao().getAllEntries(),
            habitLogs = database.habitDao().getAllLogs(),
            trackEntries = entries,
            trackValues = entries.takeIf { it.isNotEmpty() }
                ?.let { database.trackDao().getValuesForEntries(it.map(TrackEntryEntity::id)) }
                .orEmpty(),
        )
    }

    private data class PersistenceSnapshot(
        val rules: List<LinkRuleEntity>,
        val ruleConditions: List<LinkRuleConditionEntity>,
        val contributions: List<ContributionEntity>,
        val goals: List<GoalEntity>,
        val measurementEntries: List<MeasurementEntryEntity>,
        val habitLogs: List<HabitLogEntity>,
        val trackEntries: List<TrackEntryEntity>,
        val trackValues: List<TrackValueEntity>,
    )

    private object TestClock : WhipClock {
        override fun now(): Instant = Instant.parse("2026-08-23T12:00:00Z")
        override fun zoneId(): ZoneId = ZoneId.of("UTC")
        override fun today(zoneId: ZoneId): LocalDate = LocalDate.of(2026, 8, 23)
    }

    private class SequentialIds : WhipIdGenerator {
        private val next = AtomicInteger()
        override fun nextId(): String = "link-preview-${next.incrementAndGet()}"
    }
}

package com.whip.app

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.whip.app.core.WhipClock
import com.whip.app.core.WhipIdGenerator
import com.whip.app.data.RoomAreaRepository
import com.whip.app.data.RoomGoalRepository
import com.whip.app.data.RoomHabitRepository
import com.whip.app.data.RoomLinkRepository
import com.whip.app.data.RoomMeasurementRepository
import com.whip.app.data.RoomTaskRepository
import com.whip.app.data.RoomTrackRepository
import com.whip.app.data.WhipDatabase
import com.whip.app.domain.GoalAggregation
import com.whip.app.domain.GoalDraft
import com.whip.app.domain.GoalConsistencyPeriod
import com.whip.app.domain.GoalType
import com.whip.app.domain.HabitDraft
import com.whip.app.domain.HabitTrackingMode
import com.whip.app.domain.LinkRuleDraft
import com.whip.app.domain.LinkSourceMetric
import com.whip.app.domain.LinkSourceType
import com.whip.app.domain.TrackAggregation
import com.whip.app.domain.TrackChoiceOptionDraft
import com.whip.app.domain.TrackCondition
import com.whip.app.domain.TrackConditionOperator
import com.whip.app.domain.TrackDraft
import com.whip.app.domain.TrackEntryDraft
import com.whip.app.domain.TrackFieldDraft
import com.whip.app.domain.TrackFieldType
import com.whip.app.domain.TrackValueDraft
import com.whip.app.domain.TriggerAction
import com.whip.app.domain.TriggerFieldMapping
import com.whip.app.domain.TriggerOutcome
import com.whip.app.domain.TriggerRuleDraft
import com.whip.app.domain.TriggerSourceProperty
import com.whip.app.domain.TriggerTargetType
import com.whip.app.domain.UnitDimension
import com.whip.app.domain.aggregate
import com.whip.app.domain.ScheduledTask
import com.whip.app.domain.TaskDraft
import com.whip.app.domain.TaskStepDraft
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TrackRepositoryTest {
    private lateinit var database: WhipDatabase
    private lateinit var tracks: RoomTrackRepository
    private lateinit var measurements: RoomMeasurementRepository
    private lateinit var goals: RoomGoalRepository
    private lateinit var habits: RoomHabitRepository
    private lateinit var tasks: RoomTaskRepository
    private lateinit var links: RoomLinkRepository

    @Before fun setUp() = runBlocking {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), WhipDatabase::class.java)
            .addCallback(WhipDatabase.integrityGuardCallback)
            .build()
        val ids = TestIds()
        RoomAreaRepository(database, TestClock, ids).ensureDefaultArea()
        measurements = RoomMeasurementRepository(database, TestClock, ids)
        goals = RoomGoalRepository(database, measurements, TestClock, ids)
        habits = RoomHabitRepository(database, measurements, TestClock, ids)
        tasks = RoomTaskRepository(database, TestClock)
        tracks = RoomTrackRepository(database, TestClock, ids)
        links = RoomLinkRepository(database, measurements, TestClock, ids)
    }

    @After fun tearDown() = database.close()

    @Test fun projectionsNeverExposeATrackBeforeItsRequiredFields() = runBlocking {
        var partialProjectionObserved = false
        val collector = launch {
            tracks.projections.collect { rows ->
                if (rows.any { it.fields.isEmpty() }) partialProjectionObserved = true
            }
        }
        yield()

        val id = tracks.create(booksDraft())
        tracks.projections.first { rows -> rows.any { it.track.id == id } }
        collector.cancelAndJoin()

        assertFalse(partialProjectionObserved)
    }

    @Test fun identityOnlyEditSkipsSchemaAndAutomationInvalidation() = runBlocking {
        val id = tracks.create(booksDraft())
        val before = requireNotNull(tracks.projection(id))
        val entryId = tracks.addEntry(
            id,
            TrackEntryDraft(
                TestClock.today(),
                mapOf(before.primaryField.uuid to TrackValueDraft(textValue = "Fast save")),
            ),
        )

        val impact = tracks.update(id, before.toDraft().copy(icon = "⚡"))

        assertFalse(impact.automationInputsChanged)
        assertEquals("⚡", requireNotNull(tracks.projection(id)).track.icon)
        assertEquals(setOf(entryId), tracks.searchEntryIds(id, "Fast save"))
    }

    @Test fun fractionalScaleRoundTripsThroughStorageCsvAnalyticsAndSchemaEdits() = runBlocking {
        val trackId = tracks.create(
            TrackDraft(
                name = "Films",
                fields = listOf(
                    TrackFieldDraft("Title", TrackFieldType.ShortText, primary = true),
                    TrackFieldDraft(
                        "Rating",
                        TrackFieldType.Scale,
                        showInList = true,
                        scaleMin = 1,
                        scaleMax = 5,
                        scaleStep = 0.5,
                        scaleLowLabel = "Poor",
                        scaleHighLabel = "Excellent",
                    ),
                ),
            ),
        )
        var projection = requireNotNull(tracks.projection(trackId))
        val rating = projection.fields.single { it.name == "Rating" }
        tracks.addEntry(
            trackId,
            TrackEntryDraft(
                entryDate = TestClock.today(),
                values = mapOf(
                    projection.primaryField.uuid to TrackValueDraft(textValue = "Arrival"),
                    rating.uuid to TrackValueDraft(scaleValue = 3.5),
                ),
            ),
        )

        projection = requireNotNull(tracks.projection(trackId))
        val storedRating = projection.fields.single { it.name == "Rating" }
        assertEquals(0.5, storedRating.scaleStep, 0.0)
        assertEquals(3.5, projection.entries.single().value(storedRating.id)?.scaleValue ?: 0.0, 0.0)
        assertEquals(3.5, projection.aggregate(TrackAggregation.Average, storedRating.uuid).value ?: 0.0, 0.0)
        assertTrue(tracks.exportCsv(trackId).contains("\"3.5\""))

        val habitId = habits.create(
            HabitDraft("Watch a Film", trackingMode = HabitTrackingMode.CheckOff, startDate = TestClock.today()),
        )
        val triggerId = links.createTrigger(
            TriggerRuleDraft(
                name = "Rate Watched Film",
                sourceType = LinkSourceType.Habit,
                sourceEntityId = habitId,
                targetType = TriggerTargetType.Track,
                targetEntityId = trackId,
                action = TriggerAction.PromptTrackEntry,
                mappings = listOf(
                    TriggerFieldMapping(
                        targetFieldId = storedRating.id,
                        sourceProperty = TriggerSourceProperty.Constant,
                        constantValue = TrackValueDraft(scaleValue = 3.5),
                    ),
                ),
            ),
        )
        habits.setCheckOff(habitId, TestClock.today(), true)
        links.rebuildAll()
        val occurrence = links.triggerOccurrences.first().single { it.triggerRuleId == triggerId }
        assertEquals(3.5, links.trackPromptDraft(occurrence.id).values.getValue(storedRating.uuid).scaleValue ?: 0.0, 0.0)

        val integerOnly = projection.toDraft().copy(
            fields = projection.toDraft().fields.map { field ->
                if (field.id == storedRating.id) field.copy(scaleStep = 1.0) else field
            },
        )
        val rejected = runCatching { tracks.update(trackId, integerOnly) }
        assertTrue(rejected.isFailure)
        assertTrue(rejected.exceptionOrNull()?.message.orEmpty().contains("existing Scale value"))

        val quarterPoint = integerOnly.copy(
            fields = integerOnly.fields.map { field ->
                if (field.id == storedRating.id) field.copy(scaleStep = 0.25) else field
            },
        )
        tracks.update(trackId, quarterPoint)
        assertEquals(0.25, requireNotNull(tracks.projection(trackId)).fields.single { it.id == storedRating.id }.scaleStep, 0.0)
    }

    @Test fun crudSearchCsvAndDestructiveChoiceConfirmationPreserveIdentity() = runBlocking {
        val id = tracks.create(booksDraft())
        var projection = requireNotNull(tracks.projection(id))
        val title = projection.primaryField
        val genre = projection.fields.first { it.name == "Genre" }
        val rating = projection.fields.first { it.name == "Rating" }
        val history = projection.optionsFor(genre.id).first { it.label == "History" }
        val entryId = tracks.addEntry(
            id,
            TrackEntryDraft(
                TestClock.today(),
                mapOf(
                    title.uuid to TrackValueDraft(textValue = "The Dispossessed, annotated"),
                    genre.uuid to TrackValueDraft(choiceOptionUuid = history.uuid),
                    rating.uuid to TrackValueDraft(enteredNumber = 4.5, enteredUnitId = "unitless"),
                ),
            ),
        )
        assertEquals(setOf(entryId), tracks.searchEntryIds(id, "Dispossessed annotated"))
        assertTrue(tracks.exportCsv(id).contains("\"The Dispossessed, annotated\""))

        projection = requireNotNull(tracks.projection(id))
        val existingDraft = projection.toDraft()
        val draftWithoutHistory = existingDraft.copy(
            fields = existingDraft.fields.map { field ->
                if (field.id == genre.id) field.copy(options = field.options.filterNot { it.id == history.id }) else field
            },
        )
        val rejected = runCatching { tracks.update(id, draftWithoutHistory) }
        assertTrue(rejected.isFailure)
        assertTrue(rejected.exceptionOrNull()?.message.orEmpty().contains("Confirm"))
        tracks.update(id, draftWithoutHistory, confirmedOptionValueDeletionIds = setOf(history.id))
        projection = requireNotNull(tracks.projection(id))
        assertEquals("The Dispossessed, annotated", projection.primaryText(projection.entries.single()))
        assertEquals(null, projection.entries.single().value(genre.id))

        val deleted = requireNotNull(tracks.deleteEntry(entryId))
        assertTrue(requireNotNull(tracks.projection(id)).entries.isEmpty())
        val restored = tracks.restoreEntry(deleted)
        assertNotNull(tracks.projection(id)?.entries?.firstOrNull { it.entry.id == restored })
    }

    @Test fun compositeEntryIdentityAndInlineLabelRoundTripWithoutSilentClearing() = runBlocking {
        val trackId = tracks.create(
            TrackDraft(
                name = "Films",
                fields = listOf(
                    TrackFieldDraft("Title", TrackFieldType.ShortText, primary = true),
                    TrackFieldDraft("Director", TrackFieldType.ShortText, primary = true, showInList = true),
                    TrackFieldDraft("Year", TrackFieldType.Number, primary = true, showInList = true, dimension = UnitDimension.Count, unitId = "count", precision = 0),
                ),
            ),
        )
        var projection = requireNotNull(tracks.projection(trackId))
        val title = projection.fields.first { it.name == "Title" }
        val director = projection.fields.first { it.name == "Director" }
        val year = projection.fields.first { it.name == "Year" }
        val entryId = tracks.addEntry(
            trackId,
            TrackEntryDraft(
                TestClock.today(),
                mapOf(
                    title.uuid to TrackValueDraft(textValue = "Dune"),
                    director.uuid to TrackValueDraft(textValue = "Denis Villeneuve"),
                    year.uuid to TrackValueDraft(enteredNumber = 2021.0, enteredUnitId = "count"),
                ),
            ),
        )
        projection = requireNotNull(tracks.projection(trackId))
        assertEquals(3, projection.primaryFields.size)
        assertEquals(2, projection.fields.count { it.showInList })
        assertEquals("Dune · Denis Villeneuve · 2021 count", projection.primaryText(projection.entries.single { it.entry.id == entryId }))

        tracks.update(trackId, projection.toDraft())
        projection = requireNotNull(tracks.projection(trackId))
        assertEquals(3, projection.primaryFields.size)
        assertTrue(projection.fields.first { it.name == "Director" }.showInList)
        assertTrue(projection.fields.first { it.name == "Year" }.showInList)
    }

    @Test fun filteredTrackContributionsReconcileIdempotentlyAcrossEditDeleteRestoreAndOverride() = runBlocking {
        val trackId = tracks.create(booksDraft())
        var projection = requireNotNull(tracks.projection(trackId))
        val title = projection.primaryField
        val genre = projection.fields.first { it.name == "Genre" }
        val history = projection.optionsFor(genre.id).first { it.label == "History" }
        val fiction = projection.optionsFor(genre.id).first { it.label == "Fiction" }
        fun draft(name: String, option: String) = TrackEntryDraft(TestClock.today(), mapOf(title.uuid to TrackValueDraft(textValue = name), genre.uuid to TrackValueDraft(choiceOptionUuid = option)))
        val first = tracks.addEntry(trackId, draft("A", history.uuid))
        val second = tracks.addEntry(trackId, draft("B", fiction.uuid))
        val goalId = goals.create(GoalDraft("Read History", type = GoalType.ReachValue, dimension = UnitDimension.Count, unitId = "count", targetMin = 10.0, startDate = TestClock.today(), aggregation = GoalAggregation.Sum))
        val ruleId = links.createRule(
            LinkRuleDraft(
                "History Books",
                sourceType = LinkSourceType.Track,
                sourceEntityId = trackId,
                sourceMetric = LinkSourceMetric.EntryCount,
                targetGoalId = goalId,
                retroactiveFrom = TestClock.today(),
                trackAggregation = TrackAggregation.CountMatchingEntries,
                conditions = listOf(TrackCondition(genre.uuid, TrackConditionOperator.Is, choiceOptionUuids = setOf(history.uuid))),
            ),
            commitBackfill = true,
        )
        assertEquals(1, links.contributions.first().size)
        tracks.updateEntry(second, draft("B", history.uuid))
        links.rebuildRule(ruleId)
        assertEquals(2, links.contributions.first().size)
        links.rebuildRule(ruleId)
        assertEquals(2, links.contributions.first().size)
        val contribution = links.contributions.first().first { it.sourceEventId.contains("entry:") }
        links.setContributionOverride(contribution.id, 3.0)
        tracks.updateEntry(first, draft("A revised", history.uuid))
        links.rebuildRule(ruleId)
        assertEquals(3.0, links.contributions.first().first { it.id == contribution.id }.overrideValue ?: -1.0, 0.0)
        val deleted = requireNotNull(tracks.deleteEntry(second))
        links.rebuildRule(ruleId)
        assertEquals(1, links.contributions.first().size)
        tracks.restoreEntry(deleted)
        links.rebuildRule(ruleId)
        assertEquals(2, links.contributions.first().size)
    }

    @Test fun replacingAChoiceRetargetsEntriesAndAutomationConditionsWithoutChangingTheirIdentity() = runBlocking {
        val trackId = tracks.create(booksDraft())
        val projection = requireNotNull(tracks.projection(trackId))
        val genre = projection.fields.first { it.name == "Genre" }
        val history = projection.optionsFor(genre.id).first { it.label == "History" }
        val fiction = projection.optionsFor(genre.id).first { it.label == "Fiction" }
        tracks.addEntry(
            trackId,
            TrackEntryDraft(
                TestClock.today(),
                mapOf(
                    projection.primaryField.uuid to TrackValueDraft(textValue = "A Choice in Motion"),
                    genre.uuid to TrackValueDraft(choiceOptionUuid = history.uuid),
                ),
            ),
        )
        val goalId = goals.create(
            GoalDraft(
                name = "Read Books",
                type = GoalType.ReachValue,
                dimension = UnitDimension.Count,
                unitId = "count",
                targetMin = 10.0,
                startDate = TestClock.today(),
                aggregation = GoalAggregation.Sum,
            ),
        )
        links.createRule(
            LinkRuleDraft(
                name = "History Books",
                sourceType = LinkSourceType.Track,
                sourceEntityId = trackId,
                sourceMetric = LinkSourceMetric.EntryCount,
                targetGoalId = goalId,
                retroactiveFrom = TestClock.today(),
                trackAggregation = TrackAggregation.CountMatchingEntries,
                conditions = listOf(TrackCondition(genre.uuid, TrackConditionOperator.Is, choiceOptionUuids = setOf(history.uuid))),
            ),
            commitBackfill = true,
        )
        val withoutHistory = projection.toDraft().copy(
            fields = projection.toDraft().fields.map { field ->
                if (field.id == genre.id) field.copy(options = field.options.filterNot { it.id == history.id }) else field
            },
        )

        tracks.update(trackId, withoutHistory, optionReplacementIds = mapOf(history.id to fiction.id))

        val updated = requireNotNull(tracks.projection(trackId))
        assertEquals(fiction.id, updated.entries.single().value(genre.id)?.choiceOptionId)
        assertEquals(setOf(fiction.uuid), links.rules.first().single().conditions.single().choiceOptionUuids)
    }

    @Test fun habitCapturePromptIsDurableMappedRemindableAndSurvivesSourceUndoAfterFulfillment() = runBlocking {
        val trackId = tracks.create(booksDraft())
        val projection = requireNotNull(tracks.projection(trackId))
        val habitId = habits.create(HabitDraft("Read", trackingMode = HabitTrackingMode.CheckOff, startDate = TestClock.today()))
        val ruleId = links.createTrigger(
            TriggerRuleDraft(
                name = "Capture Finished Book",
                sourceType = LinkSourceType.Habit,
                sourceEntityId = habitId,
                targetType = TriggerTargetType.Track,
                targetEntityId = trackId,
                action = TriggerAction.PromptTrackEntry,
                mappings = listOf(
                    TriggerFieldMapping(projection.primaryField.id, TriggerSourceProperty.Name),
                    TriggerFieldMapping(projection.fields.first { it.name == "Rating" }.id, TriggerSourceProperty.Constant, TrackValueDraft(enteredNumber = 5.0, enteredUnitId = "unitless")),
                ),
            ),
        )
        habits.setCheckOff(habitId, TestClock.today(), true)
        links.rebuildAll()
        val occurrence = links.triggerOccurrences.first().single { it.triggerRuleId == ruleId }
        val prefill = links.trackPromptDraft(occurrence.id)
        assertTrue(prefill.values.getValue(projection.primaryField.uuid).textValue.orEmpty().contains("Read"))
        val remindAt = TestClock.now().plusSeconds(3600)
        links.remindTriggerOccurrence(occurrence.id, remindAt)
        assertEquals(remindAt, links.triggerOccurrences.first().single().remindAt)
        val entryId = links.fulfillTrackPrompt(occurrence.id, prefill)
        assertEquals(entryId, links.triggerOccurrences.first().single().fulfilledEntryId)
        assertEquals(prefill.sourceExplanation, requireNotNull(tracks.projection(trackId)).entries.single().entry.sourceExplanation)
        habits.setCheckOff(habitId, TestClock.today(), false)
        links.rebuildAll()
        assertEquals(entryId, links.triggerOccurrences.first().single().fulfilledEntryId)
        assertFalse(requireNotNull(tracks.projection(trackId)).entries.isEmpty())

        links.deleteTrigger(ruleId)
        val preserved = requireNotNull(tracks.projection(trackId)).entries.single().entry
        assertEquals(null, preserved.sourceOccurrenceId)
        assertTrue(preserved.sourceExplanation.isNotBlank())
    }

    @Test fun archivedCaptureTargetSuspendsPromptsAndRestoreKeepsTheUsersEnabledChoice() = runBlocking {
        val trackId = tracks.create(booksDraft())
        val habitId = habits.create(HabitDraft("Read", trackingMode = HabitTrackingMode.CheckOff, startDate = TestClock.today()))
        val ruleId = links.createTrigger(
            TriggerRuleDraft(
                name = "Capture Book",
                sourceType = LinkSourceType.Habit,
                sourceEntityId = habitId,
                targetType = TriggerTargetType.Track,
                targetEntityId = trackId,
                action = TriggerAction.PromptTrackEntry,
            ),
        )
        tracks.setArchived(trackId, true)
        habits.setCheckOff(habitId, TestClock.today(), true)
        links.rebuildAll()
        assertTrue(links.triggerOccurrences.first().isEmpty())

        tracks.setArchived(trackId, false)
        links.rebuildAll()
        assertEquals(1, links.triggerOccurrences.first().size)

        val rule = links.triggerRules.first().single { it.id == ruleId }
        links.updateTrigger(ruleId, rule.toDraft(enabled = false))
        tracks.setArchived(trackId, true)
        tracks.setArchived(trackId, false)
        links.rebuildAll()
        assertTrue(links.triggerOccurrences.first().isEmpty())
    }

    @Test fun recordedHabitAndSpecificCompletedSubtaskCreateDistinctCapturePrompts() = runBlocking {
        val trackId = tracks.create(booksDraft())
        val habitId = habits.create(
            HabitDraft(
                name = "Pages Read",
                trackingMode = HabitTrackingMode.Count,
                startDate = TestClock.today(),
            ),
        )
        val recordedRuleId = links.createTrigger(
            TriggerRuleDraft(
                name = "Capture Reading Log",
                sourceType = LinkSourceType.Habit,
                sourceEntityId = habitId,
                outcome = TriggerOutcome.Recorded,
                targetType = TriggerTargetType.Track,
                targetEntityId = trackId,
                action = TriggerAction.PromptTrackEntry,
            ),
        )
        habits.log(habitId, 12.0)

        val taskId = tasks.create(
            TaskDraft(
                title = "Publish Notes",
                autoCompleteFromSteps = false,
                steps = listOf(TaskStepDraft(title = "Write Review", position = 0)),
            ),
        )
        val step = tasks.steps.first().single { it.taskId == taskId }
        val subtaskRuleId = links.createTrigger(
            TriggerRuleDraft(
                name = "Capture Written Review",
                sourceType = LinkSourceType.Subtask,
                sourceEntityId = taskId,
                sourceItemId = step.id,
                outcome = TriggerOutcome.Completed,
                targetType = TriggerTargetType.Track,
                targetEntityId = trackId,
                action = TriggerAction.PromptTrackEntry,
            ),
        )
        val task = requireNotNull(tasks.getTask(taskId))
        tasks.setStepCompleted(ScheduledTask(task, originalDate = null, scheduledDate = null), step.id, true)

        links.rebuildAll()
        val occurrences = links.triggerOccurrences.first()
        assertEquals(1, occurrences.count { it.triggerRuleId == recordedRuleId })
        assertEquals(1, occurrences.count { it.triggerRuleId == subtaskRuleId })
        assertTrue(links.trackPromptDraft(occurrences.single { it.triggerRuleId == subtaskRuleId }.id).sourceExplanation.contains("Write Review"))
    }

    @Test fun matchingTrackEntryCanCheckOffHabitAndEntryEditReconcilesIdempotently() = runBlocking {
        val trackId = tracks.create(booksDraft())
        val projection = requireNotNull(tracks.projection(trackId))
        val rating = projection.fields.first { it.name == "Rating" }
        val habitId = habits.create(HabitDraft("Review Great Books", trackingMode = HabitTrackingMode.CheckOff, startDate = TestClock.today()))
        links.createTrigger(
            TriggerRuleDraft(
                name = "Review Highly Rated Book",
                sourceType = LinkSourceType.Track,
                sourceEntityId = trackId,
                targetType = TriggerTargetType.Habit,
                targetEntityId = habitId,
                action = TriggerAction.CheckOffHabit,
                conditions = listOf(TrackCondition(rating.uuid, TrackConditionOperator.AtLeast, numberValue = 4.0)),
            ),
        )
        val entryId = tracks.addEntry(
            trackId,
            TrackEntryDraft(
                TestClock.today(),
                mapOf(
                    projection.primaryField.uuid to TrackValueDraft(textValue = "Excellent Book"),
                    rating.uuid to TrackValueDraft(enteredNumber = 5.0, enteredUnitId = "unitless"),
                ),
            ),
        )
        links.rebuildAll()
        assertEquals(1, habits.logs.first().size)
        links.rebuildAll()
        assertEquals(1, habits.logs.first().size)

        tracks.updateEntry(
            entryId,
            TrackEntryDraft(
                TestClock.today(),
                mapOf(
                    projection.primaryField.uuid to TrackValueDraft(textValue = "Excellent Book"),
                    rating.uuid to TrackValueDraft(enteredNumber = 2.0, enteredUnitId = "unitless"),
                ),
            ),
        )
        links.rebuildAll()
        assertTrue(habits.logs.first().isEmpty())
    }

    @Test fun largeTrackHistoryIsReadThroughStableBoundedRepositoryPages() = runBlocking {
        val trackId = tracks.create(booksDraft())
        val projection = requireNotNull(tracks.projection(trackId))
        repeat(205) { index ->
            tracks.addEntry(
                trackId,
                TrackEntryDraft(
                    entryDate = TestClock.today().minusDays((index % 31).toLong()),
                    values = mapOf(projection.primaryField.uuid to TrackValueDraft(textValue = "Book $index")),
                ),
            )
        }

        val first = tracks.entryPage(trackId, offset = 0, limit = 100)
        val second = tracks.entryPage(trackId, offset = first.nextOffset, limit = 100)
        val third = tracks.entryPage(trackId, offset = second.nextOffset, limit = 100)

        assertEquals(205, first.totalCount)
        assertEquals(100, first.entries.size)
        assertEquals(100, second.entries.size)
        assertEquals(5, third.entries.size)
        assertTrue(first.hasMore)
        assertTrue(second.hasMore)
        assertFalse(third.hasMore)
        val allIds = (first.entries + second.entries + third.entries).map { it.entry.id }
        assertEquals(205, allIds.distinct().size)
    }

    @Test fun entryCountsCanDriveAConsistencyGoalWithoutNumericLogging() = runBlocking {
        val trackId = tracks.create(booksDraft())
        val projection = requireNotNull(tracks.projection(trackId))
        tracks.addEntry(
            trackId,
            TrackEntryDraft(
                TestClock.today(),
                mapOf(projection.primaryField.uuid to TrackValueDraft(textValue = "Consistency evidence")),
            ),
        )
        val goalId = goals.create(
            GoalDraft(
                name = "Read Every Week",
                type = GoalType.Consistency,
                dimension = UnitDimension.Count,
                unitId = "count",
                targetMin = 1.0,
                startDate = TestClock.today(),
                aggregation = GoalAggregation.CompletionCount,
                consistencyPeriod = GoalConsistencyPeriod.Week,
                consistencyRequiredPeriods = 4,
            ),
        )

        links.createRule(
            LinkRuleDraft(
                name = "Books Read Consistency",
                sourceType = LinkSourceType.Track,
                sourceEntityId = trackId,
                sourceMetric = LinkSourceMetric.EntryCount,
                targetGoalId = goalId,
                retroactiveFrom = TestClock.today(),
                trackAggregation = TrackAggregation.CountEntries,
            ),
            commitBackfill = true,
        )

        assertEquals(1, links.contributions.first().size)
    }

    @Test fun movieEntryCountDrivesAReachGoalAndReconcilesEveryCauseAndEffect() = runBlocking {
        val trackId = tracks.create(
            TrackDraft(
                name = "Movies Watched",
                fields = listOf(TrackFieldDraft("Title", TrackFieldType.ShortText, required = true, primary = true)),
            ),
        )
        val projection = requireNotNull(tracks.projection(trackId))
        fun movie(title: String) = TrackEntryDraft(
            TestClock.today(),
            mapOf(projection.primaryField.uuid to TrackValueDraft(textValue = title)),
        )
        val arrivalId = tracks.addEntry(trackId, movie("Arrival"))
        tracks.addEntry(trackId, movie("Moonlight"))
        tracks.addEntry(trackId, movie("Spirited Away"))
        val goalId = goals.create(
            GoalDraft(
                name = "Watch 50 Movies",
                type = GoalType.ReachValue,
                dimension = UnitDimension.Count,
                unitId = "count",
                precision = 0,
                targetMin = 50.0,
                startDate = TestClock.today(),
                aggregation = GoalAggregation.Sum,
            ),
        )
        val goal = goals.goals.first().single { it.id == goalId }
        links.createRule(
            LinkRuleDraft(
                name = "Movies Watched → Watch 50 Movies",
                sourceType = LinkSourceType.Track,
                sourceEntityId = trackId,
                sourceMetric = LinkSourceMetric.EntryCount,
                targetGoalId = goalId,
                retroactiveFrom = TestClock.today(),
                trackAggregation = TrackAggregation.CountEntries,
            ),
            commitBackfill = true,
        )

        assertEquals(3, links.contributions.first().size)
        assertTrue(links.contributions.first().all { it.canonicalValue == 1.0 })
        assertEquals(3, measurements.entries.first().count { it.metricId == goal.metricId })

        val deleted = requireNotNull(tracks.deleteEntry(arrivalId))
        links.rebuildSources(setOf(LinkSourceType.Track))
        assertEquals(2, links.contributions.first().size)
        assertEquals(2, measurements.entries.first().count { it.metricId == goal.metricId })

        tracks.restoreEntry(deleted)
        links.rebuildSources(setOf(LinkSourceType.Track))
        links.rebuildSources(setOf(LinkSourceType.Track))
        assertEquals(3, links.contributions.first().size)
        assertEquals(3, measurements.entries.first().count { it.metricId == goal.metricId })
    }

    private fun booksDraft() = TrackDraft(
        name = "Books Read",
        fields = listOf(
            TrackFieldDraft("Title", TrackFieldType.ShortText, required = true, primary = true),
            TrackFieldDraft("Genre", TrackFieldType.SingleChoice, options = listOf(TrackChoiceOptionDraft("History"), TrackChoiceOptionDraft("Fiction"))),
            TrackFieldDraft("Rating", TrackFieldType.Number, dimension = UnitDimension.Unitless, unitId = "unitless"),
        ),
    )

    private fun com.whip.app.domain.TrackProjection.toDraft() = TrackDraft(
        name = track.name,
        description = track.description,
        icon = track.icon,
        areaId = track.areaId,
        area = track.area,
        tags = track.tags,
        fields = fields.map { field ->
            TrackFieldDraft(
                name = field.name,
                type = field.type,
                required = field.required,
                primary = field.primary,
                showInList = field.showInList,
                dimension = field.dimension,
                unitId = field.unitId,
                precision = field.precision,
                scaleMin = field.scaleMin,
                scaleMax = field.scaleMax,
                scaleLowLabel = field.scaleLowLabel,
                scaleHighLabel = field.scaleHighLabel,
                scaleStep = field.scaleStep,
                options = optionsFor(field.id).map { TrackChoiceOptionDraft(it.label, it.uuid, it.id) },
                uuid = field.uuid,
                id = field.id,
            )
        },
    )

    private fun com.whip.app.domain.TriggerRule.toDraft(enabled: Boolean) = TriggerRuleDraft(
        name = name,
        sourceType = sourceType,
        sourceEntityId = sourceEntityId,
        sourceItemId = sourceItemId,
        outcome = outcome,
        targetType = targetType,
        targetEntityId = targetEntityId,
        delayMinutes = delayMinutes,
        quietStartMinutes = quietStartMinutes,
        quietEndMinutes = quietEndMinutes,
        action = action,
        notificationEnabled = notificationEnabled,
        enabled = enabled,
        conditionMode = conditionMode,
        conditions = conditions,
        mappings = mappings,
    )

    private object TestClock : WhipClock {
        override fun now(): Instant = Instant.parse("2026-08-23T12:00:00Z")
        override fun zoneId(): ZoneId = ZoneId.of("UTC")
        override fun today(zoneId: ZoneId): LocalDate = LocalDate.of(2026, 8, 23)
    }

    private class TestIds : WhipIdGenerator {
        private val next = AtomicInteger()
        override fun nextId(): String = "track-test-${next.incrementAndGet()}"
    }
}

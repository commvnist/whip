package com.whip.app

import androidx.room.migration.Migration
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.whip.app.data.WhipDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WhipDatabaseMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        WhipDatabase::class.java,
    )

    @Test
    fun migrationOneToNinePreservesTasksLinksContributionsTriggersAndOccurrences() {
        helper.createDatabase(V1_DATABASE_NAME, 1).apply {
            insertMainArea()
            insertSchemaOneTask()
            insertSchemaOneGoal()
            execSQL(
                """
                INSERT INTO link_rules (
                    id, uuid, name, kind, sourceType, sourceEntityId, sourceMetricId,
                    sourceItemId, sourceMetric, targetGoalId, targetMilestoneId,
                    valueMode, fixedValue, multiplier, offset, retroactiveFromEpochDay,
                    enabled, createdAtMillis, updatedAtMillis
                ) VALUES (
                    11, 'link-11', 'Habit to Goal', 'Contribution', 'Habit', 3, NULL,
                    NULL, 'Value', 7, NULL, 'SourceValue', NULL, 1, 0, NULL,
                    1, 100, 100
                )
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO contributions (
                    id, uuid, linkRuleId, sourceEventId, sourceType, sourceEntityId,
                    targetGoalId, metricEntryId, canonicalValue, localEpochDay,
                    timestampMillis, excluded, overrideValue, explanation,
                    createdAtMillis, updatedAtMillis
                ) VALUES (
                    12, 'contribution-12', 11, 'habit-event-1', 'Habit', 3,
                    7, NULL, 2.5, 20690, 1000, 0, NULL, 'Habit value', 1000, 1000
                )
                """.trimIndent(),
            )
            insertSchemaOneTrigger(
                id = 21,
                uuid = "prompt-task",
                targetType = "Task",
                autoCompleteTargetHabit = 0,
            )
            insertSchemaOneTrigger(
                id = 22,
                uuid = "check-habit",
                targetType = "Habit",
                autoCompleteTargetHabit = 1,
            )
            execSQL(
                "INSERT INTO trigger_occurrences " +
                    "(id, triggerRuleId, sourceEventId, availableAtMillis, deliveredAtMillis, dismissedAtMillis) " +
                    "VALUES (31, 21, 'task-event-1', 2000, 2100, NULL)",
            )
            close()
        }

        helper.runMigrationsAndValidate(
            V1_DATABASE_NAME,
            34,
            true,
            *allMigrations,
        ).use { database ->
            database.query(
                """
                SELECT l.id, l.uuid, l.conditionMode, c.id, c.canonicalValue, c.explanation, l.enabled
                FROM link_rules l JOIN contributions c ON c.linkRuleId = l.id
                WHERE l.id = 11
                """.trimIndent(),
            ).use { cursor ->
                check(cursor.moveToFirst())
                assertEquals(11L, cursor.getLong(0))
                assertEquals("link-11", cursor.getString(1))
                assertEquals("MatchAll", cursor.getString(2))
                assertEquals(12L, cursor.getLong(3))
                assertEquals(2.5, cursor.getDouble(4), 0.0)
                assertEquals("Habit value", cursor.getString(5))
                assertEquals(0, cursor.getInt(6))
            }
            database.query("SELECT uuid, action, notificationEnabled, conditionMode, sourceItemId, enabled FROM trigger_rules ORDER BY id").use { cursor ->
                check(cursor.moveToFirst())
                assertEquals("prompt-task", cursor.getString(0))
                assertEquals("PromptTask", cursor.getString(1))
                assertEquals(0, cursor.getInt(2))
                assertEquals("MatchAll", cursor.getString(3))
                check(cursor.isNull(4))
                assertEquals(0, cursor.getInt(5))
                check(cursor.moveToNext())
                assertEquals("check-habit", cursor.getString(0))
                assertEquals("CheckOffHabit", cursor.getString(1))
            }
            database.query(
                "SELECT id, sourceEventId, deliveredAtMillis, remindAtMillis, fulfilledEntryId, sourceSnapshot, dismissedAtMillis " +
                    "FROM trigger_occurrences WHERE id = 31",
            ).use { cursor ->
                check(cursor.moveToFirst())
                assertEquals(31L, cursor.getLong(0))
                assertEquals("task-event-1", cursor.getString(1))
                assertEquals(2100L, cursor.getLong(2))
                check(cursor.isNull(3))
                check(cursor.isNull(4))
                assertEquals("", cursor.getString(5))
                assertEquals(2100L, cursor.getLong(6))
            }
            database.query("SELECT title, icon FROM tasks WHERE id = 5").use { cursor ->
                check(cursor.moveToFirst())
                assertEquals("Preserved Task", cursor.getString(0))
                assertEquals("✅", cursor.getString(1))
            }
        }
    }

    @Test
    fun migrationTwoToNinePreservesTrackIdentityFieldsEntriesAndFractionalValues() {
        helper.createDatabase(V2_DATABASE_NAME, 2).apply {
            insertMainArea()
            execSQL(
                "INSERT INTO tracks " +
                    "(id, uuid, name, description, icon, areaId, area, tagsCsv, pinned, archived, position, createdAtMillis, updatedAtMillis) " +
                    "VALUES (41, 'track-41', 'Movies', '', '▤', 'main', 'Main', '', 1, 0, 0, 100, 100)",
            )
            execSQL(
                "INSERT INTO track_fields " +
                    "(id, uuid, trackId, name, type, position, required, primaryField, showInList, dimension, unitId, precision, scaleMin, scaleMax, scaleLowLabel, scaleHighLabel, createdAtMillis, updatedAtMillis) " +
                    "VALUES (42, 'field-42', 41, 'Rating', 'Scale', 0, 1, 1, 1, NULL, NULL, 1, 1, 5, 'Poor', 'Excellent', 100, 100)",
            )
            execSQL(
                "INSERT INTO track_entries " +
                    "(id, uuid, trackId, entryEpochDay, sourceOccurrenceId, sourceExplanation, createdAtMillis, updatedAtMillis) " +
                    "VALUES (43, 'entry-43', 41, 20690, NULL, '', 100, 100)",
            )
            execSQL(
                "INSERT INTO track_values " +
                    "(id, uuid, entryId, fieldId, textValue, enteredNumber, canonicalNumber, enteredUnitId, dateEpochDay, booleanValue, choiceOptionId, scaleValue, createdAtMillis, updatedAtMillis) " +
                    "VALUES (44, 'value-44', 43, 42, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 4, 100, 100)",
            )
            execSQL(
                """
                INSERT INTO trigger_rules (
                    id, uuid, name, sourceType, sourceEntityId, outcome, targetType,
                    targetEntityId, delayMinutes, quietStartMinutes, quietEndMinutes,
                    action, notificationEnabled, conditionMode, enabled,
                    createdAtMillis, updatedAtMillis
                ) VALUES (
                    45, 'trigger-45', 'Follow Up', 'Track', 41, 'Recorded', 'Habit',
                    9, 0, NULL, NULL, 'PromptHabit', 0, 'MatchAll', 1, 100, 100
                )
                """.trimIndent(),
            )
            close()
        }

        helper.runMigrationsAndValidate(
            V2_DATABASE_NAME,
            34,
            true,
            *allMigrations.drop(1).toTypedArray(),
        ).use { database ->
            database.query(
                """
                SELECT t.id, t.uuid, t.name, t.icon, f.id, f.primaryField,
                    f.showInList, f.scaleStep, e.id, v.id, v.scaleValue, typeof(v.scaleValue)
                FROM tracks t
                JOIN track_fields f ON f.trackId = t.id
                JOIN track_entries e ON e.trackId = t.id
                JOIN track_values v ON v.entryId = e.id AND v.fieldId = f.id
                WHERE t.id = 41
                """.trimIndent(),
            ).use { cursor ->
                check(cursor.moveToFirst())
                assertEquals(41L, cursor.getLong(0))
                assertEquals("track-41", cursor.getString(1))
                assertEquals("Movies", cursor.getString(2))
                assertEquals("📋", cursor.getString(3))
                assertEquals(42L, cursor.getLong(4))
                assertEquals(1, cursor.getInt(5))
                assertEquals(1, cursor.getInt(6))
                assertEquals(1.0, cursor.getDouble(7), 0.0)
                assertEquals(43L, cursor.getLong(8))
                assertEquals(44L, cursor.getLong(9))
                assertEquals(4.0, cursor.getDouble(10), 0.0)
                assertEquals("real", cursor.getString(11))
            }
            database.query("SELECT sourceItemId FROM trigger_rules WHERE id = 45").use { cursor ->
                check(cursor.moveToFirst())
                check(cursor.isNull(0))
            }
        }
    }

    @Test
    fun migrationEightToNineMovesNeutralOccurrencesOutOfMeasurementsAndDropsMissing() {
        helper.createDatabase(V8_DATABASE_NAME, 8).apply {
            insertMainArea()
            execSQL("INSERT INTO metric_definitions (id, name, valueKind, dimension, defaultUnitId, precision, dimensionLocked, archived, createdAtMillis, updatedAtMillis) VALUES ('metric-habit', 'Read', 'Boolean', 'Count', 'count', 0, 0, 0, 1, 1)")
            execSQL(
                """
                INSERT INTO habits (
                    id, uuid, metricId, name, notes, areaId, area, tagsCsv, icon,
                    trackingMode, dimension, unitId, precision, comparison, targetMin,
                    targetMax, targetPeriod, rollingDays, scheduleType, scheduleInterval,
                    weekdaysMask, flexibleTimesPerWeek, startEpochDay, endType, endEpochDay,
                    endValue, quickIncrement, quickActionsCsv, reminderMinutesCsv,
                    weekdayReminderMinutesCsv, weekStart, timerStartedAtMillis, pinned,
                    position, archived, paused, createdAtMillis, updatedAtMillis, sourceMetricId
                ) VALUES (
                    3, 'habit-3', 'metric-habit', 'Read', '', 'main', 'Main', '', '📖',
                    'CheckOff', 'Count', 'count', 0, 'AtLeast', 1, NULL, 'Day', NULL,
                    'Daily', 1, 0, NULL, 20690, 'Never', NULL, NULL, 1, '', '', '',
                    'MONDAY', NULL, 0, 0, 0, 0, 1, 1, NULL
                )
                """.trimIndent(),
            )
            listOf(
                Triple("entry-skip", "Skipped", 100L),
                Triple("entry-excuse", "Excused", 200L),
                Triple("entry-missing", "Missing", 300L),
                Triple("entry-recorded", "Recorded", 400L),
            ).forEach { (entryId, status, timestamp) ->
                execSQL(
                    "INSERT INTO metric_entries (id, metricId, canonicalValue, enteredValue, enteredUnitId, status, timestampMillis, localEpochDay, zoneId, offsetSeconds, sourceType, sourceId, note, createdAtMillis, updatedAtMillis) VALUES ('$entryId', 'metric-habit', NULL, NULL, NULL, '$status', $timestamp, ${if (status == "Recorded") 20692 else if (status == "Missing") 20691 else 20690}, 'UTC', 0, 'Habit', 'source-$status', '', $timestamp, $timestamp)",
                )
                execSQL(
                    "INSERT INTO habit_logs (uuid, habitId, value, canonicalValue, enteredUnitId, status, timestampMillis, localEpochDay, zoneId, offsetSeconds, note, sourceType, sourceId, metricEntryId, createdAtMillis, updatedAtMillis) VALUES ('log-${status.lowercase()}', 3, NULL, NULL, NULL, '$status', $timestamp, ${if (status == "Recorded") 20692 else if (status == "Missing") 20691 else 20690}, 'UTC', 0, '', 'Manual', NULL, '$entryId', $timestamp, $timestamp)",
                )
            }
            close()
        }

        helper.runMigrationsAndValidate(
            V8_DATABASE_NAME,
            32,
            true,
            WhipDatabase.migration8To9,
            WhipDatabase.migration9To28,
            WhipDatabase.migration28To29,
            WhipDatabase.migration29To30,
            WhipDatabase.migration30To31,
            WhipDatabase.migration31To32,
        ).use { database ->
            database.query("SELECT uuid, habitId, localEpochDay, skippedAtMillis FROM habit_skips").use { cursor ->
                check(cursor.moveToFirst())
                assertEquals("habit-skip-log-excused", cursor.getString(0))
                assertEquals(3L, cursor.getLong(1))
                assertEquals(20690L, cursor.getLong(2))
                assertEquals(200L, cursor.getLong(3))
                assertEquals(1, cursor.count)
            }
            database.query("SELECT status, metricEntryId FROM habit_logs").use { cursor ->
                check(cursor.moveToFirst())
                assertEquals("Recorded", cursor.getString(0))
                assertEquals("entry-recorded", cursor.getString(1))
                assertEquals(1, cursor.count)
            }
            database.query("SELECT id FROM metric_entries").use { cursor ->
                check(cursor.moveToFirst())
                assertEquals("entry-recorded", cursor.getString(0))
                assertEquals(1, cursor.count)
            }
        }
    }

    @Test
    fun migrationTwentySevenToTwentyEightPreservesPublicReleaseDataAndRelationships() {
        helper.createDatabase(V27_DATABASE_NAME, 27).apply {
            execSQL(
                "INSERT INTO tags (id, name, colorArgb, archived, createdAtMillis, updatedAtMillis) " +
                    "VALUES ('tag-release', 'Release', 123, 0, 100, 100)",
            )
            execSQL(
                """
                INSERT INTO tasks (
                    id, uuid, title, notes, scheduleKind, dateEpochDay, recurrenceUnit,
                    recurrenceInterval, weekdaysMask, recurrenceEnd, recurrenceEndEpochDay,
                    recurrenceCount, timeMinutes, reminderEnabled, archived, completedAtMillis,
                    createdAtMillis, updatedAtMillis, showSubtaskProgress, progressDisplay,
                    autoCompleteFromSteps, repeatStepPolicy, pinned, priority, areaId, area,
                    tagsCsv, deadlineEpochDay, recurrenceAnchor, reminderOffsetsMinutesCsv,
                    locationReminderEnabled, locationName, locationLatitude, locationLongitude,
                    locationRadiusMeters, locationTrigger, missedOccurrencePolicy, inbox,
                    durationMinutes, effort, manualPosition
                ) VALUES (
                    5, 'task-public-5', 'Upgrade Sentinel', 'keep me', 'Anytime', NULL, NULL,
                    1, 0, NULL, NULL, NULL, NULL, 0, 0, NULL,
                    100, 100, 1, 'Percent', 1, 'Reset', 1, 'High', NULL, '',
                    'Release', NULL, 'Schedule', '', 1, 'Home', 43.7, -79.4,
                    150, 'Arrive', 'KeepLatest', 1, 25, 'Light', 4
                )
                """.trimIndent(),
            )
            execSQL(
                "INSERT INTO task_steps (id, uuid, taskId, title, position, notes, weight, archived, createdAtMillis, updatedAtMillis) " +
                    "VALUES (6, 'step-public-6', 5, 'Preserved step', 0, 'step note', 2.5, 0, 100, 100)",
            )
            execSQL(
                "INSERT INTO task_step_snapshots (taskId, occurrenceKey, stepId, title, position, notes, weight, completed, completedAtMillis) " +
                    "VALUES (5, 20690, 6, 'Preserved step', 0, 'snapshot note', 2.5, 1, 200)",
            )
            execSQL(
                "INSERT INTO entity_tag_links (entityType, entityId, tagId) " +
                    "VALUES ('Task', '5', 'tag-release')",
            )
            execSQL(
                """
                INSERT INTO goals (
                    id, uuid, metricId, name, description, areaId, area, tagsCsv, icon,
                    colorArgb, type, dimension, unitId, precision, baseline, targetMin,
                    targetMax, direction, startEpochDay, deadlineEpochDay, aggregation,
                    aggregationPeriod, rollingDays, entryMode, paceType, consistencyPeriod,
                    consistencyRequiredPeriods, reminderMinutes, status, pinned, position,
                    createdAtMillis, updatedAtMillis
                ) VALUES (
                    7, 'goal-public-7', 'metric-public-7', 'Preserved Goal', '', NULL, '', '', '🎯',
                    456, 'ReachValue', 'Count', 'count', 1, 0, 50,
                    NULL, 'Increase', 20690, NULL, 'Sum', 'All', NULL, 'Manual', 'None', 'Week',
                    NULL, NULL, 'Completed', 1, 0, 100, 200
                )
                """.trimIndent(),
            )
            execSQL(
                "INSERT INTO goal_milestones (id, uuid, goalId, name, position, weight, targetValue, completed, completedAtMillis, linkedTaskId, reward, createdAtMillis, updatedAtMillis) " +
                    "VALUES (8, 'milestone-public-8', 7, 'Preserved milestone', 0, 1.5, 10, 1, 200, 5, 'Reward', 100, 200)",
            )
            execSQL(
                "INSERT INTO goal_completion_snapshots (id, goalId, completedAtMillis, value, progress, status) " +
                    "VALUES (9, 7, 200, 50, 1, 'Completed')",
            )
            execSQL(
                """
                INSERT INTO link_rules (
                    id, uuid, name, kind, sourceType, sourceEntityId, sourceMetricId,
                    sourceItemId, sourceMetric, targetGoalId, targetMilestoneId,
                    valueMode, fixedValue, multiplier, offset, retroactiveFromEpochDay,
                    enabled, createdAtMillis, updatedAtMillis
                ) VALUES (
                    11, 'link-public-11', 'Task to Goal', 'Contribution', 'Task', 5, NULL,
                    NULL, 'Value', 7, 8, 'SourceValue', NULL, 1, 0, NULL, 1, 100, 100
                )
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO contributions (
                    id, uuid, linkRuleId, sourceEventId, sourceType, sourceEntityId,
                    targetGoalId, metricEntryId, canonicalValue, localEpochDay,
                    timestampMillis, excluded, overrideValue, explanation,
                    createdAtMillis, updatedAtMillis
                ) VALUES (
                    12, 'contribution-public-12', 11, 'task-event-public', 'Task', 5,
                    7, NULL, 2.5, 20690, 1000, 0, NULL, 'Public contribution', 1000, 1000
                )
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO trigger_rules (
                    id, uuid, name, sourceType, sourceEntityId, outcome, targetType,
                    targetEntityId, delayMinutes, quietStartMinutes, quietEndMinutes,
                    autoCompleteTargetHabit, enabled, createdAtMillis, updatedAtMillis
                ) VALUES (
                    21, 'trigger-public-21', 'Follow Up', 'Task', 5, 'Completed', 'Habit',
                    3, 0, NULL, NULL, 1, 1, 100, 100
                )
                """.trimIndent(),
            )
            execSQL(
                "INSERT INTO trigger_occurrences (id, triggerRuleId, sourceEventId, availableAtMillis, deliveredAtMillis, dismissedAtMillis) " +
                    "VALUES (22, 21, 'trigger-event-public', 2000, 2100, NULL)",
            )
            close()
        }

        helper.runMigrationsAndValidate(
            V27_DATABASE_NAME,
            32,
            true,
            WhipDatabase.migration27To28,
            WhipDatabase.migration28To29,
            WhipDatabase.migration29To30,
            WhipDatabase.migration30To31,
            WhipDatabase.migration31To32,
        ).use { database ->
            database.query("SELECT title, notes, icon, tagsCsv, inbox FROM tasks WHERE id = 5").use { cursor ->
                check(cursor.moveToFirst())
                assertEquals("Upgrade Sentinel", cursor.getString(0))
                assertEquals("keep me", cursor.getString(1))
                assertEquals("✅", cursor.getString(2))
                assertEquals("Release", cursor.getString(3))
                assertEquals(1, cursor.getInt(4))
            }
            database.query(
                "SELECT s.title, s.notes, p.completed, p.notes FROM task_steps s " +
                    "JOIN task_step_snapshots p ON p.stepId = s.id WHERE s.id = 6",
            ).use { cursor ->
                check(cursor.moveToFirst())
                assertEquals("Preserved step", cursor.getString(0))
                assertEquals("step note", cursor.getString(1))
                assertEquals(1, cursor.getInt(2))
                assertEquals("snapshot note", cursor.getString(3))
            }
            database.query(
                "SELECT g.name, g.elapsedStartMillis, g.elapsedDisplayUnit, m.name, s.status " +
                    "FROM goals g JOIN goal_milestones m ON m.goalId = g.id " +
                    "JOIN goal_completion_snapshots s ON s.goalId = g.id WHERE g.id = 7",
            ).use { cursor ->
                check(cursor.moveToFirst())
                assertEquals("Preserved Goal", cursor.getString(0))
                check(cursor.isNull(1))
                assertEquals("Auto", cursor.getString(2))
                assertEquals("Preserved milestone", cursor.getString(3))
                assertEquals("Completed", cursor.getString(4))
            }
            database.query(
                "SELECT l.conditionMode, c.explanation, l.enabled FROM link_rules l " +
                    "JOIN contributions c ON c.linkRuleId = l.id WHERE l.id = 11",
            ).use { cursor ->
                check(cursor.moveToFirst())
                assertEquals("MatchAll", cursor.getString(0))
                assertEquals("Public contribution", cursor.getString(1))
                assertEquals(0, cursor.getInt(2))
            }
            database.query(
                "SELECT r.action, r.notificationEnabled, r.conditionMode, o.sourceSnapshot, r.enabled, o.dismissedAtMillis " +
                    "FROM trigger_rules r JOIN trigger_occurrences o ON o.triggerRuleId = r.id WHERE r.id = 21",
            ).use { cursor ->
                check(cursor.moveToFirst())
                assertEquals("CheckOffHabit", cursor.getString(0))
                assertEquals(0, cursor.getInt(1))
                assertEquals("MatchAll", cursor.getString(2))
                assertEquals("", cursor.getString(3))
                assertEquals(0, cursor.getInt(4))
                assertEquals(2100L, cursor.getLong(5))
            }
            database.query("SELECT tagId FROM entity_tag_links WHERE entityType = 'Task' AND entityId = '5'").use { cursor ->
                check(cursor.moveToFirst())
                assertEquals("tag-release", cursor.getString(0))
            }
        }
    }

    @Test
    fun migrationTwentyEightToTwentyNineKeepsExistingChecklistHabitsAutomatic() {
        helper.createDatabase(V28_DATABASE_NAME, 28).apply {
            execSQL(
                "INSERT INTO metric_definitions (id, name, valueKind, dimension, defaultUnitId, precision, dimensionLocked, archived, createdAtMillis, updatedAtMillis) " +
                    "VALUES ('metric-checklist', 'Medication', 'Checklist', 'Count', 'count', 0, 0, 0, 1, 1)",
            )
            execSQL(
                """
                INSERT INTO habits (
                    id, uuid, metricId, name, notes, areaId, area, tagsCsv, icon,
                    trackingMode, dimension, unitId, precision, comparison, targetMin,
                    targetMax, targetPeriod, rollingDays, scheduleType, scheduleInterval,
                    weekdaysMask, flexibleTimesPerWeek, startEpochDay, endType, endEpochDay,
                    endValue, quickIncrement, quickActionsCsv, reminderMinutesCsv,
                    weekdayReminderMinutesCsv, weekStart, timerStartedAtMillis, pinned,
                    position, archived, paused, createdAtMillis, updatedAtMillis, sourceMetricId
                ) VALUES (
                    3, 'habit-checklist', 'metric-checklist', 'Medication', '', NULL, '', '', '💊',
                    'Checklist', 'Count', 'count', 0, 'AtLeast', 1, NULL, 'Occurrence', NULL,
                    'Daily', 1, 0, NULL, 20690, 'Never', NULL, NULL, 1, '', '', '',
                    'MONDAY', NULL, 0, 0, 0, 0, 1, 1, NULL
                )
                """.trimIndent(),
            )
            execSQL(
                "INSERT INTO habit_checklist_items (id, uuid, habitId, name, position, archived, createdAtMillis, updatedAtMillis) " +
                    "VALUES (4, 'item-4', 3, 'Medication 1', 0, 0, 1, 1)",
            )
            close()
        }

        helper.runMigrationsAndValidate(
            V28_DATABASE_NAME,
            32,
            true,
            WhipDatabase.migration28To29,
            WhipDatabase.migration29To30,
            WhipDatabase.migration30To31,
            WhipDatabase.migration31To32,
        ).use { database ->
            database.query(
                "SELECT name, autoCompleteFromItems FROM habits WHERE id = 3",
            ).use { cursor ->
                check(cursor.moveToFirst())
                assertEquals("Medication", cursor.getString(0))
                assertEquals(1, cursor.getInt(1))
            }
            database.query("SELECT name FROM habit_checklist_items WHERE id = 4").use { cursor ->
                check(cursor.moveToFirst())
                assertEquals("Medication 1", cursor.getString(0))
            }
        }
    }

    @Test
    fun migrationTwentyNineToThirtyPreservesMachineLinkAndAllowsUnattachedMachines() {
        helper.createDatabase(V29_DATABASE_NAME, 29).apply {
            execSQL(
                """
                INSERT INTO exercises (
                    id, uuid, name, trackingType, notes, equipment, primaryMuscles, secondaryMuscles,
                    weightUnitId, weightIncrement, repetitionIncrement, defaultRestSeconds,
                    defaultGraphMetric, oneRepMaxFormula, barWeightKg, availablePlatesKgCsv,
                    includeInVolume, includeInPersonalRecords, bodyweightLoadPolicy,
                    effectiveBodyweightPercent, showRpe, showRir, showTempo, favorite, position,
                    archived, createdAtMillis, updatedAtMillis, loadInterpretation
                ) VALUES (
                    7, 'exercise-7', 'Cable row', 'WeightReps', '', 'Cable', 'Back', '',
                    'kilogram', 2.5, 1, 90, 'EstimatedOneRepMax', 'Epley', NULL, '',
                    1, 1, 'ExternalWeightOnly', 100, NULL, NULL, NULL, 0, 0,
                    0, 100, 100, 'Total'
                )
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO gym_machines (
                    id, uuid, exerciseId, name, location, details, loadType, unitId,
                    levelLabel, availableLoadsCsv, archived, createdAtMillis, updatedAtMillis,
                    loadInterpretation, baseLoadKg, configurationGroupId, configurationVersion,
                    seatPosition, backPosition, attachment, pulleyRatio, stackMode, addOnPlateKg,
                    stackLabelsCsv, massMappingCsv, compatibleForComparison
                ) VALUES (
                    9, 'machine-9', 7, 'Cable tower', 'Gym', '', 'Level', '',
                    'pin', '1,2,3', 0, 100, 100, 'OrdinalSetting', NULL, 'machine-9', 1,
                    '', '', '', 1, 'Single', NULL, '', '', 0
                )
                """.trimIndent(),
            )
            close()
        }

        helper.runMigrationsAndValidate(
            V29_DATABASE_NAME,
            32,
            true,
            WhipDatabase.migration29To30,
            WhipDatabase.migration30To31,
            WhipDatabase.migration31To32,
        ).use { database ->
            database.query("SELECT exerciseId, levelDirection FROM gym_machines WHERE id = 9").use { cursor ->
                check(cursor.moveToFirst())
                assertEquals(7L, cursor.getLong(0))
                assertEquals("HigherNumberMoreResistance", cursor.getString(1))
            }
            database.query("SELECT machineId, exerciseId FROM gym_machine_exercise_joins").use { cursor ->
                check(cursor.moveToFirst())
                assertEquals(9L, cursor.getLong(0))
                assertEquals(7L, cursor.getLong(1))
                assertEquals(1, cursor.count)
            }
            database.execSQL(
                """
                INSERT INTO gym_machines (
                    uuid, exerciseId, name, location, details, loadType, unitId, levelLabel,
                    availableLoadsCsv, archived, createdAtMillis, updatedAtMillis,
                    loadInterpretation, baseLoadKg, configurationGroupId, configurationVersion,
                    seatPosition, backPosition, attachment, pulleyRatio, stackMode, addOnPlateKg,
                    stackLabelsCsv, massMappingCsv, compatibleForComparison, levelDirection
                ) VALUES (
                    'machine-unattached', NULL, 'Unattached machine', '', '', 'Level', '', 'level',
                    '1,2,3', 0, 200, 200, 'OrdinalSetting', NULL, 'machine-unattached', 1,
                    '', '', '', 1, 'Single', NULL, '', '', 0, 'HigherNumberLessResistance'
                )
                """.trimIndent(),
            )
            database.query("SELECT exerciseId, levelDirection FROM gym_machines WHERE uuid = 'machine-unattached'").use { cursor ->
                check(cursor.moveToFirst())
                check(cursor.isNull(0))
                assertEquals("HigherNumberLessResistance", cursor.getString(1))
            }
        }
    }

    @Test
    fun migrationThirtyToThirtyOneRetiresRulesPreservesHistoryAndUnblocksTrackDeletion() {
        helper.createDatabase(V30_DATABASE_NAME, 30).apply {
            insertMainArea()
            execSQL("INSERT INTO metric_definitions (id, name, valueKind, dimension, defaultUnitId, precision, dimensionLocked, archived, createdAtMillis, updatedAtMillis) VALUES ('goal-metric', 'Goal', 'Number', 'Count', 'count', 0, 0, 0, 1, 1)")
            execSQL("INSERT INTO metric_definitions (id, name, valueKind, dimension, defaultUnitId, precision, dimensionLocked, archived, createdAtMillis, updatedAtMillis) VALUES ('habit-metric', 'Habit', 'Boolean', 'Count', 'count', 0, 0, 0, 1, 1)")
            execSQL(
                """
                INSERT INTO goals (
                    id, uuid, metricId, name, description, areaId, area, tagsCsv, icon,
                    type, dimension, unitId, precision, baseline, targetMin, targetMax,
                    direction, startEpochDay, deadlineEpochDay, aggregation,
                    aggregationPeriod, rollingDays, paceType, consistencyPeriod,
                    consistencyRequiredPeriods, elapsedStartMillis, elapsedDisplayUnit,
                    reminderMinutes, status, pinned, position, createdAtMillis, updatedAtMillis
                ) VALUES (
                    7, 'goal-7', 'goal-metric', 'Preserved Goal', '', 'main', 'Main', '', '🎯',
                    'ReachValue', 'Count', 'count', 0, 0, 10, NULL, 'Increase', 20690,
                    NULL, 'Sum', 'All', NULL, 'None', 'Week', NULL, NULL, 'Auto', NULL,
                    'Active', 0, 0, 100, 100
                )
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO habits (
                    id, uuid, metricId, name, notes, areaId, area, tagsCsv, icon,
                    trackingMode, dimension, unitId, precision, comparison, targetMin,
                    targetMax, targetPeriod, rollingDays, scheduleType, scheduleInterval,
                    weekdaysMask, flexibleTimesPerWeek, startEpochDay, endType, endEpochDay,
                    endValue, quickIncrement, quickActionsCsv, reminderMinutesCsv,
                    weekdayReminderMinutesCsv, weekStart, timerStartedAtMillis, pinned,
                    position, archived, paused, createdAtMillis, updatedAtMillis, sourceMetricId,
                    autoCompleteFromItems
                ) VALUES (
                    3, 'habit-3', 'habit-metric', 'Preserved Habit', '', 'main', 'Main', '', '✅',
                    'CheckOff', 'Count', 'count', 0, 'AtLeast', 1, NULL, 'Day', NULL,
                    'Daily', 1, 0, NULL, 20690, 'Never', NULL, NULL, 1, '', '', '',
                    'MONDAY', NULL, 0, 0, 0, 0, 100, 100, NULL, 1
                )
                """.trimIndent(),
            )
            execSQL("INSERT INTO tracks (id, uuid, name, description, icon, areaId, area, tagsCsv, pinned, archived, position, createdAtMillis, updatedAtMillis) VALUES (41, 'track-41', 'Preserved Track', '', '📋', 'main', 'Main', '', 0, 0, 0, 100, 100)")
            execSQL("INSERT INTO track_fields (id, uuid, trackId, name, type, position, required, primaryField, showInList, dimension, unitId, precision, scaleMin, scaleMax, scaleLowLabel, scaleHighLabel, createdAtMillis, updatedAtMillis, scaleStep) VALUES (42, 'field-42', 41, 'Choice', 'SingleChoice', 0, 1, 1, 1, NULL, NULL, 0, NULL, NULL, '', '', 100, 100, 1)")
            execSQL("INSERT INTO track_choice_options (id, uuid, fieldId, label, position, createdAtMillis, updatedAtMillis) VALUES (43, 'option-43', 42, 'Keep', 0, 100, 100)")
            execSQL("INSERT INTO track_entries (id, uuid, trackId, entryEpochDay, sourceOccurrenceId, sourceExplanation, createdAtMillis, updatedAtMillis) VALUES (44, 'entry-44', 41, 20690, 61, 'Preserved prompt result', 100, 100)")
            execSQL("INSERT INTO metric_entries (id, metricId, canonicalValue, enteredValue, enteredUnitId, status, timestampMillis, localEpochDay, zoneId, offsetSeconds, sourceType, sourceId, note, createdAtMillis, updatedAtMillis) VALUES ('goal-generated', 'goal-metric', 2, 2, 'count', 'Recorded', 1000, 20690, 'UTC', 0, 'Track', 'contribution-51', 'Linked history', 1000, 1000)")
            execSQL("INSERT INTO metric_entries (id, metricId, canonicalValue, enteredValue, enteredUnitId, status, timestampMillis, localEpochDay, zoneId, offsetSeconds, sourceType, sourceId, note, createdAtMillis, updatedAtMillis) VALUES ('habit-generated', 'habit-metric', 1, 1, 'count', 'Success', 1000, 20690, 'UTC', 0, 'Task', 'trigger:rule-60:event', 'Generated history', 1000, 1000)")
            execSQL("INSERT INTO habit_logs (id, uuid, habitId, value, canonicalValue, enteredUnitId, status, timestampMillis, localEpochDay, zoneId, offsetSeconds, note, sourceType, sourceId, metricEntryId, createdAtMillis, updatedAtMillis) VALUES (31, 'habit-log-31', 3, 1, 1, 'count', 'Success', 1000, 20690, 'UTC', 0, 'Generated history', 'Task', 'trigger:rule-60:event', 'habit-generated', 1000, 1000)")
            execSQL(
                """
                INSERT INTO link_rules (
                    id, uuid, name, kind, sourceType, sourceEntityId, sourceMetricId,
                    sourceItemId, sourceMetric, targetGoalId, targetMilestoneId, valueMode,
                    fixedValue, multiplier, offset, retroactiveFromEpochDay, enabled,
                    createdAtMillis, updatedAtMillis, trackAggregation, sourceFieldId, conditionMode
                ) VALUES (
                    50, 'rule-50', 'Retired progress', 'Contribution', 'Track', 41, NULL,
                    NULL, 'FieldValue', 7, NULL, 'SourceValue', NULL, 1, 0, 20690, 1,
                    100, 100, 'Latest', 42, 'MatchAll'
                )
                """.trimIndent(),
            )
            execSQL("INSERT INTO contributions (id, uuid, linkRuleId, sourceEventId, sourceType, sourceEntityId, targetGoalId, metricEntryId, canonicalValue, localEpochDay, timestampMillis, excluded, overrideValue, explanation, createdAtMillis, updatedAtMillis) VALUES (51, 'contribution-51', 50, 'track:event', 'Track', 41, 7, 'goal-generated', 2, 20690, 1000, 0, NULL, 'Preserved contribution', 1000, 1000)")
            execSQL("INSERT INTO link_rule_conditions (id, linkRuleId, fieldId, entryDate, operator, position, textValue, numberValue, secondNumberValue, dateEpochDay, secondDateEpochDay) VALUES (52, 50, 42, 0, 'Equals', 0, NULL, NULL, NULL, NULL, NULL)")
            execSQL("INSERT INTO link_condition_choices (conditionId, optionId) VALUES (52, 43)")
            execSQL("INSERT INTO trigger_rules (id, uuid, name, sourceType, sourceEntityId, sourceItemId, outcome, targetType, targetEntityId, delayMinutes, quietStartMinutes, quietEndMinutes, action, notificationEnabled, conditionMode, enabled, createdAtMillis, updatedAtMillis) VALUES (60, 'rule-60', 'Retired prompt', 'Track', 41, NULL, 'Completed', 'Track', 41, 0, NULL, NULL, 'PromptTrackEntry', 1, 'MatchAll', 1, 100, 100)")
            execSQL("INSERT INTO trigger_rule_conditions (id, triggerRuleId, fieldId, entryDate, operator, position, textValue, numberValue, secondNumberValue, dateEpochDay, secondDateEpochDay) VALUES (63, 60, 42, 0, 'Equals', 0, NULL, NULL, NULL, NULL, NULL)")
            execSQL("INSERT INTO trigger_condition_choices (conditionId, optionId) VALUES (63, 43)")
            execSQL("INSERT INTO trigger_field_mappings (id, triggerRuleId, targetFieldId, sourceProperty, constantText, constantNumber, constantUnitId, constantDateEpochDay, constantBoolean, constantChoiceOptionId, constantScale) VALUES (64, 60, 42, 'Constant', NULL, NULL, NULL, NULL, NULL, 43, NULL)")
            execSQL("INSERT INTO trigger_occurrences (id, triggerRuleId, sourceEventId, availableAtMillis, deliveredAtMillis, dismissedAtMillis, remindAtMillis, fulfilledEntryId, sourceSnapshot) VALUES (61, 60, 'fulfilled', 1000, 1100, NULL, NULL, 44, '{}')")
            execSQL("INSERT INTO trigger_occurrences (id, triggerRuleId, sourceEventId, availableAtMillis, deliveredAtMillis, dismissedAtMillis, remindAtMillis, fulfilledEntryId, sourceSnapshot) VALUES (62, 60, 'pending', 2000, NULL, NULL, 3000, NULL, '{}')")
            close()
        }

        helper.runMigrationsAndValidate(
            V30_DATABASE_NAME,
            32,
            true,
            WhipDatabase.migration30To31,
            WhipDatabase.migration31To32,
        ).use { database ->
            database.query("SELECT enabled FROM link_rules WHERE id = 50").use { cursor ->
                check(cursor.moveToFirst())
                assertEquals(0, cursor.getInt(0))
            }
            database.query("SELECT enabled, notificationEnabled FROM trigger_rules WHERE id = 60").use { cursor ->
                check(cursor.moveToFirst())
                assertEquals(0, cursor.getInt(0))
                assertEquals(0, cursor.getInt(1))
            }
            database.query("SELECT dismissedAtMillis, remindAtMillis FROM trigger_occurrences WHERE id = 62").use { cursor ->
                check(cursor.moveToFirst())
                assertEquals(2000L, cursor.getLong(0))
                check(cursor.isNull(1))
            }
            database.query("SELECT COUNT(*) FROM metric_entries WHERE id IN ('goal-generated', 'habit-generated')").use { cursor ->
                check(cursor.moveToFirst())
                assertEquals(2, cursor.getInt(0))
            }
            database.query("SELECT metricEntryId FROM habit_logs WHERE id = 31").use { cursor ->
                check(cursor.moveToFirst())
                assertEquals("habit-generated", cursor.getString(0))
            }
            database.query("SELECT sourceOccurrenceId FROM track_entries WHERE id = 44").use { cursor ->
                check(cursor.moveToFirst())
                assertEquals(61L, cursor.getLong(0))
            }

            database.execSQL("PRAGMA foreign_keys = ON")
            database.execSQL("DELETE FROM track_fields WHERE id = 42")
            database.query("SELECT sourceFieldId FROM link_rules WHERE id = 50").use { cursor ->
                check(cursor.moveToFirst())
                check(cursor.isNull(0))
            }
            database.query("SELECT fieldId FROM link_rule_conditions WHERE id = 52").use { cursor ->
                check(cursor.moveToFirst())
                check(cursor.isNull(0))
            }
            database.query("SELECT COUNT(*) FROM link_condition_choices").use { cursor ->
                check(cursor.moveToFirst())
                assertEquals(0, cursor.getInt(0))
            }
            database.query("SELECT COUNT(*) FROM trigger_field_mappings").use { cursor ->
                check(cursor.moveToFirst())
                assertEquals(0, cursor.getInt(0))
            }
            database.query("SELECT COUNT(*) FROM track_entries WHERE id = 44").use { cursor ->
                check(cursor.moveToFirst())
                assertEquals(1, cursor.getInt(0))
            }
        }
    }

    @Test
    fun migrationThirtyOneToThirtyTwoKeepsRoutinesStaticAndPreservesLegacyWavePosition() {
        helper.createDatabase(V31_DATABASE_NAME, 31).apply {
            execSQL(
                "INSERT INTO gym_routines " +
                    "(id, uuid, name, notes, position, archived, pinned, createdAtMillis, updatedAtMillis) " +
                    "VALUES (7, 'routine-7', 'Existing routine', '', 0, 0, 0, 100, 100)",
            )
            execSQL(
                "INSERT INTO routine_days " +
                    "(id, uuid, routineId, name, position, createdAtMillis, updatedAtMillis) " +
                    "VALUES (8, 'day-8', 7, 'Day one', 0, 100, 100)",
            )
            execSQL(
                "INSERT INTO routine_days " +
                    "(id, uuid, routineId, name, position, createdAtMillis, updatedAtMillis) " +
                    "VALUES (9, 'day-9', 7, 'Day two', 1, 100, 100)",
            )
            execSQL(
                """
                INSERT INTO workout_sessions (
                    id, uuid, name, notes, startedAtMillis, endedAtMillis, localEpochDay,
                    zoneId, state, keepScreenAwake, restTimerDeadlineMillis,
                    restTimerDurationSeconds, archived, createdAtMillis, updatedAtMillis,
                    sourceRoutineId
                ) VALUES
                    (10, 'session-10', 'Finished one', '', 100, 200, 1, 'UTC', 'Finished', 0, NULL, NULL, 0, 100, 200, 7),
                    (11, 'session-11', 'Finished two', '', 300, 400, 2, 'UTC', 'Finished', 0, NULL, NULL, 0, 300, 400, 7),
                    (12, 'session-12', 'Archived finish', '', 500, 600, 3, 'UTC', 'Finished', 0, NULL, NULL, 1, 500, 600, 7),
                    (13, 'session-13', 'Discarded', '', 700, 800, 4, 'UTC', 'Discarded', 0, NULL, NULL, 0, 700, 800, 7),
                    (14, 'session-14', 'Unrelated finish', '', 900, 1000, 5, 'UTC', 'Finished', 0, NULL, NULL, 0, 900, 1000, NULL)
                """.trimIndent(),
            )
            close()
        }

        helper.runMigrationsAndValidate(
            V31_DATABASE_NAME,
            32,
            true,
            WhipDatabase.migration31To32,
        ).use { database ->
            database.query(
                "SELECT programKind, programPhaseCount, programPhaseLabelsCsv, " +
                    "currentProgramPhaseIndex, currentProgramCycle, nextProgramDayPosition " +
                    "FROM gym_routines WHERE id = 7",
            ).use { cursor ->
                check(cursor.moveToFirst())
                assertEquals("Static", cursor.getString(0))
                assertEquals(1, cursor.getInt(1))
                assertEquals("", cursor.getString(2))
                assertEquals(0, cursor.getInt(3))
                assertEquals(1, cursor.getInt(4))
                assertEquals(0, cursor.getInt(5))
            }
            database.query("SELECT progressionIndex FROM routine_days WHERE routineId = 7 ORDER BY position").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(2, cursor.getInt(0))
                assertTrue(cursor.moveToNext())
                assertEquals(2, cursor.getInt(0))
                assertFalse(cursor.moveToNext())
            }
        }
    }

    @Test
    fun migrationThirtyTwoToThirtyThreeAddsTypedProgramSemanticsAndInfersGeneratedSets() {
        helper.createDatabase(V32_DATABASE_NAME, 32).apply {
            execSQL("PRAGMA foreign_keys = OFF")
            execSQL(
                "INSERT INTO gym_routines (id, uuid, name, notes, position, archived, pinned, " +
                    "createdAtMillis, updatedAtMillis, programKind, programPhaseCount, " +
                    "programPhaseLabelsCsv, currentProgramPhaseIndex, currentProgramCycle, nextProgramDayPosition) " +
                    "VALUES (1, 'routine', '5/3/1', '', 0, 0, 0, 1, 1, 'BoringButBig', 4, " +
                    "'5s Week,3s Week,5/3/1 Week,Deload', 0, 1, 0)",
            )
            execSQL(
                "INSERT INTO routine_exercises (id, uuid, routineDayId, exerciseId, position, notes, groupKey, " +
                    "copyPreviousWorkout, createdAtMillis, updatedAtMillis, machineId, equipmentBindingState, " +
                    "machineProfileUuidSnapshot, machineNameSnapshot, machineLoadTypeSnapshot, machineUnitIdSnapshot, " +
                    "machineLevelLabelSnapshot, machineLoadInterpretationSnapshot, machineConfigurationGroupSnapshot, " +
                    "machineConfigurationVersionSnapshot, machineConfigurationSnapshot, trainingMaxPercent, " +
                    "progressionPercentagesCsv, alternativeExerciseIdsCsv, trainingMaxKg, trainingMaxValue, " +
                    "trainingMaxUnitId, cycleIncrementValue, trainingMaxSource) VALUES " +
                    "(2, 'placement', 999, 999, 0, '', NULL, 0, 1, 1, NULL, 'None', NULL, '', '', '', '', " +
                    "'Total', '', 1, '', 90, '', '', 90, 200, 'pound', 5, 'Explicit')",
            )
            execSQL(
                "INSERT INTO routine_sets (id, uuid, routineExerciseId, position, classification, enteredWeight, " +
                    "enteredWeightUnitId, repetitions, enteredDistance, enteredDistanceUnitId, durationSeconds, " +
                    "bodyweightKg, note, rpe, rir, tempo, restSeconds, createdAtMillis, updatedAtMillis, " +
                    "machineLoadValue, unilateral, repetitionsMax, loadPrescriptionType, loadPercentage, routinePhaseIndex) " +
                    "VALUES (3, 'main', 2, 0, 'Amrap', NULL, NULL, 5, NULL, NULL, NULL, NULL, " +
                    "'Main Work · 5s Week · 85% TM', NULL, NULL, '', NULL, 1, 1, NULL, 0, NULL, " +
                    "'PercentTrainingMax', 85, 0)",
            )
            execSQL(
                "INSERT INTO routine_sets (id, uuid, routineExerciseId, position, classification, enteredWeight, " +
                    "enteredWeightUnitId, repetitions, enteredDistance, enteredDistanceUnitId, durationSeconds, " +
                    "bodyweightKg, note, rpe, rir, tempo, restSeconds, createdAtMillis, updatedAtMillis, " +
                    "machineLoadValue, unilateral, repetitionsMax, loadPrescriptionType, loadPercentage, routinePhaseIndex) " +
                    "VALUES (4, 'supplemental', 2, 1, 'BackOff', NULL, NULL, 10, NULL, NULL, NULL, NULL, " +
                    "'Supplemental · 50% TM', NULL, NULL, '', NULL, 1, 1, NULL, 0, NULL, " +
                    "'PercentTrainingMax', 50, NULL)",
            )
            close()
        }

        helper.runMigrationsAndValidate(
            V32_DATABASE_NAME,
            33,
            true,
            WhipDatabase.migration32To33,
        ).use { database ->
            database.query(
                "SELECT trainingMaxIncreaseEligible, programPhaseRolesCsv, " +
                    "trainingMaxAdvanceAfterPhaseIndicesCsv FROM gym_routines WHERE id = 1",
            ).use { cursor ->
                check(cursor.moveToFirst())
                assertEquals(1, cursor.getInt(0))
                assertEquals("Standard,Standard,Standard,Deload", cursor.getString(1))
                assertEquals("3", cursor.getString(2))
            }
            database.query(
                "SELECT mainWorkScheme, supplementalScheme, assistanceRole, jokerSetsEnabled " +
                    "FROM routine_exercises WHERE id = 2",
            ).use { cursor ->
                check(cursor.moveToFirst())
                assertEquals("ClassicPrSet", cursor.getString(0))
                assertEquals("BoringButBig", cursor.getString(1))
                assertEquals("MainLift", cursor.getString(2))
                assertEquals(0, cursor.getInt(3))
            }
            database.query(
                "SELECT workSection, optionalWorkKind, mainWorkScheme, supplementalScheme " +
                    "FROM routine_sets ORDER BY id",
            ).use { cursor ->
                check(cursor.moveToFirst())
                assertEquals("Main", cursor.getString(0))
                assertEquals("None", cursor.getString(1))
                assertEquals("", cursor.getString(2))
                assertEquals("", cursor.getString(3))
                check(cursor.moveToNext())
                assertEquals("Supplemental", cursor.getString(0))
                assertEquals("None", cursor.getString(1))
                assertEquals("", cursor.getString(2))
                assertEquals("", cursor.getString(3))
            }
        }
    }

    @Test
    fun migrationThirtyThreeToThirtyFourPreservesSessionsAndDefaultsMainWorkInvalidation() {
        helper.createDatabase(V33_DATABASE_NAME, 33).apply {
            execSQL(
                """
                INSERT INTO workout_sessions (
                    id, uuid, name, notes, startedAtMillis, endedAtMillis, localEpochDay,
                    zoneId, state, keepScreenAwake, restTimerDeadlineMillis,
                    restTimerDurationSeconds, archived, createdAtMillis, updatedAtMillis,
                    sourceRoutineId, sourceRoutineDayId, sourceRoutineProgramKind,
                    sourceRoutinePhaseIndex, sourceRoutineCycle, sourceRoutineDayPosition,
                    sourceRoutineDayProgressionIndex, programProgressAdvanced,
                    sourceRoutinePhaseLabel, sourceRoutinePhaseRole
                ) VALUES (
                    7, 'session-7', 'Preserved workout', '', 100, NULL, 1,
                    'UTC', 'Active', 0, NULL, NULL, 0, 100, 100,
                    NULL, NULL, 'Static', NULL, NULL, NULL, NULL, 0, '', 'Standard'
                )
                """.trimIndent(),
            )
            close()
        }

        helper.runMigrationsAndValidate(
            V33_DATABASE_NAME,
            34,
            true,
            WhipDatabase.migration33To34,
        ).use { database ->
            database.query(
                "SELECT name, requiredMainWorkInvalidated FROM workout_sessions WHERE id = 7",
            ).use { cursor ->
                check(cursor.moveToFirst())
                assertEquals("Preserved workout", cursor.getString(0))
                assertEquals(0, cursor.getInt(1))
            }
        }
    }

    @Test
    fun migrationThirtyFourToThirtyFivePreservesProgramAndMarksUnknownFiveThreeOneProvenance() {
        helper.createDatabase(V34_DATABASE_NAME, 34).apply {
            execSQL(
                """
                INSERT INTO gym_routines (
                    id, uuid, name, notes, position, archived, pinned, createdAtMillis,
                    updatedAtMillis, programKind, programPhaseCount, programPhaseLabelsCsv,
                    currentProgramPhaseIndex, currentProgramCycle, nextProgramDayPosition,
                    trainingMaxIncreaseEligible, programPhaseRolesCsv,
                    trainingMaxAdvanceAfterPhaseIndicesCsv
                ) VALUES (
                    8, 'routine-8', 'Older custom program', 'User notes stay intact', 0, 0, 0,
                    100, 200, 'FiveThreeOne', 4, '5s Week,3s Week,5/3/1 Week,Deload',
                    1, 3, 2, 1, 'Standard,Standard,Standard,Deload', '3'
                )
                """.trimIndent(),
            )
            close()
        }

        helper.runMigrationsAndValidate(
            V34_DATABASE_NAME,
            35,
            true,
            WhipDatabase.migration34To35,
        ).use { database ->
            database.query(
                "SELECT name, notes, programTemplateKey, programTemplateRevision " +
                    "FROM gym_routines WHERE id = 8",
            ).use { cursor ->
                check(cursor.moveToFirst())
                assertEquals("Older custom program", cursor.getString(0))
                assertEquals("User notes stay intact", cursor.getString(1))
                assertEquals("LegacyFiveThreeOne", cursor.getString(2))
                assertEquals(1, cursor.getInt(3))
            }
        }
    }

    @Test
    fun migrationThirtyFiveToThirtySixPreservesTrainingMaxAndAddsSafeReviewState() {
        helper.createDatabase(V35_DATABASE_NAME, 35).apply {
            execSQL("PRAGMA foreign_keys = OFF")
            execSQL(
                "INSERT INTO gym_routines (id, uuid, name, notes, position, archived, pinned, createdAtMillis, " +
                    "updatedAtMillis, programKind, programPhaseCount, programPhaseLabelsCsv, currentProgramPhaseIndex, " +
                    "currentProgramCycle, nextProgramDayPosition, trainingMaxIncreaseEligible, programPhaseRolesCsv, " +
                    "trainingMaxAdvanceAfterPhaseIndicesCsv, programTemplateKey, programTemplateRevision) VALUES " +
                    "(1, 'routine-36', 'Legacy 5/3/1', '', 0, 0, 0, 1, 1, 'FiveThreeOne', 4, " +
                    "'5s,3s,5/3/1,Deload', 0, 2, 0, 0, 'Standard,Standard,Standard,Deload', '3', " +
                    "'LegacyFiveThreeOne', 1)",
            )
            execSQL(
                "INSERT INTO routine_days (id, uuid, routineId, name, position, createdAtMillis, updatedAtMillis, progressionIndex) " +
                    "VALUES (999, 'day-36', 1, 'Press', 0, 1, 1, 0)",
            )
            execSQL(
                "INSERT INTO routine_exercises (id, uuid, routineDayId, exerciseId, position, notes, groupKey, " +
                    "copyPreviousWorkout, createdAtMillis, updatedAtMillis, machineId, equipmentBindingState, " +
                    "machineProfileUuidSnapshot, machineNameSnapshot, machineLoadTypeSnapshot, machineUnitIdSnapshot, " +
                    "machineLevelLabelSnapshot, machineLoadInterpretationSnapshot, machineConfigurationGroupSnapshot, " +
                    "machineConfigurationVersionSnapshot, machineConfigurationSnapshot, trainingMaxPercent, " +
                    "progressionPercentagesCsv, alternativeExerciseIdsCsv, trainingMaxKg, trainingMaxValue, " +
                    "trainingMaxUnitId, cycleIncrementValue, trainingMaxSource, mainWorkScheme, supplementalScheme, " +
                    "assistanceRole, placementKind, assistanceCategory, jokerSetsEnabled) VALUES " +
                    "(2, 'placement-36', 999, 999, 0, '', NULL, 0, 1, 1, NULL, 'None', NULL, '', '', '', '', " +
                    "'Total', '', 1, '', 85, '', '', 90.718474, 200, 'pound', 5, 'Explicit', " +
                    "'ClassicPrSet', 'FirstSetLast', 'MainLift', 'MainLift', 'Unspecified', 1)",
            )
            close()
        }

        helper.runMigrationsAndValidate(
            V35_DATABASE_NAME,
            36,
            true,
            WhipDatabase.migration35To36,
        ).use { database ->
            database.query(
                "SELECT progressionMode, allowNonStandardHigherSuggestions FROM gym_routines WHERE id = 1",
            ).use { cursor ->
                check(cursor.moveToFirst())
                assertEquals("Standard", cursor.getString(0))
                assertEquals(0, cursor.getInt(1))
            }
            database.query(
                "SELECT trainingMaxValue, trainingMaxBasisKind, trainingMaxBasisValue, " +
                    "trainingMaxBasisUnitId, trainingMaxIncreaseEligible FROM routine_exercises WHERE id = 2",
            ).use { cursor ->
                check(cursor.moveToFirst())
                assertEquals(200.0, cursor.getDouble(0), 0.0)
                assertEquals("ExplicitTrainingMax", cursor.getString(1))
                assertEquals(200.0, cursor.getDouble(2), 0.0)
                assertEquals("pound", cursor.getString(3))
                assertEquals(0, cursor.getInt(4))
            }
            database.query("SELECT COUNT(*) FROM training_max_decisions").use { cursor ->
                check(cursor.moveToFirst())
                assertEquals(0, cursor.getInt(0))
            }
        }
    }

    @Test
    fun migrationThirtySixToThirtySevenSnapshotsExistingSetClassification() {
        helper.createDatabase(V36_DATABASE_NAME, 36).apply {
            execSQL("PRAGMA foreign_keys = OFF")
            execSQL(
                "INSERT INTO workout_sets (id, uuid, workoutExerciseId, position, classification, " +
                    "planned, completed, note, tempo, createdAtMillis, updatedAtMillis, unilateral, " +
                    "prescriptionSourceLabel, workSectionSnapshot, optionalWorkKindSnapshot) VALUES " +
                    "(1, 'tm-test-set', 999, 0, 'TrainingMaxTest', 1, 0, '', '', 1, 1, 0, '', 'Main', 'None'), " +
                    "(2, 'failed-set', 999, 1, 'Failure', 0, 1, '', '', 1, 1, 0, '', 'Main', 'None')",
            )
            close()
        }

        helper.runMigrationsAndValidate(
            V36_DATABASE_NAME,
            37,
            true,
            WhipDatabase.migration36To37,
        ).use { database ->
            database.query(
                "SELECT classification, prescribedClassificationSnapshot FROM workout_sets ORDER BY id",
            ).use { cursor ->
                check(cursor.moveToFirst())
                assertEquals("TrainingMaxTest", cursor.getString(0))
                assertEquals("TrainingMaxTest", cursor.getString(1))
                check(cursor.moveToNext())
                assertEquals("Failure", cursor.getString(0))
                assertEquals("Failure", cursor.getString(1))
                assertFalse(cursor.moveToNext())
            }
        }
    }

    @Test
    fun migrationThirtySevenToThirtyEightSeparatesArchiveLifecycleAndAddsResetHistory() {
        helper.createDatabase(V37_DATABASE_NAME, 37).apply {
            insertMainArea()
            fun insertGoal(id: Long, uuid: String, status: String) {
                execSQL(
                    "INSERT INTO goals (id, uuid, metricId, name, description, areaId, area, tagsCsv, icon, " +
                        "type, dimension, unitId, precision, baseline, targetMin, targetMax, direction, " +
                        "startEpochDay, deadlineEpochDay, aggregation, aggregationPeriod, rollingDays, paceType, " +
                        "consistencyPeriod, consistencyRequiredPeriods, elapsedStartMillis, elapsedDisplayUnit, " +
                        "reminderMinutes, status, pinned, position, createdAtMillis, updatedAtMillis) VALUES " +
                        "($id, '$uuid', 'metric-$id', 'Goal $id', '', 'main', 'Main', '', '🎯', " +
                        "'ReachValue', 'Count', 'count', 1, 0, 10, NULL, 'Increase', 20690, NULL, " +
                        "'Latest', 'All', NULL, 'None', 'Week', NULL, NULL, 'Auto', NULL, '$status', 0, $id, 100, 100)",
                )
            }
            insertGoal(1, "archived-closed", "Archived")
            insertGoal(2, "archived-open", "Archived")
            insertGoal(3, "completed-visible", "Completed")
            execSQL(
                "INSERT INTO goal_completion_snapshots " +
                    "(id, goalId, completedAtMillis, value, progress, status) " +
                    "VALUES (1, 1, 500, 8, 0.8, 'Abandoned')",
            )
            close()
        }

        helper.runMigrationsAndValidate(
            V37_DATABASE_NAME,
            38,
            true,
            WhipDatabase.migration37To38,
        ).use { database ->
            database.query("SELECT uuid, status, archived FROM goals ORDER BY id").use { cursor ->
                check(cursor.moveToFirst())
                assertEquals("archived-closed", cursor.getString(0))
                assertEquals("Abandoned", cursor.getString(1))
                assertEquals(1, cursor.getInt(2))
                check(cursor.moveToNext())
                assertEquals("archived-open", cursor.getString(0))
                assertEquals("Active", cursor.getString(1))
                assertEquals(1, cursor.getInt(2))
                check(cursor.moveToNext())
                assertEquals("completed-visible", cursor.getString(0))
                assertEquals("Completed", cursor.getString(1))
                assertEquals(0, cursor.getInt(2))
            }
            database.query("SELECT COUNT(*) FROM goal_elapsed_reset_events").use { cursor ->
                check(cursor.moveToFirst())
                assertEquals(0, cursor.getInt(0))
            }
            database.query("SELECT uuid FROM goal_completion_snapshots WHERE id = 1").use { cursor ->
                check(cursor.moveToFirst())
                assertEquals("legacy-goal-closure:archived-closed:1", cursor.getString(0))
            }
        }
    }

    @Test
    fun migrationThirtyEightToThirtyNinePreservesTrackHistoryAndSearchAndCreatesCascadingEmptyReceipts() {
        helper.createDatabase(V38_DATABASE_NAME, 38).apply {
            insertMainArea()
            execSQL(
                "INSERT INTO tracks " +
                    "(id, uuid, name, description, icon, areaId, area, tagsCsv, pinned, archived, position, createdAtMillis, updatedAtMillis) " +
                    "VALUES (41, 'track-migration-38', 'Migration Track', '', '📋', 'main', 'Main', '', 0, 0, 0, 100, 200)",
            )
            execSQL(
                "INSERT INTO track_fields " +
                    "(id, uuid, trackId, name, type, position, required, primaryField, showInList, dimension, unitId, precision, scaleMin, scaleMax, scaleLowLabel, scaleHighLabel, createdAtMillis, updatedAtMillis, scaleStep) " +
                    "VALUES (42, 'field-migration-38', 41, 'Title', 'ShortText', 0, 1, 1, 1, NULL, NULL, 0, NULL, NULL, '', '', 100, 200, 1)",
            )
            execSQL(
                "INSERT INTO track_entries " +
                    "(id, uuid, trackId, entryEpochDay, sourceOccurrenceId, sourceExplanation, createdAtMillis, updatedAtMillis) " +
                    "VALUES (43, 'entry-migration-38', 41, 20697, NULL, '', 300, 400)",
            )
            execSQL(
                "INSERT INTO track_values " +
                    "(id, uuid, entryId, fieldId, textValue, enteredNumber, canonicalNumber, enteredUnitId, dateEpochDay, booleanValue, choiceOptionId, scaleValue, createdAtMillis, updatedAtMillis) " +
                    "VALUES (44, 'value-migration-38', 43, 42, 'Preserved searchable history', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 300, 400)",
            )
            execSQL(
                "INSERT INTO track_entry_search (rowid, trackId, content) " +
                    "VALUES (43, 41, 'Migration Track Title Preserved searchable history')",
            )
            close()
        }

        helper.runMigrationsAndValidate(
            V38_DATABASE_NAME,
            39,
            true,
            WhipDatabase.migration38To39,
        ).use { database ->
            database.query(
                "SELECT t.uuid, f.uuid, e.uuid, e.entryEpochDay, v.uuid, v.textValue " +
                    "FROM tracks t JOIN track_fields f ON f.trackId = t.id " +
                    "JOIN track_entries e ON e.trackId = t.id " +
                    "JOIN track_values v ON v.entryId = e.id AND v.fieldId = f.id WHERE t.id = 41",
            ).use { cursor ->
                check(cursor.moveToFirst())
                assertEquals("track-migration-38", cursor.getString(0))
                assertEquals("field-migration-38", cursor.getString(1))
                assertEquals("entry-migration-38", cursor.getString(2))
                assertEquals(20697L, cursor.getLong(3))
                assertEquals("value-migration-38", cursor.getString(4))
                assertEquals("Preserved searchable history", cursor.getString(5))
            }
            database.query("SELECT rowid, trackId, content FROM track_entry_search WHERE rowid = 43").use { cursor ->
                check(cursor.moveToFirst())
                assertEquals(43L, cursor.getLong(0))
                assertEquals(41L, cursor.getLong(1))
                assertTrue(cursor.getString(2).contains("Preserved searchable history"))
            }
            database.query("SELECT COUNT(*) FROM track_csv_import_receipts").use { cursor ->
                check(cursor.moveToFirst())
                assertEquals(0, cursor.getInt(0))
            }

            // MigrationTestHelper opens the raw SupportSQLiteDatabase without Room's
            // normal foreign-key configuration; enable it to exercise the declared
            // on-delete lifecycle rather than only inspecting the schema.
            database.execSQL("PRAGMA foreign_keys = ON")
            database.execSQL(
                "INSERT INTO track_csv_import_receipts " +
                    "(batchUuid, trackId, trackUuid, trackCreatedAtMillis, requestFingerprint, fingerprintVersion, " +
                    "entryIdentityDigest, rowCount, identityVersion, committedAtMillis) " +
                    "VALUES ('38f00aae-477c-4fcb-8082-c35e29e70ddd', 41, 'track-migration-38', 100, " +
                    "'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa', 1, " +
                    "'bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb', 1, 1, 500)",
            )
            database.execSQL("DELETE FROM tracks WHERE id = 41")
            database.query("SELECT COUNT(*) FROM track_csv_import_receipts").use { cursor ->
                check(cursor.moveToFirst())
                assertEquals(0, cursor.getInt(0))
            }
        }
    }

    @Test
    fun migrationThirtyNineToFortyRecoversActiveRoutineRequirementsFromImmutablePrescriptions() {
        helper.createDatabase(V39_DATABASE_NAME, 39).apply {
            execSQL("PRAGMA foreign_keys = OFF")
            execSQL(
                "INSERT INTO workout_sessions " +
                    "(id, uuid, name, notes, startedAtMillis, localEpochDay, zoneId, state, keepScreenAwake, " +
                    "archived, createdAtMillis, updatedAtMillis, sourceRoutineId, sourceRoutineProgramKind, " +
                    "programProgressAdvanced, requiredMainWorkInvalidated, invalidatedMainExerciseIdsCsv, " +
                    "sourceRoutinePhaseLabel, sourceRoutinePhaseRole) VALUES " +
                    "(1, 'session-39', 'Active routine workout', '', 100, 1, 'UTC', 'Active', 0, 0, 100, 200, " +
                    "77, 'Static', 0, 0, '', '', 'Standard')",
            )
            execSQL(
                "INSERT INTO workout_exercises " +
                    "(id, uuid, sessionId, exerciseId, position, notes, createdAtMillis, updatedAtMillis, " +
                    "machineNameSnapshot, machineLoadTypeSnapshot, machineUnitIdSnapshot, machineLevelLabelSnapshot, " +
                    "loadInterpretationSnapshot, trackingTypeSnapshot, bodyweightLoadPolicySnapshot, " +
                    "effectiveBodyweightPercentSnapshot, oneRepMaxFormulaSnapshot, includeInVolumeSnapshot, " +
                    "includeInPersonalRecordsSnapshot, exerciseWeightUnitSnapshot, loadMultiplierSnapshot, " +
                    "machineConfigurationGroupSnapshot, machineConfigurationVersionSnapshot, machineConfigurationSnapshot, " +
                    "machinePulleyRatioSnapshot, machineStackModeSnapshot, machineMassMappingCsvSnapshot, " +
                    "alternativeExerciseIdsCsvSnapshot, trainingMaxUnitIdSnapshot, trainingMaxSourceSnapshot, " +
                    "mainWorkSchemeSnapshot, supplementalSchemeSnapshot, assistanceRoleSnapshot, placementKindSnapshot, " +
                    "assistanceCategorySnapshot, jokerSetsEnabledSnapshot) VALUES " +
                    "(2, 'placement-39', 1, 99, 0, '', 100, 200, '', '', '', '', 'Total', 'WeightReps', " +
                    "'ExternalWeightOnly', 100, 'Epley', 1, 1, 'kilogram', 1, '', 1, '', 1, 'Single', '', '', '', " +
                    "'EstimatedOneRepMaxPercent', 'ClassicPrSet', 'FirstSetLast', 'MainLift', 'MainLift', 'Unspecified', 1), " +
                    // An exercise plus initial set can share a later transaction timestamp.
                    "(8, 'workout-only-placement-39', 1, 100, 1, '', 150, 200, '', '', '', '', 'Total', " +
                    "'WeightReps', 'ExternalWeightOnly', 100, 'Epley', 1, 1, 'kilogram', 1, '', 1, '', 1, " +
                    "'Single', '', '', '', 'EstimatedOneRepMaxPercent', 'Unspecified', 'None', 'Unspecified', " +
                    "'Unspecified', 'Unspecified', 0)",
            )
            execSQL(
                "INSERT INTO workout_sets " +
                    "(id, uuid, workoutExerciseId, position, classification, planned, completed, note, tempo, " +
                    "deletedAtMillis, createdAtMillis, updatedAtMillis, unilateral, prescriptionSourceLabel, " +
                    "workSectionSnapshot, optionalWorkKindSnapshot, prescribedClassificationSnapshot, " +
                    "prescribedRepetitions) VALUES " +
                    // Real completion paths clear `planned`; immutable targets must retain both outcomes.
                    "(3, 'success-39', 2, 0, 'Working', 0, 1, '', '', NULL, 100, 200, 0, '', " +
                    "'Unspecified', 'None', 'Working', 5), " +
                    "(4, 'failure-39', 2, 1, 'Failure', 0, 1, '', '', NULL, 100, 200, 0, '', " +
                    "'Unspecified', 'None', 'Working', 5), " +
                    // Optional authored work and an in-workout planned row are never requirements.
                    "(5, 'optional-39', 2, 2, 'Working', 1, 0, '', '', 190, 100, 200, 0, '', " +
                    "'Optional', 'Joker', 'Working', 3), " +
                    // Creation provenance covers authored dimensions without a legacy prescribed column.
                    "(6, 'source-only-39', 2, 3, 'Working', 0, 0, '', '', NULL, 100, 200, 0, '', " +
                    "'Unspecified', 'None', 'Working', NULL), " +
                    "(7, 'workout-only-39', 2, 4, 'Working', 1, 0, '', '', NULL, 150, 200, 0, '', " +
                    "'Unspecified', 'None', 'Working', NULL), " +
                    "(9, 'workout-only-initial-39', 8, 0, 'Working', 1, 0, '', '', NULL, 150, 200, 0, '', " +
                    "'Unspecified', 'None', 'Working', NULL)",
            )
            close()
        }

        helper.runMigrationsAndValidate(
            V39_DATABASE_NAME,
            40,
            true,
            WhipDatabase.migration39To40,
        ).use { database ->
            database.query(
                "SELECT workoutRevision, restTimerRevision, restTimerCleanupPending " +
                    "FROM workout_sessions WHERE id = 1",
            ).use { cursor ->
                check(cursor.moveToFirst())
                assertEquals(0L, cursor.getLong(0))
                assertEquals(0L, cursor.getLong(1))
                assertEquals(1, cursor.getInt(2))
            }
            database.query(
                "SELECT outcome, outcomeAtMillis, replacementWorkoutExerciseUuid FROM workout_exercises WHERE id = 2",
            ).use { cursor ->
                check(cursor.moveToFirst())
                assertEquals("Active", cursor.getString(0))
                assertTrue(cursor.isNull(1))
                assertTrue(cursor.isNull(2))
            }
            database.query(
                "SELECT requiredForProgressionSnapshot, removalReason FROM workout_sets ORDER BY id",
            ).use { cursor ->
                check(cursor.moveToFirst())
                assertEquals(1, cursor.getInt(0))
                assertTrue(cursor.isNull(1))
                check(cursor.moveToNext())
                assertEquals(1, cursor.getInt(0))
                assertTrue(cursor.isNull(1))
                check(cursor.moveToNext())
                assertEquals(0, cursor.getInt(0))
                assertEquals("Removed", cursor.getString(1))
                check(cursor.moveToNext())
                assertEquals(1, cursor.getInt(0))
                assertTrue(cursor.isNull(1))
                check(cursor.moveToNext())
                assertEquals(0, cursor.getInt(0))
                assertTrue(cursor.isNull(1))
                check(cursor.moveToNext())
                assertEquals(0, cursor.getInt(0))
                assertTrue(cursor.isNull(1))
                assertFalse(cursor.moveToNext())
            }
        }
    }

    @Test
    fun migrationFortyToFortyOneMovesActiveHabitTimerIntoReviewWithoutChangingHistory() {
        helper.createDatabase(V40_DATABASE_NAME, 40).apply {
            execSQL(
                "INSERT INTO metric_definitions (id, name, valueKind, dimension, defaultUnitId, precision, dimensionLocked, archived, createdAtMillis, updatedAtMillis) " +
                    "VALUES ('metric-duration', 'Meditation', 'Duration', 'Duration', 'minute', 2, 0, 0, 1, 1)",
            )
            execSQL(
                """
                INSERT INTO habits (
                    id, uuid, metricId, name, notes, areaId, area, tagsCsv, icon,
                    trackingMode, dimension, unitId, precision, comparison, targetMin,
                    targetMax, targetPeriod, rollingDays, scheduleType, scheduleInterval,
                    weekdaysMask, flexibleTimesPerWeek, startEpochDay, endType, endEpochDay,
                    endValue, quickIncrement, quickActionsCsv, reminderMinutesCsv,
                    weekdayReminderMinutesCsv, weekStart, timerStartedAtMillis, pinned,
                    position, archived, paused, createdAtMillis, updatedAtMillis, sourceMetricId,
                    autoCompleteFromItems
                ) VALUES (
                    3, 'habit-timer', 'metric-duration', 'Meditation', '', NULL, '', '', '🧘',
                    'Duration', 'Duration', 'minute', 2, 'AtLeast', 5, NULL, 'Day', NULL,
                    'Daily', 1, 0, NULL, 20690, 'Never', NULL, NULL, 1, '', '', '',
                    'MONDAY', 1000, 0, 0, 0, 0, 1, 1, NULL, 1
                )
                """.trimIndent(),
            )
            close()
        }

        helper.runMigrationsAndValidate(
            V40_DATABASE_NAME,
            41,
            true,
            WhipDatabase.migration40To41,
        ).use { database ->
            database.query(
                "SELECT timerSessionId, timerNeedsReview, timerAccumulatedSeconds, timerStartedAtMillis, " +
                    "timerAnchorElapsedRealtimeMillis " +
                    "FROM habits WHERE id = 3",
            ).use { cursor ->
                check(cursor.moveToFirst())
                assertEquals("habit-timer-v1:legacy:habit-timer:1000", cursor.getString(0))
                assertEquals(1, cursor.getInt(1))
                assertEquals(0.0, cursor.getDouble(2), 0.0)
                assertEquals(1000L, cursor.getLong(3))
                assertTrue(cursor.isNull(4))
            }
            database.query(
                "SELECT sessionId, habitId, activeHabitId, state, anchorWallMillis, " +
                    "anchorElapsedRealtimeMillis, anchorBootId, accumulatedCanonicalSeconds, unitId " +
                    "FROM habit_timer_sessions",
            ).use { cursor ->
                check(cursor.moveToFirst())
                assertEquals("habit-timer-v1:legacy:habit-timer:1000", cursor.getString(0))
                assertEquals(3L, cursor.getLong(1))
                assertEquals(3L, cursor.getLong(2))
                assertEquals("ReviewRequired", cursor.getString(3))
                assertEquals(1000L, cursor.getLong(4))
                assertTrue(cursor.isNull(5))
                assertTrue(cursor.isNull(6))
                assertEquals(0.0, cursor.getDouble(7), 0.0)
                assertEquals("minute", cursor.getString(8))
            }
            database.query("SELECT COUNT(*) FROM habit_logs").use { cursor ->
                check(cursor.moveToFirst())
                assertEquals(0, cursor.getInt(0))
            }
        }
    }

    private fun androidx.sqlite.db.SupportSQLiteDatabase.insertMainArea() {
        execSQL(
            "INSERT INTO areas (id, name, nameKey, position, archived, createdAtMillis, updatedAtMillis) " +
                "VALUES ('main', 'Main', 'main', 0, 0, 100, 100)",
        )
    }

    private fun androidx.sqlite.db.SupportSQLiteDatabase.insertSchemaOneGoal() {
        execSQL(
            """
            INSERT INTO goals (
                id, uuid, metricId, name, description, areaId, area, tagsCsv, icon,
                type, dimension, unitId, precision, baseline, targetMin, targetMax,
                direction, startEpochDay, deadlineEpochDay, aggregation,
                aggregationPeriod, rollingDays, paceType, consistencyPeriod,
                consistencyRequiredPeriods, reminderMinutes, status, pinned,
                position, createdAtMillis, updatedAtMillis
            ) VALUES (
                7, 'goal-7', 'metric-7', 'Preserved Goal', '', 'main', 'Main', '', '🎯',
                'ReachValue', 'Count', 'count', 1, 0, 50, NULL,
                'Increase', 20690, NULL, 'Sum', 'All', NULL, 'None', 'Week',
                NULL, NULL, 'Active', 1, 0, 100, 100
            )
            """.trimIndent(),
        )
    }

    private fun androidx.sqlite.db.SupportSQLiteDatabase.insertSchemaOneTask() {
        execSQL(
            """
            INSERT INTO tasks (
                id, uuid, title, notes, scheduleKind, dateEpochDay, recurrenceUnit,
                recurrenceInterval, weekdaysMask, recurrenceEnd, recurrenceEndEpochDay,
                recurrenceCount, timeMinutes, reminderEnabled, archived, completedAtMillis,
                createdAtMillis, updatedAtMillis, showSubtaskProgress, progressDisplay,
                autoCompleteFromSteps, repeatStepPolicy, pinned, priority, areaId, area,
                tagsCsv, deadlineEpochDay, recurrenceAnchor, reminderOffsetsMinutesCsv,
                missedOccurrencePolicy, inbox, durationMinutes, effort, manualPosition
            ) VALUES (
                5, 'task-5', 'Preserved Task', '', 'Anytime', NULL, NULL,
                1, 0, NULL, NULL, NULL, NULL, 0, 0, NULL,
                100, 100, 0, 'Percent', 1, 'Reset', 0, 'None', 'main', 'Main',
                '', NULL, 'Schedule', '', 'KeepLatest', 1, NULL, 'Unspecified', 0
            )
            """.trimIndent(),
        )
    }

    private fun androidx.sqlite.db.SupportSQLiteDatabase.insertSchemaOneTrigger(
        id: Long,
        uuid: String,
        targetType: String,
        autoCompleteTargetHabit: Int,
    ) {
        execSQL(
            """
            INSERT INTO trigger_rules (
                id, uuid, name, sourceType, sourceEntityId, outcome, targetType,
                targetEntityId, delayMinutes, quietStartMinutes, quietEndMinutes,
                autoCompleteTargetHabit, enabled, createdAtMillis, updatedAtMillis
            ) VALUES (
                $id, '$uuid', 'Preserved Trigger', 'Task', 5, 'Completed', '$targetType',
                9, 0, NULL, NULL, $autoCompleteTargetHabit, 1, 100, 100
            )
            """.trimIndent(),
        )
    }

    private companion object {
        const val V1_DATABASE_NAME = "complete-v1-to-v9-migration"
        const val V2_DATABASE_NAME = "complete-v2-to-v9-migration"
        const val V8_DATABASE_NAME = "habit-skip-v8-to-v9-migration"
        const val V27_DATABASE_NAME = "public-v27-to-v28-migration"
        const val V28_DATABASE_NAME = "checklist-v28-to-v29-migration"
        const val V29_DATABASE_NAME = "machines-v29-to-v30-migration"
        const val V30_DATABASE_NAME = "automation-retirement-v30-to-v31-migration"
        const val V31_DATABASE_NAME = "routine-program-v31-to-v32-migration"
        const val V32_DATABASE_NAME = "typed-program-v32-to-v33-migration"
        const val V33_DATABASE_NAME = "main-work-safety-v33-to-v34-migration"
        const val V34_DATABASE_NAME = "program-provenance-v34-to-v35-migration"
        const val V35_DATABASE_NAME = "adaptive-training-max-v35-to-v36-migration"
        const val V36_DATABASE_NAME = "prescribed-classification-v36-to-v37-migration"
        const val V37_DATABASE_NAME = "goal-lifecycle-v37-to-v38-migration"
        const val V38_DATABASE_NAME = "track-csv-receipt-v38-to-v39-migration"
        const val V39_DATABASE_NAME = "workout-revision-v39-to-v40-migration"
        const val V40_DATABASE_NAME = "habit-timer-v40-to-v41-migration"

        val allMigrations: Array<Migration> = arrayOf(
            WhipDatabase.migration1To2,
            WhipDatabase.migration2To3,
            WhipDatabase.migration3To4,
            WhipDatabase.migration4To5,
            WhipDatabase.migration5To6,
            WhipDatabase.migration6To7,
            WhipDatabase.migration7To8,
            WhipDatabase.migration8To9,
            WhipDatabase.migration9To28,
            WhipDatabase.migration28To29,
            WhipDatabase.migration29To30,
            WhipDatabase.migration30To31,
            WhipDatabase.migration31To32,
            WhipDatabase.migration32To33,
            WhipDatabase.migration33To34,
            WhipDatabase.migration34To35,
            WhipDatabase.migration35To36,
            WhipDatabase.migration36To37,
            WhipDatabase.migration37To38,
            WhipDatabase.migration38To39,
            WhipDatabase.migration39To40,
            WhipDatabase.migration40To41,
        )
    }
}

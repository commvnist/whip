package com.whip.app

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.whip.app.data.MIGRATION_1_2
import com.whip.app.data.MIGRATION_2_3
import com.whip.app.data.MIGRATION_3_4
import com.whip.app.data.MIGRATION_4_5
import com.whip.app.data.MIGRATION_5_6
import com.whip.app.data.MIGRATION_6_7
import com.whip.app.data.MIGRATION_7_8
import com.whip.app.data.MIGRATION_8_9
import com.whip.app.data.MIGRATION_9_10
import com.whip.app.data.MIGRATION_10_11
import com.whip.app.data.MIGRATION_11_12
import com.whip.app.data.MIGRATION_12_13
import com.whip.app.data.MIGRATION_13_14
import com.whip.app.data.MIGRATION_14_15
import com.whip.app.data.MIGRATION_15_16
import com.whip.app.data.MIGRATION_16_17
import com.whip.app.data.MIGRATION_17_18
import com.whip.app.data.MIGRATION_18_19
import com.whip.app.data.MIGRATION_19_20
import com.whip.app.data.MIGRATION_20_21
import com.whip.app.data.MIGRATION_21_22
import com.whip.app.data.MIGRATION_22_23
import com.whip.app.data.MIGRATION_23_24
import com.whip.app.data.MIGRATION_24_25
import com.whip.app.data.MIGRATION_25_26
import com.whip.app.data.MIGRATION_26_27
import com.whip.app.data.WhipDatabase
import org.junit.Assert.assertEquals
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
    fun migrate1To27_preservesTasksAndAddsLinkFoundation() {
        helper.createDatabase(TEST_DATABASE, 1).apply {
            execSQL(
                """
                INSERT INTO tasks (
                    id, title, notes, scheduleKind, dateEpochDay, recurrenceUnit,
                    recurrenceInterval, weekdaysMask, recurrenceEnd,
                    recurrenceEndEpochDay, recurrenceCount, timeMinutes,
                    reminderEnabled, archived, completedAtMillis, createdAtMillis
                ) VALUES (
                    42, 'Existing task', '', 'Anytime', NULL, NULL,
                    1, 0, NULL, NULL, NULL, NULL, 0, 0, NULL, 1234
                )
                """.trimIndent(),
            )
            close()
        }

        helper.runMigrationsAndValidate(
            TEST_DATABASE,
            27,
            true,
            MIGRATION_1_2,
            MIGRATION_2_3,
            MIGRATION_3_4,
            MIGRATION_4_5,
            MIGRATION_5_6,
            MIGRATION_6_7,
            MIGRATION_7_8,
            MIGRATION_8_9,
            MIGRATION_9_10,
            MIGRATION_10_11,
            MIGRATION_11_12,
            MIGRATION_12_13,
            MIGRATION_13_14,
            MIGRATION_14_15,
            MIGRATION_15_16,
            MIGRATION_16_17,
            MIGRATION_17_18,
            MIGRATION_18_19,
            MIGRATION_19_20,
            MIGRATION_20_21,
            MIGRATION_21_22,
            MIGRATION_22_23,
            MIGRATION_23_24,
            MIGRATION_24_25,
            MIGRATION_25_26,
            MIGRATION_26_27,
        ).use { database ->
            database.query(
                "SELECT title, createdAtMillis, updatedAtMillis, " +
                    "showSubtaskProgress, uuid FROM tasks WHERE id = 42",
            ).use { cursor ->
                cursor.moveToFirst()
                assertEquals("Existing task", cursor.getString(0))
                assertEquals(1234L, cursor.getLong(1))
                assertEquals(1234L, cursor.getLong(2))
                assertEquals(0, cursor.getInt(3))
                assertEquals("legacy-task-42", cursor.getString(4))
            }

            database.query(
                "SELECT name FROM sqlite_master WHERE type = 'table' " +
                    "AND name IN ('task_steps', 'task_step_states', " +
                    "'task_step_snapshots') ORDER BY name",
            ).use { cursor ->
                assertEquals(3, cursor.count)
            }
            database.query(
                "SELECT name FROM sqlite_master WHERE type = 'table' " +
                    "AND name IN ('metric_definitions', 'metric_entries', " +
                    "'unit_definitions', 'areas', 'tags', 'entity_tag_links')",
            ).use { cursor ->
                assertEquals(6, cursor.count)
            }
            database.query(
                "SELECT name FROM sqlite_master WHERE type = 'table' " +
                    "AND name IN ('exercises', 'exercise_categories', " +
                    "'exercise_category_joins', 'workout_sessions', 'workout_groups', " +
                    "'workout_exercises', 'workout_sets')",
            ).use { cursor ->
                assertEquals(7, cursor.count)
            }
            database.query(
                "SELECT name FROM sqlite_master WHERE type = 'table' " +
                    "AND name IN ('gym_routines', 'routine_days', 'routine_exercises', " +
                    "'routine_sets', 'personal_records', 'graph_presets')",
            ).use { cursor -> assertEquals(6, cursor.count) }
            database.query(
                "SELECT name FROM sqlite_master WHERE type = 'table' " +
                    "AND name IN ('habits', 'habit_checklist_items', 'habit_logs', " +
                    "'habit_checklist_states', 'habit_pauses')",
            ).use { cursor -> assertEquals(5, cursor.count) }
            database.query(
                "SELECT name FROM sqlite_master WHERE type = 'table' " +
                    "AND name IN ('goals', 'goal_milestones', 'goal_completion_snapshots')",
            ).use { cursor -> assertEquals(3, cursor.count) }
            database.query(
                "SELECT name FROM sqlite_master WHERE type = 'table' " +
                    "AND name IN ('link_rules', 'contributions', 'trigger_rules', 'trigger_occurrences')",
            ).use { cursor -> assertEquals(4, cursor.count) }
        }
    }

    @Test
    fun migrate2To3_addsOccurrenceSnapshots() {
        helper.createDatabase(SECOND_TEST_DATABASE, 2).close()

        helper.runMigrationsAndValidate(
            SECOND_TEST_DATABASE,
            3,
            true,
            MIGRATION_2_3,
        ).use { database ->
            database.query(
                "SELECT name FROM sqlite_master WHERE type = 'table' " +
                    "AND name = 'task_step_snapshots'",
            ).use { cursor ->
                assertEquals(1, cursor.count)
            }
        }
    }

    @Test
    fun migrate3To4_addsMeasurementFoundation() {
        helper.createDatabase(THIRD_TEST_DATABASE, 3).close()
        helper.runMigrationsAndValidate(
            THIRD_TEST_DATABASE,
            4,
            true,
            MIGRATION_3_4,
        ).close()
    }

    @Test
    fun migrate4To5_addsGymFoundation() {
        helper.createDatabase(FOURTH_TEST_DATABASE, 4).close()
        helper.runMigrationsAndValidate(
            FOURTH_TEST_DATABASE,
            5,
            true,
            MIGRATION_4_5,
        ).close()
    }

    @Test
    fun migrate5To6_addsRoutinesAndRecords() {
        helper.createDatabase(FIFTH_TEST_DATABASE, 5).close()
        helper.runMigrationsAndValidate(
            FIFTH_TEST_DATABASE,
            6,
            true,
            MIGRATION_5_6,
        ).close()
    }

    @Test
    fun migrate6To7_addsHabits() {
        helper.createDatabase(SIXTH_TEST_DATABASE, 6).close()
        helper.runMigrationsAndValidate(
            SIXTH_TEST_DATABASE,
            7,
            true,
            MIGRATION_6_7,
        ).close()
    }

    @Test
    fun migrate7To8_addsGoals() {
        helper.createDatabase(SEVENTH_TEST_DATABASE, 7).close()
        helper.runMigrationsAndValidate(
            SEVENTH_TEST_DATABASE,
            8,
            true,
            MIGRATION_7_8,
        ).close()
    }

    @Test
    fun migrate8To9_addsLinksAndAutomation() {
        helper.createDatabase(EIGHTH_TEST_DATABASE, 8).close()
        helper.runMigrationsAndValidate(
            EIGHTH_TEST_DATABASE,
            9,
            true,
            MIGRATION_8_9,
        ).close()
    }

    @Test
    fun migrate9To10_addsWeekdayHabitReminders() {
        helper.createDatabase(NINTH_TEST_DATABASE, 9).close()
        helper.runMigrationsAndValidate(
            NINTH_TEST_DATABASE,
            10,
            true,
            MIGRATION_9_10,
        ).close()
    }

    @Test
    fun migrate10To11_addsPinningRoutineSourcesAndOrganization() {
        helper.createDatabase(TENTH_TEST_DATABASE, 10).close()
        helper.runMigrationsAndValidate(TENTH_TEST_DATABASE, 11, true, MIGRATION_10_11).close()
    }

    @Test
    fun migrate11To12_addsGoalWindowsAndConsistencyPeriods() {
        helper.createDatabase(ELEVENTH_TEST_DATABASE, 11).close()
        helper.runMigrationsAndValidate(ELEVENTH_TEST_DATABASE, 12, true, MIGRATION_11_12).use { database ->
            database.query("PRAGMA table_info(goals)").use { cursor ->
                val names = buildSet {
                    while (cursor.moveToNext()) add(cursor.getString(cursor.getColumnIndexOrThrow("name")))
                }
                assertEquals(
                    setOf("aggregationPeriod", "rollingDays", "consistencyPeriod", "consistencyRequiredPeriods"),
                    names.intersect(setOf("aggregationPeriod", "rollingDays", "consistencyPeriod", "consistencyRequiredPeriods")),
                )
            }
        }
    }

    @Test
    fun migrate12To13_addsSubtaskNotesAndNormalizesLegacyWeights() {
        helper.createDatabase(TWELFTH_TEST_DATABASE, 12).apply {
            execSQL("INSERT INTO tasks (id, title, notes, scheduleKind, recurrenceInterval, weekdaysMask, reminderEnabled, archived, createdAtMillis, updatedAtMillis, showSubtaskProgress, progressDisplay, autoCompleteFromSteps, repeatStepPolicy, pinned) VALUES (1, 'Parent', '', 'Anytime', 1, 0, 0, 0, 1, 1, 0, 'Percent', 1, 'Reset', 0)")
            execSQL("INSERT INTO task_steps (id, taskId, title, position, weight, archived, createdAtMillis, updatedAtMillis) VALUES (1, 1, 'Step', 0, 4.0, 0, 1, 1)")
            close()
        }
        helper.runMigrationsAndValidate(TWELFTH_TEST_DATABASE, 13, true, MIGRATION_12_13).use { database ->
            database.query("SELECT notes, weight FROM task_steps WHERE id = 1").use { cursor ->
                cursor.moveToFirst()
                assertEquals("", cursor.getString(0))
                assertEquals(1.0, cursor.getDouble(1), 0.0)
            }
        }
    }

    @Test
    fun migrate13To14_addsPlanningAndLocationFieldsAndPreservesLegacyReminder() {
        helper.createDatabase(THIRTEENTH_TEST_DATABASE, 13).apply {
            execSQL("INSERT INTO tasks (id, title, notes, scheduleKind, recurrenceInterval, weekdaysMask, reminderEnabled, archived, createdAtMillis, updatedAtMillis, showSubtaskProgress, progressDisplay, autoCompleteFromSteps, repeatStepPolicy, pinned) VALUES (1, 'Plan', '', 'Once', 1, 0, 1, 0, 1, 1, 0, 'Percent', 1, 'Reset', 0)")
            close()
        }
        helper.runMigrationsAndValidate(THIRTEENTH_TEST_DATABASE, 14, true, MIGRATION_13_14).use { database ->
            database.query("SELECT priority, area, tagsCsv, recurrenceAnchor, reminderOffsetsMinutesCsv, locationReminderEnabled FROM tasks WHERE id = 1").use { cursor ->
                cursor.moveToFirst()
                assertEquals("None", cursor.getString(0))
                assertEquals("", cursor.getString(1))
                assertEquals("", cursor.getString(2))
                assertEquals("Schedule", cursor.getString(3))
                assertEquals("0", cursor.getString(4))
                assertEquals(0, cursor.getInt(5))
            }
        }
    }

    @Test
    fun migrate14To15_addsMachineProfilesAndScopedLoadFields() {
        helper.createDatabase(FOURTEENTH_TEST_DATABASE, 14).close()
        helper.runMigrationsAndValidate(FOURTEENTH_TEST_DATABASE, 15, true, MIGRATION_14_15).use { database ->
            database.query("SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'gym_machines'").use { cursor ->
                assertEquals(1, cursor.count)
            }
            database.query("PRAGMA table_info(workout_exercises)").use { cursor ->
                val names = buildSet {
                    while (cursor.moveToNext()) add(cursor.getString(cursor.getColumnIndexOrThrow("name")))
                }
                assertEquals(
                    setOf("machineId", "machineNameSnapshot", "machineLoadTypeSnapshot", "machineUnitIdSnapshot", "machineLevelLabelSnapshot"),
                    names.intersect(setOf("machineId", "machineNameSnapshot", "machineLoadTypeSnapshot", "machineUnitIdSnapshot", "machineLevelLabelSnapshot")),
                )
            }
        }
    }

    @Test
    fun migrate15To16_addsImmutableLoadMeaningFields() {
        helper.createDatabase(FIFTEENTH_TEST_DATABASE, 15).close()
        helper.runMigrationsAndValidate(FIFTEENTH_TEST_DATABASE, 16, true, MIGRATION_15_16).use { database ->
            fun columns(table: String): Set<String> = database.query("PRAGMA table_info($table)").use { cursor ->
                buildSet {
                    while (cursor.moveToNext()) add(cursor.getString(cursor.getColumnIndexOrThrow("name")))
                }
            }
            assertEquals(true, "loadInterpretation" in columns("exercises"))
            assertEquals(setOf("loadInterpretation", "baseLoadKg"), columns("gym_machines").intersect(setOf("loadInterpretation", "baseLoadKg")))
            assertEquals(
                setOf("loadInterpretationSnapshot", "baseLoadKgSnapshot"),
                columns("workout_exercises").intersect(setOf("loadInterpretationSnapshot", "baseLoadKgSnapshot")),
            )
        }
    }

    @Test
    fun migrate16To17_addsMissedOccurrencePolicy() {
        helper.createDatabase(SIXTEENTH_TEST_DATABASE, 16).close()
        helper.runMigrationsAndValidate(SIXTEENTH_TEST_DATABASE, 17, true, MIGRATION_16_17).use { database ->
            database.query("PRAGMA table_info(tasks)").use { cursor ->
                val names = buildSet {
                    while (cursor.moveToNext()) add(cursor.getString(cursor.getColumnIndexOrThrow("name")))
                }
                assertEquals(true, "missedOccurrencePolicy" in names)
            }
        }
    }

    @Test
    fun migrate17To18_snapshotsExerciseAnalyticsPolicy() {
        helper.createDatabase(SEVENTEENTH_TEST_DATABASE, 17).close()
        helper.runMigrationsAndValidate(SEVENTEENTH_TEST_DATABASE, 18, true, MIGRATION_17_18).use { database ->
            database.query("PRAGMA table_info(workout_exercises)").use { cursor ->
                val names = buildSet {
                    while (cursor.moveToNext()) add(cursor.getString(cursor.getColumnIndexOrThrow("name")))
                }
                assertEquals(
                    setOf(
                        "trackingTypeSnapshot", "bodyweightLoadPolicySnapshot", "effectiveBodyweightPercentSnapshot",
                        "oneRepMaxFormulaSnapshot", "includeInVolumeSnapshot", "includeInPersonalRecordsSnapshot",
                    ),
                    names.intersect(
                        setOf(
                            "trackingTypeSnapshot", "bodyweightLoadPolicySnapshot", "effectiveBodyweightPercentSnapshot",
                            "oneRepMaxFormulaSnapshot", "includeInVolumeSnapshot", "includeInPersonalRecordsSnapshot",
                        ),
                    ),
                )
            }
        }
    }

    @Test
    fun migrate18To19_versionsMachineMeaningAndPreservesPlannedPrescription() {
        helper.createDatabase(EIGHTEENTH_TEST_DATABASE, 18).apply {
            execSQL(
                """
                INSERT INTO exercises (
                    id, uuid, name, trackingType, notes, equipment, primaryMuscles, secondaryMuscles,
                    weightUnitId, weightIncrement, repetitionIncrement, defaultRestSeconds, defaultGraphMetric,
                    oneRepMaxFormula, barWeightKg, availablePlatesKgCsv, includeInVolume,
                    includeInPersonalRecords, bodyweightLoadPolicy, effectiveBodyweightPercent,
                    showRpe, showRir, showTempo, favorite, position, archived, createdAtMillis,
                    updatedAtMillis, loadInterpretation
                ) VALUES (
                    1, 'exercise-1', 'Cable row', 'WeightReps', '', '', '', '',
                    'pound', 5, 1, 120, 'EstimatedOneRepMax', 'Epley', NULL, '', 1,
                    1, 'ExternalWeightOnly', 100, NULL, NULL, NULL, 0, 0, 0, 1, 1,
                    'PerSide'
                )
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO gym_machines (
                    id, uuid, exerciseId, name, location, details, loadType, unitId, levelLabel,
                    availableLoadsCsv, loadInterpretation, baseLoadKg, archived, createdAtMillis, updatedAtMillis
                ) VALUES (
                    3, 'machine-3', 1, 'Cable tower', 'Home', '', 'Mass', 'pound', 'level',
                    '10,20', 'PerSide', 0, 0, 1, 1
                )
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO workout_sessions (
                    id, uuid, name, notes, startedAtMillis, endedAtMillis, localEpochDay, zoneId,
                    state, keepScreenAwake, restTimerDeadlineMillis, restTimerDurationSeconds, archived,
                    createdAtMillis, updatedAtMillis, sourceRoutineId
                ) VALUES (4, 'session-4', '', '', 1, NULL, 1, 'UTC', 'Active', 0, NULL, NULL, 0, 1, 1, NULL)
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO workout_exercises (
                    id, uuid, sessionId, exerciseId, position, notes, groupId, createdAtMillis,
                    updatedAtMillis, machineId, machineNameSnapshot, machineLoadTypeSnapshot,
                    machineUnitIdSnapshot, machineLevelLabelSnapshot, loadInterpretationSnapshot,
                    baseLoadKgSnapshot, trackingTypeSnapshot, bodyweightLoadPolicySnapshot,
                    effectiveBodyweightPercentSnapshot, oneRepMaxFormulaSnapshot,
                    includeInVolumeSnapshot, includeInPersonalRecordsSnapshot
                ) VALUES (
                    5, 'placement-5', 4, 1, 0, '', NULL, 1, 1, 3, 'Cable tower', 'Mass',
                    'pound', 'level', 'PerSide', 0, 'WeightReps', 'ExternalWeightOnly', 100,
                    'Epley', 1, 1
                )
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO workout_sets (
                    id, uuid, workoutExerciseId, position, classification, planned, completed,
                    canonicalWeightKg, enteredWeight, enteredWeightUnitId, repetitions,
                    canonicalDistanceMetres, enteredDistance, enteredDistanceUnitId, durationSeconds,
                    bodyweightKg, note, rpe, rir, tempo, restSeconds, completedAtMillis,
                    deletedAtMillis, createdAtMillis, updatedAtMillis, machineLoadValue
                ) VALUES (
                    6, 'set-6', 5, 0, 'Working', 1, 0, 20, 45, 'pound', 8,
                    NULL, NULL, NULL, NULL, NULL, '', 8, 2, '', NULL, NULL, NULL, 1, 1, NULL
                )
                """.trimIndent(),
            )
            close()
        }

        helper.runMigrationsAndValidate(EIGHTEENTH_TEST_DATABASE, 19, true, MIGRATION_18_19).use { database ->
            database.query(
                "SELECT configurationGroupId, configurationVersion, pulleyRatio, stackMode " +
                    "FROM gym_machines WHERE id = 3",
            ).use { cursor ->
                cursor.moveToFirst()
                assertEquals("machine-3", cursor.getString(0))
                assertEquals(1, cursor.getInt(1))
                assertEquals(1.0, cursor.getDouble(2), 0.0)
                assertEquals("Single", cursor.getString(3))
            }
            database.query(
                "SELECT exerciseWeightUnitSnapshot, loadMultiplierSnapshot, " +
                    "machineConfigurationGroupSnapshot FROM workout_exercises WHERE id = 5",
            ).use { cursor ->
                cursor.moveToFirst()
                assertEquals("pound", cursor.getString(0))
                assertEquals(2.0, cursor.getDouble(1), 0.0)
                assertEquals("machine-3", cursor.getString(2))
            }
            database.query(
                "SELECT prescribedEnteredWeight, prescribedWeightUnitId, prescribedRepetitions, " +
                    "prescribedRpe, prescribedRir, unilateral FROM workout_sets WHERE id = 6",
            ).use { cursor ->
                cursor.moveToFirst()
                assertEquals(45.0, cursor.getDouble(0), 0.0)
                assertEquals("pound", cursor.getString(1))
                assertEquals(8, cursor.getInt(2))
                assertEquals(8.0, cursor.getDouble(3), 0.0)
                assertEquals(2.0, cursor.getDouble(4), 0.0)
                assertEquals(0, cursor.getInt(5))
            }
        }
    }

    @Test
    fun migrate19To20_addsInboxPlanningFieldsWithoutReclassifyingExistingTasks() {
        helper.createDatabase(NINETEENTH_TEST_DATABASE, 19).close()
        helper.runMigrationsAndValidate(NINETEENTH_TEST_DATABASE, 20, true, MIGRATION_19_20).use { database ->
            database.query("PRAGMA table_info(tasks)").use { cursor ->
                val names = buildSet {
                    while (cursor.moveToNext()) add(cursor.getString(cursor.getColumnIndexOrThrow("name")))
                }
                assertEquals(setOf("inbox", "durationMinutes", "effort"), names.intersect(setOf("inbox", "durationMinutes", "effort")))
            }
        }
    }

    @Test
    fun migrate20To21_addsOptionalHabitSourceWithoutReclassifyingExistingHabits() {
        helper.createDatabase(TWENTIETH_TEST_DATABASE, 20).close()
        helper.runMigrationsAndValidate(TWENTIETH_TEST_DATABASE, 21, true, MIGRATION_20_21).use { database ->
            database.query("PRAGMA table_info(habits)").use { cursor ->
                val names = buildSet {
                    while (cursor.moveToNext()) add(cursor.getString(cursor.getColumnIndexOrThrow("name")))
                }
                assertEquals(true, "sourceMetricId" in names)
            }
        }
    }

    @Test
    fun migrate21To22_backfillsStableMachineScopesAndRoutineSnapshots() {
        helper.createDatabase(TWENTY_FIRST_TEST_DATABASE, 21).apply {
            execSQL(
                """
                INSERT INTO exercises (
                    id, uuid, name, trackingType, notes, equipment, primaryMuscles, secondaryMuscles,
                    weightUnitId, weightIncrement, repetitionIncrement, defaultRestSeconds,
                    defaultGraphMetric, oneRepMaxFormula, barWeightKg, availablePlatesKgCsv,
                    includeInVolume, includeInPersonalRecords, bodyweightLoadPolicy,
                    effectiveBodyweightPercent, showRpe, showRir, showTempo, favorite, position,
                    archived, createdAtMillis, updatedAtMillis, loadInterpretation
                ) VALUES (1, 'exercise-1', 'Press', 'WeightReps', '', '', '', '', 'kilogram',
                    2.5, 1, 120, 'EstimatedOneRepMax', 'Epley', NULL, '', 1, 1,
                    'ExternalWeightOnly', 100, NULL, NULL, NULL, 0, 0, 0, 1, 1, 'Total')
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO gym_machines (
                    id, uuid, exerciseId, name, location, details, loadType, unitId, levelLabel,
                    availableLoadsCsv, archived, createdAtMillis, updatedAtMillis, loadInterpretation,
                    baseLoadKg, configurationGroupId, configurationVersion, seatPosition, backPosition,
                    attachment, pulleyRatio, stackMode, addOnPlateKg, stackLabelsCsv, massMappingCsv,
                    compatibleForComparison
                ) VALUES (7, 'machine-stable-uuid', 1, 'Cable press', 'Downtown', '', 'Mass',
                    'kilogram', 'level', '10,20', 0, 1, 5, 'Total', NULL, 'family-1', 2,
                    '3', '2', 'rope', 1, 'Single', NULL, '', '', 0)
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO workout_sessions (
                    id, uuid, name, notes, startedAtMillis, endedAtMillis, localEpochDay, zoneId,
                    state, keepScreenAwake, restTimerDeadlineMillis, restTimerDurationSeconds,
                    archived, createdAtMillis, updatedAtMillis, sourceRoutineId
                ) VALUES (2, 'session-2', 'Push', '', 1, 2, 20000, 'UTC', 'Finished', 0,
                    NULL, NULL, 0, 1, 2, NULL)
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO workout_exercises (
                    id, uuid, sessionId, exerciseId, position, notes, groupId, createdAtMillis,
                    updatedAtMillis, machineId, machineNameSnapshot, machineLoadTypeSnapshot,
                    machineUnitIdSnapshot, machineLevelLabelSnapshot, loadInterpretationSnapshot,
                    baseLoadKgSnapshot, trackingTypeSnapshot, bodyweightLoadPolicySnapshot,
                    effectiveBodyweightPercentSnapshot, oneRepMaxFormulaSnapshot,
                    includeInVolumeSnapshot, includeInPersonalRecordsSnapshot,
                    exerciseWeightUnitSnapshot, loadMultiplierSnapshot,
                    machineConfigurationGroupSnapshot, machineConfigurationVersionSnapshot,
                    machineConfigurationSnapshot, machinePulleyRatioSnapshot, machineStackModeSnapshot,
                    machineAddOnPlateKgSnapshot, machineMassMappingCsvSnapshot
                ) VALUES (3, 'placement-3', 2, 1, 0, '', NULL, 1, 2, 7,
                    'Cable press · Downtown', 'Mass', 'kilogram', 'level', 'Total', NULL,
                    'WeightReps', 'ExternalWeightOnly', 100, 'Epley', 1, 1, 'kilogram', 1,
                    'family-1', 2, 'Seat 3 · Back 2 · rope', 1, 'Single', NULL, '')
                """.trimIndent(),
            )
            execSQL("INSERT INTO gym_routines (id, uuid, name, notes, position, archived, pinned, createdAtMillis, updatedAtMillis) VALUES (4, 'routine-4', 'Plan', '', 0, 0, 0, 1, 1)")
            execSQL("INSERT INTO routine_days (id, uuid, routineId, name, position, createdAtMillis, updatedAtMillis) VALUES (5, 'day-5', 4, 'A', 0, 1, 1)")
            execSQL("INSERT INTO routine_exercises (id, uuid, routineDayId, exerciseId, position, notes, groupKey, copyPreviousWorkout, createdAtMillis, updatedAtMillis, machineId) VALUES (6, 'routine-placement-6', 5, 1, 0, '', NULL, 0, 1, 1, 7)")
            execSQL("INSERT INTO routine_exercises (id, uuid, routineDayId, exerciseId, position, notes, groupKey, copyPreviousWorkout, createdAtMillis, updatedAtMillis, machineId) VALUES (8, 'dangling-routine-placement', 5, 1, 1, '', NULL, 0, 1, 1, 999)")
            execSQL("INSERT INTO personal_records (uuid, exerciseId, type, value, secondaryValue, unitId, sourceSetId, sourceSessionId, achievedAtMillis, current, imported, createdAtMillis, updatedAtMillis, machineId) VALUES ('pr-1', 1, 'MaxWeight', 50, NULL, 'kilogram', NULL, 2, 2, 1, 0, 1, 1, 7)")
            close()
        }

        helper.runMigrationsAndValidate(
            TWENTY_FIRST_TEST_DATABASE,
            22,
            true,
            MIGRATION_21_22,
        ).use { database ->
            database.query("SELECT machineProfileUuidSnapshot FROM workout_exercises WHERE id = 3").use { cursor ->
                cursor.moveToFirst()
                assertEquals("machine-stable-uuid", cursor.getString(0))
            }
            database.query(
                "SELECT equipmentBindingState, machineProfileUuidSnapshot, machineNameSnapshot, " +
                    "machineConfigurationVersionSnapshot FROM routine_exercises WHERE id = 6",
            ).use { cursor ->
                cursor.moveToFirst()
                assertEquals("Resolved", cursor.getString(0))
                assertEquals("machine-stable-uuid", cursor.getString(1))
                assertEquals("Cable press · Downtown", cursor.getString(2))
                assertEquals(2, cursor.getInt(3))
            }
            database.query("SELECT machineProfileUuidSnapshot FROM personal_records WHERE uuid = 'pr-1'").use { cursor ->
                cursor.moveToFirst()
                assertEquals("machine-stable-uuid", cursor.getString(0))
            }
            database.query("SELECT equipmentBindingState, machineProfileUuidSnapshot FROM routine_exercises WHERE id = 8").use { cursor ->
                cursor.moveToFirst()
                assertEquals("NeedsEquipment", cursor.getString(0))
                assertEquals("legacy-machine-id:999", cursor.getString(1))
            }
            database.query("PRAGMA foreign_key_check").use { cursor -> assertEquals(0, cursor.count) }
        }
    }

    @Test
    fun migrate22To23_addsRoutineRepRanges() {
        helper.createDatabase(TWENTY_SECOND_TEST_DATABASE, 22).close()

        helper.runMigrationsAndValidate(
            TWENTY_SECOND_TEST_DATABASE,
            23,
            true,
            MIGRATION_22_23,
        ).use { database ->
            database.query("PRAGMA table_info(routine_sets)").use { cursor ->
                val nameIndex = cursor.getColumnIndexOrThrow("name")
                var found = false
                while (cursor.moveToNext()) if (cursor.getString(nameIndex) == "repetitionsMax") found = true
                assertTrue(found)
            }
            database.query("PRAGMA table_info(workout_sets)").use { cursor ->
                val nameIndex = cursor.getColumnIndexOrThrow("name")
                var found = false
                while (cursor.moveToNext()) if (cursor.getString(nameIndex) == "prescribedRepetitionsMax") found = true
                assertTrue(found)
            }
            database.query("PRAGMA foreign_key_check").use { cursor -> assertEquals(0, cursor.count) }
        }
    }

    @Test
    fun migrate23To24_addsCustomUnitLifecycleWithoutChangingConversions() {
        helper.createDatabase(TWENTY_THIRD_TEST_DATABASE, 23).apply {
            execSQL(
                "INSERT INTO unit_definitions (id,name,symbol,dimension,toCanonicalFactor,toCanonicalOffset,custom,createdAtMillis,updatedAtMillis) " +
                    "VALUES ('glass','glass','gl','Volume',250.0,0.0,1,1,1)",
            )
            close()
        }

        helper.runMigrationsAndValidate(
            TWENTY_THIRD_TEST_DATABASE,
            24,
            true,
            MIGRATION_23_24,
        ).use { database ->
            database.query("SELECT toCanonicalFactor, archived FROM unit_definitions WHERE id='glass'").use { cursor ->
                cursor.moveToFirst()
                assertEquals(250.0, cursor.getDouble(0), 0.0)
                assertEquals(0, cursor.getInt(1))
            }
        }
    }

    @Test
    fun migrate24To25_addsRoutineProgrammingSnapshots() {
        helper.createDatabase(TWENTY_FOURTH_TEST_DATABASE, 24).close()

        helper.runMigrationsAndValidate(
            TWENTY_FOURTH_TEST_DATABASE,
            25,
            true,
            MIGRATION_24_25,
        ).use { database ->
            fun columns(table: String): Set<String> = buildSet {
                database.query("PRAGMA table_info($table)").use { cursor ->
                    val nameIndex = cursor.getColumnIndexOrThrow("name")
                    while (cursor.moveToNext()) add(cursor.getString(nameIndex))
                }
            }
            assertTrue(columns("routine_exercises").containsAll(setOf("trainingMaxPercent", "progressionPercentagesCsv", "alternativeExerciseIdsCsv")))
            assertTrue(columns("routine_sets").containsAll(setOf("loadPrescriptionType", "loadPercentage")))
            assertTrue("alternativeExerciseIdsCsvSnapshot" in columns("workout_exercises"))
            assertTrue("prescriptionSourceLabel" in columns("workout_sets"))
            database.query("PRAGMA foreign_key_check").use { cursor -> assertEquals(0, cursor.count) }
        }
    }

    @Test
    fun migrate25To26_addsStableTaskAndStepIdentities() {
        helper.createDatabase(TWENTY_FIFTH_TEST_DATABASE, 25).apply {
            execSQL(
                "INSERT INTO tasks (id, title, notes, scheduleKind, recurrenceInterval, weekdaysMask, reminderEnabled, archived, createdAtMillis, updatedAtMillis, showSubtaskProgress, progressDisplay, autoCompleteFromSteps, repeatStepPolicy, pinned, priority, area, tagsCsv, recurrenceAnchor, reminderOffsetsMinutesCsv, locationReminderEnabled, locationName, locationRadiusMeters, locationTrigger, missedOccurrencePolicy, inbox, effort) " +
                    "VALUES (7, 'Task', '', 'Anytime', 1, 0, 0, 0, 1, 1, 0, 'Percent', 1, 'Reset', 0, 'None', '', '', 'Schedule', '', 0, '', 150, 'Arrive', 'KeepLatest', 0, 'Moderate')",
            )
            execSQL("INSERT INTO task_steps (id, taskId, title, position, notes, weight, archived, createdAtMillis, updatedAtMillis) VALUES (9, 7, 'Step', 0, '', 1, 0, 1, 1)")
            close()
        }

        helper.runMigrationsAndValidate(
            TWENTY_FIFTH_TEST_DATABASE,
            26,
            true,
            MIGRATION_25_26,
        ).use { database ->
            database.query("SELECT uuid FROM tasks WHERE id=7").use { cursor -> cursor.moveToFirst(); assertEquals("legacy-task-7", cursor.getString(0)) }
            database.query("SELECT manualPosition FROM tasks WHERE id=7").use { cursor -> cursor.moveToFirst(); assertEquals(7, cursor.getInt(0)) }
            database.query("SELECT uuid FROM task_steps WHERE id=9").use { cursor -> cursor.moveToFirst(); assertEquals("legacy-task-step-9", cursor.getString(0)) }
            database.query("PRAGMA foreign_key_check").use { cursor -> assertEquals(0, cursor.count) }
        }
    }

    @Test
    fun migrate26To27_normalizesAndBackfillsAreasForEveryProductivityDomain() {
        helper.createDatabase(AREA_TEST_DATABASE, 26).apply {
            execSQL(
                "INSERT INTO areas (id, name, colorArgb, position, archived, createdAtMillis, updatedAtMillis) " +
                    "VALUES ('work-primary', ' Work ', 123, 0, 0, 1, 1)",
            )
            execSQL(
                "INSERT INTO areas (id, name, colorArgb, position, archived, createdAtMillis, updatedAtMillis) " +
                    "VALUES ('work-duplicate', 'work', NULL, 1, 0, 1, 1)",
            )
            insertMinimal("tasks", mapOf("uuid" to "task-area", "title" to "Task", "area" to " work "))
            insertMinimal(
                "habits",
                mapOf("uuid" to "habit-area", "metricId" to "habit-metric", "name" to "Habit", "area" to "WORK"),
            )
            insertMinimal(
                "goals",
                mapOf("uuid" to "goal-area", "metricId" to "goal-metric", "name" to "Goal", "area" to "Work"),
            )
            close()
        }

        helper.runMigrationsAndValidate(AREA_TEST_DATABASE, 27, true, MIGRATION_26_27).use { database ->
            database.query("SELECT id, name, nameKey, colorArgb FROM areas WHERE nameKey = 'work'").use { cursor ->
                assertEquals(1, cursor.count)
                cursor.moveToFirst()
                assertEquals("work-primary", cursor.getString(0))
                assertEquals("Work", cursor.getString(1))
                assertEquals("work", cursor.getString(2))
                assertEquals(123L, cursor.getLong(3))
            }
            listOf("tasks", "habits", "goals").forEach { table ->
                database.query("SELECT areaId, area FROM $table").use { cursor ->
                    cursor.moveToFirst()
                    assertEquals("work-primary", cursor.getString(0))
                    assertEquals("Work", cursor.getString(1))
                }
                database.query("EXPLAIN QUERY PLAN SELECT * FROM $table WHERE areaId = 'work-primary'").use { cursor ->
                    cursor.moveToFirst()
                    assertTrue(cursor.getString(cursor.getColumnIndexOrThrow("detail")).contains("index_${table}_areaId"))
                }
            }
            database.query("PRAGMA foreign_key_check").use { cursor -> assertEquals(0, cursor.count) }
        }
    }

    private fun androidx.sqlite.db.SupportSQLiteDatabase.insertMinimal(
        table: String,
        overrides: Map<String, Any>,
    ) {
        val values = ContentValues()
        query("PRAGMA table_info($table)").use { cursor ->
            val nameIndex = cursor.getColumnIndexOrThrow("name")
            val typeIndex = cursor.getColumnIndexOrThrow("type")
            val notNullIndex = cursor.getColumnIndexOrThrow("notnull")
            val defaultIndex = cursor.getColumnIndexOrThrow("dflt_value")
            val primaryKeyIndex = cursor.getColumnIndexOrThrow("pk")
            while (cursor.moveToNext()) {
                val name = cursor.getString(nameIndex)
                val override = overrides[name]
                if (override != null) {
                    when (override) {
                        is String -> values.put(name, override)
                        is Int -> values.put(name, override)
                        is Long -> values.put(name, override)
                        is Double -> values.put(name, override)
                        else -> error("Unsupported test value for $table.$name")
                    }
                } else if (
                    cursor.getInt(notNullIndex) == 1 &&
                    cursor.isNull(defaultIndex) &&
                    cursor.getInt(primaryKeyIndex) == 0
                ) {
                    when (cursor.getString(typeIndex).uppercase()) {
                        "TEXT" -> values.put(name, "")
                        "REAL" -> values.put(name, 0.0)
                        else -> values.put(name, 0L)
                    }
                }
            }
        }
        check(insert(table, SQLiteDatabase.CONFLICT_ABORT, values) != -1L)
    }

    private companion object {
        const val TEST_DATABASE = "whip-migration-test"
        const val SECOND_TEST_DATABASE = "whip-migration-two-test"
        const val THIRD_TEST_DATABASE = "whip-migration-three-test"
        const val FOURTH_TEST_DATABASE = "whip-migration-four-test"
        const val FIFTH_TEST_DATABASE = "whip-migration-five-test"
        const val SIXTH_TEST_DATABASE = "whip-migration-six-test"
        const val SEVENTH_TEST_DATABASE = "whip-migration-seven-test"
        const val EIGHTH_TEST_DATABASE = "whip-migration-eight-test"
        const val NINTH_TEST_DATABASE = "whip-migration-nine-test"
        const val TENTH_TEST_DATABASE = "whip-migration-ten-test"
        const val ELEVENTH_TEST_DATABASE = "whip-migration-eleven-test"
        const val TWELFTH_TEST_DATABASE = "whip-migration-twelve-test"
        const val THIRTEENTH_TEST_DATABASE = "whip-migration-thirteen-test"
        const val FOURTEENTH_TEST_DATABASE = "whip-migration-fourteen-test"
        const val FIFTEENTH_TEST_DATABASE = "whip-migration-fifteen-test"
        const val SIXTEENTH_TEST_DATABASE = "whip-migration-sixteen-test"
        const val SEVENTEENTH_TEST_DATABASE = "whip-migration-seventeen-test"
        const val EIGHTEENTH_TEST_DATABASE = "whip-migration-eighteen-test"
        const val NINETEENTH_TEST_DATABASE = "whip-migration-nineteen-test"
        const val TWENTIETH_TEST_DATABASE = "whip-migration-twenty-test"
        const val TWENTY_FIRST_TEST_DATABASE = "whip-migration-twenty-one-test"
        const val TWENTY_SECOND_TEST_DATABASE = "whip-migration-twenty-two-test"
        const val TWENTY_THIRD_TEST_DATABASE = "whip-migration-twenty-three-test"
        const val TWENTY_FOURTH_TEST_DATABASE = "whip-migration-twenty-four-test"
        const val TWENTY_FIFTH_TEST_DATABASE = "whip-migration-twenty-five-test"
        const val AREA_TEST_DATABASE = "whip-area-migration-test"
    }
}

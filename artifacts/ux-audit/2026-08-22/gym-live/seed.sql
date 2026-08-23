PRAGMA foreign_keys = ON;
BEGIN;

INSERT INTO exercises (
    id, uuid, name, trackingType, notes, equipment, primaryMuscles, secondaryMuscles,
    weightUnitId, weightIncrement, repetitionIncrement, defaultRestSeconds,
    defaultGraphMetric, oneRepMaxFormula, barWeightKg, availablePlatesKgCsv,
    includeInVolume, includeInPersonalRecords, bodyweightLoadPolicy,
    effectiveBodyweightPercent, showRpe, showRir, showTempo, favorite, position,
    archived, createdAtMillis, updatedAtMillis, loadInterpretation
) VALUES
    (1, 'audit-exercise-bench', 'Barbell Bench Press', 'WeightReps', 'Keep shoulder blades set.', 'Barbell', 'Chest', 'Triceps', 'kilogram', 2.5, 1, 120, 'EstimatedOneRepMax', 'Epley', 20.0, '1.25,2.5,5.0,10.0,15.0,20.0,25.0', 1, 1, 'ExternalWeightOnly', 100.0, 1, 1, 0, 1, 0, 0, 1787356800000, 1787356800000, 'Total'),
    (2, 'audit-exercise-row', 'Cable Row', 'WeightReps', 'Pause at the torso.', 'Cable', 'Back', 'Biceps', 'kilogram', 5.0, 1, 90, 'EstimatedOneRepMax', 'Epley', NULL, '', 1, 1, 'ExternalWeightOnly', 100.0, 1, 0, 0, 0, 1, 0, 1787356800000, 1787356800000, 'MachineDisplayedMass'),
    (3, 'audit-exercise-plank', 'Plank', 'DurationOnly', 'Maintain a neutral spine.', 'Floor', 'Core', '', 'kilogram', 2.5, 1, 60, 'Duration', 'Epley', NULL, '', 0, 0, 'ExternalWeightOnly', 100.0, 0, 0, 0, 0, 2, 0, 1787356800000, 1787356800000, 'Total');

INSERT INTO gym_machines (
    id, uuid, exerciseId, name, location, details, loadType, unitId, levelLabel,
    availableLoadsCsv, archived, createdAtMillis, updatedAtMillis,
    loadInterpretation, baseLoadKg, configurationGroupId, configurationVersion,
    seatPosition, backPosition, attachment, pulleyRatio, stackMode, addOnPlateKg,
    stackLabelsCsv, massMappingCsv, compatibleForComparison
) VALUES (
    1, 'audit-machine-cable', 2, 'Dual Cable Station', 'Downtown Gym',
    'Use the narrow neutral handle.', 'Mass', 'kilogram', 'setting',
    '5,10,15,20,25,30,35,40,45,50,55,60,65,70,75,80', 0,
    1787356800000, 1787356800000, 'MachineDisplayedMass', NULL,
    'audit-cable-family', 1, '4', '2', 'Neutral handle', 1.0,
    'Single', NULL, '', '', 0
);

INSERT INTO workout_sessions (
    id, uuid, name, notes, startedAtMillis, endedAtMillis, localEpochDay, zoneId,
    state, keepScreenAwake, restTimerDeadlineMillis, restTimerDurationSeconds,
    archived, createdAtMillis, updatedAtMillis, sourceRoutineId
) VALUES
    (1, 'audit-session-active', 'Upper Strength', 'Working session for the live UX audit.', 1787418000000, NULL, 20687, 'America/Toronto', 'Active', 1, NULL, NULL, 0, 1787418000000, 1787418000000, NULL),
    (2, 'audit-session-history-1', 'Upper Strength', 'Solid session.', 1786813200000, 1786816800000, 20680, 'America/Toronto', 'Finished', 0, NULL, NULL, 0, 1786813200000, 1786816800000, NULL),
    (3, 'audit-session-history-2', 'Upper Volume', '', 1786208400000, 1786212600000, 20673, 'America/Toronto', 'Finished', 0, NULL, NULL, 0, 1786208400000, 1786212600000, NULL),
    (4, 'audit-session-history-3', 'Technique Day', 'Paused reps.', 1784998800000, 1785001800000, 20659, 'America/Toronto', 'Finished', 0, NULL, NULL, 0, 1784998800000, 1785001800000, NULL);

INSERT INTO workout_exercises (
    id, uuid, sessionId, exerciseId, position, notes, groupId, createdAtMillis,
    updatedAtMillis, machineProfileUuidSnapshot, machineId, machineNameSnapshot,
    machineLoadTypeSnapshot, machineUnitIdSnapshot, machineLevelLabelSnapshot,
    loadInterpretationSnapshot, baseLoadKgSnapshot, trackingTypeSnapshot,
    bodyweightLoadPolicySnapshot, effectiveBodyweightPercentSnapshot,
    oneRepMaxFormulaSnapshot, includeInVolumeSnapshot,
    includeInPersonalRecordsSnapshot, exerciseWeightUnitSnapshot,
    loadMultiplierSnapshot, machineConfigurationGroupSnapshot,
    machineConfigurationVersionSnapshot, machineConfigurationSnapshot,
    machinePulleyRatioSnapshot, machineStackModeSnapshot,
    machineAddOnPlateKgSnapshot, machineMassMappingCsvSnapshot,
    alternativeExerciseIdsCsvSnapshot
) VALUES
    (1, 'audit-we-active-bench', 1, 1, 0, '', NULL, 1787418000000, 1787418000000, NULL, NULL, '', '', '', '', 'Total', NULL, 'WeightReps', 'ExternalWeightOnly', 100.0, 'Epley', 1, 1, 'kilogram', 1.0, '', 1, '', 1.0, 'Single', NULL, '', ''),
    (2, 'audit-we-active-row', 1, 2, 1, 'Keep elbows close.', NULL, 1787418000000, 1787418000000, 'audit-machine-cable', 1, 'Dual Cable Station · Downtown Gym', 'Mass', 'kilogram', 'setting', 'MachineDisplayedMass', NULL, 'WeightReps', 'ExternalWeightOnly', 100.0, 'Epley', 1, 1, 'kilogram', 1.0, 'audit-cable-family', 1, 'Seat 4 · Back 2 · Neutral handle', 1.0, 'Single', NULL, '', ''),
    (3, 'audit-we-h1-bench', 2, 1, 0, '', NULL, 1786813200000, 1786816800000, NULL, NULL, '', '', '', '', 'Total', NULL, 'WeightReps', 'ExternalWeightOnly', 100.0, 'Epley', 1, 1, 'kilogram', 1.0, '', 1, '', 1.0, 'Single', NULL, '', ''),
    (4, 'audit-we-h1-row', 2, 2, 1, '', NULL, 1786813200000, 1786816800000, 'audit-machine-cable', 1, 'Dual Cable Station · Downtown Gym', 'Mass', 'kilogram', 'setting', 'MachineDisplayedMass', NULL, 'WeightReps', 'ExternalWeightOnly', 100.0, 'Epley', 1, 1, 'kilogram', 1.0, 'audit-cable-family', 1, 'Seat 4 · Back 2 · Neutral handle', 1.0, 'Single', NULL, '', ''),
    (5, 'audit-we-h2-bench', 3, 1, 0, '', NULL, 1786208400000, 1786212600000, NULL, NULL, '', '', '', '', 'Total', NULL, 'WeightReps', 'ExternalWeightOnly', 100.0, 'Epley', 1, 1, 'kilogram', 1.0, '', 1, '', 1.0, 'Single', NULL, '', ''),
    (6, 'audit-we-h3-bench', 4, 1, 0, '', NULL, 1784998800000, 1785001800000, NULL, NULL, '', '', '', '', 'Total', NULL, 'WeightReps', 'ExternalWeightOnly', 100.0, 'Epley', 1, 1, 'kilogram', 1.0, '', 1, '', 1.0, 'Single', NULL, '', '');

INSERT INTO workout_sets (
    id, uuid, workoutExerciseId, position, classification, planned, completed,
    canonicalWeightKg, enteredWeight, enteredWeightUnitId, repetitions,
    canonicalDistanceMetres, enteredDistance, enteredDistanceUnitId,
    durationSeconds, bodyweightKg, note, rpe, rir, tempo, restSeconds,
    completedAtMillis, deletedAtMillis, createdAtMillis, updatedAtMillis,
    machineLoadValue, unilateral, prescribedCanonicalWeightKg,
    prescribedEnteredWeight, prescribedWeightUnitId, prescribedRepetitions,
    prescribedRpe, prescribedRir, prescribedDurationSeconds,
    prescribedMachineLoadValue, prescribedRepetitionsMax,
    prescriptionSourceLabel
) VALUES
    (1, 'audit-set-a1', 1, 0, 'Working', 0, 1, 80.0, 80.0, 'kilogram', 8, NULL, NULL, NULL, NULL, NULL, '', 8.0, 2.0, '', 120, 1787418600000, NULL, 1787418000000, 1787418600000, NULL, 0, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, ''),
    (2, 'audit-set-a2', 1, 1, 'Working', 0, 1, 82.5, 82.5, 'kilogram', 6, NULL, NULL, NULL, NULL, NULL, '', 8.5, 1.5, '', 120, 1787419200000, NULL, 1787418000000, 1787419200000, NULL, 0, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, ''),
    (3, 'audit-set-a3', 1, 2, 'Working', 0, 0, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '', NULL, NULL, '', NULL, NULL, NULL, 1787418000000, 1787418000000, NULL, 0, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, ''),
    (4, 'audit-set-a4', 2, 0, 'Working', 0, 1, 50.0, 50.0, 'kilogram', 10, NULL, NULL, NULL, NULL, NULL, '', 8.0, NULL, '', 90, 1787419800000, NULL, 1787418000000, 1787419800000, 50.0, 0, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, ''),
    (5, 'audit-set-a5', 2, 1, 'Working', 0, 0, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '', NULL, NULL, '', NULL, NULL, NULL, 1787418000000, 1787418000000, NULL, 0, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, ''),
    (6, 'audit-set-h1a', 3, 0, 'Working', 0, 1, 75.0, 75.0, 'kilogram', 10, NULL, NULL, NULL, NULL, NULL, '', 8.0, 2.0, '', 120, 1786813800000, NULL, 1786813200000, 1786813800000, NULL, 0, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, ''),
    (7, 'audit-set-h1b', 3, 1, 'Working', 0, 1, 80.0, 80.0, 'kilogram', 8, NULL, NULL, NULL, NULL, NULL, '', 8.5, 1.5, '', 120, 1786814400000, NULL, 1786813200000, 1786814400000, NULL, 0, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, ''),
    (8, 'audit-set-h1c', 4, 0, 'Working', 0, 1, 45.0, 45.0, 'kilogram', 12, NULL, NULL, NULL, NULL, NULL, '', 8.0, NULL, '', 90, 1786815000000, NULL, 1786813200000, 1786815000000, 45.0, 0, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, ''),
    (9, 'audit-set-h2a', 5, 0, 'Working', 0, 1, 72.5, 72.5, 'kilogram', 12, NULL, NULL, NULL, NULL, NULL, '', 8.0, 2.0, '', 120, 1786209000000, NULL, 1786208400000, 1786209000000, NULL, 0, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, ''),
    (10, 'audit-set-h3a', 6, 0, 'Working', 0, 1, 70.0, 70.0, 'kilogram', 10, NULL, NULL, NULL, NULL, NULL, 'Two-second pause.', 7.5, 2.5, '2-1-1', 120, 1784999400000, NULL, 1784998800000, 1784999400000, NULL, 0, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '');

INSERT INTO gym_routines (
    id, uuid, name, notes, position, archived, pinned, createdAtMillis,
    updatedAtMillis
) VALUES (
    1, 'audit-routine-upper', 'Upper Strength', 'Primary upper-body template.', 0,
    0, 1, 1787356800000, 1787356800000
);

INSERT INTO routine_days (
    id, uuid, routineId, name, position, createdAtMillis, updatedAtMillis
) VALUES (
    1, 'audit-routine-day-a', 1, 'Day A', 0, 1787356800000, 1787356800000
);

INSERT INTO routine_exercises (
    id, uuid, routineDayId, exerciseId, position, notes, groupKey,
    copyPreviousWorkout, createdAtMillis, updatedAtMillis, machineId,
    equipmentBindingState, machineProfileUuidSnapshot, machineNameSnapshot,
    machineLoadTypeSnapshot, machineUnitIdSnapshot, machineLevelLabelSnapshot,
    machineLoadInterpretationSnapshot, machineConfigurationGroupSnapshot,
    machineConfigurationVersionSnapshot, machineConfigurationSnapshot,
    trainingMaxPercent, progressionPercentagesCsv, alternativeExerciseIdsCsv
) VALUES
    (1, 'audit-routine-ex-bench', 1, 1, 0, '', NULL, 1, 1787356800000, 1787356800000, NULL, 'None', NULL, '', '', '', '', 'Total', '', 1, '', 90.0, '', '2'),
    (2, 'audit-routine-ex-row', 1, 2, 1, '', NULL, 1, 1787356800000, 1787356800000, 1, 'Resolved', 'audit-machine-cable', 'Dual Cable Station · Downtown Gym', 'Mass', 'kilogram', 'setting', 'MachineDisplayedMass', 'audit-cable-family', 1, 'Seat 4 · Back 2 · Neutral handle', 90.0, '', '');

INSERT INTO routine_sets (
    id, uuid, routineExerciseId, position, classification, enteredWeight,
    enteredWeightUnitId, repetitions, enteredDistance, enteredDistanceUnitId,
    durationSeconds, bodyweightKg, note, rpe, rir, tempo, restSeconds,
    createdAtMillis, updatedAtMillis, machineLoadValue, unilateral,
    repetitionsMax, loadPrescriptionType, loadPercentage
) VALUES
    (1, 'audit-routine-set-b1', 1, 0, 'Working', 80.0, 'kilogram', 6, NULL, NULL, NULL, NULL, '', 8.0, 2.0, '', 120, 1787356800000, 1787356800000, NULL, 0, 8, 'Absolute', NULL),
    (2, 'audit-routine-set-b2', 1, 1, 'Working', 80.0, 'kilogram', 6, NULL, NULL, NULL, NULL, '', 8.0, 2.0, '', 120, 1787356800000, 1787356800000, NULL, 0, 8, 'Absolute', NULL),
    (3, 'audit-routine-set-r1', 2, 0, 'Working', 50.0, 'kilogram', 10, NULL, NULL, NULL, NULL, '', 8.0, NULL, '', 90, 1787356800000, 1787356800000, 50.0, 0, 12, 'Absolute', NULL);

COMMIT;

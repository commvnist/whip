package com.whip.app.data

import com.whip.app.domain.RoutineMainWorkScheme
import com.whip.app.domain.RoutineOptionalWorkKind
import com.whip.app.domain.RoutineProgramKind
import com.whip.app.domain.RoutineProgramTemplateKey
import com.whip.app.domain.RoutineProgressionMode
import com.whip.app.domain.RoutineSupplementalScheme
import com.whip.app.domain.WorkoutSetClassification

/**
 * Called inside the owner's existing Room transaction, at startup and after finishing a legacy
 * workout. Only future routine templates change; performed sessions and decisions are immutable.
 * Repeating this operation is safe, including after an older backup is restored.
 */
internal suspend fun WhipDatabase.retireLegacyFiveThreeOneRoutinesInTransaction(
    onlyRoutineId: Long? = null,
): Int {
    val routineDao = routineDao()
    val activeRoutineId = gymDao().getActiveSession()?.sourceRoutineId
    var retired = 0
    routineDao.getAllRoutines()
        .filter { it.programKind == RoutineProgramKind.FiveThreeOne.name }
        .filter { onlyRoutineId == null || it.id == onlyRoutineId }
        .filter { it.id != activeRoutineId }
        .forEach { routine ->
            routineDao.getDays(routine.id).forEach { day ->
                routineDao.getExercises(day.id).forEach { placement ->
                    routineDao.getSets(placement.id).forEach { set ->
                        routineDao.updateSet(
                            set.copy(
                                classification = if (set.classification == WorkoutSetClassification.TrainingMaxTest.name) {
                                    WorkoutSetClassification.Working.name
                                } else {
                                    set.classification
                                },
                                optionalWorkKind = RoutineOptionalWorkKind.None.name,
                                mainWorkScheme = "",
                                supplementalScheme = "",
                            ),
                        )
                    }
                    routineDao.updateExercise(
                        placement.copy(
                            mainWorkScheme = RoutineMainWorkScheme.Unspecified.name,
                            supplementalScheme = RoutineSupplementalScheme.None.name,
                            jokerSetsEnabled = false,
                        ),
                    )
                }
            }
            routineDao.updateRoutine(
                routine.copy(
                    programKind = RoutineProgramKind.Custom.name,
                    programTemplateKey = RoutineProgramTemplateKey.None.name,
                    programTemplateRevision = 0,
                    progressionMode = RoutineProgressionMode.Standard.name,
                    allowNonStandardHigherSuggestions = false,
                    trainingMaxAdvanceAfterPhaseIndicesCsv =
                        if (routine.progressionMode == RoutineProgressionMode.PerformanceInformed.name) {
                            "" // Review-only recommendations are retired; do not silently enable automatic increases.
                        } else {
                            routine.trainingMaxAdvanceAfterPhaseIndicesCsv
                        },
                ),
            )
            retired++
        }
    return retired
}

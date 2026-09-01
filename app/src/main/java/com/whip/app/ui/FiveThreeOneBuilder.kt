package com.whip.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.whip.app.domain.editableNumericValue
import com.whip.app.domain.RoutineMainWorkScheme
import com.whip.app.domain.RoutineProgramKind
import com.whip.app.domain.RoutineSupplementalScheme
import com.whip.app.domain.toWhipDoubleOrNull
import com.whip.app.domain.unitSymbol

@Composable
internal fun RoutineLabeledSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    testTag: String? = null,
    enabled: Boolean = true,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .then(if (testTag == null) Modifier else Modifier.testTag(testTag))
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            )
            .semantics(mergeDescendants = true) {
                contentDescription = label
                stateDescription = if (checked) "On" else "Off"
            }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            supportingText?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = null,
            modifier = Modifier.clearAndSetSemantics {},
        )
    }
}

@Composable
internal fun FiveThreeOneBuilder(
    placementKey: Long,
    exerciseName: String,
    currentSets: List<RoutineBuilderSetState>,
    unitId: String,
    increment: Double,
    availableLoads: List<Double>,
    suggestedTrainingMax: Double?,
    initialTrainingMax: Double? = null,
    initialCycleIncrement: Double? = null,
    initialProgramKind: RoutineProgramKind? = null,
    initialMainWorkScheme: RoutineMainWorkScheme = RoutineMainWorkScheme.Unspecified,
    initialSupplementalScheme: RoutineSupplementalScheme = RoutineSupplementalScheme.None,
    initialJokerSetsEnabled: Boolean = false,
    modifier: Modifier = Modifier,
    onApply: (FiveThreeOneBuilderResult) -> Unit,
) {
    // A recent-max suggestion is never accepted implicitly; it must be copied explicitly.
    val initialTrainingMaxText = initialTrainingMax?.let(::editableNumericValue).orEmpty()
    var trainingMaxText by rememberSaveable(placementKey) { mutableStateOf(initialTrainingMaxText) }
    val inferredMainScheme = when {
        initialMainWorkScheme == RoutineMainWorkScheme.FivesPro -> FiveThreeOneMainScheme.FivesPro
        initialMainWorkScheme != RoutineMainWorkScheme.Unspecified -> FiveThreeOneMainScheme.Classic
        initialProgramKind == RoutineProgramKind.FiveSPro -> FiveThreeOneMainScheme.FivesPro
        currentSets.any { it.classification == com.whip.app.domain.WorkoutSetClassification.Amrap.name } -> FiveThreeOneMainScheme.Classic
        currentSets.filter { it.routinePhaseIndex != null && it.classification != com.whip.app.domain.WorkoutSetClassification.BackOff.name }
            .takeIf(List<RoutineBuilderSetState>::isNotEmpty)
            ?.all { it.repetitionsMin == "5" } == true -> FiveThreeOneMainScheme.FivesPro
        else -> FiveThreeOneMainScheme.Classic
    }
    val inferredSupplement = when {
        initialSupplementalScheme == RoutineSupplementalScheme.BoringButBig -> FiveThreeOneSupplement.BoringButBig
        initialSupplementalScheme == RoutineSupplementalScheme.FirstSetLast -> FiveThreeOneSupplement.FirstSetLast
        initialSupplementalScheme == RoutineSupplementalScheme.SecondSetLast -> FiveThreeOneSupplement.SecondSetLast
        initialSupplementalScheme == RoutineSupplementalScheme.BoringButStrong -> FiveThreeOneSupplement.BoringButStrong
        initialProgramKind == RoutineProgramKind.BoringButBig || currentSets.any {
            it.routinePhaseIndex == null && it.classification == com.whip.app.domain.WorkoutSetClassification.BackOff.name && it.repetitionsMin == "10"
        } -> FiveThreeOneSupplement.BoringButBig
        initialProgramKind == RoutineProgramKind.FirstSetLast || currentSets.any {
            it.routinePhaseIndex != null && it.classification == com.whip.app.domain.WorkoutSetClassification.BackOff.name
        } -> FiveThreeOneSupplement.FirstSetLast
        else -> FiveThreeOneSupplement.None
    }
    val inferredBbbPercent = currentSets.firstOrNull {
        it.routinePhaseIndex == null && it.classification == com.whip.app.domain.WorkoutSetClassification.BackOff.name
    }?.loadPercentage?.takeIf(String::isNotBlank) ?: "50"
    var mainSchemeName by rememberSaveable(placementKey) { mutableStateOf(inferredMainScheme.name) }
    var phaseName by rememberSaveable(placementKey) { mutableStateOf(FiveThreeOnePhase.Fives.name) }
    var supplementName by rememberSaveable(placementKey) { mutableStateOf(inferredSupplement.name) }
    var finalSetAmrap by rememberSaveable(placementKey) {
        mutableStateOf(currentSets.none { it.routinePhaseIndex != null } || currentSets.any {
            it.classification == com.whip.app.domain.WorkoutSetClassification.Amrap.name
        })
    }
    var boringButBigPercentText by rememberSaveable(placementKey) { mutableStateOf(inferredBbbPercent) }
    var cycleIncrementText by rememberSaveable(placementKey) {
        mutableStateOf(
            editableNumericValue(
                initialCycleIncrement ?: defaultFiveThreeOneCycleIncrease(unitId, exerciseName),
            ),
        )
    }
    var jokerSetsEnabled by rememberSaveable(placementKey) { mutableStateOf(initialJokerSetsEnabled) }
    var confirmReplace by rememberSaveable(placementKey) { mutableStateOf(false) }

    val mainScheme = runCatching { FiveThreeOneMainScheme.valueOf(mainSchemeName) }
        .getOrDefault(FiveThreeOneMainScheme.Classic)
    val phase = runCatching { FiveThreeOnePhase.valueOf(phaseName) }
        .getOrDefault(FiveThreeOnePhase.Fives)
    val supplement = runCatching { FiveThreeOneSupplement.valueOf(supplementName) }
        .getOrDefault(FiveThreeOneSupplement.None)
    val trainingMax = trainingMaxText.toWhipDoubleOrNull()
    val boringButBigPercent = boringButBigPercentText.toWhipDoubleOrNull()
    val cycleIncrementValue = cycleIncrementText.toWhipDoubleOrNull()
    val inputError = when {
        trainingMax == null || !trainingMax.isFinite() || trainingMax <= 0.0 -> "Enter a training max above zero"
        cycleIncrementValue == null || !cycleIncrementValue.isFinite() || cycleIncrementValue <= 0.0 ->
            "Enter a cycle increase above zero"
        supplement == FiveThreeOneSupplement.BoringButBig &&
            (boringButBigPercent == null || boringButBigPercent !in 1.0..100.0) -> "BBB percentage must be from 1 to 100%"
        else -> null
    }
    val config = trainingMax?.takeIf { inputError == null }?.let {
        FiveThreeOneAuthoringConfig(
            trainingMax = it,
            mainScheme = mainScheme,
            phase = phase,
            supplement = supplement,
            classicFinalSetAmrap = finalSetAmrap,
            boringButBigPercent = boringButBigPercent ?: 50.0,
            jokerSetsEnabled = jokerSetsEnabled,
        )
    }
    val previews = config?.let { previewFiveThreeOneSets(it, increment, availableLoads) }.orEmpty()
    val fullCyclePreviews = config?.let { previewFiveThreeOneCycle(it, increment, availableLoads) }.orEmpty()
    val hasMeaningfulCurrentSets = currentSets.size > 1 || currentSets.any { set ->
        listOf(
            set.load,
            set.repetitionsMin,
            set.repetitionsMax,
            set.distance,
            set.durationSeconds,
            set.rpe,
            set.rir,
            set.restSeconds,
            set.tempo,
            set.note,
            set.loadPercentage,
        ).any(String::isNotBlank)
    }
    fun applyPreview() {
        val validConfig = config ?: return
        onApply(
            FiveThreeOneBuilderResult(
                sets = fiveThreeOneBuilderSets(
                    currentSets,
                    fullCyclePreviews,
                    mainWorkScheme = fiveThreeOneMainWorkScheme(validConfig),
                    supplementalScheme = fiveThreeOneSupplementalScheme(validConfig),
                ),
                trainingMax = validConfig.trainingMax,
                trainingMaxUnitId = unitId,
                cycleIncrementValue = cycleIncrementValue ?: return,
                programKind = fiveThreeOneProgramKind(validConfig),
                mainWorkScheme = fiveThreeOneMainWorkScheme(validConfig),
                supplementalScheme = fiveThreeOneSupplementalScheme(validConfig),
                jokerSetsEnabled = validConfig.jokerSetsEnabled,
            ),
        )
        confirmReplace = false
    }

    OutlinedCard(modifier.fillMaxWidth().testTag("five-three-one-builder")) {
        Column(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                "Guided 5/3/1 Programming",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                "Build all four phases for $exerciseName at once. Choose a week below to preview its practical rounded loads; percentages use this explicit training max and rise with each completed cycle.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            OutlinedTextField(
                value = trainingMaxText,
                onValueChange = { value ->
                    trainingMaxText = value.filter { it.isDigit() || it == '.' || it == ',' }.take(12)
                },
                label = { Text("Stable training max (${unitSymbol(unitId)})") },
                supportingText = {
                    Text(
                        suggestedTrainingMax?.let {
                            "Suggested 85% e1RM: ${editableNumericValue(it)} ${unitSymbol(unitId)} · suggestion only"
                        } ?: "Enter the training max you intend to use for this lift.",
                    )
                },
                isError = trainingMaxText.isNotBlank() && inputError == "Enter a training max above zero",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("five-three-one-training-max"),
            )
            suggestedTrainingMax?.let { suggestion ->
                WhipTextButton(
                    onClick = { trainingMaxText = editableNumericValue(suggestion) },
                    modifier = Modifier.testTag("five-three-one-use-suggestion"),
                ) { Text("Use ${editableNumericValue(suggestion)} ${unitSymbol(unitId)} Suggestion") }
            }
            OutlinedTextField(
                value = cycleIncrementText,
                onValueChange = { value ->
                    cycleIncrementText = value.filter { it.isDigit() || it == '.' || it == ',' }.take(12)
                },
                label = { Text("Training max increase each cycle (${unitSymbol(unitId)})") },
                supportingText = { Text("Applied after the final phase. You can use a different increase for each main lift.") },
                isError = cycleIncrementText.isNotBlank() && inputError == "Enter a cycle increase above zero",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("five-three-one-cycle-increment"),
            )

            Text("Main Work", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                FiveThreeOneMainScheme.entries.forEach { choice ->
                    WhipFilterChip(
                        selected = mainScheme == choice,
                        onClick = {
                            mainSchemeName = choice.name
                            finalSetAmrap = choice == FiveThreeOneMainScheme.Classic
                        },
                        label = { Text(choice.label) },
                        modifier = Modifier.testTag("five-three-one-main-${choice.name}"),
                    )
                }
            }

            Text("Phase", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                FiveThreeOnePhase.entries.forEach { choice ->
                    WhipFilterChip(
                        selected = phase == choice,
                        onClick = {
                            phaseName = choice.name
                        },
                        label = { Text(choice.label) },
                        modifier = Modifier.testTag("five-three-one-phase-${choice.name}"),
                    )
                }
            }

            if (mainScheme == FiveThreeOneMainScheme.Classic && phase != FiveThreeOnePhase.Deload) {
                RoutineLabeledSwitchRow(
                    label = "Final main set is a PR set",
                    checked = finalSetAmrap,
                    onCheckedChange = { finalSetAmrap = it },
                    supportingText = "The listed reps are the minimum. Continue for a rep record only while reps stay strong and technically sound.",
                    testTag = "five-three-one-amrap",
                )
            } else {
                Text(
                    if (mainScheme == FiveThreeOneMainScheme.FivesPro) "5s PRO uses five reps for all three main sets and has no AMRAP set."
                    else "Deload main sets have no AMRAP target.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Text("Supplemental Work", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                FiveThreeOneSupplement.entries.forEach { choice ->
                    WhipFilterChip(
                        selected = supplement == choice,
                        onClick = { supplementName = choice.name },
                        label = { Text(choice.label) },
                        modifier = Modifier.testTag("five-three-one-supplement-${choice.name}"),
                    )
                }
            }
            if (supplement == FiveThreeOneSupplement.BoringButBig) {
                OutlinedTextField(
                    value = boringButBigPercentText,
                    onValueChange = { value ->
                        boringButBigPercentText = value.filter { it.isDigit() || it == '.' || it == ',' }.take(6)
                    },
                    label = { Text("BBB percentage of training max") },
                    supportingText = { Text("Creates five back-off sets of 10 reps.") },
                    isError = inputError == "BBB percentage must be from 1 to 100%",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("five-three-one-bbb-percent"),
                )
            }
            RoutineLabeledSwitchRow(
                label = "Offer an optional Joker set",
                checked = jokerSetsEnabled,
                onCheckedChange = { jokerSetsEnabled = it },
                supportingText = if (mainScheme == FiveThreeOneMainScheme.FivesPro) {
                    "Added after Main work without replacing Supplemental work. Its reps follow the classic week's minimum (5/3/1), not 5s PRO, and it never blocks the workout."
                } else {
                    "Added after Main work at 5 percentage points above the top set without replacing BBB, FSL, SSL, BBS, or custom Supplemental work. It is optional and never blocks the workout."
                },
                testTag = "five-three-one-joker",
            )

            HorizontalDivider()
            Text("${phase.label} Rounded Preview", style = MaterialTheme.typography.labelLarge)
            Text(
                if (availableLoads.isNotEmpty()) "Loads snap to this machine's available settings."
                else "Loads round to the nearest ${editableNumericValue(increment)} ${unitSymbol(unitId)}.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (inputError != null) {
                Text(inputError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            } else {
                previews.forEachIndexed { index, preview ->
                    val setName = if (preview.plan.optionalWorkKind == com.whip.app.domain.RoutineOptionalWorkKind.Joker) "Optional Joker" else preview.plan.section.label
                    val label = "$setName ${index + 1}: " +
                        "${editableNumericValue(preview.plan.percentageOfTrainingMax)}% TM · " +
                        "${editableNumericValue(preview.roundedLoad)} ${unitSymbol(unitId)} × ${preview.plan.repetitionLabel}"
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .semantics { contentDescription = label },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(label, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            WhipButton(
                enabled = fullCyclePreviews.isNotEmpty(),
                onClick = {
                    if (hasMeaningfulCurrentSets) confirmReplace = true else applyPreview()
                },
                modifier = Modifier.fillMaxWidth().testTag("five-three-one-apply"),
            ) {
                Text("Apply Complete Four-Phase Program")
            }
            Text(
                "Saves ${fullCyclePreviews.size} prescriptions across 5s, 3s, 5/3/1, and deload phases. Applying replaces this exercise's current planned sets; other exercises and days are unchanged.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (confirmReplace) {
        PaneAwareAlertDialog(
            onDismissRequest = { confirmReplace = false },
            title = { Text("Replace Planned Sets for $exerciseName?") },
            text = {
                Text("This replaces ${currentSets.size} current planned set${if (currentSets.size == 1) "" else "s"} with ${fullCyclePreviews.size} prescriptions for the complete four-phase cycle. You can continue editing every generated set afterward.")
            },
            confirmButton = {
                WhipTextButton(onClick = ::applyPreview) { Text("Replace Sets") }
            },
            dismissButton = {
                WhipTextButton(onClick = { confirmReplace = false }) { Text("Cancel") }
            },
        )
    }
}

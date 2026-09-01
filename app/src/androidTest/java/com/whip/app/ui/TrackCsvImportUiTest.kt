package com.whip.app.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertContentDescriptionContains
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.whip.app.domain.Track
import com.whip.app.domain.TrackCsvImportPreparation
import com.whip.app.domain.TrackCsvImportPreview
import com.whip.app.domain.TrackCsvImportReceipt
import com.whip.app.domain.TrackCsvImportRequest
import com.whip.app.domain.TrackCsvMapping
import com.whip.app.domain.TrackEntryDraft
import com.whip.app.domain.TrackEntryFieldContract
import com.whip.app.domain.TrackEntryFormBoundary
import com.whip.app.domain.TrackEntryFormSnapshot
import com.whip.app.domain.TrackField
import com.whip.app.domain.TrackFieldType
import com.whip.app.domain.TrackProjection
import com.whip.app.ui.theme.WhipTheme
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TrackCsvImportUiTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun importingShieldsTheWholeDialogAndBlocksBackOrDuplicateSubmission() {
        var saving by mutableStateOf(false)
        var submissions = 0
        var dismissals = 0
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                TrackCsvImportDialog(
                    projection = projection(),
                    state = readyState(),
                    saving = saving,
                    persistenceError = null,
                    onMappingChange = {},
                    onRetry = {},
                    onChooseAnother = {},
                    onDismiss = { dismissals++ },
                    onImport = {
                        submissions++
                        saving = true
                    },
                )
            }
        }

        compose.onNodeWithTag("track-csv-import-confirm").performClick()
        compose.onNodeWithTag("persistence-saving-overlay").assertIsDisplayed()
        compose.onAllNodesWithTag("track-csv-import-confirm").assertCountEquals(0)
        pressBack()
        compose.onNodeWithTag("persistence-saving-overlay").assertIsDisplayed()
        compose.runOnIdle {
            assertEquals(1, submissions)
            assertEquals(0, dismissals)
        }
    }

    @Test
    fun missingTrackStillShowsTruthfulCancellableImportState() {
        var dismissals = 0
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                TrackCsvImportDialog(
                    projection = null,
                    state = readyState().copy(
                        phase = TrackCsvImportPhase.Error,
                        preview = null,
                        preparation = null,
                        errorMessage = "Runs is no longer available. No Entries were imported.",
                        requiresNewFile = true,
                    ),
                    saving = false,
                    persistenceError = null,
                    onMappingChange = {},
                    onRetry = {},
                    onChooseAnother = {},
                    onDismiss = { dismissals++ },
                    onImport = {},
                )
            }
        }

        compose.onNodeWithTag("track-csv-import-dialog").assertIsDisplayed()
        compose.onNodeWithTag("track-csv-import-problem").assertIsDisplayed()
        compose.onNodeWithText("Runs is no longer available. No Entries were imported.").assertIsDisplayed()
        compose.onNodeWithText("Cancel").performClick()
        compose.runOnIdle { assertEquals(1, dismissals) }
    }

    @Test
    fun largeTextKeepsFileDateMappingAndRecoveryActionReachable() {
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                val density = LocalDensity.current
                CompositionLocalProvider(
                    LocalDensity provides Density(density.density, fontScale = 2f),
                    LocalWhipDialogPlacement provides WhipDialogPlacement(maxWidth = 320.dp),
                ) {
                    TrackCsvImportDialog(
                        projection = projection(),
                        state = readyState().copy(
                            phase = TrackCsvImportPhase.Ready,
                            preview = TrackCsvImportPreview(
                                headers = listOf("Notes"),
                                totalRows = 1,
                                validDrafts = emptyList(),
                                issues = emptyList(),
                            ),
                            preparation = null,
                            errorMessage = "The selected file is empty.",
                        ),
                        saving = false,
                        persistenceError = null,
                        onMappingChange = {},
                        onRetry = {},
                        onChooseAnother = {},
                        onDismiss = {},
                        onImport = {},
                    )
                }
            }
        }

        compose.onNodeWithText("runs.csv").assertIsDisplayed()
        compose.onNodeWithText("1 Field mapped").assertIsDisplayed()
        compose.onNodeWithTag("track-csv-replace-file").assertIsDisplayed()
        compose.onNodeWithTag("track-csv-import-confirm").assertIsNotEnabled()
    }

    @Test
    fun completedRecoverySuppressesAnOrphanedCoordinatorError() {
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                TrackCsvImportDialog(
                    projection = projection(),
                    state = readyState().copy(
                        phase = TrackCsvImportPhase.Complete,
                        completionReceipt = receipt(alreadyApplied = true),
                        commitAttempted = true,
                    ),
                    saving = false,
                    persistenceError = "The previous import was interrupted. Whip is checking its saved result before any retry.",
                    onMappingChange = {},
                    onRetry = {},
                    onChooseAnother = {},
                    onDismiss = {},
                    onImport = {},
                )
            }
        }

        compose.onNodeWithTag("track-csv-import-complete").assertIsDisplayed()
        compose.onAllNodesWithTag("track-csv-import-commit-problem").assertCountEquals(0)
        compose.onAllNodesWithText("Import Didn’t Finish").assertCountEquals(0)
    }

    @Test
    fun durableNoReceiptRecoveryNoticeSuppressesAnOrphanedCoordinatorError() {
        val recovery = "No completed import was found. Review this preview, then import again when you are ready."
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                TrackCsvImportDialog(
                    projection = projection(),
                    state = readyState().copy(recoveryNotice = recovery, commitAttempted = true),
                    saving = false,
                    persistenceError = "The previous import was interrupted. Whip is checking its saved result before any retry.",
                    onMappingChange = {},
                    onRetry = {},
                    onChooseAnother = {},
                    onDismiss = {},
                    onImport = {},
                )
            }
        }

        compose.onNodeWithText(recovery).assertIsDisplayed()
        compose.onAllNodesWithTag("track-csv-import-commit-problem").assertCountEquals(0)
    }

    @Test
    fun domainConflictSuppressesUnsafeExactRetryAdvice() {
        val domainError = "The Track form changed after this CSV preview was prepared. Choose Replace File to build a new preview."
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                TrackCsvImportDialog(
                    projection = projection(),
                    state = readyState().copy(
                        phase = TrackCsvImportPhase.Error,
                        requiresNewFile = true,
                        errorMessage = domainError,
                        commitAttempted = true,
                    ),
                    saving = false,
                    persistenceError = "The Track form changed after this CSV preview was prepared.",
                    onMappingChange = {},
                    onRetry = {},
                    onChooseAnother = {},
                    onDismiss = {},
                    onImport = {},
                )
            }
        }

        compose.onNodeWithText(domainError).assertIsDisplayed()
        compose.onNodeWithTag("track-csv-replace-file").assertIsDisplayed()
        compose.onAllNodesWithTag("track-csv-import-commit-problem").assertCountEquals(0)
        compose.onAllNodesWithText("The reviewed import is still here. Retry without changing its file or mapping.")
            .assertCountEquals(0)
    }

    @Test
    fun ordinaryReadyPersistenceFailureKeepsExactRetryAvailable() {
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                TrackCsvImportDialog(
                    projection = projection(),
                    state = readyState().copy(commitAttempted = true),
                    saving = false,
                    persistenceError = "The database was temporarily unavailable.",
                    onMappingChange = {},
                    onRetry = {},
                    onChooseAnother = {},
                    onDismiss = {},
                    onImport = {},
                )
            }
        }

        compose.onNodeWithTag("track-csv-import-commit-problem").assertIsDisplayed()
        compose.onNodeWithTag("track-csv-import-commit-problem")
            .assertContentDescriptionContains(
                "The reviewed import is still here",
                substring = true,
            )
        compose.onNodeWithTag("track-csv-import-confirm").assertIsEnabled()
    }

    @Test
    fun completedReceiptRemainsAuthoritativeWhenTrackLookupSettlesMissing() {
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                TrackCsvImportDialog(
                    projection = null,
                    targetLookupComplete = true,
                    targetLookupError = "Stale projection error",
                    state = readyState().copy(
                        phase = TrackCsvImportPhase.Complete,
                        completionReceipt = receipt(alreadyApplied = true),
                        errorMessage = "Stale target error",
                    ),
                    saving = false,
                    persistenceError = "Stale coordinator error",
                    onMappingChange = {},
                    onRetry = {},
                    onChooseAnother = {},
                    onDismiss = {},
                    onImport = {},
                )
            }
        }

        compose.onNodeWithTag("track-csv-import-complete").assertIsDisplayed()
        compose.onAllNodesWithTag("track-csv-import-problem").assertCountEquals(0)
        compose.onAllNodesWithTag("track-csv-import-commit-problem").assertCountEquals(0)
        compose.onAllNodesWithTag("track-csv-target-loading").assertCountEquals(0)
    }

    @Test
    fun completedReceiptRemainsAuthoritativeWhenCurrentTrackIsArchived() {
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                TrackCsvImportDialog(
                    projection = archivedProjection(),
                    state = readyState().copy(
                        phase = TrackCsvImportPhase.Complete,
                        completionReceipt = receipt(alreadyApplied = true),
                        errorMessage = "Restore this Track before importing Entries.",
                    ),
                    saving = false,
                    persistenceError = "Stale coordinator error",
                    onMappingChange = {},
                    onRetry = {},
                    onChooseAnother = {},
                    onDismiss = {},
                    onImport = {},
                )
            }
        }

        compose.onNodeWithTag("track-csv-import-complete").assertIsDisplayed()
        compose.onAllNodesWithTag("track-csv-import-problem").assertCountEquals(0)
        compose.onAllNodesWithTag("track-csv-import-commit-problem").assertCountEquals(0)
    }

    @Test
    fun unresolvedTrackLookupShowsNeutralLoadingInsteadOfInventingMissingTarget() {
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                TrackCsvImportDialog(
                    projection = null,
                    targetLookupComplete = false,
                    state = readyState(),
                    saving = false,
                    persistenceError = null,
                    onMappingChange = {},
                    onRetry = {},
                    onChooseAnother = {},
                    onDismiss = {},
                    onImport = {},
                )
            }
        }

        compose.onNodeWithTag("track-csv-target-loading").assertIsDisplayed()
        compose.onAllNodesWithTag("track-csv-import-problem").assertCountEquals(0)
        compose.onAllNodesWithText("Runs is no longer available. No Entries were imported.").assertCountEquals(0)
        compose.onNodeWithTag("track-csv-import-confirm").assertIsNotEnabled()
    }

    @Test
    fun failedTrackLookupKeepsReviewedImportAndOffersARealRetry() {
        var retries = 0
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                TrackCsvImportDialog(
                    projection = null,
                    targetLookupComplete = true,
                    targetLookupError = "Track storage is temporarily unavailable",
                    state = readyState(),
                    saving = false,
                    persistenceError = "Stale coordinator advice",
                    onMappingChange = {},
                    onRetry = { retries++ },
                    onChooseAnother = {},
                    onDismiss = {},
                    onImport = {},
                )
            }
        }

        compose.onNodeWithTag("track-csv-import-problem").assertIsDisplayed()
        compose.onNodeWithText("Whip couldn’t check whether Runs is still available", substring = true)
            .assertIsDisplayed()
        compose.onNodeWithText("Your reviewed import is still here", substring = true)
            .assertIsDisplayed()
        compose.onAllNodesWithTag("track-csv-replace-file").assertCountEquals(0)
        compose.onAllNodesWithTag("track-csv-import-commit-problem").assertCountEquals(0)
        compose.onAllNodesWithTag("track-csv-import-confirm").assertCountEquals(0)
        compose.onNodeWithText("Try Again").performClick()
        compose.runOnIdle { assertEquals(1, retries) }
    }

    @Test
    fun archivedTargetExplainsTheAvailableCancelRestoreAndReopenPath() {
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                TrackCsvImportDialog(
                    projection = archivedProjection(),
                    state = readyState().copy(
                        phase = TrackCsvImportPhase.Error,
                        requiresNewFile = true,
                        errorMessage = "Choose Replace File to continue.",
                    ),
                    saving = false,
                    persistenceError = "Choose Replace File to continue.",
                    onMappingChange = {},
                    onRetry = {},
                    onChooseAnother = {},
                    onDismiss = {},
                    onImport = {},
                )
            }
        }

        compose.onNodeWithText("Runs is archived. Cancel this import, restore the Track, then reopen CSV Import.")
            .assertIsDisplayed()
        compose.onAllNodesWithTag("track-csv-replace-file").assertCountEquals(0)
        compose.onNodeWithText("Cancel").assertIsDisplayed()
    }

    @Test
    fun constrainedMultiFieldErrorIsVisibleBeforeFrozenMappingControls() {
        val form = multiFieldForm()
        val headers = form.fields.map(TrackField::name)
        val mapping = TrackCsvMapping(fieldColumns = form.fields.associate { it.uuid to it.name })
        val domainError = "This frozen import preview needs attention before anything can be written."
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                val density = LocalDensity.current
                CompositionLocalProvider(
                    LocalDensity provides Density(density.density, fontScale = 2f),
                    LocalWhipDialogPlacement provides WhipDialogPlacement(maxWidth = 320.dp),
                ) {
                    TrackCsvImportDialog(
                        projection = projection(),
                        state = readyState().copy(
                            phase = TrackCsvImportPhase.Error,
                            headers = headers,
                            mapping = mapping,
                            openingForm = form,
                            preparation = null,
                            preview = null,
                            errorMessage = domainError,
                            requiresNewFile = true,
                        ),
                        saving = false,
                        persistenceError = "Unsafe generic retry advice",
                        onMappingChange = {},
                        onRetry = {},
                        onChooseAnother = {},
                        onDismiss = {},
                        onImport = {},
                    )
                }
            }
        }

        compose.onNodeWithText(domainError).assertIsDisplayed()
        compose.onNodeWithTag("track-csv-replace-file").assertIsDisplayed()
        compose.onNodeWithText("Review Field Mapping (5 mapped)").performScrollTo().performClick()
        compose.onNodeWithTag("track-csv-import-content").performScrollToNode(hasText("Frozen Field 5"))
        compose.onAllNodesWithText("Frozen Field 5").assertCountEquals(2)
    }

    private fun readyState(): TrackCsvImportUiState {
        val mapping = TrackCsvMapping(fieldColumns = mapOf("notes" to "Notes"))
        val preparation = preparation(mapping)
        return TrackCsvImportUiState(
            trackId = TRACK_ID,
            trackName = "Runs",
            batchUuid = BATCH_UUID,
            fileLabel = "runs.csv",
            fallbackDate = DATE,
            dataGeneration = 7,
            previewRevision = 2,
            phase = TrackCsvImportPhase.Ready,
            headers = listOf("Notes"),
            mapping = mapping,
            openingForm = preparation.form,
            preview = TrackCsvImportPreview(
                headers = listOf("Notes"),
                totalRows = 1,
                validDrafts = listOf(TrackEntryDraft(DATE, emptyMap())),
                issues = emptyList(),
            ),
            preparation = preparation,
        )
    }

    private fun preparation(mapping: TrackCsvMapping): TrackCsvImportPreparation {
        val projection = projection()
        val field = projection.fields.single()
        val boundary = TrackEntryFormBoundary(
            trackId = TRACK_ID,
            trackUuid = "track-7",
            trackCreatedAtMillis = 1,
            writable = true,
            semanticRevisionToken = "form-7",
            fieldContracts = listOf(
                TrackEntryFieldContract(
                    id = field.id,
                    uuid = field.uuid,
                    trackId = TRACK_ID,
                    name = field.name,
                    type = field.type,
                    required = field.required,
                    primary = field.primary,
                    dimension = null,
                    unitId = null,
                    precision = 0,
                    scaleMin = null,
                    scaleMax = null,
                    scaleLowLabel = "",
                    scaleHighLabel = "",
                    scaleStep = 1.0,
                ),
            ),
        )
        return TrackCsvImportPreparation(
            request = TrackCsvImportRequest(
                batchUuid = BATCH_UUID,
                openingFormBoundary = boundary,
                payloadFingerprint = "a".repeat(64),
                mapping = mapping,
                defaultEntryDate = DATE,
                requestFingerprint = "b".repeat(64),
                entryUuids = listOf("22222222-2222-4222-8222-222222222222"),
                entryIdentityDigest = "c".repeat(64),
                rowCount = 1,
            ),
            form = TrackEntryFormSnapshot(
                boundary = boundary,
                track = projection.track,
                fields = projection.fields,
                options = emptyList(),
                units = emptyList(),
            ),
        )
    }

    private fun receipt(alreadyApplied: Boolean) = TrackCsvImportReceipt(
        batchUuid = BATCH_UUID,
        trackId = TRACK_ID,
        trackUuid = "track-7",
        trackCreatedAtMillis = 1L,
        requestFingerprint = preparation(TrackCsvMapping(fieldColumns = mapOf("notes" to "Notes")))
            .request.requestFingerprint,
        entryIdentityDigest = "c".repeat(64),
        rowCount = 1,
        fingerprintVersion = 1,
        identityVersion = 1,
        committedAtMillis = 2L,
        changed = !alreadyApplied,
        alreadyApplied = alreadyApplied,
    )

    private fun projection(): TrackProjection {
        val track = Track(
            id = TRACK_ID,
            uuid = "track-7",
            name = "Runs",
            description = "",
            icon = "🏃",
            areaId = "fitness",
            area = "Fitness",
            tags = emptyList(),
            pinned = false,
            archived = false,
            position = 0,
            createdAtMillis = 1,
            updatedAtMillis = 1,
        )
        val field = TrackField(
            id = 1,
            uuid = "notes",
            trackId = TRACK_ID,
            name = "Notes",
            type = TrackFieldType.ShortText,
            position = 0,
            required = true,
            primary = true,
            showInList = true,
            dimension = null,
            unitId = null,
            precision = 0,
            scaleMin = null,
            scaleMax = null,
            scaleLowLabel = "",
            scaleHighLabel = "",
            createdAtMillis = 1,
            updatedAtMillis = 1,
        )
        return TrackProjection(track, listOf(field), emptyList(), emptyList())
    }

    private fun archivedProjection(): TrackProjection {
        val projection = projection()
        return projection.copy(track = projection.track.copy(archived = true))
    }

    private fun multiFieldForm(): TrackEntryFormSnapshot {
        val base = preparation(TrackCsvMapping(fieldColumns = mapOf("notes" to "Notes"))).form
        val fields = (1L..5L).map { ordinal ->
            base.fields.single().copy(
                id = ordinal,
                uuid = "frozen-field-$ordinal",
                name = "Frozen Field $ordinal",
                position = ordinal.toInt() - 1,
                primary = ordinal == 1L,
            )
        }
        val contracts = fields.map { field ->
            TrackEntryFieldContract(
                id = field.id,
                uuid = field.uuid,
                trackId = field.trackId,
                name = field.name,
                type = field.type,
                required = field.required,
                primary = field.primary,
                dimension = field.dimension,
                unitId = field.unitId,
                precision = field.precision,
                scaleMin = field.scaleMin,
                scaleMax = field.scaleMax,
                scaleLowLabel = field.scaleLowLabel,
                scaleHighLabel = field.scaleHighLabel,
                scaleStep = field.scaleStep,
            )
        }
        return base.copy(boundary = base.boundary.copy(fieldContracts = contracts), fields = fields)
    }

    private companion object {
        const val TRACK_ID = 7L
        const val BATCH_UUID = "11111111-1111-4111-8111-111111111111"
        val DATE: LocalDate = LocalDate.of(2026, 9, 1)
    }
}

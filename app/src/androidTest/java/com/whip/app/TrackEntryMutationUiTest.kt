package com.whip.app

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.dp
import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.whip.app.domain.Track
import com.whip.app.domain.TrackEntryBoundary
import com.whip.app.domain.TrackEntryDraft
import com.whip.app.domain.TrackEntryEditSnapshot
import com.whip.app.domain.TrackEntryFieldContract
import com.whip.app.domain.TrackEntryFormBoundary
import com.whip.app.domain.TrackEntryFormSnapshot
import com.whip.app.domain.TrackField
import com.whip.app.domain.TrackFieldType
import com.whip.app.domain.TrackValueDraft
import com.whip.app.ui.TrackEntryEditor
import com.whip.app.ui.theme.WhipTheme
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TrackEntryMutationUiTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun savingEntryShieldsInputAndFailureKeepsTheUsersDraft() {
        var saving by mutableStateOf(false)
        var persistenceError by mutableStateOf<String?>(null)
        var dismissed = 0
        val form = entryForm()
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                TrackEntryEditor(
                    form = form,
                    editSnapshot = null,
                    today = LocalDate.of(2026, 9, 1),
                    saving = saving,
                    persistenceError = persistenceError,
                    modifier = Modifier.width(320.dp),
                    sessionId = 51,
                    onDismiss = { dismissed++ },
                    onSave = {},
                )
            }
        }

        compose.onNodeWithTag("track-entry-short-text-title")
            .performTextReplacement("Unsaved exact draft")
        compose.runOnIdle { saving = true }
        compose.onNodeWithContentDescription("Saving Entry", substring = true).assertIsDisplayed()
        pressBack()
        compose.runOnIdle { assertEquals(0, dismissed) }

        compose.runOnIdle {
            saving = false
            persistenceError = "The Entry could not be saved."
        }
        compose.onNodeWithTag("track-entry-persistence-problem").assertIsDisplayed()
        compose.onNodeWithTag("track-entry-short-text-title")
            .assertIsDisplayed()
        compose.onNodeWithText("Unsaved exact draft").assertIsDisplayed()
    }

    @Test
    fun deletingEntryRequiresExactReviewAndFailureKeepsTheConfirmationOpen() {
        val form = entryForm()
        val snapshot = entryEditSnapshot(form)
        var persistenceError by mutableStateOf<String?>(null)
        var deleteRequests = 0
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                TrackEntryEditor(
                    form = form,
                    editSnapshot = snapshot,
                    today = LocalDate.of(2026, 9, 1),
                    saving = false,
                    persistenceError = persistenceError,
                    modifier = Modifier.width(320.dp),
                    sessionId = 52,
                    onDismiss = {},
                    onSave = {},
                    onDelete = {
                        deleteRequests++
                        persistenceError = "The Entry could not be deleted."
                    },
                )
            }
        }

        compose.onNodeWithTag("track-entry-editor-list")
            .performScrollToNode(hasText("Delete Entry"))
        compose.onNodeWithText("Delete Entry").performClick()
        compose.onNodeWithTag("track-entry-delete-confirmation").assertIsDisplayed()
        compose.onNodeWithText("Delete The Dispossessed?").assertIsDisplayed()
        compose.onNodeWithText(
            "This removes the Entry dated Sep 1, 2026 and 1 saved value.",
        ).assertIsDisplayed()

        compose.onNodeWithTag("track-entry-delete-confirm").performClick()
        compose.runOnIdle { assertEquals(1, deleteRequests) }
        compose.onNodeWithTag("track-entry-delete-confirmation").assertIsDisplayed()
        compose.onNodeWithTag("track-entry-delete-problem").assertIsDisplayed()
        compose.onNodeWithTag("track-entry-delete-cancel").assertIsDisplayed()
    }

    private fun entryForm(): TrackEntryFormSnapshot {
        val track = Track(
            id = 7,
            uuid = "track-7",
            name = "Books",
            description = "",
            icon = "📚",
            areaId = "",
            area = "",
            tags = emptyList(),
            pinned = false,
            archived = false,
            position = 0,
            createdAtMillis = 1,
            updatedAtMillis = 1,
        )
        val field = TrackField(
            id = 11,
            uuid = "title",
            trackId = track.id,
            name = "Title",
            type = TrackFieldType.ShortText,
            position = 0,
            required = true,
            primary = true,
            showInList = true,
            dimension = null,
            unitId = null,
            precision = 1,
            scaleMin = null,
            scaleMax = null,
            scaleLowLabel = "",
            scaleHighLabel = "",
            createdAtMillis = 1,
            updatedAtMillis = 1,
        )
        val boundary = TrackEntryFormBoundary(
            trackId = track.id,
            trackUuid = track.uuid,
            trackCreatedAtMillis = track.createdAtMillis,
            writable = true,
            semanticRevisionToken = "form-r1",
            fieldContracts = listOf(
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
                ),
            ),
        )
        return TrackEntryFormSnapshot(boundary, track, listOf(field), emptyList(), emptyList())
    }

    private fun entryEditSnapshot(form: TrackEntryFormSnapshot) = TrackEntryEditSnapshot(
        boundary = TrackEntryBoundary(
            formBoundary = form.boundary,
            entryId = 41,
            entryUuid = "entry-41",
            entryCreatedAtMillis = 1,
            semanticRevisionToken = "entry-r1",
        ),
        form = form,
        draft = TrackEntryDraft(
            entryDate = LocalDate.of(2026, 9, 1),
            values = mapOf("title" to TrackValueDraft(textValue = "The Dispossessed")),
        ),
        displayName = "The Dispossessed",
        populatedValueCount = 1,
    )
}

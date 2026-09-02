package com.whip.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.whip.app.WhipApplication
import com.whip.app.core.AppSettings
import com.whip.app.domain.WhipTag
import com.whip.app.ui.theme.WhipTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TagManagementUiTest {
    @get:Rule val compose = createComposeRule()

    private val app: WhipApplication
        get() = ApplicationProvider.getApplicationContext()

    @Before
    fun resetData() = runBlocking {
        app.backupRepository.deleteAllData()
        app.settingsRepository.update { AppSettings(setupCompleted = true) }
    }

    @Test
    fun searchFindsArchivedTagWithoutOpeningArchivedSection() {
        showManager(
            SettingsUiState(
                tags = listOf(tag("active", "Work"), tag("archived", "Powerlifting", archived = true)),
            ),
        )

        compose.onAllNodesWithText("#Powerlifting").assertCountEquals(0)
        compose.onNodeWithTag("tag-search").performTextReplacement("power")

        compose.onNodeWithText("Archived Matches · 1").assertIsDisplayed()
        compose.onNodeWithText("#Powerlifting").assertIsDisplayed()
        compose.onNodeWithText("Restore").assertIsDisplayed()
    }

    @Test
    fun createWithArchivedNameOffersRestoreInsteadOfDuplicate() {
        showManager(
            SettingsUiState(tags = listOf(tag("archived", "Focus", archived = true))),
        )

        compose.onNodeWithTag("create-tag-action").performClick()
        compose.onNodeWithTag("create-tag-name").performTextReplacement("focus")

        compose.onNodeWithText("#Focus is archived. Restore the same Tag and its current references.")
            .assertIsDisplayed()
        compose.onNodeWithText("Restore Existing Tag").assertIsDisplayed()
    }

    @Test
    fun commaSeparatedNameExplainsThePersistenceBoundaryBeforeSubmission() {
        showManager(SettingsUiState())

        compose.onNodeWithTag("create-tag-action").performClick()
        compose.onNodeWithTag("create-tag-name").performTextReplacement("Work, Home")

        compose.onNodeWithText("Use separate Tags instead of commas.").assertIsDisplayed()
        compose.onNodeWithText("Create").assertIsNotEnabled()
    }

    @Test
    fun renameConflictExplainsMergeAndCannotBeSubmitted() {
        showManager(
            SettingsUiState(tags = listOf(tag("source", "Next"), tag("target", "Focus"))),
        )

        compose.onNodeWithTag("tag-menu-source").performClick()
        compose.onNodeWithText("Rename").performClick()
        compose.onNodeWithTag("rename-tag-name").performTextReplacement("focus")

        compose.onNodeWithText("#Focus already exists. Cancel and use Merge instead.")
            .assertIsDisplayed()
        compose.onNodeWithText("Rename Everywhere").assertIsNotEnabled()
    }

    @Test
    fun failedRenameKeepsTheDialogAndDraftOwnedByTheRequest() {
        showManager(
            SettingsUiState(tags = listOf(tag("missing", "Original"))),
        )

        compose.onNodeWithTag("tag-menu-missing").performClick()
        compose.onNodeWithText("Rename").performClick()
        compose.onNodeWithTag("rename-tag-name").performTextReplacement("Reviewed")
        compose.onNodeWithText("Rename Everywhere").performClick()

        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithTag("tag-mutation-error").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("rename-tag-dialog").assertIsDisplayed()
        compose.onNodeWithTag("rename-tag-name").assertTextContains("Reviewed")
        compose.onNodeWithTag("tag-mutation-error").assertIsDisplayed()
    }

    @Test
    fun archiveConfirmationNamesEveryAffectedProductAreaAndPreservesHistory() {
        val tag = tag("focus", "Focus")
        showManager(
            SettingsUiState(
                tags = listOf(tag),
                tagUsage = mapOf(
                    tag.id to TagUsageCounts(tasks = 1, habits = 2, goals = 3, tracks = 4),
                ),
            ),
        )

        compose.onNodeWithTag("tag-menu-focus").performClick()
        compose.onNodeWithText("Archive").performClick()

        compose.onNodeWithTag("archive-tag-dialog").assertIsDisplayed()
        compose.onNodeWithText(
            "1 Task · 2 Habits · 3 Goals · 4 Tracks keep this label and remain searchable. " +
                "The Tag moves out of the active list until restored.",
        ).assertIsDisplayed()
    }

    @Test
    fun narrowLargeTextHeaderStacksWithoutHidingPrimaryActions() {
        val viewModel = SettingsViewModel(app)
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
                WhipTheme(dynamicColor = false) {
                    Box(Modifier.width(320.dp).fillMaxHeight()) {
                        TagManagementDialog(
                            state = SettingsUiState(),
                            viewModel = viewModel,
                            paneMaxWidth = 320.dp,
                            onDismiss = {},
                        )
                    }
                }
            }
        }

        val close = compose.onNodeWithTag("tag-close-action").assertIsDisplayed().getUnclippedBoundsInRoot()
        val create = compose.onNodeWithTag("create-tag-action").assertIsDisplayed().getUnclippedBoundsInRoot()
        compose.onNodeWithTag("tag-search").assertIsDisplayed()
        assertTrue(close.bottom <= create.top)

        compose.onNodeWithTag("create-tag-action").performClick()
        compose.onNodeWithTag("create-tag-name").assertIsDisplayed()
        compose.onNodeWithText("Cancel").assertIsDisplayed()
    }

    private fun showManager(state: SettingsUiState) {
        val viewModel = SettingsViewModel(app)
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                TagManagementDialog(state = state, viewModel = viewModel, onDismiss = {})
            }
        }
    }

    private fun tag(id: String, name: String, archived: Boolean = false) = WhipTag(
        id = id,
        name = name,
        archived = archived,
        createdAtMillis = 1L,
        updatedAtMillis = 1L,
    )
}

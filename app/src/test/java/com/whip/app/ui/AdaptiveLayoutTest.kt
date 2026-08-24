package com.whip.app.ui

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptiveLayoutTest {
    @Test fun compactWindowsKeepBottomNavigation() {
        assertEquals(WhipAdaptiveLayout.Compact, selectWhipAdaptiveLayout(599, 900, null))
        assertEquals(WhipAdaptiveLayout.Compact, selectWhipAdaptiveLayout(700, 479, null))
    }

    @Test fun wideUsableWindowsUseNavigationRail() {
        assertEquals(WhipAdaptiveLayout.NavigationRail, selectWhipAdaptiveLayout(600, 480, null))
        assertEquals(WhipAdaptiveLayout.NavigationRail, selectWhipAdaptiveLayout(839, 900, null))
        assertEquals(WhipAdaptiveLayout.ExpandedDashboard, selectWhipAdaptiveLayout(840, 900, null))
    }

    @Test fun separatingFoldsTakePriorityOverGenericWidth() {
        val book = WhipFoldInfo(WhipFoldOrientation.Vertical, 880, 0, 920, 1_800, true, false)
        val tabletop = WhipFoldInfo(WhipFoldOrientation.Horizontal, 0, 880, 1_800, 920, true, true)

        assertEquals(WhipAdaptiveLayout.BookFold, selectWhipAdaptiveLayout(700, 900, book))
        assertEquals(WhipAdaptiveLayout.TabletopFold, selectWhipAdaptiveLayout(700, 900, tabletop))
    }

    @Test fun nonSeparatingCreaseDoesNotForceTwoPanes() {
        val crease = WhipFoldInfo(WhipFoldOrientation.Vertical, 880, 0, 880, 1_800, false, false)
        assertEquals(WhipAdaptiveLayout.Compact, selectWhipAdaptiveLayout(500, 900, crease))
    }

    @Test fun flatVerticalFoldUsesBothSidesOfTheLargeInnerDisplay() {
        val flatFold = WhipFoldInfo(WhipFoldOrientation.Vertical, 880, 0, 880, 1_800, false, false)

        assertEquals(WhipAdaptiveLayout.BookFold, selectWhipAdaptiveLayout(700, 900, flatFold))
    }

    @Test fun rootEditorsExpandOnlyWhenThePaneCannotFitReadableContent() {
        assertFalse(shouldExpandRootEditor(320.dp, 1f))
        assertTrue(shouldExpandRootEditor(319.dp, 1f))
        assertFalse(shouldExpandRootEditor(640.dp, 2f))
        assertTrue(shouldExpandRootEditor(500.dp, 2f))
        assertFalse(shouldExpandRootEditor(640.dp, 3.2f))
    }
}

package com.whip.app.reminders

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutomationPromptNotificationsTest {
    @Test fun `prompt wakes when its delay expires`() {
        assertFalse(automationPromptShouldNotify(true, null, null, 20_000, 10_000))
        assertTrue(automationPromptShouldNotify(true, null, null, 20_000, 19_000))
    }

    @Test fun `paused dismissed and delivered prompts never notify again`() {
        assertFalse(automationPromptShouldNotify(false, null, null, 10_000, 10_000))
        assertFalse(automationPromptShouldNotify(true, 9_000, null, 10_000, 10_000))
        assertFalse(automationPromptShouldNotify(true, null, 9_000, 10_000, 10_000))
        assertFalse(automationPromptShouldNotify(true, null, null, 10_000, 10_000, manualPrompt = false))
    }
}

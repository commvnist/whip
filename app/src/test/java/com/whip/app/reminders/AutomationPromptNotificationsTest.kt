package com.whip.app.reminders

import org.junit.Assert.assertFalse
import org.junit.Test

class AutomationPromptNotificationsTest {
    @Test fun `retired prompts never notify`() {
        assertFalse(automationPromptShouldNotify(true, null, null, 20_000, 10_000))
        assertFalse(automationPromptShouldNotify(true, null, null, 20_000, 19_000))
        assertFalse(automationPromptShouldNotify(true, null, null, 10_000, 10_000, manualPrompt = false))
    }
}

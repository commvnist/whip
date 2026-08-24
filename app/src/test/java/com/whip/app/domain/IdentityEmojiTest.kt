package com.whip.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IdentityEmojiTest {
    @Test
    fun `accepts the emoji sequences users reasonably paste`() {
        listOf("✅", "📚", "♟️", "🧑🏽‍💻", "👨‍👩‍👧‍👦", "🇨🇦", "1️⃣", "❤️")
            .forEach { emoji -> assertTrue(emoji, emoji.isIdentityEmoji()) }
    }

    @Test
    fun `rejects prose symbols and unrelated multiple emoji`() {
        listOf("", "A", "check", "✓", "◆", "📚🔥", "📚 books")
            .forEach { value -> assertFalse(value, value.isIdentityEmoji()) }
    }

    @Test
    fun `normalization converts every shipped legacy identity symbol`() {
        val expected = mapOf(
            "✓" to "✅", "○" to "⭕", "◆" to "🔹", "◎" to "🎯", "⚑" to "🚩",
            "✚" to "💊", "◉" to "💧", "▤" to "📋", "★" to "⭐", "⏱" to "⏱️",
            "◈" to "⚖️", "$" to "💰", "↗" to "📈", "◫" to "🗂️", "✦" to "✨", "♟" to "♟️",
        )
        expected.forEach { (legacy, emoji) -> assertEquals(emoji, legacy.normalizedIdentityEmoji(DEFAULT_TRACK_EMOJI)) }
    }

    @Test
    fun `normalization preserves valid custom emoji and defaults invalid values`() {
        assertEquals("🧑🏽‍💻", "  🧑🏽‍💻  ".normalizedIdentityEmoji(DEFAULT_TRACK_EMOJI))
        assertEquals(DEFAULT_TRACK_EMOJI, "database".normalizedIdentityEmoji(DEFAULT_TRACK_EMOJI))
    }

    @Test
    fun `common library has one hundred valid unique ranked choices`() {
        assertEquals(100, IDENTITY_EMOJI_PRESETS.size)
        assertEquals(100, IDENTITY_EMOJI_PRESETS.map { it.emoji }.distinct().size)
        assertEquals(100, IDENTITY_EMOJI_PRESETS.map { it.label }.distinct().size)
        IDENTITY_EMOJI_PRESETS.forEach { preset ->
            assertTrue("${preset.label}: ${preset.emoji}", preset.emoji.isIdentityEmoji())
            assertTrue(preset.label, preset.searchTerms.isNotBlank())
        }
        assertEquals(
            listOf("General Task", "Plan & Schedule", "Goal", "Notes & Writing", "Reminder"),
            IDENTITY_EMOJI_PRESETS.take(5).map { it.label },
        )
        val fitnessIndex = IDENTITY_EMOJI_PRESETS.indexOfFirst { it.label == "Fitness" }
        assertTrue("Fitness must be visible near the top of the common library", fitnessIndex in 0 until 10)
        assertEquals("💪", IDENTITY_EMOJI_PRESETS[fitnessIndex].emoji)
    }

    @Test
    fun `common library covers core Whip and everyday planning intents`() {
        val searchable = IDENTITY_EMOJI_PRESETS.associateBy { it.label }
        listOf(
            "Work", "Home", "Health", "Fitness", "Reading", "Groceries", "Cleaning", "Sleep",
            "Hydration", "Study", "Budget & Saving", "Medication",
            "Recovery & Sobriety", "Checklist & Tracking", "Bills & Payments", "Pet Care",
            "Coding", "Movies & Shows", "Chess", "Language Practice", "Childcare",
        ).forEach { intent -> assertTrue("Missing $intent", intent in searchable) }
    }

    @Test
    fun `custom emoji normalization preserves named choices without duplicating defaults`() {
        assertEquals(
            listOf(CustomIdentityEmoji("🦊", "Fox"), CustomIdentityEmoji("🦄", "Unicorn")),
            normalizeCustomIdentityEmojis(
                listOf(
                    CustomIdentityEmoji(" 🦊 ", " Fox "),
                    CustomIdentityEmoji("✅", "Built-In"),
                    CustomIdentityEmoji("text", "Invalid"),
                    CustomIdentityEmoji("🦄", "Unicorn"),
                    CustomIdentityEmoji("🦊", "Duplicate Fox"),
                    CustomIdentityEmoji("🦁", " "),
                    CustomIdentityEmoji("🦋", "fox"),
                ),
            ),
        )
    }
}

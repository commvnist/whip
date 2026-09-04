package com.whip.app.ui

import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WhipCalendarPresentationTest {
    @Test
    fun formatterUsesRequestedLocaleForFullAndShortLabels() {
        val locale = Locale.FRENCH
        val formatter = WhipWeekdayFormatter(locale)

        DayOfWeek.entries.forEach { day ->
            assertEquals(
                day.getDisplayName(TextStyle.FULL, locale),
                formatter.label(day, WhipWeekdayLabelWidth.Full),
            )
            assertEquals(
                day.getDisplayName(TextStyle.SHORT, locale),
                formatter.label(day, WhipWeekdayLabelWidth.Short),
            )
        }
    }

    @Test
    fun compactEnglishLabelsStayShortAndDistinguishEveryWeekday() {
        val formatter = WhipWeekdayFormatter(Locale.ENGLISH)
        val labels = DayOfWeek.entries.map { day ->
            formatter.label(day, WhipWeekdayLabelWidth.Compact)
        }

        assertEquals(listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su"), labels)
        assertEquals(labels.size, labels.map(String::lowercase).distinct().size)
    }

    @Test
    fun compactLabelsRemainCollisionSafeAcrossRepresentativeScripts() {
        listOf(Locale.FRENCH, Locale.GERMAN, Locale.forLanguageTag("ar"), Locale.JAPANESE).forEach { locale ->
            val formatter = WhipWeekdayFormatter(locale)
            val labels = DayOfWeek.entries.map { day ->
                formatter.label(day, WhipWeekdayLabelWidth.Compact).lowercase(locale)
            }

            assertEquals("Compact labels collide for $locale: $labels", 7, labels.distinct().size)
            assertTrue("Compact labels must not be blank for $locale", labels.none(String::isBlank))
        }
    }

    @Test
    fun weekdayOrderChangesPresentationWithoutChangingStoredIdentity() {
        val sundayFirst = orderedWhipWeekdays(DayOfWeek.SUNDAY)

        assertEquals(DayOfWeek.SUNDAY, sundayFirst.first())
        assertEquals(DayOfWeek.SATURDAY, sundayFirst.last())
        assertEquals(
            listOf("SUNDAY", "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY"),
            sundayFirst.map(DayOfWeek::name),
        )
    }
}

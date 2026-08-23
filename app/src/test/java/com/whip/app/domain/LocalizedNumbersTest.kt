package com.whip.app.domain

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalizedNumbersTest {
    @Test fun directInputsAcceptLocaleDecimalAndInvariantDotWithoutGroupingAmbiguity() {
        assertEquals(2.5, "2,5".toWhipDoubleOrNull(Locale.GERMANY) ?: -1.0, 0.0)
        assertEquals(2.5, "2.5".toWhipDoubleOrNull(Locale.GERMANY) ?: -1.0, 0.0)
        assertNull("1,000".toWhipDoubleOrNull(Locale.US))
        assertNull("1.000,5".toWhipDoubleOrNull(Locale.GERMANY))
    }

    @Test fun commaDecimalRangesAndSemicolonListsAreUnambiguous() {
        val range = parseNumericSequence("1,5-2,5", .5, locale = Locale.GERMANY)
        assertEquals(listOf(1.5, 2.0, 2.5), range.values)
        assertEquals(listOf(1.25, 2.5), parseNumericSequence("1,25;2,5", null, locale = Locale.GERMANY).values)
        assertTrue(parseNumericSequence("1,25,2,5", null, locale = Locale.GERMANY).error?.contains("semicolons") == true)
    }
}

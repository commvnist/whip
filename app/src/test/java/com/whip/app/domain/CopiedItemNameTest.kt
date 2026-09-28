package com.whip.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CopiedItemNameTest {
    @Test fun copyNamesStayWithinTheirExistingLengthContractsWithoutSplittingUnicode() {
        assertEquals("Read copy", copiedItemName("Read"))
        val utf16Name = "a".repeat(94) + "😀" + "tail"
        val copy = copiedItemName(utf16Name)
        assertEquals("a".repeat(94) + " copy", copy)
        assertTrue(copy.length <= 100)
        assertTrue(copiedItemName(copy).length <= 100)
        assertEquals("a".repeat(95) + " Copy", copiedItemName("a".repeat(100), suffix = " Copy"))

        val taskName = "😀".repeat(200)
        val taskCopy = copiedItemName(taskName, maximumLength = 200, codePointLimit = true)
        assertEquals("😀".repeat(195) + " copy", taskCopy)
        assertEquals(200, taskCopy.codePointCount(0, taskCopy.length))
        assertTrue(TaskDraft(title = taskCopy).validationErrors().isEmpty())
        assertEquals(200, copiedItemName(taskCopy, maximumLength = 200, codePointLimit = true).let { it.codePointCount(0, it.length) })
    }
}

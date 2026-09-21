package com.whip.app

import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.whip.app.data.SafPortableBackupDocumentStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SafPortableBackupDocumentStoreTest {
    private val resolver = ApplicationProvider.getApplicationContext<android.content.Context>().contentResolver
    private val store = SafPortableBackupDocumentStore(resolver)

    @Test
    fun failedChildQueryIsNotMistakenForAnEmptyBackupFolder() {
        val tree = Uri.parse("content://com.whip.app.test.null-cursor-documents/tree/offline")

        val failure = runCatching { store.list(tree) }.exceptionOrNull()

        assertTrue("A null provider Cursor must fail closed", failure is IllegalStateException)
        assertTrue(failure?.message.orEmpty().contains("could not list", ignoreCase = true))
    }

    @Test
    fun successfulEmptyChildQueryStillReportsAnEmptyFolder() {
        val tree = Uri.parse("content://com.whip.app.test.null-cursor-documents/tree/empty")

        assertEquals(emptyList<Any>(), store.list(tree))
    }
}

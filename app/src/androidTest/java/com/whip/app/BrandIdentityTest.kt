package com.whip.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.runner.RunWith
import org.junit.Test

@RunWith(AndroidJUnit4::class)
class BrandIdentityTest {
    @Test
    fun debugBuildUsesThePlayPackageFamilyAndCapitalizedName() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        assertEquals("commvne.com.whip.app.debug", context.packageName)
        assertEquals("Whip Dev", context.applicationInfo.loadLabel(context.packageManager).toString())
    }
}

package com.whip.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.runner.RunWith
import org.junit.Test
import kotlin.math.abs

@RunWith(AndroidJUnit4::class)
class BrandIdentityTest {
    @Test
    fun debugBuildUsesThePlayPackageFamilyAndCapitalizedName() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        assertEquals("commvne.com.whip.app.debug", context.packageName)
        assertEquals("Whip Dev", context.applicationInfo.loadLabel(context.packageManager).toString())
    }

    @Test
    fun launcherAndInAppMarksShareCenteredSafeZoneGeometry() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val launcherBounds = context.lightPixelBounds(R.drawable.ic_launcher_foreground)
        val inAppBounds = context.lightPixelBounds(R.drawable.ic_whip_mark)

        assertEquals(launcherBounds.centerX, inAppBounds.centerX, 0.25f)
        assertEquals(launcherBounds.centerY, inAppBounds.centerY, 0.25f)
        assertTrue(abs(launcherBounds.width - inAppBounds.width) <= 2)
        assertTrue(abs(launcherBounds.height - inAppBounds.height) <= 2)
        listOf(launcherBounds, inAppBounds).forEach { bounds ->
            assertEquals(53.5f, bounds.centerX, 0.75f)
            assertEquals(53.5f, bounds.centerY, 0.75f)
            assertTrue(bounds.width in 49..55)
            assertTrue(bounds.height in 37..42)
            assertTrue(bounds.minX >= 27 && bounds.maxX <= 81)
            assertTrue(bounds.minY >= 33 && bounds.maxY <= 75)
        }
    }
}

private data class PixelBounds(val minX: Int, val minY: Int, val maxX: Int, val maxY: Int) {
    val width: Int get() = maxX - minX + 1
    val height: Int get() = maxY - minY + 1
    val centerX: Float get() = (minX + maxX) / 2f
    val centerY: Float get() = (minY + maxY) / 2f
}

private fun Context.lightPixelBounds(resourceId: Int): PixelBounds {
    val bitmap = Bitmap.createBitmap(108, 108, Bitmap.Config.ARGB_8888)
    val drawable = requireNotNull(getDrawable(resourceId))
    drawable.setBounds(0, 0, bitmap.width, bitmap.height)
    drawable.draw(Canvas(bitmap))

    var minX = bitmap.width
    var minY = bitmap.height
    var maxX = -1
    var maxY = -1
    for (y in 0 until bitmap.height) {
        for (x in 0 until bitmap.width) {
            val pixel = bitmap.getPixel(x, y)
            if (Color.alpha(pixel) > 0 && minOf(Color.red(pixel), Color.green(pixel), Color.blue(pixel)) > 200) {
                minX = minOf(minX, x)
                minY = minOf(minY, y)
                maxX = maxOf(maxX, x)
                maxY = maxOf(maxY, y)
            }
        }
    }
    check(maxX >= minX && maxY >= minY) { "No light mark pixels found in resource $resourceId" }
    return PixelBounds(minX, minY, maxX, maxY)
}

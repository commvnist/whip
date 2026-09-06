package com.whip.app

import android.content.ContentValues
import android.graphics.Bitmap
import android.os.Environment
import android.provider.MediaStore
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import java.io.File

private val visualCatalogId = Regex("^[a-z0-9][a-z0-9._-]*$")
private val capturedVisualCatalogIds = linkedSetOf<String>()
private val visualCatalogRelativePath = "${Environment.DIRECTORY_DOWNLOADS}/whip-ui-catalog/"

/** Records one device-level PNG and matching accessibility hierarchy for catalog accounting. */
internal fun captureVisualCatalogSurface(surfaceId: String) {
    require(visualCatalogId.matches(surfaceId)) { "Unsafe visual catalog surface ID: $surfaceId" }
    synchronized(capturedVisualCatalogIds) {
        check(capturedVisualCatalogIds.add(surfaceId)) {
            "Visual catalog surface was captured twice in one instrumentation process: $surfaceId"
        }
    }

    val instrumentation = InstrumentationRegistry.getInstrumentation()
    instrumentation.waitForIdleSync()
    instrumentation.uiAutomation.waitForIdle(750L, 5_000L)

    val screenshot = checkNotNull(instrumentation.uiAutomation.takeScreenshot()) {
        "Android did not provide a screenshot for $surfaceId"
    }
    try {
        insertCatalogAsset(surfaceId, "png", "image/png").use { output ->
            check(screenshot.compress(Bitmap.CompressFormat.PNG, 100, output)) {
                "Could not encode visual catalog screenshot for $surfaceId"
            }
        }
    } finally {
        screenshot.recycle()
    }

    val hierarchyFile = File(instrumentation.targetContext.cacheDir, "$surfaceId.xml")
    try {
        UiDevice.getInstance(instrumentation).dumpWindowHierarchy(hierarchyFile)
        check(hierarchyFile.isFile && hierarchyFile.length() > 0L) {
            "Visual catalog hierarchy is missing or empty for $surfaceId"
        }
        hierarchyFile.inputStream().use { input ->
            insertCatalogAsset(surfaceId, "xml", "application/xml").use(input::copyTo)
        }
    } finally {
        hierarchyFile.delete()
    }
}

private fun insertCatalogAsset(surfaceId: String, extension: String, mimeType: String) =
    InstrumentationRegistry.getInstrumentation().targetContext.contentResolver.let { resolver ->
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "$surfaceId.$extension")
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, visualCatalogRelativePath)
        }
        val uri = checkNotNull(resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)) {
            "Could not create visual catalog asset for $surfaceId.$extension"
        }
        checkNotNull(resolver.openOutputStream(uri, "w")) {
            "Could not open visual catalog asset for $surfaceId.$extension"
        }
    }

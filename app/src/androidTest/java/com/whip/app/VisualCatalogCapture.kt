package com.whip.app

import android.content.ContentValues
import android.graphics.Bitmap
import android.os.Environment
import android.provider.MediaStore
import android.view.Choreographer
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

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
    // Compose semantics can become idle before the window has drawn the newly
    // asserted state. Wait through two real display frames so a correctly named
    // catalog artifact cannot contain the preceding page or dialog.
    repeat(2) {
        val frameDrawn = CountDownLatch(1)
        instrumentation.runOnMainSync {
            Choreographer.getInstance().postFrameCallback { frameDrawn.countDown() }
        }
        check(frameDrawn.await(5L, TimeUnit.SECONDS)) {
            "The UI did not draw a stable frame for $surfaceId"
        }
    }
    instrumentation.waitForIdleSync()

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
        val hierarchy = hierarchyFile.readText()
        check("package=\"${instrumentation.targetContext.packageName}\"" in hierarchy) {
            "Visual catalog hierarchy for $surfaceId is obscured by a non-Whip window"
        }
        check(" keeps stopping\"" !in hierarchy && " isn't responding\"" !in hierarchy) {
            "Visual catalog hierarchy for $surfaceId contains an Android crash or ANR sheet"
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

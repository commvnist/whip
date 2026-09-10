package com.whip.app

import android.content.ContentValues
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.os.SystemClock
import android.provider.MediaStore
import android.view.Choreographer
import android.view.ViewTreeObserver
import android.view.accessibility.AccessibilityEvent
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import androidx.test.uiautomator.UiDevice
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

private val visualCatalogId = Regex("^[a-z0-9][a-z0-9._-]*$")
private val capturedVisualCatalogIds = linkedSetOf<String>()
private val capturedVisualCatalogFingerprints = mutableMapOf<String, VisualFingerprint>()
private val visualCatalogRelativePath = "${Environment.DIRECTORY_DOWNLOADS}/whip-ui-catalog/"

/** Records one device-level PNG and matching accessibility hierarchy for catalog accounting. */
internal fun captureVisualCatalogSurface(
    surfaceId: String,
    visuallyDistinctFrom: String? = null,
) {
    require(visualCatalogId.matches(surfaceId)) { "Unsafe visual catalog surface ID: $surfaceId" }
    synchronized(capturedVisualCatalogIds) {
        check(capturedVisualCatalogIds.add(surfaceId)) {
            "Visual catalog surface was captured twice in one instrumentation process: $surfaceId"
        }
    }

    val instrumentation = InstrumentationRegistry.getInstrumentation()
    awaitVisualCaptureFrame(surfaceId)

    val referenceFingerprint = visuallyDistinctFrom?.let { referenceId ->
        synchronized(capturedVisualCatalogFingerprints) {
            checkNotNull(capturedVisualCatalogFingerprints[referenceId]) {
                "Visual reference $referenceId must be captured before $surfaceId"
            }
        }
    }
    var screenshot = takeDeviceScreenshot(surfaceId)
    var fingerprint = screenshot.contentFingerprint()
    var renderAttempts = 0
    while (
        referenceFingerprint != null &&
        fingerprint.meanChannelDifference(referenceFingerprint) < MIN_DISTINCT_CONTENT_DIFFERENCE &&
        renderAttempts < MAX_DISTINCT_RENDER_ATTEMPTS
    ) {
        renderAttempts += 1
        screenshot.recycle()
        SystemClock.sleep(DISTINCT_RENDER_RETRY_MILLIS)
        waitForWindowDraw(surfaceId)
        waitForRenderedFrames(surfaceId)
        instrumentation.waitForIdleSync()
        screenshot = takeDeviceScreenshot(surfaceId)
        fingerprint = screenshot.contentFingerprint()
    }
    check(
        referenceFingerprint == null ||
            fingerprint.meanChannelDifference(referenceFingerprint) >= MIN_DISTINCT_CONTENT_DIFFERENCE,
    ) { "$surfaceId never rendered distinctly from $visuallyDistinctFrom" }
    synchronized(capturedVisualCatalogFingerprints) {
        capturedVisualCatalogFingerprints[surfaceId] = fingerprint
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

    refreshAccessibilityHierarchy(surfaceId)
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
        check("NAF=\"true\"" !in hierarchy) {
            "Visual catalog hierarchy for $surfaceId contains an unlabeled interactive node"
        }
        hierarchyFile.inputStream().use { input ->
            insertCatalogAsset(surfaceId, "xml", "application/xml").use(input::copyTo)
        }
    } finally {
        hierarchyFile.delete()
    }
}

private const val MIN_DISTINCT_CONTENT_DIFFERENCE = 2.0
private const val MAX_DISTINCT_RENDER_ATTEMPTS = 12
private const val DISTINCT_RENDER_RETRY_MILLIS = 50L
private const val FINGERPRINT_SAMPLE_STEP = 8

private data class VisualFingerprint(
    val width: Int,
    val height: Int,
    val pixels: IntArray,
)

private fun takeDeviceScreenshot(surfaceId: String): Bitmap = checkNotNull(
    InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot(),
) { "Android did not provide a screenshot for $surfaceId" }

private fun Bitmap.contentFingerprint(): VisualFingerprint {
    val contentTop = height / 16
    val contentBottom = height - (height / 10)
    val sampleWidth = (width + FINGERPRINT_SAMPLE_STEP - 1) / FINGERPRINT_SAMPLE_STEP
    val sampleHeight = (contentBottom - contentTop + FINGERPRINT_SAMPLE_STEP - 1) / FINGERPRINT_SAMPLE_STEP
    val pixels = IntArray(sampleWidth * sampleHeight)
    var index = 0
    var y = contentTop
    while (y < contentBottom) {
        var x = 0
        while (x < width) {
            pixels[index++] = getPixel(x, y)
            x += FINGERPRINT_SAMPLE_STEP
        }
        y += FINGERPRINT_SAMPLE_STEP
    }
    return VisualFingerprint(sampleWidth, sampleHeight, pixels)
}

private fun VisualFingerprint.meanChannelDifference(other: VisualFingerprint): Double {
    check(width == other.width && height == other.height) {
        "Cannot compare visual fingerprints with different dimensions"
    }
    var totalDifference = 0L
    pixels.indices.forEach { index ->
        val current = pixels[index]
        val reference = other.pixels[index]
        totalDifference += kotlin.math.abs((current shr 16 and 0xff) - (reference shr 16 and 0xff))
        totalDifference += kotlin.math.abs((current shr 8 and 0xff) - (reference shr 8 and 0xff))
        totalDifference += kotlin.math.abs((current and 0xff) - (reference and 0xff))
    }
    return totalDifference.toDouble() / (pixels.size * 3)
}

/** Shared by MediaStore captures and legacy app-private captures. */
internal fun awaitVisualCaptureFrame(surfaceId: String) {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    instrumentation.waitForIdleSync()
    instrumentation.uiAutomation.waitForIdle(750L, 5_000L)
    waitForRenderedFrames(surfaceId)
    // Accessibility can precede SurfaceFlinger. Prime the screenshot path, then cross a draw boundary.
    checkNotNull(instrumentation.uiAutomation.takeScreenshot()) {
        "Android did not provide a synchronization screenshot for $surfaceId"
    }.recycle()
    waitForWindowDraw(surfaceId)
    waitForRenderedFrames(surfaceId)
    instrumentation.waitForIdleSync()
}

private fun waitForRenderedFrames(surfaceId: String) {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    repeat(2) {
        val frameDrawn = CountDownLatch(1)
        instrumentation.runOnMainSync {
            Choreographer.getInstance().postFrameCallback { frameDrawn.countDown() }
        }
        check(frameDrawn.await(5L, TimeUnit.SECONDS)) {
            "The UI did not draw a stable frame for $surfaceId"
        }
    }
}

private fun waitForWindowDraw(surfaceId: String) {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val windowDrawn = CountDownLatch(1)
    instrumentation.runOnMainSync {
        val activity = checkNotNull(
            ActivityLifecycleMonitorRegistry.getInstance()
                .getActivitiesInStage(Stage.RESUMED)
                .singleOrNull(),
        ) { "Expected one resumed Activity while capturing $surfaceId" }
        val decorView = activity.window.decorView
        val observer = decorView.viewTreeObserver
        val listener = object : ViewTreeObserver.OnDrawListener {
            override fun onDraw() {
                windowDrawn.countDown()
                decorView.post {
                    if (observer.isAlive) observer.removeOnDrawListener(this)
                }
            }
        }
        observer.addOnDrawListener(listener)
        decorView.invalidate()
    }
    check(windowDrawn.await(5L, TimeUnit.SECONDS)) {
        "The active window did not draw the requested state for $surfaceId"
    }
}

internal fun refreshAccessibilityHierarchy(surfaceId: String) {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    instrumentation.runOnMainSync {
        val activity = checkNotNull(
            ActivityLifecycleMonitorRegistry.getInstance()
                .getActivitiesInStage(Stage.RESUMED)
                .singleOrNull(),
        ) { "Expected one resumed Activity while refreshing accessibility for $surfaceId" }
        activity.window.decorView.sendAccessibilityEvent(
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
        )
    }
    instrumentation.uiAutomation.waitForIdle(250L, 5_000L)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        instrumentation.uiAutomation.clearCache()
    }
    val root = checkNotNull(instrumentation.uiAutomation.rootInActiveWindow) {
        "Android did not expose an accessibility root for $surfaceId"
    }
    check(root.refresh()) {
        "Android could not refresh the accessibility hierarchy for $surfaceId"
    }

    // UiAutomator's hierarchy dumper asks UiAutomation for the window roots
    // again, and that call can reuse descendant nodes cached before a fast
    // Compose destination change. Refresh the connected tree so the cache that
    // the dumper reads represents the same rendered state as the screenshot.
    val pending = ArrayDeque<android.view.accessibility.AccessibilityNodeInfo>()
    pending.add(root)
    var refreshedNodes = 0
    while (pending.isNotEmpty()) {
        val node = pending.removeFirst()
        refreshedNodes += 1
        repeat(node.childCount) { index ->
            node.getChild(index)?.let { child ->
                if (child.refresh()) pending.add(child)
            }
        }
    }
    check(refreshedNodes > 1) {
        "Android exposed an empty accessibility hierarchy for $surfaceId"
    }
}

private fun insertCatalogAsset(surfaceId: String, extension: String, mimeType: String): java.io.OutputStream {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        val directory = checkNotNull(context.getExternalFilesDir("whip-ui-catalog"))
        check(directory.isDirectory || directory.mkdirs())
        return File(directory, "$surfaceId.$extension").outputStream()
    }
    return context.contentResolver.let { resolver ->
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
}

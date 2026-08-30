package com.whip.app.widget

import android.content.Context
import android.content.Intent
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StrikethroughSpan
import android.view.View
import android.widget.RemoteViews
import androidx.core.content.edit
import com.whip.app.R
import java.nio.charset.StandardCharsets
import java.util.Base64

internal enum class WidgetSnapshotKind {
    TaskAgenda,
    HabitTracking,
}

/**
 * Display-only widget data retained after a successful refresh. Cached rows are
 * deliberately not actionable: they keep a failed widget informative without
 * applying a completion against data that may no longer be current.
 */
internal data class CachedWidgetRow(
    val title: String,
    val meta: String,
    val isChild: Boolean,
    val completed: Boolean,
)

internal data class CachedWidgetSnapshot(
    val rows: List<CachedWidgetRow>,
    val savedAtMillis: Long,
)

internal object WidgetSnapshotCache {
    private const val PREFS = "whip_widget_snapshots"
    private const val MAX_CACHED_ROWS = 100

    fun save(
        context: Context,
        kind: WidgetSnapshotKind,
        appWidgetId: Int,
        rows: List<CachedWidgetRow>,
        savedAtMillis: Long = System.currentTimeMillis(),
    ) {
        if (appWidgetId < 0) return
        val snapshot = CachedWidgetSnapshot(rows.take(MAX_CACHED_ROWS), savedAtMillis)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit {
            putString(key(kind, appWidgetId), WidgetSnapshotCodec.encode(snapshot))
        }
    }

    fun load(
        context: Context,
        kind: WidgetSnapshotKind,
        appWidgetId: Int,
    ): CachedWidgetSnapshot? {
        if (appWidgetId < 0) return null
        val encoded = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(key(kind, appWidgetId), null)
            ?: return null
        return WidgetSnapshotCodec.decode(encoded)
    }

    fun remove(context: Context, appWidgetIds: IntArray) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit {
            appWidgetIds.forEach { appWidgetId ->
                WidgetSnapshotKind.entries.forEach { kind -> remove(key(kind, appWidgetId)) }
            }
        }
    }

    private fun key(kind: WidgetSnapshotKind, appWidgetId: Int): String =
        "${kind.name}_$appWidgetId"
}

/** A small dependency-free codec so corruption never prevents a widget refresh. */
internal object WidgetSnapshotCodec {
    private const val VERSION = "1"
    private const val FIELD_SEPARATOR = '|'
    private const val ROW_SEPARATOR = '\n'

    fun encode(snapshot: CachedWidgetSnapshot): String = buildString {
        append(VERSION)
        append(FIELD_SEPARATOR)
        append(snapshot.savedAtMillis)
        snapshot.rows.forEach { row ->
            append(ROW_SEPARATOR)
            append(if (row.isChild) '1' else '0')
            append(FIELD_SEPARATOR)
            append(if (row.completed) '1' else '0')
            append(FIELD_SEPARATOR)
            append(encodeText(row.title))
            append(FIELD_SEPARATOR)
            append(encodeText(row.meta))
        }
    }

    fun decode(encoded: String): CachedWidgetSnapshot? = runCatching {
        val lines = encoded.lineSequence().toList()
        val header = lines.firstOrNull()?.split(FIELD_SEPARATOR) ?: return null
        if (header.size != 2 || header[0] != VERSION) return null
        val savedAtMillis = header[1].toLongOrNull() ?: return null
        val rows = lines.drop(1).map { line ->
            val fields = line.split(FIELD_SEPARATOR)
            require(fields.size == 4)
            CachedWidgetRow(
                isChild = fields[0] == "1",
                completed = fields[1] == "1",
                title = decodeText(fields[2]),
                meta = decodeText(fields[3]),
            )
        }
        CachedWidgetSnapshot(rows, savedAtMillis)
    }.getOrNull()

    private fun encodeText(value: String): String = Base64.getUrlEncoder().withoutPadding()
        .encodeToString(value.toByteArray(StandardCharsets.UTF_8))

    private fun decodeText(value: String): String = String(
        Base64.getUrlDecoder().decode(value),
        StandardCharsets.UTF_8,
    )
}

internal fun cachedCollectionRow(
    context: Context,
    row: CachedWidgetRow,
): RemoteViews = RemoteViews(context.packageName, R.layout.widget_status_row).apply {
    val renderedTitle = if (row.completed) SpannableString(row.title).also { title ->
        title.setSpan(StrikethroughSpan(), 0, title.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    } else row.title
    setTextViewText(R.id.widget_row_title, renderedTitle)
    setTextViewText(R.id.widget_row_meta, row.meta)
    setTextColor(
        R.id.widget_row_title,
        context.getColor(if (row.completed) R.color.widget_secondary_text else R.color.widget_primary_text),
    )
    val density = context.resources.displayMetrics.density
    fun Int.dp(): Int = (this * density).toInt()
    val (left, right) = resolveWidgetHorizontalPadding(
        startPx = (if (row.isChild) 28 else 12).dp(),
        endPx = 8.dp(),
        isRtl = context.resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL,
    )
    setViewPadding(R.id.widget_row_body, left, 7.dp(), right, 7.dp())
    setViewVisibility(R.id.widget_row_action_icon, View.GONE)
    setContentDescription(
        R.id.widget_row,
        context.getString(R.string.widget_cached_row, row.title),
    )
    applyResponsiveCollectionRow(context, this, row.isChild)
}

internal fun refreshErrorRow(
    context: Context,
    hasCachedRows: Boolean,
    retryActionKey: String,
    retryAction: String,
): RemoteViews = RemoteViews(context.packageName, R.layout.widget_status_row).apply {
    setTextViewText(R.id.widget_row_title, context.getString(R.string.widget_refresh_failed))
    setTextViewText(
        R.id.widget_row_meta,
        context.getString(
            if (hasCachedRows) R.string.widget_refresh_failed_with_cache
            else R.string.widget_refresh_failed_without_cache,
        ),
    )
    setViewVisibility(R.id.widget_row_action_icon, View.VISIBLE)
    setOnClickFillInIntent(R.id.widget_row, Intent().putExtra(retryActionKey, retryAction))
    setContentDescription(
        R.id.widget_row,
        context.getString(
            if (hasCachedRows) R.string.widget_retry_refresh_with_cache
            else R.string.widget_retry_refresh_without_cache,
        ),
    )
    applyResponsiveCollectionRow(context, this, isChild = false)
}

/**
 * RemoteViews rows cannot reflow controls into a second column. At extreme
 * font scales keep the primary label and 48 dp trailing action intact, while
 * secondary metadata remains available in the row's content description.
 */
internal fun applyResponsiveCollectionRow(
    context: Context,
    views: RemoteViews,
    isChild: Boolean,
) {
    if (!useSingleLineWidgetRows(context.resources.configuration.fontScale)) return
    val density = context.resources.displayMetrics.density
    fun Int.dp(): Int = (this * density).toInt()
    val (left, right) = resolveWidgetHorizontalPadding(
        startPx = (if (isChild) 28 else 12).dp(),
        endPx = 8.dp(),
        isRtl = context.resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL,
    )
    views.setViewVisibility(R.id.widget_row_meta, View.GONE)
    views.setInt(R.id.widget_row_title, "setMaxLines", 1)
    views.setViewPadding(
        R.id.widget_row_body,
        left,
        2.dp(),
        right,
        2.dp(),
    )
}

internal fun resolveWidgetHorizontalPadding(
    startPx: Int,
    endPx: Int,
    isRtl: Boolean,
): Pair<Int, Int> = if (isRtl) endPx to startPx else startPx to endPx

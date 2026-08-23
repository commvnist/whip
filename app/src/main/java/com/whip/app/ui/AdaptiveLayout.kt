package com.whip.app.ui

enum class WhipFoldOrientation { Vertical, Horizontal }

data class WhipFoldInfo(
    val orientation: WhipFoldOrientation,
    val leftPx: Int,
    val topPx: Int,
    val rightPx: Int,
    val bottomPx: Int,
    val separating: Boolean,
    val halfOpened: Boolean,
) {
    val widthPx: Int get() = (rightPx - leftPx).coerceAtLeast(0)
    val heightPx: Int get() = (bottomPx - topPx).coerceAtLeast(0)
}

enum class WhipAdaptiveLayout { Compact, NavigationRail, ExpandedDashboard, BookFold, TabletopFold }

fun selectWhipAdaptiveLayout(
    widthDp: Int,
    heightDp: Int,
    fold: WhipFoldInfo?,
): WhipAdaptiveLayout = when {
    fold?.separating == true && fold.orientation == WhipFoldOrientation.Horizontal ->
        WhipAdaptiveLayout.TabletopFold
    fold?.orientation == WhipFoldOrientation.Vertical && (fold.separating || widthDp >= 600) ->
        WhipAdaptiveLayout.BookFold
    widthDp >= 840 && heightDp >= 480 -> WhipAdaptiveLayout.ExpandedDashboard
    widthDp >= 600 && heightDp >= 480 -> WhipAdaptiveLayout.NavigationRail
    else -> WhipAdaptiveLayout.Compact
}

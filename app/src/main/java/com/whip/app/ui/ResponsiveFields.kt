package com.whip.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp

/** Keeps dense editor pairs readable at large text sizes instead of squeezing both fields. */
@Composable
internal fun ResponsiveFieldPair(
    modifier: Modifier = Modifier,
    minimumFieldWidth: Dp = 236.dp,
    first: @Composable (Modifier) -> Unit,
    second: @Composable (Modifier) -> Unit,
) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val stack = maxWidth < minimumFieldWidth * 2 + WhipSpacing.sibling || LocalDensity.current.fontScale >= 1.5f
        if (stack) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                first(Modifier.fillMaxWidth())
                second(Modifier.fillMaxWidth())
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                first(Modifier.weight(1f))
                second(Modifier.weight(1f))
            }
        }
    }
}

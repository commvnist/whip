package com.whip.app.ui

import com.whip.app.core.AppSettings
import com.whip.app.domain.UnitDimension

internal fun AppSettings.preferredUnitId(dimension: UnitDimension): String? = when (dimension) {
    UnitDimension.Mass -> massUnitId
    UnitDimension.Distance -> distanceUnitId
    UnitDimension.Volume -> volumeUnitId
    else -> null
}

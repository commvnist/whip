package com.whip.app.domain

fun massFromKilograms(kilograms: Double, unitId: String): Double =
    (BuiltInUnits.get(unitId)?.takeIf { it.dimension == UnitDimension.Mass }
        ?: requireNotNull(BuiltInUnits.get("kilogram"))).fromCanonical(kilograms)

fun massToKilograms(value: Double, unitId: String): Double =
    (BuiltInUnits.get(unitId)?.takeIf { it.dimension == UnitDimension.Mass }
        ?: requireNotNull(BuiltInUnits.get("kilogram"))).toCanonical(value)

fun distanceFromMetres(metres: Double, unitId: String): Double =
    (BuiltInUnits.get(unitId)?.takeIf { it.dimension == UnitDimension.Distance }
        ?: requireNotNull(BuiltInUnits.get("kilometre"))).fromCanonical(metres)

fun distanceToMetres(value: Double, unitId: String): Double =
    (BuiltInUnits.get(unitId)?.takeIf { it.dimension == UnitDimension.Distance }
        ?: requireNotNull(BuiltInUnits.get("kilometre"))).toCanonical(value)

fun unitSymbol(unitId: String): String = BuiltInUnits.get(unitId)?.symbol ?: unitId

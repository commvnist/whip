package com.whip.app.domain

import java.math.BigDecimal
import java.math.MathContext

/** Authored finite decimal readings must not lose small values through floating-point cancellation. */
internal fun Iterable<Double>.decimalTotal(): BigDecimal = fold(BigDecimal.ZERO) { total, value ->
    total.add(BigDecimal.valueOf(value))
}

// Divide before converting: an overflowing Double total can still have a finite mean.
internal fun BigDecimal.decimalMean(count: Int): Double =
    divide(BigDecimal.valueOf(count.toLong()), MathContext.DECIMAL128).toDouble()

internal fun Collection<Double>.preciseSum(): Double =
    if (any { !it.isFinite() }) sum() else decimalTotal().toDouble()

internal fun Collection<Double>.preciseAverage(): Double =
    if (isEmpty() || any { !it.isFinite() }) average() else decimalTotal().decimalMean(size)

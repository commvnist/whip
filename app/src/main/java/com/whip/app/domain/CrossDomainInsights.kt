package com.whip.app.domain

import kotlin.math.sqrt

data class CorrelationResult(
    val coefficient: Double,
    val sampleSize: Int,
)

/** Returns no result until both series contain enough actual observations and variation. */
fun pearsonCorrelation(
    left: List<Double>,
    right: List<Double>,
    minimumSamples: Int = 7,
    minimumObservedDays: Int = 7,
): CorrelationResult? {
    require(left.size == right.size) { "Series must have the same number of points" }
    if (left.size < minimumSamples) return null
    if (left.count { it != 0.0 } < minimumObservedDays || right.count { it != 0.0 } < minimumObservedDays) return null
    val leftMean = left.average()
    val rightMean = right.average()
    var numerator = 0.0
    var leftSquares = 0.0
    var rightSquares = 0.0
    left.indices.forEach { index ->
        val x = left[index] - leftMean
        val y = right[index] - rightMean
        numerator += x * y
        leftSquares += x * x
        rightSquares += y * y
    }
    val denominator = sqrt(leftSquares * rightSquares)
    if (denominator == 0.0) return null
    return CorrelationResult((numerator / denominator).coerceIn(-1.0, 1.0), left.size)
}

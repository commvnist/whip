package com.whip.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CrossDomainInsightsTest {
    @Test fun correlationRequiresEnoughObservedDays() {
        assertNull(pearsonCorrelation(List(30) { if (it < 6) 1.0 else 0.0 }, List(30) { if (it < 6) 2.0 else 0.0 }))
    }

    @Test fun correlationReportsCoefficientAndSampleSize() {
        val result = pearsonCorrelation((1..10).map(Int::toDouble), (1..10).map { it * 2.0 })
        requireNotNull(result)
        assertEquals(1.0, result.coefficient, 1e-9)
        assertEquals(10, result.sampleSize)
    }
}

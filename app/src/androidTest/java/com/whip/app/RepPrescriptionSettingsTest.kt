package com.whip.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.whip.app.core.RepPrescriptionScheme
import com.whip.app.core.SharedPreferencesSettingsRepository
import com.whip.app.domain.WorkoutSetClassification
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RepPrescriptionSettingsTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val preferences get() = context.getSharedPreferences("whip-settings", Context.MODE_PRIVATE)

    @Before
    fun clearBefore() {
        preferences.edit().clear().commit()
    }

    @After
    fun clearAfter() {
        preferences.edit().clear().commit()
    }

    @Test
    fun schemesSurviveRepositoryRecreationAndStableIdEditsAndDeletes() {
        val original = RepPrescriptionScheme(
            id = "my-strength-plan",
            name = "Strength",
            setCount = 5,
            repetitionsMin = 3,
            repetitionsMax = 5,
            classification = WorkoutSetClassification.Working,
            restSeconds = 180,
        )
        SharedPreferencesSettingsRepository(context).update {
            it.copy(repPrescriptionSchemes = listOf(original))
        }

        val recreated = SharedPreferencesSettingsRepository(context)
        assertEquals(listOf(original), recreated.current().repPrescriptionSchemes)

        val edited = original.copy(name = "Heavy", repetitionsMax = 3)
        recreated.update { it.copy(repPrescriptionSchemes = listOf(edited)) }
        assertEquals(listOf(edited), SharedPreferencesSettingsRepository(context).current().repPrescriptionSchemes)

        recreated.update { it.copy(repPrescriptionSchemes = emptyList()) }
        assertEquals(emptyList<RepPrescriptionScheme>(), SharedPreferencesSettingsRepository(context).current().repPrescriptionSchemes)
    }
}

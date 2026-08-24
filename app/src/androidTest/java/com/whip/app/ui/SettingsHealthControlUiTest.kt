package com.whip.app.ui

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.whip.app.core.HealthDataType
import com.whip.app.ui.theme.WhipTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsHealthControlUiTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun pausedHealthSyncShowsSavedCategorySelectionInsteadOfFalselyClearingIt() {
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                HealthDataTypeSetting(
                    type = HealthDataType.Weight,
                    syncEnabled = false,
                    selected = true,
                    accessGranted = false,
                    onChange = {},
                )
            }
        }

        compose.onNodeWithTag("health-type-Weight")
            .assertIsOn()
            .assertIsNotEnabled()
    }
}

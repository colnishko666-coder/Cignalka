package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun `app name matches IBT Start`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("IBT Start", appName)
    }

    @Test
    fun `control dashboard and tabs render and respond`() {
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("engine_start_button").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("btn_lock").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("btn_unlock").performScrollTo().assertIsDisplayed()

        // Unlock car in simulation mode
        composeRule.onNodeWithTag("btn_unlock").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Открыта").assertExists()

        // Navigate to Automation & PKE tab
        composeRule.onNodeWithTag("nav_automation").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Расписание автозапуска").assertIsDisplayed()

        // Navigate to Device Settings tab
        composeRule.onNodeWithTag("nav_settings").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Bluetooth-модуль IBT Alarm").assertIsDisplayed()
    }
}

package com.example.eventia

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.eventia.ui.screens.eventdetail.EventDetailContent
import com.example.eventia.ui.screens.eventdetail.EventDetailUiState
import com.example.eventia.ui.theme.EventiaTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EventLoadingTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun loadingDetailShowsAnAccessibleSkeletonAndBackArrow() {
        val resources = InstrumentationRegistry.getInstrumentation().targetContext.resources
        val loading = resources.getString(R.string.event_detail_loading)
        var wentBack = false
        compose.setContent {
            EventiaTheme {
                EventDetailContent(
                    EventDetailUiState.Loading,
                    onRetry = {},
                    onBack = { wentBack = true },
                    onVenueClick = {}
                )
            }
        }
        compose.onNodeWithContentDescription(loading).assertIsDisplayed()
        compose.onNodeWithText(loading).assertDoesNotExist()
        compose.onNodeWithText(resources.getString(R.string.detail_event)).assertDoesNotExist()
        compose.onNodeWithContentDescription(resources.getString(R.string.app_name)).assertDoesNotExist()
        compose.onNodeWithContentDescription(resources.getString(R.string.navigate_back)).performClick()
        compose.runOnIdle { assertTrue(wentBack) }
    }
}

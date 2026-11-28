package com.example.eventia

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.eventia.domain.model.Category
import com.example.eventia.domain.model.Event
import com.example.eventia.domain.model.Venue
import com.example.eventia.ui.screens.eventdetail.EventDetailContent
import com.example.eventia.ui.screens.eventdetail.EventDetailUiState
import com.example.eventia.ui.theme.EventiaTheme
import java.time.OffsetDateTime
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EventAttendanceBarTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun fitTicketAndBothActionsOnNarrowScreenWithLargeText() {
        val resources = InstrumentationRegistry.getInstrumentation().targetContext.resources
        val event = Event(
            1, "Event", "Event summary", "Event details", OffsetDateTime.now().plusDays(1),
            null, Category(2, "Culture"), null, Venue(3, "Venue", "Address"), 1240, 1060, false
        )
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 1.5f)) {
                EventiaTheme {
                    var current by remember { mutableStateOf(event) }
                    Box(Modifier.width(320.dp).fillMaxHeight()) {
                        EventDetailContent(
                            state = EventDetailUiState.Content(current, authenticated = true),
                            onRetry = {},
                            onBack = {},
                            onVenueClick = {},
                            onAttend = { current = it.copy(isAttending = true, availableSpots = it.availableSpots - 1) }
                        )
                    }
                }
            }
        }
        val total = compose.onNodeWithText(resources.getString(R.string.event_total_capacity, 1240))
        val attend = compose.onNodeWithText(resources.getString(R.string.event_attend))
        attend.assertIsDisplayed()
        compose.onNodeWithText(resources.getString(R.string.event_available_count, 1060)).assertIsDisplayed()
        total.assertIsDisplayed()
        attend.performClick()
        val cancel = compose.onNodeWithText(resources.getString(R.string.event_cancel_attendance)).assertIsDisplayed()
        compose.onNodeWithText(resources.getString(R.string.event_available_count, 1059)).assertIsDisplayed()
        total.assertIsDisplayed()
        val directions = compose.onNodeWithText(resources.getString(R.string.venue_directions))
        directions.performScrollTo().assertIsDisplayed()
        assertTrue(directions.getUnclippedBoundsInRoot().bottom <= cancel.getUnclippedBoundsInRoot().top)
    }
}

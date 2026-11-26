package com.example.eventia

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.eventia.domain.model.Venue
import com.example.eventia.ui.screens.explore.ExploreContent
import com.example.eventia.ui.screens.explore.ExploreUiState
import com.example.eventia.ui.screens.venue.VenueListContent
import com.example.eventia.ui.screens.venue.VenueListUiState
import com.example.eventia.ui.theme.EventiaTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExploreRefreshGestureTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun emptyEventsRefreshWhenPulled() {
        var requests = 0
        compose.setContent {
            EventiaTheme {
                ExploreContent(
                    state = ExploreUiState(
                        content = ExploreUiState.Content(emptyList(), emptyList()),
                        isLoading = false
                    ),
                    onQueryChange = {},
                    onCategorySelect = {},
                    onClearFilters = {},
                    onRefresh = { requests++ },
                    onEventClick = {},
                    sections = {}
                )
            }
        }
        assertTransientRefresh { assertEquals(1, requests) }
    }

    @Test
    fun venueListRefreshesWhenPulled() {
        var requests = 0
        compose.setContent {
            EventiaTheme {
                VenueListContent(
                    state = VenueListUiState(venues = listOf(Venue(1, "Teatro", "Calle Sucre"))),
                    onQueryChange = {},
                    onRefresh = { requests++ },
                    onVenueClick = {},
                    sections = {}
                )
            }
        }
        assertTransientRefresh { assertEquals(1, requests) }
    }

    private fun assertTransientRefresh(assertRequest: () -> Unit) {
        val indicator = compose.onNodeWithContentDescription("Actualizar")
        compose.onNodeWithText("Actualizar").assertDoesNotExist()
        indicator.assertDoesNotExist()
        compose.mainClock.autoAdvance = false
        compose.onRoot().performTouchInput {
            swipe(
                start = Offset(center.x, height * 0.4f),
                end = Offset(center.x, height * 0.84f),
                durationMillis = 350
            )
        }
        compose.mainClock.advanceTimeBy(180)
        indicator.assertExists()
        compose.runOnIdle(assertRequest)
        compose.mainClock.advanceTimeBy(1_200)
        indicator.assertDoesNotExist()
    }
}
